package com.cyberhub.manager;

import com.reandroid.apk.ApkModule;
import com.reandroid.apk.ResFile;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResConfig;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.value.ValueType;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** APK 资源表操作：精简、资源名/文件名混淆、反混淆、重复资源复用。 */
public final class ResTools {
    private ResTools() {}

    private static final Pattern ID_REF = Pattern.compile("@0x([0-9a-fA-F]{8})");
    private static final Pattern NAME_REF = Pattern.compile("@([a-zA-Z0-9_]+):([a-zA-Z0-9_]+)/([a-zA-Z0-9_$.]+)|@([a-zA-Z0-9_]+)/([a-zA-Z0-9_$.]+)");

    // ---------------------------------------------------------------- slim

    public static JSONObject slim(File source, File destination, JSONObject opts) throws Exception {
        ApkModule module = ApkModule.loadApkFile(source);
        try {
            if (!module.hasTableBlock()) throw new Exception("APK 没有 resources.arsc");
            TableBlock table = module.getTableBlock();
            boolean unused = opts.optBoolean("unused", true);
            Set<String> locales = split(opts.optString("locales", ""));
            Set<String> types = split(opts.optString("types", ""));
            String nameRule = opts.optString("names", "").trim();
            Pattern names = nameRule.isEmpty() ? null : Pattern.compile(nameRule);

            Set<Integer> kept = null;
            boolean allKeep = false;
            if (unused) {
                Scan scan = collectUsed(module, table);
                kept = scan.ids;
                allKeep = scan.allKeep;
            }

            int removedEntries = 0, removedConfigs = 0;
            for (PackageBlock pkg : table.listPackages()) {
                for (Iterator<ResourceEntry> it = pkg.getResources(); it.hasNext(); ) {
                    ResourceEntry resource = it.next();
                    String type = resource.getType();
                    String name = resource.getName();
                    boolean dropAll = (kept != null && !allKeep && !kept.contains(resource.getResourceId()))
                            || types.contains(type)
                            || (names != null && names.matcher(name).matches());
                    if (dropAll) {
                        for (Entry entry : resource) if (entry != null && !entry.isNull()) entry.setNull(true);
                        removedEntries++;
                        continue;
                    }
                    if (locales.isEmpty()) continue;
                    for (Iterator<ResConfig> cit = resource.getConfigs(); cit.hasNext(); ) {
                        ResConfig config = cit.next();
                        String qualifiers = config.getQualifiers();
                        for (String locale : locales) {
                            if (qualifiers.equals(locale) || qualifiers.startsWith(locale + "-")) {
                                Entry entry = resource.get(config);
                                if (entry != null && !entry.isNull()) { entry.setNull(true); removedConfigs++; }
                                break;
                            }
                        }
                    }
                }
            }
            int removedFiles = 0;
            for (ResFile resFile : module.listResFiles()) {
                boolean any = false;
                for (Entry entry : resFile) {
                    if (entry != null && !entry.isNull()) { any = true; break; }
                }
                if (!any) {
                    module.getZipEntryMap().remove(resFile.getInputSource().getName());
                    removedFiles++;
                }
            }
            try {
                table.refreshFull();
                for (PackageBlock pkg : table.listPackages()) pkg.removeUnusedSpecs();
                module.refreshTable();
            } catch (Exception ignored) {
            }
            File temp = File.createTempFile(".res-slim-", ".apk", destination.getAbsoluteFile().getParentFile());
            try {
                module.writeApk(temp);
                FileOps.replace(temp, destination);
            } finally {
                temp.delete();
            }
            if (removedEntries == 0 && removedConfigs == 0 && removedFiles == 0)
                throw new Exception("没有可精简的资源");
            return new JSONObject().put("entries", removedEntries).put("configs", removedConfigs)
                    .put("files", removedFiles).put("allKept", allKeep);
        } finally {
            module.close();
        }
    }

    private static final class Scan {
        final Set<Integer> ids = new HashSet<>();
        boolean allKeep;
    }

    private static Scan collectUsed(ApkModule module, TableBlock table) throws Exception {
        Scan scan = new Scan();
        Map<Integer, ResourceEntry> byId = new LinkedHashMap<>();
        Map<String, ResourceEntry> byName = new HashMap<>();
        for (PackageBlock pkg : table.listPackages()) {
            for (Iterator<ResourceEntry> it = pkg.getResources(); it.hasNext(); ) {
                ResourceEntry resource = it.next();
                byId.put(resource.getResourceId(), resource);
                byName.put(resource.getType() + ":" + resource.getName(), resource);
            }
        }
        // 清单里的图标/标题/主题等根引用
        if (module.hasAndroidManifest()) {
            com.reandroid.arsc.chunk.xml.AndroidManifestBlock manifest = module.getAndroidManifest();
            try {
                int icon = manifest.getIconResourceId();
                if (icon != 0) scan.ids.add(icon);
                int round = manifest.getRoundIconResourceId();
                if (round != 0) scan.ids.add(round);
                Integer label = manifest.getApplicationLabelReference();
                if (label != null) scan.ids.add(label);
            } catch (Exception ignored) {
            }
        }
        // AXML 中的 @0x... 与 @type/name
        for (InputSource input : module.getInputSources()) {
            String name = input.getName();
            if (!name.equals("AndroidManifest.xml") && (!name.endsWith(".xml") || name.startsWith("assets/") || name.startsWith("META-INF/"))) continue;
            try (InputStream in = input.openStream()) {
                byte[] bytes = FileOps.bytes(in, 64L * 1024 * 1024);
                if (!name.equals("AndroidManifest.xml") && !Axml.isBinary(bytes)) continue;
                String xml = new Axml().decode(bytes);
                Matcher ids = ID_REF.matcher(xml);
                while (ids.find()) scan.ids.add((int) Long.parseLong(ids.group(1), 16));
                Matcher refs = NAME_REF.matcher(xml);
                while (refs.find()) {
                    String type = refs.group(1) != null ? refs.group(2) : refs.group(4);
                    String resName = refs.group(1) != null ? refs.group(3) : refs.group(5);
                    ResourceEntry resource = byName.get(type + ":" + resName);
                    if (resource != null) scan.ids.add(resource.getResourceId());
                }
            } catch (Exception ignored) {
            }
        }
        // DEX 常量与 getIdentifier
        for (InputSource input : module.getInputSources()) {
            if (!input.getName().matches("classes[0-9]*\\.dex")) continue;
            byte[] bytes;
            try (InputStream in = input.openStream()) { bytes = FileOps.bytes(in, 128L * 1024 * 1024); }
            DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(version(bytes)), bytes);
            for (ClassDef classDef : dex.getClasses()) {
                for (Method method : classDef.getMethods()) {
                    MethodImplementation impl = method.getImplementation();
                    if (impl == null) continue;
                    for (Instruction instruction : impl.getInstructions()) {
                        if (instruction instanceof NarrowLiteralInstruction) {
                            int value = ((NarrowLiteralInstruction) instruction).getNarrowLiteral();
                            if (isResourceId(value)) scan.ids.add(value);
                        } else if (instruction instanceof WideLiteralInstruction) {
                            long value = ((WideLiteralInstruction) instruction).getWideLiteral();
                            if (value >= 0x7f000000L && value <= 0x7fffffffL) scan.ids.add((int) value);
                        }
                        if (instruction instanceof ReferenceInstruction) {
                            Reference reference = ((ReferenceInstruction) instruction).getReference();
                            if (reference instanceof MethodReference) {
                                String methodName = ((MethodReference) reference).getName();
                                if (methodName.equals("getIdentifier") || methodName.equals("getResourceEntryName")
                                        || methodName.equals("getResourceName")) scan.allKeep = true;
                            } else if (reference instanceof StringReference) {
                                String value = ((StringReference) reference).getString();
                                if (value.equals("getIdentifier") || value.contains("/res/")) scan.allKeep = true;
                            }
                        }
                    }
                }
            }
        }
        if (scan.allKeep) return scan;
        // BFS 沿资源引用闭包
        Deque<Integer> queue = new ArrayDeque<>(scan.ids);
        Set<Integer> used = new HashSet<>();
        while (!queue.isEmpty()) {
            int id = queue.poll();
            if (!used.add(id)) continue;
            ResourceEntry resource = byId.get(id);
            if (resource == null) continue;
            for (Entry entry : resource) {
                if (entry == null || entry.isNull()) continue;
                try {
                    if (entry.isComplex()) {
                        ResValueMapArray maps = entry.getResValueMapArray();
                        if (maps == null) continue;
                        for (int i = 0; i < maps.size(); i++) {
                            ResValueMap item = maps.get(i);
                            if (item.getValueType().isReference()) queue.add(item.getData());
                            else if (item.getValueType() == ValueType.STRING) {
                                String key = nameKey(item.getValueAsString());
                                if (key != null && byName.containsKey(key)) queue.add(byName.get(key).getResourceId());
                            }
                        }
                    } else if (entry.getValueType().isReference()) {
                        queue.add(entry.getResValue().getData());
                    } else if (entry.getValueType() == ValueType.STRING) {
                        String key = nameKey(entry.getValueAsString());
                        if (key != null && byName.containsKey(key)) queue.add(byName.get(key).getResourceId());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        scan.ids.clear();
        scan.ids.addAll(used);
        return scan;
    }

    private static String nameKey(String value) {
        if (value == null) return null;
        Matcher matcher = NAME_REF.matcher(value);
        if (!matcher.find()) return null;
        String type = matcher.group(1) != null ? matcher.group(2) : matcher.group(4);
        String name = matcher.group(1) != null ? matcher.group(3) : matcher.group(5);
        return type + ":" + name;
    }

    // ----------------------------------------------------------- obfuscate

    public static JSONObject obfuscate(File source, File destination, JSONObject opts) throws Exception {
        ApkModule module = ApkModule.loadApkFile(source);
        try {
            if (!module.hasTableBlock()) throw new Exception("APK 没有 resources.arsc");
            TableBlock table = module.getTableBlock();
            boolean force = opts.optBoolean("force", false);
            if (!force && dynamicNameLookup(module)) throw new Exception("DEX 使用 getIdentifier/getResourceName 动态查找资源；请勾选“强制混淆”。");
            Map<String, Integer> counters = new HashMap<>();
            Map<String, Set<String>> used = new HashMap<>();
            int renamed = 0;
            for (PackageBlock pkg : table.listPackages()) {
                for (Iterator<ResourceEntry> it = pkg.getResources(); it.hasNext(); ) {
                    ResourceEntry resource = it.next();
                    String type = resource.getType();
                    Set<String> names = used.computeIfAbsent(type, key -> new HashSet<>());
                    String next;
                    do {
                        int index = counters.getOrDefault(type, 0);
                        counters.put(type, index + 1);
                        next = shortName(index);
                    } while (!names.add(next));
                    resource.setName(next);
                    renamed++;
                }
            }
            int files = renameResourceFiles(module, true);
            try {
                table.refreshFull();
                module.refreshTable();
            } catch (Exception ignored) {
            }
            File temp = File.createTempFile(".res-obf-", ".apk", destination.getAbsoluteFile().getParentFile());
            try {
                module.writeApk(temp);
                FileOps.replace(temp, destination);
            } finally {
                temp.delete();
            }
            return new JSONObject().put("entries", renamed).put("files", files);
        } finally {
            module.close();
        }
    }

    // --------------------------------------------------------- deobfuscate

    public static JSONObject deobfuscate(File source, File destination, JSONObject opts) throws Exception {
        ApkModule module = ApkModule.loadApkFile(source);
        try {
            if (!module.hasTableBlock()) throw new Exception("APK 没有 resources.arsc");
            TableBlock table = module.getTableBlock();
            Map<String, Set<String>> used = new HashMap<>();
            int renamed = 0;
            for (PackageBlock pkg : table.listPackages()) {
                for (Iterator<ResourceEntry> it = pkg.getResources(); it.hasNext(); ) {
                    ResourceEntry resource = it.next();
                    String type = resource.getType();
                    String current = resource.getName();
                    Set<String> names = used.computeIfAbsent(type, key -> new HashSet<>());
                    if (!isObfuscated(current)) { names.add(current); continue; }
                    String base = type + "_" + Integer.toHexString(resource.getResourceId());
                    String next = base;
                    int suffix = 2;
                    while (!names.add(next)) next = base + "_" + (suffix++);
                    resource.setName(next);
                    renamed++;
                }
            }
            int files = renameResourceFiles(module, false);
            try {
                table.refreshFull();
                module.refreshTable();
            } catch (Exception ignored) {
            }
            File temp = File.createTempFile(".res-deobf-", ".apk", destination.getAbsoluteFile().getParentFile());
            try {
                module.writeApk(temp);
                FileOps.replace(temp, destination);
            } finally {
                temp.delete();
            }
            return new JSONObject().put("entries", renamed).put("files", files);
        } finally {
            module.close();
        }
    }

    private static int renameResourceFiles(ApkModule module, boolean obfuscate) throws Exception {
        int renamed = 0;
        Set<String> seen = new HashSet<>();
        for (ResFile resFile : module.listResFiles()) {
            if (resFile.getFilePath() != null) seen.add(resFile.getFilePath());
        }
        for (ResFile resFile : module.listResFiles()) {
            String old = resFile.getFilePath();
            if (old == null || old.isEmpty()) continue;
            int slash = old.lastIndexOf('/');
            String dir = slash < 0 ? "" : old.substring(0, slash + 1);
            int dot = old.lastIndexOf('.');
            String ext = dot > slash ? old.substring(dot) : "";
            Entry first = resFile.getFirst();
            if (first == null) continue;
            ResourceEntry resource = first.getResourceEntry();
            String base = resource == null ? resFile.getEntryNameFromPath() : resource.getName();
            if (base == null || base.isEmpty()) continue;
            if (!obfuscate && isObfuscated(base)) continue;
            String next = dir + base + ext;
            if (next.equals(old)) { seen.add(next); renamed += 0; continue; }
            String candidate = next;
            int suffix = 2;
            while (seen.contains(candidate)) {
                String stem = dot > slash ? base + "_" + suffix + ext : base + "_" + suffix;
                candidate = dir + stem;
                suffix++;
            }
            seen.add(candidate);
            resFile.setFilePath(candidate);
            renamed++;
        }
        return renamed;
    }

    // -------------------------------------------------------------- dedupe

    public static JSONObject dedupe(File source, File destination) throws Exception {
        ApkModule module = ApkModule.loadApkFile(source);
        try {
            if (!module.hasTableBlock()) throw new Exception("APK 没有 resources.arsc");
            Map<String, ResFile> canonical = new HashMap<>();
            int removed = 0;
            long saved = 0;
            for (ResFile resFile : module.listResFiles()) {
                byte[] hash;
                try (InputStream in = resFile.getInputSource().openStream()) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[32768];
                    int n;
                    while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
                    hash = digest.digest();
                } catch (Exception ignored) {
                    continue;
                }
                String key = hex(hash);
                ResFile first = canonical.get(key);
                if (first == null) { canonical.put(key, resFile); continue; }
                String target = first.getFilePath();
                if (target == null || target.isEmpty()) continue;
                for (Entry entry : resFile) {
                    if (entry != null && !entry.isNull()) {
                        try { entry.setValueAsString(target); } catch (Exception ignored) { }
                    }
                }
                saved += resFile.getInputSource().getLength();
                module.getZipEntryMap().remove(resFile.getInputSource().getName());
                removed++;
            }
            if (removed == 0) throw new Exception("没有发现重复的资源文件");
            try {
                module.getTableBlock().refreshFull();
                module.refreshTable();
            } catch (Exception ignored) {
            }
            File temp = File.createTempFile(".res-dedupe-", ".apk", destination.getAbsoluteFile().getParentFile());
            try {
                module.writeApk(temp);
                FileOps.replace(temp, destination);
            } finally {
                temp.delete();
            }
            return new JSONObject().put("removed", removed).put("savedBytes", saved);
        } finally {
            module.close();
        }
    }

    // -------------------------------------------------------------- shared

    private static boolean dynamicNameLookup(ApkModule module) {
        for (InputSource input : module.getInputSources()) {
            if (!input.getName().matches("classes[0-9]*\\.dex")) continue;
            try (InputStream in = input.openStream()) {
                String text = new String(FileOps.bytes(in, 128L * 1024 * 1024), StandardCharsets.ISO_8859_1);
                if (text.contains("getIdentifier") || text.contains("getResourceEntryName")
                        || text.contains("getResourceName")) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static boolean isResourceId(int value) {
        return value >= 0x7f000000 && value <= 0x7fffffff;
    }

    private static boolean isObfuscated(String name) {
        if (name == null) return false;
        return name.matches("[a-z]{1,3}[0-9]{0,2}") && name.length() <= 4;
    }

    private static String shortName(int index) {
        StringBuilder out = new StringBuilder();
        int value = index;
        do {
            out.insert(0, (char) ('a' + value % 26));
            value = value / 26 - 1;
        } while (value >= 0);
        return out.toString();
    }

    private static Set<String> split(String value) {
        Set<String> out = new LinkedHashSet<>();
        if (value == null) return out;
        for (String part : value.split("[,，\\s]+")) if (!part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte b : data) out.append(String.format(Locale.US, "%02x", b & 255));
        return out.toString();
    }

    private static int version(byte[] data) {
        try { return Integer.parseInt(new String(data, 4, 3, StandardCharsets.US_ASCII)); }
        catch (Exception ignored) { return 35; }
    }
}
