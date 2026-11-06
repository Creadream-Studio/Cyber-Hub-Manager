package com.cyberhub.manager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/** APK 信息页（原生，替代 WebView 版「APK 信息」）。 */
public class ApkInfoActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.activity_apk_info);
        final String path=getIntent().getStringExtra("path");
        ImageView icon=findViewById(R.id.ivApkIcon);
        icon.setImageResource(R.drawable.ic_apk);
        icon.setColorFilter(getColor(R.color.apk_tint));
        TextView tvName=findViewById(R.id.tvApkName);
        TextView tvPackage=findViewById(R.id.tvApkPackage);
        TextView details=findViewById(R.id.tvApkDetails);
        Button install=findViewById(R.id.btnApkInstall);
        tvName.setText(new File(path).getName());
        tvPackage.setText(path);
        details.setText("正在解析…");
        install.setOnClickListener(v->{
            try{
                File f=new File(path);
                File cacheDir=new File(getCacheDir(),"shared");cacheDir.mkdirs();
                File dst=FileOps.child(cacheDir,f.getName());
                java.nio.file.Files.copy(f.toPath(),dst.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".files").appendPath(dst.getName()).build();
                if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
                    startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));
                    Ui.toast(this,"允许安装后，再轻点安装");
                    return;
                }
                startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            }catch(Throwable e){Ui.toast(this,"安装失败："+e.getMessage());}
        });
        new Thread(()->{
            final StringBuilder sb=new StringBuilder();
            String shown=new File(path).getName();
            try{
                PackageManager pm=getPackageManager();
                PackageInfo info=pm.getPackageArchiveInfo(path,PackageManager.GET_PERMISSIONS|PackageManager.GET_ACTIVITIES);
                if(info!=null){
                    ApplicationInfo app=info.applicationInfo;
                    app.sourceDir=path;app.publicSourceDir=path;
                    try{shown=String.valueOf(pm.getApplicationLabel(app));}catch(Throwable ignored){}
                    try{
                        Drawable d=pm.getApplicationIcon(app);
                        runOnUiThread(()->{ImageView iv=findViewById(R.id.ivApkIcon);iv.setImageDrawable(d);iv.setColorFilter(null);});
                    }catch(Throwable ignored){}
                    sb.append("应用名：").append(shown).append("\n");
                    sb.append("包名：").append(info.packageName).append("\n");
                    sb.append("版本：").append(info.versionName==null?"未知":info.versionName)
                      .append("（versionCode ").append(info.versionCode).append("）\n");
                    sb.append("大小：").append(Ui.formatSize(new File(path).length())).append("\n");
                    sb.append("SDK：min ").append(app.minSdkVersion<=0?"—":String.valueOf(app.minSdkVersion))
                      .append(" / target ").append(app.targetSdkVersion<=0?"—":String.valueOf(app.targetSdkVersion)).append("\n");
                    if(info.permissions!=null&&info.permissions.length>0){
                        Set<String> perms=new TreeSet<>();
                        for(android.content.pm.PermissionInfo p:info.permissions)perms.add(p.name);
                        sb.append("\n权限（").append(perms.size()).append("）：\n");
                        for(String p:perms)sb.append("· ").append(p).append("\n");
                    }
                    if(info.activities!=null&&info.activities.length>0)
                        sb.append("\n组件：").append(info.activities.length).append(" 个 Activity\n");
                    try{sb.append("\nSHA-256：\n").append(FileOps.sha256(new File(path)));}catch(Throwable ignored){}
                }else sb.append("无法解析该 APK（可能已损坏）。\n");
            }catch(Throwable t){sb.append("解析失败：").append(t.getMessage()).append("\n");}
            final String body=sb.toString();
            final String label=shown;
            runOnUiThread(()->{
                ((TextView)findViewById(R.id.tvApkDetails)).setText(body);
                ((TextView)findViewById(R.id.tvApkName)).setText(label);
            });
        },"chm-apkinfo").start();
    }
}
