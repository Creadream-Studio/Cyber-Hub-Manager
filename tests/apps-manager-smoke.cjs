const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.48',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'a.txt',path:'${root}/a.txt',dir:false,size:1,time:1}];data.total=1;data.free=1;break;
   case'apps':data=[
    {name:'抖音',package:'com.ss.android',path:'/data/app/douyin.apk',version:'41.0.0',size:364202686,system:false},
    {name:'哔哩哔哩',package:'tv.bili',path:'/data/app/bili.apk',version:'9.18.0',size:242115285,system:false},
    {name:'设置',package:'com.android.settings',path:'/system/app/settings.apk',version:'14',size:10000,system:true}
   ];break;
   case'appIcon':data='iVBORw0KGgo=';break;
   case'appUninstall':case'batchTransfer':data='${root}/下载';break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,act,appsPage,pref};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=s=>w.eval(s.replace(/\b(state|act|appsPage|pref)\b/g,'window.__test.$1'));
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
// 打开已安装应用页
await evaluate("act(appsPage)");
await wait(()=>w.document.querySelectorAll('#appsRows .appsrow').length===2,'apps rows (user tab)');
pass('用户应用标签默认只列用户应用（2 项，系统应用被过滤）');

// 1) 轻点选中：状态显示 已选：N，行高亮
const row1=w.document.querySelector('#appsRows [data-app-pkg="com.ss.android"]');
row1.click();await sleep();
assert.equal(w.document.querySelector('#appsStatus').textContent,'已选：1','status count');
assert(w.document.querySelector('#appsRows [data-app-pkg="com.ss.android"]').className.includes('appsSel'),'row highlighted');
pass('轻点应用选中并显示「已选：1」');

// 2) 应用图标懒加载（appIcon rpc + data:image 替换）
await wait(()=>{const h=w.document.querySelector('#appsRows [data-app-pkg="com.ss.android"] .appicon');return h&&h.querySelector('img');},'app icon img');
const iconSrc=w.document.querySelector('#appsRows [data-app-pkg="com.ss.android"] .appicon img').src;
assert(iconSrc.startsWith('data:image/png;base64,'),'icon data url');
assert(w.window.__calls.some(c=>c.action==='appIcon'&&c.args.package==='com.ss.android'),'appIcon rpc');
pass('应用图标经 appIcon 接口懒加载显示');

// 3) 反选
w.document.querySelector('.fabInvert').click();await sleep();
assert.equal(w.document.querySelector('#appsStatus').textContent,'已选：1','invert count');
assert(w.document.querySelector('#appsRows [data-app-pkg="tv.bili"]').className.includes('appsSel'),'bili selected');
assert(!w.document.querySelector('#appsRows [data-app-pkg="com.ss.android"]').className.includes('appsSel'),'douyin deselected');
pass('反选按钮互换选中状态');

// 4) 提取 APK：batchTransfer 收到 应用名-版本.apk 与目标目录
w.document.querySelector('.fabExtract').click();await sleep();
assert(w.document.querySelector('#appsExtractDest'),'extract dialog');
w.document.querySelector('#appsExtractDest').value='/storage/emulated/0/Download';
w.document.querySelectorAll('.dialog button').forEach(b=>{if(b.textContent==='确定')b.click();});
await wait(()=>w.window.__calls.some(c=>c.action==='batchTransfer'),'batchTransfer');
const bt=w.window.__calls.find(c=>c.action==='batchTransfer');
assert.equal(bt.args.destination,'/storage/emulated/0/Download','dest');
assert.equal(bt.args.files[0].name,'哔哩哔哩-9.18.0.apk','apk name');
assert.equal(bt.args.move,false,'copy not move');
pass('提取 APK 批量复制到目标目录（应用名-版本.apk）');

// 5) 卸载：逐个 appUninstall
w.document.querySelector('.fabUninstall').click();await sleep();
w.document.querySelectorAll('.dialog button').forEach(b=>{if(b.textContent==='逐个卸载'||b.textContent==='卸载')b.click();});
await wait(()=>w.window.__calls.some(c=>c.action==='appUninstall'),'appUninstall');
const au=w.window.__calls.filter(c=>c.action==='appUninstall');
assert.equal(au.length,1,'one uninstall request at a time');
assert.equal(au[0].args.package,'tv.bili','uninstall pkg');
pass('卸载按钮逐个发出 appUninstall 请求');

// 6) 放弃选择
w.document.querySelector('.fabCancel').click();await sleep();
assert.equal(w.document.querySelector('#appsStatus').textContent,'','status cleared');
assert(!w.document.querySelector('.appsSel'),'no highlighted rows');
pass('放弃按钮清空全部选择');

// 7) 系统应用标签
w.document.querySelector('[data-app-tab="system"]').click();await sleep();
assert.equal(w.document.querySelectorAll('#appsRows .appsrow').length,1,'system tab rows');
assert(w.document.querySelector('#appsRows [data-app-pkg="com.android.settings"]'),'settings app listed');
pass('系统应用标签切换过滤生效');

console.log('TOTAL',count,'PASS');process.exit(errors.length?1:0);
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
