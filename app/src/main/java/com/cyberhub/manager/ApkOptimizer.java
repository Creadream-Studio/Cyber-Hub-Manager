package com.cyberhub.manager;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * 基础 APK 优化（重新压缩）：
 * - AndroidManifest.xml / resources.arsc → STORE（无压缩，加快加载）
 * - res/ 目录下非 .xml → STORE（图片/资源通常已压缩）
 * - 其余 → DEFLATE 极限压缩（-9 或 -5）
 * - 可选：按文件后缀/正则删除指定条目
 */
public final class ApkOptimizer {
 private ApkOptimizer() {}

 public static class Result { public File output; public int removedFiles; public long savedBytes; }

 public static Result optimize(File source, File destination, int level, Set<String> deleteFiles) throws Exception {
  if(destination.exists())throw new IOException("输出已存在；请删除或改文件名");
  if(deleteFiles!=null&&deleteFiles.isEmpty())deleteFiles=null;
  Result r=new Result();r.output=destination;
  File temp=File.createTempFile(".apkopt-",".apk",destination.getAbsoluteFile().getParentFile());
  long outCount=0;int skipped=0;
  try{
   try(ZipFile in=new ZipFile(source);Counted out=new Counted(new FileOutputStream(temp));ZipOutputStream zo=new ZipOutputStream(out)){
    if(level>0)zo.setLevel(level);
    Enumeration<? extends ZipEntry> en=in.entries();
    while(en.hasMoreElements()){
     ZipEntry e=en.nextElement();String name=e.getName();
     if(e.isDirectory())continue;
     if(name.matches("(?i)META-INF/[^/]+\\.(SF|RSA|DSA|EC)")||name.equalsIgnoreCase("META-INF/MANIFEST.MF"))continue;
     if(deleteFiles!=null){
      boolean del=false;for(String fd:deleteFiles){if(name.endsWith(fd)||name.matches(fd)){del=true;break;}}
      if(del){skipped++;continue;}
     }
     ZipEntry n=new ZipEntry(name);n.setTime(Math.max(e.getTime(),315619200000L));
     boolean isManifest=name.equals("AndroidManifest.xml");
     boolean isArsc=name.equals("resources.arsc");
     boolean isResXml=name.startsWith("res/")&&name.endsWith(".xml");
     boolean isNativeLib=name.startsWith("lib/")&&name.endsWith(".so");
     // native 库必须 STORED + 16KB 对齐，否则系统无法加载（extractNativeLibs 失败）
     boolean store=isManifest||isArsc||isNativeLib||(!isResXml&&name.startsWith("res/"));
     if(store){
      n.setMethod(ZipEntry.STORED);
      byte[] data=FileOps.zipEntry(source,name,512L*1024*1024);
      CRC32 crc=new CRC32();crc.update(data);
      n.setSize(data.length);n.setCompressedSize(data.length);n.setCrc(crc.getValue());
       int align=isNativeLib?16384:4;
       int nameLen=name.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
       int already=(int)((out.count+30+nameLen)%align);
       int pad=(align-already)%align;
       if(pad>0){if(pad<4)pad+=align;byte[] extra=new byte[pad];extra[0]=0x35;extra[1]=(byte)0xd9;extra[2]=(byte)(pad-4);extra[3]=(byte)((pad-4)>>>8);n.setExtra(extra);}
      zo.putNextEntry(n);
      zo.write(data);
      zo.closeEntry();
     }else{
      n.setMethod(ZipEntry.DEFLATED);
      zo.putNextEntry(n);
      try(InputStream is=in.getInputStream(e)){FileOps.copy(is,zo,512L*1024*1024);}
      zo.closeEntry();
     }
    }
    zo.finish();zo.close();
    outCount=out.count;
   }
   r.removedFiles=skipped;
   r.savedBytes=Math.max(0,source.length()-outCount);
   FileOps.replace(temp,destination);
   return r;
  }catch(Throwable t){temp.delete();throw t;}
 }

 static class Counted extends FilterOutputStream{long count;Counted(OutputStream o){super(o);}
  @Override public void write(int b) throws IOException{out.write(b);count++;}
  @Override public void write(byte[] b,int off,int len) throws IOException{out.write(b,off,len);count+=len;}}
}
