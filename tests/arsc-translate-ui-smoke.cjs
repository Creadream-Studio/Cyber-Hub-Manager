const fs=require('fs'),path=require('path'),assert=require('assert');
(async()=>{const modulePath=process.env.HAPPY_DOM_PATH||path.resolve(__dirname,'../../toolchain/test-node/node_modules/happy-dom/lib/index.js');const {Window}=await import(modulePath);const w=new Window({url:'https://cyber-hub.local/',width:398,height:768,settings:{disableJavaScriptFileLoading:true,disableCSSFileLoading:true}});const errors=[];w.addEventListener('error',e=>errors.push(e.message));w.console.error=(...a)=>errors.push(a.join(' '));w.console.warn=()=>{};
const assets=path.resolve(__dirname,'../app/src/main/assets');
w.document.write(fs.readFileSync(path.join(assets,'app.html'),'utf8').replace(/<script[^>]*>[\s\S]*?<\/script>/g,'').replace(/<link[^>]*>/g,''));
const root='/storage/emulated/0';
const rows=[{id:1,name:'a',source:'Hello',target:'',hasTarget:false},{id:2,name:'b',source:'World',target:'世界',hasTarget:true},{id:3,name:'c',source:'Save',target:'',hasTarget:false}];
w.eval(`(()=>{
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:'${root}',workspace:'${root}/CyberHub',version:'0.1.38',ownApk:'${root}/self.apk'};break;
   case'list':data=[{name:'res.apk',path:'${root}/res.apk',dir:false,size:340000,time:1}];data.total=1;data.free=1;break;
   case'arscOpen':data={session:'s1',name:'resources.arsc',size:10};break;
   case'arscTranslate':if(a.config===''){data={configs:[{qualifiers:'',label:'默认'},{qualifiers:'en',label:'英语'}],rows:[],total:0};}else{data={configs:[],rows:${JSON.stringify(rows)},total:3};}break;
   case'arscEdit':data={ok:true};break;
   case'translate':data={text:'T['+a.text+']'};break;
   case'translateConfig':data={config:{provider:'tencent',target:'en'},languages:[{key:'en',name:'英语'},{key:'zh',name:'中文'}]};break;
   default:data=true;
  }
  window.receive({id,ok:true,data});
 }catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
 window.CyberNativeBridge=window.CyberNative;
})();`);
for(const file of ['boot.js','vendor/codemirror.js','vendor/clike.js','vendor/xml.js','vendor/searchcursor.js','app.js'])w.eval(file==='app.js'?fs.readFileSync(path.join(assets,file),'utf8').replace('\ninit();','\nwindow.__test={state,act,showArscTranslate,arscTranslateRender};\ninit();'):fs.readFileSync(path.join(assets,file),'utf8'));
const sleep=()=>new Promise(r=>setTimeout(r,30));const evaluate=s=>w.eval(s.replace(/\b(state|act|showArscTranslate|arscTranslateRender)\b/g,'window.__test.$1'));
let waitName='?';async function wait(fn){waitName=(waitName==='?')?arguments[1]||waitName:waitName;for(let i=0;i<200;i++){if(fn())return;await sleep();}console.error('TIMEOUT at',fn.toString().slice(0,80));throw Error('Timed out: '+errors.join('|'));}
let count=0;const pass=n=>{count++;console.log('PASS',n)};

await wait(()=>w.document.querySelectorAll('.file').length>0,'files');
// 打开 ARSC 翻译（模拟语言列表页）
await evaluate("act(()=>showArscTranslate({path:'/x/res.apk'},'resources.arsc'))");
await wait(()=>w.document.querySelector('#arscTransLangs .listrow'),'langs');
const langLabels=[...w.document.querySelectorAll('#arscTransLangs .listrow .main')].map(x=>x.childNodes[0].textContent);
assert.deepEqual(langLabels,['[DEFAULT]','en（英语）'],'language list renders');
pass('language list renders');

// 点语言进入字符串页
w.document.querySelector('#arscTransLangs .listrow:last-child').click();
await wait(()=>w.document.querySelector('#arscTlRows tr'),'table');
pass('strings table renders');

// 底部五键存在且可点
const bottomKeys=()=>[...w.document.querySelectorAll('#bottom button')].filter(b=>b.isConnected);
assert.equal(bottomKeys().length,5,'5 bottom keys, got '+bottomKeys().length);
assert.deepEqual(bottomKeys().map(k=>k.getAttribute('aria-label')),['全选','反选','多选','翻译','连选'],'key order');
// 点全选
console.error('click 全选');bottomKeys()[0].click();await sleep();console.error('sel=',evaluate("state.arscTranslate.sel.size"));
const selAfter=evaluate("state.arscTranslate.sel.size");
assert.equal(selAfter,3,'select-all works, got '+selAfter);
pass('全选 works');
// 反选
bottomKeys()[1].click();await sleep();console.error('after invert sel=',evaluate("state.arscTranslate.sel.size"));
pass('反选 works');
// 多选开关
bottomKeys()[2].click();await sleep();
assert.equal(evaluate("state.arscTranslate.multi"),true,'multi on');
bottomKeys()[2].click();await sleep();
assert.equal(evaluate("state.arscTranslate.multi"),false,'multi off');
pass('多选 toggles');
// 翻译键弹对话框
bottomKeys()[3].click();
await wait(()=>[...w.document.querySelectorAll('#modalLayer h2')].some(h=>h.textContent==='自动翻译'),'dialog');
assert(w.document.querySelector('#arscTlEngine'),'engine select present');
pass('翻译 dialog opens');
// 关闭对话框, 连选开关
[...w.document.querySelectorAll('#modalLayer .actions button')].find(b=>b.textContent==='取消').click();
await sleep();
bottomKeys()[4].click();await sleep();
assert.equal(evaluate("state.arscTranslate.rangeOn"),true,'range on');
pass('连选 toggles');

// 全选后行高亮 + 选中数量显示
bottomKeys()[0].click();await sleep();
assert.equal(evaluate("state.arscTranslate.sel.size"),3,'all selected');
assert.equal(w.document.querySelectorAll('#arscTlRows tr.tlSel').length,3,'rows highlighted');
assert(w.document.querySelector('#toolbar .subtitle').textContent.includes('已选'),'header shows selection');
pass('全选 highlights rows');

// 翻译对话框确认 → 调用 translate 并写回 arscEdit
bottomKeys()[3].click();
await wait(()=>[...w.document.querySelectorAll('#modalLayer h2')].some(h=>h.textContent==='自动翻译'),'dialog2');
[...w.document.querySelectorAll('#modalLayer .actions button')].find(b=>b.textContent==='确认').click();
await wait(()=>w.window.__calls.filter(c=>c.action==='arscEdit').length>=2,'edits');
const edited=w.window.__calls.filter(c=>c.action==='arscEdit').map(c=>c.args.value);
assert(edited.includes('T[Hello]')&&edited.includes('T[Save]'),'untranslated rows written: '+edited.join(','));
assert.equal(evaluate("state.arscTranslate.sel.size"),0,'selection cleared after run');
pass('自动翻译确认写回 ARSC');

if(errors.length)throw Error('page errors: '+errors.join(' | '));
console.log('ALL '+count+' PASS');
})().catch(e=>{console.error('FAIL',e.message);process.exit(1);});
