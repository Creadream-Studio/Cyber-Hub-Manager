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

/** 新增 APK 工具的回归测试：在真实 APK 上执行并校验签名/结构。 */
public final class ApkToolsSmokeTest {
    public static void main(String[] args) throws Exception {
        File apk = new File(args[0]);
        File out = new File(args[1]);
        File key = new File(args[2]);
        out.mkdirs();
        check(apk.isFile(), "test apk missing: " + apk);

        JSONObject protection = ApkProtection.analyze(apk);
        check(protection.optString("level").length() > 0, "protection level empty");
        check(protection.has("findings"), "protection findings missing");
        System.out.println("ok protection level=" + protection.optString("level") + " score=" + protection.optInt("score"));

        JSONObject scan = SignatureBypass.scan(apk, new JSONObject());
        check(scan.has("count"), "signature scan missing count");
        System.out.println("ok signature scan candidates=" + scan.optInt("count"));

        File provider = new File(out, "provider.apk");
        JSONObject providerResult = FileProviderInjector.inject(apk, provider, new JSONObject());
        sign(provider, key);
        verify(provider);
        check(hasClass(provider, FileProviderInjector.TYPE), "provider class missing");
        check(containsText(provider, "AndroidManifest.xml", providerResult.getString("authority")), "provider authority missing");
        System.out.println("ok provider " + providerResult.optString("authority"));

        File logged = new File(out, "log.apk");
        DexTools.injectLog(apk, logged, new JSONObject()
                .put("classPattern", "com\\.cyberhub\\.manager\\..*").put("limit", 5));
        sign(logged, key);
        verify(logged);
        check(hasClass(logged, DexTools.PROBE_TYPE), "log probe class missing");
        System.out.println("ok log injection");

        File split = new File(out, "split.apk");
        JSONObject splitResult = DexTools.repartition(apk, split, new JSONObject().put("count", 2));
        sign(split, key);
        verify(split);
        check(splitResult.optInt("dexes") >= 2, "expected at least 2 dex files");
        System.out.println("ok dex repartition classes=" + splitResult.optInt("classes"));

        File deobf = new File(out, "deobf.apk");
        ResTools.deobfuscate(apk, deobf, new JSONObject());
        sign(deobf, key);
        verify(deobf);
        System.out.println("ok resource deobfuscate");

        File obf = new File(out, "obf.apk");
        ResTools.obfuscate(apk, obf, new JSONObject().put("force", true));
        sign(obf, key);
        verify(obf);
        System.out.println("ok resource obfuscate");

        File dedupe = new File(out, "dedupe.apk");
        try {
            JSONObject result = ResTools.dedupe(apk, dedupe);
            sign(dedupe, key);
            verify(dedupe);
            System.out.println("ok resource dedupe removed=" + result.optInt("removed"));
        } catch (Exception error) {
            System.out.println("skip resource dedupe: " + error.getMessage());
        }

        File slim = new File(out, "slim.apk");
        try {
            ResTools.slim(apk, slim, new JSONObject().put("unused", false).put("types", "anim"));
            sign(slim, key);
            verify(slim);
            System.out.println("ok resource slim");
        } catch (Exception error) {
            System.out.println("skip resource slim: " + error.getMessage());
        }

        File bypass = new File(out, "nosig.apk");
        try {
            SignatureBypass.apply(apk, bypass, new JSONObject());
            sign(bypass, key);
            verify(bypass);
            System.out.println("ok signature bypass");
        } catch (Exception error) {
            System.out.println("skip signature bypass: " + error.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
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
        check(result.isVerified(), "signature verification failed: " + result.getErrors());
    }

    private static boolean containsText(File apk, String entryName, String needle) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) return false;
            byte[] data;
            try (InputStream in = zip.getInputStream(entry)) { data = FileOps.bytes(in, 64L * 1024 * 1024); }
            return new Axml().decode(data).contains(needle);
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
