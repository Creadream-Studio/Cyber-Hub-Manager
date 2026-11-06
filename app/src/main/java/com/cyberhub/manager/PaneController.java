package com.cyberhub.manager;

import android.app.Activity;
import android.os.Build;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/** 单个文件窗格：导航历史、加载列表、排序与显示选项。 */
public final class PaneController {
    interface Host{
        DataEngine engine();
        void onNavigated(PaneController pane);
        void onSelectionChanged();
        void onError(String message);
        boolean isHiddenFiles();
        int sortMode();
        boolean sortAscending();
    }

    public final Host host;
    public final ListView list;
    final TextView empty;
    public final FileListAdapter adapter;
    public String path="/storage/emulated/0";
    private final ArrayDeque<String> backStack=new ArrayDeque<>();
    private final ArrayDeque<String> forwardStack=new ArrayDeque<>();
    private boolean loading;

    public PaneController(Host host,ListView list,TextView empty){
        this.host=host;this.list=list;this.empty=empty;
        this.adapter=new FileListAdapter(list.getContext());
        list.setAdapter(adapter);
        empty.setVisibility(android.view.View.GONE);
    }

    public boolean isLoading(){return loading;}

    /** 异步加载目录并替换当前路径（不压历史栈）。 */
    public void load(String target){
        String p=normalize(target);
        DataEngine engine=host.engine();
        if(engine==null)return;
        loading=true;
        final String self=this.path;
        new Thread(()->{
            JSONArray arr;
            try{
                JSONObject args=new JSONObject().put("path",p).put("hidden",host.isHiddenFiles());
                Object result=engine.call("list",args);
                arr=(result instanceof JSONArray)?(JSONArray)result:new JSONArray();
            }catch(Throwable error){
                Activity a=(Activity)list.getContext();
                a.runOnUiThread(()->{
                    loading=false;
                    Toast.makeText(a,error.getMessage()==null?"无法读取此文件夹":error.getMessage(),Toast.LENGTH_LONG).show();
                    host.onError(error.getMessage()==null?"无法读取此文件夹":error.getMessage());
                });
                return;
            }
            sort(arr,host.sortMode(),host.sortAscending());
            Activity a=(Activity)list.getContext();
            a.runOnUiThread(()->{
                path=p;
                adapter.setEntries(arr);
                adapter.clearSelection();
                empty.setVisibility(arr.length()==0?android.view.View.VISIBLE:android.view.View.GONE);
                empty.setText("空文件夹");
                loading=false;
                host.onNavigated(this);
            });
        },"chm-list").start();
    }

    public void navigate(String target){
        String p=normalize(target);
        if(p.equals(path))return;
        backStack.push(path);
        forwardStack.clear();
        load(p);
    }

    public void goBack(){
        if(backStack.isEmpty())return;
        forwardStack.push(path);
        load(backStack.pop());
    }

    public boolean canGoBack(){return !backStack.isEmpty();}

    public void goForward(){
        if(forwardStack.isEmpty())return;
        backStack.push(path);
        load(forwardStack.pop());
    }

    public boolean canGoForward(){return !forwardStack.isEmpty();}

    public void goUp(){
        if(path.equals("/"))return;
        File parent=new File(path).getParentFile();
        navigate(parent==null?"/":parent.getPath());
    }

    public void refresh(){load(path);}

    /** 目录跳转后清空历史（切换位置类操作）。 */
    public void jump(String target){
        backStack.clear();
        forwardStack.clear();
        load(target);
    }

    static String normalize(String p){
        if(p==null||p.isEmpty())return "/storage/emulated/0";
        if(!p.startsWith("/"))p="/"+p;
        while(p.length()>1&&p.endsWith("/"))p=p.substring(0,p.length()-1);
        return p;
    }

    static void sort(JSONArray arr,final int mode,final boolean asc){
        try{
            ArrayList<JSONObject> items=new ArrayList<>();
            for(int i=0;i<arr.length();i++){JSONObject e=arr.optJSONObject(i);if(e!=null)items.add(e);}
            Collections.sort(items,new Comparator<JSONObject>(){
                @Override public int compare(JSONObject a,JSONObject b){
                    boolean da=a.optBoolean("dir"),db=b.optBoolean("dir");
                    if(da!=db)return da?-1:1;
                    int r;
                    switch(mode){
                        case 1:r=Long.compare(a.optLong("size",0),b.optLong("size",0));break;
                        case 2:r=Long.compare(a.optLong("time",0),b.optLong("time",0));break;
                        default:r=a.optString("name").compareToIgnoreCase(b.optString("name"));
                    }
                    if(r==0)r=a.optString("name").compareToIgnoreCase(b.optString("name"));
                    return asc?r:-r;
                }
            });
            for(int i=0;i<items.size();i++)arr.put(i,items.get(i));
        }catch(Throwable ignored){}
    }

    @SuppressWarnings("unused")
    private static String lower(String s){return s.toLowerCase(Locale.ROOT);}

    @SuppressWarnings("unused")
    private static boolean atLeast(int sdk){return Build.VERSION.SDK_INT>=sdk;}
}
