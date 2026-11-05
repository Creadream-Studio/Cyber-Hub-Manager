package com.cyberhub.manager;

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.item.StringItem;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.model.ResourceType;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ResTableMapEntry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.value.ValueItem;
import com.reandroid.arsc.value.ValueType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Resource table browser/editor backed by ARSCLib. */
public final class ArscEngine {
    private static final long MAX_ARSC = 64L * 1024 * 1024;
    private static final int PAGE = 200;
    private final DataEngine owner;
    private final Map<String, Session> sessions = new HashMap<>();
    private long counter;

    static final class Session {
        final String id;
        final File file;
        final String archive, entry, label;
        final TableBlock table;
        boolean dirty;
        Session(String id, File file, String archive, String entry, String label, TableBlock table) {
            this.id = id; this.file = file; this.archive = archive; this.entry = entry; this.label = label; this.table = table;
        }
    }

    ArscEngine(DataEngine owner) { this.owner = owner; }

    synchronized JSONObject open(File file, String archive, String entry, String name) throws Exception {
        if (file.length() > MAX_ARSC) throw new IOException("resources.arsc 超过 64MB 限制");
        TableBlock table;
        try { table = TableBlock.load(file); }
        catch (Exception e) { throw new IOException("无法解析资源表：" + e.getMessage()); }
        String id = "arsc" + (++counter);
        String label = name;
        if (label == null || label.isEmpty()) label = file.getName();
        Session s = new Session(id, file, archive == null ? "" : archive, entry == null ? "" : entry, label, table);
        sessions.put(id, s);
        JSONObject out = new JSONObject().put("session", id).put("name", label).put("archive", s.archive).put("entry", s.entry);
        out.put("packages", table.size()).put("strings", table.getStringPool().size()).put("stringCount", table.getStringPool().size());
        out.put("size", file.length());
        return out;
    }

    synchronized JSONObject browse(String sid, int pkgId, int typeId, String config, int offset, int limit) throws Exception {
        Session s = get(sid);
        JSONObject out = new JSONObject().put("session", sid).put("offset", Math.max(0, offset)).put("pageSize", limit <= 0 ? PAGE : Math.min(limit, 500));
        if (pkgId < 0) {
            JSONArray packages = new JSONArray();
            for (PackageBlock p : s.table) {
                JSONArray types = new JSONArray();
                int entries = 0;
                Iterator<ResourceType> typeIt = p.getTypes();
                while (typeIt.hasNext()) {
                    ResourceType t = typeIt.next();
                    int count = countEntries(t);
                    entries += count;
                    types.put(new JSONObject().put("id", t.getId()).put("name", t.getName()).put("count", count));
                }
                packages.put(new JSONObject().put("id", p.getId()).put("name", p.getName() == null ? "" : p.getName()).put("count", entries).put("types", types));
            }
            out.put("level", "root").put("packages", packages).put("strings", s.table.getStringPool().size()).put("dirty", s.dirty);
            return out;
        }
        PackageBlock p = s.table.getPackageBlockById(pkgId);
        if (p == null) throw new IOException("包不存在");
        out.put("packageId", pkgId).put("packageName", p.getName() == null ? "" : p.getName());
        if (typeId < 0) {
            JSONArray types = new JSONArray();
            Iterator<SpecTypePair> specs = p.getSpecTypePairs();
            while (specs.hasNext()) {
                SpecTypePair spec = specs.next();
                int configs = 0;
                Iterator<ResConfig> rcIt = spec.getResConfigs();
                while (rcIt.hasNext()) { rcIt.next(); configs++; }
                int count = 0;
                Iterator<ResourceEntry> resIt = spec.getResources();
                while (resIt.hasNext()) for (Entry en : resIt.next()) if (!en.isNull()) count++;
                types.put(new JSONObject().put("id", spec.getTypeId()).put("name", spec.getTypeName()).put("count", count).put("configs", configs));
            }
            out.put("level", "package").put("types", types).put("dirty", s.dirty);
            return out;
        }
        SpecTypePair spec = p.getSpecTypePair(typeId);
        if (spec == null) throw new IOException("类型不存在");
        out.put("typeId", typeId).put("typeName", spec.getTypeName());
        if (config == null) {
            JSONArray configs = new JSONArray();
            Iterator<ResConfig> rcIt = spec.getResConfigs();
            while (rcIt.hasNext()) {
                ResConfig rc = rcIt.next();
                TypeBlock tb = spec.getTypeBlock(rc);
                int count = tb == null ? 0 : tb.listEntries(true).size();
                configs.put(new JSONObject().put("qualifiers", rc.getQualifiers()).put("label", label(rc)).put("count", count));
            }
            out.put("level", "type").put("configs", configs).put("dirty", s.dirty);
            return out;
        }
        TypeBlock tb = spec.getTypeBlock(config);
        if (tb == null) throw new IOException("配置不存在");
        out.put("config", config).put("configLabel", label(ResConfig.parse(config))).put("level", "config").put("typeName", spec.getTypeName());
        List<Entry> all = tb.listEntries(true);
        int start = Math.max(0, offset), end = Math.min(all.size(), start + (limit <= 0 ? PAGE : Math.min(limit, 500)));
        JSONArray items = new JSONArray();
        for (int i = start; i < end; i++) items.put(entryBrief(s.table, all.get(i), config));
        out.put("entries", items).put("total", all.size()).put("dirty", s.dirty);
        return out;
    }

    synchronized JSONObject entry(String sid, int resId, String config) throws Exception {
        Session s = get(sid);
        ResourceEntry re = s.table.getResource(resId);
        if (re == null) throw new IOException("资源不存在：" + hex(resId));
        Entry e = re.get(config == null ? "" : config);
        if (e == null) e = re.any();
        if (e == null) throw new IOException("该配置下没有值");
        return entryDetail(s.table, re, e);
    }

    synchronized JSONObject edit(String sid, JSONObject a) throws Exception {
        Session s = get(sid);
        String op = a.optString("op");
        if ("setPool".equals(op)) {
            int index = a.getInt("index");
            if (index < 0 || index >= s.table.getStringPool().size()) throw new IOException("字符串索引越界");
            String text = a.getString("text");
            s.table.getStringPool().get(index).set(text);
            s.dirty = true;
            return new JSONObject().put("ok", true).put("dirty", true);
        }
        if ("replacePool".equals(op)) {
            String find = a.getString("find");
            if (find.isEmpty()) throw new IOException("查找文本不能为空");
            String replace = a.optString("replace", "");
            int count = 0;
            for (int i = 0; i < s.table.getStringPool().size(); i++) {
                StringItem item = s.table.getStringPool().get(i);
                String text = item == null || item.get() == null ? "" : item.get();
                if (!text.contains(find)) continue;
                item.set(text.replace(find, replace));
                count++;
            }
            s.dirty = count > 0;
            return new JSONObject().put("ok", true).put("count", count).put("dirty", s.dirty);
        }
        boolean structural = "addEntry".equals(op) || "deleteConfig".equals(op);
        ResourceEntry re = a.has("id") ? s.table.getResource(a.getInt("id")) : null;
        if (re == null && !structural) throw new IOException("资源不存在：" + hex(a.optInt("id", 0)));
        String config = a.optString("config", "");
        if ("rename".equals(op)) {
            String name = a.getString("name").trim();
            if (name.isEmpty()) throw new IOException("资源名不能为空");
            re.setName(name);
        } else if ("addConfig".equals(op) || "importConfig".equals(op)) {
            String from = a.optString("from", config);
            Entry src = re.get(from);
            if (src == null) throw new IOException("来源配置不存在");
            ResConfig rc;
            try { rc = ResConfig.parse(a.getString("qualifiers")); }
            catch (Exception e) { throw new IOException("配置限定符无效：" + e.getMessage()); }
            Iterator<ResConfig> oldConfigs = re.getConfigs();
            while (oldConfigs.hasNext()) if (oldConfigs.next().equals(rc)) throw new IOException("该配置已存在");
            Entry dst = re.getOrCreate(rc);
            dst.merge(src);
        } else if ("deleteEntry".equals(op)) {
            if (a.optBoolean("all", false)) {
                for (Entry e : re) e.setNull(true);
            } else {
                Entry e = re.get(config);
                if (e == null) throw new IOException("该配置下没有值");
                e.setNull(true);
            }
        } else if ("deleteConfig".equals(op)) {
            PackageBlock pkg = re != null ? re.getPackageBlock() : s.table.getPackageBlockById(a.getInt("packageId"));
            if (pkg == null) throw new IOException("包不存在");
            SpecTypePair spec = pkg.getSpecTypePair(a.getInt("typeId"));
            if (spec == null) throw new IOException("类型不存在");
            TypeBlock tb = spec.getTypeBlock(config);
            if (tb == null) throw new IOException("配置不存在");
            tb.clear();
            spec.removeEmptyTypeBlocks();
        } else if ("addEntry".equals(op)) {
            String name = a.getString("name").trim();
            if (name.isEmpty()) throw new IOException("资源名不能为空");
            PackageBlock pkg = re != null ? re.getPackageBlock() : s.table.getPackageBlockById(a.getInt("packageId"));
            if (pkg == null) throw new IOException("包不存在");
            int tid = re != null ? (re.getResourceId() >> 16) & 0xff : a.getInt("typeId");
            String tname = re != null ? re.getType() : a.optString("typeName", "");
            SpecTypePair spec = pkg.getSpecTypePair(tid);
            if (spec == null) {
                if (tname.isEmpty()) throw new IOException("类型不存在");
                spec = pkg.getOrCreateSpecTypePair(tid, tname);
            }
            int entryId = a.optInt("entryId", -1);
            if (entryId < 0) entryId = spec.getHighestEntryId() + 1;
            if (entryId > 0xffff) throw new IOException("条目 ID 超出 65535");
            Entry e = pkg.getOrCreateEntry((byte) tid, (short) entryId, a.optString("config", ""));
            e.ensureComplex(false);
            ResValue rv = e.getResValue();
            if (rv == null) throw new IOException("无法创建资源值");
            applyValue(rv, a.getInt("type"), a.getString("value"), s.table);
            e.setName(name);
        } else if ("setParent".equals(op)) {
            Entry e = re.get(config);
            if (e == null || !e.isComplex()) throw new IOException("只有集合值可以设置父资源");
            ResTableMapEntry me = e.getResTableMapEntry();
            if (me == null) throw new IOException("无法读取集合值头");
            me.setParentId(parseRef(s.table, a.optString("parentId", "0")));
        } else if ("addValue".equals(op)) {
            Entry e = re.get(config);
            if (e == null) throw new IOException("该配置下没有值");
            e.ensureComplex(true);
            ResValueMapArray arr = e.getResValueMapArray();
            if (arr == null) throw new IOException("无法创建集合值");
            ResValueMap map = arr.createNext();
            int nameId = parseId(a.optString("nameId", "0"));
            map.setNameId(nameId);
            applyValue(map, a.getInt("type"), a.getString("value"), s.table);
        } else if ("removeValue".equals(op)) {
            Entry e = re.get(config);
            if (e == null || !e.isComplex()) throw new IOException("不是集合值");
            ResValueMapArray arr = e.getResValueMapArray();
            int index = a.getInt("valueIndex");
            if (index < 0 || index >= arr.size()) throw new IOException("值索引越界");
            arr.remove(index);
        } else {
            Entry e = re.get(config);
            if (e == null) e = re.getOrCreate(config);
            int vi = a.optInt("valueIndex", -1);
            if (vi >= 0) {
                if (!e.isComplex()) throw new IOException("不是集合值");
                ResValueMapArray arr = e.getResValueMapArray();
                if (arr == null || vi >= arr.size()) throw new IOException("值索引越界");
                applyValue(arr.get(vi), a.getInt("type"), a.getString("value"), s.table);
            } else {
                e.ensureComplex(false);
                ResValue rv = e.getResValue();
                if (rv == null) throw new IOException("无法创建资源值");
                applyValue(rv, a.getInt("type"), a.getString("value"), s.table);
            }
        }
        s.dirty = true;
        return new JSONObject().put("ok", true).put("dirty", true);
    }

    synchronized JSONObject search(String sid, String mode, String query, int limit) throws Exception {
        Session s = get(sid);
        JSONArray rows = new JSONArray();
        String q = query == null ? "" : query.trim();
        int max = limit <= 0 ? 200 : Math.min(limit, 500);
        if ("id".equals(mode)) {
            int id = parseId(q);
            ResourceEntry re = s.table.getResource(id);
            if (re != null) {
                Entry any = re.any();
                if (any != null) rows.put(entryDetail(s.table, re, any));
            }
        } else if ("name".equals(mode)) {
            String needle = q.toLowerCase(Locale.ROOT);
            int seen = 0;
            Iterator<ResourceEntry> resIt = s.table.getResources();
            while (resIt.hasNext()) {
                ResourceEntry re = resIt.next();
                String name = re.getName();
                String type = re.getType();
                String pkg = re.getPackageName();
                String hay = (pkg + ":" + type + "/" + name).toLowerCase(Locale.ROOT);
                if (!needle.isEmpty() && !hay.contains(needle)) continue;
                Entry any = re.any();
                JSONObject row = new JSONObject().put("id", re.getResourceId()).put("hex", re.getHexId()).put("name", name == null ? "" : name)
                        .put("type", type).put("package", pkg).put("configs", re.getConfigsCount());
                if (any != null) row.put("config", any.getResConfig().getQualifiers()).put("value", preview(s.table, any));
                rows.put(row);
                if (++seen >= max) break;
            }
        } else {
            String needle = q.toLowerCase(Locale.ROOT);
            int seen = 0;
            boolean stop = false;
            Iterator<ResourceEntry> resIt = s.table.getResources();
            while (resIt.hasNext() && !stop) {
                ResourceEntry re = resIt.next();
                for (Entry e : re) {
                    if (e == null || e.isNull()) continue;
                    String value = preview(s.table, e);
                    if (!value.toLowerCase(Locale.ROOT).contains(needle)) continue;
                    JSONObject row = entryBrief(s.table, e, e.getResConfig().getQualifiers());
                    row.put("config", e.getResConfig().getQualifiers());
                    rows.put(row);
                    if (++seen >= max) { stop = true; break; }
                }
            }
        }
        return new JSONObject().put("rows", rows).put("truncated", rows.length() >= max);
    }

    synchronized JSONObject searchEx(String sid, JSONObject a) throws Exception {
        Session s = get(sid);
        String mode = a.optString("mode", "code");
        String query = a.optString("query", "").trim();
        String path = a.optString("path", "/").trim();
        boolean caseSensitive = a.optBoolean("caseSensitive", false);
        boolean regex = a.optBoolean("regex", false);
        int max = a.optInt("limit", 200);
        if (max <= 0) max = 200;
        max = Math.min(max, 500);
        Pattern pattern = null;
        if (regex) {
            try { pattern = Pattern.compile(query, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE); }
            catch (Exception e) { throw new IOException("正则表达式无效：" + e.getMessage()); }
        }
        String needle = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        String pathNeedle = path == null ? "" : path.replaceAll("^/+", "").toLowerCase(Locale.ROOT);
        long number = 0;
        boolean numberOk = false;
        int color = 0;
        boolean colorOk = false;
        try { number = parseNumber(query); numberOk = true; } catch (IOException ignored) {}
        try { color = parseColor(query, ValueType.COLOR_ARGB8.getByte()); colorOk = true; } catch (IOException ignored) {}
        JSONArray rows = new JSONArray();
        Iterator<ResourceEntry> it = s.table.getResources();
        while (it.hasNext()) {
            ResourceEntry re = it.next();
            String pkg = re.getPackageName() == null ? "" : re.getPackageName();
            String type = re.getType() == null ? "" : re.getType();
            String name = re.getName() == null ? "" : re.getName();
            String identity = (pkg + ":" + type + "/" + name).toLowerCase(Locale.ROOT);
            if (!pathNeedle.isEmpty() && !identity.contains(pathNeedle)) continue;
            for (Entry e : re) {
                if (e == null || e.isNull()) continue;
                String value = preview(s.table, e);
                boolean hit = false;
                if ("id".equals(mode)) {
                    String idText = Long.toString(e.getResourceId() & 0xffffffffL) + " " + hex(e.getResourceId());
                    hit = numberOk ? e.getResourceId() == (int) number : matches(idText, needle, pattern, caseSensitive);
                } else if ("int".equals(mode)) {
                    ResValue rv = e.getResValue();
                    hit = numberOk && rv != null && !e.isComplex() && rv.getData() == (int) number;
                } else if ("color".equals(mode)) {
                    ResValue rv = e.getResValue();
                    hit = colorOk && rv != null && !e.isComplex() && rv.getData() == color;
                } else if ("string".equals(mode)) {
                    hit = matches(value, needle, pattern, caseSensitive);
                } else {
                    hit = matches(name, needle, pattern, caseSensitive)
                            || matches(value, needle, pattern, caseSensitive)
                            || matches(hex(e.getResourceId()), needle, pattern, caseSensitive)
                            || matches(identity, needle, pattern, caseSensitive);
                }
                if (!hit) continue;
                JSONObject row = entryBrief(s.table, e, e.getResConfig().getQualifiers());
                row.put("config", e.getResConfig().getQualifiers());
                row.put("package", pkg).put("type", type);
                rows.put(row);
                if (rows.length() >= max) return new JSONObject().put("rows", rows).put("truncated", true);
            }
        }
        return new JSONObject().put("rows", rows).put("truncated", false);
    }

    private static boolean matches(String text, String needle, Pattern pattern, boolean caseSensitive) {
        String value = text == null ? "" : text;
        if (pattern != null) return pattern.matcher(value).find();
        if (needle.isEmpty()) return true;
        return (caseSensitive ? value : value.toLowerCase(Locale.ROOT)).contains(needle);
    }

    private static long parseNumber(String s) throws IOException {
        String t = s == null ? "" : s.trim().replace("_", "").replace(" ", "");
        if (t.isEmpty()) throw new IOException("缺少数字");
        boolean neg = t.startsWith("-");
        if (neg || t.startsWith("+")) t = t.substring(1);
        try {
            long v;
            if (t.startsWith("0b") || t.startsWith("0B")) v = Long.parseLong(t.substring(2), 2);
            else if (t.startsWith("0x") || t.startsWith("0X")) v = Long.parseLong(t.substring(2), 16);
            else if (t.startsWith("#")) v = Long.parseLong(t.substring(1), 16);
            else if (t.length() > 1 && t.startsWith("0")) v = Long.parseLong(t.substring(1), 8);
            else v = Long.parseLong(t);
            return neg ? -v : v;
        } catch (NumberFormatException e) { throw new IOException("数字格式无效：" + s); }
    }

    synchronized JSONObject backup(String sid) throws Exception {
        Session s = get(sid);
        File src = s.archive.isEmpty() ? s.file : new File(s.archive);
        File dst = new File(src.getPath() + ".bak");
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return new JSONObject().put("path", dst.getPath()).put("size", dst.length());
    }

    synchronized JSONObject translate(String sid, String config, int offset, int limit, String query) throws Exception {
        Session s = get(sid);
        String target = config == null ? "" : config;
        String needle = query == null ? "" : query.trim();
        String lower = needle.toLowerCase(Locale.ROOT);
        int max = limit <= 0 ? 100 : Math.min(limit, 300);
        int start = Math.max(0, offset);
        JSONArray configs = stringConfigs(s.table);
        JSONArray rows = new JSONArray();
        int matched = 0;
        for (PackageBlock p : s.table) {
            Iterator<SpecTypePair> specs = p.getSpecTypePairs();
            while (specs.hasNext()) {
                SpecTypePair spec = specs.next();
                if (!"string".equalsIgnoreCase(spec.getTypeName())) continue;
                Iterator<ResourceEntry> it = spec.getResources();
                while (it.hasNext()) {
                    ResourceEntry re = it.next();
                    Entry src = re.get("");
                    if (src == null) src = re.any();
                    Entry dst = target.isEmpty() ? null : re.get(target);
                    String source = src == null ? "" : preview(s.table, src);
                    String translated = dst == null ? "" : preview(s.table, dst);
                    String name = re.getName() == null ? "" : re.getName();
                    if (!lower.isEmpty()) {
                        String hay = (name + "\n" + source + "\n" + translated).toLowerCase(Locale.ROOT);
                        if (!hay.contains(lower)) continue;
                    }
                    matched++;
                    if (matched <= start) continue;
                    if (rows.length() >= max) continue;
                    JSONObject row = new JSONObject().put("id", re.getResourceId()).put("hex", re.getHexId())
                            .put("entryId", re.getResourceId() & 0xffff).put("name", name)
                            .put("package", re.getPackageName() == null ? "" : re.getPackageName())
                            .put("source", source).put("target", translated).put("hasTarget", dst != null)
                            .put("config", target);
                    rows.put(row);
                }
            }
        }
        return new JSONObject().put("rows", rows).put("total", matched).put("offset", start)
                .put("pageSize", max).put("config", target).put("configs", configs);
    }

    private static JSONArray stringConfigs(TableBlock table) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        for (PackageBlock p : table) {
            Iterator<SpecTypePair> specs = p.getSpecTypePairs();
            while (specs.hasNext()) {
                SpecTypePair spec = specs.next();
                if (!"string".equalsIgnoreCase(spec.getTypeName())) continue;
                Iterator<ResConfig> it = spec.getResConfigs();
                while (it.hasNext()) {
                    ResConfig rc = it.next();
                    String q = rc.getQualifiers();
                    if (!labels.containsKey(q == null ? "" : q)) labels.put(q == null ? "" : q, label(rc));
                }
            }
        }
        JSONArray out = new JSONArray();
        for (Map.Entry<String, String> en : labels.entrySet())
            out.put(new JSONObject().put("qualifiers", en.getKey()).put("label", en.getValue()));
        return out;
    }

    synchronized JSONObject translateAdd(String sid, String qualifiers) throws Exception {
        Session s = get(sid);
        ResConfig rc;
        try { rc = ResConfig.parse(qualifiers == null ? "" : qualifiers.trim()); }
        catch (Exception e) { throw new IOException("配置限定符无效：" + qualifiers); }
        String q = rc.getQualifiers();
        if (q == null || q.isEmpty()) throw new IOException("不能新建默认配置");
        int copied = 0;
        for (PackageBlock p : s.table) {
            Iterator<SpecTypePair> specs = p.getSpecTypePairs();
            while (specs.hasNext()) {
                SpecTypePair spec = specs.next();
                if (!"string".equalsIgnoreCase(spec.getTypeName())) continue;
                Iterator<ResourceEntry> it = spec.getResources();
                while (it.hasNext()) {
                    ResourceEntry re = it.next();
                    if (re.get(q) != null) continue;
                    Entry src = re.any();
                    if (src == null || src.isNull()) continue;
                    Entry dst = re.getOrCreate(rc);
                    if (dst == null) continue;
                    dst.merge(src);
                    copied++;
                }
            }
        }
        s.dirty = true;
        return new JSONObject().put("qualifiers", q).put("label", label(rc)).put("count", copied);
    }

    synchronized JSONObject query(String sid, String rawQuery) throws Exception {
        Session s = get(sid);
        String raw = rawQuery == null ? "" : rawQuery.trim();
        if (raw.isEmpty()) throw new IOException("请输入资源名、资源 ID、颜色或数字");
        if (raw.startsWith("#")) return colorResult(raw);
        if (raw.startsWith("@") || raw.startsWith("?")) return resourceResult(s.table, raw);
        try {
            long number = parseNumber(raw);
            JSONObject numberJson = numberResult(number);
            if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                ResourceEntry re = s.table.getResource((int) number);
                if (re != null) return resourceResult(s.table, re).put("number", numberJson);
            }
            return numberJson;
        } catch (IOException ignored) {
            return resourceResult(s.table, raw);
        }
    }

    private static JSONObject resourceResult(TableBlock table, String raw) throws Exception {
        boolean attr = raw.startsWith("?");
        String t = raw;
        while (!t.isEmpty() && (t.charAt(0) == '@' || t.charAt(0) == '?' || t.charAt(0) == '+')) t = t.substring(1);
        String pkg = null, rest = t;
        int colon = t.indexOf(':');
        if (colon > 0) { pkg = t.substring(0, colon); rest = t.substring(colon + 1); }
        int slash = rest.indexOf('/');
        String type, name;
        if (slash > 0) { type = rest.substring(0, slash); name = rest.substring(slash + 1); }
        else if (attr) { type = "attr"; name = rest; }
        else throw new IOException("资源名应为 @类型/名称、?attr/名称 或 @包:类型/名称");
        if (type.isEmpty() || name.isEmpty()) throw new IOException("资源名不完整");
        ResourceEntry best = null;
        Iterator<ResourceEntry> it = table.getResources();
        while (it.hasNext()) {
            ResourceEntry re = it.next();
            if (!type.equals(re.getType()) || !name.equals(re.getName())) continue;
            if (pkg != null && !pkg.isEmpty() && !pkg.equals(re.getPackageName())) continue;
            if (best == null || re.getResourceId() < best.getResourceId()) best = re;
        }
        if (best == null) throw new IOException("找不到资源：" + raw);
        return resourceResult(table, best);
    }

    private static JSONObject resourceResult(TableBlock table, ResourceEntry re) throws Exception {
        Entry any = re.any();
        JSONObject out = new JSONObject().put("kind", "resource");
        if (any != null) out.put("resource", entryDetail(table, re, any));
        out.put("id", re.getResourceId()).put("hex", re.getHexId()).put("name", re.getName() == null ? "" : re.getName())
                .put("type", re.getType() == null ? "" : re.getType()).put("package", re.getPackageName() == null ? "" : re.getPackageName())
                .put("configs", re.getConfigsCount());
        JSONArray values = new JSONArray();
        for (Entry e : re) {
            if (e == null || e.isNull()) continue;
            values.put(new JSONObject().put("config", e.getResConfig().getQualifiers()).put("label", label(e.getResConfig()))
                    .put("value", preview(table, e)).put("type", e.getTypeName()).put("complex", e.isComplex()));
        }
        out.put("values", values);
        return out;
    }

    private static JSONObject colorResult(String raw) throws Exception {
        int color = parseColor(raw, ValueType.COLOR_ARGB8.getByte());
        int a = (color >>> 24) & 0xff, r = (color >>> 16) & 0xff, g = (color >>> 8) & 0xff, b = color & 0xff;
        return new JSONObject().put("kind", "color").put("value", color).put("hex", hex(color))
                .put("argb", String.format(Locale.ROOT, "#%02X%02X%02X%02X", a, r, g, b))
                .put("rgba", String.format(Locale.ROOT, "rgba(%d,%d,%d,%.2f)", r, g, b, a / 255.0))
                .put("alpha", a).put("red", r).put("green", g).put("blue", b);
    }

    private static JSONObject numberResult(long v) throws Exception {
        return new JSONObject().put("kind", "number").put("value", v)
                .put("dec", Long.toString(v)).put("hex", "0x" + Long.toHexString(v))
                .put("oct", "0" + Long.toOctalString(v)).put("bin", "0b" + Long.toBinaryString(v))
                .put("signed32", Integer.toString((int) v)).put("unsigned32", Long.toString(v & 0xffffffffL));
    }

    synchronized JSONObject pool(String sid, int offset, int limit, String query) throws Exception {
        Session s = get(sid);
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT);
        int start = Math.max(0, offset), max = limit <= 0 ? PAGE : Math.min(limit, 500);
        int total = s.table.getStringPool().size();
        JSONArray rows = new JSONArray();
        int matched = 0, emitted = 0;
        for (int i = 0; i < total; i++) {
            StringItem item = s.table.getStringPool().get(i);
            String text = item == null || item.get() == null ? "" : item.get();
            if (!needle.isEmpty() && !text.toLowerCase(Locale.ROOT).contains(needle)) continue;
            matched++;
            if (matched <= start) continue;
            if (emitted++ >= max) break;
            rows.put(new JSONObject().put("index", i).put("text", text));
        }
        return new JSONObject().put("level", "pool").put("rows", rows).put("total", matched).put("offset", start).put("pageSize", max);
    }

    synchronized JSONObject save(String sid) throws Exception {
        Session s = get(sid);
        s.table.refreshFull();
        byte[] bytes = s.table.getBytes();
        String path;
        boolean signing = false;
        if (!s.archive.isEmpty()) {
            File out = owner.archiveSave(s.archive, s.entry, bytes);
            path = out.getPath();
            signing = out.getName().endsWith(".apk");
        } else {
            FileOps.writeAtomic(s.file, bytes, true);
            path = s.file.getPath();
        }
        s.dirty = false;
        return new JSONObject().put("path", path).put("size", bytes.length).put("needsSigning", signing);
    }

    synchronized void close(String sid) { sessions.remove(sid); }

    private Session get(String sid) throws IOException {
        Session s = sessions.get(sid);
        if (s == null) throw new IOException("资源表会话已过期，请重新打开");
        return s;
    }

    private static void applyValue(ValueItem v, int type, String text, TableBlock table) throws Exception {
        String value = text == null ? "" : text;
        switch (type) {
            case 0: v.setValueType(ValueType.NULL); v.setData(0); break;
            case 3: v.setValueAsString(value); break;
            case 1: case 2: case 7: case 8:
                v.setValueType(ValueType.valueOf(type)); v.setData(parseRef(table, value)); break;
            case 16: v.setValueType(ValueType.DEC); v.setData((int) parseLong(value)); break;
            case 17: v.setValueType(ValueType.HEX); v.setData((int) parseLong(value)); break;
            case 18:
                v.setValueType(ValueType.BOOLEAN);
                v.setData(value.equalsIgnoreCase("true") || value.equals("1") ? 1 : 0);
                break;
            case 4: v.setValueType(ValueType.FLOAT); v.setData(Float.floatToIntBits(Float.parseFloat(value.trim()))); break;
            case 5: case 6:
                v.setValueType(ValueType.valueOf(type));
                v.setData((int) parseLong(value.trim()));
                break;
            case 28: case 29: case 30: case 31:
                v.setValueType(ValueType.valueOf(type));
                v.setData(parseColor(value, type));
                break;
            default: throw new IOException("不支持的值类型：" + type);
        }
    }

    private static long parseLong(String s) throws IOException {
        String t = s.trim();
        try {
            if (t.startsWith("0x") || t.startsWith("0X")) return Long.parseLong(t.substring(2), 16);
            if (t.startsWith("#")) return Long.parseLong(t.substring(1), 16);
            return Long.parseLong(t);
        } catch (NumberFormatException e) { throw new IOException("数字格式无效：" + s); }
    }

    static int parseId(String s) throws IOException {
        String t = s.trim();
        if (t.startsWith("@")) t = t.substring(1);
        if (t.startsWith("?")) t = t.substring(1);
        if (t.startsWith("+")) t = t.substring(1);
        if (t.isEmpty()) return 0;
        try {
            if (t.startsWith("0x") || t.startsWith("0X")) return (int) Long.parseLong(t.substring(2), 16);
            if (t.startsWith("#")) return (int) Long.parseLong(t.substring(1), 16);
            if (t.matches("[0-9]+")) return (int) Long.parseLong(t);
        } catch (NumberFormatException e) { throw new IOException("资源 ID 无效：" + s); }
        throw new IOException("资源 ID 应为 0x7f010001 或十进制数字");
    }

    private static int parseColor(String s, int type) throws IOException {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        long c;
        try { c = Long.parseLong(t, 16); }
        catch (NumberFormatException e) { throw new IOException("颜色格式无效：" + s); }
        if (t.length() == 8) return (int) c;
        if (t.length() == 6) return (int) (c | 0xff000000L);
        if (t.length() == 4) {
            long a = (c >> 12) & 0xf, r = (c >> 8) & 0xf, g = (c >> 4) & 0xf, b = c & 0xf;
            return (int) ((a * 17) << 24 | (r * 17) << 16 | (g * 17) << 8 | (b * 17));
        }
        if (t.length() == 3) {
            long r = (c >> 8) & 0xf, g = (c >> 4) & 0xf, b = c & 0xf;
            return (int) (0xff000000L | (r * 17) << 16 | (g * 17) << 8 | (b * 17));
        }
        throw new IOException("颜色应为 #RGB、#ARGB、#RRGGBB 或 #AARRGGBB");
    }

    private static int parseRef(TableBlock table, String value) throws IOException {
        String t = value == null ? "" : value.trim();
        while (!t.isEmpty() && (t.charAt(0) == '@' || t.charAt(0) == '?' || t.charAt(0) == '+')) t = t.substring(1);
        try { return parseId(t); } catch (IOException ignored) {}
        String pkg = null, rest = t;
        int colon = t.indexOf(':');
        if (colon > 0) { pkg = t.substring(0, colon); rest = t.substring(colon + 1); }
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) throw new IOException("资源引用应为 0x7f010001 或 [包:]类型/名称");
        String type = rest.substring(0, slash), name = rest.substring(slash + 1);
        int best = 0;
        Iterator<ResourceEntry> it = table.getResources();
        while (it.hasNext()) {
            ResourceEntry re = it.next();
            if (type.equals(re.getType()) && name.equals(re.getName())
                    && (pkg == null || pkg.isEmpty() || pkg.equals(re.getPackageName()))) {
                int id = re.getResourceId();
                if (best == 0 || id < best) best = id;
            }
        }
        if (best != 0) return best;
        throw new IOException("找不到资源：" + value);
    }

    private static JSONObject entryBrief(TableBlock table, Entry e, String config) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", e.getResourceId()).put("hex", hex(e.getResourceId()));
        o.put("entryId", e.getId()).put("name", e.getName() == null ? "" : e.getName());
        o.put("config", config == null ? "" : config).put("complex", e.isComplex()).put("type", e.getTypeName());
        o.put("value", preview(table, e));
        return o;
    }

    private static JSONObject entryDetail(TableBlock table, ResourceEntry re, Entry e) throws Exception {
        JSONObject o = entryBrief(table, e, e.getResConfig().getQualifiers());
        o.put("package", re.getPackageName()).put("type", re.getType()).put("resourceId", re.getResourceId());
        o.put("configLabel", label(e.getResConfig())).put("null", e.isNull()).put("public", e.getSpecFlag() != null && e.getSpecFlag().isPublic());
        if (e.isComplex()) {
            ResTableMapEntry me = e.getResTableMapEntry();
            if (me != null) o.put("parentId", me.getParentId()).put("parent", refName(table, me.getParentId()));
            JSONArray values = new JSONArray();
            ResValueMapArray arr = e.getResValueMapArray();
            if (arr != null) for (int i = 0; i < arr.size(); i++) {
                ResValueMap m = arr.get(i);
                values.put(new JSONObject().put("index", i).put("nameId", m.getNameId()).put("name", m.decodeName(true))
                        .put("type", m.getType() & 0xff).put("typeName", typeName(m.getType() & 0xff))
                        .put("data", m.getData()).put("value", display(table, m, m.getType() & 0xff, m.getData())));
            }
            o.put("values", values);
        } else {
            ResValue rv = e.getResValue();
            if (rv != null) {
                o.put("type", rv.getType() & 0xff).put("typeName", typeName(rv.getType() & 0xff));
                o.put("data", rv.getData()).put("value", display(table, rv, rv.getType() & 0xff, rv.getData()));
            }
        }
        return o;
    }

    private static String preview(TableBlock table, Entry e) {
        try {
            if (e.isComplex()) {
                ResValueMapArray a = e.getResValueMapArray();
                return "集合值（" + (a == null ? 0 : a.size()) + " 项）";
            }
            ResValue rv = e.getResValue();
            if (rv == null) return "";
            String s = display(table, rv, rv.getType() & 0xff, rv.getData());
            return s.length() > 160 ? s.substring(0, 160) + "…" : s;
        } catch (Exception ex) { return ""; }
    }

    private static String display(TableBlock table, ValueItem v, int type, int data) {
        try {
            switch (type) {
                case 0: return "";
                case 1: case 7: case 8: {
                    String p = type == 7 ? "@" : type == 8 ? "?" : type == 2 ? "?" : "@";
                    String n = refName(table, data);
                    return p + hex(data) + (n.isEmpty() ? "" : "  " + n);
                }
                case 2: { String n = refName(table, data); return "?" + hex(data) + (n.isEmpty() ? "" : "  " + n); }
                case 3: return v.getValueAsString();
                case 4: return String.valueOf(Float.intBitsToFloat(data));
                case 5: return complexToFloat(data) + unit(data);
                case 6: return (complexToFloat(data) * 100f) + ((data & 0xf) == 1 ? "%p" : "%");
                case 16: return String.valueOf(data);
                case 17: return "0x" + Integer.toHexString(data);
                case 18: return data != 0 ? "true" : "false";
                case 28: case 29: case 30: case 31: return String.format("#%08X", data);
                default: return "0x" + Integer.toHexString(data);
            }
        } catch (Exception e) { return "0x" + Integer.toHexString(data); }
    }

    private static final float MANTISSA_MULT = 1.0f / (1 << 8);
    private static final float[] RADIX_MULTS = {1.0f * MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT, 1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT};
    private static float complexToFloat(int complex) {
        return (complex & 0xffffff00) * RADIX_MULTS[(complex >> 4) & 3];
    }

    private static String unit(int data) {
        switch (data & 0xf) {
            case 1: return "dp"; case 2: return "sp"; case 3: return "pt"; case 4: return "in"; case 5: return "mm";
            default: return "px";
        }
    }

    private static String refName(TableBlock table, int id) {
        if (id == 0) return "";
        try { ResourceEntry re = table.getResource(id); return re == null ? "" : re.getPackageName() + ":" + re.getType() + "/" + re.getName(); }
        catch (Exception e) { return ""; }
    }

    private static String typeName(int type) {
        switch (type) {
            case 0: return "null"; case 1: return "reference"; case 2: return "attribute"; case 3: return "string";
            case 4: return "float"; case 5: return "dimension"; case 6: return "fraction"; case 7: return "dynamic reference";
            case 8: return "dynamic attribute"; case 16: return "int"; case 17: return "hex"; case 18: return "boolean";
            case 28: return "color ARGB8"; case 29: return "color RGB8"; case 30: return "color ARGB4"; case 31: return "color RGB4";
            default: return "0x" + Integer.toHexString(type);
        }
    }

    private static String label(ResConfig rc) {
        String q = rc.getQualifiers();
        if (q == null || q.isEmpty()) return "默认";
        return q.startsWith("-") ? q.substring(1) : q;
    }

    private static int countEntries(ResourceType t) {
        int n = 0;
        for (ResourceEntry re : t) for (Entry e : re) if (!e.isNull()) n++;
        return n;
    }

    static String hex(int id) { return String.format("0x%08x", id); }
}
