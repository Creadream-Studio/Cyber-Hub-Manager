const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.41',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'a.txt',path:'${root}/a.txt',dir:false,size:1,time:1}];data.total=1;data.free=1;break;
   case'clipboardGet':data='https://clip.example.com/p.txt';break;
   case'log':data=[{text:'已创建 /x.txt',time:${Date.now()}},{text:'打开文件 /y.apk',time:${Date.now()-2*86400000}},{text:'远程 Payload 已拉取 https://e.com/p',time:${Date.now()-3600000}}];break;
   case'logRemove':case'logClear':case'record':case'logExport':data='${root}/下载/活动记录-test.txt';break;
   case'httpGet':data={status:200,bytes:2048,body:'# 注释\\nshell ls /data\\ntoast 你好'};break;
   case'shellExec':data='bin\\napp\\n[退出码 0]';break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,act,payloadPage,logPage,pref};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=s=>w.eval(s.replace(/\b(state|act|payloadPage|logPage|pref)\b/g,'window.__test.$1'));
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
// —— 远程 Payload ——
await evaluate("act(payloadPage)");
await wait(()=>w.document.querySelector('#payloadRun'),'payload page');
// 1) 粘贴按钮读取剪贴板
w.document.querySelector('#payloadPaste').click();await sleep();
assert.equal(w.document.querySelector('#payloadUrl').value,'https://clip.example.com/p.txt','paste fills input');
pass('粘贴按钮读取剪贴板');
// 2) 拉取执行 → 保存地址并写入历史
w.document.querySelector('#payloadRun').click();
await wait(()=>w.document.querySelector('#payloadHistory .listrow'),'history rendered');
assert.equal(evaluate("pref.payloadHistory[0]"),'https://clip.example.com/p.txt','history saved');
await wait(()=>w.document.querySelector('#payloadCopy'),'run done');
assert(w.document.querySelector('#payloadLog').textContent.includes('拉取 2.0 KB'),'bytes shown');
pass('执行后写入历史并显示字节数');
// 3) shell 结果点击弹完整输出
w.document.querySelectorAll('#payloadLog [data-out]')[0].click();
await wait(()=>[...w.document.querySelectorAll('#modalLayer h2, #modalLayer h3')].some(h=>h.textContent==='shell 输出'),'alert');
assert(w.document.body.textContent.includes('退出码 0'),'full shell output shown');
[...w.document.querySelectorAll('#modalLayer button')].find(b=>b.textContent==='确定').click();await sleep();
pass('shell 结果点击查看完整输出');
// 4) 复制结果
w.document.querySelector('#payloadCopy').click();await sleep();
assert(w.window.__calls.some(c=>c.action==='clipboard'&&c.args.text&&c.args.text.includes('shell ✓')),'copy result rpc');
pass('复制结果按钮');
// 5) 历史回填 + 删除
const histRows=()=>[...w.document.querySelectorAll('#payloadHistory .listrow')];
histRows()[0].click();await sleep();
assert.equal(w.document.querySelector('#payloadUrl').value,'https://clip.example.com/p.txt','history refill');
histRows()[0].querySelector('[data-del]').click();
await wait(()=>!w.document.querySelector('#payloadHistory .listrow'),'history emptied');
assert.equal(evaluate("(pref.payloadHistory||[]).length"),0,'history removed from pref');
pass('历史地址回填与删除');
// —— 活动记录 ——
await evaluate("act(logPage)");
await wait(()=>w.document.querySelector('#logExport'),'log page');
const logText=w.document.body.textContent;
assert(logText.includes('今天')&&logText.includes('7 天内'),'time grouping');
assert(logText.includes('已创建 /x.txt'),'log entry');
pass('活动记录分组与条目');
// 6) 导出
w.document.querySelector('#logExport').click();
await wait(()=>w.window.__calls.some(c=>c.action==='logExport'&&c.args.text&&c.args.text.includes('已创建')),'export rpc');
pass('导出记录为文本文件');
// 7) 点条目 → 删除该条
const entry=w.document.querySelector('[data-log]');
entry.click();await sleep();
const del=[...w.document.querySelectorAll('#modalLayer .actions button, #modalLayer button')].find(b=>b.textContent==='删除该条');
assert(del,'entry menu shows delete');
del.click();
await wait(()=>w.window.__calls.some(c=>c.action==='logRemove'),'logRemove rpc');
pass('单条删除 logRemove');
if(errors.length)throw Error('page errors: '+errors.join(' | '));
console.log('ALL '+count+' PASS');
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
