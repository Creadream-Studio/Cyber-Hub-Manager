const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
const dataDir=root+'/Android/data';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.50',ownApk:'${root}/self.apk'};break;
   case'list':if(a.path==='${dataDir}'){data=[
     {name:'com.ss.android',path:'${dataDir}/com.ss.android',dir:true,size:0,time:1},
     {name:'com.android.settings',path:'${dataDir}/com.android.settings',dir:true,size:0,time:1},
     {name:'readme.txt',path:'${dataDir}/readme.txt',dir:false,size:3,time:1}
    ];}else{data=[{name:'a.txt',path:'${root}/a.txt',dir:false,size:1,time:1}];}
    data.total=1;data.free=1;break;
   case'appIcon':data='iVBORw0KGgo=';break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={act,isAppDirPath,go:p=>navigate(0,{path:p,archive:"",entry:""})};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,60));const evaluate=s=>w.eval(s.replace(/\b(act|isAppDirPath|go)\b/g,'window.__test.$1'));
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
assert(!w.window.__test.isAppDirPath('/storage/emulated/0'),'root not app dir');
assert(!w.window.__test.isAppDirPath('/storage/emulated/0/Android/data/com.x'),'subfolder not app dir');
assert(w.window.__test.isAppDirPath('/storage/emulated/0/Android/data/'),'data with trailing slash');
assert(w.window.__test.isAppDirPath('/storage/emulated/0/Android/media')&&w.window.__test.isAppDirPath('/storage/emulated/0/Android/obb'),'media+obb match');
pass('isAppDirPath 仅匹配 Android/data|media|obb 根目录');

// 进入 Android/data：包名文件夹应带角标宿主
await evaluate("act(()=>go('"+dataDir+"'))");
await wait(()=>w.document.querySelectorAll('[data-badge-pkg]').length===2,'badge rows');
assert(w.document.querySelectorAll('.fileicon.appBadgeHost').length===2,'badge host class');
assert(w.document.querySelector('[data-badge-pkg="com.ss.android"]'),'pkg row');
assert(!w.document.querySelector('[data-badge-pkg="readme.txt"]'),'file row no badge');
pass('Android/data 下文件夹标记 data-badge-pkg，文件不标记');

// 应用图标角标经 appIcon 懒加载叠加
await wait(()=>{const h=w.document.querySelector('[data-badge-pkg="com.ss.android"] .fileicon');return h&&h.querySelector('img.appBadge');},'badge img');
assert(w.document.querySelector('[data-badge-pkg="com.ss.android"] .fileicon .appBadge').src.startsWith('data:image/png;base64,'),'badge data url');
assert(w.window.__calls.some(c=>c.action==='appIcon'&&c.args.package==='com.ss.android'),'appIcon rpc');
pass('文件夹图标右下角叠加应用图标（appIcon 接口）');

// 返回普通目录：无角标
await evaluate("act(()=>go('"+root+"'))");
await wait(()=>w.document.querySelectorAll('[data-badge-pkg]').length===0,'no badge rows');
assert(w.document.querySelectorAll('.fileicon.appBadgeHost').length===0,'no host class');
pass('普通目录不显示应用图标角标');

console.log('DONE '+count+' PASS');process.exit(errors.length?1:0);
})().catch(e=>{console.error('FAIL',e);process.exit(1)});
