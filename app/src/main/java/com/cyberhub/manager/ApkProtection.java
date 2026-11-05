package com.cyberhub.manager;

import com.android.apksig.ApkVerifier;
import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 防护等级评估：从签名方案、清单危险开关、加固特征库、反调试/反 Frida/反 Root
 * 字符串以及 native 库分布推导一个可解释的分数，不做任何“破解”动作。
 */
public final class ApkProtection {
    private ApkProtection() {}

    private static final Map<String,String> PACKERS = new LinkedHashMap<>();
    static {
        PACKERS.put("libjiagu", "360 加固");
        PACKERS.put("libsecexe", "梆梆安全");
        PACKERS.put("libsecmain", "梆梆安全");
        PACKERS.put("libDexHelper", "DexGuard / 娜迦");
        PACKERS.put("libmobisec", "阿里聚安全");
        PACKERS.put("libnesec", "网易易盾");
        PACKERS.put("libnqshield", "网秦");
        PACKERS.put("libtup", "腾讯乐固");
        PACKERS.put("libbaiduprotect", "百度加固");
        PACKERS.put("libkwscmm", "金山");
        PACKERS.put("libappprotect", "通付盾");
        PACKERS.put("libshell", "通用加固壳");
        PACKERS.put("libexecmain", "爱加密");
        PACKERS.put("libexec.so", "爱加密");
        PACKERS.put("libapktoolplus", "ApktoolPlus 壳");
    }

    private static final String[] ANTI_DEBUG = {
            "TracerPid", "ptrace", "android.os.Debug", "isDebuggerConnected",
            "waitForDebugger", "anti_debug", "antiDebug"
    };
    private static final String[] ANTI_HOOK = {
            "frida", "xposed", "XposedBridge", "substrate", "LSPosed",
            "Magisk", "magisk", "/su", "supersu", "busybox",
            "de.robv.android.xposed", "re.frida.server"
    };

    public static JSONObject analyze(File apk) throws Exception {
        JSONObject out = new JSONObject();
        JSONArray findings = new JSONArray();
        int score = 0;

        String signature = "未签名 / 未校验";
        boolean v1 = false, v2 = false, v3 = false, verified = false;
        try {
            ApkVerifier.Result r = new ApkVerifier.Builder(apk).build().verify();
            verified = r.isVerified();
            v1 = r.isVerifiedUsingV1Scheme();
            v2 = r.isVerifiedUsingV2Scheme();
            v3 = r.isVerifiedUsingV3Scheme();
            List<String> schemes = new ArrayList<>();
            if (v1) schemes.add("V1");
            if (v2) schemes.add("V2");
            if (v3) schemes.add("V3");
            signature = verified ? String.join(" + ", schemes) : "签名无效或未签名";
        } catch (Exception error) {
            signature = "无法校验：" + error.getMessage();
        }
        if (verified) {
            if (v2 && v3) { score += 2; findings.put(finding("info", "已启用 V2 + V3 签名，完整性较强")); }
            else if (v2 || v3) { score += 1; findings.put(finding("info", "仅启用部分新版签名方案")); }
            else { findings.put(finding("warn", "仅有 V1 (JAR) 签名，容易被二次打包")); }
        } else {
            findings.put(finding("warn", "未签名或签名校验失败"));
        }

        boolean debuggable = false, testOnly = false, allowBackup = true, extractNativeLibs = true;
        String packageName = "";
        int targetSdk = 0;
        try (ApkModule module = ApkModule.loadApkFile(apk)) {
            if (module.hasAndroidManifest()) {
                AndroidManifestBlock manifest = module.getAndroidManifest();
                packageName = manifest.getPackageName();
                debuggable = manifest.isDebuggable();
                Integer sdk = manifest.getTargetSdkVersion();
                targetSdk = sdk == null ? 0 : sdk;
                Boolean extract = manifest.isExtractNativeLibs();
                extractNativeLibs = extract == null || extract;
                com.reandroid.arsc.chunk.xml.ResXmlElement app = manifest.getApplicationElement();
                if (app != null) {
                    ResXmlAttribute test = app.searchAttributeByName("testOnly");
                    if (test != null) testOnly = test.getValueAsBoolean();
                    ResXmlAttribute backup = app.searchAttributeByName("allowBackup");
                    if (backup != null) allowBackup = backup.getValueAsBoolean();
                }
            }
        } catch (Exception error) {
            findings.put(finding("warn", "清单解析失败：" + error.getMessage()));
        }
        if (debuggable) { score += 2; findings.put(finding("warn", "android:debuggable=true，可直接附加调试器")); }
        if (testOnly) { score += 1; findings.put(finding("warn", "android:testOnly=true")); }
        if (!allowBackup) findings.put(finding("info", "已关闭 allowBackup"));
        if (!extractNativeLibs) findings.put(finding("info", "native 库未解压 (extractNativeLibs=false)"));

        int nativeCount = 0;
        StringBuilder packed = new StringBuilder();
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (name.startsWith("lib/") && name.endsWith(".so")) {
                    nativeCount++;
                    String lower = name.toLowerCase(Locale.ROOT);
                    for (Map.Entry<String,String> packer : PACKERS.entrySet()) {
                        if (lower.contains(packer.getKey().toLowerCase(Locale.ROOT))) {
                            if (packed.length() > 0) packed.append('、');
                            packed.append(packer.getValue());
                        }
                    }
                }
            }
        }
        if (nativeCount > 0) { score += 1; findings.put(finding("info", "包含 " + nativeCount + " 个 native 库，核心逻辑可能在 SO")); }
        if (packed.length() > 0) {
            score += 2;
            findings.put(finding("danger", "检测到加固特征：" + packed));
        }

        int antiDebug = 0, antiHook = 0, dexCount = 0;
        try (ZipFile zip = new ZipFile(apk)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                dexCount++;
                byte[] data;
                try (InputStream in = zip.getInputStream(entry)) { data = FileOps.bytes(in, 128L * 1024 * 1024); }
                for (String needle : ANTI_DEBUG) if (contains(data, needle.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))) antiDebug++;
                for (String needle : ANTI_HOOK) if (contains(data, needle.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1))) antiHook++;
            }
        }
        if (antiDebug > 0) { score += 1; findings.put(finding("warn", "发现反调试特征 " + antiDebug + " 处")); }
        if (antiHook > 0) { score += 1; findings.put(finding("warn", "发现反 Hook / 反 Root 特征 " + antiHook + " 处")); }
        if (dexCount > 1) findings.put(finding("info", "多 DEX (" + dexCount + " 个)"));

        String level;
        if (score >= 6) level = "高";
        else if (score >= 3) level = "中";
        else level = "低";

        out.put("level", level);
        out.put("score", score);
        out.put("signature", signature);
        out.put("verified", verified);
        out.put("debuggable", debuggable);
        out.put("testOnly", testOnly);
        out.put("allowBackup", allowBackup);
        out.put("extractNativeLibs", extractNativeLibs);
        out.put("package", packageName);
        out.put("targetSdk", targetSdk);
        out.put("nativeLibs", nativeCount);
        out.put("dexFiles", dexCount);
        out.put("packers", packed.toString());
        out.put("antiDebugHits", antiDebug);
        out.put("antiHookHits", antiHook);
        out.put("findings", findings);
        return out;
    }

    private static boolean contains(byte[] data, byte[] needle) {
        int limit = data.length - needle.length;
        outer:
        for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < needle.length; j++) if (data[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }

    private static JSONObject finding(String severity, String text) throws Exception {
        return new JSONObject().put("severity", severity).put("text", text);
    }
}
