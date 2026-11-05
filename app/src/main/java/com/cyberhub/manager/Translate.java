package com.cyberhub.manager;
import android.content.*;import android.content.SharedPreferences;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import org.json.*;

/** Machine translation backends: Tencent Cloud TMT, Baidu, Microsoft Translator and OpenAI-compatible models. */
public final class Translate {
 public static final String PREFS="translate-config";
 static final String[] PROVIDERS={"tencent","baidu","microsoft","ai"};
 // key, 中文名, tencent, baidu, microsoft, ai
 static final String[][] LANGUAGES={
  {"zh","中文","zh","zh","zh-Hans","中文"},
  {"en","English","en","en","en","English"},
  {"ja","日本語","ja","jp","ja","Japanese"},
  {"ko","한국어","ko","kor","ko","Korean"},
  {"fr","Français","fr","fra","fr","French"},
  {"de","Deutsch","de","de","de","German"},
  {"es","Español","es","spa","es","Spanish"},
  {"pt","Português","pt","pt","pt","Portuguese"},
  {"it","Italiano","it","it","it","Italian"},
  {"ru","Русский","ru","ru","ru","Russian"},
  {"ar","العربية","ar","ara","ar","Arabic"},
  {"th","ไทย","th","th","th","Thai"},
  {"vi","Tiếng Việt","vi","vie","vi","Vietnamese"},
  {"tr","Türkçe","tr","tur","tr","Turkish"},
  {"pl","Polski","pl","pol","pl","Polish"},
  {"nl","Nederlands","nl","dut","nl","Dutch"},
  {"id","Bahasa Indonesia","id","id","id","Indonesian"},
  {"ms","Bahasa Melayu","ms","may","ms","Malay"},
  {"zhTW","繁體中文","zh-TW","cht","zh-Hant","Traditional Chinese"}
 };
 final Context context;
 final SecretStore secrets;
 static final String[] SECRET_KEYS={"tencentSecretId","tencentSecretKey","baiduAppKey","microsoftKey","aiApiKey"};
 Translate(Context c){context=c;secrets=new SecretStore(c);}
 // ---------- configuration ----------
 SharedPreferences prefs(){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
 static boolean secretKey(String key){for(String k:SECRET_KEYS)if(k.equals(key))return true;return false;}
 JSONObject config()throws Exception{
  SharedPreferences p=prefs();JSONObject o=new JSONObject();
  o.put("provider",p.getString("provider","tencent"));
  o.put("target",p.getString("target","en"));
  for(String k:new String[]{"tencentSecretId","tencentSecretKey","tencentProjectId","tencentRegion","baiduAppId","baiduAppKey","microsoftKey","microsoftRegion","aiBaseUrl","aiApiKey","aiModel","aiPrompt"})o.put(k,secretKey(k)?secrets.get(k):p.getString(k,""));
  o.put("configured",configured(o));
  return o;
 }
 boolean configured(JSONObject c){
  String p=c.optString("provider","tencent");
  if(p.equals("tencent"))return !c.optString("tencentSecretId").isEmpty()&&!c.optString("tencentSecretKey").isEmpty();
  if(p.equals("baidu"))return !c.optString("baiduAppId").isEmpty()&&!c.optString("baiduAppKey").isEmpty();
  if(p.equals("microsoft"))return !c.optString("microsoftKey").isEmpty();
  if(p.equals("ai"))return !c.optString("aiApiKey").isEmpty()||!c.optString("aiBaseUrl").isEmpty();
  return false;
 }
 void saveConfig(JSONObject a)throws Exception{
  SharedPreferences.Editor e=prefs().edit();
  for(String k:new String[]{"provider","target","tencentSecretId","tencentSecretKey","tencentProjectId","tencentRegion","baiduAppId","baiduAppKey","microsoftKey","microsoftRegion","aiBaseUrl","aiApiKey","aiModel","aiPrompt"})if(a.has(k)){
   if(secretKey(k))secrets.put(k,a.optString(k,""));
   else e.putString(k,a.optString(k,""));
  }
  e.apply();
 }
 JSONObject languages()throws Exception{
  JSONArray arr=new JSONArray();
  for(String[] row:LANGUAGES)arr.put(new JSONObject().put("key",row[0]).put("name",row[1]));
  return new JSONObject().put("providers",new JSONArray(Arrays.asList(PROVIDERS))).put("languages",arr);
 }
 // ---------- translation ----------
 JSONObject translate(JSONObject a)throws Exception{
  String text=a.getString("text");
  if(text.trim().isEmpty())return new JSONObject().put("text",text);
  JSONObject c=config();String provider=c.optString("provider","tencent");
  String target=a.optString("to");if(target.isEmpty())target=c.optString("target","en");String from=a.optString("from","auto");
  String result;
  if(provider.equals("tencent"))result=tencent(c,text,from,target);
  else if(provider.equals("baidu"))result=baidu(c,text,from,target);
  else if(provider.equals("microsoft"))result=microsoft(c,text,from,target);
  else if(provider.equals("ai"))result=ai(c,text,target);
  else throw new IOException("未知的翻译服务："+provider);
  return new JSONObject().put("text",result).put("provider",provider).put("target",target);
 }
 static String code(String[] row,int index){return row==null?null:row[index];}
 String[] languageRow(String key){
  for(String[] row:LANGUAGES)if(row[0].equals(key))return row;
  return null;
 }
 // ---------- Tencent Cloud TMT (TC3-HMAC-SHA256) ----------
 String tencent(JSONObject c,String text,String from,String to)throws Exception{
  String secretId=c.optString("tencentSecretId"),secretKey=c.optString("tencentSecretKey"),projectId=c.optString("tencentProjectId").trim();
  if(secretId.isEmpty()||secretKey.isEmpty())throw new IOException("请先在「翻译服务」中填写腾讯云的 SecretId 与 SecretKey。");
  if(!projectId.isEmpty()&&!projectId.matches("\\d+"))throw new IOException("腾讯云项目 ID 必须是纯数字（可留空使用默认项目 0）。");
  String[] row=languageRow(to);if(row==null)throw new IOException("不支持的目标语言："+to);
  String target=row[2];String source=from.equals("auto")?"auto":(languageRow(from)==null?from:languageRow(from)[2]);
  JSONObject payload=new JSONObject().put("SourceText",text).put("Source",source).put("Target",target).put("ProjectId",projectId.isEmpty()?0L:Long.parseLong(projectId));
  String body=payload.toString();
  String host="tmt.tencentcloudapi.com",service="tmt",action="TextTranslate",version="2018-03-21";
  long timestamp=System.currentTimeMillis()/1000L;
  String authorization=tencentAuthorization(secretId,secretKey,service,host,action,body,timestamp);
  JSONObject headers=new JSONObject().put("Content-Type","application/json; charset=utf-8").put("Host",host)
   .put("X-TC-Action",action).put("X-TC-Version",version).put("X-TC-Timestamp",String.valueOf(timestamp)).put("Authorization",authorization);
  String region=c.optString("tencentRegion").trim();if(region.isEmpty())region="ap-guangzhou";
  headers.put("X-TC-Region",region);
  String raw=post("https://"+host+"/",headers,body);
  JSONObject o=new JSONObject(raw),response=o.optJSONObject("Response");
  if(response==null)throw new IOException("腾讯云返回异常："+raw);
  if(response.has("Error"))throw new IOException("腾讯云错误："+response.optJSONObject("Error").optString("Message",raw));
  return response.optString("TargetText","");
 }
 static String tencentAuthorization(String secretId,String secretKey,String service,String host,String action,String payload,long timestamp)throws Exception{
  String date=java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochSecond(timestamp));
  String canonicalHeaders="content-type:application/json; charset=utf-8\nhost:"+host+"\n";
  String signedHeaders="content-type;host";
  String canonicalRequest="POST\n/\n\n"+canonicalHeaders+"\n"+signedHeaders+"\n"+sha256Hex(payload.getBytes(StandardCharsets.UTF_8));
  String scope=date+"/"+service+"/tc3_request";
  String stringToSign="TC3-HMAC-SHA256\n"+timestamp+"\n"+scope+"\n"+sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
  byte[] secretDate=hmac(("TC3"+secretKey).getBytes(StandardCharsets.UTF_8),date.getBytes(StandardCharsets.UTF_8));
  byte[] secretService=hmac(secretDate,service.getBytes(StandardCharsets.UTF_8));
  byte[] secretSigning=hmac(secretService,"tc3_request".getBytes(StandardCharsets.UTF_8));
  String signature=hex(hmac(secretSigning,stringToSign.getBytes(StandardCharsets.UTF_8)));
  return "TC3-HMAC-SHA256 Credential="+secretId+"/"+scope+", SignedHeaders="+signedHeaders+", Signature="+signature;
 }
 // ---------- Baidu ----------
 String baidu(JSONObject c,String text,String from,String to)throws Exception{
  String appId=c.optString("baiduAppId"),key=c.optString("baiduAppKey");
  if(appId.isEmpty()||key.isEmpty())throw new IOException("请先在「翻译服务」中填写百度翻译的 AppID 与密钥。");
  String[] row=languageRow(to);if(row==null)throw new IOException("不支持的目标语言："+to);
  String target=row[3];
  String source=from.equals("auto")?"auto":(languageRow(from)==null?from:languageRow(from)[3]);
  String salt=String.valueOf(System.currentTimeMillis()%1000000000L);
  String sign=md5Hex((appId+text+salt+key).getBytes(StandardCharsets.UTF_8));
  StringBuilder form=new StringBuilder();
  form.append("q=").append(urlEncode(text)).append("&from=").append(urlEncode(source)).append("&to=").append(urlEncode(target))
   .append("&appid=").append(urlEncode(appId)).append("&salt=").append(urlEncode(salt)).append("&sign=").append(sign);
  String raw=post("https://fanyi-api.baidu.com/api/trans/vip/translate",new JSONObject().put("Content-Type","application/x-www-form-urlencoded"),form.toString());
  JSONObject o=new JSONObject(raw);
  if(o.has("error_code"))throw new IOException("百度翻译错误："+o.optString("error_msg",raw));
  JSONArray results=o.optJSONArray("trans_result");
  if(results==null||results.length()==0)throw new IOException("百度翻译未返回结果："+raw);
  StringBuilder sb=new StringBuilder();
  for(int i=0;i<results.length();i++){if(i>0)sb.append('\n');sb.append(results.getJSONObject(i).optString("dst",""));}
  return sb.toString();
 }
 // ---------- Microsoft Translator ----------
 String microsoft(JSONObject c,String text,String from,String to)throws Exception{
  String key=c.optString("microsoftKey");
  if(key.isEmpty())throw new IOException("请先在「翻译服务」中填写微软翻译的订阅密钥。");
  String[] row=languageRow(to);if(row==null)throw new IOException("不支持的目标语言："+to);
  String target=row[4];
  StringBuilder url=new StringBuilder("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=").append(urlEncode(target));
  if(!from.equals("auto")){String[] fr=languageRow(from);url.append("&from=").append(urlEncode(fr==null?from:fr[4]));}
  JSONObject headers=new JSONObject().put("Content-Type","application/json").put("Ocp-Apim-Subscription-Key",key);
  if(!c.optString("microsoftRegion").isEmpty())headers.put("Ocp-Apim-Subscription-Region",c.optString("microsoftRegion"));
  String raw=post(url.toString(),headers,new JSONArray().put(new JSONObject().put("Text",text)).toString());
  JSONArray arr=new JSONArray(raw);
  if(arr.length()==0)throw new IOException("微软翻译未返回结果："+raw);
  JSONArray translations=arr.getJSONObject(0).optJSONArray("translations");
  if(translations==null||translations.length()==0)throw new IOException("微软翻译未返回结果："+raw);
  return translations.getJSONObject(0).optString("text","");
 }
 // ---------- OpenAI-compatible model ----------
 String ai(JSONObject c,String text,String to)throws Exception{
  String base=c.optString("aiBaseUrl").trim(),key=c.optString("aiApiKey").trim(),model=c.optString("aiModel").trim();
  if(base.isEmpty()&&key.isEmpty())throw new IOException("请先在「翻译服务」中填写大模型的接口地址与密钥。");
  if(base.isEmpty())base="https://api.openai.com/v1";
  if(model.isEmpty())model="gpt-4o-mini";
  if(base.endsWith("/"))base=base.substring(0,base.length()-1);
  String endpoint=base.endsWith("/chat/completions")?base:base+"/chat/completions";
  String[] row=languageRow(to);String language=row==null?to:row[5];
  String prompt=c.optString("aiPrompt").trim();
  if(prompt.isEmpty())prompt="你是专业翻译。把用户文本翻译成 {lang}，只输出译文，不要解释。";
  prompt=prompt.replace("{lang}",language).replace("{text}",text);
  JSONArray messages=new JSONArray().put(new JSONObject().put("role","system").put("content",prompt)).put(new JSONObject().put("role","user").put("content",text));
  JSONObject body=new JSONObject().put("model",model).put("messages",messages).put("temperature",0.2);
  JSONObject headers=new JSONObject().put("Content-Type","application/json");
  if(!key.isEmpty())headers.put("Authorization","Bearer "+key);
  String raw=post(endpoint,headers,body.toString());
  JSONObject o=new JSONObject(raw);
  if(o.has("error"))throw new IOException("模型接口错误："+o.optJSONObject("error").optString("message",raw));
  JSONArray choices=o.optJSONArray("choices");
  if(choices==null||choices.length()==0)throw new IOException("模型未返回结果："+raw);
  JSONObject message=choices.getJSONObject(0).optJSONObject("message");
  if(message==null)throw new IOException("模型未返回结果："+raw);
  return message.optString("content","").trim();
 }
 // ---------- http ----------
 static String post(String url,JSONObject headers,String body)throws Exception{
  HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
  try{
   conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(60000);
   conn.setDoOutput(true);conn.setRequestProperty("Accept","application/json");
   JSONArray names=headers.names();if(names!=null)for(int i=0;i<names.length();i++){String key=names.getString(i);conn.setRequestProperty(key,headers.optString(key,""));}
   byte[] payload=body.getBytes(StandardCharsets.UTF_8);
   conn.setFixedLengthStreamingMode(payload.length);
   try(OutputStream out=conn.getOutputStream()){out.write(payload);}
   int code=conn.getResponseCode();InputStream in=code>=200&&code<300?conn.getInputStream():conn.getErrorStream();
   String text=in==null?"":new String(readAll(in),StandardCharsets.UTF_8);
   if(code<200||code>=300)throw new IOException("HTTP "+code+"："+(text.isEmpty()?"无响应内容":text));
   return text;
  }finally{conn.disconnect();}
 }
 static byte[] readAll(InputStream in)throws IOException{
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=in.read(buffer))>0)out.write(buffer,0,n);
  return out.toByteArray();
 }
 static String urlEncode(String value)throws Exception{return java.net.URLEncoder.encode(value,"UTF-8");}
 // ---------- crypto helpers ----------
 static byte[] hmac(byte[] key,byte[] data)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(data);}
 static String sha256Hex(byte[] data)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");return hex(md.digest(data));}
 static String md5Hex(byte[] data)throws Exception{MessageDigest md=MessageDigest.getInstance("MD5");return hex(md.digest(data));}
 static String hex(byte[] data){StringBuilder sb=new StringBuilder();for(byte b:data)sb.append(Character.forDigit((b>>4)&0xf,16)).append(Character.forDigit(b&0xf,16));return sb.toString();}
}
