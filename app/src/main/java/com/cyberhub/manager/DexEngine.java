package com.cyberhub.manager;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.regex.*;import java.util.*;import java.util.zip.*;import org.json.*;
import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.dexbacked.*;import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.baksmali.*;import org.jf.baksmali.Adaptors.ClassDefinition;import org.jf.baksmali.formatter.BaksmaliWriter;import org.jf.smali.*;import org.jf.dexlib2.iface.instruction.*;import org.jf.dexlib2.iface.instruction.formats.*;import org.jf.dexlib2.iface.reference.*;import org.jf.dexlib2.immutable.instruction.*;import org.jf.dexlib2.immutable.reference.*;import com.googlecode.d2j.dex.Dex2jar;
import jadx.api.*;import jadx.plugins.input.dex.DexInputPlugin;

public final class DexEngine {
 final File root;File source,session;final Map<String,File>files=new LinkedHashMap<>();final Map<String,DexBackedDexFile>dexes=new LinkedHashMap<>();final SortedMap<String,ClassDef>classes=new TreeMap<>();final Map<String,String>owners=new HashMap<>();String sourceHash;JadxDecompiler jadx;String jadxMode="";
 public DexEngine(File r){root=r;root.mkdirs();}
 public DexEngine scratch(File src,JSONArray names)throws Exception{
  DexEngine t=new DexEngine(new File(root,"scratch-"+UUID.randomUUID()));
  try{t.open(src,names);return t;}catch(Exception error){t.dispose();throw error;}
 }
 public void dispose(){close();try{FileOps.deleteTree(root);}catch(Exception ignored){}}
 public JSONObject open(File src,JSONArray names)throws Exception{close();files.clear();dexes.clear();classes.clear();owners.clear();source=src;sourceHash=FileOps.sha256(src);session=new File(root,UUID.randomUUID().toString());session.mkdirs();
  if(names.length()==0)throw new IOException("请至少选择一个 DEX");
  for(int n=0;n<names.length();n++){String name=names.getString(n);File out=FileOps.child(session,new File(name).getName());byte[] data=src.getName().endsWith(".dex")?FileOps.read(src,256L*1024*1024):FileOps.zipEntry(src,name,256L*1024*1024);FileOps.writeAtomic(out,data,false);files.put(name,out);DexBackedDexFile d=DexFileFactory.loadDexFile(out,Opcodes.forApi(apiOf(out)));dexes.put(name,d);for(ClassDef c:d.getClasses()){if(classes.containsKey(c.getType()))throw new IOException("所选 DEX 文件间存在重复类："+c.getType());classes.put(c.getType(),c);owners.put(c.getType(),name);}}
  return new JSONObject().put("count",classes.size()).put("files",new JSONArray(files.keySet())).put("source",src.getPath());
 }
 public JSONArray tree(String prefix)throws Exception{
  TreeMap<String,JSONObject>items=new TreeMap<>();String p=prefix.replace('.','/');
  for(String type:classes.tailMap("L"+p).keySet()){
   String name=type.substring(1,type.length()-1);if(!name.startsWith(p))break;
   String remain=name.substring(p.length());int slash=remain.indexOf('/');
   if(slash>=0){String folder=remain.substring(0,slash);if(!items.containsKey("0"+folder)){
    String compact=compactPackage(p+folder+"/");
    items.put("0"+folder,new JSONObject().put("name",compact.substring(p.length(),compact.length()-1).replace('/','.')).put("prefix",compact).put("dir",true));
   }}else items.put("1"+remain,new JSONObject().put("name",remain).put("class",type).put("dex",owners.get(type)).put("dir",false));
  }return new JSONArray(items.values());
 }
 private String compactPackage(String prefix){
  // Collapse only single-child package chains; never skip a package with classes.
  for(;;){String only=null;boolean stop=false;
   for(String type:classes.tailMap("L"+prefix).keySet()){
    if(!type.startsWith("L"+prefix))break;
    String rest=type.substring(prefix.length()+1,type.length()-1);int slash=rest.indexOf('/');
    if(slash<0){stop=true;break;}String child=rest.substring(0,slash);
    if(only!=null&&!only.equals(child)){stop=true;break;}only=child;
   }
   if(stop||only==null)return prefix;prefix+=only+"/";
  }
 }
 String smali(ClassDef c)throws IOException{StringWriter writer=new StringWriter();BaksmaliOptions o=new BaksmaliOptions();o.apiLevel=28;new ClassDefinition(o,c).writeTo(new BaksmaliWriter(writer));return writer.toString();}
 public JSONObject code(String type,String mode,String decompiler)throws Exception{ClassDef c=classes.get(type);if(c==null)throw new IOException("未找到类");String text;
  if(mode.equals("java")){
   if(jadx==null||!jadxMode.equals(decompiler)){close();JadxArgs a=new JadxArgs();a.setInputFiles(new ArrayList<>(files.values()));a.setSkipResources(true);a.setThreadsCount(2);a.setInlineAnonymousClasses(false);a.setInlineMethods(false);a.setUseSourceNameAsClassAlias(false);a.setDeobfuscationOn(false);a.setRenameFlags(EnumSet.noneOf(JadxArgs.RenameEnum.class));a.setDecompilationMode(decompiler.equals("simple")?DecompilationMode.SIMPLE:decompiler.equals("fallback")?DecompilationMode.FALLBACK:DecompilationMode.AUTO);jadx=new JadxDecompiler(a);jadx.registerPlugin(new DexInputPlugin());jadx.load();jadxMode=decompiler;}
   JavaClass jc=jadx.searchJavaClassByOrigFullName(type.substring(1,type.length()-1).replace('/','.'));if(jc==null)jc=jadx.searchJavaClassOrItsParentByOrigFullName(type.substring(1,type.length()-1).replace('/','.'));if(jc==null)jc=jadx.searchJavaClassByOrigFullName(type.substring(1,type.length()-1).replace('/','.').replace('$','.'));
   if(jc==null){String parent=type.substring(1,type.length()-1);while(jc==null&&parent.lastIndexOf('$')>=0){parent=parent.substring(0,parent.lastIndexOf('$'));if(classes.containsKey("L"+parent+";"))jc=jadx.searchJavaClassByOrigFullName(parent.replace('/','.'));}}
   if(jc==null)throw new IOException("JADX 无法定位此类");jc=jc.getTopParentClass();type="L"+jc.getRawName().replace('.','/')+";";text=jc.getCode();
  }else text="# "+owners.get(type)+"\n"+smali(c);
  return new JSONObject().put("text",text).put("class",type).put("mode",mode).put("name",type.substring(type.lastIndexOf('/')+1,type.length()-1)+(mode.equals("java")?".java":".smali")).put("dex",owners.get(type));
 }
 public JSONObject saveJava(String type,String text,File platform)throws Exception {
  if(!classes.containsKey(type))throw new IOException("未找到类");
  if(text.contains("方法未反编译：")||text.contains("Method not decompiled:")||text.contains("JADX ERROR:"))throw new IOException("此 Java 输出包含一个未反编译的方法。编译前请恢复其 Java 方法体或编辑 Smali；现有 DEX 未作更改。");
  long started=System.nanoTime();Collection<? extends ClassDef> replacements=JavaEditCompiler.compile(type,text,classes,platform,session);
  applyClasses(type,replacements);return new JSONObject().put("saved",true).put("classes",replacements.size()).put("compileMs",(System.nanoTime()-started)/1000000).put("message","Java 已编译为 DEX。请保存工程，然后签名 APK。");
 }
 private void applyClasses(String rootType,Collection<? extends ClassDef> replacements)throws Exception {
  String rootOwner=owners.get(rootType);if(rootOwner==null)throw new IOException("未找到类");
  Map<String,ClassDef>updates=new HashMap<>();Set<String>affected=new LinkedHashSet<>();
  for(ClassDef c:replacements){String t=c.getType();if(!t.equals(rootType)&&!t.startsWith(rootType.substring(0,rootType.length()-1)+"$"))throw new IOException("编译输出的类不在被编辑的类族内："+t);updates.put(t,c);affected.add(owners.getOrDefault(t,rootOwner));}
  if(!updates.containsKey(rootType))throw new IOException("编译器输出中缺少原始类");
  File transaction=new File(session,"merge-"+UUID.randomUUID());transaction.mkdirs();Map<String,File>staged=new LinkedHashMap<>(),backups=new LinkedHashMap<>();List<String>published=new ArrayList<>();
  try{
   for(String owner:affected){List<ClassDef>all=new ArrayList<>();for(Map.Entry<String,ClassDef>e:classes.entrySet())if(owner.equals(owners.get(e.getKey())))all.add(updates.getOrDefault(e.getKey(),e.getValue()));
    for(ClassDef c:updates.values())if(!owners.containsKey(c.getType())&&owner.equals(rootOwner))all.add(c);
    File changed=new File(transaction,owner+".new");DexPool.writeTo(changed.getPath(),new ImmutableDexFile(dexes.get(owner).getOpcodes(),all));DexFileFactory.loadDexFile(changed,Opcodes.forApi(apiOf(changed)));staged.put(owner,changed);
    File backup=new File(transaction,owner+".bak");FileOps.copyTree(files.get(owner),backup);backups.put(owner,backup);
   }
   try{for(String owner:affected){FileOps.replace(staged.get(owner),files.get(owner));published.add(owner);}}
   catch(Exception failure){for(String owner:published)FileOps.replace(backups.get(owner),files.get(owner));throw failure;}
   for(String owner:affected){DexBackedDexFile d=DexFileFactory.loadDexFile(files.get(owner),Opcodes.forApi(apiOf(files.get(owner))));dexes.put(owner,d);for(ClassDef c:d.getClasses()){classes.put(c.getType(),c);owners.put(c.getType(),owner);}}
   close();
  }finally{FileOps.deleteTree(transaction);}
 }
 public JSONObject save(String type,String text)throws Exception{
  if(!classes.containsKey(type))throw new IOException("未找到类");File input=new File(session,"edit.smali"),compiled=new File(session,"one.dex");FileOps.writeAtomic(input,text.getBytes(StandardCharsets.UTF_8),false);SmaliOptions o=new SmaliOptions();o.apiLevel=28;o.jobs=1;o.outputDexFile=compiled.getPath();o.verboseErrors=true;
  ByteArrayOutputStream errors=new ByteArrayOutputStream();PrintStream previous=System.err;boolean valid;
  try{System.setErr(new PrintStream(errors));valid=Smali.assemble(o,input.getPath());}finally{System.setErr(previous);}
  if(!valid)throw new IOException("Smali 语法错误：\n"+errors.toString("UTF-8"));DexBackedDexFile changed=DexFileFactory.loadDexFile(compiled,Opcodes.forApi(apiOf(compiled)));if(changed.getClasses().size()!=1)throw new IOException("预期只有一个类");ClassDef replacement=changed.getClasses().iterator().next();if(!replacement.getType().equals(type))throw new IOException("请保持类描述符不变");String owner=owners.get(type);List<ClassDef>all=new ArrayList<>();for(Map.Entry<String,ClassDef>e:classes.entrySet())if(owner.equals(owners.get(e.getKey())))all.add(e.getKey().equals(type)?replacement:e.getValue());File dest=files.get(owner),temp=new File(session,"rebuilt.dex");DexPool.writeTo(temp.getPath(),new ImmutableDexFile(dexes.get(owner).getOpcodes(),all));FileOps.replace(temp,dest);DexBackedDexFile reloaded=DexFileFactory.loadDexFile(dest,Opcodes.forApi(apiOf(dest)));dexes.put(owner,reloaded);for(ClassDef cl:reloaded.getClasses())classes.put(cl.getType(),cl);close();return new JSONObject().put("saved",true).put("message","类已组装完成。导出 DEX/APK 以保留更改。");
 }
 public JSONObject export(File folder)throws Exception{if(!folder.isDirectory())throw new IOException("请选择一个已存在的输出文件夹");File output=new File(folder,DataEngine.strip(source.getName())+"-dex-edited"+(source.getName().endsWith(".dex")?".dex":".apk"));if(output.exists())throw new IOException("输出已存在；请先重命名或删除它");if(source.getName().endsWith(".dex"))FileOps.writeAtomic(output,FileOps.read(files.values().iterator().next(),256L*1024*1024),false);else{Map<String,byte[]>edits=new HashMap<>();for(Map.Entry<String,File>e:files.entrySet())edits.put(e.getKey(),FileOps.read(e.getValue(),256L*1024*1024));FileOps.rewriteZip(source,output,edits);}return new JSONObject().put("path",output.getPath()).put("needsSigning",!source.getName().endsWith(".dex"));}
 public JSONObject query(JSONObject a)throws Exception{return DexQueries.query(classes,a);}
 public JSONObject commit()throws Exception{
  return commit(null);
 }
 public JSONObject commit(InputStream signingKey)throws Exception{
  if(source==null||!source.isFile())throw new IOException("源文件缺失");
  if(!FileOps.sha256(source).equals(sourceHash))throw new IOException("源码在本工程外被修改。请改为导出副本。");
  File temp=File.createTempFile(".dex-commit-", ".tmp",source.getAbsoluteFile().getParentFile());
  File signed=new File(temp.getPath()+".signed.apk");
  try{
   if(source.getName().endsWith(".dex"))FileOps.writeAtomic(temp,FileOps.read(files.values().iterator().next(),256L*1024*1024),false);
   else{Map<String,byte[]>edits=new HashMap<>();for(Map.Entry<String,File>e:files.entrySet())edits.put(e.getKey(),FileOps.read(e.getValue(),256L*1024*1024));FileOps.rewriteZip(source,temp,edits);}
   if(signingKey!=null){if(source.getName().endsWith(".dex"))throw new IOException("DEX 文件不是可安装的 APK");ApkSigning.sign(temp,signed,signingKey);}
   if(!FileOps.sha256(source).equals(sourceHash))throw new IOException("保存时源码被修改。请改为导出副本。");
   File backup=FileOps.commitWithBackup(signingKey==null?temp:signed,source);sourceHash=FileOps.sha256(source);
   return new JSONObject().put("path",source.getPath()).put("backup",backup.getPath()).put("verified",signingKey!=null).put("needsSigning",signingKey==null&&!source.getName().endsWith(".dex"));
  }finally{temp.delete();signed.delete();}
 }
 public JSONArray strings(String query)throws Exception{JSONArray out=new JSONArray();String q=query.toLowerCase(Locale.ROOT);Set<String>seen=new HashSet<>();for(DexBackedDexFile d:dexes.values())for(String s:d.getStringSection())if(s.toLowerCase(Locale.ROOT).contains(q)&&seen.add(s)){out.put(s);if(out.length()>=2000)return out;}return out;}
 public JSONArray search(String query)throws Exception{JSONArray out=new JSONArray();if(query.isEmpty())return out;String q=query.toLowerCase(Locale.ROOT);for(Map.Entry<String,ClassDef>e:classes.entrySet()){ClassDef c=e.getValue();boolean found=e.getKey().toLowerCase(Locale.ROOT).contains(q);if(!found)for(Method m:c.getMethods())if(m.getName().toLowerCase(Locale.ROOT).contains(q)){found=true;break;}if(!found)for(Field f:c.getFields())if(f.getName().toLowerCase(Locale.ROOT).contains(q)){found=true;break;}if(found){out.put(new JSONObject().put("name",e.getKey().substring(1,e.getKey().length()-1).replace('/','.')).put("class",e.getKey()));if(out.length()>=1000)break;}}return out;}

 public JSONObject properties()throws Exception{
  JSONArray arr=new JSONArray();long size=0;int strings=0,types=0,classCount=0,methods=0,fields=0;
  for(Map.Entry<String,File> e:files.entrySet()){
   byte[] h=new byte[32];try(InputStream in=new FileInputStream(e.getValue())){if(in.read(h)<32)throw new IOException("不是有效的 DEX："+e.getKey());}
   String ver=new String(h,4,3,StandardCharsets.US_ASCII);int api;try{api=VersionMap.mapDexVersionToApi(Integer.parseInt(ver));}catch(Exception x){api=-1;}
   long sum=(h[8]&255L)|((h[9]&255L)<<8)|((h[10]&255L)<<16)|((h[11]&255L)<<24);
   StringBuilder sig=new StringBuilder();for(int i=12;i<32;i++)sig.append(String.format(Locale.US,"%02x",h[i]));
   DexBackedDexFile d=dexes.get(e.getKey());int ms=0,fs=0;for(ClassDef c:d.getClasses()){for(Method m:c.getMethods())ms++;for(Field f:c.getFields())fs++;}
   JSONObject o=new JSONObject().put("name",e.getKey()).put("size",e.getValue().length()).put("version",ver).put("api",api).put("checksum",String.format(Locale.US,"%08x",sum)).put("signature",sig.toString()).put("strings",d.getStringReferences().size()).put("types",d.getTypeReferences().size()).put("classes",d.getClasses().size()).put("methods",ms).put("fields",fs);
   arr.put(o);size+=e.getValue().length();strings+=o.getInt("strings");types+=o.getInt("types");classCount+=o.getInt("classes");methods+=ms;fields+=fs;
  }
  return new JSONObject().put("path",source==null?"":source.getPath()).put("size",size).put("strings",strings).put("types",types).put("classes",classCount).put("methods",methods).put("fields",fields).put("files",arr);
 }
 public JSONObject repair()throws Exception{
  JSONArray out=new JSONArray();int n=0;
  for(Map.Entry<String,File> e:new LinkedHashMap<>(files).entrySet()){
   DexBackedDexFile d=DexFileFactory.loadDexFile(e.getValue(),Opcodes.forApi(apiOf(e.getValue())));File temp=new File(session,"repair-"+UUID.randomUUID()+".dex");
   DexFileFactory.writeDexFile(temp.getPath(),d);DexBackedDexFile check=DexFileFactory.loadDexFile(temp,Opcodes.forApi(apiOf(temp)));
   if(check.getClasses().size()!=d.getClasses().size())throw new IOException("修复校验失败："+e.getKey());
   FileOps.replace(temp,e.getValue());out.put(new JSONObject().put("name",e.getKey()).put("classes",check.getClasses().size()));n++;
  }
  reloadAll();return new JSONObject().put("files",out).put("count",n).put("message","DEX 已重新生成并校验");
 }
 public JSONObject toSmali(File folder)throws Exception{
  if(folder==null||!folder.isDirectory())throw new IOException("请选择一个已存在的输出文件夹");
  String base=source==null?"classes":baseName(source.getName());File root=unusedDir(folder,base+"_smali");root.mkdirs();
  JSONArray out=new JSONArray();boolean multi=files.size()>1;
  for(Map.Entry<String,File> e:files.entrySet()){
   File dir=multi?new File(root,baseName(e.getKey())):root;DexBackedDexFile d=dexes.get(e.getKey());BaksmaliOptions o=new BaksmaliOptions();o.apiLevel=apiOf(e.getValue());
   Baksmali.disassembleDexFile(d,dir,Math.max(1,Runtime.getRuntime().availableProcessors()),o);out.put(new JSONObject().put("name",e.getKey()).put("path",dir.getPath()));
  }
  return new JSONObject().put("path",root.getPath()).put("files",out).put("smali",countFiles(root,".smali"));
 }
 public JSONObject toJar(File folder)throws Exception{
  if(folder==null||!folder.isDirectory())throw new IOException("请选择一个已存在的输出文件夹");
  String base=source==null?"classes":baseName(source.getName());File dexDir=null;List<File> inputs=new ArrayList<>();
  boolean archive=source!=null&&!source.getName().toLowerCase(Locale.ROOT).endsWith(".dex");
  if(archive){
   dexDir=unusedDir(folder,base+"_dex");dexDir.mkdirs();
   for(Map.Entry<String,File> e:files.entrySet()){File dst=new File(dexDir,new File(e.getKey()).getName());FileOps.writeAtomic(dst,FileOps.read(e.getValue(),256L*1024*1024),false);inputs.add(dst);}
  }else for(File f:files.values())inputs.add(f);
  File jar=unusedFile(folder,base+".jar");File work=new File(session,"jar-"+UUID.randomUUID());work.mkdirs();int classes=0;Set<String>names=new HashSet<>();
  try(ZipOutputStream zout=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jar)))){
   for(File input:inputs){File part=new File(work,input.getName()+".jar");Dex2jar.from(input).skipDebug(false).to(part.toPath());
    try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(new FileInputStream(part)))){
     ZipEntry ze;while((ze=zin.getNextEntry())!=null){String name=ze.getName();if(ze.isDirectory()||name.equalsIgnoreCase("META-INF/MANIFEST.MF")||!names.add(name))continue;zout.putNextEntry(new ZipEntry(name));byte[] buf=new byte[65536];int r;while((r=zin.read(buf))!=-1)zout.write(buf,0,r);zout.closeEntry();if(name.endsWith(".class"))classes++;}
    }
   }
   ZipEntry mf=new ZipEntry("META-INF/MANIFEST.MF");zout.putNextEntry(mf);zout.write("Manifest-Version: 1.0\r\nCreated-By: Cyber Hub Manager\r\n\r\n".getBytes(StandardCharsets.UTF_8));zout.closeEntry();
  }catch(Exception e){jar.delete();throw e;}finally{FileOps.deleteTree(work);}
  return new JSONObject().put("path",jar.getPath()).put("classes",classes).put("dexDir",dexDir==null?"":dexDir.getPath()).put("inputs",inputs.size());
 }
 public JSONObject mergeDex(JSONObject a,File folder)throws Exception{
  if(folder==null||!folder.isDirectory())throw new IOException("请选择一个已存在的输出文件夹");
  List<String> names=new ArrayList<>();List<byte[]> blobs=new ArrayList<>();
  JSONArray paths=a.optJSONArray("paths"),entries=a.optJSONArray("entries");
  if(paths!=null&&paths.length()>0){ // 多选本地 DEX
   for(int i=0;i<paths.length();i++){
    File f=new File(paths.getString(i));
    if(!f.isFile())throw new IOException("找不到 DEX 文件："+f.getName());
    names.add(f.getName());blobs.add(FileOps.read(f,128L*1024*1024));
   }
  }else if(entries!=null&&entries.length()>0){ // 多选压缩包内 DEX
   String archive=a.optString("archive","");
   if(archive.isEmpty())throw new IOException("缺少归档文件。");
   File src=new File(archive);
   for(int i=0;i<entries.length();i++){
    String name=entries.getString(i);
    names.add(name);blobs.add(FileOps.zipEntry(src,name,128L*1024*1024));
   }
  }else{
   File src=new File(a.getString("path"));boolean standalone=src.getName().toLowerCase(Locale.ROOT).endsWith(".dex");
   List<File> local=new ArrayList<>();
   if(standalone){
    File parent=src.getAbsoluteFile().getParentFile();File[] fs=parent==null?null:parent.listFiles();
    if(fs!=null)for(File f:fs)if(f.isFile()&&f.getName().matches("classes(\\d*)\\.dex"))local.add(f);
    if(local.isEmpty())local.add(src);
    local.sort(Comparator.comparingInt(f->dexNumber(f.getName())));
    if(local.size()<2)throw new IOException("至少需要两个 classes*.dex 文件才能合并");
    for(File f:local){names.add(f.getName());blobs.add(FileOps.read(f,128L*1024*1024));}
   }else{
    try(ZipFile z=new ZipFile(src)){
     Enumeration<? extends ZipEntry> en=z.entries();
     while(en.hasMoreElements()){ZipEntry e=en.nextElement();if(!e.isDirectory()&&e.getName().matches("classes(\\d*)\\.dex"))names.add(e.getName());}
    }
    names.sort(Comparator.comparingInt(DexEngine::dexNumber));
    if(names.size()<2)throw new IOException("APK 内至少需要两个 classes*.dex 文件才能合并");
    for(String n:names)blobs.add(FileOps.zipEntry(src,n,128L*1024*1024));
   }
  }
  if(names.size()<2)throw new IOException("至少需要两个 DEX 文件才能合并");
  File work=new File(root,"merge-"+UUID.randomUUID());work.mkdirs();int maxApi=0;List<File> inputs=new ArrayList<>();
  try{
   for(int i=0;i<names.size();i++){
    File in=new File(work,"in-"+i+".dex");FileOps.writeAtomic(in,blobs.get(i),false);inputs.add(in);maxApi=Math.max(maxApi,apiOf(in));
   }
   Opcodes opcodes=Opcodes.forApi(maxApi);List<ClassDef> all=new ArrayList<>();Set<String> seen=new HashSet<>();
   for(File in:inputs){DexBackedDexFile d=DexFileFactory.loadDexFile(in,opcodes);for(ClassDef c:d.getClasses()){if(!seen.add(c.getType()))throw new IOException("DEX 之间存在重复类："+c.getType());all.add(c);}}
   File out=unusedFile(folder,"classes-merged.dex");
   try{
    DexPool.writeTo(out.getPath(),new ImmutableDexFile(opcodes,all));
    DexBackedDexFile check=DexFileFactory.loadDexFile(out,Opcodes.forApi(apiOf(out)));
    return new JSONObject().put("path",out.getPath()).put("dexes",names.size()).put("classes",check.getClasses().size());
   }catch(Exception failure){
    out.delete();StringBuilder msg=new StringBuilder(failure.getMessage()==null?"":failure.getMessage());
    for(Throwable c=failure.getCause();c!=null;c=c.getCause())msg.append(' ').append(c.getMessage()==null?"":c.getMessage());
    String text=msg.toString();
    if(text.contains("out of range")||text.contains("Unsigned short")||text.contains("too many"))throw new IOException("合并后的 DEX 超出单个 DEX 限制，请减少 DEX 数量。",failure);
    throw new IOException("合并 DEX 失败："+text,failure);
   }
  }finally{FileOps.deleteTree(work);}
 }
 private static int dexNumber(String n){String s=n.substring(7,n.length()-4);return s.isEmpty()?1:Integer.parseInt(s);}
 public JSONObject replaceStrings(JSONObject a)throws Exception{
  Map<String,String> exact=null;String find=null,repl="";boolean matchCase=a.optBoolean("matchCase",true);
  if(a.has("replacements")){exact=new LinkedHashMap<>();JSONObject m=a.getJSONObject("replacements");for(Iterator<String>it=m.keys();it.hasNext();){String k=it.next();exact.put(k,m.optString(k,""));}}
  else{find=a.optString("find","");repl=a.optString("replace","");if(find.isEmpty())throw new IOException("查找内容不能为空");}
  int[] stats=new int[2];JSONArray changedFiles=new JSONArray();
  for(Map.Entry<String,File> e:new LinkedHashMap<>(files).entrySet()){
   String owner=e.getKey();List<ClassDef> updated=new ArrayList<>();int changed=0;
   for(ClassDef c:dexes.get(owner).getClasses()){ClassDef nc=rewriteStrings(c,exact,find,repl,matchCase,stats);if(nc!=c)changed++;updated.add(nc);}
   if(changed==0)continue;
   File temp=new File(session,"strings-"+UUID.randomUUID()+".dex");DexPool.writeTo(temp.getPath(),new ImmutableDexFile(dexes.get(owner).getOpcodes(),updated));DexBackedDexFile check=DexFileFactory.loadDexFile(temp,Opcodes.forApi(apiOf(temp)));FileOps.replace(temp,e.getValue());dexes.put(owner,check);for(ClassDef c:check.getClasses()){classes.put(c.getType(),c);owners.put(c.getType(),owner);}changedFiles.put(owner);
  }
  close();return new JSONObject().put("replaced",stats[0]).put("classes",stats[1]).put("files",changedFiles);
 }
 private ClassDef rewriteStrings(ClassDef c,Map<String,String> exact,String find,String repl,boolean matchCase,int[] stats){
  boolean any=false;List<Method> methods=new ArrayList<>();
  for(Method m:c.getMethods()){
   MethodImplementation impl=m.getImplementation();if(impl==null){methods.add(m);continue;}
   List<Instruction> insns=new ArrayList<>();boolean changed=false;
   for(Instruction ins:impl.getInstructions()){
    Instruction out=ins;
    if(ins instanceof ReferenceInstruction && (ins.getOpcode()==Opcode.CONST_STRING||ins.getOpcode()==Opcode.CONST_STRING_JUMBO)){
     Reference ref=((ReferenceInstruction)ins).getReference();
     if(ref instanceof StringReference){String old=((StringReference)ref).getString();String now=applyString(old,exact,find,repl,matchCase);
      if(now!=null&&!now.equals(old)){
       if(ins instanceof Instruction21c)out=new ImmutableInstruction21c(ins.getOpcode(),((Instruction21c)ins).getRegisterA(),new ImmutableStringReference(now));
       else if(ins instanceof Instruction31c)out=new ImmutableInstruction31c(ins.getOpcode(),((Instruction31c)ins).getRegisterA(),new ImmutableStringReference(now));
       else out=ins;
       if(out!=ins){changed=true;stats[0]++;}
      }}
    }
    insns.add(out);
   }
   if(changed){any=true;stats[1]++;methods.add(new ImmutableMethod(c.getType(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),new ImmutableMethodImplementation(impl.getRegisterCount(),insns,impl.getTryBlocks(),impl.getDebugItems())));}
   else methods.add(m);
  }
  return any?new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods):c;
 }
 private static String applyString(String s,Map<String,String> exact,String find,String repl,boolean matchCase){
  if(exact!=null){String r=exact.get(s);return r;}
  if(find==null||find.isEmpty())return null;
  if(matchCase)return s.replace(find,repl);
  return Pattern.compile(Pattern.quote(find),Pattern.CASE_INSENSITIVE).matcher(s).replaceAll(Matcher.quoteReplacement(repl));
 }
 private void reloadAll()throws Exception{
  dexes.clear();classes.clear();owners.clear();
  for(Map.Entry<String,File> e:files.entrySet()){DexBackedDexFile d=DexFileFactory.loadDexFile(e.getValue(),Opcodes.forApi(apiOf(e.getValue())));dexes.put(e.getKey(),d);for(ClassDef c:d.getClasses()){classes.put(c.getType(),c);owners.put(c.getType(),e.getKey());}}
  close();
 }
 private static int apiOf(File f)throws Exception{byte[] h=new byte[8];try(InputStream in=new FileInputStream(f)){if(in.read(h)<8)throw new IOException("不是有效的 DEX");}try{return VersionMap.mapDexVersionToApi(Integer.parseInt(new String(h,4,3,StandardCharsets.US_ASCII)));}catch(Exception e){return 28;}}
 private static String baseName(String n){int i=n.lastIndexOf('.');return i>0?n.substring(0,i):n;}
 private static File unusedFile(File d,String n){File f=new File(d,n);if(!f.exists())return f;String b=baseName(n),x=n.substring(b.length());for(int i=2;;i++){f=new File(d,b+"-"+i+x);if(!f.exists())return f;}}
 private static File unusedDir(File d,String n){File f=new File(d,n);if(!f.exists())return f;for(int i=2;;i++){f=new File(d,n+"-"+i);if(!f.exists())return f;}}
 private static int countFiles(File d,String suffix){int n=0;File[] fs=d.listFiles();if(fs==null)return 0;for(File f:fs){if(f.isDirectory())n+=countFiles(f,suffix);else if(f.getName().endsWith(suffix))n++;}return n;}
 public void close(){if(jadx!=null){jadx.close();jadx=null;}jadxMode="";}
}
