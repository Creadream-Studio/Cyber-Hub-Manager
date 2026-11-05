package com.cyberhub.manager;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.sevenz.*;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.compress.compressors.bzip2.*;
import org.apache.commons.compress.compressors.lz4.*;
import org.tukaani.xz.*;
import com.github.luben.zstd.*;
import net.lingala.zip4j.model.*;
import net.lingala.zip4j.model.enums.*;
import net.lingala.zip4j.io.outputstream.*;

/** Streaming archive operations. Output files are published only after writers close. */
public final class ArchiveTools {
 private ArchiveTools(){}
 static final List<String> FORMATS=Arrays.asList("zip","7z","tar","tar.gz","tar.xz","tar.zst","tar.bz2","tar.lz4","gzip");
 static class Item{File file;String name;Item(File f,String n){file=f;name=n;}}
 static void walk(File f,String name,List<Item> out,long[] size)throws Exception{
  if(Files.isSymbolicLink(f.toPath()))throw new IOException("符号链接不会被归档");
  if(!f.exists())throw new IOException("源文件缺失："+f);
  if(++size[1]>100000)throw new IOException("源条目过多");
  out.add(new Item(f,name+(f.isDirectory()?"/":"")));
  if(f.isDirectory()){File[]fs=f.listFiles();if(fs==null)throw new IOException("无法读取 "+f);Arrays.sort(fs);for(File c:fs)walk(c,name+"/"+c.getName(),out,size);}
  else{size[0]+=f.length();if(size[0]>FileOps.MAX_EXTRACT)throw new IOException("归档源超过 1 GB 上限");}
 }
 static int level(String s){switch(s){case "store":return 0;case "fastest":return 1;case "fast":return 3;case "normal":return 5;case "maximum":return 7;case "ultra":return 9;case "apk":return 9;default:throw new IllegalArgumentException("未知的压缩级别");}}
 public static JSONObject create(JSONObject a)throws Exception{
  String format=a.getString("format"),name=a.getString("name"),password=a.optString("password"),levelName=a.optString("level","normal");
  if(!FORMATS.contains(format))throw new IOException("不支持的归档格式");int level=level(levelName);
  if(!password.isEmpty()&&!format.equals("zip")&&!format.equals("7z"))throw new IOException("密码加密仅支持 ZIP 和 7z");
  if(levelName.equals("apk")&&!format.equals("zip"))throw new IOException("APK 模式需要 ZIP 格式");
  File folder=new File(a.getString("destination")),dest=FileOps.child(folder,name);if(!folder.isDirectory())throw new IOException("请选择一个已存在的输出文件夹");if(dest.exists())throw new IOException("输出已存在");
  long split=a.optLong("splitBytes");if(split!=0&&(split<65536||split>FileOps.MAX_EXTRACT))throw new IOException("分卷长度必须在 64 KB–1 GB 之间");
  if(levelName.equals("apk")&&(!password.isEmpty()||split>0))throw new IOException("APK 模式不支持密码或分卷输出");
  JSONArray paths=a.getJSONArray("paths");if(paths.length()==0)throw new IOException("请选择源文件");List<File>sources=new ArrayList<>();Set<String>names=new HashSet<>();List<Item>items=new ArrayList<>();long[]stats={0,0};
  for(int i=0;i<paths.length();i++){File f=new File(paths.getString(i));if(!names.add(f.getName()))throw new IOException("顶层源名称重复");if(dest.getCanonicalFile().toPath().startsWith(f.getCanonicalFile().toPath()))throw new IOException("归档不能位于其源文件夹内部");sources.add(f);walk(f,f.getName(),items,stats);}
  if(format.equals("gzip")&&(items.size()!=1||!items.get(0).file.isFile()))throw new IOException("gzip 只能接受单个文件。多个文件/文件夹请改用 tar.gz。");
  File stage=new File(folder,".cyber-archive-"+UUID.randomUUID());if(!stage.mkdir())throw new IOException("无法创建输出暂存文件夹");List<File>published=new ArrayList<>();
  try{
   File output=new File(stage,name);
   if(format.equals("zip"))writeZip(items,output,password,level,levelName.equals("apk"),split);
   else if(format.equals("7z"))write7z(items,output,password,level);
   else if(format.equals("gzip")){try(OutputStream out=compress(new FileOutputStream(output),format,level);InputStream in=new FileInputStream(items.get(0).file)){FileOps.copy(in,out,FileOps.MAX_EXTRACT);}}
   else{try(OutputStream compressed=compress(new FileOutputStream(output),format,level);TarArchiveOutputStream out=new TarArchiveOutputStream(compressed)){
    out.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);out.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
    for(Item item:items){TarArchiveEntry entry=new TarArchiveEntry(item.file,item.name);out.putArchiveEntry(entry);if(item.file.isFile())try(InputStream in=new FileInputStream(item.file)){FileOps.copy(in,out,FileOps.MAX_EXTRACT);}out.closeArchiveEntry();}
   }}
   if(split>0&&!format.equals("zip"))split(output,split);
   File[]produced=stage.listFiles();if(produced==null||produced.length==0)throw new IOException("未生成归档");Arrays.sort(produced);
   for(File f:produced)if(new File(folder,f.getName()).exists())throw new IOException("输出分卷已存在："+f.getName());
   for(File f:produced){File target=new File(folder,f.getName());Files.move(f.toPath(),target.toPath());published.add(target);}
  }catch(Exception e){for(File f:published)f.delete();throw e;}finally{FileOps.deleteTree(stage);}
  JSONArray outputs=new JSONArray();for(File f:published)outputs.put(f.getPath());return new JSONObject().put("outputs",outputs).put("sources",paths).put("size",stats[0]);
 }
 static void writeZip(List<Item>items,File dest,String password,int level,boolean apk,long split)throws Exception{
  try(SplitOutputStream sink=new SplitOutputStream(dest,split>0?split:-1);ZipOutputStream out=new ZipOutputStream(sink,password.isEmpty()?null:password.toCharArray())){
   for(Item i:items){ZipParameters p=new ZipParameters();p.setFileNameInZip(i.name);p.setLastModifiedFileTime(i.file.lastModified());p.setEntrySize(i.file.isFile()?i.file.length():0);
    boolean stored=level==0||i.file.isDirectory()||apk&&(i.name.endsWith(".arsc")||i.name.endsWith(".so"));p.setCompressionMethod(stored?CompressionMethod.STORE:CompressionMethod.DEFLATE);p.setCompressionLevel((level<=1?CompressionLevel.FASTEST:level<=3?CompressionLevel.FAST:level<=5?CompressionLevel.NORMAL:level<=7?CompressionLevel.MAXIMUM:CompressionLevel.ULTRA));
    if(!password.isEmpty()){p.setEncryptFiles(true);p.setEncryptionMethod(EncryptionMethod.AES);p.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);}
    out.putNextEntry(p);if(i.file.isFile())try(InputStream in=new FileInputStream(i.file)){FileOps.copy(in,out,FileOps.MAX_EXTRACT);}out.closeEntry();
   }
  }
  if(apk){File aligned=new File(dest.getParentFile(),".aligned");FileOps.rewriteZip(dest,aligned,Collections.emptyMap());FileOps.replace(aligned,dest);}
 }
 static void write7z(List<Item>items,File dest,String password,int level)throws Exception{
  try(SevenZOutputFile out=new SevenZOutputFile(dest,password.isEmpty()?null:password.toCharArray())){
   LZMA2Options options=new LZMA2Options(level);options.setDictSize(level>=7?16*1024*1024:level>=5?8*1024*1024:2*1024*1024);
   out.setContentMethods(Collections.singletonList(level==0?new SevenZMethodConfiguration(SevenZMethod.COPY):new SevenZMethodConfiguration(SevenZMethod.LZMA2,options)));
   for(Item i:items){out.putArchiveEntry(out.createArchiveEntry(i.file,i.name));if(i.file.isFile())try(InputStream in=new FileInputStream(i.file)){byte[]b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}out.closeArchiveEntry();}
  }
 }
 static OutputStream compress(OutputStream out,String format,int level)throws Exception{
  if(format.equals("tar"))return out;
  if(format.equals("tar.gz")||format.equals("gzip")){GzipParameters p=new GzipParameters();p.setCompressionLevel(level);return new GzipCompressorOutputStream(out,p);}
  if(format.equals("tar.xz")){LZMA2Options p=new LZMA2Options(level);p.setDictSize(level>=7?16*1024*1024:level>=5?8*1024*1024:2*1024*1024);if(level==0)p.setMode(LZMA2Options.MODE_UNCOMPRESSED);return new XZOutputStream(out,p);}
  if(format.equals("tar.bz2"))return new BZip2CompressorOutputStream(out,Math.max(1,level));
  if(format.equals("tar.lz4"))return new FramedLZ4CompressorOutputStream(out);
  if(format.equals("tar.zst"))return new ZstdOutputStream(out,level==0?1:level*2);
  throw new IOException("未知的压缩器");
 }
 static void split(File file,long length)throws Exception{try(InputStream in=new FileInputStream(file)){byte[]buf=new byte[32768];int part=1,n;while((n=in.read(buf))!=-1){File f=new File(file.getPath()+String.format(Locale.ROOT,".%03d",part++));try(OutputStream out=new FileOutputStream(f)){long left=length;while(n!=-1){int take=(int)Math.min(n,left);out.write(buf,0,take);left-=take;if(take<n)throw new IOException("分卷缓冲区边界不匹配");if(left==0)break;n=in.read(buf,0,(int)Math.min(buf.length,left));}}}}if(!file.delete())throw new IOException("无法完成分卷归档");}
 static String format(File f){String n=f.getName().toLowerCase(Locale.ROOT).replaceFirst("\\.001$","");for(String ext:Arrays.asList("tar.gz","tar.xz","tar.zst","tar.bz2","tar.lz4","7z","tar","zip","apk"))if(n.endsWith("."+ext))return ext;if(n.endsWith(".gz")||n.endsWith(".gzip"))return "gzip";return "zip";}
 static InputStream decompress(File f,String format)throws Exception{InputStream in=new BufferedInputStream(new FileInputStream(f));try{switch(format){case "tar":return in;case "tar.gz":case "gzip":return new GzipCompressorInputStream(in);case "tar.xz":return new XZInputStream(in,128*1024);case "tar.bz2":return new BZip2CompressorInputStream(in);case "tar.lz4":return new FramedLZ4CompressorInputStream(in);case "tar.zst":return new ZstdInputStream(in);default:throw new IOException("未知的归档类型");}}catch(Exception e){in.close();throw e;}}
 public static void extract(File input,File dest,String password)throws Exception{
  if(dest.exists())throw new IOException("解压文件夹已存在");if(!dest.mkdirs())throw new IOException("无法创建解压文件夹");
  File merged=null,src=input;long[]count={0,0};String format=format(input);
  try{
   if(input.getName().endsWith(".001")){merged=File.createTempFile(".joined-",".tmp",dest.getParentFile());String base=input.getPath().substring(0,input.getPath().length()-4);long total=0;try(OutputStream out=new FileOutputStream(merged)){for(int i=1;i<10000;i++){File part=new File(base+String.format(Locale.ROOT,".%03d",i));if(!part.exists())break;try(InputStream in=new FileInputStream(part)){total+=FileOps.copy(in,out,FileOps.MAX_EXTRACT-total);}}}src=merged;}
   if(format.equals("zip")||format.equals("apk")){
    try(net.lingala.zip4j.ZipFile z=new net.lingala.zip4j.ZipFile(src,password.toCharArray())){for(FileHeader h:z.getFileHeaders()){File f=output(dest,h.getFileName(),h.isDirectory(),count);if(h.isDirectory())continue;try(InputStream in=z.getInputStream(h);OutputStream out=new FileOutputStream(f)){count[0]+=FileOps.copy(in,out,FileOps.MAX_EXTRACT-count[0]);}}}
   }else if(format.equals("7z")){
    try(SevenZFile z=SevenZFile.builder().setFile(src).setPassword(password.toCharArray()).setMaxMemoryLimitKiB(128*1024).get()){
     SevenZArchiveEntry e;while((e=z.getNextEntry())!=null){File f=output(dest,e.getName(),e.isDirectory(),count);if(e.isDirectory())continue;try(OutputStream out=new FileOutputStream(f)){byte[]b=new byte[32768];int n;while((n=z.read(b))!=-1){count[0]+=n;if(count[0]>FileOps.MAX_EXTRACT)throw new IOException("解压内容超过 1 GB");out.write(b,0,n);}}}
    }
   }else if(format.equals("gzip")){String name=input.getName().replaceFirst("\\.(gz|gzip)(\\.001)?$","");File f=output(dest,name,false,count);try(InputStream in=decompress(src,format);OutputStream out=new FileOutputStream(f)){FileOps.copy(in,out,FileOps.MAX_EXTRACT);}}
   else{try(TarArchiveInputStream in=new TarArchiveInputStream(decompress(src,format))){TarArchiveEntry e;while((e=in.getNextEntry())!=null){if(!e.isFile()&&!e.isDirectory())throw new IOException("链接和特殊归档条目不会被解压");File f=output(dest,e.getName(),e.isDirectory(),count);if(e.isDirectory())continue;try(OutputStream out=new FileOutputStream(f)){count[0]+=FileOps.copy(in,out,FileOps.MAX_EXTRACT-count[0]);}}}}
  }catch(Exception e){FileOps.deleteTree(dest);throw e;}finally{if(merged!=null)merged.delete();}
 }
 static SevenZFile sevenZ(File src,String password)throws Exception{
  SevenZFile.Builder b=SevenZFile.builder().setFile(src).setMaxMemoryLimitKiB(128*1024);
  if(password!=null&&!password.isEmpty())b.setPassword(password.toCharArray());
  return b.get();
 }
 /** 列出归档（zip/apk/7z/tar 系/gzip）内 prefix 目录下的直接子项，返回与文件列表相同结构的 JSONArray。 */
 public static JSONArray listEntries(File src,String prefix,String password,String archivePath)throws Exception{
  String format=format(src);Map<String,JSONObject>items=new TreeMap<>();
  if(prefix==null)prefix="";if(!prefix.isEmpty()&&!prefix.endsWith("/"))prefix+="/";
  if(format.equals("zip")||format.equals("apk")){
   net.lingala.zip4j.ZipFile z=src!=null&&password!=null&&!password.isEmpty()?new net.lingala.zip4j.ZipFile(src,password.toCharArray()):new net.lingala.zip4j.ZipFile(src);
   for(FileHeader h:z.getFileHeaders()){
    String name=h.getFileName();if(name==null||name.startsWith("/")||name.contains("../"))continue;
    if(!name.startsWith(prefix)||name.equals(prefix))continue;
    String rest=name.substring(prefix.length());int slash=rest.indexOf('/');boolean dir=slash>=0;String shortName=dir?rest.substring(0,slash):rest;
    if(shortName.isEmpty())continue;
    long time=h.getLastModifiedTime();
    putItem(items,shortName,dir,dir?0:h.getUncompressedSize(),time,archivePath,prefix);
   }
  }else if(format.equals("7z")){
   try(SevenZFile z=sevenZ(src,password)){
    SevenZArchiveEntry e;
    while((e=z.getNextEntry())!=null){
     String name=e.getName();if(name==null||name.startsWith("/")||name.contains("../"))continue;
     if(!name.startsWith(prefix)||name.equals(prefix))continue;
     String rest=name.substring(prefix.length());int slash=rest.indexOf('/');boolean dir=slash>=0;String shortName=dir?rest.substring(0,slash):rest;
     if(shortName.isEmpty())continue;
     putItem(items,shortName,dir,dir?0:e.getSize(),e.getLastModifiedDate()!=null?e.getLastModifiedDate().getTime():0,archivePath,prefix);
    }
   }
  }else{
   try(TarArchiveInputStream in=new TarArchiveInputStream(decompress(src,format))){
    TarArchiveEntry e;
    while((e=in.getNextEntry())!=null){
     String name=e.getName();if(name.startsWith("./"))name=name.substring(2);
     if(name.isEmpty()||name.startsWith("/")||name.contains("../"))continue;
     if(!name.startsWith(prefix)||name.equals(prefix))continue;
     String rest=name.substring(prefix.length());int slash=rest.indexOf('/');boolean dir=slash>=0;String shortName=dir?rest.substring(0,slash):rest;
     if(shortName.isEmpty())continue;
     putItem(items,shortName,dir,dir?0:e.getSize(),e.getModTime()!=null?e.getModTime().getTime():0,archivePath,prefix);
    }
   }
  }
  List<JSONObject>vals=new ArrayList<>(items.values());vals.sort((x,y)->Boolean.compare(y.optBoolean("dir"),x.optBoolean("dir")));
  JSONArray out=new JSONArray();for(JSONObject v:vals)out.put(v);return out;
 }
 private static void putItem(Map<String,JSONObject>items,String shortName,boolean dir,long size,long time,String archivePath,String prefix)throws org.json.JSONException{
  items.put(shortName,new JSONObject().put("name",shortName).put("dir",dir).put("size",size).put("time",time).put("archive",archivePath).put("entry",prefix+shortName+(dir?"/":"")));
 }
 /** 读取归档内单个条目的全部字节（上限 max），zip/apk/7z/tar 系/gzip 通用。 */
 public static byte[] readEntry(File src,String entry,String password,long max)throws Exception{
  String format=format(src);ByteArrayOutputStream bos=new ByteArrayOutputStream();byte[]buf=new byte[32768];
  if(format.equals("zip")||format.equals("apk")){
   net.lingala.zip4j.ZipFile z=password!=null&&!password.isEmpty()?new net.lingala.zip4j.ZipFile(src,password.toCharArray()):new net.lingala.zip4j.ZipFile(src);
   FileHeader h=z.getFileHeader(entry);
   if(h==null){for(FileHeader cand:z.getFileHeaders())if(cand.getFileName()!=null&&(cand.getFileName().equals(entry)||cand.getFileName().replaceFirst("^\\./","").equals(entry))){h=cand;break;}}
   if(h==null||h.isDirectory())throw new IOException("归档条目不存在："+entry);
   try(InputStream in=z.getInputStream(h)){int n;while((n=in.read(buf))!=-1){if(bos.size()+n>max)throw new IOException("归档条目过大（超过 "+(max/1048576)+" MB），请先解压");bos.write(buf,0,n);}}
  }else if(format.equals("7z")){
   try(SevenZFile z=sevenZ(src,password)){
    SevenZArchiveEntry e;boolean found=false;
    while((e=z.getNextEntry())!=null){
     String name=e.getName();
     if(!name.equals(entry)&&!name.replaceFirst("^\\./","").equals(entry))continue;
     if(e.isDirectory())throw new IOException("归档条目不存在："+entry);
     int n;while((n=z.read(buf))!=-1){if(bos.size()+n>max)throw new IOException("归档条目过大（超过 "+(max/1048576)+" MB），请先解压");bos.write(buf,0,n);}
     found=true;break;
    }
    if(!found)throw new IOException("归档条目不存在："+entry);
   }
  }else{
   try(TarArchiveInputStream in=new TarArchiveInputStream(decompress(src,format))){
    TarArchiveEntry e;boolean found=false;
    while((e=in.getNextEntry())!=null){
     String name=e.getName();if(name.startsWith("./"))name=name.substring(2);
     if(!name.equals(entry))continue;
     if(!e.isFile())throw new IOException("归档条目不存在："+entry);
     int n;while((n=in.read(buf))!=-1){if(bos.size()+n>max)throw new IOException("归档条目过大（超过 "+(max/1048576)+" MB），请先解压");bos.write(buf,0,n);}
     found=true;break;
    }
    if(!found)throw new IOException("归档条目不存在："+entry);
   }
  }
  return bos.toByteArray();
 }
 static File output(File dest,String name,boolean directory,long[]count)throws Exception{  if(name==null||++count[1]>100000)throw new IOException("归档条目无效或过多");File f=FileOps.contained(dest,name);
  if(directory){if(!f.isDirectory()&&!f.mkdirs())throw new IOException("无法创建文件夹");}
  else{if(f.exists())throw new IOException("重复的归档路径："+name);if(!f.getParentFile().isDirectory()&&!f.getParentFile().mkdirs())throw new IOException("无法创建文件夹");}return f;
 }
}
