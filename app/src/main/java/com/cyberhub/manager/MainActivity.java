package com.cyberhub.manager;

import android.app.*;
import android.os.*;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.graphics.*;
import android.media.*;
import android.util.Base64;
import android.text.TextUtils;
import android.graphics.Color;
import android.graphics.Typeface;
import com.termux.terminal.*;
import com.termux.view.*;
import com.cyberhub.manager.color.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private WebView web;
    private DataEngine engine;
    private String pendingOpenPath;
    private static final String APP_URL="https://appassets.androidplatform.net/assets/app.html";
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final ExecutorService thumbnails=Executors.newSingleThreadExecutor();
    private FrameLayout root;
    private FrameLayout terminalOverlay;
    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private int terminalTextSize=14;
    private boolean terminalCtrl,terminalAlt,terminalKeyboardShown;
    private TextView terminalCtrlKey,terminalAltKey;
    private FrameLayout mediaOverlay;
    private MediaPlayer mediaPlayer;
    private VideoView mediaVideo;
    private MediaServer mediaServer;
    private SeekBar mediaSeek;
    private TextView mediaTime,mediaPlayButton;
    private final Handler mediaHandler=new Handler(Looper.getMainLooper());
    private Runnable mediaTicker;
    private boolean mediaUserSeeking,mediaLoop;
    private AudioManager mediaAudioManager;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        System.setProperty("java.io.tmpdir",getCacheDir().getPath());
        pendingOpenPath=getIntent()==null?null:getIntent().getStringExtra("open_path");
        try{engine=new DataEngine(this);web=new WebView(this);web.setBackgroundColor(0xff303030);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setMediaPlaybackRequiresUserGesture(false);s.setTextZoom(100);s.setSupportZoom(false);s.setBuiltInZoomControls(false);s.setDisplayZoomControls(false);
        web.setWebChromeClient(new WebChromeClient(){@Override public boolean onConsoleMessage(ConsoleMessage m){
            if(m.messageLevel()==ConsoleMessage.MessageLevel.ERROR)android.util.Log.e("CyberHubUI",m.message()+" @ "+m.sourceId()+":"+m.lineNumber());return true;
        }});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !APP_URL.equals(r.getUrl().toString());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return localAsset(r.getUrl(),r.getMethod(),r.getRequestHeaders());}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())showStartupError("无法加载界面："+e.getDescription());}
            @Override public void onPageFinished(WebView v,String url){
                if(APP_URL.equals(url)){
                    v.evaluateJavascript("typeof window.CyberBoot === 'object'",result->{if(!"true".equals(result))showStartupError("界面启动脚本未能加载。");});
                    if(pendingOpenPath!=null){String path=pendingOpenPath;pendingOpenPath=null;openPathInUi(path);}
                }
            }
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){
                if(web==v){web=null;v.destroy();}showStartupError("Android WebView 已停止。轻点重试以重新打开应用。");return true;
            }
        });
        web.addJavascriptInterface(new Bridge(),"CyberNative");root=new FrameLayout(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(root);web.loadUrl(APP_URL);
        }catch(Throwable error){showStartupError(error.getClass().getSimpleName()+": "+error.getMessage());}
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        String path=intent==null?null:intent.getStringExtra("open_path");
        if(path!=null)runOnUiThread(()->{if(web!=null){pendingOpenPath=null;openPathInUi(path);}else pendingOpenPath=path;});
    }
    /** Navigate the file pane to a path requested by a desktop shortcut. */
    private void openPathInUi(String path){
        if(web==null)return;
        web.evaluateJavascript("(function(){var p="+JSONObject.quote(path)+";var n=0;var t=function(){if(window.cyberOpenPath)window.cyberOpenPath(p);else if(n++<100)setTimeout(t,100);};t();})()",null);
    }
    /** Request a launcher shortcut that opens this file or folder in the app. */
    private void pinShortcut(File target){
        try{
            ShortcutManager shortcuts=getSystemService(ShortcutManager.class);
            if(shortcuts==null||!shortcuts.isRequestPinShortcutSupported()){toast("当前桌面不支持固定快捷方式");return;}
            String name=target.getName();
            String id="open-"+Integer.toHexString(target.getAbsolutePath().hashCode());
            Intent intent=new Intent(this,MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("open_path",target.getAbsolutePath());
            ShortcutInfo info=new ShortcutInfo.Builder(this,id)
                    .setShortLabel(name.length()>20?name.substring(0,20):name)
                    .setLongLabel(name)
                    .setIcon(Icon.createWithResource(this,getApplicationInfo().icon!=0?getApplicationInfo().icon:android.R.drawable.sym_def_app_icon))
                    .setIntent(intent)
                    .build();
            if(!shortcuts.requestPinShortcut(info,null))toast("桌面拒绝了快捷方式请求");
        }catch(Throwable e){toast("创建快捷方式失败："+e.getMessage());}
    }
    private WebResourceResponse localAsset(Uri uri,String method,Map<String,String> requestHeaders){
        String path=uri.getPath();
        if("GET".equals(method)&&"https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&(uri.getPort()==-1||uri.getPort()==443)&&path!=null){
            // Same-origin media streaming: /media/<id> is resolved by MediaServer
            // with full Range support. Never expose raw file paths here.
            if(path.startsWith("/media/"))return mediaResponse(path.substring(7),requestHeaders==null?null:requestHeaders.get("Range"));
            if(path.startsWith("/assets/")){
            String name=path.substring(8);
            if(Arrays.asList("app.html","app.js","boot.js","style.css","vendor/codemirror.js","vendor/codemirror.css","vendor/clike.js","vendor/xml.js","vendor/searchcursor.js").contains(name)){
                try{
                    String mime=name.endsWith(".html")?"text/html":name.endsWith(".css")?"text/css":"application/javascript";
                    return new WebResourceResponse(mime,"UTF-8",200,"OK",Collections.singletonMap("Cache-Control","no-store"),getAssets().open(name));
                }catch(IOException ignored){}
            }
            }
        }
        // No network fallback, arbitrary local files, or signing-key exposure through WebView.
        return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.emptyMap(),new ByteArrayInputStream("未找到".getBytes(StandardCharsets.UTF_8)));
    }
    private WebResourceResponse mediaResponse(String id,String range){
        if(mediaServer==null)return notFoundResponse();
        MediaServer.Response r;
        try{r=mediaServer.serve(id,range);}catch(Throwable e){return notFoundResponse();}
        if(r==null)return notFoundResponse();
        Map<String,String> headers=new HashMap<>();
        headers.put("Accept-Ranges","bytes");
        headers.put("Cache-Control","no-store");
        headers.put("Content-Length",String.valueOf(r.length));
        if(r.contentRange!=null)headers.put("Content-Range",r.contentRange);
        return new WebResourceResponse(r.mime==null?"application/octet-stream":r.mime,null,r.status,r.reason,headers,r.stream);
    }
    private WebResourceResponse notFoundResponse(){
        return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.emptyMap(),new ByteArrayInputStream("未找到".getBytes(StandardCharsets.UTF_8)));
    }
    private void showStartupError(String message){
        android.widget.LinearLayout box=new android.widget.LinearLayout(this);box.setOrientation(android.widget.LinearLayout.VERTICAL);box.setPadding(32,40,32,24);box.setBackgroundColor(0xff303030);
        android.content.pm.PackageInfo provider=WebView.getCurrentWebViewPackage();
        String details="赛博中枢管家 0.1.13\n\n"+message+"\n\nAndroid "+Build.VERSION.RELEASE+"\nWebView: "+(provider==null?"不可用":provider.packageName+" "+provider.versionName);
        android.widget.TextView text=new android.widget.TextView(this);text.setText(details);text.setTextColor(0xffdddddd);text.setTextSize(16);text.setTextIsSelectable(true);box.addView(text);
        android.widget.Button retry=new android.widget.Button(this);retry.setText("重试");retry.setOnClickListener(v->recreate());box.addView(retry);
        android.widget.Button copy=new android.widget.Button(this);copy.setText("复制错误详情");copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("启动错误",details));toast("错误详情已复制");});box.addView(copy);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(box);setContentView(scroll);
    }
    void bundleProgress(String job,String message){
        runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.bundleProgress&&window.bundleProgress("+JSONObject.quote(job)+","+JSONObject.quote(message)+")",null);});
    }
    public final class Bridge {
        @JavascriptInterface public void request(String id,String action,String args){
            (action.equals("apkIcon")?thumbnails:worker).execute(()->{JSONObject response=new JSONObject();try{
                JSONObject a=new JSONObject(args);Object result;
                switch(action){
                    case "apkIcon":result=engine.iconForPath(a.getString("path"));break;
                    case "exit":runOnUiThread(()->finish());result=true;break;
                    case "permission":runOnUiThread(()->storagePermission());result=true;break;
                    case "safPermission":{String which=a.optString("which","data");runOnUiThread(()->SafAccess.request(MainActivity.this,which));result=true;break;}
                    case "install":{File f=new File(a.getString("path"));if(!f.isFile())throw new IOException("APK 未找到");File shared=shareCopy(f);runOnUiThread(()->install(shared));result=true;break;}
                    case "shareMultiple":{ArrayList<Uri>uris=new ArrayList<>();JSONArray paths=a.getJSONArray("paths");for(int i=0;i<paths.length();i++)uris.add(shareUri(shareCopy(new File(paths.getString(i)))));if(uris.isEmpty())throw new IOException("未选择文件");runOnUiThread(()->{try{Intent intent=new Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream").putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);ClipData clip=ClipData.newRawUri("文件",uris.get(0));for(int i=1;i<uris.size();i++)clip.addItem(new ClipData.Item(uris.get(i)));intent.setClipData(clip);startActivity(Intent.createChooser(intent,"分享文件"));}catch(Exception error){toast(error.getMessage());}});result=true;break;}
                    case "share":{File shared=shareCopy(new File(a.getString("path")));runOnUiThread(()->share(shared));result=true;break;}
                    case "clipboard":runOnUiThread(()->((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("赛博中枢管家",a.optString("text"))));result=true;break;
                    case "openUrl":{String url=a.getString("url");if(!url.startsWith("https://")&&!url.startsWith("http://"))throw new IOException("不支持的链接");runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(MainActivity.this,"无法打开链接",Toast.LENGTH_SHORT).show();}});result=true;break;}
                    case "colorPicker":result=colorPicker(a);break;
                    case "elevated":result=ElevatedAccess.call(MainActivity.this,a);break;
                    case "terminalOpen":{String p=a.optString("path",getFilesDir().getPath());String command=a.optString("command","");runOnUiThread(()->openTerminal(p,command));result=true;break;}
                    case "terminalClose":runOnUiThread(()->closeTerminal());result=true;break;
                    case "mediaImage":result=mediaImage(a);break;
                    case "mediaStream":{if(mediaServer==null)mediaServer=new MediaServer(MainActivity.this);result=mediaServer.registerWeb(a.getString("path"),mediaMime(a.getString("path")));break;}
                    case "clipboardGet":{String text=null;try{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null&&cm.hasPrimaryClip()&&cm.getPrimaryClip().getItemCount()>0){CharSequence t=cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);if(t!=null)text=t.toString();}}catch(Throwable ignored){}result=text==null?"":text;break;}
                    case "mediaPlay":{String p=a.getString("path");File f=new File(p);if(!f.isFile())throw new IOException("媒体文件未找到");if(!f.canRead())throw new IOException("无法读取媒体文件");String kind=a.optString("kind","audio"),name=a.optString("name",f.getName());runOnUiThread(()->openMediaOverlay(p,kind,name));result=true;break;}
                    case "mediaClose":runOnUiThread(()->closeMedia());result=true;break;
                    case "mediaExternal":{File f=new File(a.getString("path"));if(!f.isFile())throw new IOException("文件未找到");File shared=shareCopy(f);String mime=mediaMime(f.getPath());runOnUiThread(()->openExternalMedia(shared,mime));result=true;break;}
                    case "createShortcut":{File target=new File(a.getString("path"));if(!target.exists())throw new IOException("目标不存在");runOnUiThread(()->pinShortcut(target));result=true;break;}
                    case "openWithApps":result=openWithApps(a);break;
                    case "openWithLaunch":{File f=new File(a.getString("path"));if(!f.isFile())throw new IOException("文件未找到");if(!f.canRead())throw new IOException("无法读取此文件");File shared=shareCopy(f);String mime=a.optString("mime",openWithMime(f.getPath()));String pkg=a.getString("pkg"),cls=a.getString("cls");runOnUiThread(()->openWithLaunch(shared,mime,pkg,cls));result=true;break;}
                    default:result=engine.call(action,a);
                }
                response.put("id",id);response.put("ok",true);response.put("data",result==null?JSONObject.NULL:result);
            }catch(Throwable e){try{response.put("id",id);response.put("ok",false);response.put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}catch(JSONException ignored){}}
            final String json=response.toString();runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.receive&&window.receive("+json+")",null);});
            });
        }
    }

    private Object mediaImage(JSONObject a)throws Exception{
        String path=a.getString("path");
        File f=new File(path);
        if(!f.isFile())throw new IOException("图片文件未找到");
        if(!f.canRead())throw new IOException("无法读取此图片");
        boolean thumb=a.optBoolean("thumb",false);
        int max=Math.max(160,Math.min(a.optInt("max",thumb?360:2048),4096));
        String name=f.getName(),lower=name.toLowerCase(Locale.US);
        String mime=mediaMime(path);
        JSONObject out=new JSONObject().put("name",name).put("path",path).put("size",f.length());
        if(mime.equals("image/svg+xml")){
            if(f.length()>2L*1024*1024)throw new IOException("SVG 文件过大，无法预览");
            byte[] raw=FileOps.read(f,2L*1024*1024);
            return out.put("data",dataUrl(mime,raw)).put("width",0).put("height",0).put("mime",mime);
        }
        if(!thumb&&lower.endsWith(".gif")&&f.length()<=6L*1024*1024){
            byte[] raw=FileOps.read(f,6L*1024*1024);
            BitmapFactory.Options sizes=new BitmapFactory.Options();
            sizes.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(raw,0,raw.length,sizes);
            return out.put("data",dataUrl("image/gif",raw)).put("width",sizes.outWidth).put("height",sizes.outHeight).put("mime","image/gif");
        }
        BitmapFactory.Options bounds=new BitmapFactory.Options();
        bounds.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,bounds);
        if(bounds.outWidth<=0||bounds.outHeight<=0)throw new IOException("无法解码此图片");
        int sample=1;
        while(Math.max(bounds.outWidth,bounds.outHeight)/sample>max*2)sample*=2;
        BitmapFactory.Options opts=new BitmapFactory.Options();
        opts.inSampleSize=sample;
        Bitmap bitmap=BitmapFactory.decodeFile(path,opts);
        if(bitmap==null)throw new IOException("无法解码此图片");
        int orientation=ExifInterface.ORIENTATION_NORMAL;
        try{orientation=new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(Throwable ignored){}
        bitmap=orient(bitmap,orientation);
        float scale=Math.min(1f,(float)max/Math.max(bitmap.getWidth(),bitmap.getHeight()));
        if(scale<1f){
            Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),Math.max(1,Math.round(bitmap.getHeight()*scale)),true);
            if(scaled!=bitmap)bitmap.recycle();
            bitmap=scaled;
        }
        boolean png=bitmap.hasAlpha();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        if(png)bitmap.compress(Bitmap.CompressFormat.PNG,100,buffer);
        else bitmap.compress(Bitmap.CompressFormat.JPEG,thumb?78:90,buffer);
        int width=bitmap.getWidth(),height=bitmap.getHeight();
        bitmap.recycle();
        String outMime=png?"image/png":"image/jpeg";
        return out.put("data",dataUrl(outMime,buffer.toByteArray())).put("width",width).put("height",height).put("mime",outMime);
    }
    private static Bitmap orient(Bitmap source,int orientation){
        Matrix matrix=new Matrix();
        switch(orientation){
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:matrix.setScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_180:matrix.setRotate(180);break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:matrix.setScale(1,-1);break;
            case ExifInterface.ORIENTATION_TRANSPOSE:matrix.setRotate(90);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_90:matrix.setRotate(90);break;
            case ExifInterface.ORIENTATION_TRANSVERSE:matrix.setRotate(-90);matrix.postScale(-1,1);break;
            case ExifInterface.ORIENTATION_ROTATE_270:matrix.setRotate(-90);break;
            default:return source;
        }
        try{
            Bitmap rotated=Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),matrix,true);
            if(rotated!=source)source.recycle();
            return rotated;
        }catch(Throwable ignored){return source;}
    }
    private static String dataUrl(String mime,byte[] bytes){return "data:"+mime+";base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP);}
    private static String mediaMime(String path){
        String name=path.toLowerCase(Locale.US);
        int dot=name.lastIndexOf('.');
        String ext=dot>=0?name.substring(dot+1):"";
        switch(ext){
            case "jpg":case "jpeg":case "jpe":case "jfif":return "image/jpeg";
            case "png":return "image/png";
            case "gif":return "image/gif";
            case "webp":return "image/webp";
            case "bmp":case "dib":return "image/bmp";
            case "heic":case "heif":return "image/heic";
            case "avif":return "image/avif";
            case "svg":return "image/svg+xml";
            case "ico":return "image/x-icon";
            case "cur":return "image/x-icon";
            case "tif":case "tiff":return "image/tiff";
            case "dng":return "image/x-adobe-dng";
            case "mp4":case "m4v":case "f4v":return "video/mp4";
            case "mkv":case "mk3d":return "video/x-matroska";
            case "webm":return "video/webm";
            case "mov":case "qt":return "video/quicktime";
            case "avi":case "divx":return "video/x-msvideo";
            case "3gp":return "video/3gpp";
            case "3g2":return "video/3gpp2";
            case "ts":case "m2ts":case "mts":case "m2t":case "mtv":case "tp":case "trp":case "vro":return "video/mp2t";
            case "flv":return "video/x-flv";
            case "wmv":return "video/x-ms-wmv";
            case "asf":return "video/x-ms-asf";
            case "mpg":case "mpeg":case "mpe":case "m1v":case "m2v":case "vob":return "video/mpeg";
            case "ogv":case "ogm":return "video/ogg";
            case "rm":return "application/vnd.rn-realmedia";
            case "rmvb":return "application/vnd.rn-realmedia-vbr";
            case "mxf":return "application/mxf";
            case "mp3":case "mp2":case "mpga":return "audio/mpeg";
            case "wav":case "wave":return "audio/wav";
            case "flac":return "audio/flac";
            case "aac":case "m4a":case "m4b":return "audio/mp4";
            case "ogg":case "oga":case "opus":case "spx":return "audio/ogg";
            case "wma":return "audio/x-ms-wma";
            case "amr":case "nb":return "audio/amr";
            case "awb":return "audio/amr-wb";
            case "mid":case "midi":case "rmi":return "audio/midi";
            case "ape":case "apl":return "audio/x-ape";
            case "aiff":case "aif":case "aifc":return "audio/aiff";
            case "mka":case "mkv~audio":return "audio/x-matroska";
            case "au":case "snd":return "audio/basic";
            case "caf":return "audio/x-caf";
            case "weba":return "audio/webm";
            case "ac3":return "audio/ac3";
            case "dts":return "audio/vnd.dts";
            case "tta":return "audio/x-tta";
            case "wv":return "audio/x-wavpack";
        }
        String mapped=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mapped==null?"application/octet-stream":mapped;
    }
    private void openExternalMedia(File file,String mime){
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(shareUri(file),mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent,"打开方式"));
        }catch(Exception error){toast(error.getMessage()==null?"没有可处理此文件的应用":error.getMessage());}
    }
    private static String openWithMime(String path){
        String name=new File(path).getName(),ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.US);
        switch(ext){
            case "apk":return "application/vnd.android.package-archive";
            case "zip":return "application/zip";
            case "jar":return "application/java-archive";
            case "pdf":return "application/pdf";
            case "html":case "htm":return "text/html";
            case "xml":case "axml":return "text/xml";
            case "js":return "text/javascript";
            case "css":return "text/css";
            case "json":case "geojson":return "application/json";
            case "txt":case "log":case "md":case "properties":case "ini":case "conf":case "csv":case "sh":case "smali":case "java":return "text/plain";
        }
        String media=mediaMime(path);
        if(!media.equals("application/octet-stream"))return media;
        String mapped=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mapped==null?"application/octet-stream":mapped;
    }
    private Object openWithApps(JSONObject a)throws Exception{
        File f=new File(a.getString("path"));
        if(!f.isFile())throw new IOException("文件未找到");
        if(!f.canRead())throw new IOException("无法读取此文件");
        String mime=openWithMime(f.getPath());
        JSONObject out=new JSONObject().put("mime",mime);
        out.put("suggest",openWithList(new Intent(Intent.ACTION_VIEW).setDataAndType(shareUri(f),mime)));
        out.put("text",openWithList(new Intent(Intent.ACTION_VIEW).setType("text/*")));
        out.put("image",openWithList(new Intent(Intent.ACTION_VIEW).setType("image/*")));
        out.put("video",openWithList(new Intent(Intent.ACTION_VIEW).setType("video/*")));
        out.put("audio",openWithList(new Intent(Intent.ACTION_VIEW).setType("audio/*")));
        return out;
    }
    private JSONArray openWithList(Intent intent)throws Exception{
        PackageManager pm=getPackageManager();
        List<ResolveInfo> ris=pm.queryIntentActivities(intent,PackageManager.MATCH_ALL);
        Map<String,JSONObject> seen=new TreeMap<>();
        for(ResolveInfo ri:ris){
            if(ri.activityInfo==null)continue;
            String pkg=ri.activityInfo.packageName,cls=ri.activityInfo.name;
            String key=pkg+"/"+cls;
            if(seen.containsKey(key))continue;
            String label;
            try{CharSequence l=ri.loadLabel(pm);label=l==null||l.length()==0?null:l.toString();}catch(Throwable e){label=null;}
            if(label==null)label=cls.substring(cls.lastIndexOf('.')+1);
            JSONObject o=new JSONObject();
            o.put("label",label).put("pkg",pkg).put("cls",cls);
            try{o.put("icon",openWithIcon(ri.activityInfo.loadIcon(pm)));}catch(Throwable ignored){o.put("icon","");}
            seen.put(key,o);
        }
        JSONArray list=new JSONArray();
        List<JSONObject> values=new ArrayList<>(seen.values());
        Collections.sort(values,(x,y)->String.CASE_INSENSITIVE_ORDER.compare(x.optString("label"),y.optString("label")));
        for(JSONObject o:values)list.put(o);
        return list;
    }
    private static String openWithIcon(Drawable drawable){
        if(drawable==null)return "";
        Bitmap bmp=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bmp);
        try{
            drawable.setBounds(0,0,96,96);
            drawable.draw(canvas);
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG,100,buffer);
            bmp.recycle();
            return Base64.encodeToString(buffer.toByteArray(),Base64.NO_WRAP);
        }catch(Throwable e){
            bmp.recycle();
            return "";
        }
    }
    private void openWithLaunch(File shared,String mime,String pkg,String cls){
        try{
            Uri uri=shareUri(shared);
            Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime)
                    .setComponent(new ComponentName(pkg,cls))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("文件",uri));
            try{grantUriPermission(pkg,uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Throwable ignored){}
            startActivity(intent);
        }catch(Exception error){toast(error.getMessage()==null?"无法用所选应用打开":error.getMessage());}
    }
    private void openMediaOverlay(String path,String kind,String name){
        if(mediaOverlay!=null)closeMedia();
        if(terminalOverlay!=null)closeTerminal();
        if(root==null)return;
        try{
            File file=new File(path);
            if(!file.isFile())throw new IOException("媒体文件未找到");
            if(!file.canRead())throw new IOException("无法读取媒体文件");
            mediaLoop=false;mediaUserSeeking=false;
            mediaOverlay=new FrameLayout(this);
            mediaOverlay.setBackgroundColor(0xff000000);
            mediaOverlay.setKeepScreenOn(true);
            LinearLayout column=new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setBackgroundColor(0xff000000);
            LinearLayout top=new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setPadding(dp(10),0,dp(4),0);
            top.setBackgroundColor(0xff111111);
            TextView title=new TextView(this);
            title.setText(name);title.setTextSize(15);title.setTextColor(0xfff2f2f2);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
            TextView close=new TextView(this);
            close.setText("关闭");close.setTextSize(14);close.setTextColor(0xff7fb3ff);close.setGravity(Gravity.CENTER);close.setPadding(dp(16),0,dp(16),0);close.setClickable(true);
            close.setOnClickListener(v->closeMedia());
            top.addView(close,new LinearLayout.LayoutParams(-2,dp(52)));
            column.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));
            if("video".equals(kind)){
                VideoView video=new VideoView(this);
                mediaVideo=video;
                MediaController controller=new MediaController(this);
                controller.setAnchorView(video);
                video.setMediaController(controller);
                video.setVideoPath(path);
                video.setOnPreparedListener(player->{if(mediaVideo!=video)return;player.setLooping(mediaLoop);video.start();});
                video.setOnCompletionListener(player->{if(mediaVideo==video)toast("视频播放结束");});
                video.setOnErrorListener((player,what,extra)->{toast("无法播放此视频");return true;});
                column.addView(video,new LinearLayout.LayoutParams(-1,0,1));
            }else{
                TextView art=new TextView(this);
                art.setText("♪");art.setTextSize(76);art.setTextColor(0xff4a8ad6);art.setGravity(Gravity.CENTER);
                column.addView(art,new LinearLayout.LayoutParams(-1,0,1));
                mediaTime=new TextView(this);
                mediaTime.setText("00:00 / 00:00");mediaTime.setTextSize(13);mediaTime.setTextColor(0xffa8a8a8);mediaTime.setGravity(Gravity.CENTER);
                column.addView(mediaTime,new LinearLayout.LayoutParams(-1,-2));
                mediaSeek=new SeekBar(this);
                mediaSeek.setMax(1000);
                mediaSeek.setPadding(dp(16),dp(8),dp(16),dp(6));
                mediaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                    public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                        MediaPlayer mp=mediaPlayer;
                        if(fromUser&&mp!=null&&mediaTime!=null){
                            int duration=mp.getDuration();
                            if(duration>0)mediaTime.setText(mediaClock((int)((long)progress*duration/1000))+" / "+mediaClock(duration));
                        }
                    }
                    public void onStartTrackingTouch(SeekBar bar){mediaUserSeeking=true;}
                    public void onStopTrackingTouch(SeekBar bar){
                        mediaUserSeeking=false;
                        MediaPlayer mp=mediaPlayer;
                        if(mp!=null&&mp.getDuration()>0)mp.seekTo((int)((long)bar.getProgress()*mp.getDuration()/1000));
                    }
                });
                column.addView(mediaSeek,new LinearLayout.LayoutParams(-1,-2));
                LinearLayout controls=new LinearLayout(this);
                controls.setOrientation(LinearLayout.HORIZONTAL);
                controls.setPadding(dp(10),0,dp(10),dp(12));
                TextView rewind=mediaButton("−10 秒",v->seekMedia(-10000));
                controls.addView(rewind,mediaControlParams());
                mediaPlayButton=mediaButton("暂停",v->toggleMedia());
                controls.addView(mediaPlayButton,mediaControlParams());
                TextView forward=mediaButton("+10 秒",v->seekMedia(10000));
                controls.addView(forward,mediaControlParams());
                TextView loop=mediaButton("循环：关",null);
                loop.setOnClickListener(v->{mediaLoop=!mediaLoop;loop.setText(mediaLoop?"循环：开":"循环：关");MediaPlayer lp=mediaPlayer;if(lp!=null)lp.setLooping(mediaLoop);});
                controls.addView(loop,mediaControlParams());
                column.addView(controls,new LinearLayout.LayoutParams(-1,-2));
                MediaPlayer player=new MediaPlayer();
                mediaPlayer=player;
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setDataSource(path);
                player.setOnPreparedListener(ready->{
                    if(mediaPlayer!=player)return;
                    try{
                        ready.start();
                        if(mediaPlayButton!=null)mediaPlayButton.setText("暂停");
                        updateMediaTime();
                    }catch(Throwable ignored){}
                });
                player.setOnCompletionListener(done->{
                    if(mediaPlayer!=player)return;
                    if(mediaLoop){done.seekTo(0);done.start();}
                    else if(mediaPlayButton!=null)mediaPlayButton.setText("播放");
                });
                player.setOnErrorListener((failed,what,extra)->{toast("无法播放此音频");return true;});
                player.prepareAsync();
                mediaAudioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
                if(mediaAudioManager!=null){try{mediaAudioManager.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);}catch(Throwable ignored){}}
                mediaTicker=new Runnable(){
                    public void run(){
                        if(mediaPlayer==null)return;
                        updateMediaTime();
                        mediaHandler.postDelayed(this,500);
                    }
                };
                mediaHandler.post(mediaTicker);
            }
            mediaOverlay.addView(column,new FrameLayout.LayoutParams(-1,-1));
            root.addView(mediaOverlay,new FrameLayout.LayoutParams(-1,-1));
        }catch(Throwable error){
            closeMedia();
            toast(error.getMessage()==null?"无法打开媒体":error.getMessage());
        }
    }
    private LinearLayout.LayoutParams mediaControlParams(){
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(48),1);
        params.setMargins(dp(4),dp(4),dp(4),dp(4));
        return params;
    }
    private TextView mediaButton(String label,View.OnClickListener listener){
        TextView button=new TextView(this);
        button.setText(label);button.setTextSize(14);button.setTextColor(0xfff2f2f2);button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(0xff232323);button.setClickable(true);button.setFocusable(true);button.setPadding(dp(6),0,dp(6),0);
        button.setOnClickListener(listener);
        return button;
    }
    private void toggleMedia(){
        MediaPlayer player=mediaPlayer;
        if(player==null)return;
        try{
            if(player.isPlaying()){player.pause();if(mediaPlayButton!=null)mediaPlayButton.setText("播放");}
            else{player.start();if(mediaPlayButton!=null)mediaPlayButton.setText("暂停");}
        }catch(Throwable error){toast("无法控制播放");}
    }
    private void seekMedia(int delta){
        MediaPlayer player=mediaPlayer;
        if(player==null)return;
        try{
            int duration=player.getDuration();
            int position=Math.max(0,Math.min(duration>0?duration:Integer.MAX_VALUE,player.getCurrentPosition()+delta));
            player.seekTo(position);
            updateMediaTime();
        }catch(Throwable ignored){}
    }
    private void updateMediaTime(){
        MediaPlayer player=mediaPlayer;
        if(player==null)return;
        try{
            int position=player.getCurrentPosition(),duration=player.getDuration();
            if(mediaSeek!=null&&duration>0&&!mediaUserSeeking)mediaSeek.setProgress((int)((long)position*1000/duration));
            if(mediaTime!=null)mediaTime.setText(mediaClock(position)+" / "+mediaClock(duration));
        }catch(Throwable ignored){}
    }
    private static String mediaClock(int milliseconds){
        if(milliseconds<0)milliseconds=0;
        int total=milliseconds/1000,hours=total/3600,minutes=(total%3600)/60,seconds=total%60;
        return hours>0?String.format(Locale.US,"%d:%02d:%02d",hours,minutes,seconds):String.format(Locale.US,"%02d:%02d",minutes,seconds);
    }
    private void closeMedia(){
        FrameLayout overlay=mediaOverlay;
        mediaOverlay=null;
        if(mediaTicker!=null){mediaHandler.removeCallbacks(mediaTicker);mediaTicker=null;}
        if(mediaVideo!=null){try{mediaVideo.stopPlayback();}catch(Throwable ignored){}mediaVideo=null;}
        if(mediaPlayer!=null){try{if(mediaPlayer.isPlaying())mediaPlayer.pause();}catch(Throwable ignored){}try{mediaPlayer.release();}catch(Throwable ignored){}mediaPlayer=null;}
        if(mediaAudioManager!=null){try{mediaAudioManager.abandonAudioFocus(null);}catch(Throwable ignored){}mediaAudioManager=null;}
        mediaSeek=null;mediaTime=null;mediaPlayButton=null;mediaUserSeeking=false;mediaLoop=false;
        if(overlay!=null&&root!=null)root.removeView(overlay);
        if(web!=null){web.requestFocus();web.evaluateJavascript("window.mediaClosed&&window.mediaClosed()",null);}
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private String[] terminalEnv(String home,String cwd){
        File envFile=new File(getCacheDir(),"termux.env");
        String rc=
            "PS1='$(cyber_prompt)'\n"+
            "cyber_prompt(){\n"+
            "  case \"$PWD\" in\n"+
            "    \"$HOME\") printf '~ $ ' ;;\n"+
            "    \"$HOME\"/*) printf '~/%s $ ' \"${PWD#\"$HOME\"/}\" ;;\n"+
            "    *) printf '%s $ ' \"$PWD\" ;;\n"+
            "  esac\n"+
            "}\n"+
            "LS_COLORS='di=01;34:ln=01;36:pi=33:so=01;35:bd=01;33:cd=01;33:or=01;31:mi=01;31:ex=01;32'\n"+
            "export LS_COLORS\n"+
            "ls(){\n"+
            "  if command ls --color=auto \"$@\" 2>/dev/null; then return; fi\n"+
            "  command ls -1F \"$@\" 2>/dev/null | while IFS= read -r cyber_entry; do\n"+
            "    case \"$cyber_entry\" in\n"+
            "      */) printf '\\033[01;34m%s\\033[0m\\n' \"$cyber_entry\" ;;\n"+
            "      \\*) printf '\\033[01;32m%s\\033[0m\\n' \"$cyber_entry\" ;;\n"+
            "      *) printf '%s\\n' \"$cyber_entry\" ;;\n"+
            "    esac\n"+
            "  done\n"+
            "}\n";
        try(FileOutputStream out=new FileOutputStream(envFile)){out.write(rc.getBytes(StandardCharsets.UTF_8));}catch(IOException ignored){}
        return new String[]{
        "HOME="+home,
        "PWD="+cwd,
        "TMPDIR="+getCacheDir().getAbsolutePath(),
        "PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin",
        "TERM=xterm-256color",
        "COLORTERM=truecolor",
        "LANG=en_US.UTF-8",
        "ENV="+envFile.getAbsolutePath(),
        "PS1=\\w \\$ "
    };}
    private void openTerminal(String path){openTerminal(path,"");}
    private void openTerminal(String path,String command){
        if(terminalOverlay!=null||root==null)return;
        File cwd=new File(path);
        if(!cwd.isDirectory())cwd=getFilesDir();
        final String cwdPath=cwd.getAbsolutePath();
        final String home=getFilesDir().getAbsolutePath();
        terminalTextSize=14;
        terminalKeyboardShown=false;
        terminalView=new TerminalView(this,null);
        terminalView.setTerminalViewClient(new TermViewClient());
        terminalView.setTextSize(terminalTextSize);
        terminalView.setBackgroundColor(0xff000000);
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setFocusable(true);
        terminalView.setFocusableInTouchMode(true);
        terminalView.setDefaultFocusHighlightEnabled(false);
        terminalSession=new TerminalSession("/system/bin/sh",cwdPath,new String[]{"-i"},terminalEnv(home,cwdPath),2000,new TermSessionClient());
        terminalView.attachSession(terminalSession);
        if(command!=null&&!command.trim().isEmpty()){
            final byte[] bytes=(command.trim()+"\n").getBytes(StandardCharsets.UTF_8);
            final TerminalSession session=terminalSession;
            if(terminalView!=null)terminalView.postDelayed(()->{try{if(terminalSession==session)session.write(bytes,0,bytes.length);}catch(Throwable ignored){}},350);
        }

        terminalOverlay=new FrameLayout(this);
        terminalOverlay.setBackgroundColor(0xff000000);
        terminalOverlay.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30)terminalKeyboardShown=insets.isVisible(WindowInsets.Type.ime());
            else terminalKeyboardShown=insets.getSystemWindowInsetBottom()>dp(80);
            return insets;
        });
        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(terminalView,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout keys=new LinearLayout(this);
        keys.setOrientation(LinearLayout.VERTICAL);
        keys.setBackgroundColor(0xff000000);
        addTerminalKeyRow(keys,new String[][]{{"Esc","esc"},{"Tab","tab"},{"PgUp","pgup"},{"Home","home"},{"▲","up"},{"End","end"},{"⋮","menu"}});
        addTerminalKeyRow(keys,new String[][]{{"Ctrl","ctrl"},{"Alt","alt"},{"PgDn","pgdn"},{"◀","left"},{"▼","down"},{"▶","right"},{"⇄","keyboard"}});
        column.addView(keys,new LinearLayout.LayoutParams(-1,-2));
        terminalOverlay.addView(column,new FrameLayout.LayoutParams(-1,-1));

        root.addView(terminalOverlay,new FrameLayout.LayoutParams(-1,-1));
        terminalView.post(()->{TerminalView view=terminalView;if(view!=null){view.requestFocus();showTerminalKeyboard();}});
    }
    private void addTerminalKeyRow(LinearLayout parent,String[][] keys){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for(final String[] key:keys){
            TextView b=new TextView(this);
            b.setText(key[0]);b.setTextSize(15);b.setTextColor(0xfff0f0f0);b.setTypeface(Typeface.MONOSPACE);
            b.setGravity(Gravity.CENTER);b.setBackgroundColor(0xff000000);b.setClickable(true);b.setFocusable(false);b.setPadding(0,0,0,0);
            b.setOnClickListener(v->handleTerminalKey(key[1],b));
            if(key[1].equals("ctrl"))terminalCtrlKey=b;
            if(key[1].equals("alt"))terminalAltKey=b;
            row.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));
        }
        parent.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));
    }
    private void handleTerminalKey(String action,TextView view){
        switch(action){
            case "ctrl":terminalCtrl=!terminalCtrl;updateTerminalModifiers();focusTerminal();return;
            case "alt":terminalAlt=!terminalAlt;updateTerminalModifiers();focusTerminal();return;
            case "menu":showTerminalMenu(view);return;
            case "keyboard":toggleTerminalKeyboard();return;
            case "esc":writeTerminal("\u001b");return;
            case "tab":writeTerminal("\t");return;
            case "pgup":writeTerminal("\u001b[5~");return;
            case "pgdn":writeTerminal("\u001b[6~");return;
            case "home":writeTerminal("\u001b[H");return;
            case "end":writeTerminal("\u001b[F");return;
            case "up":writeTerminal("\u001b[A");return;
            case "down":writeTerminal("\u001b[B");return;
            case "left":writeTerminal("\u001b[D");return;
            case "right":writeTerminal("\u001b[C");return;
        }
    }
    private void writeTerminal(String text){
        resetTerminalModifiers();
        TerminalSession session=terminalSession;
        if(session!=null){byte[] bytes=text.getBytes(StandardCharsets.UTF_8);session.write(bytes,0,bytes.length);}
        focusTerminal();
    }
    private void resetTerminalModifiers(){terminalCtrl=false;terminalAlt=false;updateTerminalModifiers();}
    private void updateTerminalModifiers(){
        if(terminalCtrlKey!=null){terminalCtrlKey.setTextColor(terminalCtrl?0xff7fb3ff:0xfff0f0f0);terminalCtrlKey.setBackgroundColor(terminalCtrl?0xff1d3b5a:0xff000000);}
        if(terminalAltKey!=null){terminalAltKey.setTextColor(terminalAlt?0xff7fb3ff:0xfff0f0f0);terminalAltKey.setBackgroundColor(terminalAlt?0xff1d3b5a:0xff000000);}
    }
    private void focusTerminal(){TerminalView view=terminalView;if(view!=null)view.requestFocus();}
    private void showTerminalKeyboard(){
        TerminalView view=terminalView;
        if(view==null)return;
        view.requestFocus();
        InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(imm==null)return;
        view.postDelayed(()->{TerminalView target=terminalView;if(target!=null){target.requestFocus();imm.showSoftInput(target,InputMethodManager.SHOW_IMPLICIT);}},120);
    }
    private void hideTerminalKeyboard(){
        TerminalView view=terminalView;
        if(view==null)return;
        InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(imm!=null)imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }
    private void toggleTerminalKeyboard(){
        if(terminalKeyboardShown)hideTerminalKeyboard();
        else showTerminalKeyboard();
    }
    private void changeTerminalTextSize(int delta){
        int size=Math.max(8,Math.min(40,terminalTextSize+delta));
        if(size==terminalTextSize)return;
        terminalTextSize=size;
        TerminalView view=terminalView;
        if(view!=null){view.setTextSize(size);view.requestFocus();}
    }
    private void resetTerminalTextSize(){terminalTextSize=14;TerminalView view=terminalView;if(view!=null){view.setTextSize(14);view.requestFocus();}}
    private void pasteTerminal(){
        TerminalSession session=terminalSession;
        if(session==null)return;
        ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(cm==null||!cm.hasPrimaryClip()||cm.getPrimaryClip()==null||cm.getPrimaryClip().getItemCount()==0)return;
        CharSequence text=cm.getPrimaryClip().getItemAt(0).coerceToText(this);
        if(text!=null){byte[] bytes=text.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>0)session.write(bytes,0,bytes.length);}
        focusTerminal();
    }
    private void showTerminalMenu(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,"粘贴");
        menu.getMenu().add(0,2,1,"清空屏幕");
        menu.getMenu().add(0,4,2,"放大字体");
        menu.getMenu().add(0,5,3,"缩小字体");
        menu.getMenu().add(0,6,4,"重置字号");
        menu.getMenu().add(0,3,5,"关闭终端");
        menu.setOnMenuItemClickListener(item->{
            switch(item.getItemId()){
                case 1:pasteTerminal();return true;
                case 2:writeTerminal("\u000c");return true;
                case 4:changeTerminalTextSize(1);return true;
                case 5:changeTerminalTextSize(-1);return true;
                case 6:resetTerminalTextSize();return true;
                case 3:closeTerminal();return true;
            }
            return false;
        });
        menu.show();
    }
    private void closeTerminal(){
        if(terminalOverlay==null)return;
        FrameLayout overlay=terminalOverlay;
        hideTerminalKeyboard();
        terminalOverlay=null;terminalView=null;terminalCtrlKey=null;terminalAltKey=null;terminalCtrl=false;terminalAlt=false;terminalKeyboardShown=false;
        TerminalSession session=terminalSession;terminalSession=null;
        if(root!=null)root.removeView(overlay);
        if(session!=null){try{session.finishIfRunning();}catch(Throwable ignored){}}
        if(web!=null){web.requestFocus();web.evaluateJavascript("window.terminalClosed&&window.terminalClosed()",null);}
    }
    private final class TermSessionClient implements TerminalSessionClient {
        @Override public void onTextChanged(TerminalSession session){runOnUiThread(()->{if(terminalView!=null)terminalView.onScreenUpdated();});}
        @Override public void onTitleChanged(TerminalSession session){}
        @Override public void onSessionFinished(TerminalSession session){runOnUiThread(()->{if(terminalOverlay!=null){toast("终端会话已结束");closeTerminal();}});}
        @Override public void onCopyTextToClipboard(TerminalSession session,String text){runOnUiThread(()->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("Termux",text));});}
        @Override public void onPasteTextFromClipboard(TerminalSession session){
            try{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm==null||!cm.hasPrimaryClip()||cm.getPrimaryClip()==null||cm.getPrimaryClip().getItemCount()==0)return;CharSequence text=cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);if(text!=null){byte[] bytes=text.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>0)session.write(bytes,0,bytes.length);}}catch(Throwable ignored){}
        }
        @Override public void onBell(TerminalSession session){}
        @Override public void onColorsChanged(TerminalSession session){runOnUiThread(()->{if(terminalView!=null)terminalView.onScreenUpdated();});}
        @Override public void onTerminalCursorStateChange(boolean state){}
        @Override public Integer getTerminalCursorStyle(){return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE;}
        @Override public void logError(String tag,String message){android.util.Log.e(tag,message);}
        @Override public void logWarn(String tag,String message){android.util.Log.w(tag,message);}
        @Override public void logInfo(String tag,String message){android.util.Log.i(tag,message);}
        @Override public void logDebug(String tag,String message){android.util.Log.d(tag,message);}
        @Override public void logVerbose(String tag,String message){android.util.Log.v(tag,message);}
        @Override public void logStackTraceWithMessage(String tag,String message,Exception e){android.util.Log.e(tag,message,e);}
        @Override public void logStackTrace(String tag,Exception e){android.util.Log.e(tag,"",e);}
    }
    private final class TermViewClient implements TerminalViewClient {
        @Override public float onScale(float scale){
            if(scale<0.9f||scale>1.1f){changeTerminalTextSize(scale>1f?1:-1);return 1f;}
            return scale;
        }
        @Override public void onSingleTapUp(MotionEvent e){if(!terminalKeyboardShown)showTerminalKeyboard();else focusTerminal();}
        @Override public boolean shouldBackButtonBeMappedToEscape(){return false;}
        @Override public boolean shouldEnforceCharBasedInput(){return true;}
        @Override public boolean shouldUseCtrlSpaceWorkaround(){return true;}
        @Override public boolean isTerminalViewSelected(){return terminalView!=null;}
        @Override public void copyModeChanged(boolean copyMode){}
        @Override public boolean onKeyDown(int keyCode,KeyEvent e,TerminalSession session){return false;}
        @Override public boolean onKeyUp(int keyCode,KeyEvent e){return false;}
        @Override public boolean onLongPress(MotionEvent event){return false;}
        @Override public boolean readControlKey(){return terminalCtrl;}
        @Override public boolean readAltKey(){return terminalAlt;}
        @Override public boolean readShiftKey(){return false;}
        @Override public boolean readFnKey(){return false;}
        @Override public boolean onCodePoint(int codePoint,boolean ctrlDown,TerminalSession session){if(terminalCtrl||terminalAlt)resetTerminalModifiers();return false;}
        @Override public void onEmulatorSet(){}
        @Override public void logError(String tag,String message){android.util.Log.e(tag,message);}
        @Override public void logWarn(String tag,String message){android.util.Log.w(tag,message);}
        @Override public void logInfo(String tag,String message){android.util.Log.i(tag,message);}
        @Override public void logDebug(String tag,String message){android.util.Log.d(tag,message);}
        @Override public void logVerbose(String tag,String message){android.util.Log.v(tag,message);}
        @Override public void logStackTraceWithMessage(String tag,String message,Exception e){android.util.Log.e(tag,message,e);}
        @Override public void logStackTrace(String tag,Exception e){android.util.Log.e(tag,"",e);}
    }
    private File shareCopy(File src)throws IOException{File root=new File(getCacheDir(),"shared");root.mkdirs();File dst=FileOps.child(root,src.getName());java.nio.file.Files.copy(src.toPath(),dst.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);return dst;}
    private Uri shareUri(File f){return new Uri.Builder().scheme("content").authority(getPackageName()+".files").appendPath(f.getName()).build();}
    private void install(File f){try{
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));toast("允许安装后，再轻点安装");return;}
        Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(shareUri(f),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
    }catch(Exception e){toast(e.getMessage());}}
    private void share(File f){try{Intent i=new Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM,shareUri(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"分享文件"));}catch(Exception e){toast(e.getMessage());}}
    private void storagePermission(){try{if(Build.VERSION.SDK_INT>=30)startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));else requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},10);}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}}
    private void toast(String s){android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show();}
    private static final int COLOR_PICK_REQ=200;
    private static final int COLOR_PICK_NOTIF_REQ=201;
    private boolean colorPickerNotifPending=false;
    /** 屏幕取色入口：悬浮窗权限 → 通知权限（33+）→ 屏幕录制授权 → 启动前台服务。 */
    void startColorPicker(){
        if(ColorPicker.isRunning()){toast("屏幕取色已在运行");return;}
        if(!Settings.canDrawOverlays(this)){
            toast("请先允许本应用「显示在其他应用上层」，然后再次点击开始取色");
            try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}
            return;
        }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            colorPickerNotifPending=true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},COLOR_PICK_NOTIF_REQ);
            toast("请允许通知（取色以前台服务运行），然后按提示继续");
            return;
        }
        launchColorProjection();
    }
    private void launchColorProjection(){
        try{
            android.media.projection.MediaProjectionManager m=(android.media.projection.MediaProjectionManager)getSystemService("media_projection");
            if(m==null){toast("此设备不支持屏幕取色");return;}
            startActivityForResult(m.createScreenCaptureIntent(),COLOR_PICK_REQ);
        }catch(Exception e){toast("无法启动屏幕取色："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
    }
    Object colorPicker(JSONObject a)throws Exception{
        String op=a.optString("op","start");
        if(op.equals("stop")){runOnUiThread(()->ColorPicker.stopService(this));return new JSONObject().put("running",ColorPicker.isRunning());}
        if(op.equals("status"))return new JSONObject().put("running",ColorPicker.isRunning());
        if(op.equals("get")){
            SharedPreferences p=getSharedPreferences(ColorPicker.PREFS,MODE_PRIVATE);
            return new JSONObject()
                    .put("magnifierSize",p.getString(ColorPicker.PREF_MAGNIFIER_SIZE,"small"))
                    .put("captureSpeed",p.getString(ColorPicker.PREF_CAPTURE_SPEED,"normal"))
                    .put("showGridLines",p.getBoolean(ColorPicker.PREF_SHOW_GRID_LINES,true))
                    .put("captureRange",p.getString(ColorPicker.PREF_CAPTURE_RANGE,"small"));
        }
        if(op.equals("set")){
            SharedPreferences.Editor e=getSharedPreferences(ColorPicker.PREFS,MODE_PRIVATE).edit();
            String key=a.optString("key","");
            if(key.equals("magnifierSize"))e.putString(ColorPicker.PREF_MAGNIFIER_SIZE,a.getString("value"));
            else if(key.equals("captureSpeed"))e.putString(ColorPicker.PREF_CAPTURE_SPEED,a.getString("value"));
            else if(key.equals("captureRange"))e.putString(ColorPicker.PREF_CAPTURE_RANGE,a.getString("value"));
            else if(key.equals("showGridLines"))e.putBoolean(ColorPicker.PREF_SHOW_GRID_LINES,a.getBoolean("value"));
            e.apply();
            return new JSONObject().put("ok",true);
        }
        runOnUiThread(()->startColorPicker());
        return new JSONObject().put("started",true).put("running",ColorPicker.isRunning());
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==SafAccess.REQUEST){
            String message=SafAccess.take(this,res,data);
            if(message!=null)toast(message);
            if(web!=null)web.evaluateJavascript("window.onNativeResume&&window.onNativeResume()",null);
            return;
        }
        if(req==COLOR_PICK_REQ){
            if(res==RESULT_OK&&data!=null){
                try{
                    Intent i=new Intent(this,ColorPickerService.class)
                            .putExtra(ColorPickerService.EXTRA_RESULT_CODE,res)
                            .putExtra(ColorPickerService.EXTRA_RESULT_DATA,data);
                    startForegroundService(i);
                    toast("屏幕取色已启动：拖动准星取样，轻点放大镜上的色值或坐标即复制");
                }catch(Exception e){toast("取色启动失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
            }else toast("未授权屏幕录制，取色已取消");
        }
    }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results){
        super.onRequestPermissionsResult(req,perms,results);
        if(req==COLOR_PICK_NOTIF_REQ){
            colorPickerNotifPending=false;
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)launchColorProjection();
            else toast("未授予通知权限，取色已取消");
        }
    }
    @Override protected void onResume(){super.onResume();if(web!=null)web.evaluateJavascript("window.onNativeResume&&window.onNativeResume()",null);}
    @Override public void onBackPressed(){if(mediaOverlay!=null){closeMedia();return;}if(terminalOverlay!=null){closeTerminal();return;}if(web!=null)web.evaluateJavascript("window.goBack&&window.goBack()",null);}
    @Override protected void onDestroy(){closeMedia();closeTerminal();if(mediaServer!=null){mediaServer.close();mediaServer=null;}if(engine!=null)engine.close();worker.shutdownNow();thumbnails.shutdownNow();if(web!=null){web.removeJavascriptInterface("CyberNative");web.destroy();web=null;}super.onDestroy();}
}
