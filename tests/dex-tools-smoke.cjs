const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.49',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'app.apk',path:'${root}/Download/app.apk',dir:false,size:1,time:1}];data.total=1;data.free=1;break;
   case'dexFiles':data=['classes.dex'];break;
   case'dexDecrypt':data={candidates:2,callSites:5,decrypted:3,skipped:1,samples:['xx -> yy'],errors:[]};break;
   case'dexDirectToSmali':data={path:a.destination+'/smali',smali:12};break;
   case'dexDirectToJar':data={path:a.destination+'/out.jar',classes:7};break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,act,dexFileOptions,dexToSmali,dexToJar,apkDexDecryptDialog,dexDecryptDialog};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=async s=>{w.eval(s.replace(/\b(state|act|dexFileOptions|dexToSmali|dexToJar|apkDexDecryptDialog|dexDecryptDialog)\b/g,'window.__test.$1'));await sleep();};
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|')+' DLG:'+(w.document.querySelector('.dialog')||{innerHTML:''}).innerHTML.slice(0,200));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};
const dlgBtn=t=>{let hit=null;w.document.querySelectorAll('.dialog button').forEach(b=>{if(b.textContent===t)hit=b;});return hit;};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
// 1) APK 内 DEX：菜单含 DEX 字符串解密（与 DEX 转 JAR 同级）
const archiveDex={path:root+'/Download/app.apk',name:'classes.dex',archive:root+'/Download/app.apk',entry:'classes.dex',dir:false};
await evaluate("dexFileOptions("+JSON.stringify(archiveDex)+")");
await wait(()=>w.document.querySelector('.optionsDialog'),'dex menu');
const labels=[...w.document.querySelectorAll('.optionLabel')].map(x=>x.textContent);
assert(labels.includes('DEX 字符串解密'),'menu has decrypt item');
assert(labels.indexOf('DEX 字符串解密')===labels.indexOf('DEX 转 JAR')+1,'decrypt right after to-jar');
w.document.querySelectorAll('.option').forEach(b=>{if(b.textContent.includes('DEX 字符串解密'))b.click();});
await sleep();
await wait(()=>w.document.querySelector('#dexCustom'),'decrypt dialog');
pass('APK 内 DEX 菜单新增「DEX 字符串解密」，与 DEX 转 JAR 同级');

// 2) 解密走 archive/entry 参数（无需先解压）
dlgBtn('解密').click();
await wait(()=>w.window.__calls.some(c=>c.action==='dexDecrypt'),'dexDecrypt rpc');
const dec=w.window.__calls.find(c=>c.action==='dexDecrypt');
assert.equal(dec.args.archive,root+'/Download/app.apk','archive arg');
assert.equal(dec.args.entry,'classes.dex','entry arg');
await wait(()=>[...w.document.querySelectorAll('.dialog h2,.alertBox h2, .dialog p')].length&&!w.document.querySelector('#dexCustom'),'report shown');
pass('解密请求带 archive/entry（先解临时副本再扫描，不再要求先解压）');
// 关闭报告弹窗
let okBtn=dlgBtn('确定')||dlgBtn('关闭');if(okBtn)okBtn.click();await sleep();

// 3) APK 内 DEX 转 JAR：默认输出 = APK 同级文件夹
await evaluate("act(()=>dexToJar("+JSON.stringify(archiveDex)+"))");
await wait(()=>w.document.querySelector('#promptValue'),'toJar ask');
assert.equal(w.document.querySelector('#promptValue').value,root+'/Download','default same-level folder');
pass('DEX 转 JAR 默认输出为 APK 同级文件夹');
dlgBtn('确定').click();
await wait(()=>w.window.__calls.some(c=>c.action==='dexDirectToJar'),'toJar rpc');
const jar=w.window.__calls.find(c=>c.action==='dexDirectToJar');
assert.equal(jar.args.destination,root+'/Download','jar dest');
assert.equal(jar.args.archive,root+'/Download/app.apk','jar archive arg');
let closeBtn=dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

// 4) 本地 APK 内 DEX 转 Smali：默认同级文件夹
await evaluate("act(()=>dexToSmali("+JSON.stringify(archiveDex)+"))");
await wait(()=>w.document.querySelector('#promptValue'),'toSmali ask');
assert.equal(w.document.querySelector('#promptValue').value,root+'/Download','smali default');
pass('DEX 转 Smali 默认输出为同级文件夹');
dlgBtn('确定').click();
await wait(()=>w.window.__calls.some(c=>c.action==='dexDirectToSmali'),'toSmali rpc');
closeBtn=dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

// 5) 不可写目录回退下载目录（如 /data/app 下的 APK）
const sysDex={path:'/data/app/app.apk',name:'classes.dex',archive:'/data/app/app.apk',entry:'classes.dex',dir:false};
await evaluate("act(()=>dexToJar("+JSON.stringify(sysDex)+"))");
await wait(()=>w.document.querySelector('#promptValue'),'toJar sys ask');
assert.equal(w.document.querySelector('#promptValue').value,root+'/下载','fallback to download');
pass('系统目录下默认输出回退到下载文件夹');
dlgBtn('取消').click();await sleep();

// 6) APK 功能菜单原有解密入口仍走 path
await evaluate("apkDexDecryptDialog({path:'"+root+"/Download/app.apk'})");
await wait(()=>w.document.querySelector('#dexCustom'),'apk decrypt dialog');
dlgBtn('解密').click();
await wait(()=>w.window.__calls.filter(c=>c.action==='dexDecrypt').length>=2,'second decrypt');
const dec2=w.window.__calls.filter(c=>c.action==='dexDecrypt').pop();
assert.equal(dec2.args.path,root+'/Download/app.apk','path arg preserved');
assert.equal(dec2.args.archive,undefined,'no archive for direct path');
pass('APK 功能菜单的 DEX 字符串解密入口保持不变');

console.log('TOTAL',count,'PASS');process.exit(errors.length?1:0);
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
