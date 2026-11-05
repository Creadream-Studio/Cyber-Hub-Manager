package com.cyberhub.manager;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11n;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x;
import org.jf.dexlib2.writer.pool.DexPool;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 去除签名校验 / 校验点扫描。启发式定位“返回 boolean 且读取 PackageManager / Signature /
 * 证书字符串”的方法，并把方法体替换为 return true。默认只报告，apply 时才写回副本。
 */
public final class SignatureBypass {
    private SignatureBypass() {}

    private static final String DEFAULT_NAME = "(?i).*((check|verify|valid|is)[A-Za-z0-9_]*(sign|cert)|(sign|cert)[A-Za-z0-9_]*(check|verify|valid)).*";

    public static JSONObject scan(File apk, JSONObject opts) throws Exception {
        Pattern name = Pattern.compile(opts.optString("pattern", DEFAULT_NAME));
        JSONArray found = new JSONArray();
        int total = 0;
        for (Map.Entry<String, DexBackedDexFile> entry : load(apk).entrySet()) {
            for (ClassDef classDef : entry.getValue().getClasses()) {
                for (Method method : classDef.getMethods()) {
                    if (!candidate(method)) continue;
                    String reason = reason(method, name);
                    if (reason == null) continue;
                    total++;
                    if (found.length() < 200) found.put(new JSONObject()
                            .put("class", classDef.getType())
                            .put("method", method.getName())
                            .put("signature", methodSignature(method))
                            .put("reason", reason)
                            .put("dex", entry.getKey()));
                }
            }
        }
        return new JSONObject().put("count", total).put("candidates", found);
    }

    public static JSONObject apply(File source, File destination, JSONObject opts) throws Exception {
        Pattern name = Pattern.compile(opts.optString("pattern", DEFAULT_NAME));
        JSONArray forced = opts.optJSONArray("methods");
        Map<String, List<ClassDef>> rebuilt = new LinkedHashMap<>();
        Map<String, Opcodes> opcodes = new LinkedHashMap<>();
        JSONArray patched = new JSONArray();
        int methods = 0;
        for (Map.Entry<String, DexBackedDexFile> entry : load(source).entrySet()) {
            boolean changed = false;
            List<ClassDef> classes = new ArrayList<>();
            for (ClassDef classDef : entry.getValue().getClasses()) {
                List<Method> methodsOut = new ArrayList<>();
                boolean classChanged = false;
                for (Method method : classDef.getMethods()) {
                    boolean force = forced != null && contains(forced, DexIndexKey(method));
                    if (!candidate(method) || (!force && reason(method, name) == null)) {
                        methodsOut.add(method);
                        continue;
                    }
                    Method replacement = neutralize(method);
                    if (replacement == null) { methodsOut.add(method); continue; }
                    methodsOut.add(replacement);
                    classChanged = true;
                    changed = true;
                    methods++;
                    if (patched.length() < 200) patched.put(new JSONObject()
                            .put("class", classDef.getType())
                            .put("method", method.getName())
                            .put("signature", methodSignature(method))
                            .put("forced", force));
                }
                classes.add(classChanged ? new ImmutableClassDef(classDef.getType(), classDef.getAccessFlags(),
                        classDef.getSuperclass(), classDef.getInterfaces(), classDef.getSourceFile(),
                        classDef.getAnnotations(), classDef.getFields(), methodsOut) : classDef);
            }
            if (changed) {
                rebuilt.put(entry.getKey(), classes);
                opcodes.put(entry.getKey(), entry.getValue().getOpcodes());
            }
        }
        if (methods == 0) throw new Exception("未找到可替换的签名校验方法。请用自定义规则或手动指定方法。");
        Map<String, byte[]> edits = new LinkedHashMap<>();
        File temp = File.createTempFile(".sigbypass-", ".dex");
        try {
            for (Map.Entry<String, List<ClassDef>> entry : rebuilt.entrySet()) {
                DexPool.writeTo(temp.getPath(), new ImmutableDexFile(opcodes.get(entry.getKey()), entry.getValue()));
                edits.put(entry.getKey(), FileOps.read(temp, 256L * 1024 * 1024));
            }
        } finally {
            temp.delete();
        }
        FileOps.rewriteZip(source, destination, edits);
        return new JSONObject().put("patched", methods).put("files", edits.size()).put("methods", patched);
    }

    private static String DexIndexKey(Method method) {
        return method.getDefiningClass() + "->" + method.getName() + "(" + String.join("", method.getParameterTypes()) + ")" + method.getReturnType();
    }

    private static boolean contains(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) if (value.equals(array.optString(i))) return true;
        return false;
    }

    private static boolean candidate(Method method) {
        if (method.getImplementation() == null) return false;
        if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) return false;
        String ret = method.getReturnType();
        return ret.equals("Z") || ret.equals("I");
    }

    private static String reason(Method method, Pattern namePattern) {
        int score = 0;
        String reason = null;
        if (namePattern.matcher(method.getName()).matches()) { score += 3; reason = "方法名匹配"; }
        for (Instruction instruction : method.getImplementation().getInstructions()) {
            if (!(instruction instanceof ReferenceInstruction)) continue;
            Reference ref = ((ReferenceInstruction) instruction).getReference();
            if (ref instanceof StringReference) {
                String value = ((StringReference) ref).getString().toLowerCase(Locale.ROOT);
                if (value.contains("signature") || value.contains("cert")
                        || value.contains("sha-1") || value.contains("sha1") || value.contains("md5")) {
                    score += 1;
                    if (reason == null) reason = "字符串常量包含签名/证书特征";
                }
            } else if (ref instanceof MethodReference) {
                MethodReference methodRef = (MethodReference) ref;
                String defining = methodRef.getDefiningClass();
                String methodName = methodRef.getName();
                if (defining.contains("PackageManager") || defining.contains("PackageInfo")
                        || defining.contains("Signature")) {
                    score += 1;
                    if (reason == null) reason = "调用签名 API：" + methodRef.getName();
                }
                if (methodName.equals("getPackageInfo") || methodName.equals("getInstalledPackages")
                        || methodName.equals("getSignatures") || methodName.equals("checkSignatures")) {
                    score += 2;
                    if (reason == null) reason = "调用 " + methodName;
                }
            }
        }
        if (score == 0) return null;
        if (method.getReturnType().equals("Z") && score >= 2) return reason;
        if (namePattern.matcher(method.getName()).matches()) return reason;
        return null;
    }

    private static String methodSignature(Method method) {
        return method.getName() + "(" + String.join("", method.getParameterTypes()) + ")" + method.getReturnType();
    }

    private static Method neutralize(Method method) {
        String ret = method.getReturnType();
        if (!ret.equals("Z") && !ret.equals("I")) return null;
        List<Instruction> instructions = new ArrayList<>();
        instructions.add(new ImmutableInstruction11n(Opcode.CONST_4, 0, 1));
        instructions.add(new ImmutableInstruction11x(Opcode.RETURN, 0));
        MethodImplementation impl = new ImmutableMethodImplementation(
                Math.max(1, method.getImplementation().getRegisterCount()),
                instructions, Collections.emptyList(), Collections.emptyList());
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), impl);
    }

    private static Map<String, DexBackedDexFile> load(File apk) throws Exception {
        Map<String, DexBackedDexFile> out = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(entry)) { data = FileOps.bytes(in, 128L * 1024 * 1024); }
                out.put(entry.getName(), new DexBackedDexFile(Opcodes.forDexVersion(version(data)), data));
            }
        }
        return out;
    }

    private static int version(byte[] data) {
        try { return Integer.parseInt(new String(data, 4, 3, java.nio.charset.StandardCharsets.US_ASCII)); }
        catch (Exception ignored) { return 35; }
    }
}
