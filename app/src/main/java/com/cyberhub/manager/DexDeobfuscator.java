package com.cyberhub.manager;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.rewriter.*;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.MemoryDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

/**
 * DEX 混淆对抗（对标 MT 管理器「DEX 混淆对抗」）：
 * 把干扰符号 / 单字母 / 超长名 / 非拉丁符号等混淆标识符重命名为顺序可读名。
 * 通过 DexRewriter 统一重写类型、方法、字段引用，保证声明与所有调用点一致。
 */
public final class DexDeobfuscator {

 /** 写回结果 */
 public static class Report {
  public int dexCount=0, classesRenamed=0, methodsRenamed=0, fieldsRenamed=0;
  public final List<String> renamedClasses=new ArrayList<>();
  public final List<String> errors=new ArrayList<>();
 }

 /** 默认匹配：单字母 / 干扰符号 / 非拉丁 / 超长名 */
 private static final Pattern DEFAULT_OBF = Pattern.compile(
    "^[A-Za-z0-9_]{1,2}$|^oO0lI_.*$|.{40,}");

 /**
  * 主入口。target 为 APK 或 .dex。
  * @param customPattern  自定义"哪些名字算混淆"的正则，null/空用默认
  * @param targetClasses  需要处理的类（每行一个，支持 com.foo.* / com.foo.** / ! 排除），null=全部
  */
 public static Report run(File target, String customPattern, List<String> targetClasses, Report out) throws Exception {
  if(out==null)out=new Report();
  Pattern obf = (customPattern!=null && !customPattern.isEmpty())
    ? Pattern.compile(customPattern) : DEFAULT_OBF;
  out.dexCount = collectAndRewrite(target, obf, targetClasses, out) ? 1 : 0;
  return out;
 }

 /** 收集 dex 列表并逐个重命名写回 */
 static boolean collectAndRewrite(File target, Pattern obf, List<String> targetClasses, Report out) throws Exception {
  if (target.getName().endsWith(".dex")) {
   byte[] data = FileOps.read(target, 512L*1024*1024);
   byte[] outDex = rewriteDex(data, obf, targetClasses, out);
   if(outDex==data)return false;
   File outF = new File(target.getParentFile(), target.getName().replaceFirst("\\.dex$","_deobf.dex"));
   try (FileOutputStream fo=new FileOutputStream(outF)) { fo.write(outDex); }
   return true;
  }
  // APK：对每个 classes*.dex 重命名，zip 重写
  Map<String,byte[]> edits = new LinkedHashMap<>();
  try (ZipFile z=new ZipFile(target)) {
   Enumeration<? extends ZipEntry> en=z.entries();
   while (en.hasMoreElements()) {
    ZipEntry e2=en.nextElement();
    if (!e2.getName().matches("classes\\d*\\.dex")) continue;
    byte[] data = FileOps.zipEntry(target, e2.getName(), 512L*1024*1024);
    byte[] outDex = rewriteDex(data, obf, targetClasses, out);
    if (outDex != data) edits.put(e2.getName(), outDex);
   }
  }
  if (edits.isEmpty()) return false;
  File outApk = new File(target.getParentFile(), target.getName().replaceFirst("(\\.apk)$","_deobf$1"));
  FileOps.rewriteZip(target, outApk, edits);
  return true;
 }

 /** 单 dex 重命名：先建映射，再用 DexRewriter 全量重写（声明 + 引用） */
 static byte[] rewriteDex(byte[] original, Pattern obf, List<String> targetClasses, Report out) throws Exception {
  DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forDexVersion(35), original);

  // 1) 类映射：Lold/Name; -> Lnew/Name;
  Map<String,String> classMap = new LinkedHashMap<>();
  for (ClassDef c : dex.getClasses()) {
   if (!shouldProcess(c.getType(), obf, targetClasses)) continue;
   if (!containsObfName(c.getType(), obf)) continue;
   String nt = generateReadableName(c.getType(), classMap);
   if(nt!=null)classMap.put(c.getType(), nt);
  }
  // 2) 成员映射：以旧引用为 key，统一改定义类与名字
  Map<MethodReference,String> methodNames = new HashMap<>();
  Map<FieldReference,String> fieldNames = new HashMap<>();
  for (ClassDef c : dex.getClasses()) {
   String newType = classMap.getOrDefault(c.getType(), c.getType());
   for (Method m : c.getDirectMethods()) renameMember(m, newType, obf, methodNames, out);
   for (Method m : c.getVirtualMethods()) renameMember(m, newType, obf, methodNames, out);
   for (Field f : c.getStaticFields()) renameField(f, newType, obf, fieldNames, out);
   for (Field f : c.getInstanceFields()) renameField(f, newType, obf, fieldNames, out);
  }
  if (classMap.isEmpty() && methodNames.isEmpty() && fieldNames.isEmpty()) return original;

  // 3) DexRewriter 统一重写，避免"只改声明不改引用"造成的悬空引用
  DexRewriter rewriter = new DexRewriter(new RewriterModule() {
   @Override public TypeRewriter getTypeRewriter(Rewriters rs) {
    return new TypeRewriter() {
     @Override public String rewrite(String type) {
      String nt = classMap.get(type);
      return nt!=null ? nt : type;
     }
    };
   }
   @Override public MethodReferenceRewriter getMethodReferenceRewriter(Rewriters rs) {
    return new MethodReferenceRewriter(rs) {
     @Override public MethodReference rewrite(MethodReference m) {
      String nt = methodNames.get(m);
      String name = nt!=null ? nt : m.getName();
      return new ImmutableMethodReference(
        classMap.getOrDefault(m.getDefiningClass(), m.getDefiningClass()),
        name, m.getParameterTypes(), m.getReturnType());
     }
    };
   }
   @Override public FieldReferenceRewriter getFieldReferenceRewriter(Rewriters rs) {
    return new FieldReferenceRewriter(rs) {
     @Override public FieldReference rewrite(FieldReference f) {
      String nt = fieldNames.get(f);
      String name = nt!=null ? nt : f.getName();
      return new ImmutableFieldReference(
        classMap.getOrDefault(f.getDefiningClass(), f.getDefiningClass()),
        name, f.getType());
     }
    };
   }
  });

  DexFile rewritten = rewriter.getDexFileRewriter().rewrite(dex);
  DexPool pool = new DexPool(dex.getOpcodes());
  for (ClassDef c : rewritten.getClasses()) pool.internClass(c);
  MemoryDataStore store = new MemoryDataStore();
  pool.writeTo(store);
  byte[] result = Arrays.copyOf(store.getData(), store.getSize());
  for (String old : classMap.keySet()) {
   out.renamedClasses.add(old.substring(1,old.length()-1).replace('/','.')+" → "
     + classMap.get(old).substring(1,classMap.get(old).length()-1).replace('/','.'));
  }
  out.classesRenamed += classMap.size();
  return result;
 }

 /** 只处理"目标类"；名字是否算混淆由 obf 决定 */
 static boolean shouldProcess(String type, Pattern obf, List<String> targetClasses) {
  if (targetClasses == null || targetClasses.isEmpty()) return true;
  String dotted = type.substring(1,type.length()-1).replace('/','.');
  boolean in=false, excluded=false;
  for (String rule : targetClasses) {
   String r=rule.trim(); if (r.isEmpty()) continue;
   if (r.startsWith("!")) {
    String ex=r.substring(1);
    if (matchRule(ex, dotted)) excluded=true;
   } else {
    if (matchRule(r, dotted)) in=true;
   }
  }
  return in && !excluded;
 }

 static boolean matchRule(String rule, String dotted) {
  if (rule.endsWith(".*")) return dotted.startsWith(rule.substring(0,rule.length()-2));
  if (rule.endsWith(".**")) return dotted.startsWith(rule.substring(0,rule.length()-3));
  return dotted.equals(rule);
 }

 /** 类描述符里是否有"混淆名"段 */
 static boolean containsObfName(String typeDesc, Pattern obf) {
  // Lcom/foo/Bar; -> 检查最后一段类名（Bar）
  String simple = typeDesc.substring(typeDesc.lastIndexOf('/')+1, typeDesc.length()-1);
  return obf.matcher(simple).find();
 }

 /** 生成可读名：保留包路径与内部类层级，外层类名取 abc 序列且不冲突 */
 static String generateReadableName(String typeDesc, Map<String,String> classMap) {
  int lastSlash = typeDesc.lastIndexOf('/');
  if(lastSlash<0)return null;
  String pkg = typeDesc.substring(0, lastSlash+1);
  String simple = typeDesc.substring(lastSlash+1, typeDesc.length()-1);
  int dollar = simple.indexOf('$');
  String inner = dollar>=0 ? simple.substring(dollar) : "";
  Set<String> used=new HashSet<>();
  for(String v:classMap.values()){
   if(v.startsWith(pkg)){
    String s=v.substring(pkg.length());
    int d=s.indexOf('$');
    used.add(d>=0?s.substring(0,d):s);
   }
  }
  String base;
  int n=1;
  while(true){
   base=toAbc(n++);
   if(!used.contains(base))break;
   if(n>100000)return null;
  }
  return pkg + base + inner + ";";
 }

 static String toAbc(int n) {
  // 1->abc 2->def ...（3 字母循环）
  StringBuilder sb = new StringBuilder();
  int x = n-1;
  for (int i=0;i<3;i++) { int c = (x % 26); x/=26; sb.append((char)('a'+c)); }
  return sb.reverse().toString();
 }

 /** 方法重命名（仅混淆名成员，保留 <init>/<clinit>），冲突时追加序号 */
 static void renameMember(Method m, String newDefiningClass, Pattern obf, Map<MethodReference,String> map, Report out) {
  String name = m.getName();
  if (name.equals("<init>") || name.equals("<clinit>")) return;
  if (!obf.matcher(name).find()) return;
  MethodReference oldRef = new ImmutableMethodReference(m.getDefiningClass(), name, m.getParameterTypes(), m.getReturnType());
  String newName = "m_" + hashName(name);
  newName = uniqueName(newName, map.values(), name);
  map.put(oldRef, newName);
  out.methodsRenamed++;
 }

 static void renameField(Field f, String newDefiningClass, Pattern obf, Map<FieldReference,String> map, Report out) {
  String name = f.getName();
  if (!obf.matcher(name).find()) return;
  FieldReference oldRef = new ImmutableFieldReference(f.getDefiningClass(), name, f.getType());
  String newName = "f_" + hashName(name);
  newName = uniqueName(newName, map.values(), name);
  map.put(oldRef, newName);
  out.fieldsRenamed++;
 }

 /** 避免重命名后与同类已有成员名冲突 */
 static String uniqueName(String base, Collection<String> used, String fallback) {
  if (!used.contains(base)) return base;
  for (int i=2;i<200;i++) {
   String c = base + "_" + i;
   if (!used.contains(c)) return c;
  }
  return fallback;
 }

 /** 简单稳定哈希（把混淆名映射成短数字） */
 static String hashName(String name) {
  int h = 0;
  for (int i=0;i<name.length();i++) h = h*31 + name.charAt(i);
  return Integer.toHexString(h & 0x7fffffff);
 }
}
