package com.cyberhub.manager;

import com.android.apksig.ApkVerifier;
import com.google.protobuf.CodedInputStream;
import com.reandroid.apk.ApkModule;
import com.reandroid.apk.AndroidManifestBlockMerger;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Offline resource-aware APKS conversion. All work precedes publication of a new file. */
public final class ApksConverter {
    static final long MAX_APK=256L*1024*1024, MAX_TOTAL=1024L*1024*1024;
    static final int MAX_APKS=1024, MAX_ENTRIES=100000;
    public interface Progress { void stage(String message); }
    private static final Progress SILENT=message->{};
    private static final class Plan {
        String id,label; final List<String> paths=new ArrayList<>();
        Plan(String id,String label){this.id=id;this.label=label;}
        JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("label",label).put("count",paths.size()).put("entries",new JSONArray(paths));}
    }
    private static final class Catalog {
        final Map<String,ZipEntry> entries=new LinkedHashMap<>();
        final List<Plan> plans=new ArrayList<>(); final List<String> skipped=new ArrayList<>();
    }

    public static JSONObject inspect(File source)throws Exception {
        try(ZipFile zip=new ZipFile(source)){
            Catalog c=catalog(zip);JSONArray choices=new JSONArray();for(Plan p:c.plans)choices.put(p.json());
            return new JSONObject().put("choices",choices).put("skipped",new JSONArray(c.skipped))
                    .put("fingerprint",FileOps.sha256(source)).put("name",source.getName());
        }
    }

    public static JSONObject convert(File source,File destination,String choice,String fingerprint,
                                     File cache,InputStream signingKey,Progress progress)throws Exception {
        Progress p=progress==null?SILENT:progress;
        if(!source.isFile())throw new IOException("APKS 文件已不存在");
        if(destination.exists())throw new IOException("输出已存在；请选择其他名称");
        if(!destination.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))throw new IOException("输出名称必须以 .apk 结尾");
        if(!destination.getParentFile().isDirectory())throw new IOException("目标文件夹不存在");
        p.stage("正在检查 APKS 归档…");
        if(fingerprint==null||!fingerprint.equals(FileOps.sha256(source)))throw new IOException("APKS 已发生变化。请重新打开转换为 APK。");
        cache.mkdirs();File work=Files.createTempDirectory(cache.toPath(),"apks-").toFile();
        List<ApkModule> modules=new ArrayList<>();ApkModule merged=null;
        try(ZipFile zip=new ZipFile(source)){
            Catalog catalog=catalog(zip);Plan plan=null;
            for(Plan candidate:catalog.plans)if(candidate.id.equals(choice)){plan=candidate;break;}
            if(plan==null)throw new IOException("请选择一个可用的 APK 变体");
            long total=0;List<File> files=new ArrayList<>();
            for(int i=0;i<plan.paths.size();i++){
                ZipEntry entry=catalog.entries.get(plan.paths.get(i));
                if(entry==null||entry.isDirectory())throw new IOException("缺少 APK："+plan.paths.get(i));
                p.stage("正在读取 APK "+(i+1)+" / "+plan.paths.size()+"…");
                File file=new File(work,"module-"+i+".apk");
                try(InputStream in=zip.getInputStream(entry);OutputStream out=new FileOutputStream(file)){
                    total+=copyChecked(in,out,Math.min(MAX_APK,MAX_TOTAL-total),entry);
                }
                files.add(file);
            }
            if(!fingerprint.equals(FileOps.sha256(source)))throw new IOException("转换期间 APKS 发生变化；未保存任何输出");
            p.stage("正在校验包、资源与类…");
            validateEntries(files);
            ApkModule base=null;String packageName=null,version=null;Set<String> splits=new HashSet<>(),types=new HashSet<>();
            for(int i=0;i<files.size();i++){
                ApkModule module=ApkModule.loadApkFile(files.get(i),"module-"+i);modules.add(module);module.setLoadDefaultFramework(false);
                AndroidManifestBlock m=module.getAndroidManifest();
                if(m==null||m.getManifestElement()==null||m.getPackageName()==null)throw new IOException("APK 没有可读的 Android 清单");
                String v=String.valueOf(m.getVersionCode())+":"+attribute(m.getManifestElement(),"versionCodeMajor");
                if(packageName==null){packageName=m.getPackageName();version=v;}
                if(!packageName.equals(m.getPackageName())||!version.equals(v))throw new IOException("这些 APK 属于不同的包或版本");
                String split=m.getSplit();
                if(split==null||split.isEmpty()){if(base!=null)throw new IOException("找到多个基座 APK。请选择一个 bundletool 变体，或使用单一分包集合。");base=module;}
                else if(!splits.add(split))throw new IOException("重复的分包 ID："+split);
                addTypes(types,attribute(m.getManifestElement(),"splitTypes"));
            }
            if(base==null)throw new IOException("缺少基座 APK；仅配置分包无法生成 APK");
            for(ApkModule module:modules){
                AndroidManifestBlock m=module.getAndroidManifest();ResXmlElement root=m.getManifestElement();
                String parent=attribute(root,"configForSplit");
                if(!parent.isEmpty()&&!parent.equals("base")&&!splits.contains(parent))throw new IOException("缺少功能分包："+parent);
                for(Iterator<ResXmlElement> it=root.getElements();it.hasNext();){ResXmlElement child=it.next();if("uses-split".equals(child.getName())){String name=attribute(child,"name");if(!name.equals("base")&&!splits.contains(name))throw new IOException("缺少必需的分包："+name);}}
                Set<String> required=new HashSet<>();addTypes(required,attribute(root,"requiredSplitTypes"));
                if(!types.containsAll(required))throw new IOException("缺少必需的分包类型："+required);
            }
            File ready;boolean preserved=false;
            if(modules.size()==1){
                if("true".equals(attribute(base.getAndroidManifest().getApplicationElement(),"isSplitRequired"))||"true".equals(attribute(base.getAndroidManifest().getManifestElement(),"isSplitRequired")))throw new IOException("此基座 APK 需要归档中缺失的分包");
                ready=files.get(0);p.stage("正在校验独立 APK…");
                preserved=new ApkVerifier.Builder(ready).build().verify().isVerified();
            }else{
                p.stage("正在合并资源、DEX 与功能清单…");
                merged=new ApkModule("merged",new ZipEntryMap());merged.setLoadDefaultFramework(false);merged.merge(base);
                AndroidManifestBlockMerger merger=new AndroidManifestBlockMerger().initializeBase(merged.getAndroidManifest());
                for(ApkModule module:modules)if(module!=base){merged.merge(module);merger.merge(module.getAndroidManifest());}
                merger.sanitize(merged);sanitize(merged.getAndroidManifest().getManifestElement());
                merged.setApkSignatureBlock(null);merged.refreshTable();merged.refreshManifest();
                File raw=new File(work,"merged-raw.apk");merged.writeApk(raw);
                p.stage("正在对齐 APK 内容…");ready=new File(work,"merged-aligned.apk");FileOps.rewriteZip(raw,ready,Collections.emptyMap());
            }
            File signed=new File(work,"verified.apk");
            if(preserved)Files.copy(ready.toPath(),signed.toPath());
            else{p.stage("正在签名 APK（V1、V2、V3）…");ApkSigning.sign(ready,signed,signingKey);}
            p.stage("正在校验转换后的 APK…");
            ApkVerifier.Result verification=new ApkVerifier.Builder(signed).build().verify();
            if(!verification.isVerified())throw new IOException("输出签名校验失败："+verification.getErrors());
            try(ApkModule check=ApkModule.loadApkFile(signed)){
                check.setLoadDefaultFramework(false);AndroidManifestBlock m=check.getAndroidManifest();
                if(m==null||m.isSplit()||!packageName.equals(m.getPackageName()))throw new IOException("合并后的 APK 清单校验失败");
                if(check.hasTableBlock())check.getTableBlock().refresh();
            }
            // Copy to the destination filesystem first, then publish without replacing any existing file.
            p.stage("正在保存 APK…");File staging=File.createTempFile(".apks-output-",".tmp",destination.getParentFile());
            try{
                try(InputStream in=new FileInputStream(signed);FileOutputStream out=new FileOutputStream(staging)){FileOps.copy(in,out,MAX_TOTAL);out.getFD().sync();}
                String hash=FileOps.sha256(staging);if(!hash.equals(FileOps.sha256(signed)))throw new IOException("输出副本校验失败");
                Files.move(staging.toPath(),destination.toPath());
                return new JSONObject().put("path",destination.getPath()).put("package",packageName).put("size",destination.length())
                        .put("sha256",hash).put("verified",true).put("originalSignature",preserved).put("merged",modules.size()>1).put("apkCount",modules.size());
            }finally{staging.delete();}
        }finally{
            if(merged!=null)try{merged.close();}catch(IOException ignored){}
            for(ApkModule module:modules)try{module.close();}catch(IOException ignored){}
            try{FileOps.deleteTree(work);}catch(IOException ignored){/* OS cache eviction can reclaim a locked temp file. */}
        }
    }

    private static Catalog catalog(ZipFile zip)throws Exception {
        Catalog c=new Catalog();List<String> apks=new ArrayList<>();int count=0;
        for(Enumeration<? extends ZipEntry> e=zip.entries();e.hasMoreElements();){
            ZipEntry entry=e.nextElement();String name=entry.getName();validPath(name);
            if(++count>MAX_ENTRIES)throw new IOException("归档条目过多");
            if(c.entries.put(name,entry)!=null)throw new IOException("重复的归档条目："+name);
            if(entry.isDirectory())continue;
            String lower=name.toLowerCase(Locale.ROOT);
            if(lower.endsWith(".obb"))throw new IOException("此归档包含 OBB 数据，无法嵌入单个 APK");
            if(lower.endsWith(".apk")){if(entry.getSize()<0||entry.getSize()>MAX_APK)throw new IOException("APK 超过单个文件 256 MB 的转换上限");apks.add(name);}
        }
        if(apks.isEmpty())throw new IOException("此 APKS 归档内未找到 APK 文件");
        if(apks.size()>MAX_APKS)throw new IOException("此 bundle 中 APK 过多（上限 1024）");
        ZipEntry toc=c.entries.get("toc.pb");
        if(toc==null){Plan plan=new Plan("split-set",apks.size()==1?"单个 APK":"基座 + "+(apks.size()-1)+" 个分包");plan.paths.addAll(apks);c.plans.add(plan);return c;}
        Proto result;try(InputStream in=zip.getInputStream(toc)){result=new Proto(FileOps.bytes(in,4*1024*1024));}
        if(result.has(3)||result.has(6))throw new IOException("Play Asset Delivery 资源包无法转换为自包含 APK");
        int vi=0;
        for(byte[] bytes:result.values(1)){
            Proto variant=new Proto(bytes);String id="variant-"+(vi++);String label="变体 "+variant.number(3)+targetLabel(variant.first(1));
            Proto variantTarget=new Proto(variant.first(1));
            if(variantTarget.has(5)||variantTarget.has(6)){c.skipped.add(label+"：不支持的纹理或 SDK 运行时定向");continue;}
            Plan splits=new Plan(id,label+" · 合并分包");List<Plan> standalone=new ArrayList<>();String unsupported=null;
            for(byte[] set:variant.values(2)){
                Proto apkSet=new Proto(set),metadata=new Proto(apkSet.first(1));
                if(metadata.number(3)!=0||metadata.has(8)||(metadata.number(7)>1)){unsupported="即时或 SDK 模块";break;}
                // Conditions requiring Play delivery cannot be faithfully represented by one ordinary APK.
                if(metadata.has(5)&&!new Proto(metadata.first(5)).empty()){unsupported="条件性功能分发";break;}
                for(byte[] description:apkSet.values(2)){
                    Proto apk=new Proto(description);String path=apk.string(2);validPath(path);
                    if(!c.entries.containsKey(path)||!path.toLowerCase(Locale.ROOT).endsWith(".apk"))throw new IOException("TOC 引用了缺失的 APK："+path);
                    if(apk.has(3)){
                        Proto targeting=new Proto(apk.first(1));
                        if(targeting.has(5)||targeting.has(6)||targeting.has(8)||targeting.has(9)||targeting.has(10)||targeting.has(11)){unsupported="针对特定设备的分包定向";break;}
                        if(splits.paths.contains(path))throw new IOException("TOC 重复引用了 APK："+path);splits.paths.add(path);
                    }else if(apk.has(4)){
                        if(!new Proto(apk.first(4)).string(3).isEmpty()){unsupported="独立功能分包";break;}
                        Plan single=new Plan(id+"-apk-"+standalone.size(),label+" · "+new File(path).getName());single.paths.add(path);standalone.add(single);
                    }else{unsupported="即时、系统、资源、归档或 APEX 类型 APK";break;}
                }
                if(unsupported!=null)break;
            }
            if(unsupported!=null){c.skipped.add(label+"：不支持的 "+unsupported);continue;}
            if(!splits.paths.isEmpty())c.plans.add(splits);c.plans.addAll(standalone);
        }
        if(c.plans.isEmpty())throw new IOException("此 APKS 中没有受支持的可安装变体。 "+String.join("; ",c.skipped));
        return c;
    }

    private static void validateEntries(List<File> files)throws Exception {
        Map<String,String> hashes=new HashMap<>();Set<String> classes=new HashSet<>();long expanded=0;int count=0;
        for(File file:files)try(ZipFile zip=new ZipFile(file)){
            Set<String> names=new HashSet<>();boolean manifest=false;
            for(Enumeration<? extends ZipEntry> e=zip.entries();e.hasMoreElements();){
                ZipEntry entry=e.nextElement();String name=entry.getName();validPath(name);
                if(!names.add(name))throw new IOException("重复的 APK 条目："+name);
                if(++count>MAX_ENTRIES)throw new IOException("所选 APK 的条目总数过多");
                if(entry.isDirectory())continue;
                if(entry.getSize()<0||entry.getSize()>MAX_APK||(expanded+=entry.getSize())>MAX_TOTAL)throw new IOException("解压后的 APK 内容超过 1 GB 转换上限");
                if(name.equals("AndroidManifest.xml")){manifest=true;if(entry.getSize()>8*1024*1024)throw new IOException("清单文件过大");}
                if(name.equals("resources.arsc")&&entry.getSize()>64*1024*1024)throw new IOException("资源表超过 64 MB 转换上限");
                boolean dex=name.matches("classes([2-9]|[1-9][0-9]+)?\\.dex");
                if(dex){
                    if(entry.getSize()>128*1024*1024)throw new IOException("DEX 超过 128 MB 的转换上限");
                    try(InputStream in=zip.getInputStream(entry)){
                        byte[] data=FileOps.bytes(in,128*1024*1024);CRC32 crc=new CRC32();crc.update(data);
                        if(data.length!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("损坏的 DEX 条目："+name);
                        DexBackedDexFile d=new DexBackedDexFile(Opcodes.getDefault(),data);
                        for(ClassDef cls:d.getClasses())if(!classes.add(cls.getType()))throw new IOException("冲突的 DEX 类："+cls.getType());
                    }
                }
                // ARSCLib merges resource tables and renumbers DEX. Other duplicate paths must be identical.
                if(dex)continue;
                MessageDigest digest=MessageDigest.getInstance("SHA-256");CRC32 crc=new CRC32();long size=0;
                try(InputStream in=zip.getInputStream(entry)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){if((size+=n)>MAX_APK)throw new IOException("APK 条目过大");digest.update(buffer,0,n);crc.update(buffer,0,n);}}
                if(size!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("损坏的 APK 条目："+name);
                if(name.equals("AndroidManifest.xml")||name.equals("resources.arsc")||name.startsWith("META-INF/"))continue;
                String hash=Base64.getEncoder().encodeToString(digest.digest()),prior=hashes.putIfAbsent(name,hash);
                if(prior!=null&&!prior.equals(hash))throw new IOException("分包内容冲突："+name+"。请选择一个兼容的变体。");
            }
            if(!manifest)throw new IOException("嵌入的 APK 缺少 AndroidManifest.xml");
        }
    }
    private static long copyChecked(InputStream in,OutputStream out,long limit,ZipEntry entry)throws IOException {
        CRC32 crc=new CRC32();byte[] buffer=new byte[65536];long total=0;int n;
        while((n=in.read(buffer))!=-1){if((total+=n)>limit)throw new IOException("所选 APK 超过转换大小上限");crc.update(buffer,0,n);out.write(buffer,0,n);}
        if(total!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("损坏的 APKS 条目："+entry.getName());return total;
    }
    private static void validPath(String path)throws IOException {
        if(path.isEmpty()||path.startsWith("/")||path.indexOf('\\')>=0||path.indexOf('\0')>=0||path.indexOf(':')>=0)throw new IOException("不安全的归档路径");
        for(String part:path.split("/"))if(part.equals("..")||part.equals("."))throw new IOException("不安全的归档路径："+path);
    }
    private static String attribute(ResXmlElement element,String name){
        if(element==null)return "";for(Iterator<ResXmlAttribute> it=element.getAttributes();it.hasNext();){ResXmlAttribute a=it.next();if(name.equals(a.getName())){String value=a.getValueAsString();return value!=null?value:a.getValueType().toString().contains("BOOLEAN")?String.valueOf(a.getValueAsBoolean()):String.valueOf(a.getData());}}return "";
    }
    private static void addTypes(Set<String> result,String value){for(String s:value.split(","))if(!s.trim().isEmpty())result.add(s.trim());}
    private static void sanitize(ResXmlElement element){
        if(element==null)return;
        Set<String> names=new HashSet<>(Arrays.asList("split","splitName","configForSplit","isFeatureSplit","isSplitRequired","isolatedSplits","splitTypes","requiredSplitTypes"));
        element.removeAttributeIf(a->names.contains(a.getName()));
        List<ResXmlElement> children=new ArrayList<>();element.getElements().forEachRemaining(children::add);
        for(ResXmlElement child:children){String name=attribute(child,"name");if("uses-split".equals(child.getName())||("meta-data".equals(child.getName())&&Arrays.asList("com.android.vending.splits","com.android.vending.splits.required","com.android.vending.derived.apk.id","com.android.stamp.source","com.android.stamp.type").contains(name)))child.removeSelf();else sanitize(child);}
    }

    // Small bounded reader for bundletool's public commands.proto; unknown fields are skipped.
    // Variants are deliberately separate plans: mutually exclusive base APKs are never combined.
    private static final class Proto {
        final Map<Integer,List<byte[]>> bytes=new HashMap<>();final Map<Integer,Long> numbers=new HashMap<>();
        Proto(byte[] data)throws IOException{
            CodedInputStream in=CodedInputStream.newInstance(data);int count=0,tag;
            while((tag=in.readTag())!=0){if(++count>20000)throw new IOException("bundle TOC 过大");int field=tag>>>3,wire=tag&7;
                if(wire==2)bytes.computeIfAbsent(field,k->new ArrayList<>()).add(in.readByteArray());
                else if(wire==0)numbers.put(field,in.readInt64());
                else if(wire==1||wire==5)in.skipField(tag);else throw new IOException("无效的 bundle TOC");
            }
        }
        boolean has(int field){return bytes.containsKey(field)||numbers.containsKey(field);}boolean empty(){return bytes.isEmpty()&&numbers.isEmpty();}
        List<byte[]> values(int field){return bytes.getOrDefault(field,Collections.emptyList());}
        byte[] first(int field){List<byte[]> list=values(field);return list.isEmpty()?new byte[0]:list.get(0);}
        String string(int field){return new String(first(field),java.nio.charset.StandardCharsets.UTF_8);}
        long number(int field){return numbers.getOrDefault(field,0L);}
    }
    private static String targetLabel(byte[] bytes)throws IOException {
        Proto target=new Proto(bytes);StringBuilder label=new StringBuilder();
        Proto sdk=new Proto(target.first(1));List<String> levels=new ArrayList<>();
        for(byte[] value:sdk.values(1)){Proto version=new Proto(value);levels.add(String.valueOf(new Proto(version.first(1)).number(1)));}
        if(!levels.isEmpty())label.append(" · API ").append(String.join(",",levels)).append("+");
        List<String> abis=new ArrayList<>();String[] names={"unspecified","armeabi","armeabi-v7a","arm64-v8a","x86","x86_64","mips","mips64","riscv64"};
        for(byte[] value:new Proto(target.first(2)).values(1)){int n=(int)new Proto(value).number(1);abis.add(n>=0&&n<names.length?names[n]:"ABI "+n);}
        if(!abis.isEmpty())label.append(" · ").append(String.join(",",abis));return label.toString();
    }
}
