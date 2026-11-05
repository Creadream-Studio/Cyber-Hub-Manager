package com.cyberhub.manager;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.ImmutableTryBlock;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * DEX 级通用操作：
 * <ul>
 *   <li>注入日志探针：零寄存器占用，调用注入的 {@code LogProbe.d()}，不改变原方法寄存器分配。</li>
 *   <li>DEX 重新划分：按类数量或目标 DEX 数重排 classes*.dex，保持类内容不变。</li>
 * </ul>
 */
public final class DexTools {
    private DexTools() {}

    public static final String PROBE_TYPE = "Lcom/cyberhub/inject/LogProbe;";

    private static final String PROBE_SMALI =
            ".class public final Lcom/cyberhub/inject/LogProbe;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public static d()V\n" +
            "    .registers 4\n" +
            "    const-string v0, \"CyberHub\"\n" +
            "    new-instance v1, Ljava/lang/Throwable;\n" +
            "    invoke-direct {v1}, Ljava/lang/Throwable;-><init>()V\n" +
            "    invoke-virtual {v1}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;\n" +
            "    move-result-object v1\n" +
            "    const/4 v2, 0x1\n" +
            "    aget-object v1, v1, v2\n" +
            "    invoke-virtual {v1}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;\n" +
            "    move-result-object v2\n" +
            "    invoke-virtual {v1}, Ljava/lang/StackTraceElement;->getMethodName()Ljava/lang/String;\n" +
            "    move-result-object v3\n" +
            "    new-instance v1, Ljava/lang/StringBuilder;\n" +
            "    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V\n" +
            "    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
            "    const-string v2, \"->\"\n" +
            "    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
            "    invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
            "    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;\n" +
            "    move-result-object v1\n" +
            "    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I\n" +
            "    return-void\n" +
            ".end method\n";

    public static JSONObject injectLog(File source, File destination, JSONObject opts) throws Exception {
        Pattern classPattern = Pattern.compile(opts.optString("classPattern", ".*"));
        Pattern methodPattern = Pattern.compile(opts.optString("methodPattern", ".*"));
        String[] excludes = opts.optString("exclude",
                "android.*,java.*,javax.*,kotlin.*,kotlinx.*,com.android.*,dalvik.*,org.jetbrains.*")
                .split(",");
        int limit = Math.max(1, opts.optInt("limit", 3000));

        Map<String, DexBackedDexFile> dexes = load(source);
        Map<String, List<ClassDef>> rebuilt = new LinkedHashMap<>();
        Map<String, Opcodes> opcodes = new LinkedHashMap<>();
        JSONArray injected = new JSONArray();
        int count = 0;
        boolean probePresent = false;
        String probeHostDex = null;

        List<String> order = new ArrayList<>(dexes.keySet());
        order.sort(Comparator.comparingInt(DexTools::dexNumber));
        if (order.isEmpty()) throw new Exception("APK 内没有 classes*.dex");
        probeHostDex = order.get(0);

        for (String dexName : order) {
            DexBackedDexFile dex = dexes.get(dexName);
            boolean dexChanged = false;
            List<ClassDef> out = new ArrayList<>();
            for (ClassDef classDef : dex.getClasses()) {
                if (classDef.getType().equals(PROBE_TYPE)) probePresent = true;
                List<Method> methods = new ArrayList<>();
                boolean classChanged = false;
                for (Method method : classDef.getMethods()) {
                    if (count >= limit || !matches(classDef, method, classPattern, methodPattern, excludes)) {
                        methods.add(method);
                        continue;
                    }
                    Method patched = addProbe(method);
                    if (patched == null) { methods.add(method); continue; }
                    methods.add(patched);
                    classChanged = true;
                    dexChanged = true;
                    count++;
                    if (injected.length() < 300) injected.put(new JSONObject()
                            .put("class", classDef.getType())
                            .put("method", method.getName())
                            .put("dex", dexName));
                }
                out.add(classChanged ? new ImmutableClassDef(classDef.getType(), classDef.getAccessFlags(),
                        classDef.getSuperclass(), classDef.getInterfaces(), classDef.getSourceFile(),
                        classDef.getAnnotations(), classDef.getFields(), methods) : classDef);
            }
            if (dexChanged) {
                rebuilt.put(dexName, out);
                opcodes.put(dexName, dex.getOpcodes());
            }
        }
        if (count == 0) throw new Exception("没有匹配到可注入的方法。请调整类名/方法名规则。");

        boolean newProbe = !probePresent;
        if (newProbe) {
            ClassDef probe = assembleProbe();
            List<ClassDef> host = rebuilt.get(probeHostDex);
            if (host == null) {
                host = new ArrayList<>(dexes.get(probeHostDex).getClasses());
                rebuilt.put(probeHostDex, host);
                opcodes.put(probeHostDex, dexes.get(probeHostDex).getOpcodes());
            }
            host.add(probe);
        }
        Map<String, byte[]> edits = writeDexes(rebuilt, opcodes);
        FileOps.rewriteZip(source, destination, edits);
        return new JSONObject()
                .put("injected", count)
                .put("files", edits.size())
                .put("probeAdded", newProbe)
                .put("methods", injected);
    }

    private static final int REFERENCE_BUDGET = 60000;

    private static final class ClassWeight {
        final int methods;
        final int fields;
        final int types;
        final int strings;

        ClassWeight(int methods, int fields, int types, int strings) {
            this.methods = methods;
            this.fields = fields;
            this.types = types;
            this.strings = strings;
        }
    }

    private static ClassWeight measure(ClassDef classDef) {
        java.util.Set<String> methods = new java.util.HashSet<>();
        java.util.Set<String> fields = new java.util.HashSet<>();
        java.util.Set<String> types = new java.util.HashSet<>();
        java.util.Set<String> strings = new java.util.HashSet<>();
        types.add(classDef.getType());
        if (classDef.getSuperclass() != null) types.add(classDef.getSuperclass());
        for (String itf : classDef.getInterfaces()) types.add(itf);
        for (Method method : classDef.getMethods()) {
            methods.add(method.getDefiningClass() + "->" + method.getName() + "(" + method.getParameters() + ")" + method.getReturnType());
            types.add(method.getDefiningClass());
            for (CharSequence param : method.getParameters()) types.add(param.toString());
            types.add(method.getReturnType());
            MethodImplementation impl = method.getImplementation();
            if (impl == null) continue;
            for (Instruction instruction : impl.getInstructions()) {
                if (instruction instanceof org.jf.dexlib2.iface.instruction.ReferenceInstruction) {
                    Object ref = ((org.jf.dexlib2.iface.instruction.ReferenceInstruction) instruction).getReference();
                    if (ref instanceof MethodReference) {
                        MethodReference mr = (MethodReference) ref;
                        methods.add(mr.getDefiningClass() + "->" + mr.getName() + "(" + mr.getParameterTypes() + ")" + mr.getReturnType());
                        types.add(mr.getDefiningClass());
                        for (CharSequence param : mr.getParameterTypes()) types.add(param.toString());
                        types.add(mr.getReturnType());
                    } else if (ref instanceof org.jf.dexlib2.iface.reference.FieldReference) {
                        org.jf.dexlib2.iface.reference.FieldReference fr = (org.jf.dexlib2.iface.reference.FieldReference) ref;
                        fields.add(fr.getDefiningClass() + "->" + fr.getName() + ":" + fr.getType());
                        types.add(fr.getDefiningClass());
                        types.add(fr.getType());
                    } else if (ref instanceof org.jf.dexlib2.iface.reference.TypeReference) {
                        types.add(((org.jf.dexlib2.iface.reference.TypeReference) ref).getType());
                    } else if (ref instanceof org.jf.dexlib2.iface.reference.StringReference) {
                        strings.add(((org.jf.dexlib2.iface.reference.StringReference) ref).getString());
                    }
                }
            }
        }
        return new ClassWeight(methods.size(), fields.size(), types.size(), strings.size());
    }

    public static JSONObject repartition(File source, File destination, JSONObject opts) throws Exception {
        Map<String, DexBackedDexFile> dexes = load(source);
        if (dexes.isEmpty()) throw new Exception("APK 内没有 classes*.dex");
        List<String> order = new ArrayList<>(dexes.keySet());
        order.sort(Comparator.comparingInt(DexTools::dexNumber));
        List<ClassDef> all = new ArrayList<>();
        Opcodes opcodes = dexes.get(order.get(0)).getOpcodes();
        for (String name : order) {
            DexBackedDexFile dex = dexes.get(name);
            for (ClassDef classDef : dex.getClasses()) all.add(classDef);
            opcodes = dex.getOpcodes();
        }
        if (all.isEmpty()) throw new Exception("没有可划分的类");
        List<ClassWeight> weights = new ArrayList<>(all.size());
        long totalMethods = 0, totalTypes = 0;
        for (ClassDef classDef : all) {
            ClassWeight weight = measure(classDef);
            weights.add(weight);
            totalMethods += weight.methods;
            totalTypes += weight.types;
        }
        int maxClasses = Math.max(1, opts.optInt("maxClasses", 0));
        int count = opts.optInt("count", 0);
        if (count <= 0 && maxClasses <= 0) count = 2;
        long desired = (count > 0)
                ? Math.max(1, (totalMethods + count - 1) / count)
                : Math.max(1, maxClasses);

        List<List<ClassDef>> groups = new ArrayList<>();
        List<ClassDef> current = new ArrayList<>();
        ClassWeight sum = new ClassWeight(0, 0, 0, 0);
        for (int i = 0; i < all.size(); i++) {
            ClassWeight weight = weights.get(i);
            ClassDef classDef = all.get(i);
            if (current.isEmpty() && (weight.methods > REFERENCE_BUDGET
                    || weight.types > REFERENCE_BUDGET || weight.strings > REFERENCE_BUDGET)) {
                throw new Exception("单个类 " + classDef.getType() + " 的引用数超过 DEX 上限，无法划分");
            }
            boolean overflow = !current.isEmpty() && (sum.methods + weight.methods > REFERENCE_BUDGET
                    || sum.types + weight.types > REFERENCE_BUDGET
                    || sum.strings + weight.strings > REFERENCE_BUDGET
                    || sum.fields + weight.fields > REFERENCE_BUDGET);
            boolean overClasses = maxClasses > 0 && !current.isEmpty() && current.size() >= maxClasses;
            boolean overDesired = count > 0 && groups.size() < count - 1 && !current.isEmpty()
                    && sum.methods + weight.methods > desired;
            if (overflow || overClasses || overDesired) {
                groups.add(current);
                current = new ArrayList<>();
                sum = new ClassWeight(0, 0, 0, 0);
                i--;
                continue;
            }
            current.add(classDef);
            sum = new ClassWeight(sum.methods + weight.methods, sum.fields + weight.fields,
                    sum.types + weight.types, sum.strings + weight.strings);
        }
        if (!current.isEmpty()) groups.add(current);

        File tempDir = File.createTempFile(".dex-split-", ".dir");
        tempDir.delete();
        tempDir.mkdirs();
        Map<String, byte[]> outputs = new LinkedHashMap<>();
        try {
            int index = 0;
            for (List<ClassDef> group : groups) {
                String name = index == 0 ? "classes.dex" : "classes" + (index + 1) + ".dex";
                File file = new File(tempDir, name);
                File rewritten = new File(tempDir, name + ".tmp");
                DexPool.writeTo(rewritten.getPath(), new ImmutableDexFile(opcodes, group));
                // DexPool refuses to overwrite an existing file; rename to the final name.
                FileOps.replace(rewritten, file);
                outputs.put(name, FileOps.read(file, 256L * 1024 * 1024));
                index++;
            }
        } finally {
            FileOps.deleteTree(tempDir);
        }

        File temp = File.createTempFile(".dex-repart-", ".apk");
        try {
            ApkModule module = ApkModule.loadApkFile(source);
            try {
                for (InputSource input : module.getInputSources()) {
                    if (input.getName().matches("classes[0-9]*\\.dex")) module.getZipEntryMap().remove(input.getName());
                }
                for (Map.Entry<String, byte[]> entry : outputs.entrySet()) {
                    module.add(new ByteInputSource(entry.getValue(), entry.getKey()));
                }
                module.writeApk(temp);
            } finally {
                module.close();
            }
            FileOps.replace(temp, destination);
        } finally {
            temp.delete();
        }
        JSONObject out = new JSONObject();
        out.put("dexes", outputs.size());
        out.put("classes", all.size());
        out.put("requested", Math.max(1, count > 0 ? count : maxClasses > 0 ? (all.size() + maxClasses - 1) / maxClasses : 1));
        out.put("maxMethods", REFERENCE_BUDGET);
        JSONArray names = new JSONArray();
        for (Map.Entry<String, byte[]> entry : outputs.entrySet())
            names.put(new JSONObject().put("name", entry.getKey()).put("bytes", entry.getValue().length));
        out.put("files", names);
        return out;
    }

    private static boolean matches(ClassDef classDef, Method method, Pattern classPattern,
                                   Pattern methodPattern, String[] excludes) {
        if (method.getImplementation() == null) return false;
        if (method.getName().equals("<clinit>")) return false;
        String dotted = classDef.getType();
        if (dotted.startsWith("L") && dotted.endsWith(";")) dotted = dotted.substring(1, dotted.length() - 1);
        dotted = dotted.replace('/', '.');
        for (String exclude : excludes) {
            String pattern = exclude.trim();
            if (pattern.isEmpty()) continue;
            if (Pattern.compile(pattern.replace(".", "\\.").replace("\\*", ".*")).matcher(dotted).matches()) return false;
        }
        if (!classPattern.matcher(dotted).matches()) return false;
        return methodPattern.matcher(method.getName()).matches();
    }

    private static Method addProbe(Method method) {
        MethodImplementation impl = method.getImplementation();
        if (impl == null) return null;
        List<Instruction> instructions = new ArrayList<>();
        // 4 个 code unit（nop + invoke-static/35c）：保持 payload 的 4 字节对齐不被破坏。
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 0, 0, 0, 0, 0, 0,
                new ImmutableMethodReference(PROBE_TYPE, "d", Collections.emptyList(), "V")));
        for (Instruction instruction : impl.getInstructions()) instructions.add(instruction);
        List<ImmutableTryBlock> tries = new ArrayList<>();
        for (TryBlock<? extends ExceptionHandler> block : impl.getTryBlocks()) {
            List<ImmutableExceptionHandler> handlers = new ArrayList<>();
            for (ExceptionHandler handler : block.getExceptionHandlers())
                handlers.add(new ImmutableExceptionHandler(handler.getExceptionType(), handler.getHandlerCodeAddress() + 4));
            tries.add(new ImmutableTryBlock(block.getStartCodeAddress() + 4, block.getCodeUnitCount(), handlers));
        }
        MethodImplementation next = new ImmutableMethodImplementation(impl.getRegisterCount(),
                instructions, tries, Collections.emptyList());
        return new ImmutableMethod(method.getDefiningClass(), method.getName(), method.getParameters(),
                method.getReturnType(), method.getAccessFlags(), method.getAnnotations(),
                method.getHiddenApiRestrictions(), next);
    }

    private static ClassDef assembleProbe() throws Exception {
        File smali = File.createTempFile(".logprobe-", ".smali");
        File dex = File.createTempFile(".logprobe-", ".dex");
        dex.delete();
        try {
            FileOps.writeAtomic(smali, PROBE_SMALI.getBytes(StandardCharsets.UTF_8), false);
            SmaliOptions options = new SmaliOptions();
            options.apiLevel = 28;
            options.jobs = 1;
            options.outputDexFile = dex.getPath();
            java.io.ByteArrayOutputStream errors = new java.io.ByteArrayOutputStream();
            java.io.PrintStream previous = System.err;
            boolean ok;
            try { System.setErr(new java.io.PrintStream(errors)); ok = Smali.assemble(options, smali.getPath()); }
            finally { System.setErr(previous); }
            if (!ok) throw new Exception("日志探针汇编失败：" + errors.toString("UTF-8"));
            DexBackedDexFile loaded = DexFileFactory.loadDexFile(dex, Opcodes.forApi(28));
            for (ClassDef classDef : loaded.getClasses()) if (classDef.getType().equals(PROBE_TYPE)) return classDef;
            throw new Exception("日志探针类未生成");
        } finally {
            smali.delete();
            dex.delete();
        }
    }

    private static Map<String, byte[]> writeDexes(Map<String, List<ClassDef>> rebuilt,
                                                  Map<String, Opcodes> opcodes) throws Exception {
        Map<String, byte[]> edits = new LinkedHashMap<>();
        File temp = File.createTempFile(".dex-write-", ".dex");
        try {
            for (Map.Entry<String, List<ClassDef>> entry : rebuilt.entrySet()) {
                File out = new File(temp.getParentFile(), temp.getName() + "-" + entry.getKey());
                DexPool.writeTo(out.getPath(), new ImmutableDexFile(opcodes.get(entry.getKey()), entry.getValue()));
                edits.put(entry.getKey(), FileOps.read(out, 256L * 1024 * 1024));
                out.delete();
            }
        } finally {
            temp.delete();
        }
        return edits;
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
        try { return Integer.parseInt(new String(data, 4, 3, StandardCharsets.US_ASCII)); }
        catch (Exception ignored) { return 35; }
    }

    private static int dexNumber(String name) {
        String number = name.substring(7, name.length() - 4);
        return number.isEmpty() ? 1 : Integer.parseInt(number);
    }
}
