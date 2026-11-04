package com.cyberhub.manager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;

/** 0.1.14：内置播放器的本地流媒体服务器（仅 127.0.0.1）冒烟测试。 */
public final class MediaServerSmokeTest {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        byte[] payload = new byte[300_000];
        new Random(42).nextBytes(payload);
        File media = new File(dir, "clip.mkv");
        try (OutputStream out = new FileOutputStream(media)) { out.write(payload); }

        MediaServer server = new MediaServer(null);
        JSONObject info = server.register(media.getPath(), "video/x-matroska");
        String url = info.getString("url");
        if (!url.startsWith("http://127.0.0.1:")) throw new AssertionError("url must be loopback: " + url);
        check("size", info.getLong("size") == payload.length);

        // 1) 全量 200
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        check("status 200", c.getResponseCode() == 200);
        check("mime", "video/x-matroska".equals(c.getContentType()));
        check("accept-ranges", "bytes".equals(c.getHeaderField("Accept-Ranges")));
        byte[] all = read(c);
        check("full length", all.length == payload.length);
        check("full bytes", Arrays.equals(all, payload));
        c.disconnect();

        // 2) Range: bytes=1000-1999 → 206
        HttpURLConnection r = (HttpURLConnection) new URL(url).openConnection();
        r.setRequestProperty("Range", "bytes=1000-1999");
        check("range status 206", r.getResponseCode() == 206);
        check("content-range", ("bytes 1000-1999/" + payload.length).equals(r.getHeaderField("Content-Range")));
        byte[] part = read(r);
        check("range length", part.length == 1000);
        check("range bytes", Arrays.equals(part, Arrays.copyOfRange(payload, 1000, 2000)));
        r.disconnect();

        // 3) Range 后缀 bytes=-500
        HttpURLConnection tail = (HttpURLConnection) new URL(url).openConnection();
        tail.setRequestProperty("Range", "bytes=-500");
        check("suffix status 206", tail.getResponseCode() == 206);
        byte[] tailBytes = read(tail);
        check("suffix length", tailBytes.length == 500);
        check("suffix bytes", Arrays.equals(tailBytes, Arrays.copyOfRange(payload, payload.length - 500, payload.length)));
        tail.disconnect();

        // 4) 未知 id → 404
        HttpURLConnection nf = (HttpURLConnection) new URL(url.substring(0, url.lastIndexOf('/') + 1) + "deadbeef").openConnection();
        check("unknown id 404", nf.getResponseCode() == 404);
        nf.disconnect();

        // 5) 中文文件名 + MIME 回退
        File chinese = new File(dir, "测试 音乐.flac");
        Files.write(chinese.toPath(), "fake-flac".getBytes(StandardCharsets.UTF_8));
        JSONObject c2 = server.register(chinese.getPath(), "audio/flac");
        HttpURLConnection c2c = (HttpURLConnection) new URL(c2.getString("url")).openConnection();
        check("chinese file 200", c2c.getResponseCode() == 200);
        check("chinese mime", "audio/flac".equals(c2c.getContentType()));
        c2c.disconnect();

        // 6) 不存在的文件 → 明确报错
        try {
            server.register(new File(dir, "missing.mp4").getPath(), "video/mp4");
            throw new AssertionError("missing file must throw");
        } catch (IOException expected) { check("missing throws", true); }

        // 7) 关闭后端口释放
        server.close();
        check("closed", true);

        System.out.println("ALL MEDIASERVER CHECKS PASSED");
    }

    private static byte[] read(HttpURLConnection c) throws IOException {
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }

    private static void check(String name, boolean ok) {
        if (!ok) throw new AssertionError("MediaServer check failed: " + name);
        System.out.println("PASS " + name);
    }
}
