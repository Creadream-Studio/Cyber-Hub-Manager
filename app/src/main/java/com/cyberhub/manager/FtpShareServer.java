package com.cyberhub.manager;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Small read-only FTP server used by remote management. Passive mode only. */
final class FtpShareServer implements Closeable {
    interface Events {
        boolean blocked(String ip);
        String opened(String ip, String agent, Socket socket);
        void touched(String id);
        void closed(String id);
    }

    private final File root;
    private final String code;
    private final Events events;
    private final ServerSocket server;
    private final ExecutorService clients;
    private final Set<Socket> sessions = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());
    private final Map<String, int[]> failures = new ConcurrentHashMap<>();
    private final Map<String, Long> lockedUntil = new ConcurrentHashMap<>();
    private volatile boolean active = true;

    FtpShareServer(File root, String code, int port, Events events) throws IOException {
        this.root = root.getCanonicalFile();
        this.code = code;
        this.events = events;
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(port));
        clients = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CyberHub-FTP");
            t.setDaemon(true);
            return t;
        });
    }

    int port() { return server.getLocalPort(); }

    void start() {
        Thread thread = new Thread(() -> {
            while (active) {
                try {
                    Socket socket = server.accept();
                    socket.setSoTimeout(600000);
                    String ip = socket.getInetAddress().getHostAddress();
                    if (events.blocked(ip)) {
                        try (Socket rejected = socket) {
                            rejected.getOutputStream().write("421 已断开\r\n".getBytes(StandardCharsets.UTF_8));
                        }
                        continue;
                    }
                    sessions.add(socket);
                    clients.submit(() -> session(socket));
                } catch (IOException e) {
                    if (!active) break;
                }
            }
        }, "CyberHub-FTP-accept");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() {
        active = false;
        try { server.close(); } catch (IOException ignored) {}
        for (Socket socket : sessions) try { socket.close(); } catch (IOException ignored) {}
        clients.shutdownNow();
    }

    private boolean throttled(String ip) {
        Long until = lockedUntil.get(ip);
        if (until == null) return false;
        if (until > System.currentTimeMillis()) return true;
        lockedUntil.remove(ip);
        failures.remove(ip);
        return false;
    }

    private void failed(String ip) {
        int[] count = failures.computeIfAbsent(ip, k -> new int[1]);
        if (++count[0] >= 5) {
            lockedUntil.put(ip, System.currentTimeMillis() + 60000L);
            count[0] = 0;
        }
    }

    private static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private void session(Socket socket) {
        String ip = socket.getInetAddress().getHostAddress();
        String id = null;
        DataEndpoint data = null;
        try (Socket control = socket) {
            BufferedReader in = new BufferedReader(new InputStreamReader(control.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(control.getOutputStream(), StandardCharsets.UTF_8));
            reply(out, 220, "赛博中枢管家 FTP 就绪");
            if (throttled(ip)) { reply(out, 421, "尝试次数过多，请稍后重试"); return; }
            File cwd = root;
            boolean user = false, authed = false;
            while (active) {
                String line = in.readLine();
                if (line == null) break;
                if (line.isEmpty()) { reply(out, 500, "空命令"); continue; }
                int space = line.indexOf(' ');
                String command = (space < 0 ? line : line.substring(0, space)).toUpperCase(Locale.ROOT);
                String arg = space < 0 ? "" : line.substring(space + 1);
                if (id != null) events.touched(id);

                if (command.equals("USER")) {
                    user = true;
                    authed = false;
                    reply(out, 331, "请输入配对码");
                    continue;
                }
                if (command.equals("PASS")) {
                    if (!user) { reply(out, 503, "请先发送 USER"); continue; }
                    if (!same(code, arg)) { failed(ip); authed = false; reply(out, 530, "配对码错误"); continue; }
                    authed = true;
                    id = events.opened(ip, "FTP", control);
                    reply(out, 230, "登录成功");
                    continue;
                }
                if (!authed) { reply(out, 530, "请先登录"); continue; }

                switch (command) {
                    case "SYST": reply(out, 215, "UNIX Type: L8"); break;
                    case "FEAT": raw(out, "211-Features:\r\n UTF8\r\n PASV\r\n EPSV\r\n MLST type*;size*;modify*;perm*;\r\n MLSD\r\n SIZE\r\n MDTM\r\n211 End\r\n"); break;
                    case "OPTS":
                        if (arg.equalsIgnoreCase("UTF8 ON")) reply(out, 200, "UTF8 enabled");
                        else reply(out, 501, "不支持的 OPTS");
                        break;
                    case "PWD":
                    case "XPWD": reply(out, 257, "\"" + ftpPath(cwd) + "\" is the current directory"); break;
                    case "CWD":
                    case "XCWD": {
                        try {
                            File target = resolve(cwd, arg);
                            if (!target.isDirectory() || !target.canRead()) throw new IOException();
                            cwd = target;
                            reply(out, 250, "目录已切换");
                        } catch (Exception e) { reply(out, 550, "无法进入该目录"); }
                        break;
                    }
                    case "CDUP":
                    case "XCUP": {
                        File parent = cwd.getParentFile();
                        if (parent != null && parent.toPath().startsWith(root.toPath())) cwd = parent;
                        reply(out, 250, "目录已切换");
                        break;
                    }
                    case "TYPE": reply(out, 200, "Type set to I"); break;
                    case "MODE": reply(out, 200, "Mode set to S"); break;
                    case "STRU": reply(out, 200, "Structure set to F"); break;
                    case "ALLO": reply(out, 202, "无需预分配"); break;
                    case "NOOP": reply(out, 200, "OK"); break;
                    case "ABOR": reply(out, 226, "已中止"); break;
                    case "QUIT": reply(out, 221, "再见"); return;
                    case "PASV": {
                        closeData(data);
                        data = null;
                        try {
                            DataEndpoint next = new DataEndpoint(control, false);
                            data = next;
                            reply(out, 227, "Entering Passive Mode " + next.pasv());
                        } catch (Exception e) { closeData(data); data = null; reply(out, 425, "无法打开数据端口，请改用 EPSV"); }
                        break;
                    }
                    case "EPSV": {
                        if (arg.equalsIgnoreCase("ALL")) { reply(out, 200, "EPSV ALL 已启用"); break; }
                        closeData(data);
                        data = null;
                        try {
                            DataEndpoint next = new DataEndpoint(control, true);
                            data = next;
                            reply(out, 229, "Entering Extended Passive Mode (|||" + next.port() + "|)");
                        } catch (Exception e) { closeData(data); data = null; reply(out, 425, "无法打开数据端口"); }
                        break;
                    }
                    case "PORT":
                    case "EPRT": reply(out, 502, "仅支持 PASV/EPSV 被动模式"); break;
                    case "LIST":
                    case "NLST":
                    case "MLSD": {
                        if (data == null) { reply(out, 425, "请先使用 PASV 或 EPSV"); break; }
                        DataEndpoint endpoint = data;
                        data = null;
                        Socket transfer = null;
                        try {
                            String target = arg;
                            int optionEnd = target.indexOf(' ');
                            if (target.startsWith("-") && optionEnd > 0) target = target.substring(optionEnd + 1);
                            else if (target.startsWith("-")) target = "";
                            File path = resolve(cwd, target);
                            reply(out, 150, "正在打开目录列表");
                            transfer = endpoint.open(control);
                            OutputStream output = new BufferedOutputStream(transfer.getOutputStream());
                            if (command.equals("NLST")) {
                                for (File child : children(path)) rawLine(output, child.getName());
                            } else if (command.equals("MLSD")) {
                                for (File child : children(path)) rawLine(output, fact(child) + child.getName());
                            } else {
                                if (path.isFile()) rawLine(output, listLine(path));
                                else for (File child : children(path)) rawLine(output, listLine(child));
                            }
                            output.flush();
                            reply(out, 226, "传输完成");
                        } catch (Exception e) {
                            reply(out, 426, "传输失败");
                        } finally {
                            endpoint.close();
                            if (transfer != null) try { transfer.close(); } catch (IOException ignored) {}
                        }
                        break;
                    }
                    case "RETR": {
                        if (data == null) { reply(out, 425, "请先使用 PASV 或 EPSV"); break; }
                        File path;
                        try { path = resolve(cwd, arg); }
                        catch (Exception e) { reply(out, 550, "找不到文件"); break; }
                        if (!path.isFile() || !path.canRead()) { reply(out, 550, "找不到文件"); break; }
                        if (path.length() > FileOps.MAX_EXTRACT) { reply(out, 550, "文件过大"); break; }
                        DataEndpoint endpoint = data;
                        data = null;
                        Socket transfer = null;
                        try {
                            reply(out, 150, "正在打开数据连接");
                            transfer = endpoint.open(control);
                            try (InputStream input = new FileInputStream(path)) {
                                FileOps.copy(input, transfer.getOutputStream(), FileOps.MAX_EXTRACT);
                            }
                            transfer.getOutputStream().flush();
                            reply(out, 226, "传输完成");
                        } catch (Exception e) {
                            reply(out, 426, "传输失败");
                        } finally {
                            endpoint.close();
                            if (transfer != null) try { transfer.close(); } catch (IOException ignored) {}
                        }
                        break;
                    }
                    case "SIZE": {
                        try {
                            File path = resolve(cwd, arg);
                            if (!path.isFile()) throw new IOException();
                            reply(out, 213, Long.toString(path.length()));
                        } catch (Exception e) { reply(out, 550, "找不到文件"); }
                        break;
                    }
                    case "MDTM": {
                        try {
                            File path = resolve(cwd, arg);
                            if (!path.isFile()) throw new IOException();
                            reply(out, 213, stamp(path.lastModified()));
                        } catch (Exception e) { reply(out, 550, "找不到文件"); }
                        break;
                    }
                    case "MLST": {
                        try {
                            File path = resolve(cwd, arg);
                            raw(out, "250-Listing " + ftpPath(path) + "\r\n " + fact(path) + "\r\n250 End\r\n");
                        } catch (Exception e) { reply(out, 550, "找不到文件"); }
                        break;
                    }
                    case "STAT": reply(out, 211, "赛博中枢管家 FTP"); break;
                    case "HELP": reply(out, 214, "支持 LIST, NLST, MLSD, RETR, SIZE, MDTM, MLST, CWD, PWD"); break;
                    case "REST": reply(out, 502, "不支持断点续传"); break;
                    case "STOR":
                    case "STOU":
                    case "APPE":
                    case "DELE":
                    case "MKD":
                    case "XMKD":
                    case "RMD":
                    case "XRMD":
                    case "RNFR":
                    case "RNTO": reply(out, 550, "只读共享，不能修改文件"); break;
                    case "AUTH":
                    case "PBSZ":
                    case "PROT": reply(out, 502, "未启用 TLS"); break;
                    default: reply(out, 502, "未实现的命令");
                }
            }
        } catch (Exception ignored) {
            /* Client disconnect, timeout, or kick. */
        } finally {
            closeData(data);
            sessions.remove(socket);
            if (id != null) events.closed(id);
        }
    }

    private File resolve(File cwd, String name) throws IOException {
        if (name == null) name = "";
        if (name.indexOf('\0') >= 0) throw new IOException("路径无效");
        boolean absolute = name.startsWith("/");
        String rel = name;
        while (rel.startsWith("/")) rel = rel.substring(1);
        File base = absolute ? root : cwd;
        File file = rel.isEmpty() ? base : new File(base, rel);
        file = file.getCanonicalFile();
        if (!file.toPath().startsWith(root.toPath())) throw new IOException("路径越界");
        if (Files.isSymbolicLink(file.toPath())) throw new IOException("不支持符号链接");
        return file;
    }

    private static List<File> children(File path) throws IOException {
        File[] files = path.listFiles();
        if (files == null) throw new IOException("无法读取目录");
        List<File> list = new ArrayList<>();
        for (File file : files) if (!Files.isSymbolicLink(file.toPath())) list.add(file);
        list.sort(Comparator.comparing(File::getName));
        return list;
    }

    private String ftpPath(File file) {
        String rel = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
        return rel.isEmpty() ? "/" : "/" + rel;
    }

    private static String listLine(File file) {
        String perms = file.isDirectory() ? "drwxr-xr-x" : "-rw-r--r--";
        String date = new SimpleDateFormat("MMM dd HH:mm", Locale.US).format(new Date(file.lastModified()));
        return perms + " 1 owner group " + file.length() + " " + date + " " + file.getName();
    }

    private static String fact(File file) {
        return "type=" + (file.isDirectory() ? "dir" : "file") + ";size=" + file.length()
                + ";modify=" + stamp(file.lastModified()) + ";perm="
                + (file.isDirectory() ? "elr" : "r") + "; ";
    }

    private static String stamp(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(time));
    }

    private static void rawLine(OutputStream out, String text) throws IOException {
        out.write((text + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void reply(Writer out, int code, String text) throws IOException {
        out.write(code + " " + text + "\r\n");
        out.flush();
    }

    private static void raw(Writer out, String text) throws IOException {
        out.write(text);
        out.flush();
    }

    private static void closeData(DataEndpoint data) {
        if (data != null) data.close();
    }

    private final class DataEndpoint implements Closeable {
        private final Socket control;
        private final ServerSocket listener;

        DataEndpoint(Socket control, boolean extended) throws IOException {
            this.control = control;
            InetAddress address = control.getLocalAddress();
            listener = new ServerSocket(0, 1, address);
            listener.setSoTimeout(10000);
        }

        int port() { return listener.getLocalPort(); }

        String pasv() throws IOException {
            InetAddress address = control.getLocalAddress();
            if (!(address instanceof Inet4Address)) throw new IOException("需要 IPv4，请改用 EPSV");
            byte[] bytes = address.getAddress();
            int p = port();
            return "(" + (bytes[0] & 255) + "," + (bytes[1] & 255) + "," + (bytes[2] & 255) + "," + (bytes[3] & 255)
                    + "," + (p >> 8) + "," + (p & 255) + ")";
        }

        Socket open(Socket control) throws IOException {
            Socket data = listener.accept();
            if (!data.getInetAddress().equals(control.getInetAddress())) {
                data.close();
                throw new IOException("数据连接来源不匹配");
            }
            data.setSoTimeout(30000);
            return data;
        }

        @Override
        public void close() {
            try { listener.close(); } catch (IOException ignored) {}
        }
    }
}
