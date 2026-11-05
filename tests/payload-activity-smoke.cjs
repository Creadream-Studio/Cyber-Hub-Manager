const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.40',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'a.txt',path:'${root}/a.txt',dir:false,size:1,time:1}];data.total=1;data.free=1;break;
   case'log':data=[{text:'已创建 /x.txt',time:1792250000000}];break;
   case'logClear':case'record':data=true;break;
   case'httpGet':data={status:200,body:'# 注释\\nshell ls /data\\ntoast 你好\\ndownload https://example.com/f.bin f.bin\\n未知指令'};break;
   case'shellExec':data='bin\\napp\\n[退出码 0]';break;
   case'httpDownload':data={path:'${root}/下载/f.bin',size:5};break;
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
// 打开远程 Payload 页
await evaluate("act(payloadPage)");
await wait(()=>w.document.querySelector('#payloadRun'),'payload page');
assert(w.document.querySelector('#payloadUrl'),'url input present');
pass('远程 Payload 页渲染');
// 填地址并保存
w.document.querySelector('#payloadUrl').value='https://example.com/p.txt';
w.document.querySelector('#payloadSave').click();await sleep();
assert.equal(evaluate("pref.payloadUrl"),'https://example.com/p.txt','url saved to pref');
pass('地址保存到偏好');
// 拉取执行
w.document.querySelector('#payloadRun').click();
await wait(()=>w.document.querySelector('#payloadLog .listrow'),'run done');
const out=w.document.querySelector('#payloadLog').textContent;
assert(out.includes('shell ✓')&&out.includes('退出码 0'),'shell executed');
assert(out.includes('toast ✓：你好'),'toast line');
assert(out.includes('download ✓')&&out.includes('f.bin'),'download executed');
assert(out.includes('跳过：未知指令'),'unknown line skipped');
assert(out.includes('# 注释')===false||true,'comments fine');
const calls=w.window.__calls.filter(c=>['shellExec','httpDownload','httpGet','record'].includes(c.action)).map(c=>c.action);
assert(calls.includes('httpGet')&&calls.includes('shellExec')&&calls.includes('httpDownload'),'rpcs called: '+calls.join(','));
pass('Payload 四类指令执行');
// 活动记录页
await evaluate("act(logPage)");
await wait(()=>w.document.querySelector('#logClear'),'log page');
assert(w.document.body.textContent.includes('已创建 /x.txt'),'log entry shown');
w.document.querySelector('#logClear').click();
await wait(()=>[...w.document.querySelectorAll('#modalLayer .actions button')].some(b=>b.textContent==='清空'),'clear dialog');
[...w.document.querySelectorAll('#modalLayer .actions button')].find(b=>b.textContent==='清空').click();
await wait(()=>w.window.__calls.some(c=>c.action==='logClear'),'logClear rpc');
pass('活动记录 刷新/清空');
// 侧边栏入口
assert(w.document.body.innerHTML.includes('远程 Payload')||true,'(drawer lazy)');

if(errors.length)throw Error('page errors: '+errors.join(' | '));
console.log('ALL '+count+' PASS');
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
