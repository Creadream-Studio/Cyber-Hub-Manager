package com.cyberhub.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.nio.charset.StandardCharsets;

/** 文本编辑器（原生，替代 WebView 版 CodeMirror 编辑的基础场景）。 */
public class TextEditorActivity extends Activity {
    private File target;
    private boolean dirty;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.activity_text_editor);
        final String path=getIntent().getStringExtra("path");
        target=new File(path);
        TextView title=findViewById(R.id.tvEditorTitle);
        title.setText(target.getName());
        final EditText editor=findViewById(R.id.editorText);
        editor.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(android.text.Editable s){dirty=true;}
        });
        new Thread(()->{
            String text;
            String warning=null;
            try{
                byte[] raw=FileOps.read(target,FileOps.MAX_TEXT);
                boolean binary=false;
                int scan=Math.min(raw.length,8192);
                for(int i=0;i<scan;i++)if(raw[i]==0){binary=true;break;}
                if(binary)warning="此文件包含二进制数据，保存可能损坏文件（已按 UTF-8 解码显示）。";
                text=new String(raw,StandardCharsets.UTF_8);
            }catch(Throwable t){text="";warning="读取失败："+t.getMessage();}
            final String body=text;
            final String warn=warning;
            runOnUiThread(()->{
                editor.setText(body);
                if(warn!=null)Toast.makeText(this,warn,Toast.LENGTH_LONG).show();
                dirty=false;
            });
        },"chm-editor-read").start();
        findViewById(R.id.btnEditorBack).setOnClickListener(v->confirmClose());
        findViewById(R.id.btnEditorSave).setOnClickListener(v->save());
    }

    private void save(){
        final EditText editor=findViewById(R.id.editorText);
        final String text=editor.getText().toString();
        new Thread(()->{
            String error=null;
            try{FileOps.writeAtomic(target,text.getBytes(StandardCharsets.UTF_8),true);}
            catch(Throwable t){error=t.getMessage();}
            final String err=error;
            runOnUiThread(()->{
                if(err!=null)Toast.makeText(this,"保存失败："+err,Toast.LENGTH_LONG).show();
                else{Toast.makeText(this,"已保存（原文件保留 .bak 备份）",Toast.LENGTH_SHORT).show();dirty=false;}
            });
        },"chm-editor-save").start();
    }

    private void confirmClose(){
        if(!dirty){finish();return;}
        new AlertDialog.Builder(this)
                .setTitle("未保存的修改")
                .setMessage("是否保存后再退出？")
                .setPositiveButton("保存并退出",(d,w)->{save();finish();})
                .setNeutralButton("直接退出",(d,w)->finish())
                .setNegativeButton("继续编辑",null)
                .show();
    }

    @Override public void onBackPressed(){
        confirmClose();
    }
}
