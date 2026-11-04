package com.cyberhub.manager;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/**
 * Loopback-only HTTP media server: streams registered local files to the
 * WebView <video>/<audio> player with full HTTP Range support so seeking
 * works for large or obscure-format media. Never binds to external
 * interfaces; ids are unguessable random tokens.
 */
public final class MediaServer implements Closeable {
    private final MainActivity activity;
    private ServerSocket socket;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private volatile boolean active = true;

    private static final class Entry {
        final File file;
        final String mime;
        long lastUsed;
        Entry(File f, String m) { file = f; mime = m; }
    }

    MediaServer(MainActivity a) { activity = a; }

    private synchronized void ensureStarted() throws IOException {
        if (socket != null) return;
        socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress("127.0.0.1", 0));
        Thread acceptor = new Thread(this::acceptLoop, "media-server");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private void acceptLoop() {
        while (active) {
            try {
                Socket client = socket.accept();
                pool.execute(() -> serve(client));
            } catch (Throwable ignored) {
                return; // socket closed
            }
        }
    }

    /** Registers a readable file and returns {url, mime} for the player. */
    synchronized JSONObject register(String path, String mime) throws Exception {
        File f = new File(path);
        if (!f.isFile()) throw new IOException("媒体文件未找到");
        if (!f.canRead()) throw new IOException("无法读取媒体文件");
        ensureStarted();
        if (mime == null || mime.length() == 0) mime = "application/octet-stream";
        byte[] raw = new byte[10];
        random.nextBytes(raw);
        StringBuilder id = new StringBuilder();
        for (byte b : raw) id.append(String.format(Locale.US, "%02x", b));
        entries.put(id.toString(), new Entry(f, mime));
        // Keep the table small: drop the 12 least recently used entries past 40.
        while (entries.size() > 40) {
            String oldest = null;
            long best = Long.MAX_VALUE;
            for (Map.Entry<String, Entry> e : entries.entrySet()) {
                if (e.getValue().lastUsed < best) { best = e.getValue().lastUsed; oldest = e.getKey(); }
            }
            if (oldest == null) break;
            entries.remove(oldest);
        }
        return new JSONObject()
                .put("url", "http://127.0.0.1:" + socket.getLocalPort() + "/m/" + id)
                .put("mime", mime)
                .put("size", f.length());
    }

    private void serve(Socket client) {
        try {
            client.setSoTimeout(15000);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
            String request = in.readLine();
            if (request == null) { client.close(); return; }
            String[] parts = request.split(" ");
            boolean head = parts.length >= 2 && parts[0].equalsIgnoreCase("HEAD");
            String target = parts.length >= 2 ? parts[1] : "/";
            String rangeHeader = null;
            String line;
            while ((line = in.readLine()) != null && line.length() > 0) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Range"))
                    rangeHeader = line.substring(colon + 1).trim();
            }
            String id = target.startsWith("/m/") ? target.substring(3) : "";
            Entry entry;
            synchronized (this) {
                entry = entries.get(id);
                if (entry != null) entry.lastUsed = System.currentTimeMillis();
            }
            if (entry == null) { respond(client, 404, null, 0, null, head, null); return; }
            long length = entry.file.length();
            long start = 0, end = length - 1;
            boolean partial = false;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String spec = rangeHeader.substring(6).split(",")[0].trim();
                int dash = spec.indexOf('-');
                try {
                    if (dash > 0) {
                        start = Long.parseLong(spec.substring(0, dash).trim());
                        if (dash + 1 < spec.length()) end = Long.parseLong(spec.substring(dash + 1).trim());
                        else if (start > end) end = length - 1;
                        partial = true;
                    } else if (dash == 0) {
                        long suffix = Long.parseLong(spec.substring(1).trim());
                        start = Math.max(0, length - suffix);
                        end = length - 1;
                        partial = true;
                    }
                } catch (NumberFormatException ignored) { }
            }
            if (start < 0 || start >= length || end < start || end >= length) {
                respond(client, 416, entry.mime, 0, "bytes */" + length, head, null);
                return;
            }
            long total = end - start + 1;
            OutputStream out = respond(client, partial ? 206 : 200, entry.mime, total,
                    partial ? ("bytes " + start + "-" + end + "/" + length) : null, head,
                    entry.file.isFile() ? ("attachment; filename=\"" + safeName(entry.file.getName()) + "\"") : null);
            if (out != null && !head) stream(entry.file, out, start, total);
        } catch (Throwable ignored) {
        } finally {
            try { client.close(); } catch (Throwable ignored) { }
        }
    }

    private static String safeName(String name) {
        StringBuilder b = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c > 31 && c < 127 && c != '"' && c != '\\') b.append(c);
        }
        return b.length() > 0 ? b.toString() : "media";
    }

    private OutputStream respond(Socket client, int status, String mime, long length,
                                 String contentRange, boolean head, String disposition) throws IOException {
        OutputStream raw = client.getOutputStream();
        StringBuilder head_text = new StringBuilder();
        head_text.append("HTTP/1.1 ").append(status).append(status == 200 ? " OK" : status == 206 ? " Partial Content"
                : status == 416 ? " Range Not Satisfiable" : " Not Found").append("\r\n");
        if (mime != null) head_text.append("Content-Type: ").append(mime).append("\r\n");
        head_text.append("Accept-Ranges: bytes\r\n");
        if (contentRange != null) head_text.append("Content-Range: ").append(contentRange).append("\r\n");
        if (status != 416 && status != 404) head_text.append("Content-Length: ").append(length).append("\r\n");
        if (disposition != null) head_text.append("Content-Disposition: inline").append("\r\n");
        head_text.append("Cache-Control: no-store\r\n");
        head_text.append("Connection: close\r\n\r\n");
        raw.write(head_text.toString().getBytes(StandardCharsets.ISO_8859_1));
        raw.flush();
        return (status == 200 || status == 206) && !head ? raw : null;
    }

    private void stream(File file, OutputStream out, long start, long total) {
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = new BufferedInputStream(new FileInputStream(file), 64 * 1024)) {
            long skipped = 0;
            while (skipped < start) {
                long n = in.skip(start - skipped);
                if (n <= 0) break;
                skipped += n;
            }
            long written = 0;
            int read;
            while (written < total && (read = in.read(buffer, 0, (int) Math.min(buffer.length, total - written))) > 0) {
                out.write(buffer, 0, read);
                written += read;
                out.flush();
            }
        } catch (Throwable ignored) {
            // Client aborted (seek / close) - perfectly normal for media streaming.
        }
    }

    @Override
    public synchronized void close() {
        active = false;
        try { if (socket != null) socket.close(); } catch (Throwable ignored) { }
        pool.shutdownNow();
        entries.clear();
    }
}
