const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.57',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'app.apk',path:'${root}/Download/app.apk',dir:false,size:1,time:1}];data.total=1;data.free=1;break;
   case'dexFiles':data=['classes.dex'];break;
   case'dexDecrypt':data={candidates:2,callSites:5,decrypted:3,skipped:1,samples:['xx -> yy'],errors:[]};break;
   case'batchTransfer':{const nm=a.files[0].name;if(window.__btFail>0){window.__btFail--;window.receive({id,ok:false,error:'目标已存在：'+nm});return;}data=[a.destination+'/'+nm];break;}
   case'dexDirectRepair':data={path:a.path,message:'已用 dexlib2 重新生成并校验',count:1,files:[{name:'classes.dex',classes:3}],backup:'classes.dex.bak',signed:false};break;
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
const TIP='请先解压文件到本地目录后再进行该操作';
const tipShown=()=>{const d=w.document.querySelector('.dialog');return d&&d.textContent.includes(TIP);};
const closeTip=async()=>{const b=dlgBtn('取消')||dlgBtn('确定')||dlgBtn('关闭');if(b)b.click();await sleep();};
const calls=a=>w.window.__calls.filter(c=>c.action===a);

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
const archiveDex={path:root+'/Download/app.apk',name:'classes.dex',archive:root+'/Download/app.apk',entry:'classes.dex',dir:false};
// 1) APK 内 DEX：菜单含 DEX 字符串解密（与 DEX 转 JAR 同级）
await evaluate("dexFileOptions("+JSON.stringify(archiveDex)+")");
await wait(()=>w.document.querySelector('.optionsDialog'),'dex menu');
const labels=[...w.document.querySelectorAll('.optionLabel')].map(x=>x.textContent);
assert(labels.includes('DEX 字符串解密'),'menu has decrypt item');
assert(labels.indexOf('DEX 字符串解密')===labels.indexOf('DEX 转 JAR')+1,'decrypt right after to-jar');
pass('APK 内 DEX 菜单包含「DEX 字符串解密」，与 DEX 转 JAR 同级');

// 2) APK 内 DEX 字符串解密：提示 + 解压按钮，不显示解压到哪里，不发起请求
w.document.querySelectorAll('.option').forEach(b=>{if(b.textContent.includes('DEX 字符串解密'))b.click();});
await wait(tipShown,'decrypt tip');
assert(!w.window.__calls.some(c=>c.action==='dexDecrypt'),'no dexDecrypt rpc');
assert(!w.document.querySelector('#dexCustom'),'decrypt dialog not opened');
pass('APK 内 DEX 字符串解密提示「请先解压文件到本地目录后再进行该操作」');

// 3) 提示框按钮为 取消 / 解压，且不显示目标文件夹选择
const tipBtns=[...w.document.querySelectorAll('.dialog .actions button')].map(b=>b.textContent);
assert.deepEqual(tipBtns,['取消','解压'],'tip buttons');
assert(!w.document.querySelector('#extractDestination'),'no destination picker shown');
assert(!w.document.querySelector('#promptValue'),'no path input shown');
pass('提示框只有「取消 / 解压」两个按钮，不显示解压到哪里');

// 4) 点「取消」不发起任何解压请求
dlgBtn('取消').click();await sleep();
assert(!w.window.__calls.some(c=>c.action==='batchTransfer'),'no batchTransfer after cancel');
assert(!w.document.querySelector('.dialog'),'tip closed');
pass('点「取消」直接关闭提示，不发起解压请求');

// 5) APK 内修复 DEX：提示，不发起请求
await evaluate("dexFileOptions("+JSON.stringify(archiveDex)+")");
await wait(()=>w.document.querySelector('.optionsDialog'),'dex menu 2');
w.document.querySelectorAll('.option').forEach(b=>{if(b.textContent.includes('修复 DEX'))b.click();});
await wait(tipShown,'repair tip');
assert(!w.window.__calls.some(c=>c.action==='dexDirectRepair'),'no dexDirectRepair rpc');
pass('APK 内修复 DEX 提示「请先解压文件到本地目录后再进行该操作」');
await closeTip();

// 6) APK 内 DEX 转 JAR / 转 Smali：提示，不发起请求
await evaluate("act(()=>dexToJar("+JSON.stringify(archiveDex)+"))");
await wait(tipShown,'toJar tip');
assert(!w.window.__calls.some(c=>c.action==='dexDirectToJar'),'no dexDirectToJar rpc');
await closeTip();
await evaluate("act(()=>dexToSmali("+JSON.stringify(archiveDex)+"))");
await wait(tipShown,'toSmali tip');
assert(!w.window.__calls.some(c=>c.action==='dexDirectToSmali'),'no dexDirectToSmali rpc');
pass('APK 内 DEX 转 JAR / 转 Smali 提示「请先解压文件到本地目录后再进行该操作」');
await closeTip();

// 7) 修复 DEX → 解压：默认解压到压缩包同级文件夹，自动继续修复
await evaluate("dexFileOptions("+JSON.stringify(archiveDex)+")");
await wait(()=>w.document.querySelector('.optionsDialog'),'dex menu 3');
w.document.querySelectorAll('.option').forEach(b=>{if(b.textContent.includes('修复 DEX'))b.click();});
await wait(tipShown,'repair tip 2');
dlgBtn('解压').click();
await wait(()=>calls('batchTransfer').length>0,'extract rpc');
const bt=calls('batchTransfer')[0];
assert.equal(bt.args.destination,root+'/Download','extract to same-level folder');
assert.equal(bt.args.files[0].name,'classes.dex','entry basename');
assert.equal(bt.args.files[0].archive,root+'/Download/app.apk','archive arg');
assert.equal(bt.args.files[0].entry,'classes.dex','entry arg');
assert.equal(bt.args.move,false,'copy not move');
await wait(()=>{const t=w.document.querySelector('#toast');return t&&t.textContent.includes('已解压到 '+root+'/Download/classes.dex');},'extract toast');
await wait(()=>dlgBtn('修复'),'repair confirm auto-continued');
dlgBtn('修复').click();
await wait(()=>calls('dexDirectRepair').length>0,'repair rpc');
const rep=calls('dexDirectRepair')[0];
assert.equal(rep.args.path,root+'/Download/classes.dex','repair runs on extracted local file');
assert.equal(rep.args.archive,undefined,'no archive arg after extraction');
await wait(()=>{const d=w.document.querySelector('.dialog');return d&&d.textContent.includes('DEX 已修复');},'repair done');
pass('修复 DEX → 解压：默认解压到同级文件夹并自动继续修复');
let closeBtn=dlgBtn('确定')||dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

// 8) 同名冲突自动改名：classes.dex 占用时解压为 classes(1).dex，转 Smali 自动继续
w.eval('window.__btFail=1');
const btBefore=calls('batchTransfer').length;
await evaluate("act(()=>dexToSmali("+JSON.stringify(archiveDex)+"))");
await wait(tipShown,'toSmali tip 2');
dlgBtn('解压').click();
await wait(()=>calls('batchTransfer').length>=btBefore+2,'renamed retry');
const bts=calls('batchTransfer');
assert.equal(bts[bts.length-2].args.files[0].name,'classes.dex','first attempt base name');
assert.equal(bts[bts.length-1].args.files[0].name,'classes(1).dex','second attempt renamed');
assert.equal(bts[bts.length-1].args.destination,root+'/Download','same destination');
await wait(()=>w.document.querySelector('#promptValue'),'smali ask auto-continued');
assert.equal(w.document.querySelector('#promptValue').value,root+'/Download','output prefill same-level');
dlgBtn('确定').click();
await wait(()=>calls('dexDirectToSmali').length>0,'smali rpc');
const sm=calls('dexDirectToSmali')[0];
assert.equal(sm.args.path,root+'/Download/classes(1).dex','smali runs on renamed file');
assert.equal(sm.args.archive,undefined,'no archive arg after extraction');
pass('同名冲突自动改名为 classes(1).dex，转 Smali 自动继续');
closeBtn=dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

// 9) 本地 DEX 转 JAR：默认输出 = DEX 同级文件夹，仍直接处理
const localDex={path:root+'/Download/classes.dex',name:'classes.dex',dir:false};
await evaluate("act(()=>dexToJar("+JSON.stringify(localDex)+"))");
await wait(()=>w.document.querySelector('#promptValue'),'local toJar ask');
assert.equal(w.document.querySelector('#promptValue').value,root+'/Download','local default same-level folder');
pass('本地 DEX 转 JAR 默认输出为同级文件夹');
dlgBtn('确定').click();
await wait(()=>w.window.__calls.some(c=>c.action==='dexDirectToJar'),'local toJar rpc');
const jar=w.window.__calls.find(c=>c.action==='dexDirectToJar');
assert.equal(jar.args.destination,root+'/Download','jar dest');
assert.equal(jar.args.path,root+'/Download/classes.dex','jar path arg');
assert.equal(jar.args.archive,undefined,'no archive arg for local dex');
closeBtn=dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

// 10) 不可写目录回退下载目录（本地 DEX 位于 /data/app）
const sysDex={path:'/data/app/classes.dex',name:'classes.dex',dir:false};
await evaluate("act(()=>dexToJar("+JSON.stringify(sysDex)+"))");
await wait(()=>w.document.querySelector('#promptValue'),'toJar sys ask');
assert.equal(w.document.querySelector('#promptValue').value,root+'/下载','fallback to download');
pass('系统目录下默认输出回退到下载文件夹');
dlgBtn('取消').click();await sleep();

// 11) APK 功能菜单原有解密入口仍走 path
await evaluate("apkDexDecryptDialog({path:'"+root+"/Download/app.apk'})");
await wait(()=>w.document.querySelector('#dexCustom'),'apk decrypt dialog');
dlgBtn('解密').click();
await wait(()=>w.window.__calls.some(c=>c.action==='dexDecrypt'),'apk decrypt rpc');
const dec=w.window.__calls.filter(c=>c.action==='dexDecrypt').pop();
assert.equal(dec.args.path,root+'/Download/app.apk','path arg preserved');
assert.equal(dec.args.archive,undefined,'no archive for direct path');
pass('APK 功能菜单的 DEX 字符串解密入口保持不变');
closeBtn=dlgBtn('确定')||dlgBtn('关闭');if(closeBtn)closeBtn.click();await sleep();

console.log('TOTAL',count,'PASS');process.exit(errors.length?1:0);
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
