package com.cyberhub.manager;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 通过系统“使用此文件夹”授权访问 Android/data 与 Android/obb，无需 Root 或 Shizuku。
 * 授权后所有读写都走 DocumentsContract，普通文件路径只在界面中使用。
 */
public final class SafAccess {
    private static final String AUTHORITY="com.android.externalstorage.documents";
    public static final int REQUEST=210;
    private static String pendingTarget;

    private SafAccess(){}

    private static final class Grant {
        final Uri tree;
        final String root;
        Grant(Uri tree,String root){this.tree=tree;this.root=root;}
    }

    /** 打开系统文件夹选择器，定位到 Android/data 或 Android/obb。 */
    public static void request(Activity activity,String which){
        request(activity,which,true);
    }

    private static void request(Activity activity,String which,boolean initial){
        String target="primary:Android/"+("obb".equals(which)?"obb":"data");
        pendingTarget=target;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.provider.extra.SHOW_ADVANCED",true);
        intent.putExtra("android.content.extra.SHOW_ADVANCED",true);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,DocumentsContract.buildDocumentUri(AUTHORITY,target));
        try{activity.startActivityForResult(intent,REQUEST);}catch(Exception e){
            try{
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,DocumentsContract.buildDocumentUri(AUTHORITY,"primary:Android"));
                activity.startActivityForResult(intent,REQUEST);
            }catch(Exception ignored){}
        }
    }

    /** 处理系统选择器返回；返回提示文本，失败返回 null。 */
    public static String take(Activity activity,int resultCode,Intent data){
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null){
            pendingTarget=null;
            return null;
        }
        Uri uri=data.getData();
        int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(flags==0)flags=Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try{activity.getContentResolver().takePersistableUriPermission(uri,flags);}
        catch(Exception ignored){try{activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored2){}}
        String selected;
        try{selected=DocumentsContract.getTreeDocumentId(uri);}catch(Exception e){pendingTarget=null;return null;}
        String target=pendingTarget;
        pendingTarget=null;
        if(target==null)return selected;
        if(target.equals(selected))return "已授权 "+pretty(target);
        if(selected.startsWith(target+"/"))return "已授权 "+pretty(selected);
        if(target.startsWith(selected+"/")){
            request(activity,target.endsWith("/obb")?"obb":"data",false);
            return "请继续选择 "+pretty(target);
        }
        if(selected.startsWith("primary:Android")||selected.equals("primary:Android"))
            return "已选择 "+pretty(selected)+"，请重试授权 "+pretty(target);
        return "所选文件夹不是 "+pretty(target)+"，请重试";
    }

    private static String pretty(String docId){
        int i=docId.indexOf(':');
        return i<0?docId:docId.substring(i+1).replace('/','/');
    }

    /** 当前文件/目录是否已有可用的 SAF 授权。 */
    public static boolean hasAccess(Context context,File file){
        return grant(context,file)!=null;
    }

    public static JSONArray listDetailed(Context context,File dir,boolean hidden){
        Grant grant=grant(context,dir);
        if(grant==null)return null;
        String parent=docId(dir);
        if(parent==null)return null;
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(grant.tree,parent);
        String[] projection={
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        Cursor cursor=null;
        try{
            cursor=context.getContentResolver().query(children,projection,null,null,null);
            if(cursor==null)return null;
            List<JSONObject> rows=new ArrayList<>();
            while(cursor.moveToNext()){
                String name=cursor.getString(1);
                if(name==null||name.isEmpty())continue;
                if(!hidden&&name.startsWith("."))continue;
                boolean isDir=DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2));
                long size=cursor.isNull(3)?0:cursor.getLong(3);
                long time=cursor.isNull(4)?0:cursor.getLong(4);
                rows.add(new JSONObject()
                        .put("name",name)
                        .put("path",new File(dir,name).getPath())
                        .put("dir",isDir)
                        .put("size",isDir?0:size)
                        .put("time",time));
            }
            rows.sort(Comparator.comparing((JSONObject o)->!o.optBoolean("dir"))
                    .thenComparing(o->o.optString("name").toLowerCase(java.util.Locale.ROOT)));
            JSONArray out=new JSONArray();
            for(JSONObject row:rows)out.put(row);
            return out;
        }catch(Throwable t){
            return null;
        }finally{if(cursor!=null)cursor.close();}
    }

    public static byte[] readFile(Context context,File file,long limit){
        Grant grant=grant(context,file);
        if(grant==null)return null;
        String id=docId(file);
        if(id==null)return null;
        try(InputStream in=context.getContentResolver().openInputStream(DocumentsContract.buildDocumentUriUsingTree(grant.tree,id))){
            if(in==null)return null;
            return FileOps.bytes(in,limit);
        }catch(Throwable t){
            return null;
        }
    }

    public static boolean writeFile(Context context,File file,byte[] data,boolean backup){
        Grant grant=grant(context,file);
        if(grant==null)return false;
        String id=docId(file);
        if(id==null)return false;
        Uri document=DocumentsContract.buildDocumentUriUsingTree(grant.tree,id);
        try{
            if(!exists(context,document)){
                if(!create(context,file,false))return false;
                document=DocumentsContract.buildDocumentUriUsingTree(grant.tree,docId(file));
            }
            if(backup)backup(context,grant,file,document);
            try(OutputStream out=context.getContentResolver().openOutputStream(document,"wt")){
                if(out==null)return false;
                out.write(data);
                out.flush();
            }
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    public static boolean create(Context context,File file,boolean folder){
        if(file.getParentFile()==null)return false;
        Grant grant=grant(context,file.getParentFile());
        if(grant==null)return false;
        String parent=docId(file.getParentFile());
        if(parent==null)return false;
        try{
            Uri parentUri=DocumentsContract.buildDocumentUriUsingTree(grant.tree,parent);
            String mime=folder?DocumentsContract.Document.MIME_TYPE_DIR:mime(file.getName());
            return DocumentsContract.createDocument(context.getContentResolver(),parentUri,mime,file.getName())!=null;
        }catch(Throwable t){
            return false;
        }
    }

    public static boolean delete(Context context,File file){
        Grant grant=grant(context,file);
        if(grant==null)return false;
        String id=docId(file);
        if(id==null)return false;
        try{return DocumentsContract.deleteDocument(context.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(grant.tree,id));}
        catch(Throwable t){return false;}
    }

    public static boolean rename(Context context,File file,String newName){
        Grant grant=grant(context,file);
        if(grant==null)return false;
        String id=docId(file);
        if(id==null)return false;
        try{return DocumentsContract.renameDocument(context.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(grant.tree,id),newName)!=null;}
        catch(Throwable t){return false;}
    }

    public static long lastModified(Context context,File file){
        Grant grant=grant(context,file);
        if(grant==null)return 0;
        String id=docId(file);
        if(id==null)return 0;
        Cursor cursor=null;
        try{
            cursor=context.getContentResolver().query(DocumentsContract.buildDocumentUriUsingTree(grant.tree,id),
                    new String[]{DocumentsContract.Document.COLUMN_LAST_MODIFIED},null,null,null);
            return cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0)?cursor.getLong(0):0;
        }catch(Throwable t){return 0;}
        finally{if(cursor!=null)cursor.close();}
    }

    private static boolean exists(Context context,Uri document){
        Cursor cursor=null;
        try{
            cursor=context.getContentResolver().query(document,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null);
            return cursor!=null&&cursor.moveToFirst();
        }catch(Throwable t){return false;}
        finally{if(cursor!=null)cursor.close();}
    }

    private static void backup(Context context,Grant grant,File file,Uri document)throws Exception{
        String parentId=docId(file.getParentFile());
        if(parentId==null)return;
        Uri parent=DocumentsContract.buildDocumentUriUsingTree(grant.tree,parentId);
        String name=file.getName()+".bak";
        Uri backup=findChild(context,parent,name);
        if(backup==null)backup=DocumentsContract.createDocument(context.getContentResolver(),parent,mime(name),name);
        if(backup==null)return;
        try(InputStream in=context.getContentResolver().openInputStream(document);
            OutputStream out=context.getContentResolver().openOutputStream(backup,"wt")){
            if(in==null||out==null)return;
            byte[] buffer=new byte[65536];
            int n;
            while((n=in.read(buffer))>0)out.write(buffer,0,n);
        }
    }

    private static Uri findChild(Context context,Uri parent,String name){
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));
        Cursor cursor=null;
        try{
            cursor=context.getContentResolver().query(children,new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null);
            if(cursor==null)return null;
            while(cursor.moveToNext())
                if(name.equals(cursor.getString(1)))
                    return DocumentsContract.buildDocumentUriUsingTree(parent,cursor.getString(0));
        }catch(Throwable ignored){
        }finally{if(cursor!=null)cursor.close();}
        return null;
    }

    private static String mime(String name){
        String lower=name.toLowerCase(java.util.Locale.ROOT);
        if(lower.endsWith(".txt")||lower.endsWith(".log")||lower.endsWith(".json")||lower.endsWith(".xml")||lower.endsWith(".html")||lower.endsWith(".css")||lower.endsWith(".js")||lower.endsWith(".java")||lower.endsWith(".smali"))
            return "text/plain";
        if(lower.endsWith(".png"))return "image/png";
        if(lower.endsWith(".jpg")||lower.endsWith(".jpeg"))return "image/jpeg";
        if(lower.endsWith(".apk"))return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    private static Grant grant(Context context,File file){
        String target=docId(file);
        if(target==null)return null;
        Grant best=null;
        for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions()){
            if(!permission.isReadPermission())continue;
            Uri uri=permission.getUri();
            if(uri==null||!AUTHORITY.equals(uri.getAuthority()))continue;
            String root;
            try{root=DocumentsContract.getTreeDocumentId(uri);}catch(Exception e){continue;}
            if(root==null||root.isEmpty())continue;
            if(target.equals(root)||target.startsWith(root+"/")){
                if(best==null||root.length()>best.root.length())best=new Grant(uri,root);
            }
        }
        return best;
    }

    /** 把普通路径映射为 DocumentsContract 文档 ID（primary:Android/data/...）。 */
    private static String docId(File file){
        String path=file.getAbsolutePath().replace('\\','/');
        int index=path.indexOf("/Android/");
        if(index<0)return null;
        String volume=volumeId(path.substring(0,index));
        if(volume==null)return null;
        return volume+":"+path.substring(index+1);
    }

    private static String volumeId(String root){
        if(root.equals("/sdcard")||root.equals("/storage/self/primary"))return "primary";
        if(root.startsWith("/storage/emulated/")||root.startsWith("/mnt/user/")||root.startsWith("/data/media/"))return "primary";
        if(root.startsWith("/storage/")){
            String id=root.substring("/storage/".length());
            if(!id.isEmpty()&&id.indexOf('/')<0)return id;
        }
        return null;
    }
}
