package com.cyberhub.manager;

import android.content.*;import android.content.pm.*;import android.os.*;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.text.*;import java.util.*;import java.util.zip.*;
import com.android.apksig.*;

public final class DataEngine {
 final MainActivity activity;final File workspace,cache,trash;final DexEngine dex;final ArscEngine arsc;final Map<String,Doc> documents=new HashMap<>();final Map<String,Map<String,byte[]>> archiveEdits=new HashMap<>();final Map<String,File> archiveOutputs=new HashMap<>();final Map<String,JSONObject> protectionCache=new LinkedHashMap<>();final LocalService services;final Translate translate;private long docCounter=0;
 static class Doc {File file;String archive,entry;Axml axml;long modified;}
 DataEngine(MainActivity a){activity=a;NetworkStorage.init(a.getFilesDir());workspace=new File(a.getFilesDir(),"工作区");workspace.mkdirs();cache=new File(a.getCacheDir(),"entries");cache.mkdirs();trash=new File(a.getFilesDir(),"recycle");trash.mkdirs();dex=new DexEngine(new File(a.getCacheDir(),"dex"));arsc=new ArscEngine(this);services=new LocalService(workspace);translate=new Translate(a);
  File welcome=new File(workspace,"欢迎.txt");if(!welcome.exists())try{FileOps.writeAtomic(welcome,("赛博中枢管家 0.1\n\n点「授予文件访问权限」即可浏览手机文件。\n轻点文件打开；长按进行复制、移动、重命名或移入回收站。\nAPK：查看内容、编辑清单、打开 DEX、查看 Java/Smali。\n编辑会创建备份或生成独立的已编辑归档。\nJava 和 Smali 均可编辑。保存会编译更改；「保存并签名 APK」会校验签名并保留备份。\n\n网络服务仅在本应用进程运行时可用。\n").getBytes(StandardCharsets.UTF_8),false);}catch(Exception ignored){}
 }
 boolean permission(){return Build.VERSION.SDK_INT>=30?Environment.isExternalStorageManager():activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
 synchronized Object call(String action,JSONObject a)throws Exception{
  switch(action){
   case "init":return new JSONObject().put("permission",permission()).put("home",Environment.getExternalStorageDirectory().getPath()).put("workspace",workspace.getPath()).put("version","0.1.11").put("ownApk",activity.getApplicationInfo().sourceDir);
   case "list":return list(a);
   case "disk":{File f=new File(a.getString("path"));return new JSONObject().put("total",f.getTotalSpace()).put("free",f.getUsableSpace());}
   case "exportText":{File f=FileOps.child(new File(a.getString("path")),a.getString("name"));if(f.exists())throw new IOException("输出已存在");FileOps.writeAtomic(f,a.getString("text").getBytes(StandardCharsets.UTF_8),false);record("已导出 "+f);return f.getPath();}
   case "read":return read(a);
   case "save":return save(a);
   case "pool":{byte[] b=FileOps.read(resolve(a),32L*1024*1024);List<String>ss=Axml.isBinary(b)?Axml.resourceStrings(b):Axml.resourceStrings(b);return new JSONArray(ss);}
   case "arscOpen":return arsc.open(resolve(a),a.optString("archive"),a.optString("entry"),a.optString("name"));
   case "arscBrowse":return arsc.browse(a.getString("session"),a.optInt("packageId",-1),a.optInt("typeId",-1),a.has("config")?a.getString("config"):null,a.optInt("offset",0),a.optInt("limit",200));
   case "arscEntry":return arsc.entry(a.getString("session"),a.getInt("id"),a.has("config")?a.getString("config"):null);
   case "arscEdit":return arsc.edit(a.getString("session"),a);
   case "arscSearch":return arsc.search(a.getString("session"),a.optString("mode","name"),a.optString("query",""),a.optInt("limit",200));
   case "arscPool":return arsc.pool(a.getString("session"),a.optInt("offset",0),a.optInt("limit",200),a.optString("query",""));
   case "arscSearchEx":return arsc.searchEx(a.getString("session"),a);
   case "arscTranslate":return arsc.translate(a.getString("session"),a.optString("config",""),a.optInt("offset",0),a.optInt("limit",100),a.optString("query",""));
   case "arscTranslateAdd":return arsc.translateAdd(a.getString("session"),a.getString("qualifiers"));
   case "arscQuery":return arsc.query(a.getString("session"),a.getString("query"));
   case "arscBackup":return arsc.backup(a.getString("session"));
   case "arscSave":{JSONObject r=arsc.save(a.getString("session"));record("已保存资源表 "+r.optString("path"));return r;}
   case "arscClose":arsc.close(a.getString("session"));return true;
   case "resolve":return resolve(a).getPath();
   case "translateConfig":{String op=a.optString("op","get");if(op.equals("set")){translate.saveConfig(a);return translate.config();}return new JSONObject().put("config",translate.config()).put("languages",translate.languages());}
   case "translate":return translate.translate(a);
   case "create":{File f=FileOps.child(new File(a.getString("path")),a.getString("name"));if(f.exists())throw new IOException("名称已存在");boolean restricted=RestrictedPaths.isRestricted(f,activity.getPackageName());if(a.optBoolean("folder")){if(restricted){if(!SafAccess.create(activity,f,true))throw new IOException("无法创建文件夹。请先授权 Android/data。");}else if(!f.mkdir())throw new IOException("无法创建文件夹");}else{if(restricted){if(!SafAccess.writeFile(activity,f,new byte[0],false))throw new IOException("无法创建文件。请先授权 Android/data。");}else FileOps.writeAtomic(f,new byte[0],false);}record("已创建 "+f);return f.getPath();}
   case "batchRename":{Object r=BatchFiles.rename(a.getJSONArray("files"));record("已重命名所选文件");return r;}
   case "batchTransfer":{JSONArray fs=a.getJSONArray("files");List<File>sources=new ArrayList<>();List<String>names=new ArrayList<>();boolean move=a.optBoolean("move");for(int i=0;i<fs.length();i++){JSONObject f=fs.getJSONObject(i);if(move&&!f.optString("archive").isEmpty())throw new IOException("移动前请先解压归档条目");sources.add(resolve(f));names.add(f.getString("name"));}Object r=BatchFiles.transfer(sources,names,new File(a.getString("destination")),move);record((move?"已移动 ":"已复制 ")+sources.size()+" 个所选文件");return r;}
   case "rename":{String archive=a.optString("archive");if(!archive.isEmpty()){File out=renameArchiveEntry(archive,a.getString("entry"),a.getString("name"));record("已重命名 "+a.getString("entry")+" → "+a.getString("name"));return out.getPath();}File old=new File(a.getString("path"));File dest=FileOps.child(old.getParentFile(),a.getString("name"));if(dest.exists())throw new IOException("名称已存在");if(RestrictedPaths.isRestricted(old,activity.getPackageName())){if(!SafAccess.rename(activity,old,a.getString("name")))throw new IOException("重命名失败。请先授权 Android/data。");}else Files.move(old.toPath(),dest.toPath());record("已重命名 "+old.getName()+" → "+dest.getName());return dest.getPath();}
   case "copy":case "move":{File src=resolve(a),dst=FileOps.child(new File(a.getString("destination")),src.getName());FileOps.copyTree(src,dst);if(action.equals("move"))FileOps.deleteTree(src);record((action.equals("move")?"已移动":"已复制")+" "+src+" → "+dst);return dst.getPath();}
   case "trash":return recycle(new File(a.getString("path")));
   case "recycleList":return recycleList();
   case "restore":{File meta=FileOps.child(trash,a.getString("id")+".json");JSONObject m=new JSONObject(new String(FileOps.read(meta,65536),StandardCharsets.UTF_8));File src=FileOps.child(trash,a.getString("id")),dst=new File(m.getString("path"));if(dst.exists())throw new IOException("原始路径已被占用");FileOps.copyTree(src,dst);FileOps.deleteTree(src);meta.delete();record("已还原 "+dst);return true;}
   case "purge":{FileOps.deleteTree(FileOps.child(trash,a.getString("id")));FileOps.child(trash,a.getString("id")+".json").delete();return true;}
   case "extract":{File f=resolve(a),dst=FileOps.child(new File(a.getString("destination")),a.getString("name"));FileOps.extract(f,dst);record("已解压 "+f.getName());return dst.getPath();}
   case "compress":{JSONObject r=ArchiveTools.create(a);JSONArray removed=new JSONArray(),failures=new JSONArray();if(a.optBoolean("deleteSources")){JSONArray paths=a.getJSONArray("paths");for(int i=0;i<paths.length();i++){String path=paths.getString(i);try{recycle(new File(path));removed.put(path);}catch(Exception e){failures.put(path+": "+e.getMessage());}}}r.put("removed",removed).put("cleanupErrors",failures);record("已创建归档 "+r.getJSONArray("outputs"));return r;}
   case "archiveExtract":{File src=resolve(a),dest=FileOps.child(new File(a.getString("destination")),a.getString("name"));ArchiveTools.extract(src,dest,a.optString("password"));record("已解压 "+src);return dest.getPath();}
   case "compare":return CompareTools.compare(new File(a.getString("left")),new File(a.getString("right")),a.getString("mode"));
   case "zip":{File f=new File(a.getString("path")),dst=FileOps.child(f.getParentFile(),a.getString("name"));FileOps.zip(f,dst);record("已归档 "+f.getName());return dst.getPath();}
   case "properties":{File f=resolve(a);return new JSONObject().put("path",f.getPath()).put("size",f.length()).put("modified",f.lastModified()).put("readable",f.canRead()).put("writable",f.canWrite()).put("sha256",f.isFile()?FileOps.sha256(f):"—");}
   case "apkInfo":return apkInfo(resolve(a));
   case "extractApk":return extractApk(a);
   case "bundleInspect":return ApksConverter.inspect(resolve(a));
   case "bundleConvert":{File src=resolve(a),dst=FileOps.child(new File(a.getString("destination")),a.getString("name"));try(InputStream key=activity.getAssets().open("development-signing.p12")){JSONObject result=ApksConverter.convert(src,dst,a.getString("choice"),a.getString("fingerprint"),new File(activity.getCacheDir(),"bundle-conversion"),key,message->activity.bundleProgress(a.optString("job"),message));record("已将 APKS 转换为 "+dst);return result;}}
   case "apkIcon":return iconForPath(a.getString("path"));
   case "clone":{File src=new File(a.getString("path")),parent=src.getParentFile().canWrite()?src.getParentFile():workspace,dst=FileOps.child(parent,strip(src.getName())+"_clone.apk");int suffix=2;while(dst.exists())dst=FileOps.child(parent,strip(src.getName())+"_clone-"+(suffix++)+".apk");boolean legacy=a.optBoolean("legacy",false);String newPackage=a.getString("newPackage");try(InputStream key=activity.getAssets().open("development-signing.p12")){ApkClone.clone(src,dst,newPackage,legacy,key);}record("已生成共存 "+dst.getName()+"（包名 "+newPackage+"）");return dst.getPath();}
   case "optimize":{File src=new File(a.getString("path")),parent=src.getParentFile().canWrite()?src.getParentFile():workspace,dst=FileOps.child(parent,strip(src.getName())+"_opt.apk");int suffix=2;while(dst.exists())dst=FileOps.child(parent,strip(src.getName())+"_opt-"+(suffix++)+".apk");boolean deep=a.optBoolean("deep",false);JSONArray del=a.optJSONArray("deleteFiles");Set<String> deleteFiles=del==null?null:new HashSet<>(new ArrayList<>());if(del!=null)for(int i=0;i<del.length();i++)deleteFiles.add(del.getString(i));JSONObject r=new JSONObject();if(deep){com.reandroid.apk.APKLogger logger=new com.reandroid.apk.APKLogger(){public void logMessage(String m){try{record("APK 优化："+m);}catch(Exception ignored){}}public void logError(String m,Throwable t){try{record("APK 优化："+m);}catch(Exception ignored){}}public void logVerbose(String m){}};com.cyberhub.manager.deepopt.DeepOptimizer.optimize(src,dst,deleteFiles,true,25,true,true,true,true,logger);}else{int lvl=a.optInt("ultraLevel",9);ApkOptimizer.Result opt=ApkOptimizer.optimize(src,dst,lvl,deleteFiles);r.put("removedFiles",opt.removedFiles);r.put("savedBytes",opt.savedBytes);}
    File signed=FileOps.child(dst.getParentFile(),dst.getName()+".signed");
    try(InputStream key=activity.getAssets().open("development-signing.p12")){ApkSigning.sign(dst,signed,key,true,true,true);}
    FileOps.replace(signed,dst);
    record("已优化 "+dst.getName()+(deep?"（深度）":""));return r.put("path",dst.getPath()).put("size",dst.length()).put("deep",deep);}
   case "sign":{File src=new File(a.getString("path")),parent=src.getParentFile().canWrite()?src.getParentFile():workspace,dst=FileOps.child(parent,strip(src.getName())+"_sign.apk");int suffix=2;while(dst.exists())dst=FileOps.child(parent,strip(src.getName())+"_sign-"+(suffix++)+".apk");String scheme=a.optString("scheme","v1v2v3");boolean v1=scheme.contains("1"),v2=scheme.contains("2"),v3=scheme.contains("3");if(!v1&&!v2&&!v3){v1=true;v2=true;v3=true;}sign(src,dst,v1,v2,v3);record("已签名 "+dst.getName());return dst.getPath();}
   case "protection":return protection(apkSource(a));
   case "apkXmlReplace":{File src=apkSource(a),dst=outputApk(src,"_xmlrep");JSONObject stats=ApkXmlTools.batchReplace(src,dst,a);record("XML 批量替换："+stats.optInt("hits")+" 处 / "+stats.optInt("files")+" 个文件");return publishApk(dst).put("stats",stats);}
   case "apkXmlTranslate":{File src=apkSource(a),dst=outputApk(src,"_xmltrans");JSONObject stats=ApkXmlTools.translate(src,dst,translate,a);record("XML 翻译："+stats.optInt("translated")+" 条 / "+stats.optInt("files")+" 个文件");return publishApk(dst).put("stats",stats);}
   case "sigScan":return SignatureBypass.scan(apkSource(a),a);
   case "sigBypass":{File src=apkSource(a),dst=outputApk(src,"_nosig");JSONObject r=SignatureBypass.apply(src,dst,a);record("已去除签名校验："+r.optInt("patched")+" 个方法");return publishApk(dst).put("patched",r.optInt("patched")).put("methods",r.optJSONArray("methods"));}
   case "injectLog":{File src=apkSource(a),dst=outputApk(src,"_log");JSONObject r=DexTools.injectLog(src,dst,a);record("已注入日志："+r.optInt("injected")+" 个方法");return publishApk(dst).put("injected",r.optInt("injected")).put("methods",r.optJSONArray("methods"));}
   case "injectProvider":{File src=apkSource(a),dst=outputApk(src,"_fp");JSONObject r=FileProviderInjector.inject(src,dst,a);record("已注入文件提供器："+r.optString("authority"));return publishApk(dst).put("authority",r.optString("authority")).put("uri",r.optString("uri"));}
   case "resSlim":{File src=apkSource(a),dst=outputApk(src,"_slim");JSONObject r=ResTools.slim(src,dst,a);record("RES 精简：条目 "+r.optInt("entries")+"，配置 "+r.optInt("configs")+"，文件 "+r.optInt("files"));return publishApk(dst).put("stats",r);}
   case "resObfuscate":{File src=apkSource(a),dst=outputApk(src,"_resobf");JSONObject r=ResTools.obfuscate(src,dst,a);record("RES 混淆："+r.optInt("entries")+" 个资源名");return publishApk(dst).put("stats",r);}
   case "resDeobfuscate":{File src=apkSource(a),dst=outputApk(src,"_resdeobf");JSONObject r=ResTools.deobfuscate(src,dst,a);record("RES 反混淆："+r.optInt("entries")+" 个资源名");return publishApk(dst).put("stats",r);}
   case "resDedupe":{File src=apkSource(a),dst=outputApk(src,"_dedupe");JSONObject r=ResTools.dedupe(src,dst);record("数据复用：合并 "+r.optInt("removed")+" 个重复资源");return publishApk(dst).put("stats",r);}
   case "dexRepartition":{File src=apkSource(a),dst=outputApk(src,"_split");JSONObject r=DexTools.repartition(src,dst,a);record("DEX 重新划分："+r.optInt("dexes")+" 个 DEX");return publishApk(dst).put("stats",r);}
   case "dexDecrypt":{File f=new File(a.getString("path"));DexStringDecryptor.Report r=DexStringDecryptor.run(f,a.optString("custom",""),new DexStringDecryptor.Report());record("DEX 字符串解密：候选 "+r.candidates+"，调用点 "+r.callSites+"，解出 "+r.decrypted);JSONObject o=new JSONObject();o.put("candidates",r.candidates);o.put("callSites",r.callSites);o.put("decrypted",r.decrypted);o.put("skipped",r.skipped);JSONArray s=new JSONArray();for(String x:r.samples)s.put(x);o.put("samples",s);JSONArray er=new JSONArray();for(String x:r.errors)er.put(x);o.put("errors",er);return o;}
   case "dexDeobfuscate":{File f=new File(a.getString("path"));List<String> targetList=new ArrayList<>();if(a.has("targetClasses")){JSONArray cl=a.getJSONArray("targetClasses");for(int i=0;i<cl.length();i++)targetList.add(cl.getString(i));}DexDeobfuscator.Report r=DexDeobfuscator.run(f,a.optString("custom",""),targetList,new DexDeobfuscator.Report());File out=new File(f.getParentFile(),f.getName().replaceFirst("(\\.apk|\\.dex)$","_deobf$1"));if(r.dexCount>0&&out.isFile()){JSONObject p=publishApk(out);record("DEX 混淆对抗：类 "+r.classesRenamed+"，方法 "+r.methodsRenamed+"，字段 "+r.fieldsRenamed);return p.put("classesRenamed",r.classesRenamed).put("methodsRenamed",r.methodsRenamed).put("fieldsRenamed",r.fieldsRenamed).put("renamedClasses",new JSONArray(r.renamedClasses)).put("errors",new JSONArray(r.errors));}JSONObject o=new JSONObject();o.put("classesRenamed",r.classesRenamed);o.put("methodsRenamed",r.methodsRenamed);o.put("fieldsRenamed",r.fieldsRenamed);o.put("renamedClasses",new JSONArray(r.renamedClasses));o.put("errors",new JSONArray(r.errors));o.put("output",out.getName());return o;}
   case "dexFiles":return dexFiles(resolve(a));
   case "dexProperties":return dex.properties();
   case "dexRepair":{JSONObject r=dex.repair();record("已修复 DEX："+r.getInt("count")+" 个");return r;}
   case "dexToSmali":{File out=new File(a.getString("destination"));JSONObject r=dex.toSmali(out);record("已导出 Smali："+r.getString("path"));return r;}
   case "dexToJar":{File out=new File(a.getString("destination"));JSONObject r=dex.toJar(out);record("已生成 JAR："+r.getString("path"));return r;}
   case "dexDirectProperties":{File src=resolve(a);DexEngine t=dex.scratch(src,new JSONArray().put(dexEntryName(a,src)));try{JSONObject r=t.properties().put("path",dexLabel(a,src));return r;}finally{t.dispose();}}
   case "dexDirectRepair":{File src=resolve(a);String name=dexEntryName(a,src);DexEngine t=dex.scratch(src,new JSONArray().put(name));try{JSONObject r=t.repair();byte[] data=FileOps.read(t.files.get(name),256L*1024*1024);JSONObject p=publishDex(a,name,data);record("已修复 DEX："+dexLabel(a,src));return r.put("path",p.getString("path")).put("backup",p.getString("backup")).put("signed",p.getBoolean("signed"));}finally{t.dispose();}}
   case "dexDirectToSmali":{File src=resolve(a);File out=new File(a.getString("destination"));DexEngine t=dex.scratch(src,new JSONArray().put(dexEntryName(a,src)));try{JSONObject r=t.toSmali(out).put("source",dexLabel(a,src));record("已导出 Smali："+r.getString("path"));return r;}finally{t.dispose();}}
   case "dexDirectToJar":{File src=resolve(a);File out=new File(a.getString("destination"));DexEngine t=dex.scratch(src,new JSONArray().put(dexEntryName(a,src)));try{JSONObject r=t.toJar(out).put("source",dexLabel(a,src));record("已生成 JAR："+r.getString("path"));return r;}finally{t.dispose();}}
   case "dexDirectReplaceStrings":{File src=resolve(a);String name=dexEntryName(a,src);DexEngine t=dex.scratch(src,new JSONArray().put(name));try{JSONObject r=t.replaceStrings(a).put("path",dexLabel(a,src));if(r.optInt("replaced",0)==0)return r.put("changed",false);byte[] data=FileOps.read(t.files.get(name),256L*1024*1024);JSONObject p=publishDex(a,name,data);record("已替换 DEX 字符串 "+r.getInt("replaced")+" 处");return r.put("changed",true).put("backup",p.getString("backup")).put("signed",p.getBoolean("signed"));}finally{t.dispose();}}
   case "dexMerge":{File out=new File(a.getString("destination"));JSONObject r=dex.mergeDex(a,out);record("已合并 DEX："+r.getInt("classes")+" 个类");return r;}
   case "dexReplaceStrings":{JSONObject r=dex.replaceStrings(a);record("已替换 DEX 字符串 "+r.getInt("replaced")+" 处");return r;}
   case "dexOpen":return dex.open(resolve(a),a.getJSONArray("entries"));
   case "dexTree":return dex.tree(a.optString("prefix"));
   case "dexCode":return dex.code(a.getString("class"),a.optString("mode","smali"),a.optString("decompiler","auto"));
   case "dexSave":{Object out=dex.save(a.getString("class"),a.getString("text"));record("已保存 Smali "+a.getString("class"));return out;}
   case "dexJavaSave":{File api=new File(activity.getCacheDir(),"compiler-api-34.jar");if(!api.isFile())try(InputStream in=activity.getAssets().open("compiler/android-api.jar")){FileOps.writeAtomic(api,FileOps.bytes(in,16L*1024*1024),false);}Object out=dex.saveJava(a.getString("class"),a.getString("text"),api);record("已编译 Java "+a.getString("class"));return out;}
   case "dexExport":{File output=new File(a.getString("destination"));Object out=dex.export(output);record("已导出 DEX 更改");return out;}
   case "dexQuery":return dex.query(a);
   case "dexSaveSign":{try(InputStream key=activity.getAssets().open("development-signing.p12")){JSONObject r=dex.commit(key);record("已保存、签名并校验 "+r.getString("path"));return r;}}
   case "dexCommit":{JSONObject r=dex.commit();record("已保存 DEX 工程到 "+r.getString("path"));return r;}
   case "restoreBackup":{File f=FileOps.restoreBackup(new File(a.getString("path")));record("已还原备份 "+f);return f.getPath();}
   case "dexStrings":return dex.strings(a.optString("query"));
   case "dexSearch":return dex.search(a.optString("query"));
   case "apps":return apps();
   case "log":return new JSONArray(activity.getPreferences(0).getString("log","[]"));
   case "plugins":return plugins(a);
   case "service":return services.action(a);
   case "ftp":return FtpTools.call(a);
   case "netStorage":return NetworkStorage.call(a);
   default:throw new IOException("未知操作："+action);
  }
 }
  File resolve(JSONObject a)throws Exception{
   String archive=a.optString("archive");if(archive.isEmpty())return new File(a.getString("path"));String entry=a.getString("entry");String token=Integer.toHexString((archive+"|"+entry).hashCode());File dir=new File(cache,token);dir.mkdirs();File f=FileOps.child(dir,new File(entry).getName());byte[] bytes=FileOps.zipEntry(new File(archive),entry,256L*1024*1024);FileOps.writeAtomic(f,bytes,false);return f;
  }
  static String strip(String name){int p=name.lastIndexOf('.');return p>0?name.substring(0,p):name;}
  File apkSource(JSONObject a)throws Exception{File f=resolve(a);if(!f.isFile()||!f.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))throw new IOException("请选择一个 APK 文件");return f;}
  File outputApk(File source,String suffix)throws Exception{
   File parent=source.getParentFile();
   if(parent==null||!parent.canWrite())parent=workspace;
   String base=strip(source.getName())+suffix;
   File dst=FileOps.child(parent,base+".apk");
   int n=2;
   while(dst.exists())dst=FileOps.child(parent,base+"-"+n+".apk");
   return dst;
  }
  JSONObject publishApk(File rewritten)throws Exception{
   File signed=new File(rewritten.getPath()+".signed");
   try{
    try(InputStream key=activity.getAssets().open("development-signing.p12")){ApkSigning.sign(rewritten,signed,key);}
    FileOps.replace(signed,rewritten);
    return new JSONObject().put("path",rewritten.getPath()).put("size",rewritten.length()).put("signed",true);
   }catch(Exception error){
    signed.delete();
    if(rewritten.exists())rewritten.delete();
    throw error;
   }
  }
 JSONArray list(JSONObject a)throws Exception{
  JSONArray out=new JSONArray();String archive=a.optString("archive");if(!archive.isEmpty()){
   String prefix=a.optString("entry");if(!prefix.isEmpty()&&!prefix.endsWith("/"))prefix+="/";Map<String,JSONObject>items=new TreeMap<>();
   try(ZipFile z=new ZipFile(archive)){Enumeration<? extends ZipEntry>en=z.entries();while(en.hasMoreElements()){ZipEntry e=en.nextElement();String name=e.getName();if(!name.startsWith(prefix)||name.equals(prefix))continue;String rest=name.substring(prefix.length());int slash=rest.indexOf('/');boolean dir=slash>=0;String shortName=dir?rest.substring(0,slash):rest;if(shortName.isEmpty())continue;items.put(shortName,new JSONObject().put("name",shortName).put("dir",dir).put("size",dir?0:e.getSize()).put("time",e.getTime()).put("archive",archive).put("entry",prefix+shortName+(dir?"/":"")));}}
   List<JSONObject>vals=new ArrayList<>(items.values());vals.sort((x,y)->Boolean.compare(y.optBoolean("dir"),x.optBoolean("dir")));for(JSONObject v:vals)out.put(v);
  }else{File dir=new File(a.getString("path"));boolean restrictedRoot=RestrictedPaths.isRestrictedRoot(dir);if(RestrictedPaths.isRestricted(dir,activity.getPackageName())&&!restrictedRoot){JSONArray saf=SafAccess.listDetailed(activity,dir,a.optBoolean("hidden"));if(saf!=null){long free=dir.getUsableSpace(),total=dir.getTotalSpace();for(int i=0;i<saf.length();i++)saf.getJSONObject(i).put("free",free).put("total",total);return saf;}JSONArray elevated=ElevatedAccess.listDetailed(activity,dir,a.optBoolean("hidden"));if(elevated!=null){long free=dir.getUsableSpace(),total=dir.getTotalSpace();for(int i=0;i<elevated.length();i++)elevated.getJSONObject(i).put("free",free).put("total",total);return elevated;}if(!ElevatedAccess.hasChannel(activity))throw new IOException("Android/data 受系统保护。请到「设置 → 启动 → 授权 Android/data」选择该文件夹（无需 Root），或授予 Shizuku/Root 权限后重试。");}File[] files=dir.listFiles();if(files==null){File[] elevated=ElevatedAccess.listFiles(activity,dir);if(elevated!=null)files=elevated;}if(files==null||files.length==0){if(dir.getPath().equals("/")){String[] names={"data","etc","mnt","proc","product","sdcard","storage","system","system_ext","vendor"};List<File> found=new ArrayList<>();for(String n:names){File f=new File(dir,n);if(f.exists())found.add(f);}files=found.toArray(new File[0]);}if(files==null||files.length==0)throw new IOException(RestrictedPaths.isRestricted(dir,activity.getPackageName())?"无法读取 Android/data。请确认 Shizuku 或 Root 已授权，并允许其访问该目录。":"无法读取此文件夹。请授予文件访问权限，或选择一个可访问的目录。");}Arrays.sort(files,(x,y)->x.isDirectory()!=y.isDirectory()?(x.isDirectory()?-1:1):x.getName().compareToIgnoreCase(y.getName()));long free=dir.getUsableSpace(),total=dir.getTotalSpace();for(File f:files){if(!a.optBoolean("hidden")&&f.getName().startsWith("."))continue;out.put(new JSONObject().put("name",f.getName()).put("path",f.getPath()).put("dir",(restrictedRoot||f.isDirectory())).put("size",f.length()).put("time",f.lastModified()).put("free",free).put("total",total));}}
  return out;
 }
 JSONObject read(JSONObject a)throws Exception{
  File f=resolve(a);byte[] b;if(RestrictedPaths.isRestricted(f,activity.getPackageName())){b=SafAccess.readFile(activity,f,FileOps.MAX_TEXT);if(b==null)b=ElevatedAccess.readFile(activity,f,FileOps.MAX_TEXT);if(b==null)throw new IOException("无法读取 Android/data 文件。请到「设置 → 启动 → 授权 Android/data」选择该文件夹（无需 Root），或授予 Shizuku/Root 权限后重试。");}else b=FileOps.read(f,FileOps.MAX_TEXT);Doc d=new Doc();d.file=f;d.archive=a.optString("archive");d.entry=a.optString("entry");d.modified=f.lastModified();String text;
  if(Axml.isBinary(b)){d.axml=new Axml();text=d.axml.decode(b);}else{for(int n=0;n<Math.min(b.length,2048);n++)if(b[n]==0)throw new IOException("二进制文件：请使用 APK、DEX 或资源工具");text=new String(b,StandardCharsets.UTF_8);}
  String id="doc"+(++docCounter);documents.put(id,d);return new JSONObject().put("id",id).put("text",text).put("name",f.getName()).put("path",f.getPath()).put("binaryXml",d.axml!=null);
 }
 JSONObject save(JSONObject a)throws Exception{
  Doc d=documents.get(a.getString("id"));if(d==null)throw new IOException("文档会话已过期");if(d.file.lastModified()!=d.modified)throw new IOException("文件已在外部被修改；保存前请重新加载");String text=a.getString("text");byte[] bytes=d.axml==null?text.getBytes(StandardCharsets.UTF_8):d.axml.encode(text);if(RestrictedPaths.isRestricted(d.file,activity.getPackageName())){if(!SafAccess.writeFile(activity,d.file,bytes,a.optBoolean("backup",true))&&!ElevatedAccess.writeFile(activity,d.file,bytes,a.optBoolean("backup",true)))throw new IOException("写入 Android/data 失败。请重新授权「设置 → 启动 → 授权 Android/data」，或确认 Shizuku/Root 拥有写权限。");}else FileOps.writeAtomic(d.file,bytes,a.optBoolean("backup",true));d.modified=d.file.lastModified();File output=d.file;
  if(!d.archive.isEmpty()){
   Map<String,byte[]>edits=archiveEdits.computeIfAbsent(d.archive,k->new HashMap<>());edits.put(d.entry,bytes);output=archiveOutput(d.archive);
   FileOps.rewriteZip(new File(d.archive),output,edits);
  }record("已保存 "+output);return new JSONObject().put("path",output.getPath()).put("needsSigning",output.getName().endsWith(".apk"));
 }
 File archiveSave(String archive,String entry,byte[] bytes)throws Exception{
  Map<String,byte[]>edits=archiveEdits.computeIfAbsent(archive,k->new HashMap<>());edits.put(entry,bytes);File output=archiveOutput(archive);
  FileOps.rewriteZip(new File(archive),output,edits);return output;
 }
 /** Edited copy of an archive; every edit and rename of the same archive lands in one file. */
 File archiveOutput(String archive){
  File output=archiveOutputs.get(archive);
  if(output==null){File src=new File(archive);File parent=src.getParentFile()!=null&&src.getParentFile().canWrite()?src.getParentFile():workspace;String ext=src.getName().endsWith(".apk")?".apk":".zip";output=new File(parent,strip(src.getName())+"-edited"+ext);int n=2;while(output.exists())output=new File(parent,strip(src.getName())+"-edited-"+(n++)+ext);archiveOutputs.put(archive,output);}
  return output;
 }
 /** Renames one entry inside an archive; the original archive is kept and an edited copy is written. */
 File renameArchiveEntry(String archive,String entry,String name)throws Exception{
  File src=new File(archive);
  if(!src.isFile())throw new IOException("找不到归档文件");
  if(name==null||name.trim().isEmpty())throw new IOException("名称不能为空");
  if(name.contains("/")||name.contains("\\"))throw new IOException("名称不能包含斜杠");
  boolean isDir=entry.endsWith("/");
  String parent=entry.contains("/")?entry.substring(0,entry.lastIndexOf('/')+1):"";
  String newBase=parent+name+(isDir?"/":"");
  Map<String,String> renames=new LinkedHashMap<>();
  try(ZipFile z=new ZipFile(src)){
   if(z.getEntry(newBase)!=null)throw new IOException("名称已存在");
   Enumeration<?extends ZipEntry>en=z.entries();
   while(en.hasMoreElements()){ZipEntry e=en.nextElement();String n=e.getName();if(n.equals(entry)||n.startsWith(entry))renames.put(n,newBase+n.substring(entry.length()));}
  }
  if(renames.isEmpty())throw new IOException("归档内未找到该条目");
  File output=archiveOutput(archive);
  Map<String,byte[]>edits=archiveEdits.computeIfAbsent(archive,k->new HashMap<>());
  FileOps.rewriteZip(src,output,edits,renames);
  return output;
 }
 String recycle(File f)throws Exception{
  if(!f.exists())throw new IOException("文件已不存在");String id=UUID.randomUUID().toString();File target=new File(trash,id);long bytes=treeSize(f);FileOps.copyTree(f,target);JSONObject meta=new JSONObject().put("path",f.getCanonicalPath()).put("name",f.getName()).put("time",System.currentTimeMillis()).put("id",id).put("dir",f.isDirectory()).put("size",bytes);FileOps.writeAtomic(new File(trash,id+".json"),meta.toString().getBytes(StandardCharsets.UTF_8),false);FileOps.deleteTree(f);record("已移入回收站 "+f.getName());return id;
 }
 JSONArray recycleList()throws Exception{JSONArray arr=new JSONArray();File[] fs=trash.listFiles();if(fs==null)return arr;List<JSONObject> rows=new ArrayList<>();for(File f:fs){if(!f.getName().endsWith(".json"))continue;JSONObject o=new JSONObject(new String(FileOps.read(f,65536),StandardCharsets.UTF_8));File item=new File(trash,o.optString("id"));if(!o.has("dir"))o.put("dir",item.isDirectory());if(!o.has("size"))o.put("size",item.exists()?treeSize(item):0);rows.add(o);}rows.sort((x,y)->Long.compare(y.optLong("time"),x.optLong("time")));for(JSONObject o:rows)arr.put(o);return arr;}
 static long treeSize(File f){if(f.isFile())return f.length();File[] fs=f.listFiles();long n=0;if(fs!=null)for(File c:fs)n+=treeSize(c);return n;}
 String extractApk(JSONObject a)throws Exception{
  File src=resolve(a);
  if(!src.isFile()||!src.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))throw new IOException("源文件不是 APK");
  File dir=new File(a.getString("destination"));
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("无法创建目标文件夹："+dir);
  String name=a.optString("name","").trim();
  if(name.isEmpty())name=src.getName();
  if(!name.toLowerCase(Locale.ROOT).endsWith(".apk"))name+=".apk";
  File dst=FileOps.child(dir,name);
  if(dst.exists())throw new IOException("目标已存在："+dst.getName());
  FileOps.copyTree(src,dst);
  record("已提取 APK "+dst.getName());
  return dst.getPath();
 }
 JSONObject apkInfo(File f)throws Exception{
  PackageManager pm=activity.getPackageManager();PackageInfo p=pm.getPackageArchiveInfo(f.getPath(),PackageManager.GET_PERMISSIONS);if(p==null)throw new IOException("Android 无法解析此 APK");p.applicationInfo.sourceDir=f.getPath();p.applicationInfo.publicSourceDir=f.getPath();String sig="未签名 / 未校验";
  try{ApkVerifier.Result v=new ApkVerifier.Builder(f).build().verify();List<String>s=new ArrayList<>();if(v.isVerifiedUsingV1Scheme())s.add("V1");if(v.isVerifiedUsingV2Scheme())s.add("V2");if(v.isVerifiedUsingV3Scheme())s.add("V3");sig=v.isVerified()?String.join(" + ",s):"签名无效或未签名";}catch(Exception e){sig="无法校验："+e.getMessage();}
   JSONObject protection=protection(f);
   return new JSONObject().put("icon",appIcon(p.applicationInfo)).put("name",String.valueOf(p.applicationInfo.loadLabel(pm))).put("package",p.packageName).put("version",p.versionName).put("versionCode",Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode).put("size",f.length()).put("signature",sig).put("minSdk",p.applicationInfo.minSdkVersion).put("targetSdk",p.applicationInfo.targetSdkVersion).put("path",f.getPath()).put("protection",protection).put("protectionLevel",protection.optString("level","未知"));
  }
  JSONObject protection(File f)throws Exception{
   String key=f.getPath()+"|"+f.length()+"|"+f.lastModified();
   JSONObject cached=protectionCache.get(key);
   if(cached!=null)return cached;
   JSONObject value;
   try{value=ApkProtection.analyze(f);}catch(Exception error){value=new JSONObject().put("level","未知").put("score",0).put("findings",new JSONArray().put(new JSONObject().put("severity","warn").put("text","评估失败："+error.getMessage())));}
   protectionCache.put(key,value);
   while(protectionCache.size()>8)protectionCache.remove(protectionCache.keySet().iterator().next());
   return value;
  }
 String iconForPath(String path){PackageInfo info=activity.getPackageManager().getPackageArchiveInfo(path,0);if(info==null||info.applicationInfo==null)return "";info.applicationInfo.sourceDir=path;info.applicationInfo.publicSourceDir=path;return appIcon(info.applicationInfo);}
 String appIcon(ApplicationInfo info){try{android.graphics.drawable.Drawable drawable=info.loadIcon(activity.getPackageManager());android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(96,96,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);drawable.setBounds(0,0,96,96);drawable.draw(canvas);ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);bitmap.recycle();return "data:image/png;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);}catch(Exception e){return "";}}
 void sign(File src,File dst)throws Exception{sign(src,dst,true,true,true);}
  void sign(File src,File dst,boolean v1,boolean v2,boolean v3)throws Exception{try(InputStream key=activity.getAssets().open("development-signing.p12")){ApkSigning.sign(src,dst,key,v1,v2,v3);}}
 JSONArray dexFiles(File f)throws Exception{JSONArray a=new JSONArray();if(f.getName().endsWith(".dex"))return a.put(f.getName());try(ZipFile z=new ZipFile(f)){Enumeration<? extends ZipEntry>en=z.entries();while(en.hasMoreElements()){String n=en.nextElement().getName();if(n.matches("classes[0-9]*\\.dex"))a.put(n);}}return a;}
   static String dexEntryName(JSONObject a,File src)throws Exception{return a.optString("archive").isEmpty()?src.getName():a.getString("entry");}
   static String dexLabel(JSONObject a,File src){String archive=a.optString("archive");return archive.isEmpty()?src.getPath():archive+"!"+a.optString("entry");}
  JSONObject publishDex(JSONObject a,String entry,byte[] data)throws Exception{
   String archive=a.optString("archive");
   if(archive.isEmpty()){
    File original=new File(a.getString("path"));if(!original.isFile())throw new IOException("找不到 DEX 文件："+original.getName());
    File prepared=File.createTempFile(".dex-direct-",".tmp",original.getAbsoluteFile().getParentFile());
    try{FileOps.writeAtomic(prepared,data,false);File backup=FileOps.commitWithBackup(prepared,original);return new JSONObject().put("path",original.getPath()).put("backup",backup.getPath()).put("signed",false);}
    finally{prepared.delete();}
   }
   File original=new File(archive);if(!original.isFile())throw new IOException("找不到 APK 文件："+original.getName());
   File rewritten=File.createTempFile(".apk-direct-",".tmp",original.getAbsoluteFile().getParentFile());
   File signed=new File(rewritten.getPath()+".signed.apk");
   try{
    Map<String,byte[]> edits=new HashMap<>();edits.put(a.getString("entry"),data);
    FileOps.rewriteZip(original,rewritten,edits);
    try(InputStream key=activity.getAssets().open("development-signing.p12")){ApkSigning.sign(rewritten,signed,key);}
    File backup=FileOps.commitWithBackup(signed,original);
    return new JSONObject().put("path",original.getPath()).put("backup",backup.getPath()).put("signed",true);
   }finally{rewritten.delete();signed.delete();}
  }
 JSONArray apps()throws Exception{JSONArray a=new JSONArray();List<ApplicationInfo>apps=activity.getPackageManager().getInstalledApplications(0);apps.sort((x,y)->String.valueOf(x.loadLabel(activity.getPackageManager())).compareToIgnoreCase(String.valueOf(y.loadLabel(activity.getPackageManager()))));for(ApplicationInfo p:apps)a.put(new JSONObject().put("name",String.valueOf(p.loadLabel(activity.getPackageManager()))).put("package",p.packageName).put("path",p.sourceDir));return a;}
 void record(String text){try{JSONArray old=new JSONArray(activity.getPreferences(0).getString("log","[]"));JSONArray next=new JSONArray();next.put(new JSONObject().put("text",text).put("time",System.currentTimeMillis()));for(int i=0;i<Math.min(199,old.length());i++)next.put(old.get(i));activity.getPreferences(0).edit().putString("log",next.toString()).apply();}catch(Exception ignored){}}
 String terminal(String command,String path)throws Exception{if(command.trim().isEmpty())return "";File dir=new File(path);if(!dir.isDirectory())throw new IOException("请先选择一个普通文件夹");java.lang.Process p=new ProcessBuilder("/system/bin/sh","-c",command).directory(dir).redirectErrorStream(true).start();java.util.concurrent.ExecutorService one=java.util.concurrent.Executors.newSingleThreadExecutor();java.util.concurrent.Future<byte[]>future=one.submit(()->FileOps.bytes(p.getInputStream(),1024*1024));try{if(!p.waitFor(20,java.util.concurrent.TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("命令在 20 秒后超时");}String result=new String(future.get(2,java.util.concurrent.TimeUnit.SECONDS),StandardCharsets.UTF_8);record("终端命令完成（退出码 "+p.exitValue()+")");return result+"\n[退出码 "+p.exitValue()+"]";}finally{p.destroy();one.shutdownNow();}}
 Object plugins(JSONObject a)throws Exception{File dir=new File(workspace,"Plugins");dir.mkdirs();String op=a.optString("op","list");if(op.equals("import")){File src=new File(a.getString("path"));if(!src.getName().endsWith(".sh"))throw new IOException("插件必须是 .sh 脚本");FileOps.copyTree(src,FileOps.child(dir,src.getName()));record("已导入脚本插件 "+src.getName());return true;}if(op.equals("run")){File f=FileOps.child(dir,a.getString("name"));return terminal("/system/bin/sh '"+f.getPath().replace("'","'\\''")+"'",workspace.getPath());}if(op.equals("remove")){FileOps.deleteTree(FileOps.child(dir,a.getString("name")));return true;}JSONArray arr=new JSONArray();File[]fs=dir.listFiles();if(fs!=null)for(File f:fs)if(f.getName().endsWith(".sh"))arr.put(f.getName());return arr;}
 void close(){services.stop();dex.close();}
}
