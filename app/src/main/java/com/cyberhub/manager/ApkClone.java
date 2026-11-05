package com.cyberhub.manager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** APK 共存：修改包名生成可并装副本。旧版共存仅改清单；新版另替换 dex 字符串池中的旧包名。 */
public final class ApkClone {
 private ApkClone() {}

 static int le32(byte[] b,int p){return (b[p]&255)|(b[p+1]&255)<<8|(b[p+2]&255)<<16|b[p+3]<<24;}
 static void set32(byte[] b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);b[p+2]=(byte)(v>>>16);b[p+3]=(byte)(v>>>24);}
 static int uleb(byte[] b,int[] p){int r=0,s=0;while(true){int c=b[p[0]++]&255;r|=(c&127)<<s;if((c&128)==0)break;s+=7;}return r;}

 /** 替换 dex 字符串池中精确等于 oldPkg 的字符串为 newPkg（其余原字节不动）。失败返回原数组。 */
 static byte[] replacePackageInDex(byte[] dex,String oldPkg,String newPkg){
  if(oldPkg==null||oldPkg.isEmpty()||oldPkg.equals(newPkg))return dex;
  if(dex.length<0x70||(dex[0]&255)!=0x64||dex[1]!=(byte)0x65||dex[2]!=(byte)0x78)return dex; // "dex"
  int count=le32(dex,0x38),off=le32(dex,0x3c),dataOff=le32(dex,0x68),dataSize=le32(dex,0x6c),mapOff=le32(dex,0x34),fileSize=le32(dex,0x20);
  if(count<=0||count>1000000||off<=0||(long)off+count*4L>dex.length)return dex;
  List<byte[]> items=new ArrayList<>();boolean changed=false;
  for(int i=0;i<count;i++){
   int p0=le32(dex,off+i*4);if(p0<0||p0>=dex.length)return dex;
   int[] pp={p0};int utf16=uleb(dex,pp);int start=pp[0];
   if(utf16<0||start>dex.length)return dex;
   int end=start;while(end<dex.length&&dex[end]!=0)end++;if(end>=dex.length)return dex;
   byte[] raw=Arrays.copyOfRange(dex,start,end+1);
   String s=decodeMutf8(dex,start,end);
   if(s.equals(oldPkg)){raw=encodeItem(oldPkg,newPkg);changed=true;}
   items.add(raw);
  }
  if(!changed)return dex;
  // 新 string_data 追加到文件末尾，其余字符串原字节连续排布
  ByteArrayOutputStream data=new ByteArrayOutputStream();
  int base=dex.length;int[] offsets=new int[count];
  for(int i=0;i<count;i++){offsets[i]=base+data.size();data.write(items.get(i),0,items.get(i).length);}
  byte[] appended=data.toByteArray();
  int newSize=dex.length+appended.length;
  byte[] out=Arrays.copyOf(dex,newSize);
  System.arraycopy(appended,0,out,dex.length,appended.length);
  for(int i=0;i<count;i++)set32(out,off+i*4,offsets[i]);
  set32(out,0x20,newSize); // file_size
  if(dataOff>0&&dataOff<=dex.length)set32(out,0x6c,dataSize+appended.length); // data_size 扩大
  // map_list: 更新 string_data 条目（type 0x0006）
  if(mapOff>0&&mapOff+4<=dex.length){
   int itemCount=le32(out,mapOff);int p=mapOff+4;
   for(int n=0;n<itemCount&&p+12<=out.length;n++){
    int type=out[p]&255;int size=le32(out,p+4);int offset=le32(out,p+8);
    if(type==0x0006){set32(out,p+4,count);set32(out,p+8,base);break;}
    p+=12;
   }
  }
  // checksum (adler32 over [12,size)) + signature (sha1 over [32,size))
  java.util.zip.Adler32 ad=new java.util.zip.Adler32();ad.update(out,12,newSize-12);set32(out,8,(int)ad.getValue());
  try{MessageDigest md=MessageDigest.getInstance("SHA-1");md.update(out,32,newSize-32);byte[] dg=md.digest();System.arraycopy(dg,0,out,12,20);}catch(Exception e){}
  return out;
 }

 static String decodeMutf8(byte[] b,int start,int end){
  StringBuilder sb=new StringBuilder();int p=start;
  while(p<end){int c=b[p++]&255;if((c&128)==0){sb.append((char)c);}else if((c&0xe0)==0xc0){int c2=b[p++]&255;sb.append((char)(((c&0x1f)<<6)|(c2&0x3f)));}else if((c&0xf0)==0xe0){int c2=b[p++]&255,c3=b[p++]&255;sb.append((char)(((c&0x0f)<<12)|((c2&0x3f)<<6)|(c3&0x3f)));}else{p++;}}
  return sb.toString();
 }

 static byte[] encodeItem(String oldPkg,String newPkg){
  byte[] body=newPkg.getBytes(StandardCharsets.UTF_8);
  int len=newPkg.length();ByteArrayOutputStream b=new ByteArrayOutputStream();
  // uleb128 utf16 length
  int v=len;do{int c=v&127;v>>>=7;if(v!=0)c|=128;b.write(c);}while(v!=0);
  b.write(body,0,body.length);b.write(0);return b.toByteArray();
 }

 /** 主入口：生成共存 APK 到源同级目录。legacy=true 仅改清单。 */
 public static String clone(File src,File out,String newPackage,boolean legacy,InputStream signingKey)throws Exception{
  if(newPackage==null||!newPackage.matches("[a-zA-Z][a-zA-Z0-9_]*([.][a-zA-Z][a-zA-Z0-9_]*)+"))throw new IOException("新包名无效：应形如 com.example.app");
  if(out.exists())throw new IOException("输出已存在；请先删除或重命名");
  String manifestName="AndroidManifest.xml";
  byte[] manifest=FileOps.zipEntry(src,manifestName,16L*1024*1024);
  if(!Axml.isBinary(manifest))throw new IOException("清单不是二进制 AXML，无法改包名");
  Axml ax=new Axml();String xml=ax.decode(manifest);
  String oldPkg=packageOf(xml);
  if(oldPkg==null||oldPkg.isEmpty())throw new IOException("清单中未找到 package 属性");
  if(oldPkg.equals(newPackage))throw new IOException("新包名与当前包名相同");
  String patched=replacePackageInXml(xml,oldPkg,newPackage);
  byte[] newManifest=ax.encode(patched);
  Map<String,byte[]> edits=new LinkedHashMap<>();
  edits.put(manifestName,newManifest);
  if(!legacy){
   try(ZipFile z=new ZipFile(src)){Enumeration<? extends ZipEntry>en=z.entries();while(en.hasMoreElements()){ZipEntry e=en.nextElement();String n=e.getName();if(n.matches("classes[0-9]*\\.dex")){byte[] d=FileOps.zipEntry(src,n,512L*1024*1024);byte[] r=replacePackageInDex(d,oldPkg,newPackage);if(r!=d)edits.put(n,r);}}}
  }
  FileOps.rewriteZip(src,out,edits);
  if(signingKey!=null){
   File signed=new File(out.getParentFile(),out.getName()+".signed");
   try{ApkSigning.sign(out,signed,signingKey);FileOps.replace(signed,out);}finally{signed.delete();}
  }
  return out.getPath();
 }

 static String packageOf(String xml){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("<manifest[^>]*\\bpackage\\s*=\\s*[\"']([^\"']+)[\"']").matcher(xml);
  return m.find()?m.group(1):null;
 }

 static String replacePackageInXml(String xml,String oldPkg,String newPkg){
  // 只替换 manifest 根元素的 package 属性。类名/android:name 不动（类路径与安装包名相互独立）。
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(<manifest[^>]*?\\bpackage\\s*=\\s*[\"'])("+java.util.regex.Pattern.quote(oldPkg)+")([\"'])").matcher(xml);
  StringBuffer sb=new StringBuffer();
  while(m.find())m.appendReplacement(sb,java.util.regex.Matcher.quoteReplacement(m.group(1)+newPkg+m.group(3)));
  m.appendTail(sb);return sb.toString();
 }
}
