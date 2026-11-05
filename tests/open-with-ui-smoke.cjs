const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
// 与 ui-smoke 同形的原生桥 mock，补齐 openWithApps / openWithLaunch / hexView / terminalOpen。
w.eval(`(()=>{
 const root='/storage/emulated/0',now=1792250000000;
 const files=[{name:'sample.apk',path:root+'/sample.apk',dir:false,size:340000,time:now},{name:'notes.txt',path:root+'/notes.txt',dir:false,size:200,time:now},{name:'photo.jpg',path:root+'/photo.jpg',dir:false,size:9000,time:now},{name:'run.sh',path:root+'/run.sh',dir:false,size:60,time:now}];
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:root,workspace:root+'/CyberHub',version:'0.1.13',ownApk:root+'/self.apk'};break;
   case'disk':data={total:237888888888,free:170000000000};break;
   case'list':data=files.map(x=>({...x,total:237888888888,free:170000000000}));break;
   case'resolve':data=a.path;break;
   case'openWithApps':data={mime:'text/plain',suggest:[{label:'查看',pkg:'com.test.viewer',cls:'com.test.viewer.ViewActivity',icon:''}],text:[{label:'编辑器 A',pkg:'com.test.editor',cls:'com.test.editor.Editor',icon:''},{label:'编辑器 B',pkg:'com.test.editorb',cls:'com.test.editorb.Editor',icon:''}],image:[{label:'相册',pkg:'com.test.gallery',cls:'com.test.gallery.Photo',icon:''}],video:[{label:'播放器',pkg:'com.test.player',cls:'com.test.player.Video',icon:''}],audio:[]};break;
   case'openWithLaunch':data=true;break;
   case'hexView':{const off=Math.max(0,a.offset||0),len=Math.min(a.length||4096,64);let hex='',ascii='';for(let i=0;i<len;i++){const v=(off+i)&0xff;hex+=v.toString(16).padStart(2,'0');ascii+=(v>=32&&v<127)?String.fromCharCode(v):'.';}data={path:a.path,name:a.path.split('/').pop(),size:128,offset:off,length:len,hex,ascii};break;}
   case'terminalOpen':data=true;break;
   case'properties':data={path:a.path,size:200,modified:now,readable:true,writable:true,sha256:'x'};break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,pref,fileActions,openWithDialog};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=s=>w.eval(s.replace(/\bstate\b/g,'window.__test.state').replace(/\bpref\b/g,'window.__test.pref').replace(/\bfileActions\b/g,'window.__test.fileActions').replace(/\bopenWithDialog\b/g,'window.__test.openWithDialog'));
async function wait(fn){for(let i=0;i<120;i++){if(fn())return;await sleep();}throw Error('Timed out waiting for DOM');}
let count=0;const pass=n=>{count++;console.log('PASS',n)};
function button(text,root=w.document){const b=[...root.querySelectorAll('button')].find(x=>x.textContent.trim()===text||x.getAttribute('aria-label')===text);if(!b)throw Error('Button not found: '+text);b.click();return b;}
function menuButton(text){const b=[...w.document.querySelectorAll('[role=menuitem]')].find(x=>x.textContent.trim()===text);if(!b)throw Error('Menu item not found: '+text);b.click();return b;}
function gridItem(label){const b=[...w.document.querySelectorAll('.owItem')].find(x=>x.getAttribute('aria-label')===label);if(!b)throw Error('Open-with item not found: '+label);return b;}
function tab(label){const b=[...w.document.querySelectorAll('.owTabs .tab')].find(x=>x.textContent.trim()===label);if(!b)throw Error('Tab not found: '+label);b.click();return b;}

// 1) 长按 notes.txt → 打开方式… → 新对话框结构
await wait(()=>w.document.querySelectorAll('.file').length>3);
evaluate("fileActions(state.panes[0].items.find(x=>x.name==='notes.txt'))");
await wait(()=>w.document.querySelector('.fileActionMenu'));
menuButton('打开方式…');
await wait(()=>w.document.querySelector('.owDialog'));
assert.equal(w.document.querySelector('.owDialog h2 .owTitle').textContent,'打开方式…');
assert(w.document.querySelector('.owHeadBtns [aria-label="搜索打开方式"]'),'search button in header');
assert(w.document.querySelector('.owHeadBtns [aria-label="打开方式说明"]'),'help button in header');
const tabs=[...w.document.querySelectorAll('.owTabs .tab')].map(x=>x.textContent.trim());
assert.deepEqual(tabs,['内置','系统建议','文本'],'text file shows builtin/suggest/text tabs only');
assert.equal([...w.document.querySelectorAll('.owTabs .tab')].filter(x=>x.classList.contains('active')).length,1);
pass('open-with dialog renders MT-style header and tabs');

// 2) 内置网格：12 项、彩色瓦片、按类型置灰
await wait(()=>w.document.querySelectorAll('.owItem').length>=12);
const labels=[...w.document.querySelectorAll('.owItem')].map(x=>x.getAttribute('aria-label'));
assert.deepEqual(labels,['编辑文本','浏览压缩包','查看图片','十六进制','播放音乐','播放视频','XML 功能','DEX 功能','ARSC 功能','APK 信息','APKS 功能','Linux 脚本']);
assert(gridItem('编辑文本').disabled===false,'text editor enabled for txt');
assert(gridItem('查看图片').disabled===true,'image viewer disabled for txt');
assert(gridItem('播放音乐').disabled===true,'music player disabled for txt');
assert(gridItem('APK 信息').disabled===true,'apk info disabled for txt');
assert(gridItem('编辑文本').querySelector('.owTile').style.background,'colourful tile background set');
assert(gridItem('十六进制').querySelector('.owGlyph'),'hex tile uses glyph');
assert(w.document.querySelector('.owDialog .actions button').textContent.trim()==='关闭','close button at bottom');
pass('builtin grid has 12 tiles with dynamic greying');

// 3) 搜索过滤当前页
w.document.querySelector('.owHeadBtns [aria-label="搜索打开方式"]').click();
await sleep();
const searchInput=w.document.querySelector('#owSearchInput');
assert(searchInput&&w.document.querySelector('#owSearch').hidden===false,'search row revealed');
searchInput.value='文本';searchInput.dispatchEvent(new w.Event('input',{bubbles:true}));
await sleep();
assert.equal(w.document.querySelectorAll('.owItem').length,1);
assert.equal(w.document.querySelectorAll('.owItem')[0].getAttribute('aria-label'),'编辑文本');
searchInput.value='';searchInput.dispatchEvent(new w.Event('input',{bubbles:true}));
await sleep();
assert.equal(w.document.querySelectorAll('.owItem').length,12);
pass('header search filters current tab');

// 4) 系统建议 / 文本 标签：外部应用网格 + 显式启动
tab('系统建议');
await wait(()=>w.__calls.some(c=>c.action==='openWithApps'));
await wait(()=>w.document.querySelectorAll('.owItem').length===1);
assert.equal(gridItem('查看').querySelector('.owLabel').textContent,'查看');
assert(w.__calls.find(c=>c.action==='openWithApps').args.path==='/storage/emulated/0/notes.txt','openWithApps receives resolved path');
gridItem('查看').click();
await wait(()=>w.__calls.some(c=>c.action==='openWithLaunch'));
const launch=w.__calls.find(c=>c.action==='openWithLaunch');
assert.equal(launch.args.pkg,'com.test.viewer');assert.equal(launch.args.cls,'com.test.viewer.ViewActivity');assert.equal(launch.args.mime,'text/plain');
await wait(()=>!w.document.querySelector('.owDialog'));
pass('suggest tab lists external apps and launches explicitly');

// 5) 文本标签列出多个应用
evaluate("openWithDialog(state.panes[0].items.find(x=>x.name==='notes.txt'))");
await wait(()=>w.document.querySelector('.owDialog'));
tab('文本');
await wait(()=>w.document.querySelectorAll('.owItem').length===2);
assert(gridItem('编辑器 A')&&gridItem('编辑器 B'));
button('关闭');
await wait(()=>!w.document.querySelector('.owDialog'));
pass('text tab lists filtered apps and close button works');

// 6) 图片文件出现媒体标签；.sh 文件的 Linux 脚本可用
evaluate("openWithDialog(state.panes[0].items.find(x=>x.name==='photo.jpg'))");
await wait(()=>w.document.querySelector('.owDialog'));
assert.deepEqual([...w.document.querySelectorAll('.owTabs .tab')].map(x=>x.textContent.trim()),['内置','系统建议','文本','图片'],'image file adds image tab');
assert(gridItem('查看图片').disabled===false,'image viewer enabled for jpg');
assert(gridItem('播放视频').disabled===true,'video player disabled for jpg');
button('关闭');
await wait(()=>!w.document.querySelector('.owDialog'));
evaluate("openWithDialog(state.panes[0].items.find(x=>x.name==='run.sh'))");
await wait(()=>w.document.querySelector('.owDialog'));
assert(gridItem('Linux 脚本').disabled===false,'script tile enabled for .sh');
gridItem('Linux 脚本').click();
await wait(()=>w.__calls.some(c=>c.action==='terminalOpen'));
const term=w.__calls.find(c=>c.action==='terminalOpen');
assert(term.args.command.indexOf('run.sh')>=0&&term.args.command.indexOf('sh ')===0,'terminal runs sh script');
await wait(()=>!w.document.querySelector('.owDialog'));
pass('media tabs adapt to file type and script tile opens terminal');

// 7) 内置「十六进制」打开十六进制查看器
evaluate("openWithDialog(state.panes[0].items.find(x=>x.name==='notes.txt'))");
await wait(()=>w.document.querySelector('.owDialog'));
gridItem('十六进制').click();
await wait(()=>w.document.querySelector('.hexBody'));
await wait(()=>w.document.querySelector('.hexBody').textContent.length>0);
const hexCall=w.__calls.filter(c=>c.action==='hexView').pop();
assert.equal(hexCall.args.length,4096);
assert.equal(hexCall.args.offset,0);
const rows=w.document.querySelector('.hexBody').textContent.split('\n');
const firstRow=rows[0];
assert(/^[0-9a-f]{8}  /.test(firstRow),'row starts with 8-digit hex address');
assert(firstRow.indexOf('|')>0,'row has ASCII column');
for(const row of rows){const ascii=row.match(/\|([^|]*)\|$/);assert(ascii&&ascii[1].length===8,'every ASCII cell is 8 chars: '+row);}
assert.equal(rows.length,8,'64 mock bytes produce 8 rows of 8 bytes');
assert(/ · 0x/.test(w.document.querySelector('#hexPos').textContent),'position label shows hex offset');
assert(w.document.querySelector('.hexNav'),'navigation buttons present');
pass('hex viewer tool page renders paged rows');

assert.deepEqual(errors,[]);pass('no JavaScript errors in DOM harness');
console.log('ALL '+count+' OPEN-WITH CHECKS PASSED (layout and Android WebView not emulated)');await w.happyDOM.close();})().catch(e=>{console.error(e);process.exit(1)});
