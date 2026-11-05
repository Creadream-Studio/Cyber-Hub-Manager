package com.cyberhub.manager;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Explicitly started, pairing-code protected, read-only remote and MCP servers. */
public final class LocalService {
    private final File defaultRoot;
    private final Map<String, Server> running = new HashMap<>();
    private final ExecutorService clients = Executors.newFixedThreadPool(3);

    LocalService(File root) { defaultRoot = root; }

    synchronized JSONObject action(JSONObject a) throws Exception {
        String kind = a.optString("kind", "remote"), op = a.optString("op", "status");
        if (!kind.equals("remote") && !kind.equals("mcp")) throw new IOException("未知的服务类型");
        Server current = running.get(kind);
        if (op.equals("kick") && current != null) current.kick(a.optString("ip"));
        if (op.equals("unblock") && current != null) current.unblock(a.optString("ip"));
        if (op.equals("stop")) {
            current = running.remove(kind);
            if (current != null) current.close();
        }
        if (op.equals("start") && !running.containsKey(kind)) {
            File folder = new File(a.optString("path", defaultRoot.getPath())).getCanonicalFile();
            if (!folder.isDirectory() || !folder.canRead()) throw new IOException("请选择一个可读的文件夹");
            String protocol, code;
            int port;
            if (kind.equals("mcp")) {
                protocol = "http";
                port = 8766;
                code = null;
            } else {
                protocol = a.optString("protocol", "http").trim().toLowerCase(Locale.ROOT);
                if (!protocol.equals("http") && !protocol.equals("ftp")) throw new IOException("协议只能是 HTTP 或 FTP");
                port = a.optInt("port", protocol.equals("ftp") ? 2121 : 8765);
                if (port < 1024 || port > 65535) throw new IOException("端口需在 1024-65535 之间");
                code = a.optString("code", "").trim();
                if (!code.matches("[A-Za-z0-9_-]{4,32}")) throw new IOException("配对码需为 4-32 位字母、数字、- 或 _");
            }
            Server server = new Server(kind, folder, protocol, port, code);
            running.put(kind, server);
            server.start();
        }
        JSONArray list = new JSONArray();
        for (Server server : running.values()) list.put(server.info());
        return new JSONObject().put("servers", list);
    }

    synchronized void stop() {
        for (Server server : running.values()) server.close();
        running.clear();
        clients.shutdownNow();
    }

    private static boolean same(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    class Server {
        final String kind, protocol, code, token;
        final File root;
        final int port;
        final ServerSocket socket;
        final FtpShareServer ftp;
        final Map<String, Device> devices = new LinkedHashMap<>();
        final Set<String> blocked = new LinkedHashSet<>();
        final Map<String, int[]> failures = new HashMap<>();
        final Map<String, Long> lockedUntil = new HashMap<>();
        volatile boolean active = true;

        Server(String k, File f, String p, int requestedPort, String c) throws Exception {
            kind = k;
            root = f;
            if (kind.equals("mcp")) {
                token = randomToken();
                protocol = "http";
                code = token;
                ftp = null;
                socket = new ServerSocket();
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("127.0.0.1", 8766));
                port = socket.getLocalPort();
            } else {
                protocol = p;
                code = c;
                token = c;
                if (protocol.equals("ftp")) {
                    socket = null;
                    ftp = new FtpShareServer(root, code, requestedPort, new FtpShareServer.Events() {
                        public boolean blocked(String ip) { return isBlocked(ip); }
                        public String opened(String ip, String agent, Socket s) { return device(ip, "FTP", agent, s); }
                        public void touched(String id) { synchronized (Server.this) { Device d = devices.get(id); if (d != null) d.last = System.currentTimeMillis(); } }
                        public void closed(String id) { synchronized (Server.this) { devices.remove(id); } }
                    });
                    port = ftp.port();
                } else {
                    ftp = null;
                    socket = new ServerSocket();
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress("0.0.0.0", requestedPort));
                    port = socket.getLocalPort();
                }
            }
        }

        JSONObject info() throws Exception {
            JSONArray urls = new JSONArray();
            if (kind.equals("mcp")) {
                urls.put("http://127.0.0.1:8766/mcp");
            } else {
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress ip = addresses.nextElement();
                        if (!(ip instanceof Inet4Address) || ip.isLoopbackAddress()) continue;
                        urls.put(protocol.equals("ftp")
                                ? "ftp://" + ip.getHostAddress() + ":" + port + "/"
                                : "http://" + ip.getHostAddress() + ":" + port + "/?code=" + URLEncoder.encode(code, "UTF-8"));
                    }
                }
            }
            JSONObject result = new JSONObject().put("kind", kind).put("root", root.getPath())
                    .put("protocol", protocol).put("port", port).put("urls", urls);
            if (kind.equals("mcp")) {
                result.put("token", token);
            } else {
                result.put("code", code).put("devices", devicesJson());
                JSONArray disabled = new JSONArray();
                synchronized (this) { for (String ip : blocked) disabled.put(ip); }
                result.put("blocked", disabled);
            }
            return result;
        }

        void start() {
            if (ftp != null) { ftp.start(); return; }
            Thread thread = new Thread(() -> {
                while (active) {
                    try {
                        Socket client = socket.accept();
                        clients.submit(() -> serve(client));
                    } catch (IOException e) { break; }
                }
            }, "CyberHub-" + kind);
            thread.setDaemon(true);
            thread.start();
        }

        void close() {
            active = false;
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
            if (ftp != null) ftp.close();
            synchronized (this) {
                for (Device d : devices.values()) if (d.socket != null) try { d.socket.close(); } catch (IOException ignored) {}
                devices.clear();
            }
        }

        synchronized boolean isBlocked(String ip) { return blocked.contains(ip); }

        synchronized boolean throttled(String ip) {
            Long until = lockedUntil.get(ip);
            if (until == null) return false;
            if (until > System.currentTimeMillis()) return true;
            lockedUntil.remove(ip);
            failures.remove(ip);
            return false;
        }

        synchronized void failed(String ip) {
            int[] count = failures.computeIfAbsent(ip, k -> new int[1]);
            if (++count[0] >= 5) {
                lockedUntil.put(ip, System.currentTimeMillis() + 60000L);
                count[0] = 0;
            }
        }

        synchronized void succeeded(String ip) {
            failures.remove(ip);
            lockedUntil.remove(ip);
        }

        synchronized String device(String ip, String protocol, String agent, Socket s) {
            String id = s == null ? "http:" + ip : "ftp:" + ip + ":" + s.getPort();
            Device d = devices.get(id);
            if (d == null) {
                d = new Device(id, ip, protocol, agent, s);
                devices.put(id, d);
            } else {
                d.last = System.currentTimeMillis();
                if (agent != null && !agent.isEmpty()) d.agent = agent;
                if (s != null) d.socket = s;
            }
            return id;
        }

        synchronized void kick(String ip) {
            if (ip == null || ip.isEmpty()) return;
            blocked.add(ip);
            for (Iterator<Device> it = devices.values().iterator(); it.hasNext();) {
                Device d = it.next();
                if (d.ip.equals(ip)) {
                    it.remove();
                    if (d.socket != null) try { d.socket.close(); } catch (IOException ignored) {}
                }
            }
        }

        synchronized void unblock(String ip) { blocked.remove(ip); }

        synchronized JSONArray devicesJson() throws Exception {
            long stale = System.currentTimeMillis() - 600000;
            for (Iterator<Device> it = devices.values().iterator(); it.hasNext();) {
                Device d = it.next();
                if (d.socket == null && d.last < stale) it.remove();
            }
            JSONArray list = new JSONArray();
            for (Device d : devices.values()) list.put(new JSONObject().put("id", d.id).put("ip", d.ip)
                    .put("protocol", d.protocol).put("agent", d.agent).put("since", d.since)
                    .put("last", d.last).put("active", d.socket != null && !d.socket.isClosed()));
            return list;
        }

        void serve(Socket clientSocket) {
            try (Socket client = clientSocket) {
                client.setSoTimeout(8000);
                InputStream in = client.getInputStream();
                ByteArrayOutputStream hb = new ByteArrayOutputStream();
                int b, last = 0;
                while ((b = in.read()) != -1) {
                    hb.write(b);
                    last = (last << 8) | b;
                    if (last == 0x0d0a0d0a) break;
                    if (hb.size() > 16384) throw new IOException("请求头过大");
                }
                String[] lines = hb.toString("UTF-8").split("\r\n");
                if (lines.length == 0) return;
                String[] req = lines[0].split(" ");
                if (req.length < 2) return;
                Map<String, String> headers = new HashMap<>();
                for (int j = 1; j < lines.length; j++) {
                    int p = lines[j].indexOf(':');
                    if (p > 0) headers.put(lines[j].substring(0, p).toLowerCase(Locale.ROOT), lines[j].substring(p + 1).trim());
                }
                String ip = client.getInetAddress().getHostAddress();
                if (!kind.equals("mcp") && isBlocked(ip)) {
                    reply(client, 403, "text/plain", "已断开".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                if (throttled(ip)) {
                    reply(client, 429, "text/plain", "尝试次数过多，请稍后重试".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                URI uri = URI.create(req[1]);
                Map<String, String> q = query(uri.getRawQuery());
                String auth = headers.getOrDefault("authorization", "");
                boolean authorized = kind.equals("mcp")
                        ? same(auth, "Bearer " + token)
                        : same(auth, "Bearer " + code) || same(q.get("code"), code) || same(q.get("token"), code)
                                || same(headers.get("x-pairing-code"), code);
                if (!authorized) {
                    failed(ip);
                    reply(client, 401, "text/plain", (kind.equals("mcp") ? "需要 token" : "需要配对码").getBytes(StandardCharsets.UTF_8));
                    return;
                }
                succeeded(ip);
                if (!kind.equals("mcp")) device(ip, "HTTP", headers.getOrDefault("user-agent", ""), null);

                if (kind.equals("mcp")) {
                    if (!req[0].equals("POST") || !uri.getPath().equals("/mcp")) {
                        reply(client, 405, "text/plain", new byte[0]);
                        return;
                    }
                    int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
                    if (length < 0 || length > 1048576) throw new IOException("无效的请求体长度");
                    byte[] body = new byte[length];
                    int off = 0;
                    while (off < length) {
                        int n = in.read(body, off, length - off);
                        if (n < 0) throw new EOFException();
                        off += n;
                    }
                    JSONObject request = new JSONObject(new String(body, StandardCharsets.UTF_8));
                    if (!request.has("id")) {
                        reply(client, 202, "application/json", new byte[0]);
                        return;
                    }
                    JSONObject response = rpc(request);
                    reply(client, 200, "application/json", response.toString().getBytes(StandardCharsets.UTF_8));
                    return;
                }

                if (!req[0].equals("GET")) {
                    reply(client, 405, "text/plain", new byte[0]);
                    return;
                }
                String rel = q.getOrDefault("path", "");
                File f = rel.isEmpty() ? root : FileOps.contained(root, rel);
                if (f.isFile()) {
                    if (f.length() > FileOps.MAX_EXTRACT) throw new IOException("文件过大");
                    OutputStream out = client.getOutputStream();
                    out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment\r\nX-Content-Type-Options: nosniff\r\nContent-Length: " + f.length() + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    try (InputStream input = new FileInputStream(f)) { FileOps.copy(input, out, FileOps.MAX_EXTRACT); }
                    return;
                }
                File[] files = f.listFiles();
                if (files == null) throw new IOException("文件夹不可用");
                Arrays.sort(files, Comparator.comparing(File::getName));
                StringBuilder html = new StringBuilder("<!doctype html><meta name=viewport content='width=device-width'><title>赛博中枢管家</title><style>body{background:#303030;color:#ddd;font:16px system-ui;padding:24px}a{color:#70bfff}li{padding:12px}</style><h1>赛博中枢管家</h1><p>只读共享文件夹</p><ul>");
                if (!rel.isEmpty()) html.append("<li><a href='?code=").append(URLEncoder.encode(code, "UTF-8")).append("'>主页</a></li>");
                for (File c : files) {
                    if (FilesSupport.symlink(c)) continue;
                    String path = root.toPath().relativize(c.toPath()).toString();
                    html.append("<li><a href='?code=").append(URLEncoder.encode(code, "UTF-8")).append("&amp;path=").append(URLEncoder.encode(path, "UTF-8")).append("'>").append(escape(c.getName())).append(c.isDirectory() ? "/" : "").append("</a></li>");
                }
                html.append("</ul>");
                reply(client, 200, "text/html; charset=utf-8", html.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) { /* Client disconnect, invalid request, or inaccessible path. */ }
        }

        JSONObject rpc(JSONObject q) throws Exception {
            JSONObject response = new JSONObject().put("jsonrpc", "2.0").put("id", q.get("id"));
            try {
                String method = q.getString("method");
                Object result;
                switch (method) {
                    case "initialize":
                        result = new JSONObject().put("protocolVersion", "2025-03-26")
                                .put("capabilities", new JSONObject().put("tools", new JSONObject()))
                                .put("serverInfo", new JSONObject().put("name", "赛博中枢管家").put("version", "0.1.0"));
                        break;
                    case "ping":
                        result = new JSONObject();
                        break;
                    case "tools/list": {
                        JSONArray tools = new JSONArray();
                        for (String name : new String[]{"list_files", "read_text"})
                            tools.put(new JSONObject().put("name", name)
                                    .put("description", name.equals("list_files") ? "列出共享文件夹中的文件" : "读取共享文件夹内的 UTF-8 文件（最大 1 MB）")
                                    .put("inputSchema", new JSONObject().put("type", "object")
                                            .put("properties", new JSONObject().put("path", new JSONObject().put("type", "string")))
                                            .put("required", new JSONArray().put("path"))));
                        result = new JSONObject().put("tools", tools);
                        break;
                    }
                    case "tools/call": {
                        JSONObject p = q.getJSONObject("params"), args = p.optJSONObject("arguments");
                        String rel = args == null ? "" : args.optString("path");
                        File f = rel.isEmpty() ? root : FileOps.contained(root, rel);
                        String text;
                        if (p.getString("name").equals("list_files")) {
                            File[] files = f.listFiles();
                            if (files == null) throw new IOException("文件夹不可用");
                            JSONArray list = new JSONArray();
                            for (File c : files) list.put(new JSONObject().put("name", c.getName()).put("directory", c.isDirectory()).put("size", c.length()));
                            text = list.toString();
                        } else if (p.getString("name").equals("read_text")) {
                            text = new String(FileOps.read(f, 1048576), StandardCharsets.UTF_8);
                        } else throw new IOException("未知的工具");
                        result = new JSONObject().put("content", new JSONArray().put(new JSONObject().put("type", "text").put("text", text)));
                        break;
                    }
                    default:
                        throw new IOException("未找到方法");
                }
                response.put("result", result);
            } catch (Exception e) {
                response.put("error", new JSONObject().put("code", -32602).put("message", e.getMessage()));
            }
            return response;
        }
    }

    static final class Device {
        final String id, ip, protocol;
        String agent;
        final long since;
        volatile long last;
        volatile Socket socket;

        Device(String id, String ip, String protocol, String agent, Socket socket) {
            this.id = id;
            this.ip = ip;
            this.protocol = protocol;
            this.agent = agent == null ? "" : agent.length() > 160 ? agent.substring(0, 160) : agent;
            this.socket = socket;
            since = last = System.currentTimeMillis();
        }
    }

    static class FilesSupport { static boolean symlink(File f) { return java.nio.file.Files.isSymbolicLink(f.toPath()); } }

    static Map<String, String> query(String q) throws Exception {
        Map<String, String> m = new HashMap<>();
        if (q != null) for (String part : q.split("&")) {
            String[] p = part.split("=", 2);
            m.put(URLDecoder.decode(p[0], "UTF-8"), p.length > 1 ? URLDecoder.decode(p[1], "UTF-8") : "");
        }
        return m;
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    static void reply(Socket c, int status, String type, byte[] data) throws IOException {
        OutputStream out = c.getOutputStream();
        out.write(("HTTP/1.1 " + status + " Response\r\nContent-Type: " + type + "\r\nContent-Length: " + data.length
                + "\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff"
                + "\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer"
                + "\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; img-src data:"
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    static String randomToken() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 255));
        return sb.toString();
    }
}
