package com.cyberhub.manager;

import com.reandroid.apk.ApkModule;
import com.reandroid.archive.ByteInputSource;
import com.reandroid.archive.InputSource;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 注入文件提供器：在清单的 application 下新增一个 exported=false 的 ContentProvider，
 * 并把 {@code com.cyberhub.inject.CyberFileProvider} 写入 classes.dex。
 * 访问方式：{@code content://<包名>.cyberhub.fileprovider/files/<相对路径>}。
 */
public final class FileProviderInjector {
    private FileProviderInjector() {}

    public static final String TYPE = "Lcom/cyberhub/inject/CyberFileProvider;";
    public static final String CLASS = "com.cyberhub.inject.CyberFileProvider";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final String SMALI =
            ".class public Lcom/cyberhub/inject/CyberFileProvider;\n" +
            ".super Landroid/content/ContentProvider;\n" +
            ".source \"CyberFileProvider.java\"\n" +
            "\n" +
            ".method public constructor <init>()V\n" +
            "    .registers 1\n" +
            "    invoke-direct {p0}, Landroid/content/ContentProvider;-><init>()V\n" +
            "    return-void\n" +
            ".end method\n" +
            "\n" +
            ".method public onCreate()Z\n" +
            "    .registers 2\n" +
            "    const/4 v0, 0x1\n" +
            "    return v0\n" +
            ".end method\n" +
            "\n" +
            ".method public query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;\n" +
            "    .registers 7\n" +
            "    const/4 v0, 0x0\n" +
            "    return-object v0\n" +
            ".end method\n" +
            "\n" +
            ".method public getType(Landroid/net/Uri;)Ljava/lang/String;\n" +
            "    .registers 3\n" +
            "    const-string v0, \"application/octet-stream\"\n" +
            "    return-object v0\n" +
            ".end method\n" +
            "\n" +
            ".method public insert(Landroid/net/Uri;Landroid/content/ContentValues;)Landroid/net/Uri;\n" +
            "    .registers 4\n" +
            "    const/4 v0, 0x0\n" +
            "    return-object v0\n" +
            ".end method\n" +
            "\n" +
            ".method public delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I\n" +
            "    .registers 5\n" +
            "    const/4 v0, 0x0\n" +
            "    return v0\n" +
            ".end method\n" +
            "\n" +
            ".method public update(Landroid/net/Uri;Landroid/content/ContentValues;Ljava/lang/String;[Ljava/lang/String;)I\n" +
            "    .registers 6\n" +
            "    const/4 v0, 0x0\n" +
            "    return v0\n" +
            ".end method\n" +
            "\n" +
            ".method public openFile(Landroid/net/Uri;Ljava/lang/String;)Landroid/os/ParcelFileDescriptor;\n" +
            "    .registers 9\n" +
            "    invoke-virtual {p1}, Landroid/net/Uri;->getPath()Ljava/lang/String;\n" +
            "    move-result-object v0\n" +
            "    if-nez v0, :bad\n" +
            "    const-string v1, \"/\"\n" +
            "    const-string v2, \"\"\n" +
            "    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;\n" +
            "    move-result-object v0\n" +
            "    invoke-virtual {p0}, Lcom/cyberhub/inject/CyberFileProvider;->getContext()Landroid/content/Context;\n" +
            "    move-result-object v1\n" +
            "    invoke-virtual {v1}, Landroid/content/Context;->getFilesDir()Ljava/io/File;\n" +
            "    move-result-object v2\n" +
            "    const-string v3, \"cache/\"\n" +
            "    invoke-virtual {v0, v3}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z\n" +
            "    move-result v3\n" +
            "    if-eqz v3, :root\n" +
            "    invoke-virtual {v1}, Landroid/content/Context;->getCacheDir()Ljava/io/File;\n" +
            "    move-result-object v2\n" +
            "    const/4 v3, 0x6\n" +
            "    invoke-virtual {v0, v3}, Ljava/lang/String;->substring(I)Ljava/lang/String;\n" +
            "    move-result-object v0\n" +
            "    :root\n" +
            "    const-string v3, \"external/\"\n" +
            "    invoke-virtual {v0, v3}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z\n" +
            "    move-result v3\n" +
            "    if-eqz v3, :open\n" +
            "    invoke-virtual {v1}, Landroid/content/Context;->getExternalFilesDir(Ljava/lang/String;)Ljava/io/File;\n" +
            "    move-result-object v2\n" +
            "    const/16 v3, 0x9\n" +
            "    invoke-virtual {v0, v3}, Ljava/lang/String;->substring(I)Ljava/lang/String;\n" +
            "    move-result-object v0\n" +
            "    :open\n" +
            "    new-instance v3, Ljava/io/File;\n" +
            "    invoke-direct {v3, v2, v0}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V\n" +
            "    invoke-virtual {v3}, Ljava/io/File;->getCanonicalFile()Ljava/io/File;\n" +
            "    move-result-object v3\n" +
            "    invoke-virtual {v2}, Ljava/io/File;->getCanonicalFile()Ljava/io/File;\n" +
            "    move-result-object v2\n" +
            "    invoke-virtual {v3}, Ljava/io/File;->getPath()Ljava/lang/String;\n" +
            "    move-result-object v4\n" +
            "    invoke-virtual {v2}, Ljava/io/File;->getPath()Ljava/lang/String;\n" +
            "    move-result-object v5\n" +
            "    invoke-virtual {v4, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z\n" +
            "    move-result v4\n" +
            "    if-eqz v4, :safe\n" +
            "    new-instance v0, Ljava/lang/SecurityException;\n" +
            "    const-string v1, \"path escapes provider root\"\n" +
            "    invoke-direct {v0, v1}, Ljava/lang/SecurityException;-><init>(Ljava/lang/String;)V\n" +
            "    throw v0\n" +
            "    :safe\n" +
            "    const/high16 v4, 0x10000000\n" +
            "    invoke-static {v3, v4}, Landroid/os/ParcelFileDescriptor;->open(Ljava/io/File;I)Landroid/os/ParcelFileDescriptor;\n" +
            "    move-result-object v0\n" +
            "    return-object v0\n" +
            "    :bad\n" +
            "    new-instance v0, Ljava/lang/IllegalArgumentException;\n" +
            "    const-string v1, \"uri\"\n" +
            "    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V\n" +
            "    throw v0\n" +
            ".end method\n";

    public static JSONObject inject(File source, File destination, JSONObject opts) throws Exception {
        String authoritySuffix = opts.optString("authority", "cyberhub.fileprovider");
        if (!authoritySuffix.matches("[a-zA-Z0-9_.]+")) throw new Exception("authority 只能包含字母、数字、下划线和点");
        ApkModule module = ApkModule.loadApkFile(source);
        File temp = null;
        try {
            if (!module.hasAndroidManifest()) throw new Exception("APK 没有 AndroidManifest.xml");
            String packageName = module.getPackageName();
            if (packageName == null || packageName.isEmpty()) throw new Exception("无法读取包名");
            String authority = packageName + "." + authoritySuffix;
            AndroidManifestBlock manifest = module.getAndroidManifest();
            ResXmlElement app = manifest.getApplicationElement();
            if (app == null) app = manifest.getOrCreateApplicationElement();
            java.util.Iterator<?> providerIt = app.getElements("provider");
            while (providerIt.hasNext()) {
                Object item = providerIt.next();
                if (!(item instanceof ResXmlElement)) continue;
                ResXmlElement existing = (ResXmlElement) item;
                ResXmlAttribute attr = existing.searchAttributeByName("authorities");
                if (attr != null && authority.equals(attr.getValueAsString()))
                    throw new Exception("清单中已存在同名 provider：" + authority);
            }
            ResXmlElement provider = app.newElement("provider");
            addString(provider, "name", 0x01010003, CLASS);
            addString(provider, "authorities", 0x01010018, authority);
            addBool(provider, "exported", 0x01010010, false);
            addBool(provider, "grantUriPermissions", 0x0101001b, true);
            module.refreshManifest();

            String host = "classes.dex";
            boolean present = false;
            for (InputSource input : module.getInputSources()) {
                if (!input.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] data;
                try (InputStream in = input.openStream()) { data = FileOps.bytes(in, 128L * 1024 * 1024); }
                DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(version(data)), data);
                for (ClassDef existing : dex.getClasses()) if (existing.getType().equals(TYPE)) { present = true; break; }
            }
            if (!present) {
                ClassDef providerClass = assembleProvider();
                InputSource hostInput = module.getInputSource(host);
                File out = File.createTempFile(".provider-", ".dex");
                try {
                    if (hostInput != null) {
                        byte[] data;
                        try (InputStream in = hostInput.openStream()) { data = FileOps.bytes(in, 128L * 1024 * 1024); }
                        DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(version(data)), data);
                        List<ClassDef> classes = new ArrayList<>();
                        for (ClassDef existing : dex.getClasses()) classes.add(existing);
                        classes.add(providerClass);
                        DexPool.writeTo(out.getPath(), new ImmutableDexFile(dex.getOpcodes(), classes));
                    } else {
                        DexPool.writeTo(out.getPath(), new ImmutableDexFile(Opcodes.forApi(28), Collections.singletonList(providerClass)));
                    }
                    module.add(new ByteInputSource(FileOps.read(out, 256L * 1024 * 1024), host));
                } finally {
                    out.delete();
                }
            }
            temp = File.createTempFile(".provider-", ".apk", destination.getAbsoluteFile().getParentFile());
            module.writeApk(temp);
            FileOps.replace(temp, destination);
            return new JSONObject().put("authority", authority).put("class", CLASS)
                    .put("uri", "content://" + authority + "/files/<path>");
        } finally {
            if (temp != null) temp.delete();
            module.close();
        }
    }

    private static void addString(ResXmlElement element, String name, int id, String value) {
        ResXmlAttribute attr = element.newAttribute(name, id);
        attr.setNamespace(ANDROID_NS, "android");
        attr.setValueAsString(value);
    }

    private static void addBool(ResXmlElement element, String name, int id, boolean value) {
        ResXmlAttribute attr = element.newAttribute(name, id);
        attr.setNamespace(ANDROID_NS, "android");
        attr.setValueAsBoolean(value);
    }

    private static ClassDef assembleProvider() throws Exception {
        File smali = File.createTempFile(".provider-", ".smali");
        File dex = File.createTempFile(".provider-", ".dex");
        dex.delete();
        try {
            FileOps.writeAtomic(smali, SMALI.getBytes(StandardCharsets.UTF_8), false);
            SmaliOptions options = new SmaliOptions();
            options.apiLevel = 28;
            options.jobs = 1;
            options.outputDexFile = dex.getPath();
            java.io.ByteArrayOutputStream errors = new java.io.ByteArrayOutputStream();
            java.io.PrintStream previous = System.err;
            boolean ok;
            try { System.setErr(new java.io.PrintStream(errors)); ok = Smali.assemble(options, smali.getPath()); }
            finally { System.setErr(previous); }
            if (!ok) throw new Exception("文件提供器汇编失败：" + errors.toString("UTF-8"));
            DexBackedDexFile loaded = DexFileFactory.loadDexFile(dex, Opcodes.forApi(28));
            for (ClassDef classDef : loaded.getClasses()) if (classDef.getType().equals(TYPE)) return classDef;
            throw new Exception("文件提供器类未生成");
        } finally {
            smali.delete();
            dex.delete();
        }
    }

    private static int version(byte[] data) {
        try { return Integer.parseInt(new String(data, 4, 3, StandardCharsets.US_ASCII)); }
        catch (Exception ignored) { return 35; }
    }
}
