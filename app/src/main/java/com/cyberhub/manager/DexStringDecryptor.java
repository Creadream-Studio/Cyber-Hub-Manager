package com.cyberhub.manager;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.security.Key;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/**
 * DEX 字符串静态解密器。
 * P0：JCE 类（Cipher.doFinal，密钥为常量）+ 字节表 XOR 类（单字节/多字节 key 循环异或）。
 * 写回：只写报告 + 重新 dex（P0 暂不写回字符串池，避免半实现；写回在 P2 做，
 * 与深度优化共用 dexlib2 DexPool 重写路径）。
 */
public final class DexStringDecryptor {

 /** 调用点：方法 + 指令位置 + 密文字符串（池条目） */
 public static class CallSite {
  public final Method method;
  public final int instructionIndex;
  public final String encrypted;
  public String decryptedValue;
  CallSite(Method m,int i,String s){method=m;instructionIndex=i;encrypted=s;}
 }

 public static class Report {
  public int dexScanned=0, candidates=0, callSites=0, decrypted=0, skipped=0;
  public final List<String> candidateMethods=new ArrayList<>();
  public final List<String> samples=new ArrayList<>();
  public final List<String> errors=new ArrayList<>();
 }

 /**
  * 主入口。target 为 APK 或 .dex。
  * customPattern：可选，形如 "Lcom/foo/Str;->a([B[B)Ljava/lang/String;"，* 视为 .+。
  */
 public static Report run(File target, String customPattern, Report out) throws Exception {
  out.dexScanned++;
  Pattern custom = compileCustomPattern(customPattern);
  // 收集所有 dex
  Map<String,byte[]> dexes = collectDexes(target);
  if (dexes.isEmpty()) { out.errors.add("未找到 dex 文件"); return out; }

  // 对每个 dex：识别候选解密方法 + 定位调用点 + 本地求值
  for (Map.Entry<String,byte[]> e : dexes.entrySet()) {
   processOneDex(e.getKey(), e.getValue(), custom, out);
  }
  return out;
 }

 private static Map<String,byte[]> collectDexes(File target) throws Exception {
  Map<String,byte[]> out = new LinkedHashMap<>();
  if (target.getName().endsWith(".dex")) {
   out.put("classes.dex", FileOps.read(target, 512L*1024*1024));
  } else {
   try (ZipFile z=new ZipFile(target)) {
    Enumeration<? extends ZipEntry> en=z.entries();
    while (en.hasMoreElements()) {
     ZipEntry e2=en.nextElement();
     if (e2.getName().matches("classes\\d*\\.dex"))
      out.put(e2.getName(), FileOps.zipEntry(target, e2.getName(), 512L*1024*1024));
    }
   }
  }
  return out;
 }

 private static void processOneDex(String name, byte[] data, Pattern custom, Report out) throws Exception {
  DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(35), data);

  // 1) 识别候选解密方法（JCE + XOR 字节表）
  Map<String, DecryptorInfo> candidates = new LinkedHashMap<>();
  for (ClassDef c : dex.getClasses()) {
   for (Method m : c.getMethods()) {
    MethodImplementation impl = m.getImplementation();
    if (impl == null) continue;
    DecryptorInfo info = tryBuildDecryptor(impl);
    if (info == null) continue;
    if (custom != null && !custom.matcher(info.signature).matches()) continue;
    info.definingClass = m.getDefiningClass();
    info.returnDesc = m.getReturnType();
    candidates.put(info.signature, info);
   }
  }
  out.candidates += candidates.size();
  for (DecryptorInfo d : candidates.values())
   if (out.candidateMethods.size()<50) out.candidateMethods.add(d.signature);

  // 2) 定位调用点并求值
  for (ClassDef c : dex.getClasses()) {
   for (Method m : c.getMethods()) {
    MethodImplementation impl = m.getImplementation();
    if (impl == null) continue;
    List<Instruction> insns = new ArrayList<>();
    for (Instruction x : impl.getInstructions()) insns.add(x);
    for (int i=0;i<insns.size();i++) {
     Instruction ins = insns.get(i);
     if (!(ins instanceof ReferenceInstruction)) continue;
     Reference ref = ((ReferenceInstruction) ins).getReference();
     if (!(ref instanceof MethodReference)) continue;
     MethodReference mr = (MethodReference) ref;
     DecryptorInfo di = candidates.get(sigOf(mr));
     if (di == null) continue;
     // 提取实参：向前回溯窗口，找 const-string / const/16 / const 字面量
     String enc = findConstAbove(insns, i);
     if (enc == null) { out.skipped++; continue; }
     CallSite site = new CallSite(m, i, enc);
     String plain = di.evaluate(enc, out);
     if (plain != null && isPrintable(plain)) {
      site.decryptedValue = plain;
      out.decrypted++;
      if (out.samples.size()<20) out.samples.add(enc + " → " + plain + "  ("+di.kind+" "+di.signature+")");
     } else out.skipped++;
    }
   }
  }
  out.callSites += candidates.size(); // 近似
 }

 /** 解密器元信息 */
 static class DecryptorInfo {
  String signature, definingClass, returnDesc;
  String kind; // "JCE-AES" / "JCE-DES" / "XOR"
  String transformation;
  byte[] keyBytes;
  int xorKey;
  int xorStart;
  int xorEnd;
  boolean reverse;

  String evaluate(String enc, Report rep) {
   try {
    if (kind.startsWith("JCE")) {
     Cipher c = Cipher.getInstance(transformation);
     String algo = (keyBytes.length==24 || keyBytes.length==8) ? "DES" : "AES";
     c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, algo));
     byte[] raw = enc.getBytes(StandardCharsets.ISO_8859_1);
     byte[] dec = c.doFinal(raw);
     return new String(dec, StandardCharsets.UTF_8);
    }
    if (kind.equals("XOR")) {
     byte[] raw = enc.getBytes(StandardCharsets.ISO_8859_1);
     int n = raw.length;
     byte[] buf = new byte[n];
     for (int i=0;i<n;i++) {
      int idx = reverse ? (n-1-i) : i;
      int k = (xorKey != 0) ? (xorStart + idx) % (xorEnd - xorStart + 1) : 0;
      buf[i] = (byte)(raw[i] ^ ((k >= 0 && k <= 127) ? k : 0x27));
     }
     return new String(buf, StandardCharsets.UTF_8);
    }
    rep.errors.add("未知 kind: "+kind);
    return null;
   } catch (Exception t) { rep.errors.add(kind+" 失败: "+t.getMessage()); return null; }
  }
 }

 /** 尝试从方法体构造一个可工作的解密器 */
 static DecryptorInfo tryBuildDecryptor(MethodImplementation impl) {
  // JCE 路径
  DecryptorInfo jce = buildJce(impl);
  if (jce != null) return jce;
  // XOR 字节表路径
  DecryptorInfo xor = buildXor(impl);
  if (xor != null) return xor;
  return null;
 }

 /** JCE：方法体里找 Cipher.getInstance("AES/...") 常量 + 常量密钥 */
 static DecryptorInfo buildJce(MethodImplementation impl) {
  String transform = null;
  byte[] key = null;
  boolean doFinal=false;
  for (Instruction ins : impl.getInstructions()) {
   if (!(ins instanceof ReferenceInstruction)) continue;
   Reference ref = ((ReferenceInstruction) ins).getReference();
   if (ref instanceof StringReference) {
    String s = ((StringReference) ref).getString();
    if (s != null && s.matches("[A-Z]{2,5}/[A-Z]{2,4}(/[A-Z]{3,20})?")) transform = s;
   }
   if (ref instanceof MethodReference) {
    MethodReference mr = (MethodReference) ref;
    if ("Ljava/security/Cipher;".equals(mr.getDefiningClass()) && "doFinal".equals(mr.getName()))
     doFinal = true;
   }
   // 密钥：const-string 指向 byte[] 初始化或 const/16
   if (ins instanceof OneRegisterInstruction) {
    OneRegisterInstruction o = (OneRegisterInstruction) ins;
    if (o.getOpcode() == Opcode.CONST_STRING && o instanceof ReferenceInstruction) {
     Reference r2 = ((ReferenceInstruction) o).getReference();
     if (r2 instanceof StringReference) {
      String ks = ((StringReference) r2).getString();
      if (ks != null && key==null && ks.length()>=16 && ks.length()%4==0)
       key = Base64decode(ks);
     }
    }
   }
  }
  if (!doFinal || transform==null) return null;
  if (key == null) return null; // P0 要求密钥在方法体里是常量
  DecryptorInfo d = new DecryptorInfo();
  d.transformation = transform;
  d.keyBytes = key;
  d.kind = "JCE-"+transform.split("/")[0];
  return d;
 }

 /** XOR：方法体里找 ixor + 数组访问 + 常量 key/边界 */
 static DecryptorInfo buildXor(MethodImplementation impl) {
  boolean hasIxor=false, hasArrayGet=false, hasIntConst=false;
  int intConst=0;
  for (Instruction ins : impl.getInstructions()) {
   String op = ins.getOpcode().name;
   if (op.equals("ixor")) hasIxor=true;
   if (op.equals("iget-byte") || op.equals("aget-byte")) hasArrayGet=true;
   if (ins instanceof NarrowLiteralInstruction) {
    NarrowLiteralInstruction n = (NarrowLiteralInstruction) ins;
    if (n.getOpcode() == Opcode.CONST_4 || n.getOpcode() == Opcode.CONST_16) {
     intConst = n.getNarrowLiteral(); hasIntConst=true;
    }
   }
  }
  if (!hasIxor || !hasArrayGet) return null;
  DecryptorInfo d = new DecryptorInfo();
  d.kind = "XOR";
  d.xorKey = hasIntConst ? intConst : 0x27;
  d.xorStart = 0; d.xorEnd = 255;
  d.reverse = false;
  return d;
 }

 static byte[] Base64decode(String s) {
  try { return Base64.getDecoder().decode(s); }
  catch (Exception e) { try { return s.getBytes(StandardCharsets.UTF_8); } catch (Exception e2) { return null; } }
 }

 static String sigOf(MethodReference r) {
  return r.getDefiningClass()+"->"+r.getName()
    +"("+String.join("",r.getParameterTypes())+")"+r.getReturnType();
}
 static String sigOf(Method m) {
  return m.getDefiningClass()+"->"+m.getName()
    +"("+String.join("",m.getParameterTypes())+")"+m.getReturnType();
}

 /** 向前回溯 8 条，找 const-string 字面量 */
 static String findConstAbove(List<Instruction> insns, int i) {
  int from = Math.max(0, i-8);
  for (int j=i-1;j>=from;j--) {
   Instruction cur = insns.get(j);
   if (cur.getOpcode() == Opcode.CONST_STRING && cur instanceof ReferenceInstruction) {
    Reference r = ((ReferenceInstruction) cur).getReference();
    if (r instanceof StringReference) return ((StringReference) r).getString();
   }
   if (cur.getOpcode() == Opcode.MOVE_RESULT || cur.getOpcode() == Opcode.MOVE_RESULT_OBJECT) continue;
   break;
  }
  return null;
 }

 static boolean isPrintable(String s) {
  if (s==null || s.isEmpty()) return false;
  for (int i=0;i<s.length();i++) {
   char c=s.charAt(i);
   if (c<32 && c!='\t' && c!='\n') return false;
  }
  return true;
 }

 static Pattern compileCustomPattern(String p) {
  if (p==null || p.isEmpty()) return null;
  StringBuilder sb = new StringBuilder();
  for (char c : p.toCharArray()) sb.append(c=='*' ? ".+" : Pattern.quote(String.valueOf(c)));
  return Pattern.compile(sb.toString());
 }
}
