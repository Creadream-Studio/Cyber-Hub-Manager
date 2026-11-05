package com.cyberhub.manager;

import com.android.apksig.ApkVerifier;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 桌面冒烟测试：直接调用新增的 APK 工具，不依赖 Android 运行时。 */
public final class ApkToolsSmoke {
    public static void main(String[] args) throws Exception {
        File apk = new File(args[0]);
        File out = new File(args[1]);
        out.mkdirs();
        File key = new File(args.length > 2 ? args[2] : "app/src/main/assets/development-signing.p12");

        JSONObject protection = ApkProtection.analyze(apk);
        System.out.println("PROTECTION level=" + protection.getString("level")
                + " score=" + protection.getInt("score")
                + " sig=" + protection.getString("signature")
                + " debuggable=" + protection.getBoolean("debuggable"));

        File dst = new File(out, "xml.apk");
        JSONObject r = ApkXmlTools.batchReplace(apk, dst, new JSONObject().put("find", " Ok ").put("replace", " TEST "));
        System.out.println("XML_REPLACE " + r);
        sign(dst, key);
        verify(dst);
        System.out.println("XML_VERIFY " + containsText(dst, "r/f/c.xml", "TEST"));

        dst = new File(out, "xmltrans.apk");
        r = ApkXmlTools.translate(apk, dst, (text, lang) -> "T:" + text, new JSONObject().put("lang", "en").put("limit", 50));
        System.out.println("XML_TRANSLATE " + r.getInt("translated"));
        sign(dst, key);
        verify(dst);
        System.out.println("XML_TRANSLATE_TEXT " + containsText(dst, "r/f/c.xml", "T:"));

        dst = new File(out, "dedupe.apk");
        r = ResTools.dedupe(apk, dst);
        System.out.println("DEDUPE " + r);
        sign(dst, key);
        verify(dst);

        dst = new File(out, "deobf.apk");
        r = ResTools.deobfuscate(apk, dst, new JSONObject());
        System.out.println("DEOBF " + r);
        sign(dst, key);
        verify(dst);

        dst = new File(out, "slim.apk");
        r = ResTools.slim(apk, dst, new JSONObject().put("unused", false).put("types", "anim"));
        System.out.println("SLIM " + r);
        sign(dst, key);
        verify(dst);

        dst = new File(out, "obf.apk");
        r = ResTools.obfuscate(apk, dst, new JSONObject().put("force", true));
        System.out.println("OBF " + r);
        sign(dst, key);
        verify(dst);

        JSONObject scan = SignatureBypass.scan(apk, new JSONObject());
        System.out.println("SIG_SCAN " + scan.getInt("count"));
        dst = new File(out, "nosig.apk");
        try {
            r = SignatureBypass.apply(apk, dst, new JSONObject());
            System.out.println("SIG_BYPASS " + r.getInt("patched"));
            sign(dst, key);
            verify(dst);
        } catch (Exception error) {
            System.out.println("SIG_BYPASS_SKIP " + error.getMessage());
        }

        dst = new File(out, "log.apk");
        r = DexTools.injectLog(apk, dst, new JSONObject().put("limit", 3));
        System.out.println("INJECT_LOG " + r.getInt("injected"));
        sign(dst, key);
        verify(dst);
        System.out.println("LOG_PROBE " + hasClass(dst, DexTools.PROBE_TYPE));

        dst = new File(out, "split.apk");
        r = DexTools.repartition(apk, dst, new JSONObject().put("count", 2));
        System.out.println("REPARTITION " + r.getInt("dexes") + " classes=" + r.getInt("classes"));
        sign(dst, key);
        verify(dst);

        dst = new File(out, "fp.apk");
        r = FileProviderInjector.inject(apk, dst, new JSONObject());
        System.out.println("PROVIDER " + r.getString("authority"));
        sign(dst, key);
        verify(dst);
        System.out.println("PROVIDER_CLASS " + hasClass(dst, FileProviderInjector.TYPE));
        System.out.println("PROVIDER_MANIFEST " + containsText(dst, "AndroidManifest.xml", "cyberhub.fileprovider"));
    }

    private static void sign(File apk, File key) throws Exception {
        File signed = new File(apk.getPath() + ".signed");
        try (InputStream in = new java.io.FileInputStream(key)) {
            ApkSigning.sign(apk, signed, in);
        }
        FileOps.replace(signed, apk);
    }

    private static void verify(File apk) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(apk).build().verify();
        if (!result.isVerified()) throw new IllegalStateException("签名校验失败: " + result.getErrors());
        System.out.println("  verified v1=" + result.isVerifiedUsingV1Scheme()
                + " v2=" + result.isVerifiedUsingV2Scheme()
                + " v3=" + result.isVerifiedUsingV3Scheme()
                + " bytes=" + apk.length());
    }

    private static boolean containsText(File apk, String entryName, String needle) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) return false;
            byte[] data;
            try (InputStream in = zip.getInputStream(entry)) { data = FileOps.bytes(in, 64L * 1024 * 1024); }
            String xml = new Axml().decode(data);
            return xml.contains(needle);
        }
    }

    private static boolean hasClass(File apk, String type) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] data;
                try (InputStream in = zip.getInputStream(entry)) { data = FileOps.bytes(in, 128L * 1024 * 1024); }
                int version;
                try { version = Integer.parseInt(new String(data, 4, 3, StandardCharsets.US_ASCII)); }
                catch (Exception ignored) { version = 35; }
                DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(version), data);
                for (ClassDef classDef : dex.getClasses()) if (classDef.getType().equals(type)) return true;
            }
        }
        return false;
    }
}
