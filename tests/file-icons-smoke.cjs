const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.59',ownApk:'${root}/self.apk'};break;
   case'list':data=[
    {name:'相册',path:'${root}/相册',dir:true,size:0,time:1},
    {name:'classes.dex',path:'${root}/classes.dex',dir:false,size:1,time:1},
    {name:'classes.dex.bak',path:'${root}/classes.dex.bak',dir:false,size:1,time:1},
    {name:'data.zip',path:'${root}/data.zip',dir:false,size:1,time:1},
    {name:'docs.rar',path:'${root}/docs.rar',dir:false,size:1,time:1},
    {name:'bundle.7z',path:'${root}/bundle.7z',dir:false,size:1,time:1},
    {name:'backup.tar.gz',path:'${root}/backup.tar.gz',dir:false,size:1,time:1},
    {name:'layout.xml',path:'${root}/layout.xml',dir:false,size:1,time:1},
    {name:'call_rec.arec',path:'${root}/call_rec.arec',dir:false,size:1,time:1},
    {name:'libtest.so',path:'${root}/libtest.so',dir:false,size:1,time:1},
    {name:'image.bin',path:'${root}/image.bin',dir:false,size:1,time:1},
    {name:'firmware.bin',path:'${root}/firmware.bin',dir:false,size:1,time:1},
    {name:'harmony_app.hap',path:'${root}/harmony_app.hap',dir:false,size:1,time:1},
    {name:'update_pack.iap',path:'${root}/update_pack.iap',dir:false,size:1,time:1},
    {name:'iOS安装包.ipa',path:'${root}/iOS安装包.ipa',dir:false,size:1,time:1},
    {name:'photo.jpg',path:'${root}/photo.jpg',dir:false,size:1,time:1},
    {name:'music.mp3',path:'${root}/music.mp3',dir:false,size:1,time:1},
    {name:'clip.mp4',path:'${root}/clip.mp4',dir:false,size:1,time:1},
    {name:'note.txt',path:'${root}/note.txt',dir:false,size:1,time:1},
    {name:'unknown.dat',path:'${root}/unknown.dat',dir:false,size:1,time:1},
    {name:'part.001',path:'${root}/part.001',dir:false,size:1,time:1},
    {name:'app-release.apk',path:'${root}/app-release.apk',dir:false,size:1,time:1},
    {name:'splits.apks',path:'${root}/splits.apks',dir:false,size:1,time:1}
   ];data.total=1;data.free=1;break;
   case'apkIcon':data='data:image/png;base64,iVBORw0KGgo=';break;
   case'listThumb':data='';break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={fileType,icon,act,openWithBuiltinItems,go:p=>navigate(0,{path:p,archive:"",entry:""})};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=async s=>{w.eval(s.replace(/\b(fileType|icon|act|openWithBuiltinItems|go)\b/g,'window.__test.$1'));await sleep();};
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};
const calls=a=>w.window.__calls.filter(c=>c.action===a);
const row=name=>{let hit=null;w.document.querySelectorAll('.pane.active .file').forEach(r=>{const n=r.querySelector('.filename');if(n&&n.textContent===name)hit=r;});return hit;};
const holder=name=>{const r=row(name);return r?r.querySelector('.fileicon'):null;};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');

// 1) fileType 映射：指定后缀 + 大类兜底
const typeOf=n=>w.eval(`window.__test.fileType({name:${JSON.stringify(n)},dir:false})`);
assert.equal(typeOf('classes.dex'),'dex');
assert.equal(typeOf('classes.dex.bak'),'bak');
assert.equal(typeOf('data.zip'),'archive');
assert.equal(typeOf('docs.rar'),'archive');
assert.equal(typeOf('bundle.7z'),'archive');
assert.equal(typeOf('backup.tar.gz'),'archive');
assert.equal(typeOf('pack.tar'),'archive');
assert.equal(typeOf('layout.xml'),'xml');
assert.equal(typeOf('call_rec.arec'),'arec');
assert.equal(typeOf('libtest.so'),'so');
assert.equal(typeOf('image.bin'),'bin');
assert.equal(typeOf('firmware.bin'),'bin');
assert.equal(typeOf('libtest.so.bak'),'bak');
assert.equal(typeOf('harmony_app.hap'),'hap');
assert.equal(typeOf('update_pack.iap'),'iap');
assert.equal(typeOf('iOS安装包.ipa'),'ipa');
assert.equal(typeOf('app.hap.bak'),'bak');
assert.equal(typeOf('photo.jpg'),'image');
assert.equal(typeOf('music.mp3'),'audio');
assert.equal(typeOf('clip.mp4'),'video');
assert.equal(typeOf('note.txt'),'text');
assert.equal(typeOf('unknown.dat'),'file');
assert.equal(typeOf('part.001'),'file');
assert.equal(typeOf('app-release.apk'),'apk');
assert.equal(typeOf('splits.apks'),'apks');
assert.equal(w.eval('window.__test.fileType({name:"x",dir:true})'),'folder');
pass('fileType 映射：dex/bak/压缩包/xml/arec/so/bin/hap/iap/ipa 与图片、音频、视频、文本、未知、分卷兜底正确');

// 2) 富图标 SVG：特征颜色与徽标文字
const iconOf=n=>w.eval(`window.__test.icon(${JSON.stringify(n)})`);
const dexSvg=iconOf('dex');
assert(dexSvg.includes('#3ddc84')&&dexSvg.includes('>DEX<'),'dex green badge with DEX text');
assert(iconOf('bak').includes('>BAK<'),'bak badge text');
const zipSvg=iconOf('zip'),archiveSvg=iconOf('archive');
assert(archiveSvg.includes('#8a6538'),'archive carton brown');
assert.equal(zipSvg,archiveSvg,'zip alias renders archive carton');
assert(iconOf('xml').includes('#f0842e'),'xml orange brackets');
const arecSvg=iconOf('arec');
assert(arecSvg.includes('#e5484d')&&(arecSvg.match(/<rect /g)||[]).length>=5,'arec red dot + waveform bars');
assert(iconOf('apks').includes('#3ddc84'),'apks android robot green');
assert(iconOf('image').includes('#4fc3d9'),'image cyan');
assert(iconOf('audio').includes('#a07ad8'),'audio purple');
assert(iconOf('video').includes('#4e7fd0'),'video blue');
assert(iconOf('text').includes('#aeb6bf'),'text paper');
assert(iconOf('folder').includes('fill="currentColor"'),'folder keeps currentColor icon');
const soSvg=iconOf('so');
assert(soSvg.includes('#7e57c2')&&soSvg.includes('>SO<')&&soSvg.includes('<circle'),'so purple badge with gear and SO text');
const binSvg=iconOf('bin');
assert(binSvg.includes('#37474f')&&binSvg.includes('#7ee787')&&binSvg.includes('>0110<'),'bin dark slate with green binary digits');
const hapSvg=iconOf('hap');
assert(hapSvg.includes('#0a5fc4')&&hapSvg.includes('>HAP<')&&hapSvg.includes('#5fd0ff'),'hap harmony blue badge with cyan orbit and HAP text');
const iapSvg=iconOf('iap');
assert(iapSvg.includes('#00838f')&&iapSvg.includes('>IAP<')&&iapSvg.includes('M12 6.4v3'),'iap teal badge with install arrow and IAP text');
const ipaSvg=iconOf('ipa');
assert(ipaSvg.includes('#d4dae1')&&ipaSvg.includes('>IPA<')&&ipaSvg.includes('#2c3945'),'ipa silver badge with dark install arrow and IPA text');
pass('富图标 SVG：DEX/BAK 徽标、纸箱、橙色尖括号、红点波形、机器人、so 齿轮徽标、bin 二进制数字、hap 蓝青轨道、iap/ipa 安装箭头及媒体配色齐备，zip 别名渲染纸箱');

// 3) 列表渲染：每行图标节点与类型类名
const expectIcon=(name,cls)=>{const h=holder(name);assert(h,'icon holder '+name);assert(h.classList.contains(cls),'class '+cls+' for '+name);assert(h.querySelector('svg'),'svg in '+name);return h;};
expectIcon('classes.dex','dex');
expectIcon('classes.dex.bak','bak');
expectIcon('data.zip','archive');
expectIcon('docs.rar','archive');
expectIcon('bundle.7z','archive');
expectIcon('backup.tar.gz','archive');
expectIcon('layout.xml','xml');
expectIcon('call_rec.arec','arec');
expectIcon('libtest.so','so');
expectIcon('image.bin','bin');
expectIcon('firmware.bin','bin');
expectIcon('harmony_app.hap','hap');
expectIcon('update_pack.iap','iap');
expectIcon('iOS安装包.ipa','ipa');
expectIcon('photo.jpg','image');
expectIcon('music.mp3','audio');
expectIcon('clip.mp4','video');
expectIcon('note.txt','text');
expectIcon('unknown.dat','file');
expectIcon('part.001','file');
expectIcon('app-release.apk','apk');
const apksHolder=expectIcon('splits.apks','apks');
assert(apksHolder.classList.contains('appBadgeHost'),'apks badge host');
const folderHolder=holder('相册');
assert(folderHolder&&folderHolder.classList.contains('folder')&&folderHolder.querySelector('svg'),'folder icon node');
pass('文件列表为每种类型渲染带类型类名的图标节点（含 hap/iap/ipa 安装包），apks 为角标宿主');

// 4) APK 真实软件图标：懒加载后整格替换为图片
await wait(()=>{const h=holder('app-release.apk');return h&&h.querySelector('img')&&!h.querySelector('svg');},'apk real icon');
assert(holder('app-release.apk').querySelector('img').src.startsWith('data:image/png;base64,'),'apk icon data url');
pass('.apk 懒加载真实软件图标并整格替换');

// 5) APKS 机器人 + 右下角小图标角标：svg 保留、img 叠加
await wait(()=>{const h=holder('splits.apks');return h&&h.querySelector('img.appBadge')&&h.querySelector('svg');},'apks badge');
const bh=holder('splits.apks');
assert(bh.querySelector('svg').innerHTML.includes('#3ddc84'),'robot kept under badge');
assert(bh.querySelectorAll('img.appBadge').length===1,'single badge');
pass('.apks 显示安卓机器人并在右下角叠加小软件图标');

// 6) apkIcon RPC 参数：apk 与 apks 都请求，路径正确
const iconCalls=calls('apkIcon');
assert(iconCalls.some(c=>c.args.path===root+'/app-release.apk'),'apk rpc path');
assert(iconCalls.some(c=>c.args.path===root+'/splits.apks'),'apks rpc path');
pass('apkIcon RPC 对 .apk 与 .apks 均按文件路径调用');

// 7) 前端缓存：重复渲染不重复请求
const before=iconCalls.length;
await evaluate("act(()=>go('"+root+"'))");
await wait(()=>w.document.querySelectorAll('.pane.active .file').length>1,'rerender');
await new Promise(r=>setTimeout(r,120));
assert(calls('apkIcon').length===before,'no duplicate apkIcon rpc');
assert(holder('splits.apks').querySelectorAll('img.appBadge').length<=1,'badge not duplicated');
pass('重复渲染命中前端缓存，不重复发起 apkIcon 请求');

// 8) 打开方式分类：rar/zip 走「浏览压缩包」，APKS 用机器人图标
await evaluate("act(()=>go('"+root+"'))");
await wait(()=>w.document.querySelectorAll('.pane.active .file').length>1,'files 2');
const owFor=n=>JSON.parse(w.eval(`JSON.stringify(window.__test.openWithBuiltinItems({name:${JSON.stringify(n)},path:'${root}/x',dir:false}).map(i=>({label:i.label,glyph:i.glyph,ok:i.test()})))`));
const owRar=owFor('docs.rar').find(i=>i.label==='浏览压缩包');
assert(owRar&&owRar.ok===true,'rar browsable as archive');
const ow001=owFor('part.001').find(i=>i.label==='浏览压缩包');
assert(ow001&&ow001.ok===true,'001 keeps archive entry');
const owApks=owFor('splits.apks').find(i=>i.label==='APKS 功能');
assert(owApks&&owApks.ok===true&&String(owApks.glyph).includes('#3ddc84'),'apks category uses robot glyph');
const owTxt=owFor('note.txt').find(i=>i.label==='浏览压缩包');
assert(!owTxt||owTxt.ok===false,'txt not archive');
pass('打开方式：rar/zip 进入「浏览压缩包」分类，APKS 功能使用机器人图标');

console.log('TOTAL',count,'PASS');process.exit(errors.length?1:0);
})().catch(e=>{console.error('FAIL',e.message);process.exit(1)});
