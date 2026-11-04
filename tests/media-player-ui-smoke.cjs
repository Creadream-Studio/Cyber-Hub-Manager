const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
// 原生桥 mock：init/list/媒体流/剪贴板等。
w.eval(`(()=>{
 const root='/storage/emulated/0',now=1792250000000;
 const files=[{name:'sample.apk',path:root+'/sample.apk',dir:false,size:340000,time:now},{name:'notes.txt',path:root+'/notes.txt',dir:false,size:200,time:now},{name:'photo.jpg',path:root+'/photo.jpg',dir:false,size:9000,time:now},{name:'movie.mkv',path:root+'/movie.mkv',dir:false,size:4200000,time:now},{name:'song.flac',path:root+'/song.flac',dir:false,size:800000,time:now},{name:'run.sh',path:root+'/run.sh',dir:false,size:60,time:now}];
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:root,workspace:root+'/CyberHub',version:'0.1.14',ownApk:root+'/self.apk'};break;
   case'list':data=files.map(x=>({...x,total:237888888888,free:170000000000}));break;
   case'mediaStream':data={url:'/media/stream-'+encodeURIComponent(a.path),mime:a.path.endsWith('.mkv')?'video/x-matroska':'audio/flac',size:1234};break;
   case'mediaImage':data={data:'data:image/jpeg;base64,/9j/4AAQSkZJRg==',width:10,height:10,mime:'image/jpeg'};break;
   case'clipboardGet':data='/storage/emulated/0/Download';break;
   case'mediaPlay':data=true;break;
   case'clipboard':data=true;break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,pref,fileActions,openFile,playMedia,mediaKind,jumpDialog,openQuickPanel,closeQuick,recordRecentFolder,renderQuickBody,closePlayer,closePhoto,renderFiles,activatePane,fitPath};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,25));const evaluate=s=>w.eval(s.replace(/\b(state|pref|fileActions|openFile|playMedia|mediaKind|jumpDialog|openQuickPanel|closeQuick|recordRecentFolder|renderQuickBody|closePlayer|closePhoto|renderFiles|activatePane|fitPath)\b/g,'window.__test.$1'));
async function wait(fn){for(let i=0;i<160;i++){if(fn())return;await sleep();}throw Error('Timed out waiting for DOM');}
let count=0;const pass=n=>{count++;console.log('PASS',n)};
function menuItems(){return [...w.document.querySelectorAll('.fileActionMenu [role=menuitem]')];}
function menuButton(text){const b=[...w.document.querySelectorAll('[role=menuitem],[role=menuitemcheckbox]')].find(x=>x.textContent.replace('•','').trim()===text);if(!b)throw Error('Menu item not found: '+text);return b;}
function dialogButton(text){const b=[...w.document.querySelectorAll('#modalLayer .actions button')].find(x=>x.textContent.trim()===text);if(!b)throw Error('Dialog button not found: '+text);return b;}

// 1) 冷门格式识别
assert.equal(evaluate("mediaKind({name:'a.rmvb'})"),'video','rmvb is video');
assert.equal(evaluate("mediaKind({name:'b.m2ts'})"),'video','m2ts is video');
assert.equal(evaluate("mediaKind({name:'c.ape'})"),'audio','ape is audio');
assert.equal(evaluate("mediaKind({name:'d.opus'})"),'audio','opus is audio');
assert.equal(evaluate("mediaKind({name:'e.tiff'})"),'image','tiff is image');
assert.equal(evaluate("mediaKind({name:'f.dng'})"),'image','dng is image');
pass('cold formats map to image/video/audio kinds');

// 2) 长按菜单：十项基础菜单、无预览/播放/外部打开项、• 提示语
await wait(()=>w.document.querySelectorAll('.file').length>5);
evaluate("fileActions(state.panes[0].items.find(x=>x.name==='photo.jpg'))");
await wait(()=>w.document.querySelector('.fileActionMenu'));
const labels=menuItems().map(x=>x.textContent.replace('•','').trim());
assert.deepEqual(labels,['→ 复制','→ 移动','删除','重命名','工具','压缩','属性','分享','打开方式…','添加书签'],'base menu order matches screenshot 2');
assert(!labels.some(x=>/预览|播放|其他应用/.test(x)),'no preview / external-open rows');
assert.equal(w.document.querySelector('.fileActionMenu .actionHint').textContent,'带 • 的菜单表示可以长按触发单窗口操作','hint matches screenshot');
const dots=menuItems().filter(x=>x.querySelector('.menuDot'));
assert.equal(dots.length,2,'only copy & move carry the dot');
assert(dots[0].textContent.includes('复制')&&dots[1].textContent.includes('移动'),'dots are on 复制/移动');
pass('long-press menu is the 10-item list with • markers');

// 3) 内置图片预览（无「用其他应用打开」按钮）
evaluate("openFile(state.panes[0].items.find(x=>x.name==='photo.jpg'))");
await wait(()=>w.document.querySelector('#mediaLayer.open .photoImage'));
assert(!w.document.querySelector('[data-photo-external]'),'external-open button removed from photo viewer');
assert(w.document.querySelector('[data-photo-info]'),'info button kept');
evaluate("closePhoto()");
pass('photo preview opens directly without external button');

// 4) 内置视频播放器：DOM 结构 + 流地址 + 手势提示
evaluate("playMedia(state.panes[0].items.find(x=>x.name==='movie.mkv'),'video')");
await wait(()=>w.document.querySelector('#mediaLayer.open .playerMedia'));
await wait(()=>{const v=w.document.querySelector('.playerMedia');return v&&v.src&&v.src.includes('/media/');});
assert(w.document.querySelector('#mediaLayer .playerStage'),'gesture stage present');
assert(w.document.querySelector('#mediaLayer [data-player-dim]'),'brightness overlay present');
assert(w.document.querySelector('#mediaLayer [data-player-boost]'),'boost badge present');
assert(w.document.querySelector('#mediaLayer [data-player-side]'),'volume/brightness indicator present');
assert(w.document.querySelector('#mediaLayer .playerSeek'),'seek slider present');
assert(w.document.querySelector('#mediaLayer [data-player-play]'),'play/pause button present');
assert(w.document.querySelector('#mediaLayer [data-player-foot]').textContent.includes('长按屏幕'),'gesture hint in footer');
assert(w.document.querySelector('#mediaLayer .photoSub').textContent.includes('视频'),'video player subtitle');
evaluate("closePlayer()");
await wait(()=>!w.document.querySelector('#mediaLayer .playerMedia'));
pass('video player renders with gestures and same-origin stream url');

// 5) 内置音频播放器
evaluate("playMedia(state.panes[0].items.find(x=>x.name==='song.flac'),'audio')");
await wait(()=>w.document.querySelector('#mediaLayer.open .playerMedia'));
await wait(()=>{const v=w.document.querySelector('.playerMedia');return v&&v.src&&v.src.includes('/media/');});
assert(w.document.querySelector('#mediaLayer .playerArt'),'audio art present');
assert(w.document.querySelector('#mediaLayer .photoSub').textContent.includes('音频'),'audio player subtitle');
evaluate("closePlayer()");
pass('audio player renders through the same layer');

// 6) 点击顶部路径 → 跳转对话框（粘贴/取消/确定）
await wait(()=>w.document.querySelector('#toolbar .titleblock.pathJump'));
w.document.querySelector('#toolbar .titleblock.pathJump').dispatchEvent(new w.Event('click',{bubbles:true}));
await wait(()=>[...w.document.querySelectorAll('#modalLayer .dialog h2')].some(x=>x.textContent==='跳转'));
const jumpInput=w.document.querySelector('#jumpPath');
assert(jumpInput&&jumpInput.value.startsWith('/storage/emulated/0'),'input prefilled with current path');
dialogButton('粘贴').click();
await wait(()=>jumpInput.value==='/storage/emulated/0/Download');
assert([...w.document.querySelectorAll('#modalLayer .actions button')].map(b=>b.textContent.trim()).join(',').includes('取消'),'cancel button present');
dialogButton('取消').click();
await wait(()=>!w.document.querySelector('#modalLayer .dialog'));
pass('path bar opens 跳转 dialog with 粘贴/取消/确定');

// 7) 底栏上滑 → 快速访问面板（最近/书签 + 菜单六项）
evaluate("recordRecentFolder('/storage/emulated/0/Pictures/.thumbnails')");
evaluate("openQuickPanel()");
await wait(()=>w.document.querySelector('#quickLayer .quickPanel'));
assert.equal(w.document.querySelector('#quickLayer .quickPanel').style.height,'60vh','default height 60%');
const tabs=[...w.document.querySelectorAll('#quickLayer [data-quick-tab]')].map(b=>b.textContent.trim());
assert.deepEqual(tabs,['最近','书签'],'recent/bookmarks tabs');
await wait(()=>w.document.querySelector('#quickLayer .quickRow'));
assert(w.document.querySelector('#quickLayer .quickRow small').textContent.includes('.thumbnails'),'recent entry row shown');
w.document.querySelector('[data-quick-menu]').click();
await wait(()=>[...w.document.querySelectorAll('[role=menuitem],[role=menuitemcheckbox]')].some(x=>x.textContent.trim()==='清空记录'));
const quickMenu=[...w.document.querySelectorAll('[role=menuitem],[role=menuitemcheckbox]')].map(x=>x.textContent.trim());
assert.deepEqual(quickMenu,['过滤','添加分组','调整高度','手势说明','开启记录','清空记录'],'quick panel menu matches screenshot 3');
assert.equal(menuButton('开启记录').getAttribute('aria-checked'),'true','recording enabled by default');
w.document.querySelector('#quickLayer .shade').dispatchEvent(new w.Event('click',{bubbles:true}));
await wait(()=>!w.document.querySelector('#quickLayer .quickPanel'));
pass('bottom swipe-up panel with 最近/书签 and 6-item menu');

// 8) 书签分组渲染
evaluate("pref.bookmarkGroups=['工作'];pref.bookmarks=[{name:'photo.jpg',path:'/storage/emulated/0/photo.jpg',dir:false,group:'工作'},{name:'Download',path:'/storage/emulated/0/Download',dir:true}];");
evaluate("openQuickPanel()");
await wait(()=>w.document.querySelector('#quickLayer [data-quick-tab="bookmarks"]'));
w.document.querySelector('[data-quick-tab="bookmarks"]').dispatchEvent(new w.Event('click',{bubbles:true}));
await wait(()=>w.document.querySelector('#quickLayer .quickGroup'));
assert.equal(w.document.querySelector('#quickLayer .quickGroup').textContent,'工作');
assert([...w.document.querySelectorAll('#quickLayer .quickRow')].length===2);
pass('bookmark groups render in panel');

// 9) 0.1.15 修复：底栏上滑手势（touch-action）+ 窗格切换即时刷新顶部路径
const css=fs.readFileSync(path.join(assets,'style.css'),'utf8');
assert(css.includes('#bottom{touch-action:none}'),'bottom bar owns vertical swipes');
assert(/\.mediaLayer\{[^}]*user-select:none/.test(css),'media layer blocks text selection');
evaluate("state.panes[1].path='/storage/emulated/0/Download';state.panes[1].items=state.panes[1].items||[];renderFiles();");
const titleBefore=w.document.querySelector('#toolbar .title').textContent;
evaluate("activatePane(1)");
assert.notEqual(w.document.querySelector('#toolbar .title').textContent,titleBefore,'pane switch refreshes header path immediately');
evaluate("activatePane(0)");
assert.equal(w.document.querySelector('#toolbar .title').textContent,titleBefore,'switch back restores left path');
pass('0.1.15 fixes: bottom swipe touch-action and pane path refresh');

// 10) 0.1.16 修复：底栏上滑吞掉按钮点击（不误触「上级目录」）+ 路径跑马灯
evaluate("renderFiles()");
const bar=w.document.querySelector('#bottom');
bar.dispatchEvent(new w.MouseEvent('pointerdown',{bubbles:true,clientY:700,clientX:50}));
bar.dispatchEvent(new w.MouseEvent('pointermove',{bubbles:true,clientY:640,clientX:50}));
bar.dispatchEvent(new w.MouseEvent('pointermove',{bubbles:true,clientY:600,clientX:50}));
const ev=new w.MouseEvent('click',{bubbles:true,cancelable:true});
bar.dispatchEvent(ev);
assert(ev.defaultPrevented,'swipe flag suppresses trailing button click');
const ev2=new w.MouseEvent('click',{bubbles:true,cancelable:true});
bar.dispatchEvent(ev2);
assert(!ev2.defaultPrevented,'flag resets after one click / new press');
const cssAll=fs.readFileSync(path.join(assets,'style.css'),'utf8');
assert(cssAll.includes('@keyframes pathMarquee'),'path marquee keyframes present');
assert(cssAll.includes('.pathScroll .pathMarquee'),'marquee applies via pathScroll class');
// 长路径触发跑马灯（happy-dom 无真实布局，stub 尺寸后直接驱动 fitPath）
evaluate("const tt=document.querySelector('#toolbar .title');Object.defineProperty(tt,'clientWidth',{value:100,configurable:true});Object.defineProperty(tt,'scrollWidth',{value:900,configurable:true});fitPath(tt,'/storage/emulated/0/very/long/path/segment')");
await wait(()=>w.document.querySelector('#toolbar .title.pathScroll .pathMarquee'));
assert(w.document.querySelector('#toolbar .title .pathMarquee').textContent.includes('very/long/path'),'marquee keeps full path text');
pass('0.1.16 fixes: swipe click suppression and path marquee');

if(errors.length){console.log('PAGE ERRORS:',errors.join(' | '));process.exit(1);}
console.log('ALL '+count+' PASS');
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
