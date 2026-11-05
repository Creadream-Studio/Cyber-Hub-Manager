package com.cyberhub.manager;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.net.ftp.FTPSClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Remote file access for the saved network storage entries. */
public final class NetworkStorage {
    private static volatile File knownHosts;
    private NetworkStorage(){}

    /** Optional trust store location; without it SFTP falls back to in-memory pinning only. */
    public static void init(File dir){ knownHosts=dir; }

    public static Object call(JSONObject a)throws Exception{
        JSONObject c=a.optJSONObject("connection");
        if(c==null)throw new IOException("缺少网络存储连接信息");
        String protocol=c.optString("protocol","").toLowerCase(Locale.ROOT);
        String op=a.optString("op","list");
        switch(protocol){
            case "ftp":return ftp(c,a,op,false);
            case "ftps":return ftp(c,a,op,true);
            case "webdav":return webdav(c,a,op);
            case "sftp":return sftp(c,a,op);
            case "smb":return smb(c,a,op);
            case "s3":case "object":return s3(c,a,op);
            default:throw new IOException("不支持的协议："+protocol);
        }
    }

    private static String required(JSONObject c,String key)throws IOException{
        String value=c.optString(key,"").trim();
        if(value.isEmpty())throw new IOException("请填写"+key);
        return value;
    }

    private static String clean(String value){
        String s=value==null?"":value.trim().replace('\\','/');
        while(s.startsWith("/"))s=s.substring(1);
        while(s.endsWith("/"))s=s.substring(0,s.length()-1);
        if(s.isEmpty())return s;
        Deque<String> parts=new ArrayDeque<>();
        for(String part:s.split("/")){
            if(part.isEmpty()||part.equals("."))continue;
            if(part.equals("..")){
                if(parts.isEmpty())throw new IllegalArgumentException("远程路径不能超出根目录");
                parts.removeLast();
                continue;
            }
            parts.addLast(part);
        }
        return String.join("/",parts);
    }

    private static String join(String parent,String child){
        String a=clean(parent),b=clean(child);
        if(a.isEmpty())return b;
        if(b.isEmpty())return a;
        return a+"/"+b;
    }

    private static String joinAbsolute(String root,String child){
        String base=root==null||root.trim().isEmpty()?"/":root.trim();
        if(!base.startsWith("/"))base="/"+base;
        while(base.length()>1&&base.endsWith("/"))base=base.substring(0,base.length()-1);
        String c=clean(child);
        if(c.isEmpty())return base;
        return (base.equals("/")?"":base)+"/"+c;
    }

    private static String nameOf(String path){
        String s=clean(path);
        int slash=s.lastIndexOf('/');
        return slash<0?s:s.substring(slash+1);
    }

    private static JSONObject entry(String name,boolean dir,long size,long time,String path)throws Exception{
        return new JSONObject().put("name",name).put("dir",dir).put("size",size).put("time",time).put("path",path);
    }

    private static JSONArray sorted(List<JSONObject> values)throws Exception{
        values.sort((a,b)->{
            boolean da=a.optBoolean("dir"),db=b.optBoolean("dir");
            if(da!=db)return da?-1:1;
            return a.optString("name").compareToIgnoreCase(b.optString("name"));
        });
        JSONArray out=new JSONArray();
        for(JSONObject value:values)out.put(value);
        return out;
    }

    private static String encodeSegment(String value)throws Exception{
        return URLEncoder.encode(value,StandardCharsets.UTF_8.name()).replace("+","%20");
    }

    private static String encodePath(String path)throws Exception{
        String[] parts=path.split("/",-1);
        StringBuilder out=new StringBuilder();
        for(int i=0;i<parts.length;i++){
            if(i>0)out.append('/');
            out.append(encodeSegment(parts[i]));
        }
        return out.toString();
    }

    private static void makeParent(File file)throws IOException{
        File parent=file.getParentFile();
        if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IOException("无法创建本地文件夹："+parent);
    }

    // ---------------------------------------------------------------- FTP / FTPS

    private static Object ftp(JSONObject c,JSONObject a,String op,boolean secure)throws Exception{
        String host=required(c,"host");
        int port=c.optInt("port",secure?21:21);
        FTPClient client;
        if(secure)client=new FTPSClient(c.optBoolean("implicit",false));
        else client=new FTPClient();
        client.setConnectTimeout(20000);
        client.setDefaultTimeout(20000);
        client.setDataTimeout(Duration.ofSeconds(90));
        client.setControlEncoding("UTF-8");
        try{
            client.connect(host,port);
            if(secure){
                FTPSClient ftps=(FTPSClient)client;
                ftps.setEndpointCheckingEnabled(true);
                ftps.execPBSZ(0);
                ftps.execPROT("P");
            }
            String user=c.optString("user","");
            String pass=c.optString("password","");
            if(!client.login(user,pass))throw new IOException("FTP 登录失败："+client.getReplyString());
            client.setFileType(FTP.BINARY_FILE_TYPE);
            if(c.optBoolean("passive",true))client.enterLocalPassiveMode();
            else client.enterLocalActiveMode();
            String root=c.optString("root","/");
            if(root.isEmpty())root="/";
            if(!root.startsWith("/"))root="/"+root;
            String path=clean(a.optString("path",""));
            String remote=joinAbsolute(root,path);
            switch(op){
                case "test":
                    if(!client.changeWorkingDirectory(remote))throw new IOException("无法进入目录："+client.getReplyString());
                    return true;
                case "list":{
                    FTPFile[] files=client.listFiles(remote);
                    if(files==null)throw new IOException("无法列出目录："+client.getReplyString());
                    List<JSONObject> values=new ArrayList<>();
                    for(FTPFile file:files){
                        String name=file.getName();
                        if(name.equals(".")||name.equals(".."))continue;
                        long time=file.getTimestamp()==null?0:file.getTimestamp().getTimeInMillis();
                        values.add(entry(name,file.isDirectory(),file.isDirectory()?0:file.getSize(),time,join(path,name)));
                    }
                    return sorted(values);
                }
                case "download":{
                    File dst=new File(a.getString("local"));
                    if(dst.exists())throw new IOException("本地文件已存在："+dst);
                    makeParent(dst);
                    try(OutputStream out=new FileOutputStream(dst)){
                        if(!client.retrieveFile(remote,out))throw new IOException("下载失败："+client.getReplyString());
                    }catch(Exception e){dst.delete();throw e;}
                    return dst.getPath();
                }
                case "upload":{
                    File src=new File(a.getString("local"));
                    if(!src.isFile())throw new IOException("本地文件不存在："+src);
                    try(InputStream in=new FileInputStream(src)){
                        if(!client.storeFile(remote,in))throw new IOException("上传失败："+client.getReplyString());
                    }
                    return true;
                }
                case "mkdir":
                    if(!client.makeDirectory(remote))throw new IOException("创建失败："+client.getReplyString());
                    return true;
                case "delete":
                    if(a.optBoolean("dir",false)){
                        if(!client.removeDirectory(remote))throw new IOException("删除失败："+client.getReplyString());
                    }else if(!client.deleteFile(remote))throw new IOException("删除失败："+client.getReplyString());
                    return true;
                case "rename":{
                    String to=joinAbsolute(root,clean(a.getString("to")));
                    if(!client.rename(remote,to))throw new IOException("重命名失败："+client.getReplyString());
                    return true;
                }
                default:throw new IOException("未知的 FTP 操作："+op);
            }
        }finally{
            try{client.logout();}catch(Exception ignored){}
            try{if(client.isConnected())client.disconnect();}catch(Exception ignored){}
        }
    }

    // ------------------------------------------------------------------- WebDAV

    private static String davUrl(JSONObject c,String path,boolean dir)throws Exception{
        String base=c.optString("url","").trim();
        if(base.isEmpty())throw new IOException("请填写 WebDAV 地址");
        if(!base.contains("://"))base="https://"+base;
        while(base.endsWith("/"))base=base.substring(0,base.length()-1);
        String cleanPath=clean(path);
        String url=cleanPath.isEmpty()?base:base+"/"+encodePath(cleanPath);
        if(dir&&!url.endsWith("/"))url+="/";
        return url;
    }

    private static final class DavHttp {
        static final OkHttpClient CLIENT=new OkHttpClient.Builder()
            .connectTimeout(20,TimeUnit.SECONDS)
            .readTimeout(90,TimeUnit.SECONDS)
            .writeTimeout(90,TimeUnit.SECONDS)
            .build();
    }

    private static Request davRequest(JSONObject c,String method,String url,Map<String,String> headers,RequestBody body){
        Request.Builder builder=new Request.Builder().url(url)
            .header("User-Agent","CyberHubManager")
            .header("Accept","*/*");
        String user=c.optString("user",""),pass=c.optString("password","");
        if(!user.isEmpty()||!pass.isEmpty())builder.header("Authorization","Basic "+Base64.getEncoder().encodeToString((user+":"+pass).getBytes(StandardCharsets.UTF_8)));
        if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())builder.header(e.getKey(),e.getValue());
        return builder.method(method,body).build();
    }

    private static Response davExecute(JSONObject c,String method,String url,Map<String,String> headers,RequestBody body)throws IOException{
        return DavHttp.CLIENT.newCall(davRequest(c,method,url,headers,body)).execute();
    }

    private static RequestBody davXml(String xml){
        return RequestBody.create(MediaType.parse("application/xml; charset=utf-8"),xml);
    }

    private static Object webdav(JSONObject c,JSONObject a,String op)throws Exception{
        String base=c.optString("url","").trim().toLowerCase(Locale.ROOT);
        boolean creds=!c.optString("user","").isEmpty()||!c.optString("password","").isEmpty();
        if(creds&&base.startsWith("http://")&&!c.optBoolean("allowInsecure",false))
            throw new IOException("WebDAV 密码不能经明文 HTTP 发送，请改用 https://");
        String path=clean(a.optString("path",""));
        switch(op){
            case "test":{
                String url=davUrl(c,path,true);
                Map<String,String> headers=new TreeMap<>();
                headers.put("Depth","0");
                String body="<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>";
                try(Response response=davExecute(c,"PROPFIND",url,headers,davXml(body))){
                    int code=response.code();
                    if(code!=207&&code!=200)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                }
                return true;
            }
            case "list":{
                String url=davUrl(c,path,true);
                Map<String,String> headers=new TreeMap<>();
                headers.put("Depth","1");
                String body="<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/><d:getcontentlength/><d:getlastmodified/></d:prop></d:propfind>";
                Document doc;
                try(Response response=davExecute(c,"PROPFIND",url,headers,davXml(body))){
                    int code=response.code();
                    if(code!=207&&code!=200)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                    ResponseBody responseBody=response.body();
                    if(responseBody==null)throw new IOException("WebDAV 返回空响应");
                    try(InputStream in=responseBody.byteStream()){doc=parseXml(in);}
                }
                URI baseUri=URI.create(url);
                String basePath=baseUri.getPath();
                if(!basePath.endsWith("/"))basePath+="/";
                NodeList responses=doc.getElementsByTagNameNS("*","response");
                List<JSONObject> values=new ArrayList<>();
                for(int i=0;i<responses.getLength();i++){
                    Element response=(Element)responses.item(i);
                    String href=text(response,"href");
                    if(href.isEmpty())continue;
                    URI resolved;
                    try{resolved=baseUri.resolve(href);}catch(Exception ignored){continue;}
                    String hrefPath=resolved.getPath();
                    if(hrefPath==null)continue;
                    String rel;
                    if(hrefPath.startsWith(basePath))rel=hrefPath.substring(basePath.length());
                    else{
                        String trimmed=basePath.endsWith("/")?basePath.substring(0,basePath.length()-1):basePath;
                        if(hrefPath.equals(trimmed))continue;
                        continue;
                    }
                    rel=rel.replaceAll("^/+","").replaceAll("/+$","");
                    if(rel.isEmpty())continue;
                    boolean dir=response.getElementsByTagNameNS("*","collection").getLength()>0||href.endsWith("/");
                    long size=parseLong(text(response,"getcontentlength"));
                    long time=parseHttpDate(text(response,"getlastmodified"));
                    values.add(entry(nameOf(rel),dir,dir?0:size,time,rel));
                }
                return sorted(values);
            }
            case "download":{
                String url=davUrl(c,path,false);
                File dst=new File(a.getString("local"));
                if(dst.exists())throw new IOException("本地文件已存在："+dst);
                makeParent(dst);
                try{
                    try(Response response=davExecute(c,"GET",url,null,null)){
                        int code=response.code();
                        if(code<200||code>=300)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                        ResponseBody responseBody=response.body();
                        if(responseBody==null)throw new IOException("WebDAV 返回空响应");
                        try(InputStream in=responseBody.byteStream();OutputStream out=new FileOutputStream(dst)){copy(in,out);}
                    }
                }catch(Exception e){dst.delete();throw e;}
                return dst.getPath();
            }
            case "upload":{
                File src=new File(a.getString("local"));
                if(!src.isFile())throw new IOException("本地文件不存在："+src);
                String url=davUrl(c,path,false);
                RequestBody body=RequestBody.create(MediaType.parse("application/octet-stream"),src);
                try(Response response=davExecute(c,"PUT",url,null,body)){
                    int code=response.code();
                    if(code<200||code>=300)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                }
                return true;
            }
            case "mkdir":{
                String url=davUrl(c,path,true);
                try(Response response=davExecute(c,"MKCOL",url,null,null)){
                    int code=response.code();
                    if(code!=201&&code!=200&&code!=204)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                }
                return true;
            }
            case "delete":{
                String url=davUrl(c,path,a.optBoolean("dir",false));
                try(Response response=davExecute(c,"DELETE",url,null,null)){
                    int code=response.code();
                    if(code<200||code>=300)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                }
                return true;
            }
            case "rename":{
                String from=davUrl(c,path,false);
                String to=davUrl(c,clean(a.getString("to")),false);
                Map<String,String> headers=new TreeMap<>();
                headers.put("Destination",to);
                headers.put("Overwrite","F");
                try(Response response=davExecute(c,"MOVE",from,headers,null)){
                    int code=response.code();
                    if(code!=201&&code!=204&&code!=200)throw new IOException("WebDAV 返回 "+code+"："+readSnippet(response));
                }
                return true;
            }
            default:throw new IOException("未知的 WebDAV 操作："+op);
        }
    }

    private static Document parseXml(InputStream in)throws Exception{
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try{factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(Exception ignored){}
        try{factory.setFeature("http://xml.org/sax/features/external-general-entities",false);}catch(Exception ignored){}
        try{factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);}catch(Exception ignored){}
        return factory.newDocumentBuilder().parse(in);
    }

    private static String text(Element parent,String localName){
        NodeList nodes=parent.getElementsByTagNameNS("*",localName);
        if(nodes.getLength()==0)return "";
        String value=nodes.item(0).getTextContent();
        return value==null?"":value.trim();
    }

    private static long parseLong(String value){
        if(value==null||value.isEmpty())return 0;
        try{return Long.parseLong(value);}catch(Exception ignored){return 0;}
    }

    private static long parseHttpDate(String value){
        if(value==null||value.isEmpty())return 0;
        try{
            SimpleDateFormat format=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
            Date date=format.parse(value);
            return date==null?0:date.getTime();
        }catch(Exception ignored){return 0;}
    }

    private static String readSnippet(Response response){
        try{
            ResponseBody body=response.body();
            if(body==null)return "";
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=body.byteStream()){
                byte[] buffer=new byte[2048];
                int read, total=0;
                while(total<8192&&(read=in.read(buffer))>0){out.write(buffer,0,read);total+=read;}
            }
            String text=new String(out.toByteArray(),StandardCharsets.UTF_8).replaceAll("\\s+"," ").trim();
            return text.length()>400?text.substring(0,400)+"…":text;
        }catch(Exception ignored){return "";}
    }

    private static void copy(InputStream in,OutputStream out)throws IOException{
        byte[] buffer=new byte[64*1024];
        int read;
        while((read=in.read(buffer))>0)out.write(buffer,0,read);
    }

    // ---------------------------------------------------------------------- SFTP

    private static Object sftp(JSONObject c,JSONObject a,String op)throws Exception{
        String host=required(c,"host");
        int port=c.optInt("port",22);
        JSch jsch=new JSch();
        String key=c.optString("privateKey","").trim();
        if(!key.isEmpty())jsch.addIdentity(key,c.optString("passphrase",""));
        Session session=jsch.getSession(c.optString("user",""),host,port);
        session.setPassword(c.optString("password",""));
        session.setHostKeyRepository(new HostKeys(knownHosts,c.optString("hostKeyFingerprint","").trim()));
        session.setConfig("StrictHostKeyChecking",c.optBoolean("strictHostKey",true)?"yes":"no");
        session.setConfig("PreferredAuthentications",key.isEmpty()?"password,keyboard-interactive,publickey":"publickey,password,keyboard-interactive");
        session.connect(20000);
        ChannelSftp channel=null;
        try{
            channel=(ChannelSftp)session.openChannel("sftp");
            channel.connect(20000);
            String root=c.optString("root","/");
            if(root.isEmpty())root="/";
            if(!root.startsWith("/"))root="/"+root;
            while(root.length()>1&&root.endsWith("/"))root=root.substring(0,root.length()-1);
            String path=clean(a.optString("path",""));
            String remote=joinAbsolute(root,path);
            switch(op){
                case "test":
                    channel.ls(remote);
                    return true;
                case "list":{
                    @SuppressWarnings("unchecked")
                    Vector<ChannelSftp.LsEntry> entries=channel.ls(remote);
                    List<JSONObject> values=new ArrayList<>();
                    for(ChannelSftp.LsEntry item:entries){
                        String name=item.getFilename();
                        if(name.equals(".")||name.equals(".."))continue;
                        boolean dir=item.getAttrs().isDir();
                        values.add(entry(name,dir,dir?0:item.getAttrs().getSize(),item.getAttrs().getMTime()*1000L,join(path,name)));
                    }
                    return sorted(values);
                }
                case "download":{
                    File dst=new File(a.getString("local"));
                    if(dst.exists())throw new IOException("本地文件已存在："+dst);
                    makeParent(dst);
                    try(OutputStream out=new FileOutputStream(dst)){channel.get(remote,out);}
                    catch(Exception e){dst.delete();throw e;}
                    return dst.getPath();
                }
                case "upload":{
                    File src=new File(a.getString("local"));
                    if(!src.isFile())throw new IOException("本地文件不存在："+src);
                    try(InputStream in=new FileInputStream(src)){channel.put(in,remote);}
                    return true;
                }
                case "mkdir":channel.mkdir(remote);return true;
                case "delete":
                    if(a.optBoolean("dir",false))channel.rmdir(remote);
                    else channel.rm(remote);
                    return true;
                case "rename":channel.rename(remote,joinAbsolute(root,clean(a.getString("to"))));return true;
                default:throw new IOException("未知的 SFTP 操作："+op);
            }
        }finally{
            if(channel!=null){try{channel.disconnect();}catch(Exception ignored){}}
            try{session.disconnect();}catch(Exception ignored){}
        }
    }

    // ----------------------------------------------------------------------- SMB

    private static String smbUrl(JSONObject c,String path,boolean dir)throws Exception{
        String host=required(c,"host");
        int port=c.optInt("port",445);
        String share=required(c,"share");
        String full=join(c.optString("root",""),path);
        String url="smb://"+host+(port>0?":"+port:"")+"/"+encodeSegment(share)+"/"+(full.isEmpty()?"":encodePath(full));
        if(dir&&!url.endsWith("/"))url+="/";
        return url;
    }

    private static CIFSContext smbContext(JSONObject c){
        NtlmPasswordAuthenticator auth=new NtlmPasswordAuthenticator(c.optString("domain",""),c.optString("user",""),c.optString("password",""));
        return SingletonContext.getInstance().withCredentials(auth);
    }

    private static Object smb(JSONObject c,JSONObject a,String op)throws Exception{
        String path=clean(a.optString("path",""));
        CIFSContext context=smbContext(c);
        SmbFile remote=new SmbFile(smbUrl(c,path,a.optBoolean("dir",false)),context);
        switch(op){
            case "test":{
                SmbFile base=new SmbFile(smbUrl(c,"",true),context);
                if(!base.exists())throw new IOException("无法访问共享目录");
                base.close();
                return true;
            }
            case "list":{
                SmbFile dir=new SmbFile(smbUrl(c,path,true),context);
                SmbFile[] files=dir.listFiles();
                if(files==null)throw new IOException("无法列出目录");
                List<JSONObject> values=new ArrayList<>();
                for(SmbFile file:files){
                    String name=file.getName();
                    if(name.endsWith("/"))name=name.substring(0,name.length()-1);
                    boolean isDir=file.isDirectory();
                    values.add(entry(name,isDir,isDir?0:file.length(),file.lastModified(),join(path,name)));
                    file.close();
                }
                dir.close();
                return sorted(values);
            }
            case "download":{
                File dst=new File(a.getString("local"));
                if(dst.exists())throw new IOException("本地文件已存在："+dst);
                makeParent(dst);
                try(InputStream in=new SmbFileInputStream(remote);OutputStream out=new FileOutputStream(dst)){copy(in,out);}
                catch(Exception e){dst.delete();throw e;}
                remote.close();
                return dst.getPath();
            }
            case "upload":{
                File src=new File(a.getString("local"));
                if(!src.isFile())throw new IOException("本地文件不存在："+src);
                try(InputStream in=new FileInputStream(src);OutputStream out=new SmbFileOutputStream(remote,false)){copy(in,out);}
                remote.close();
                return true;
            }
            case "mkdir":
                remote.mkdir();
                remote.close();
                return true;
            case "delete":
                remote.delete();
                remote.close();
                return true;
            case "rename":{
                SmbFile target=new SmbFile(smbUrl(c,clean(a.getString("to")),a.optBoolean("dir",false)),context);
                remote.renameTo(target);
                remote.close();
                target.close();
                return true;
            }
            default:throw new IOException("未知的 SMB 操作："+op);
        }
    }

    // ------------------------------------------------------------ S3 / object storage

    private static Object s3(JSONObject c,JSONObject a,String op)throws Exception{
        S3 s3=new S3(c);
        String path=clean(a.optString("path",""));
        switch(op){
            case "test":
                s3.list(s3.prefix(path),1);
                return true;
            case "list":return s3.list(s3.prefix(path),0);
            case "download":{
                String key=s3.key(path);
                HttpURLConnection conn=s3.open("GET",key,new ArrayList<>(),null,S3.EMPTY_SHA, -1);
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+s3.error(conn));
                File dst=new File(a.getString("local"));
                if(dst.exists())throw new IOException("本地文件已存在："+dst);
                makeParent(dst);
                try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(dst)){copy(in,out);}
                catch(Exception e){dst.delete();throw e;}
                conn.disconnect();
                return dst.getPath();
            }
            case "upload":{
                File src=new File(a.getString("local"));
                if(!src.isFile())throw new IOException("本地文件不存在："+src);
                String key=s3.key(path);
                Map<String,String> headers=new TreeMap<>();
                headers.put("content-type","application/octet-stream");
                HttpURLConnection conn=s3.open("PUT",key,new ArrayList<>(),headers,sha256Hex(src),src.length());
                try(InputStream in=new FileInputStream(src);OutputStream out=conn.getOutputStream()){copy(in,out);}
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+s3.error(conn));
                conn.disconnect();
                return true;
            }
            case "mkdir":{
                String key=s3.key(path);
                if(!key.endsWith("/"))key+="/";
                HttpURLConnection conn=s3.open("PUT",key,new ArrayList<>(),null,S3.EMPTY_SHA,0);
                conn.setDoOutput(true);
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+s3.error(conn));
                conn.disconnect();
                return true;
            }
            case "delete":{
                if(a.optBoolean("dir",false)){
                    List<String> keys=s3.keys(s3.prefix(path));
                    for(String key:keys)s3.delete(key);
                    String marker=s3.key(path);
                    if(!marker.endsWith("/"))marker+="/";
                    s3.delete(marker);
                }else s3.delete(s3.key(path));
                return true;
            }
            case "rename":{
                String from=s3.key(path);
                String to=s3.key(clean(a.getString("to")));
                if(a.optBoolean("dir",false)){
                    String fromPrefix=from.endsWith("/")?from:from+"/";
                    String toPrefix=to.endsWith("/")?to:to+"/";
                    for(String key:s3.keys(fromPrefix)){
                        String suffix=key.substring(fromPrefix.length());
                        s3.copyObject(key,toPrefix+suffix);
                        s3.delete(key);
                    }
                }else{
                    s3.copyObject(from,to);
                    s3.delete(from);
                }
                return true;
            }
            default:throw new IOException("未知的对象存储操作："+op);
        }
    }

    private static final class S3 {
        static final String EMPTY_SHA="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        private final String scheme,host,basePath,bucket,region,access,secret,token;
        private final int port;
        private final boolean pathStyle;
        private final String cRoot;

        S3(JSONObject c)throws Exception{
            String endpoint=c.optString("endpoint","").trim();
            if(endpoint.isEmpty())throw new IOException("请填写对象存储 Endpoint");
            if(!endpoint.contains("://"))endpoint="https://"+endpoint;
            URI uri=URI.create(endpoint);
            scheme=uri.getScheme();
            host=uri.getHost();
            int configuredPort=uri.getPort();
            if(("https".equalsIgnoreCase(scheme)&&configuredPort==443)||("http".equalsIgnoreCase(scheme)&&configuredPort==80))configuredPort=-1;
            port=configuredPort;
            String p=uri.getPath();
            basePath=p==null||p.equals("/")?"":p.replaceAll("/+$","");
            bucket=required(c,"bucket");
            String configuredRegion=c.optString("region","us-east-1").trim();
            region=configuredRegion.isEmpty()?"us-east-1":configuredRegion;
            access=required(c,"accessKey");
            secret=required(c,"secretKey");
            token=c.optString("sessionToken","").trim();
            pathStyle=c.optBoolean("pathStyle",false);
            cRoot=clean(c.optString("root",""));
            if(host==null||host.isEmpty())throw new IOException("Endpoint 格式不正确");
        }

        String rootPrefix(){return cRoot.isEmpty()?"":cRoot+"/";}

        String prefix(String path){
            String root=rootPrefix();
            String p=clean(path);
            return p.isEmpty()?root:root+p+"/";
        }

        String key(String path){return rootPrefix()+clean(path);}

        private String hostHeader(){
            if(pathStyle)return port>0?host+":"+port:host;
            return bucket+"."+host+(port>0?":"+port:"");
        }

        private String canonicalUri(String key){
            String base=basePath.isEmpty()?"":basePath;
            if(pathStyle)return uriEncode(base+"/"+bucket+"/"+key,false);
            return uriEncode(base+"/"+key,false);
        }

        HttpURLConnection open(String method,String key,List<String[]> query,Map<String,String> extra,String payloadHash,long length)throws Exception{
            TreeMap<String,String> headers=new TreeMap<>();
            headers.put("host",hostHeader());
            headers.put("x-amz-content-sha256",payloadHash);
            Date now=new Date();
            SimpleDateFormat amz=new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.US);
            amz.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat day=new SimpleDateFormat("yyyyMMdd",Locale.US);
            day.setTimeZone(TimeZone.getTimeZone("UTC"));
            String amzDate=amz.format(now), date=day.format(now);
            headers.put("x-amz-date",amzDate);
            if(!token.isEmpty())headers.put("x-amz-security-token",token);
            if(extra!=null)for(Map.Entry<String,String> e:extra.entrySet())headers.put(e.getKey().toLowerCase(Locale.ROOT),e.getValue());
            List<String[]> params=new ArrayList<>(query);
            params.sort((a,b)->{
                int c=a[0].compareTo(b[0]);
                return c!=0?c:a[1].compareTo(b[1]);
            });
            StringBuilder canonicalQuery=new StringBuilder();
            for(String[] param:params){
                if(canonicalQuery.length()>0)canonicalQuery.append('&');
                canonicalQuery.append(uriEncode(param[0],true)).append('=').append(uriEncode(param[1],true));
            }
            StringBuilder canonicalHeaders=new StringBuilder();
            StringBuilder signedHeaders=new StringBuilder();
            for(Map.Entry<String,String> e:headers.entrySet()){
                if(signedHeaders.length()>0)signedHeaders.append(';');
                signedHeaders.append(e.getKey());
                canonicalHeaders.append(e.getKey()).append(':').append(e.getValue().replaceAll("\\s+"," ").trim()).append('\n');
            }
            String canonicalRequest=method+"\n"+canonicalUri(key)+"\n"+canonicalQuery+"\n"+canonicalHeaders+"\n"+signedHeaders+"\n"+payloadHash;
            String scope=date+"/"+region+"/s3/aws4_request";
            String stringToSign="AWS4-HMAC-SHA256\n"+amzDate+"\n"+scope+"\n"+sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            byte[] signingKey=hmac(hmac(hmac(hmac(("AWS4"+secret).getBytes(StandardCharsets.UTF_8),date),region),"s3"),"aws4_request");
            String signature=hex(hmac(signingKey,stringToSign));
            String authorization="AWS4-HMAC-SHA256 Credential="+access+"/"+scope+", SignedHeaders="+signedHeaders+", Signature="+signature;
            String url=scheme+"://"+hostHeader()+canonicalUri(key)+(canonicalQuery.length()>0?"?"+canonicalQuery:"");
            HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(120000);
            conn.setRequestMethod(method);
            for(Map.Entry<String,String> e:headers.entrySet())if(!e.getKey().equals("host"))conn.setRequestProperty(e.getKey(),e.getValue());
            conn.setRequestProperty("Authorization",authorization);
            if(length>=0){conn.setDoOutput(true);conn.setFixedLengthStreamingMode(length);}
            return conn;
        }

        JSONArray list(String prefix,int maxKeys)throws Exception{
            List<JSONObject> values=new ArrayList<>();
            String tokenValue=null;
            int pages=0;
            do{
                List<String[]> query=new ArrayList<>();
                query.add(new String[]{"list-type","2"});
                query.add(new String[]{"delimiter","/"});
                query.add(new String[]{"prefix",prefix});
                query.add(new String[]{"max-keys",String.valueOf(1000)});
                if(tokenValue!=null&&!tokenValue.isEmpty())query.add(new String[]{"continuation-token",tokenValue});
                HttpURLConnection conn=open("GET","",query,null,EMPTY_SHA,-1);
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+error(conn));
                Document doc;
                try(InputStream in=conn.getInputStream()){doc=parseXml(in);}
                conn.disconnect();
                NodeList contents=doc.getElementsByTagNameNS("*","Contents");
                for(int i=0;i<contents.getLength();i++){
                    Element item=(Element)contents.item(i);
                    String key=text(item,"Key");
                    if(key.isEmpty()||key.endsWith("/")||!key.startsWith(prefix))continue;
                    String rel=key.substring(rootPrefix().length());
                    if(rel.isEmpty())continue;
                    values.add(entry(nameOf(rel),false,parseLong(text(item,"Size")),parseIso(text(item,"LastModified")),rel));
                }
                NodeList common=doc.getElementsByTagNameNS("*","CommonPrefixes");
                for(int i=0;i<common.getLength();i++){
                    String commonPrefix=text((Element)common.item(i),"Prefix");
                    if(commonPrefix.isEmpty()||!commonPrefix.startsWith(prefix))continue;
                    String rel=commonPrefix.substring(rootPrefix().length());
                    while(rel.endsWith("/"))rel=rel.substring(0,rel.length()-1);
                    if(rel.isEmpty())continue;
                    values.add(entry(nameOf(rel),true,0,0,rel));
                }
                boolean truncated="true".equalsIgnoreCase(text(doc.getDocumentElement(),"IsTruncated"));
                String next=text(doc.getDocumentElement(),"NextContinuationToken");
                tokenValue=truncated?next:null;
                pages++;
                if(maxKeys>0&&values.size()>=maxKeys)break;
            }while(tokenValue!=null&&!tokenValue.isEmpty()&&pages<100);
            return sorted(values);
        }

        List<String> keys(String prefix)throws Exception{
            List<String> out=new ArrayList<>();
            String tokenValue=null;
            int pages=0;
            do{
                List<String[]> query=new ArrayList<>();
                query.add(new String[]{"list-type","2"});
                query.add(new String[]{"prefix",prefix});
                query.add(new String[]{"max-keys",String.valueOf(1000)});
                if(tokenValue!=null&&!tokenValue.isEmpty())query.add(new String[]{"continuation-token",tokenValue});
                HttpURLConnection conn=open("GET","",query,null,EMPTY_SHA,-1);
                int code=conn.getResponseCode();
                if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+error(conn));
                Document doc;
                try(InputStream in=conn.getInputStream()){doc=parseXml(in);}
                conn.disconnect();
                NodeList contents=doc.getElementsByTagNameNS("*","Contents");
                for(int i=0;i<contents.getLength();i++){
                    String key=text((Element)contents.item(i),"Key");
                    if(!key.isEmpty())out.add(key);
                }
                String next=text(doc.getDocumentElement(),"NextContinuationToken");
                tokenValue="true".equalsIgnoreCase(text(doc.getDocumentElement(),"IsTruncated"))?next:null;
                pages++;
            }while(tokenValue!=null&&!tokenValue.isEmpty()&&pages<100);
            return out;
        }

        void delete(String key)throws Exception{
            HttpURLConnection conn=open("DELETE",key,new ArrayList<>(),null,EMPTY_SHA,-1);
            int code=conn.getResponseCode();
            if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+error(conn));
            conn.disconnect();
        }

        void copyObject(String from,String to)throws Exception{
            Map<String,String> headers=new TreeMap<>();
            headers.put("x-amz-copy-source",uriEncode("/"+bucket+"/"+from,false));
            HttpURLConnection conn=open("PUT",to,new ArrayList<>(),headers,EMPTY_SHA,0);
            conn.setDoOutput(true);
            int code=conn.getResponseCode();
            String body="";
            try(InputStream in=code>=200&&code<300?conn.getInputStream():conn.getErrorStream()){
                if(in!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();copy(in,out);body=new String(out.toByteArray(),StandardCharsets.UTF_8);}
            }
            conn.disconnect();
            if(code<200||code>=300)throw new IOException("对象存储返回 "+code+"："+body);
            if(body.contains("<Error>"))throw new IOException("复制对象失败："+body);
        }

        String error(HttpURLConnection conn){
            try(InputStream in=conn.getErrorStream()){
                if(in==null)return "";
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                copy(in,out);
                String text=new String(out.toByteArray(),StandardCharsets.UTF_8);
                int start=text.indexOf("<Message>"),end=text.indexOf("</Message>");
                if(start>=0&&end>start)return text.substring(start+9,end);
                text=text.replaceAll("\\s+"," ").trim();
                return text.length()>300?text.substring(0,300)+"…":text;
            }catch(Exception ignored){return "";}
        }

        private static long parseIso(String value){
            if(value==null||value.isEmpty())return 0;
            try{return Instant.parse(value).toEpochMilli();}catch(Exception ignored){return 0;}
        }
    }

    private static String uriEncode(String value,boolean encodeSlash){
        if(value==null)return "";
        StringBuilder out=new StringBuilder();
        for(byte b:value.getBytes(StandardCharsets.UTF_8)){
            int c=b&0xff;
            if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='-'||c=='_'||c=='.'||c=='~'||(c=='/'&&!encodeSlash)){
                out.append((char)c);
            }else{
                out.append('%');
                out.append(Character.toUpperCase(Character.forDigit((c>>4)&15,16)));
                out.append(Character.toUpperCase(Character.forDigit(c&15,16)));
            }
        }
        return out.toString();
    }

    private static String sha256Hex(byte[] data)throws Exception{
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String sha256Hex(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){
            byte[] buffer=new byte[64*1024];
            int read;
            while((read=in.read(buffer))>0)digest.update(buffer,0,read);
        }
        return hex(digest.digest());
    }

    private static byte[] hmac(byte[] key,String value)throws Exception{
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key,"HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String hex(byte[] bytes){
        StringBuilder out=new StringBuilder(bytes.length*2);
        for(byte b:bytes){
            out.append(Character.forDigit((b>>4)&15,16));
            out.append(Character.forDigit(b&15,16));
        }
        return out.toString();
    }

    /**
     * Host-key pinning for SFTP. First contact is recorded (accept-new), every later
     * connection must present the same key; hostKeyFingerprint pins out of band.
     */
    private static final class HostKeys implements HostKeyRepository {
        private static final Map<String,String> MEMORY=new ConcurrentHashMap<>();
        private final File file;
        private final String fingerprint;
        HostKeys(File dir,String fingerprint){this.file=dir==null?null:new File(dir,"known_hosts");this.fingerprint=fingerprint;}
        private Map<String,String> load(){
            Map<String,String> map=new TreeMap<>(MEMORY);
            if(file==null||!file.isFile())return map;
            Properties p=new Properties();
            try(InputStream in=new FileInputStream(file)){p.load(in);}catch(Exception ignored){return map;}
            for(String name:p.stringPropertyNames())map.put(name,p.getProperty(name));
            return map;
        }
        private void store(String host,String value){
            if(file==null){MEMORY.put(host,value);return;}
            Map<String,String> map=load();map.put(host,value);
            Properties p=new Properties();p.putAll(map);
            try(OutputStream out=new FileOutputStream(file)){p.store(out,"cyberhub known hosts");}catch(Exception ignored){}
        }
        @Override public int check(String host,byte[] key){
            String id=host==null?"":host.toLowerCase(Locale.ROOT);
            if(!fingerprint.isEmpty()){
                String actual=sha256Base64(key);
                if(!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),fingerprint.getBytes(StandardCharsets.UTF_8)))return CHANGED;
            }
            String known=load().get(id);
            if(known==null){store(id,Base64.getEncoder().encodeToString(key));return OK;}
            byte[] expected;
            try{expected=Base64.getDecoder().decode(known);}catch(Exception e){return CHANGED;}
            return MessageDigest.isEqual(expected,key)?OK:CHANGED;
        }
        @Override public void add(HostKey hostkey,UserInfo ui){
            if(hostkey!=null)store(hostkey.getHost().toLowerCase(Locale.ROOT),hostkey.getKey());
        }
        @Override public void remove(String host,String type,byte[] key){remove(host,type);}
        @Override public void remove(String host,String type){
            if(file==null){MEMORY.remove(host==null?"":host.toLowerCase(Locale.ROOT));return;}
            Map<String,String> map=load();
            if(host!=null)map.remove(host.toLowerCase(Locale.ROOT));
            Properties p=new Properties();p.putAll(map);
            try(OutputStream out=new FileOutputStream(file)){p.store(out,"cyberhub known hosts");}catch(Exception ignored){}
        }
        @Override public String getKnownHostsRepositoryID(){return file==null?"memory":file.getPath();}
        @Override public HostKey[] getHostKey(){return new HostKey[0];}
        @Override public HostKey[] getHostKey(String host,String type){return new HostKey[0];}
        private static String sha256Base64(byte[] data){
            try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));}
            catch(Exception e){return "";}
        }
    }
}
