const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const {Window}=await import('/tmp/test-node/node_modules/happy-dom/lib/index.js');const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.45',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'img1.jpg',path:'${root}/pics.zip',archive:'${root}/pics.zip',entry:'img1.jpg',dir:false,size:100,time:1},{name:'img2.jpg',path:'${root}/pics.zip',archive:'${root}/pics.zip',entry:'img2.jpg',dir:false,size:100,time:2},{name:'doc.txt',path:'${root}/pics.zip',archive:'${root}/pics.zip',entry:'doc.txt',dir:false,size:1,time:3}];data.total=1;data.free=1;break;
   case'resolve':data='/cache/tmp/'+(a.entry||'x');break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,act,openPhoto,playMedia,closePhoto};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=s=>w.eval(s.replace(/\b(state|act|openPhoto|playMedia|closePhoto)\b/g,'window.__test.$1'));
async function wait(fn,name){for(let i=0;i<200;i++){if(fn())return;await sleep();}throw Error('Timed out at '+(name||'?')+': '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
// 0) 压缩包内图片/视频行带 media dataset（缩略图支持）
const mediaRow=[...w.document.querySelectorAll('.file')].find(r=>r.dataset.mediaKind==='image'&&r.dataset.mediaArchive);
assert(mediaRow,'archive image row has media dataset');
assert(mediaRow.dataset.mediaEntry==='img1.jpg','entry recorded');
pass('压缩包内媒体行标记缩略图属性');
// 1) 压缩包内图片直接预览（不再弹"无法预览"）
await evaluate("openPhoto(state.panes[0].items.find(x=>x.name==='img1.jpg'))");
await wait(()=>w.document.querySelector('#mediaLayer.open .photoImage'),'photo opens');
assert(!w.document.body.textContent.includes('暂不支持内置图片预览'),'no unavailable dialog');
const src=w.document.querySelector('.photoImage').getAttribute('src')||'';
const calls=w.window.__calls.filter(c=>c.action==='resolve');
assert(calls.length>=2,'archive entries resolved: '+calls.length);
const entry=(calls[0].args.entry)||'';
assert(calls.every(c=>c.args.archive&&c.args.archive.endsWith('.zip')),'resolve called with archive args');
pass('压缩包内图片解出临时副本后预览');
// 2) 压缩包内相册包含多张图片，可切换
const sub=w.document.querySelector('.photoSub');
assert(sub&&sub.textContent.trim()==='1 / 2','gallery has 2 images: '+(sub&&sub.textContent));
w.document.querySelector('[data-photo-next]').click();await sleep();
assert(w.document.querySelector('.photoTitle').textContent==='img2.jpg','next navigates');
pass('压缩包内相册切换');
// 3) 压缩包内音频可播放（先 resolve）
await evaluate("closePhoto();playMedia(state.panes[0].items.find(x=>x.name==='img1.jpg'),'audio')");
await wait(()=>w.window.__calls.some(c=>c.action==='resolve'&&!w.document.querySelector('.photoImage')),'resolved for audio');
assert(!w.document.body.textContent.includes('暂不支持内置音频预览'),'no audio unavailable dialog');
pass('压缩包内音频解出后播放');
if(errors.length)throw Error('page errors: '+errors.join(' | '));
console.log('ALL '+count+' PASS');
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
