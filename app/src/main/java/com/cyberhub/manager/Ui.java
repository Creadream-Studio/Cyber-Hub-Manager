package com.cyberhub.manager;

import android.content.Context;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 原生 UI 小工具：尺寸、图标分类、格式化。 */
public final class Ui {
    private Ui(){}

    public static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}

    public static void toast(Context c,String s){Toast.makeText(c,s,Toast.LENGTH_LONG).show();}

    public static String ext(String name){
        int i=name.lastIndexOf('.');
        return i<0?"":name.substring(i+1).toLowerCase(Locale.ROOT);
    }

    /** 图标资源分类：folder/text/image/audio/video/archive/apk/code/file */
    public static String category(String name,boolean dir){
        if(dir)return "folder";
        String e=ext(name);
        switch(e){
            case "apk":case "apks":case "xapk":case "apkm":case "ipa":case "hap":case "iap":return "apk";
            case "zip":case "rar":case "7z":case "tar":case "gz":case "tgz":case "bz2":case "xz":case "zst":case "lz4":case "iso":case "jar":case "cab":return "archive";
            case "jpg":case "jpeg":case "png":case "gif":case "webp":case "bmp":case "svg":case "ico":case "heic":case "avif":case "tif":case "tiff":case "dng":return "image";
            case "mp3":case "wav":case "flac":case "ogg":case "opus":case "m4a":case "aac":case "wma":case "amr":case "mid":case "ape":case "mka":case "ac3":case "dts":return "audio";
            case "mp4":case "mkv":case "avi":case "mov":case "webm":case "flv":case "wmv":case "ts":case "3gp":case "rmvb":case "m4v":case "mpg":return "video";
            case "java":case "smali":case "kt":case "c":case "cpp":case "h":case "hpp":case "py":case "sh":case "js":case "css":case "json":case "gradle":case "xml":case "html":case "htm":case "php":case "rs":case "go":case "yaml":case "yml":case "ini":case "properties":return "code";
            case "txt":case "md":case "log":case "csv":case "text":case "lrc":case "srt":case "conf":case "list":return "text";
            default:return "file";
        }
    }

    public static int iconRes(String category){
        switch(category){
            case "folder":return R.drawable.ic_folder;
            case "apk":return R.drawable.ic_apk;
            case "archive":return R.drawable.ic_archive;
            case "image":return R.drawable.ic_image;
            case "audio":return R.drawable.ic_audio;
            case "video":return R.drawable.ic_video;
            case "code":return R.drawable.ic_code;
            case "text":return R.drawable.ic_text;
            default:return R.drawable.ic_file;
        }
    }

    public static int tintRes(String category){
        switch(category){
            case "folder":return R.color.folder_tint;
            case "apk":return R.color.apk_tint;
            case "archive":return R.color.archive_tint;
            case "image":return R.color.image_tint;
            case "audio":return R.color.audio_tint;
            case "video":return R.color.video_tint;
            case "code":return R.color.code_tint;
            default:return R.color.text_tint;
        }
    }

    public static boolean isTextFile(String name){
        String c=category(name,false);
        return c.equals("text")||c.equals("code");
    }

    /** 常用 MIME 映射（交给系统打开时用）。 */
    public static String mimeFor(String name){
        switch(ext(name)){
            case "jpg":case "jpeg":case "png":case "gif":case "webp":case "bmp":case "svg":case "ico":case "heic":case "avif":return "image/*";
            case "mp3":case "wav":case "flac":case "ogg":case "opus":case "m4a":case "aac":case "wma":case "amr":case "mid":case "ape":case "mka":return "audio/*";
            case "mp4":case "mkv":case "avi":case "mov":case "webm":case "flv":case "wmv":case "ts":case "3gp":case "rmvb":case "m4v":return "video/*";
            case "pdf":return "application/pdf";
            case "zip":return "application/zip";
            case "jar":return "application/java-archive";
            case "apk":return "application/vnd.android.package-archive";
            case "html":case "htm":case "xhtml":return "text/html";
            case "css":return "text/css";
            case "js":return "text/javascript";
            case "json":return "application/json";
            case "xml":return "text/xml";
            case "doc":case "docx":return "application/msword";
            case "xls":case "xlsx":case "csv":return "text/plain";
            case "ppt":case "pptx":return "application/vnd.ms-powerpoint";
            default:break;
        }
        if(isTextFile(name))return "text/plain";
        return "application/octet-stream";
    }

    public static String formatSize(long bytes){
        if(bytes<1024)return bytes+" B";
        double v=bytes;
        String[]u={"KB","MB","GB","TB"};int i=-1;
        while(v>=1024&&i<u.length-1){v/=1024;i++;}
        return String.format(Locale.ROOT,v>=100?"%.0f %s":"%.1f %s",v,u[Math.max(i,0)]);
    }

    public static String formatDate(long time){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT).format(new Date(time));
    }
}
