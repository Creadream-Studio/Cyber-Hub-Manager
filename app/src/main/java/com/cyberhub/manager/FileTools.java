package com.cyberhub.manager;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.*;

/** Long-press file tools: split, merge, name swap and folder listing export.
 *  Pure java.io so the desktop smoke tests can exercise it on the JVM. */
public final class FileTools {
    private FileTools() {}

    /** name.part01, name.part02 … (part numbers keep at least two digits). */
    private static final Pattern PART = Pattern.compile("^(.*)\\.part(\\d{2,})$");

    /** Split one file into fixed-size volumes named name.partNN in the same folder. */
    public static JSONObject split(File src, long chunkBytes) throws IOException, JSONException {
        if (!src.isFile()) throw new IOException("只能分割普通文件");
        if (chunkBytes < 1) throw new IOException("分卷大小必须大于 0 字节");
        long total = src.length();
        if (total <= chunkBytes) throw new IOException("文件大小不超过分卷大小，无需分割");
        int count = (int) ((total + chunkBytes - 1) / chunkBytes);
        int digits = Math.max(String.valueOf(count).length(), 2);
        JSONArray outputs = new JSONArray();
        try (InputStream in = new BufferedInputStream(new FileInputStream(src))) {
            byte[] buf = new byte[65536];
            int index = 1;
            long remaining = total;
            while (remaining > 0) {
                File part = FileOps.child(src.getParentFile(), src.getName() + ".part" + String.format("%0" + digits + "d", index));
                if (part.exists()) throw new IOException("分卷已存在：" + part.getName());
                long n = Math.min(chunkBytes, remaining);
                try (OutputStream out = new BufferedOutputStream(new FileOutputStream(part))) {
                    while (n > 0) {
                        int r = in.read(buf, 0, (int) Math.min(buf.length, n));
                        if (r < 0) throw new IOException("源文件读取中断");
                        out.write(buf, 0, r);
                        n -= r;
                        remaining -= r;
                    }
                }
                outputs.put(part.getName());
                index++;
            }
        }
        return new JSONObject().put("outputs", outputs).put("parts", outputs.length()).put("bytes", total);
    }

    /** Merge split volumes back into the original file. Any subset of the set can be
     *  selected; every volume of the same base name in the folder is required. */
    public static JSONObject merge(List<File> selected) throws IOException, JSONException {
        if (selected.size() < 2) throw new IOException("请选择至少两个分卷（name.partNN）");
        String base = null;
        File parent = null;
        for (File f : selected) {
            Matcher m = PART.matcher(f.getName());
            if (!m.matches()) throw new IOException("不是分卷文件：" + f.getName() + "（应为 name.partNN）");
            if (base == null) { base = m.group(1); parent = f.getParentFile(); }
            else if (!base.equals(m.group(1)) || !parent.getAbsoluteFile().equals(f.getParentFile().getAbsoluteFile()))
                throw new IOException("所选分卷不属于同一文件");
        }
        String baseName = base;
        File[] siblings = parent.listFiles((d, n) -> {
            Matcher m = PART.matcher(n);
            return m.matches() && m.group(1).equals(baseName);
        });
        if (siblings == null) throw new IOException("无法读取文件夹");
        TreeMap<Long, File> ordered = new TreeMap<>();
        for (File f : siblings) {
            Matcher m = PART.matcher(f.getName());
            if (m.matches()) ordered.put(Long.parseLong(m.group(2)), f);
        }
        for (long i = 1; i <= ordered.size(); i++)
            if (!ordered.containsKey(i)) throw new IOException("缺少分卷 part" + String.format("%02d", i));
        File out = FileOps.child(parent, base);
        if (out.exists()) throw new IOException("输出已存在：" + base);
        File tmp = new File(parent, "." + base + ".merging");
        try {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
                for (File f : ordered.values())
                    try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
                        FileOps.copy(in, os, FileOps.MAX_EXTRACT);
                    }
            }
            Files.move(tmp.toPath(), out.toPath());
        } catch (IOException e) {
            tmp.delete();
            throw e;
        }
        return new JSONObject().put("path", out.getPath()).put("parts", ordered.size()).put("bytes", out.length());
    }

    /** Swap the names of two files (or folders) in the same folder. */
    public static void swapNames(File a, File b) throws IOException {
        if (!a.exists() || !b.exists()) throw new IOException("请选择两个已存在的文件");
        if (!a.getParentFile().getAbsoluteFile().equals(b.getParentFile().getAbsoluteFile()))
            throw new IOException("两个文件必须在同一文件夹");
        if (a.getCanonicalFile().equals(b.getCanonicalFile())) throw new IOException("请选择两个不同的文件");
        File tmp = new File(a.getParentFile(), ".swap-" + System.nanoTime());
        boolean movedA = false, movedB = false;
        try {
            Files.move(a.toPath(), tmp.toPath());
            movedA = true;
            Files.move(b.toPath(), a.toPath());
            movedB = true;
            Files.move(tmp.toPath(), b.toPath());
        } catch (IOException e) {
            try {
                if (movedB) Files.move(a.toPath(), b.toPath());
                if (movedA) Files.move(tmp.toPath(), a.toPath());
            } catch (IOException recovery) { e.addSuppressed(recovery); }
            throw new IOException("交换文件名失败，已尝试还原：" + e.getMessage());
        }
    }

    /** Write a plain-text listing of one folder next to its contents. */
    public static JSONObject exportList(File dir, String name) throws IOException, JSONException {
        if (!dir.isDirectory()) throw new IOException("请选择一个文件夹");
        File[] items = dir.listFiles();
        if (items == null) throw new IOException("无法读取文件夹");
        Arrays.sort(items, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        File out = FileOps.child(dir, name);
        if (out.exists()) throw new IOException("输出已存在：" + out.getName());
        SimpleDateFormat stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        StringBuilder text = new StringBuilder();
        text.append("# 文件列表\n# 文件夹：").append(dir.getAbsolutePath())
                .append("\n# 导出时间：").append(stamp.format(new Date())).append("\n");
        int folders = 0, files = 0;
        long bytes = 0;
        for (File f : items) {
            if (f.getName().equals(name)) continue;
            if (f.isDirectory()) {
                folders++;
                text.append("文件夹\t-\t-\t").append(f.getName()).append('\n');
            } else {
                files++;
                bytes += f.length();
                text.append("文件\t").append(f.length()).append('\t')
                        .append(stamp.format(new Date(f.lastModified()))).append('\t')
                        .append(f.getName()).append('\n');
            }
        }
        text.append("\n# 共 ").append(folders).append(" 个文件夹，").append(files).append(" 个文件")
                .append("，合计 ").append(bytes).append(" 字节\n");
        FileOps.writeAtomic(out, text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), false);
        return new JSONObject().put("path", out.getPath()).put("folders", folders).put("files", files).put("bytes", bytes);
    }
}
