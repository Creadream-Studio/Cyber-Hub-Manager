package com.cyberhub.manager;

import android.Manifest;
import android.view.ActionMode;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** CH管理器 原生主界面（MP-Manager 风格双窗格文件管理，无 WebView）。 */
public class MainActivity extends Activity implements PaneController.Host {
    private volatile DataEngine engine;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private FrameLayout root;
    private PaneController pane1,pane2;
    private PaneController active;
    private boolean dualMode;
    private TextView tvPath,tvCount;
    private View paneWrap1,paneWrap2,paneDivider;
    private ActionMode actionMode;
    static final int UNINSTALL_REQ=4242;
    public volatile boolean uninstallBusy;

    // —— 终端（自旧版 MainActivity 移植）——
    private FrameLayout terminalOverlay;
    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private int terminalTextSize=14;
    private boolean terminalCtrl,terminalAlt,terminalKeyboardShown;
    private TextView terminalCtrlKey,terminalAltKey;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        System.setProperty("java.io.tmpdir",getCacheDir().getPath());
        try{engine=new DataEngine(this);}catch(Throwable error){
            FrameLayout fail=new FrameLayout(this);
            TextView msg=new TextView(this);
            msg.setText("引擎初始化失败："+error.getClass().getSimpleName()+" "+error.getMessage());
            msg.setPadding(40,60,40,40);msg.setTextColor(0xffe3e2e9);
            fail.addView(msg);setContentView(fail);return;
        }
        root=new FrameLayout(this);
        getLayoutInflater().inflate(R.layout.activity_main,root,true);
        setContentView(root);

        ListView list1=findViewById(R.id.list1),list2=findViewById(R.id.list2);
        pane1=new PaneController(this,list1,findViewById(R.id.empty1));
        pane2=new PaneController(this,list2,findViewById(R.id.empty2));
        active=pane1;
        paneWrap1=findViewById(R.id.paneWrap1);paneWrap2=findViewById(R.id.paneWrap2);
        paneDivider=findViewById(R.id.paneDivider);
        tvPath=findViewById(R.id.tvPath);tvCount=findViewById(R.id.tvCount);
        int screenWidth=getResources().getConfiguration().screenWidthDp;
        setDualMode(screenWidth>=600);
        pane1.load("/storage/emulated/0");
        if(dualMode)pane2.load("/storage/emulated/0");
        markActivePane();

        findViewById(R.id.btnBack).setOnClickListener(v->active.goBack());
        findViewById(R.id.btnForward).setOnClickListener(v->active.goForward());
        findViewById(R.id.btnUp).setOnClickListener(v->active.goUp());
        findViewById(R.id.btnAdd).setOnClickListener(v->showNewDialog());
        findViewById(R.id.btnSwap).setOnClickListener(v->swapPane());
        findViewById(R.id.btnMenu).setOnClickListener(v->showStorageDialog(v));
        findViewById(R.id.btnMore).setOnClickListener(v->showMoreMenu(v));
        bindPane(pane1);
        bindPane(pane2);
        checkPermissionIntro();
        String openPath=getIntent()==null?null:getIntent().getStringExtra("open_path");
        if(openPath!=null){
            File target=new File(openPath);
            if(target.isDirectory())pane1.jump(openPath);
            else openFile(openPath,target.getName());
        }
    }

    @Override public DataEngine engine(){return engine;}

    // ———————— 窗格绑定与交互 ————————
    private void bindPane(final PaneController pane){
        pane.list.setOnItemClickListener((parent,view,position,id)->{
            if(actionMode!=null){pane.adapter.toggleSelect(position);onSelectionChanged();return;}
            JSONObject e=pane.adapter.entryAt(position);
            if(e==null)return;
            if(e.optBoolean("dir"))pane.navigate(e.optString("path"));
            else openFile(e.optString("path"),e.optString("name"));
        });
        pane.list.setOnItemLongClickListener((parent,view,position,id)->{
            if(actionMode==null)actionMode=startActionMode(actionCallback);
            if(pane!=active)setActivePane(pane);
            pane.adapter.toggleSelect(position);
            onSelectionChanged();
            return true;
        });
        pane.list.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&pane!=active)setActivePane(pane);return false;});
    }

    private void setActivePane(PaneController pane){active=pane;markActivePane();}

    private void markActivePane(){
        pane1.list.setAlpha(!dualMode||active==pane1?1f:0.55f);
        pane2.list.setAlpha(!dualMode||active==pane2?1f:0.55f);
        updatePathHeader();
    }

    private void setDualMode(boolean on){
        dualMode=on;
        paneWrap2.setVisibility(on?View.VISIBLE:View.GONE);
        paneDivider.setVisibility(on?View.VISIBLE:View.GONE);
        if(on&&pane2.adapter.getCount()==0&&!pane2.isLoading())pane2.load(pane1.path);
        markActivePane();
    }

    private void swapPane(){
        if(!dualMode){
            File parent=new File(active.path).getParentFile();
            PaneController other=active==pane1?pane2:pane1;
            if(other.adapter.getCount()==0&&!other.isLoading())other.load(parent==null?"/":parent.getPath());
            setActivePane(active==pane1?pane2:pane1);
            Toast.makeText(this,"窗格 "+(active==pane1?"1":"2"),Toast.LENGTH_SHORT).show();
        }else{
            PaneController other=active==pane1?pane2:pane1;
            other.jump(active.path);
            Toast.makeText(this,"已同步另一窗格",Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onNavigated(PaneController pane){
        if(pane==active)updatePathHeader();
        clearSelection();
    }

    @Override public void onSelectionChanged(){
        if(actionMode!=null){
            int n=active.adapter.selected.size();
            if(n==0)actionMode.finish();
            else actionMode.setTitle("已选 "+n+" 项");
        }
    }

    @Override public void onError(String message){/* 列表错误已在加载处提示 */}

    @Override public boolean isHiddenFiles(){return getPreferences(Context.MODE_PRIVATE).getBoolean("hidden",false);}

    @Override public int sortMode(){return getPreferences(Context.MODE_PRIVATE).getInt("sort",0);}

    @Override public boolean sortAscending(){return getPreferences(Context.MODE_PRIVATE).getBoolean("asc",true);}

    private void updatePathHeader(){
        tvPath.setText(active.path);
        int count=active.adapter.getCount();
        String extra="";
        try{
            if(count>0){
                JSONObject last=active.adapter.entries().optJSONObject(count-1);
                if(last!=null&&last.has("free"))
                    extra=" · 剩余 "+Ui.formatSize(last.optLong("free",0));
            }
        }catch(Throwable ignored){}
        tvCount.setText("窗格 "+(active==pane1?"1":"2")+" · "+count+" 项"+extra);
    }

    // ———————— 打开文件 ————————
    void openFile(String path,String name){
        try{
            File f=new File(path);
            if(!f.isFile()){Ui.toast(this,"文件不存在");return;}
            String category=Ui.category(name,false);
            String ext=Ui.ext(name);
            switch(category){
                case "apk":startActivity(new Intent(this,ApkInfoActivity.class).putExtra("path",path));return;
                case "video":startActivity(new Intent(this,com.cyberhub.manager.player.MediaPlayerActivity.class)
                        .putExtra("play_path",path).putExtra("play_title",name));return;
                case "image":
                case "audio":openExternal(f,Ui.mimeFor(name));return;
                default:break;
            }
            if(ext.equals("html")||ext.equals("htm")||ext.equals("xhtml")){
                startActivity(new Intent(this,com.cyberhub.manager.preview.HtmlPreviewActivity.class)
                        .putExtra("preview_path",path).putExtra("preview_title",name));
                return;
            }
            if(Ui.isTextFile(name)&&f.length()<=FileOps.MAX_TEXT){
                startActivity(new Intent(this,TextEditorActivity.class).putExtra("path",path));
                return;
            }
            openExternal(f,Ui.mimeFor(name));
        }catch(Throwable error){Ui.toast(this,"无法打开："+error.getMessage());}
    }

    private void openExternal(File f,String mime){
        try{
            File shared=shareCopy(f);
            Intent i=new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(shareUri(shared),mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i,"打开方式"));
        }catch(Throwable error){Ui.toast(this,"无法打开："+error.getMessage());}
    }

    private File shareCopy(File src)throws IOException{
        File rootDir=new File(getCacheDir(),"shared");rootDir.mkdirs();
        File dst=FileOps.child(rootDir,src.getName());
        java.nio.file.Files.copy(src.toPath(),dst.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return dst;
    }

    private Uri shareUri(File f){
        return new Uri.Builder().scheme("content").authority(getPackageName()+".files").appendPath(f.getName()).build();
    }

    void install(File f){
        try{
            if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));
                Ui.toast(this,"允许安装后，再轻点安装");
                return;
            }
            Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(shareUri(f),"application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        }catch(Throwable e){Ui.toast(this,e.getMessage());}
    }

    // ———————— 多选与文件操作 ————————
    private void clearSelection(){
        for(PaneController pane:new PaneController[]{pane1,pane2}){
            if(!pane.adapter.selected.isEmpty()){pane.adapter.clearSelection();}
        }
        if(actionMode!=null){actionMode.finish();}
    }

    private final ActionMode.Callback actionCallback=new ActionMode.Callback(){
        @Override public boolean onCreateActionMode(ActionMode mode,Menu menu){
            mode.getMenuInflater().inflate(R.menu.menu_selection,mode.getMenu());
            return true;
        }
        @Override public boolean onPrepareActionMode(ActionMode mode,Menu menu){
            int n=active.adapter.selected.size();
            menu.findItem(R.id.actionRename).setVisible(n==1);
            menu.findItem(R.id.actionProps).setVisible(n==1);
            mode.setTitle("已选 "+n+" 项");
            return true;
        }
        @Override public boolean onActionItemClicked(ActionMode mode,MenuItem item){
            int id=item.getItemId();
            if(id==R.id.actionSelectAll)active.adapter.selectAll();
            else if(id==R.id.actionCopy){mode.finish();startTransfer(false);}
            else if(id==R.id.actionMove){mode.finish();startTransfer(true);}
            else if(id==R.id.actionDelete){mode.finish();confirmDelete();}
            else if(id==R.id.actionRename){mode.finish();showRenameDialog();}
            else if(id==R.id.actionProps){mode.finish();showProperties();}
            onSelectionChanged();
            return true;
        }
        @Override public void onDestroyActionMode(ActionMode mode){
            actionMode=null;
            pane1.adapter.clearSelection();pane2.adapter.clearSelection();
        }
    };

    private void startTransfer(final boolean move){
        final HashSet<String> selected=new HashSet<>(active.adapter.selected);
        if(selected.isEmpty())return;
        final PaneController sourcePane=active;
        final PaneController other=active==pane1?pane2:pane1;
        final String prefill=dualMode?other.path:sourcePane.path;
        final EditText input=promptDialog(move?"移动到…":"复制到…","目标文件夹",prefill,(inputBox)->{
            String dest=PaneController.normalize(inputBox.getText().toString());
            JSONArray files=new JSONArray();
            for(int i=0;i<sourcePane.adapter.getCount();i++){
                JSONObject e=sourcePane.adapter.entryAt(i);
                if(e!=null&&selected.contains(e.optString("path"))){
                    JSONObject o=new JSONObject();
                    try{o.put("path",e.optString("path")).put("name",e.optString("name"));files.put(o);}catch(Throwable ignored){}
                }
            }
            if(files.length()==0)return;
            final android.app.ProgressDialog progress=new android.app.ProgressDialog(this);
            progress.setMessage((move?"正在移动 ":"正在复制 ")+files.length()+" 项…");
            progress.setIndeterminate(true);progress.setCancelable(false);progress.show();
            worker.execute(()->{
                String error=null;
                try{engine.call("batchTransfer",new JSONObject().put("files",files).put("destination",dest).put("move",move));}
                catch(Throwable t){error=t.getMessage();}
                final String err=error;
                runOnUiThread(()->{
                    try{progress.dismiss();}catch(Throwable ignored){}
                    if(err!=null)Ui.toast(MainActivity.this,(move?"移动":"复制")+"失败："+err);
                    else Ui.toast(this,(move?"已移动 ":"已复制 ")+files.length()+" 项");
                    pane1.refresh();pane2.refresh();
                });
            });
        });
    }

    private void confirmDelete(){
        final HashSet<String> selected=new HashSet<>(active.adapter.selected);
        if(selected.isEmpty())return;
        new AlertDialog.Builder(this)
                .setTitle("删除")
                .setMessage("将 "+selected.size()+" 项移入回收站？")
                .setPositiveButton("删除",(d,w)->{
                    worker.execute(()->{
                        int ok=0;String error=null;
                        for(String path:selected){
                            try{engine.call("trash",new JSONObject().put("path",path));ok++;}
                            catch(Throwable t){error=t.getMessage();}
                        }
                        final int done=ok;final String err=error;
                        runOnUiThread(()->{
                            if(err!=null)Ui.toast(this,"部分删除失败："+err);
                            Ui.toast(this,"已删除 "+done+" 项");
                            pane1.refresh();pane2.refresh();
                        });
                    });
                })
                .setNegativeButton("取消",null).show();
    }

    private void showRenameDialog(){
        HashSet<String> selected=active.adapter.selected;
        if(selected.size()!=1)return;
        final String path=selected.iterator().next();
        final File src=new File(path);
        final EditText input=promptDialog("重命名","新名称",src.getName(),(inputBox)->{
            String name=inputBox.getText().toString().trim();
            if(name.isEmpty()||name.equals(src.getName()))return;
            worker.execute(()->{
                String error=null;
                try{engine.call("rename",new JSONObject().put("path",path).put("name",name));}
                catch(Throwable t){error=t.getMessage();}
                final String err=error;
                runOnUiThread(()->{
                    if(err!=null)Ui.toast(this,"重命名失败："+err);
                    pane1.refresh();pane2.refresh();
                });
            });
        });
    }

    private void showNewDialog(){
        new AlertDialog.Builder(this)
                .setTitle("新建")
                .setItems(new String[]{"新建文件夹","新建文件"},(d,w)->{
                    final boolean folder=w==0;
                    promptDialog(folder?"新建文件夹":"新建文件","名称","",(input)->{
                        String name=input.getText().toString().trim();
                        if(name.isEmpty())return;
                        worker.execute(()->{
                            String error=null;
                            try{engine.call("create",new JSONObject().put("path",active.path).put("name",name).put("folder",folder));}
                            catch(Throwable t){error=t.getMessage();}
                            final String err=error;
                            runOnUiThread(()->{
                                if(err!=null)Ui.toast(this,"创建失败："+err);
                                active.refresh();
                            });
                        });
                    });
                }).setNegativeButton("取消",null).show();
    }

    private void showProperties(){
        HashSet<String> selected=active.adapter.selected;
        if(selected.size()!=1)return;
        final String path=selected.iterator().next();
        worker.execute(()->{
            String text;
            try{
                JSONObject p=(JSONObject)engine.call("properties",new JSONObject().put("path",path));
                StringBuilder sb=new StringBuilder();
                sb.append("路径：").append(p.optString("path")).append("\n");
                sb.append("大小：").append(Ui.formatSize(p.optLong("size",0))).append("\n");
                sb.append("修改时间：").append(Ui.formatDate(p.optLong("modified",0))).append("\n");
                sb.append("可读：").append(p.optBoolean("readable")?"是":"否")
                  .append("  可写：").append(p.optBoolean("writable")?"是":"否");
                if(p.has("sha256"))sb.append("\nSHA-256：").append(p.optString("sha256"));
                text=sb.toString();
            }catch(Throwable t){text="读取失败："+t.getMessage();}
            final String body=text;
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("属性").setMessage(body)
                    .setPositiveButton("关闭",null).show());
        });
    }

    /** 弹出带输入框的对话框（返回输入框引用）。 */
    private EditText promptDialog(String title,String hint,String prefill,final OnConfirm action){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input=new EditText(this);
        input.setHint(hint);input.setText(prefill);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setBackground(getDrawable(R.drawable.bg_input));
        input.setTextColor(0xffe3e2e9);input.setHintTextColor(0xff9a9aa5);
        int pad=Ui.dp(this,12);
        box.setPadding(pad,pad/2,pad,pad/2);
        box.addView(input,new LinearLayout.LayoutParams(-1,-2));
        new AlertDialog.Builder(this).setTitle(title).setView(box)
                .setPositiveButton("确定",(d,w)->action.run(input))
                .setNegativeButton("取消",null).show();
        input.post(()->{input.requestFocus();input.setSelection(input.getText().length());
            InputMethodManager imm=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(imm!=null)imm.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
        return input;
    }

    interface OnConfirm{void run(EditText input);}

    // ———————— 顶部菜单 ————————
    private void showMoreMenu(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,"刷新");
        menu.getMenu().add(0,2,1,isHiddenFiles()?"隐藏隐藏文件":"显示隐藏文件");
        menu.getMenu().add(0,3,2,"排序：名称"+(sortMode()==0&&sortAscending()?" ↑":""));
        menu.getMenu().add(0,4,3,"排序：大小"+(sortMode()==1&&sortAscending()?" ↑":""));
        menu.getMenu().add(0,5,4,"排序：时间"+(sortMode()==2&&sortAscending()?" ↑":""));
        menu.getMenu().add(0,6,5,"切换升序/降序");
        menu.getMenu().add(0,7,6,"双窗格模式"+(dualMode?" ✓":""));
        menu.getMenu().add(0,8,7,"跳转到路径");
        menu.getMenu().add(0,9,8,"打开终端");
        menu.getMenu().add(0,10,9,"关于");
        menu.setOnMenuItemClickListener(item->{
            switch(item.getItemId()){
                case 1:pane1.refresh();pane2.refresh();return true;
                case 2:
                    getPreferences(Context.MODE_PRIVATE).edit().putBoolean("hidden",!isHiddenFiles()).apply();
                    pane1.refresh();pane2.refresh();return true;
                case 3:setSort(0);return true;
                case 4:setSort(1);return true;
                case 5:setSort(2);return true;
                case 6:
                    getPreferences(Context.MODE_PRIVATE).edit().putBoolean("asc",!sortAscending()).apply();
                    pane1.refresh();pane2.refresh();return true;
                case 7:setDualMode(!dualMode);return true;
                case 8:promptDialog("跳转到路径","输入绝对路径",active.path,(input)->
                        active.jump(PaneController.normalize(input.getText().toString())));return true;
                case 9:openTerminal(active.path);return true;
                case 10:showAbout();return true;
            }
            return false;
        });
        menu.show();
    }

    private void setSort(int mode){
        getPreferences(Context.MODE_PRIVATE).edit().putInt("sort",mode).apply();
        pane1.refresh();pane2.refresh();
    }

    private void showAbout(){
        new AlertDialog.Builder(this)
                .setTitle("CH管理器 · 0.0.1")
                .setMessage("CH Manager（CH管理器）原生界面版\n\nMP-Manager 风格双窗格文件管理。\n引擎层完整保留：DEX/APK 工具、网络存储、终端、Root/Shizuku。\n\n本版本为原生界面首个测试版。")
                .setPositiveButton("关闭",null).show();
    }

    private void showStorageDialog(View anchor){
        final String[][] places={
                {"主存储","/storage/emulated/0"},
                {"下载","/storage/emulated/0/Download"},
                {"文档","/storage/emulated/0/Documents"},
                {"图片","/storage/emulated/0/Pictures"},
                {"相机","/storage/emulated/0/DCIM/Camera"},
                {"音乐","/storage/emulated/0/Music"},
                {"工作区",new File(getFilesDir(),"工作区").getPath()},
                {"根目录","/"},
        };
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        for(final String[] place:places){
            View row=LayoutInflater.from(this).inflate(R.layout.item_storage,box,false);
            ImageView icon=row.findViewById(R.id.ivStorageIcon);
            icon.setImageResource(place[0].equals("根目录")?R.drawable.ic_terminal:R.drawable.ic_storage);
            icon.setColorFilter(getColor(R.color.primary));
            TextView name=row.findViewById(R.id.tvStorageName);
            name.setText(place[0]);
            row.setOnClickListener(v->{setActivePane(active);active.jump(place[1]);((AlertDialog)v.getTag()).dismiss();});
            box.addView(row);
        }
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("位置").setView(box)
                .setNegativeButton("关闭",null).create();
        for(int i=0;i<box.getChildCount();i++)box.getChildAt(i).setTag(dialog);
        dialog.show();
    }

    private void checkPermissionIntro(){
        worker.execute(()->{
            boolean ok=false;
            try{ok=((JSONObject)engine.call("init",new JSONObject())).optBoolean("permission",false);}catch(Throwable ignored){}
            final boolean granted=ok;
            runOnUiThread(()->{
                if(granted)return;
                new AlertDialog.Builder(this)
                        .setTitle("授予文件访问权限")
                        .setMessage("浏览手机文件需要「所有文件访问」权限。\n轻点「去授权」后打开开关并返回。")
                        .setPositiveButton("去授权",(d,w)->storagePermission())
                        .setNegativeButton("稍后",null)
                        .show();
            });
        });
    }

    private void storagePermission(){
        try{
            if(Build.VERSION.SDK_INT>=30)startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));
            else requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},10);
        }catch(Exception e){
            try{startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}catch(Throwable ignored){}
        }
    }

    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        pane1.jump("/storage/emulated/0");pane2.jump("/storage/emulated/0");
    }

    // ———————— 终端（移植自旧版 WebView 壳） ————————
    void bundleProgress(String job,String message){Ui.toast(this,message);}

    private void openTerminal(String path){
        if(terminalOverlay!=null||root==null)return;
        File cwd=new File(path);
        if(!cwd.isDirectory())cwd=getFilesDir();
        final String cwdPath=cwd.getAbsolutePath();
        final String home=getFilesDir().getAbsolutePath();
        terminalTextSize=14;terminalKeyboardShown=false;
        com.termux.view.TerminalView view=new com.termux.view.TerminalView(this,null);
        terminalView=view;
        view.setTerminalViewClient(new TermViewClient());
        view.setTextSize(terminalTextSize);
        view.setBackgroundColor(0xff000000);
        view.setTypeface(Typeface.MONOSPACE);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setDefaultFocusHighlightEnabled(false);
        terminalSession=new TerminalSession("/system/bin/sh",cwdPath,new String[]{"-i"},terminalEnv(home,cwdPath),2000,new TermSessionClient());
        view.attachSession(terminalSession);
        terminalOverlay=new FrameLayout(this);
        terminalOverlay.setBackgroundColor(0xff000000);
        terminalOverlay.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30)terminalKeyboardShown=insets.isVisible(WindowInsets.Type.ime());
            else terminalKeyboardShown=insets.getSystemWindowInsetBottom()>Ui.dp(this,80);
            return insets;
        });
        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout keys=new LinearLayout(this);
        keys.setOrientation(LinearLayout.VERTICAL);
        keys.setBackgroundColor(0xff000000);
        addTerminalKeyRow(keys,new String[][]{{"Esc","esc"},{"Tab","tab"},{"PgUp","pgup"},{"Home","home"},{"▲","up"},{"End","end"},{"⋮","menu"}});
        addTerminalKeyRow(keys,new String[][]{{"Ctrl","ctrl"},{"Alt","alt"},{"PgDn","pgdn"},{"◀","left"},{"▼","down"},{"▶","right"},{"⇄","keyboard"}});
        column.addView(keys,new LinearLayout.LayoutParams(-1,-2));
        terminalOverlay.addView(column,new FrameLayout.LayoutParams(-1,-1));
        root.addView(terminalOverlay,new FrameLayout.LayoutParams(-1,-1));
        view.post(()->{if(terminalView!=null){terminalView.requestFocus();showTerminalKeyboard();}});
    }

    private void addTerminalKeyRow(LinearLayout parent,String[][] keys){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for(final String[] key:keys){
            TextView b=new TextView(this);
            b.setText(key[0]);b.setTextSize(15);b.setTextColor(0xfff0f0f0);b.setTypeface(Typeface.MONOSPACE);
            b.setGravity(Gravity.CENTER);b.setBackgroundColor(0xff000000);b.setClickable(true);b.setFocusable(false);
            b.setOnClickListener(v->handleTerminalKey(key[1],b));
            if(key[1].equals("ctrl"))terminalCtrlKey=b;
            if(key[1].equals("alt"))terminalAltKey=b;
            row.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,42),1));
        }
        parent.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(this,42)));
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

    private void toggleTerminalKeyboard(){if(terminalKeyboardShown)hideTerminalKeyboard();else showTerminalKeyboard();}

    private void changeTerminalTextSize(int delta){
        int size=Math.max(8,Math.min(40,terminalTextSize+delta));
        if(size==terminalTextSize)return;
        terminalTextSize=size;
        TerminalView view=terminalView;
        if(view!=null){view.setTextSize(size);view.requestFocus();}
    }

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
        menu.getMenu().add(0,6,4,"关闭终端");
        menu.setOnMenuItemClickListener(item->{
            switch(item.getItemId()){
                case 1:pasteTerminal();return true;
                case 2:writeTerminal("\u000c");return true;
                case 4:changeTerminalTextSize(1);return true;
                case 5:changeTerminalTextSize(-1);return true;
                case 6:closeTerminal();return true;
            }
            return false;
        });
        menu.show();
    }

    private void closeTerminal(){
        if(terminalOverlay==null)return;
        FrameLayout overlay=terminalOverlay;
        hideTerminalKeyboard();
        terminalOverlay=null;terminalView=null;terminalCtrlKey=null;terminalAltKey=null;
        terminalCtrl=false;terminalAlt=false;terminalKeyboardShown=false;
        TerminalSession session=terminalSession;terminalSession=null;
        if(root!=null)root.removeView(overlay);
        if(session!=null){try{session.finishIfRunning();}catch(Throwable ignored){}}
    }

    private String[] terminalEnv(String home,String cwd){
        File envFile=new File(getCacheDir(),"termux.env");
        String rc=
            "PS1='$(ch_prompt)'\n"+
            "ch_prompt(){\n"+
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
            "  command ls -1F \"$@\" 2>/dev/null | while IFS= read -r ch_entry; do\n"+
            "    case \"$ch_entry\" in\n"+
            "      */) printf '\\033[01;34m%s\\033[0m\\n' \"$ch_entry\" ;;\n"+
            "      \\*) printf '\\033[01;32m%s\\033[0m\\n' \"$ch_entry\" ;;\n"+
            "      *) printf '%s\\n' \"$ch_entry\" ;;\n"+
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
        };
    }

    private final class TermSessionClient implements TerminalSessionClient {
        @Override public void onTextChanged(TerminalSession session){runOnUiThread(()->{if(terminalView!=null)terminalView.onScreenUpdated();});}
        @Override public void onTitleChanged(TerminalSession session){}
        @Override public void onSessionFinished(TerminalSession session){runOnUiThread(()->{if(terminalOverlay!=null){Ui.toast(MainActivity.this,"终端会话已结束");closeTerminal();}});}
        @Override public void onCopyTextToClipboard(TerminalSession session,String text){runOnUiThread(()->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("CH管理器",text));});}
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

    // ———————— 返回键 ————————
    @Override public void onBackPressed(){
        if(terminalOverlay!=null){closeTerminal();return;}
        if(active.canGoBack()){active.goBack();return;}
        if(!active.path.equals("/storage/emulated/0")&&!active.path.equals("/")){active.goUp();return;}
        super.onBackPressed();
    }

    @Override protected void onDestroy(){
        super.onDestroy();
        worker.shutdownNow();
    }
}
