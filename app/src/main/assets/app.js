(function () {
'use strict';
const $=s=>document.querySelector(s),esc=s=>String(s == null ? '' : s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const paths={menu:'M4 6h16M4 12h16M4 18h16',more:'M12 5v.01M12 12v.01M12 19v.01',back:'M15 5l-7 7 7 7',forward:'M9 5l7 7-7 7',up:'M5 11l7-7 7 7M12 4v16',down:'M5 13l7 7 7-7M12 20V4',plus:'M12 4v16M4 12h16',swap:'M4 8h16l-4-4M20 16H4l4 4',folder:'M3 6h7l2 2h9v12H3z',file:'M6 3h8l4 4v14H6zM14 3v5h4',code:'M9 9l-4 3 4 3M15 9l4 3-4 3',dex:'M6 3h8l4 4v14H6zM9 11v6M12 11h3v6h-3z',layers:'M3 8l9-5 9 5-9 5zM3 12l9 5 9-5M3 16l9 5 9-5',apk:'M5 10h14v9H5zM8 4l-2-2M16 4l2-2M5 8a7 7 0 0114 0zM2 11v6M22 11v6M8 19v3M16 19v3',zip:'M5 3h14v18H5zM10 3h3v3h-3v3h3v3h-3v3h3v3h-3',search:'M10 3a7 7 0 110 14 7 7 0 010-14M15 15l6 6',save:'M4 3h14l3 3v15H3V3zM7 3v6h10V3M7 21v-8h10v8',undo:'M3 7v6h6M3 13a9 9 0 0117 0',redo:'M21 7v6h-6M21 13a9 9 0 00-17 0',edit:'M4 16l12-12 4 4L8 20H4zM13 7l4 4',pin:'M9 3h6l-1 7 4 4H6l4-4zM12 14v8',compass:'M12 2a10 10 0 110 20 10 10 0 010-20M16 7l-3 6-6 3 3-6z',build:'M5 3l6 6-3 3-6-6M10 10l11 11M16 3l5 5M13 6l5-3',refresh:'M20 3v6h-6M20 9a8 8 0 10-1 9',switch:'M3 13l5 5 13-13M3 7l5 5 9-9',jump:'M11 5h10M11 10h10M11 15h10M11 20h10M3 10l4 5-4 5',wrap:'M3 5h18M3 10h14a4 4 0 010 8h-5M12 15l-3 3 3 3M3 15h3',export:'M12 15V2M7 7l5-5 5 5M4 13v8h16v-8',settings:'M9 3h6l1 4 4 2v6l-4 2-1 4H9l-1-4-4-2V9l4-2zM12 9a3 3 0 110 6 3 3 0 010-6',close:'M5 5l14 14M19 5L5 19',trash:'M3 6h18M8 6V3h8v3M5 6l1 15h12l1-15M10 10v7M14 10v7',plugin:'M9 3h6v5h5v6h-5v7H9v-7H4V8h5z',remote:'M3 3h18v13H3zM12 16v5M8 21h8M7 8h10M9 11h6',phone:'M7 2h10v20H7zM11 18h2',sd:'M8 3h10v18H6V7zM10 4v4M13 4v4M16 4v4',terminal:'M3 4h18v16H3zM7 8l4 4-4 4M13 16h4',history:'M3 5v6h6M3 11a9 9 0 113 8M12 6v7l4 2',sun:'M12 7a5 5 0 110 10 5 5 0 010-10M12 1v3M12 20v3M1 12h3M20 12h3M4 4l2 2M18 18l2 2M4 20l2-2M18 6l2-2',info:'M12 2a10 10 0 110 20 10 10 0 010-20M12 10v7M12 6v.1',check:'M4 12l5 5L20 6',copy:'M8 7h13v15H8zM16 7V2H3v15h5',network:'M3 4h18v11H3zM12 15v6M5 21h14M8 8h8',text:'M3 4h18M12 4v17M8 21h8',play:'M7 3l14 9-14 9z',photo:'M4 5h16v14H4zM8 13l3-4 2 3 2-2 3 3M8 9h.01',gallery:'M3 4h18v16H3zM3 16l5-5 4 4 2-2 7 6M8 9h.01',video:'M3 5h13v14H3zM16 9l5-3v12l-5-3z',audio:'M9 18V6l10-2v12M9 18a3 3 0 11-6 0 3 3 0 016 0M19 16a3 3 0 11-6 0 3 3 0 016 0',pause:'M8 5v14M16 5v14',download:'M12 2v13M7 10l5 5 5-5M4 17v4h16v-4',lock:'M5 11h14v9H5zM8 11V7a4 4 0 018 0v4',unlock:'M5 11h14v9H5zM8 11V7a4 4 0 018 0'};
const filledIcons={
 menu:'M3 6h18v2H3zm0 5h18v2H3zm0 5h18v2H3z',more:'M12 4a2 2 0 110 4 2 2 0 010-4zm0 6a2 2 0 110 4 2 2 0 010-4zm0 6a2 2 0 110 4 2 2 0 010-4z',
 folder:'M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8z',
 back:'M15.4 5L8.4 12l7 7-1.4 1.4L5.6 12 14 3.6z',forward:'M8.6 5l7 7-7 7 1.4 1.4 8.4-8.4L10 3.6z',
 up:'M11 21h2V7l5.6 5.6L20 11.2l-8-8-8 8 1.4 1.4L11 7z',plus:'M11 3h2v8h8v2h-8v8h-2v-8H3v-2h8z',
 swap:'M3 7l4-4v3h14v2H7v3zm18 10l-4 4v-3H3v-2h14v-3z',
 file:'M6 2h8l6 6v12a2 2 0 01-2 2H6a2 2 0 01-2-2V4a2 2 0 012-2zm7 1v6h6z',
 edit:'M3 17.3V21h3.7L17.8 9.9l-3.7-3.7zM20.7 7a1 1 0 000-1.4l-2.3-2.3a1 1 0 00-1.4 0l-1.8 1.8 3.7 3.7z',
 save:'M17 3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V7zm-5 16a3 3 0 110-6 3 3 0 010 6zM6 4h10v5H6z',
 build:'M2 7l5-5h5L9 5l2 2-3 3-2-2-2 2zm8 3l2-2 10 10-4 4-9-10z',
 pin:'M9 3h6v2h-1v5l3 3v2h-4v7h-2v-7H7v-2l3-3V5H9z',
 compass:'M12 2a10 10 0 100 20 10 10 0 000-20zm5.6 4.4L14 14l-7.6 3.6L10 10zm-5.6 4.1a1.5 1.5 0 100 3 1.5 1.5 0 000-3z',
 apk:'M4 10h16v9H4zM7 19h3v3H7zm7 0h3v3h-3zM1 10h2v8H1zm20 0h2v8h-2zM5.1 2l1.4-.8L8 4.1a10 10 0 018 0l1.5-2.9 1.4.8-1.6 3A7.6 7.6 0 0120 9H4a7.6 7.6 0 012.7-4z',
 arsc:'M12 2L1 8l11 6 11-6zm-9 9l9 5 9-5 2 1-11 6L1 12zm0 4l9 5 9-5 2 1-11 6L1 16z'
};
function icon(name){
 if(name==='apks')name='zip';
 const base='<svg class="appIcon" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" focusable="false"';
 if(name==='xml'||name==='dex')return base+' fill="currentColor" opacity=".9"><path d="M6 2h8l5 5v13a2 2 0 01-2 2H6a2 2 0 01-2-2V4a2 2 0 012-2zm7 1v5h5z"/>'+(name==='xml'?'<path d="M10 11l-3 3 3 3m4-6l3 3-3 3" fill="none" stroke="currentColor" stroke-width="1.5" opacity=".55"/>':'<text x="6.5" y="18" fill="currentColor" opacity=".55" font-family="sans-serif" font-size="9" font-weight="700">01</text>')+'</svg>';
 if(filledIcons[name])return base+' fill="currentColor"><path fill-rule="evenodd" d="'+filledIcons[name]+'"/></svg>';
 return base+' fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="square" stroke-linejoin="miter"><path d="'+(paths[name]||paths.file)+'"/></svg>';
}
function brandLogo(){return '<svg class="brandLogo" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" focusable="false" fill="none"><rect x="3.5" y="4.5" width="17" height="15" rx="2" stroke="currentColor" stroke-width="1.4"/><path d="M12 4.5v15" stroke="currentColor" stroke-width="1" opacity=".35"/><path class="brandAccent" d="M6 15.5h3" stroke-width="1.6" stroke-linecap="round"/><path d="M15 8.5h2.5M15 11.5h2.5" stroke="currentColor" stroke-width="1" opacity=".45" stroke-linecap="round"/></svg>';}
const EDITOR_SYMBOLS=['→','/','+','-','*','=','<'];
function symbolBar(insert){const bottom=$('#bottom');bottom.replaceChildren();bottom.classList.add('editorBottom');for(const text of EDITOR_SYMBOLS){const b=document.createElement('button');b.className='bottom-key';b.textContent=text;b.onclick=()=>insert(text);bottom.append(b);}}
function iconButton(name,label,fn){const b=document.createElement('button');b.className='iconbtn';b.setAttribute('aria-label',label);b.title=label;b.innerHTML=icon(name);b.onclick=()=>act(fn);return b;}
const pending=new Map();let seq=0,busy=0,bundleJob=null;
window.bundleProgress=(job,message)=>{if(bundleJob&&bundleJob.id===job){const label=document.querySelector('#bundleProgress');if(label)label.textContent=message;}};
window.receive=r=>{const p=pending.get(r.id);if(!p)return;pending.delete(r.id);clearTimeout(p.timer);if(p.foreground)busy--;document.body.classList.toggle('busy',busy>0);r.ok?p.resolve(r.data):p.reject(Error(r.error));};
function rpc(action,args={}){return new Promise((resolve,reject)=>{if(!window.CyberNative)return reject(Error('请在 Android APK 内打开本应用以访问文件。'));const id=String(++seq);const timer=setTimeout(()=>window.receive({id,ok:false,error:'操作超时。应用可能仍在处理；重试前请先刷新。'}),action==='init'?15000:['bundleConvert','resSlim','resObfuscate','resDeobfuscate','resDedupe','sigBypass','injectLog','injectProvider','dexRepartition','apkXmlReplace','apkXmlTranslate','fileSplit','fileMerge','gpgEncrypt','gpgDecrypt'].includes(action)?600000:180000);const foreground=action!=='apkIcon'&&action!=='listThumb';pending.set(id,{resolve,reject,timer,foreground});if(foreground){busy++;document.body.classList.add('busy');}try{window.CyberNative.request(id,action,JSON.stringify(args));}catch(e){window.receive({id,ok:false,error:e.message||String(e)});}});}
async function act(fn){try{await fn();}catch(e){alertBox('无法完成',e.message||String(e));}}
let toastTimer;function toast(text){$('#toast').textContent=text;$('#toast').style.display='block';clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('#toast').style.display='none',3600);}
const storage={get(key){try{return window.localStorage.getItem(key);}catch(e){return null;}},set(key,value){try{window.localStorage.setItem(key,value);}catch(e){/* Keep this session usable when storage is unavailable. */}}};
function readPreferences(){try{const value=JSON.parse(storage.get('cyber-preferences')||'{}');return value && typeof value==='object' && !Array.isArray(value) ? value : {};}catch(e){return {};}}
const pref=Object.assign({font:13,wrap:false,lines:true,hidden:false,light:false,motion:true,pinchZoom:true,networkStorages:[],startRoot:false,startShell:false,startLeft:'首页',startRight:'首页',autoService:'未启用',listSize:'中',nameLines:'2 行',timeStyle:'隐藏秒数，精简年份',dateFormat:'yyyy-MM-dd HH:mm:ss',showExtensions:true,sortBy:'名称',sortAsc:true,foldersFirst:true,autoClean:'禁用',bookmarksInDrawer:false,bookmarkTop:true,confirmInstall:false,recycleEnabled:true,deleteWarning:true,bottomMargin:'12 dp',externalThumbs:true,fastExternalTransfer:true,netThumbs:true,netWifiOnly:true,language:'自动检测'},readPreferences());
const state={screen:'files',active:0,panes:[{path:'',archive:'',entry:'',mode:'',items:[],history:[],future:[]},{path:'',archive:'',entry:'',mode:'',items:[],history:[],future:[]}],clipboard:null,info:null,editor:null,cm:null,dex:{prefix:'',tab:'explorer',history:[],source:null,mode:'auto',query:''},previous:'files'};
function applyDisplayPrefs(){
 document.body.classList.toggle('list-small',pref.listSize==='小');
 document.body.classList.toggle('list-large',pref.listSize==='大');
 document.documentElement.style.setProperty('--name-lines',String(parseInt(pref.nameLines,10)||2));
 const extra=parseInt(pref.bottomMargin,10)||0;
 document.documentElement.style.setProperty('--bottom-extra',extra+'px');
}
function savePref(){storage.set('cyber-preferences',JSON.stringify(pref));document.body.classList.toggle('light',pref.light);syncMotion();applyDisplayPrefs();}
savePref();
function size(n){if(n<1024)return n+'B';if(n<1048576)return(n/1024).toFixed(2)+'K';if(n<1073741824)return(n/1048576).toFixed(2)+'M';return(n/1073741824).toFixed(2)+'G';}
function formatDate(t,style=pref.timeStyle,fmt=pref.dateFormat){
 if(t<=0)return '';
 const d=new Date(t),now=Date.now(),p=n=>String(n).padStart(2,'0');
 if(style==='相对时间'){
  const diff=Math.max(0,now-t);
  if(diff<60000)return '刚刚';
  if(diff<3600000)return Math.floor(diff/60000)+' 分钟前';
  if(diff<86400000)return Math.floor(diff/3600000)+' 小时前';
  if(diff<2592000000)return Math.floor(diff/86400000)+' 天前';
 }
 let out=String(fmt||'yyyy-MM-dd HH:mm:ss');
 if(style==='隐藏秒数，精简年份')out=out.replace(/yyyy/g,'yy').replace(/[:.]ss/g,'');
 return out.replace(/yyyy/g,d.getFullYear()).replace(/yy/g,String(d.getFullYear()).slice(-2)).replace(/MM/g,p(d.getMonth()+1)).replace(/dd/g,p(d.getDate())).replace(/HH/g,p(d.getHours())).replace(/mm/g,p(d.getMinutes())).replace(/ss/g,p(d.getSeconds()));
}
function date(t){return formatDate(t);}
function displayName(f){const n=f.name||'';if(pref.showExtensions||f.dir)return n;const i=n.lastIndexOf('.');return i>0?n.slice(0,i):n;}
function sortItems(items){
 if(!Array.isArray(items))return items;
 const dir=pref.sortAsc?1:-1,key=pref.sortBy||'名称';
 return items.slice().sort((a,b)=>{
  if(pref.foldersFirst&&a.dir!==b.dir)return a.dir?-1:1;
  let r=0;
  if(key==='大小')r=(Number(a.size)||0)-(Number(b.size)||0);
  else if(key==='时间')r=(Number(a.time)||0)-(Number(b.time)||0);
  else if(key==='类型')r=(fileType(a)||'').localeCompare(fileType(b)||'');
  else r=String(a.name||'').localeCompare(String(b.name||''),'zh-Hans-CN',{numeric:true});
  return r*dir;
 });
}
async function installApk(path){if(pref.confirmInstall&&!await confirmBox('安装 APK？','将调用系统安装器安装：\n'+path,'安装'))return;await rpc('install',{path});}
function openExternal(url){return rpc('openUrl',{url}).catch(e=>alertBox('无法打开链接',e.message||String(e)));}
function apksOptions(file,index=state.active){
 options(file.name,[{icon:'build',label:'转换为 APK',run:()=>convertBundleDialog(file)},{icon:'zip',label:'浏览内容',run:async()=>{const path=await rpc('resolve',fileArgs(file));await navigate(index,{path:state.panes[index].path,archive:path,entry:''});}},{icon:'export',label:'解压到…',run:()=>extractDialog(file)}]);
}
async function convertBundleDialog(file){
 if(bundleJob)return toast('已有一个转换正在进行。');
 const args=fileArgs(file),info=await rpc('bundleInspect',args);
 const destination=!file.archive&&file.path?file.path.slice(0,file.path.lastIndexOf('/')):state.info.workspace;
 const d=dialog('将 APKS 转换为 APK',`<p class="bundleSource">${esc(file.name)}</p><label for="bundleChoice">APK 变体</label><select id="bundleChoice">${info.choices.length>1?'<option value="">请选择变体…</option>':''}${info.choices.map(c=>`<option value="${esc(c.id)}">${esc(c.label)} (${c.count} APK}')</option>`).join('')}</select><div id="bundleContents" class="bundleHint"></div><label for="bundleDestination">保存到文件夹</label><input id="bundleDestination" value="${esc(destination)}"><label for="bundleName">APK 文件名</label><input id="bundleName" value="${esc(file.name.replace(/\.apks$/i,'')+'-merged.apk')}"><p class="bundleHint">合并的 APK 使用 赛博中枢 开发密钥，无法用于更新以其他密钥签名的应用。已签名的独立 APK 将保留其原始签名。</p>${info.skipped.length?'<details class="bundleHint"><summary>不可用的变体</summary><p>'+info.skipped.map(esc).join('<br>')+'</p></details>':''}`,[['取消',closeModal],['转换',async()=>{
  if(bundleJob)return;
  const choice=d.querySelector('#bundleChoice').value,name=d.querySelector('#bundleName').value.trim(),folder=d.querySelector('#bundleDestination').value.trim();
  if(!choice)return toast('请选择一个 APK 变体。');
  if(!name.toLowerCase().endsWith('.apk')||/[\\/]/.test(name))return toast('请输入以 .apk 结尾的文件名。');
  if(!folder)return toast('请选择目标文件夹。');
  closeModal(false);dialog('正在转换为 APK','<div class="bundleWorking" role="status"><span class="bundleSpinner" aria-hidden="true"></span><p id="bundleProgress" aria-live="polite">正在准备转换…</p></div><p class="bundleHint">请保持应用打开。您的原始 APKS 不会改变。</p>');
  const job='bundle-'+Date.now();bundleJob={id:job};let result;
  try{result=await rpc('bundleConvert',{...args,choice,name,destination:folder,fingerprint:info.fingerprint,job});}
  finally{bundleJob=null;closeModal(false);}
  try{await refresh();}catch(ignore){toast('APK 已保存。打开其文件夹以刷新文件列表。');}
  const resultDialog=dialog('APK 已就绪',`<p>${result.merged?result.apkCount+' 个 APK 已合并。':'已提取独立 APK。'} 签名已校验。</p><p class="bundleSource">${esc(result.path)}</p><p class="bundleHint">${size(result.size)} · ${result.originalSignature?'保留了原始签名':'使用 赛博中枢 开发密钥签名'}</p>`,[['关闭',closeModal],['打开文件夹',async()=>{closeModal();state.screen='files';await navigate(state.active,{path:folder,archive:'',entry:''});}],['安装',async()=>{closeModal();await installApk(result.path);}]]);resultDialog.classList.add('bundleDialog');
 }]]);
 d.classList.add('bundleDialog');const update=()=>{const choice=info.choices.find(c=>c.id===d.querySelector('#bundleChoice').value);d.querySelector('#bundleContents').textContent=choice?choice.entries.slice(0,8).map(x=>x.split('/').pop()).join(', ')+(choice.entries.length>8?' …':''):'变体分别转换。请选择与您的设备匹配的一个。';};d.querySelector('#bundleChoice').onchange=update;update();
}
// Reuse one isolated exit surface per layer. 移动 existing nodes: no DOM cloning,
// CSS parsing or geometry measurement on each close. Reopening cancels the old exit.
function motionEnabled(){return pref.motion!==false&&!(window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches);}
function syncMotion(){document.body.classList.toggle('reduceMotion',!motionEnabled());}
let exitStyles='';const closingLayers=new Map();
function clearLayer(layer,animate=true){
 let exit=closingLayers.get(layer.id);
 if(exit){clearTimeout(exit.timer);exit.group.replaceChildren();exit.ghost.remove();}
 if(animate&&layer.children.length&&motionEnabled()&&Element.prototype.attachShadow){
  if(!exit){
   const ghost=document.createElement('div');ghost.className='closingLayer';ghost.setAttribute('aria-hidden','true');ghost.inert=true;ghost.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:'+(layer.id==='menuLayer'?41:layer.id==='modalLayer'?31:21);
   const shadow=ghost.attachShadow({mode:'closed'}),style=document.createElement('style');
   if(!exitStyles)for(const sheet of Array.from(document.styleSheets)){try{exitStyles+=Array.from(sheet.cssRules).map(r=>r.cssText).join('\n');}catch(ignore){}}
   style.textContent=exitStyles;shadow.append(style);const group=document.createElement('div');group.className='closing';shadow.append(group);exit={ghost,group,timer:0};closingLayers.set(layer.id,exit);
  }
  while(layer.firstChild)exit.group.append(layer.firstChild);
  document.body.append(exit.ghost);exit.timer=setTimeout(()=>{exit.group.replaceChildren();exit.ghost.remove();},170);
 }
 layer.replaceChildren();
}
function closeMenu(){clearLayer($('#menuLayer'));}
function closeDrawer(){clearLayer($('#drawerLayer'));}
let modalCancel=null;
function closeModal(animate=true){if(bundleJob){toast('转换正在进行。请保持应用打开。');return;}const cancel=modalCancel;modalCancel=null;clearLayer($('#modalLayer'),animate);if(cancel)cancel();}
const heightMotions=new WeakMap();
function expandGroup(el,open){
 const current=el.hidden?0:el.getBoundingClientRect().height,prior=heightMotions.get(el);if(prior){prior.onfinish=null;prior.cancel();}
 el.setAttribute('aria-hidden',String(!open));
 if(!el.animate||!motionEnabled()){el.hidden=!open;return;}
 el.hidden=false;el.style.overflow='hidden';el.style.pointerEvents=open?'':'none';
 const animation=el.animate([{height:current+'px',opacity:open ? .55 : 1},{height:(open?el.scrollHeight:0)+'px',opacity:open?1:0}],{duration:180,easing:'cubic-bezier(.2,0,0,1)'});heightMotions.set(el,animation);
 animation.onfinish=()=>{el.hidden=!open;el.style.overflow='';el.style.pointerEvents='';heightMotions.delete(el);animation.cancel();};
}
document.addEventListener('pointerdown',event=>{
 if(!motionEnabled()||event.isPrimary===false)return;const b=event.target.closest&&event.target.closest('button');if(!b||b.disabled)return;
 const rect=b.getBoundingClientRect(),r=document.createElement('span'),diameter=Math.max(rect.width,rect.height)*2;
 r.className='touchRipple';r.setAttribute('aria-hidden','true');r.style.cssText='width:'+diameter+'px;height:'+diameter+'px;left:'+(event.clientX-rect.left-diameter/2)+'px;top:'+(event.clientY-rect.top-diameter/2)+'px';b.append(r);setTimeout(()=>r.remove(),450);
},{passive:true});
function dialog(title,body,buttons=[],cancel=null){clearLayer($('#menuLayer'),false);closeModal(false);modalCancel=cancel;const layer=$('#modalLayer');layer.innerHTML=`<div class="shade"></div><section class="dialog" role="dialog" aria-modal="true" aria-label="${esc(title)}"><h2>${esc(title)}</h2><div class="dialogBody">${body}</div><div class="actions"></div></section>`;layer.querySelector('.shade').onclick=()=>closeModal();for(const [label,fn]of buttons){const b=document.createElement('button');b.textContent=label;b.onclick=()=>act(fn);layer.querySelector('.actions').append(b);}return layer.querySelector('.dialog');}
function alertBox(title,text){dialog(title,`<p class="break">${esc(text).replace(/\n/g,'<br>')}</p>`,[['确定',closeModal]]);}
function ask(title,value='',label=''){return new Promise(resolve=>{const d=dialog(title,`${label?`<label>${esc(label)}</label>`:''}<input id="promptValue" value="${esc(value)}" autocomplete="off" spellcheck="false">`,[['取消',()=>{modalCancel=null;closeModal();resolve(null);}],['确定',()=>{const v=$('#promptValue').value;modalCancel=null;closeModal();resolve(v);}]],()=>resolve(null));setTimeout(()=>{const i=d.querySelector('input');if(i&&i.isConnected){i.focus();i.select();}},30);});}
function confirmBox(title,text,yes='继续'){return new Promise(resolve=>{const d=dialog(title,`<p>${esc(text)}</p>`,[['取消',()=>{modalCancel=null;closeModal();resolve(false);}], [yes,()=>{modalCancel=null;closeModal();resolve(true);}]],()=>resolve(false));if(/删除|清空|移除|停止|放弃|永久/.test(yes))d.classList.add('dangerDialog');});}
function options(title,rows,grid){const d=dialog(title,'<div class="options"></div>');d.classList.add('optionsDialog');const c=d.querySelector('.options');if(grid)c.classList.add('optionsGrid');for(const row of rows){const b=document.createElement('button');b.className='option';b.disabled=!!row.disabled;b.innerHTML=(row.icon?'<span class="optionIcon">'+icon(row.icon)+'</span>':'')+'<span class="optionLabel">'+esc(row.label)+'</span>'+(row.icon?'':'<span class="optionChevron">'+icon('forward')+'</span>');b.onclick=()=>act(async()=>{if(row.disabled)return;closeModal();await row.run();});c.append(b);}}
function menu(rows){const fromDrawer=!!$('#drawerLayer').children.length,layer=$('#menuLayer');clearLayer(layer,false);layer.innerHTML='<div class="overflowShade"></div><div class="overflowMenu" role="menu"></div>';layer.firstChild.onclick=closeMenu;for(const r of rows){const b=document.createElement('button');b.setAttribute('role',r.checked===undefined?'menuitem':'menuitemcheckbox');if(r.checked!==undefined)b.setAttribute('aria-checked',String(r.checked));b.disabled=!!r.disabled;b.innerHTML=icon(r.icon)+`<span>${esc(r.label)}</span>${r.end?`<span class="end">${esc(r.end)}</span>`:''}`;b.onclick=()=>act(async()=>{closeMenu();if(fromDrawer)closeDrawer();await r.run();});layer.lastChild.append(b);}}
function header(title,subtitle,actions=[],path=false){$('#bottom').classList.remove('editorBottom');const bar=$('#toolbar');bar.replaceChildren(iconButton('menu','打开导航',openDrawer));const t=document.createElement('div');t.className='titleblock';t.innerHTML=`<h1 class="title ${path?'pathTitle':''}">${esc(title)}</h1>${subtitle?`<div class="subtitle">${esc(subtitle)}</div>`:''}`;bar.append(t);for(const a of actions)bar.append(iconButton(a.icon,a.label,a.run));if(path){fitPath(t.querySelector('.title'),title);if(state.screen==='files'){t.classList.add('pathJump');t.setAttribute('role','button');t.setAttribute('aria-label','跳转到路径');t.onclick=()=>jumpDialog();}}}
// 跳转对话框（参考截图）：输入路径 + 粘贴/取消/确定。
function jumpDialog(){const p=activePane();if(p.mode==='recycle')return;const start=(p.mode===''&&!p.archive&&p.path)||'/storage/emulated/0';
 const d=dialog('跳转','<input id="jumpPath" value="'+esc(start)+'" spellcheck="false" autocomplete="off">',[
 ['粘贴',()=>act(async()=>{const t=await rpc('clipboardGet');const i=$('#jumpPath');if(i&&t)i.value=String(t==null?'':t).trim();})],
 ['取消',closeModal],
 ['确定',()=>act(async()=>{const v=($('#jumpPath').value||'').trim();closeModal();if(!v)return;if(!v.startsWith('/'))throw Error('请输入以 / 开头的绝对路径');await navigate(state.active,{path:v,archive:'',entry:''});})]]);
 const input=d.querySelector('input');setTimeout(()=>{if(input&&input.isConnected){input.focus();const pos=input.value.length;try{input.setSelectionRange(pos,pos);}catch(e){}}},40);}
function paneLocation(p){return {path:p.path,archive:p.archive||'',entry:p.entry||'',mode:p.mode||'',net:p.net||null,scrollTop:p.scrollTop||0};}
function fitPath(el,text){el.title=text;el.classList.remove('pathScroll');el.textContent=text;window.requestAnimationFrame(()=>{if(!el.isConnected||el.clientWidth<=0)return;if(el.scrollWidth<=el.clientWidth)return;// 参考 MP-Manager：路径过长时不截断，改为跑马灯滚动完整展示
 const over=el.scrollWidth-el.clientWidth;el.innerHTML='<span class="pathMarquee">'+esc(text)+'</span>';el.style.setProperty('--path-shift',over+'px');el.classList.add('pathScroll');});}
function paneSummary(p){const folders=p.items.filter(x=>x.dir).length;const sizeText=p.mode==='recycle'?'  大小：'+size(p.items.reduce((n,x)=>n+(Number(x.size)||0),0)):'';const remote=p.mode==='network'?'  远程存储':'';return `文件夹：${folders}  文件：${p.items.length-folders}${sizeText}${remote}${p.selected&&p.selected.size?'  已选：'+p.selected.size:''}${p.items[0]&&p.items[0].total?'  磁盘：'+size(p.items[0].total-p.items[0].free)+'/'+size(p.items[0].total):''}`;}
function activePane(){return state.panes[state.active];}
function displayPath(p){if(p.mode==='recycle')return '回收站/';if(p.mode==='network'){const n=p.net&&(p.net.name||networkProtocolLabel(p.net))||'远程存储';const q=String(p.path||'').replace(/^\/+|\/+$/g,'');return n+'/'+(q?q+'/':'');}return p.archive?p.archive.split('/').pop()+'/'+p.entry:p.path.replace(/\/$/,'')+'/';}
async function loadPane(i){const p=state.panes[i];try{if(p.mode==='recycle'){p.items=(await rpc('recycleList')).map(r=>({...r,recycleKey:r.id}));p.selected=new Set();}else if(p.mode==='network'){if(!p.net)throw Error('未选择远程存储');p.items=sortItems(await rpc('netStorage',{op:'list',connection:p.net,path:String(p.path||'')}));p.selected=new Set();}else{p.items=sortItems(await rpc('list',{...paneLocation(p),hidden:pref.hidden}));if(p.selected)p.selected=new Set(p.items.filter(f=>p.selected.has(fileKey(f))).map(fileKey));}p.error='';}catch(e){p.items=[];p.error=e.message;}}
async function refresh(){await Promise.all(state.panes.map((_,i)=>loadPane(i)));if(state.screen==='files')renderFiles();}
async function navigate(i,loc,save=true){const p=state.panes[i];if(save){p.history.push(paneLocation(p));p.future=[];}Object.assign(p,{scrollTop:0,selected:new Set(),appendMode:false,gestureAnchor:null,mode:'',net:null},loc);state.active=i;await loadPane(i);state.screen='files';renderFiles();animateContent($('.pane.active'));if(!loc.archive&&!loc.mode&&!loc.net&&loc.path)recordRecentFolder(loc.path);}
async function up(){const p=activePane();if(p.mode==='recycle'){if(p.history.length)return backPane();return navigate(state.active,{path:state.info.home,mode:''});}if(p.mode==='network'){const q=String(p.path||'').replace(/^\/+|\/+$/g,'');if(!q)return leaveRemote();const parts=q.split('/');parts.pop();return navigate(state.active,{mode:'network',net:p.net,path:parts.join('/')});}if(p.archive){if(p.entry){let arr=p.entry.replace(/\/$/,'').split('/');arr.pop();await navigate(state.active,{...paneLocation(p),entry:arr.length?arr.join('/')+'/':''});}else await navigate(state.active,{path:p.archive.slice(0,p.archive.lastIndexOf('/'))||'/',archive:'',entry:''});}else{const path=p.path.replace(/\/$/,'');await navigate(state.active,{path:path.slice(0,path.lastIndexOf('/'))||'/',archive:'',entry:''});}}
async function backPane(forward=false){const p=activePane(),from=forward?p.future:p.history,to=forward?p.history:p.future;if(!from.length)return;to.push(paneLocation(p));Object.assign(p,from.pop());await loadPane(state.active);renderFiles();animateContent($('.pane.active'),forward?1:-1);}
const imageExtensions=['jpg','jpeg','jpe','jfif','png','gif','webp','bmp','dib','heic','heif','avif','svg','ico','cur','tif','tiff','dng','arw','cr2','crw','nef','nrw','orf','raf','rw2','pef','sr2','srw','x3f','3fr','fff','iiq'];
const videoExtensions=['mp4','m4v','f4v','mkv','mk3d','webm','mov','qt','avi','divx','3gp','3g2','ts','m2ts','mts','m2t','mtv','tp','trp','vro','flv','wmv','asf','mpg','mpeg','mpe','m1v','m2v','vob','ogv','ogm','rm','rmvb','mxf'];
const audioExtensions=['mp3','mp2','mpga','wav','wave','flac','aac','m4a','m4b','ogg','oga','opus','spx','wma','amr','nb','awb','mid','midi','rmi','ape','apl','aiff','aif','aifc','mka','au','snd','caf','weba','ac3','dts','tta','wv'];
function mediaKind(f){const ext=String(f.name||'').split('.').pop().toLowerCase();if(imageExtensions.includes(ext))return 'image';if(videoExtensions.includes(ext))return 'video';if(audioExtensions.includes(ext))return 'audio';return '';}
function fileType(f){if(f.dir)return 'folder';const media=mediaKind(f);if(media)return media;const ext=f.name.split('.').pop().toLowerCase();return ['apk','apks','dex','xml','java','smali','arsc','zip'].includes(ext)?ext:'file';}
const apkIconCache=new Map();const listThumbCache=new Map();let iconObservers=[];
function loadVisibleMediaThumbs(pane){
 if(!window.IntersectionObserver)return;
 const observer=new window.IntersectionObserver(entries=>{for(const entry of entries){if(!entry.isIntersecting)continue;const row=entry.target;observer.unobserve(row);const kind=row.dataset.mediaKind,key=row.dataset.mediaPath+'|'+row.dataset.mediaStamp;const show=data=>{if(!row.isConnected||typeof data!=='string'||!data.startsWith('data:image/'))return;const holder=row.querySelector('.fileicon');if(!holder)return;holder.classList.add('thumbFill');const img=document.createElement('img');img.alt='';img.src=data;img.onerror=()=>{holder.classList.remove('thumbFill');holder.innerHTML=icon(kind);};if(kind==='video')holder.innerHTML='<span class="playBadge">'+icon('play')+'</span>';holder.append(img);};if(!listThumbCache.has(key)){if(listThumbCache.size>400)listThumbCache.delete(listThumbCache.keys().next().value);listThumbCache.set(key,rpc('listThumb',{path:row.dataset.mediaPath,kind}).catch(()=>''));}Promise.resolve(listThumbCache.get(key)).then(show);}},{root:pane,rootMargin:'60px'});
 iconObservers.push(observer);pane.querySelectorAll('[data-media-path]').forEach(row=>observer.observe(row));
}
function loadVisibleApkIcons(pane){
 if(!window.IntersectionObserver)return;
 const observer=new window.IntersectionObserver(entries=>{for(const entry of entries){if(!entry.isIntersecting)continue;const row=entry.target;observer.unobserve(row);const key=row.dataset.apkPath+'|'+row.dataset.apkStamp;const show=data=>{if(!row.isConnected||typeof data!=='string'||!data.startsWith('data:image/'))return;const holder=row.querySelector('.fileicon');const img=document.createElement('img');img.alt='';img.src=data;img.onerror=()=>holder.innerHTML=icon('apk');holder.replaceChildren(img);};if(!apkIconCache.has(key)){if(apkIconCache.size>200)apkIconCache.delete(apkIconCache.keys().next().value);apkIconCache.set(key,rpc('apkIcon',{path:row.dataset.apkPath}).catch(()=>''));}Promise.resolve(apkIconCache.get(key)).then(show);}},{root:pane,rootMargin:'40px'});
 iconObservers.push(observer);pane.querySelectorAll('[data-apk-path]').forEach(row=>observer.observe(row));
}
function renderFiles(){iconObservers.forEach(o=>o.disconnect());iconObservers=[];state.screen='files';const p=activePane();header(displayPath(p),paneSummary(p),[{icon:'more',label:'文件管理器菜单',run:()=>selectedFiles().length?fileActions(selectedFiles()[0]):fileMenu()}],true);
 $('#subbar').innerHTML=state.info&&!state.info.permission?'<div class="permission"><span>正在浏览应用工作区</span><button id="grant">授予文件访问权限</button></div>':'';if($('#grant'))$('#grant').onclick=()=>act(()=>rpc('permission'));
 $('#content').innerHTML='<div class="filepanes"></div>';const panes=$('.filepanes');state.panes.forEach((pane,index)=>{const recycle=pane.mode==='recycle',network=pane.mode==='network';const el=document.createElement('section');el.className='pane'+(state.active===index?' active':'')+(recycle?' recyclePane':'')+(network?' networkPane':'');el.setAttribute('aria-label',(index===0?'左侧':'右侧')+(recycle?'回收站窗格':network?'远程存储窗格':'文件窗格'));el.onscroll=()=>pane.scrollTop=el.scrollTop;el.onpointerdown=()=>activatePane(index);
 if(recycle){if(pane.error){const e=document.createElement('div');e.className='empty';e.textContent=pane.error;el.append(e);}else if(!pane.items.length){const e=document.createElement('div');e.className='empty';e.textContent='回收站是空的';el.append(e);}else for(const g of recycleGroups(pane.items)){const h=document.createElement('div');h.className='recycleGroup';h.textContent=g.label;el.append(h);for(const r of g.items)el.append(recycleRow(r,index));}panes.append(el);el.scrollTop=pane.scrollTop||0;return;}
 if(network){const parent=document.createElement('button');parent.className='file parent';parent.innerHTML=`<span class="fileicon">${icon('folder')}</span><span class="filename">..</span>`;parent.onclick=()=>act(async()=>{state.active=index;await up();});el.append(parent);if(pane.error){const e=document.createElement('div');e.className='empty';e.textContent=pane.error;el.append(e);}else if(!pane.items.length){const e=document.createElement('div');e.className='empty';e.textContent='此远程文件夹为空';el.append(e);}else pane.items.forEach(f=>el.append(networkRow(f,index)));panes.append(el);el.scrollTop=pane.scrollTop||0;return;}
 const parent=document.createElement('button');parent.className='file parent';parent.innerHTML=`<span class="fileicon">${icon('folder')}</span><span class="filename">..</span>`;parent.onclick=()=>act(async()=>{state.active=index;await up();});el.append(parent);
 if(pane.error){const e=document.createElement('div');e.className='empty';e.textContent=pane.error;el.append(e);}else if(!pane.items.length){const e=document.createElement('div');e.className='empty';e.textContent='空文件夹';el.append(e);}pane.items.forEach((f,i)=>{const row=document.createElement('button');row.className='file'+(pane.selected&&pane.selected.has(fileKey(f))?' selected':'')+(pane.appendMode&&pane.gestureAnchor===i?' dragAnchor':'');row.setAttribute('aria-selected',String(!!(pane.selected&&pane.selected.has(fileKey(f)))));row.dataset.index=String(i);const type=fileType(f);if(type==='apk'&&!f.archive){row.dataset.apkPath=f.path;row.dataset.apkStamp=String(f.time)+'|'+f.size;}else if((type==='image'||type==='video')&&!f.archive){row.dataset.mediaPath=f.path;row.dataset.mediaStamp=String(f.time)+'|'+f.size;row.dataset.mediaKind=type;}row.innerHTML=`<span class="fileicon ${type}">${icon(type)}</span><span class="fileinfo"><span class="fileline"><span class="filename">${esc(displayName(f))}</span><span class="filesize">${f.dir?'':size(f.size)}</span></span><span class="filemeta">${date(f.time)}</span></span>`;fileGesture(row,f,index);el.append(row);});panes.append(el);el.scrollTop=pane.scrollTop||0;loadVisibleApkIcons(el);loadVisibleMediaThumbs(el);});
 renderFileBottom();}
function swapPanes(){[state.panes[0],state.panes[1]]=[state.panes[1],state.panes[0]];state.active=1-state.active;renderFiles();}
function activatePane(index){if(state.active===index)return;state.active=index;document.querySelectorAll('.pane').forEach((x,j)=>x.classList.toggle('active',j===index));const pane=state.panes[index];fitPath($('#toolbar .title'),displayPath(pane));$('#toolbar .subtitle').textContent=paneSummary(pane);renderFileBottom();}
function renderFileBottom(){const bottom=$('#bottom'),p=activePane();if(p.mode==='recycle'){bottom.replaceChildren(iconButton('back','返回',()=>backPane()),iconButton('forward','前进',()=>backPane(true)),iconButton('plus','回收站操作',recycleMenu),iconButton('swap','交换窗格',swapPanes),iconButton('up','上级目录',up));if(selectedFiles().length)selectionBar();return;}if(p.mode==='network'){bottom.replaceChildren(iconButton('back','返回',()=>backPane()),iconButton('forward','前进',()=>backPane(true)),iconButton('plus','远程存储操作',remoteMenu),iconButton('swap','交换窗格',swapPanes),iconButton('up','上级目录',up));return;}bottom.replaceChildren(iconButton('back','返回',()=>backPane()),iconButton('forward','前进',()=>backPane(true)),iconButton('plus','新建或粘贴',createMenu),iconButton('swap','交换窗格',swapPanes),iconButton('up','上级目录',up));if(selectedFiles().length)selectionBar();}

function fileArgs(f){return f.archive?{archive:f.archive,entry:f.entry,path:f.archive}:{path:f.path};}
async function openFile(f,index=state.active){if(f.dir)return navigate(index,f.archive?{path:activePane().path,archive:f.archive,entry:f.entry}:{path:f.path,archive:'',entry:''});if(!f.archive&&!f.recycleKey)recordRecentFile(f);const media=mediaKind(f);if(media==='image')return openPhoto(f,index);if(media==='video'||media==='audio')return playMedia(f,media,index);const type=fileType(f);if(type==='apks')return apksOptions(f,index);if(type==='apk')return showApk(fileArgs(f));if(type==='zip'||/\.(7z|tar|tar\.(gz|xz|zst|bz2|lz4)|gz|gzip)$/i.test(f.name))return navigate(index,{path:activePane().path,archive:f.path,entry:''});if(/\.001$/i.test(f.name))return extractDialog(f);if(type==='dex')return dexFileOptions(f);if(type==='arsc')return arscOpenWith(fileArgs(f),f.name);if(type==='xml')return options('打开方式…',[{label:'反编译',run:()=>openText(fileArgs(f))},{label:'字符串池',run:()=>showPool(fileArgs(f),f.name)}]);return openText(fileArgs(f));}
let photoState=null,photoLoadToken=0,photoKeyHandler=null;
const mediaThumbCache=new Map(),mediaPhotoCache=new Map(),mediaPhotoInfo=new Map();
function photoGalleryItems(paneIndex=state.active){const pane=state.panes[paneIndex]||activePane();if(pane.mode==='network'||pane.archive)return [];return pane.items.filter(f=>!f.dir&&mediaKind(f)==='image');}
function mediaUnavailable(f,label){const p=activePane(),remote=p.mode==='network',archive=!!f.archive||!!p.archive;const buttons=[];if(archive)buttons.push(['解压到…',()=>{closeModal();extractDialog(f);}]);buttons.push(['关闭',closeModal]);dialog('无法预览',`<p class="break">该文件位于${remote?'远程存储':'压缩包'}中，暂不支持内置${label}预览。${remote?'请先下载到本地文件夹。':'请先解压到本地文件夹。'}</p>`,buttons);}
function mediaLayer(){let layer=$('#mediaLayer');if(!layer){layer=document.createElement('div');layer.id='mediaLayer';layer.className='mediaLayer';layer.addEventListener('contextmenu',e=>e.preventDefault());document.body.append(layer);}return layer;}
function openPhoto(f,paneIndex=state.active,itemsArg=null){const pane=state.panes[paneIndex]||activePane();if(pane.mode==='network'||f.archive||pane.archive)return mediaUnavailable(f,'图片');closePlayer();let items=itemsArg||photoGalleryItems(paneIndex);if(!items.length||!items.some(x=>fileKey(x)===fileKey(f)))items=[f,...items];const index=items.findIndex(x=>fileKey(x)===fileKey(f));photoState={items,index:index<0?0:index};if(photoKeyHandler)document.removeEventListener('keydown',photoKeyHandler);photoKeyHandler=e=>{if(!photoState)return;if(e.key==='Escape'){e.preventDefault();closePhoto();}else if(e.key==='ArrowLeft'){e.preventDefault();movePhoto(-1);}else if(e.key==='ArrowRight'){e.preventDefault();movePhoto(1);}};document.addEventListener('keydown',photoKeyHandler);renderPhoto();}
function renderPhoto(){const s=photoState;if(!s)return;const item=s.items[s.index],layer=mediaLayer(),many=s.items.length>1;layer.classList.add('open');layer.innerHTML=`<div class="photoTop"><button class="photoBtn" data-photo-close aria-label="关闭">${icon('close')}</button><div class="photoHead"><div class="photoTitle">${esc(item.name)}</div><div class="photoSub">${s.index+1} / ${s.items.length}</div></div><div class="photoTopActions"><button class="photoBtn" data-photo-info aria-label="图片信息">${icon('info')}</button></div></div><div class="photoStage" data-photo-stage><div class="photoLoading">正在加载…</div><img class="photoImage" alt="${esc(item.name)}"><button class="photoArrow prev" data-photo-prev aria-label="上一张" ${s.index<=0?'disabled':''}>‹</button><button class="photoArrow next" data-photo-next aria-label="下一张" ${s.index>=s.items.length-1?'disabled':''}>›</button></div><div class="photoFoot">${many?'左右滑动或使用方向键切换 · 双击放大':'双击图片放大或缩小'}</div>`;bindPhoto();loadPhoto(item);}
function bindPhoto(){const layer=$('#mediaLayer');if(!layer)return;layer.querySelector('[data-photo-close]').onclick=closePhoto;layer.querySelector('[data-photo-info]').onclick=()=>photoInfo(photoState.items[photoState.index]);layer.querySelector('[data-photo-prev]').onclick=()=>movePhoto(-1);layer.querySelector('[data-photo-next]').onclick=()=>movePhoto(1);const stage=layer.querySelector('[data-photo-stage]');let startX=0,startY=0,down=false;stage.addEventListener('pointerdown',e=>{down=true;startX=e.clientX;startY=e.clientY;});stage.addEventListener('pointerup',e=>{if(!down)return;down=false;const dx=e.clientX-startX,dy=e.clientY-startY;if(Math.abs(dx)>45&&Math.abs(dx)>Math.abs(dy)*1.4)movePhoto(dx>0?-1:1);});stage.addEventListener('pointercancel',()=>{down=false;});const img=layer.querySelector('.photoImage');let lastTap=0;stage.addEventListener('click',e=>{if(e.target.closest('.photoArrow')||e.target.closest('.photoBtn'))return;const now=Date.now();if(now-lastTap<320){img.classList.toggle('zoomed');if(img.classList.contains('zoomed')){const rect=stage.getBoundingClientRect();img.style.transformOrigin=`${((e.clientX-rect.left)/rect.width)*100}% ${((e.clientY-rect.top)/rect.height)*100}%`;}}lastTap=now;});}
function loadPhoto(item){const layer=$('#mediaLayer');if(!layer)return;const img=layer.querySelector('.photoImage'),loading=layer.querySelector('.photoLoading');if(!img||!loading)return;const key=fileKey(item),token=++photoLoadToken;img.classList.remove('zoomed');img.removeAttribute('src');loading.style.display='block';loading.textContent='正在加载…';const reveal=src=>{if(token!==photoLoadToken)return;img.onload=()=>{if(token===photoLoadToken)loading.style.display='none';};img.onerror=()=>{if(token===photoLoadToken){loading.style.display='block';loading.textContent='无法显示此图片';}};img.src=src;};const cached=mediaPhotoCache.get(key);if(cached)return reveal(cached);rpc('mediaImage',{path:item.path,max:2048}).then(data=>{mediaPhotoInfo.set(key,data);mediaPhotoCache.set(key,data.data);if(mediaPhotoCache.size>4)mediaPhotoCache.delete(mediaPhotoCache.keys().next().value);if(photoState&&fileKey(photoState.items[photoState.index])===key)reveal(data.data);}).catch(e=>{if(token!==photoLoadToken)return;loading.textContent=e.message||'无法加载图片';});}
function movePhoto(delta){const s=photoState;if(!s)return;const next=s.index+delta;if(next<0||next>=s.items.length)return;s.index=next;renderPhoto();}
function closePhoto(){const layer=$('#mediaLayer');photoState=null;photoLoadToken++;if(photoKeyHandler){document.removeEventListener('keydown',photoKeyHandler);photoKeyHandler=null;}if(layer){layer.classList.remove('open');layer.replaceChildren();}}
function photoInfo(item){const key=fileKey(item),data=mediaPhotoInfo.get(key);dialog('图片信息',`<p class="break">${esc(item.name)}</p><p>尺寸：${data&&data.width?data.width+' × '+data.height:'—'}<br>大小：${size(Number(item.size)||0)}<br>修改时间：${date(Number(item.time)||0)}</p><p class="break muted">${esc(item.path)}</p>`,[['复制路径',()=>act(()=>rpc('clipboard',{text:item.path}).then(()=>{closeModal();toast('已复制路径');}))],['关闭',closeModal]]);}
// 内置媒体播放器：视频支持长按倍速、右半屏上下滑调音量、左半屏上下滑调亮度；音频同步内置播放。
let playerState=null,playerKeyHandler=null,playerLoadToken=0,playerHideTimer=0;
function playMedia(f,kind,paneIndex=state.active){const pane=state.panes[paneIndex]||activePane();if(pane.mode==='network'||f.archive||pane.archive)return mediaUnavailable(f,kind==='video'?'视频':'音频');if(kind==='video'){closePhoto();closePlayer();rpc('playVideo',{path:f.path,name:f.name}).catch(err=>{toast('无法播放：'+err);});return;}closePhoto();closePlayer();playerState={f,kind:kind==='video'?'video':'audio',volume:Math.max(0,Math.min(1,Number(pref.playerVolume==null?1:pref.playerVolume))),brightness:Math.max(.15,Math.min(1,Number(pref.playerBrightness==null?1:pref.playerBrightness)||1)),boostRate:0,scrubbing:false,fallback:false,controls:true};if(playerKeyHandler)document.removeEventListener('keydown',playerKeyHandler);playerKeyHandler=e=>{if(!playerState)return;if(e.key==='Escape'){e.preventDefault();closePlayer();}else if(e.key===' '){e.preventDefault();togglePlayerPlay();}else if(e.key==='ArrowLeft'||e.key==='ArrowRight'){e.preventDefault();const el=$('#mediaLayer .playerMedia');if(el&&isFinite(el.duration)&&el.duration>0)el.currentTime=Math.max(0,Math.min(el.duration,el.currentTime+(e.key==='ArrowRight'?10:-10)));}};document.addEventListener('keydown',playerKeyHandler);renderPlayer();}
function renderPlayer(){const s=playerState;if(!s)return;const layer=mediaLayer(),video=s.kind==='video';layer.classList.add('open');layer.innerHTML=`<div class="photoTop"><button class="photoBtn" data-player-close aria-label="关闭">${icon('close')}</button><div class="photoHead"><div class="photoTitle">${esc(s.f.name)}</div><div class="photoSub">内置${video?'视频':'音频'}播放器</div></div><div class="photoTopActions"></div></div><div class="playerStage" data-player-stage>${video?'':'<div class="playerArt" aria-hidden="true">♪</div>'}<video class="playerMedia" playsinline preload="metadata"></video><div class="playerDim" data-player-dim></div><div class="playerBoost" data-player-boost hidden>2× 快进中</div><div class="playerSide" data-player-side hidden></div><div class="playerLoading">正在打开…</div></div><div class="playerControls" data-player-controls><button class="photoBtn" data-player-play aria-label="播放或暂停">${icon('play')}</button><span class="playerTime" data-player-time>00:00</span><input class="playerSeek" type="range" min="0" max="1000" step="1" value="0" aria-label="播放进度"><span class="playerTime" data-player-duration>--:--</span></div><div class="photoFoot" data-player-foot>${video?'长按屏幕 2×/3× 快进 · 左半屏上下滑调亮度 · 右半屏上下滑调音量 · 轻点显示或隐藏控制':'轻点屏幕播放或暂停 · 拖动进度条跳转'}</div>`;bindPlayer();loadPlayerMedia();}
function bindPlayer(){const layer=$('#mediaLayer');if(!layer)return;layer.querySelector('[data-player-close]').onclick=closePlayer;layer.querySelector('[data-player-play]').onclick=togglePlayerPlay;const el=layer.querySelector('.playerMedia'),seek=layer.querySelector('.playerSeek'),stage=layer.querySelector('[data-player-stage]'),dim=layer.querySelector('[data-player-dim]'),boost=layer.querySelector('[data-player-boost]'),side=layer.querySelector('[data-player-side]'),loading=layer.querySelector('.playerLoading');if(!el||!seek||!stage)return;const s=playerState;if(dim&&s)dim.style.opacity=String((1-s.brightness)*.85);if(s)el.volume=s.volume;
 el.addEventListener('timeupdate',()=>{if(!playerState||playerState.scrubbing)return;const d=el.duration;if(isFinite(d)&&d>0)seek.value=String(Math.round(el.currentTime/d*1000));updatePlayerTime();});
 el.addEventListener('loadedmetadata',()=>{hidePlayerLoading();updatePlayerTime();});
 el.addEventListener('canplay',hidePlayerLoading);el.addEventListener('playing',hidePlayerLoading);
 el.addEventListener('play',()=>{syncPlayerButton();schedulePlayerHide();});el.addEventListener('pause',()=>{syncPlayerButton();showPlayerControls(true);});el.addEventListener('ended',syncPlayerButton);
 seek.addEventListener('pointerdown',()=>{if(playerState)playerState.scrubbing=true;});
 seek.addEventListener('change',()=>{const d=el.duration;if(isFinite(d)&&d>0)el.currentTime=Number(seek.value)/1000*d;if(playerState)playerState.scrubbing=false;});
 let sideTimer=0;const showSide=(name,value)=>{clearTimeout(sideTimer);if(side){side.hidden=false;side.innerHTML=icon(name)+'<span class="playerSideTrack"><i style="width:'+Math.max(2,Math.round(value*100))+'%"></i></span><b>'+Math.round(value*100)+'%</b>';}sideTimer=setTimeout(()=>{if(side)side.hidden=true;},900);};
 const setVolume=v=>{v=Math.max(0,Math.min(1,v));if(playerState)playerState.volume=v;el.volume=v;showSide('audio',v);};
 const setBrightness=b=>{b=Math.max(.15,Math.min(1,b));if(playerState)playerState.brightness=b;if(dim)dim.style.opacity=String((1-b)*.85);showSide('sun',b);};
 let lastRate=0,lastRelease=0;const beginBoost=()=>{if(!playerState||playerState.kind!=='video')return;const rate=Date.now()-lastRelease<2500?(lastRate===2?3:2):2;lastRate=rate;playerState.boostRate=rate;try{el.playbackRate=rate;}catch(e){}if(boost){boost.hidden=false;boost.textContent=rate+'× 快进中';}};const endBoost=()=>{if(!playerState||!playerState.boostRate)return;playerState.boostRate=0;try{el.playbackRate=1;}catch(e){}if(boost)boost.hidden=true;lastRelease=Date.now();};
 let down=false,startX=0,startY=0,mode='';let pressTimer=0;
 stage.addEventListener('pointerdown',e=>{if(e.target.closest('.playerControls')||e.target.closest('input'))return;e.preventDefault();down=true;startX=e.clientX;startY=e.clientY;mode='';clearTimeout(pressTimer);pressTimer=setTimeout(beginBoost,420);});
 stage.addEventListener('pointermove',e=>{if(!down)return;const dx=e.clientX-startX,dy=e.clientY-startY;if(mode===''&&(Math.abs(dx)>16||Math.abs(dy)>16)){clearTimeout(pressTimer);mode=Math.abs(dy)>Math.abs(dx)?'swipe':'pan';}
  if(mode==='swipe'&&playerState&&playerState.kind==='video'){const rect=stage.getBoundingClientRect();const step=(e.clientY-startY)/(rect.height||400);startX=e.clientX;startY=e.clientY;const right=e.clientX>rect.left+rect.width/2;if(right)setVolume(playerState.volume-step);else setBrightness(playerState.brightness-step);}});
 const finish=()=>{clearTimeout(pressTimer);if(playerState&&playerState.boostRate){endBoost();down=false;return;}if(down&&mode===''){if(playerState&&playerState.kind==='audio')togglePlayerPlay();else togglePlayerControls();}down=false;};
 stage.addEventListener('pointerup',finish);stage.addEventListener('pointercancel',()=>{clearTimeout(pressTimer);if(playerState&&playerState.boostRate)endBoost();down=false;});}
function hidePlayerLoading(){const l=document.querySelector('#mediaLayer .playerLoading');if(l)l.style.display='none';}
function loadPlayerMedia(){const s=playerState;if(!s)return;const layer=$('#mediaLayer');if(!layer)return;const el=layer.querySelector('.playerMedia'),loading=layer.querySelector('.playerLoading');if(!el)return;const token=++playerLoadToken;
 act(async()=>{let info;try{info=await rpc('mediaStream',{path:s.f.path});}catch(e){if(token===playerLoadToken&&loading)loading.textContent=e.message||'无法打开媒体';return;}
  if(token!==playerLoadToken)return;
  el.onerror=()=>{if(token!==playerLoadToken||!playerState||playerState.fallback)return;playerState.fallback=true;closePlayer();toast('内置播放器不支持此格式，改用系统播放器…');act(()=>rpc('mediaPlay',{path:s.f.path,kind:s.kind,name:s.f.name}));};
  el.src=info.url;el.volume=s.volume;const p=el.play();if(p&&p.catch)p.catch(()=>{});
  // 防卡死：部分 WebView/编码下 loadedmetadata 与 error 都不触发，界面会永久停在「正在打开…」。
  const watchdog=setTimeout(()=>{if(token!==playerLoadToken||!playerState||playerState.fallback)return;if(el.readyState>=1)return;if(!loading)return;loading.innerHTML='打开超时，可能是编码不支持。<button type="button" class="playerFallbackBtn">用系统播放器打开</button>';const b=loading.querySelector('.playerFallbackBtn');if(b)b.onclick=()=>{playerState.fallback=true;closePlayer();act(()=>rpc('mediaPlay',{path:s.f.path,kind:s.kind,name:s.f.name}));};},12000);
  const autoFallback=setTimeout(()=>{if(token!==playerLoadToken||!playerState||playerState.fallback)return;if(el.readyState>=1)return;playerState.fallback=true;closePlayer();toast('内置播放器打开超时，改用系统播放器…');act(()=>rpc('mediaPlay',{path:s.f.path,kind:s.kind,name:s.f.name}));},30000);
  el.addEventListener('loadedmetadata',()=>{clearTimeout(watchdog);clearTimeout(autoFallback);},{once:true});
  el.addEventListener('canplay',()=>{clearTimeout(watchdog);clearTimeout(autoFallback);},{once:true});});}
function togglePlayerPlay(){const layer=$('#mediaLayer');if(!layer)return;const el=layer.querySelector('.playerMedia');if(!el)return;const p=el.paused?el.play():el.pause();if(p&&p.catch)p.catch(()=>{});syncPlayerButton();}
function syncPlayerButton(){const layer=$('#mediaLayer');if(!layer)return;const el=layer.querySelector('.playerMedia'),b=layer.querySelector('[data-player-play]');if(!el||!b)return;b.innerHTML=icon(el.paused?'play':'pause');schedulePlayerHide();}
function showPlayerControls(show){if(!playerState)return;playerState.controls=!!show;const layer=$('#mediaLayer');if(!layer)return;const c=layer.querySelector('[data-player-controls]'),f=layer.querySelector('[data-player-foot]');if(c)c.classList.toggle('playerHide',!show);if(f)f.classList.toggle('playerHide',!show);}
function togglePlayerControls(){if(!playerState)return;showPlayerControls(!playerState.controls);if(playerState.controls)schedulePlayerHide();}
function schedulePlayerHide(){clearTimeout(playerHideTimer);if(!playerState||playerState.kind!=='video'||!playerState.controls)return;playerHideTimer=setTimeout(()=>{const layer=$('#mediaLayer');const el=layer&&layer.querySelector('.playerMedia');if(playerState&&playerState.kind==='video'&&el&&!el.paused)showPlayerControls(false);},4000);}
function updatePlayerTime(){const layer=$('#mediaLayer');if(!layer)return;const el=layer.querySelector('.playerMedia'),t=layer.querySelector('[data-player-time]'),d=layer.querySelector('[data-player-duration]');if(!el||!t||!d)return;if(isFinite(el.duration)&&el.duration>0){t.textContent=playerClock(el.currentTime);d.textContent=playerClock(el.duration);}else{t.textContent='直播';d.textContent='—';}}
function playerClock(t){if(!isFinite(t)||t<0)t=0;const total=Math.floor(t),h=Math.floor(total/3600),m=Math.floor(total%3600/60),s=total%60,p=n=>String(n).padStart(2,'0');return h>0?h+':'+p(m)+':'+p(s):p(m)+':'+p(s);}
function closePlayer(){const layer=$('#mediaLayer');if(playerKeyHandler){document.removeEventListener('keydown',playerKeyHandler);playerKeyHandler=null;}clearTimeout(playerHideTimer);if(playerState){pref.playerVolume=playerState.volume;pref.playerBrightness=playerState.brightness;}playerState=null;playerLoadToken++;if(layer){layer.classList.remove('open');layer.replaceChildren();}}
const HEX_PAGE=4096;
async function hexScreen(f){
 state.hex={args:fileArgs(f),name:f.name,offset:0,size:0};
 const body=toolPage('十六进制查看',f.name);
 body.innerHTML='<div class="hexBar"><button id="hexFirst" class="hexNav">首页</button><button id="hexPrev" class="hexNav">上一页</button><span id="hexPos" class="hexPos muted"></span><button id="hexNext" class="hexNav">下一页</button><button id="hexEnd" class="hexNav">末页</button></div><div class="searchline"><input id="hexJump" placeholder="跳转到偏移（支持 0x 十六进制或十进制）" aria-label="跳转到偏移"></div><pre id="hexBody" class="hexBody"></pre>';
 const jump=v=>act(async()=>{const raw=String(v).trim();if(!raw)return;let n=/^0x/i.test(raw)?parseInt(raw.substring(2),16):parseInt(raw,10);if(!Number.isFinite(n)||n<0)return toast('请输入有效的偏移');if(state.hex.size&&n>=state.hex.size)n=state.hex.size-1;await hexLoad(n);});
 $('#hexFirst').onclick=()=>act(()=>hexLoad(0));
 $('#hexPrev').onclick=()=>act(()=>hexLoad(Math.max(0,state.hex.offset-HEX_PAGE)));
 $('#hexNext').onclick=()=>act(()=>hexLoad(state.hex.offset+HEX_PAGE>=state.hex.size?state.hex.offset:state.hex.offset+HEX_PAGE));
 $('#hexEnd').onclick=()=>act(async()=>{if(state.hex.size)await hexLoad(Math.floor((state.hex.size-1)/HEX_PAGE)*HEX_PAGE);});
 $('#hexJump').onkeydown=e=>{if(e.key==='Enter'){jump(e.target.value);e.target.value='';}};
 await hexLoad(0);
}
async function hexLoad(offset){
 const h=state.hex;if(!h)return;const r=await rpc('hexView',{...h.args,offset:Math.max(0,offset|0),length:HEX_PAGE});
 h.offset=r.offset;h.size=r.size||r.length;
 const perRow=window.innerWidth>=620?16:8,groups=[];
 const lines=[];for(let i=0;i<r.length;i+=perRow){const addr=(r.offset+i).toString(16).padStart(8,'0');const hex=r.hex.substring(i*2,(i+perRow)*2);groups.length=0;for(let g=0;g<perRow;g+=8)groups.push(hex.substring(g*2,(g+8)*2).replace(/(..)/g,'$1 ').trim());lines.push(addr+'  '+groups.join('  ').padEnd(perRow===16?48:23)+'  |'+r.ascii.substring(i,i+perRow).padEnd(perRow)+'|');}
 $('#hexBody').textContent=lines.join('\n')||'（空文件）';
 $('#hexPos').textContent=h.size?size(h.size)+' · 0x'+h.offset.toString(16).toUpperCase():'空文件';
}
function openWithBuiltinItems(f){return [
 {label:'编辑文本',color:'#3f6fb5',glyph:icon('edit'),test:()=>true,run:()=>openText(fileArgs(f))},
 {label:'浏览压缩包',color:'#8d6e4f',glyph:icon('zip'),test:()=>fileType(f)==='zip'||/\.(7z|tar|tar\.(gz|xz|zst|bz2|lz4)|gz|gzip|001)$/i.test(f.name),run:async()=>navigate(state.active,{path:activePane().path,archive:await rpc('resolve',fileArgs(f)),entry:''})},
 {label:'查看图片',color:'#2e8b83',glyph:icon('photo'),test:()=>mediaKind(f)==='image',run:()=>openPhoto(f)},
 {label:'十六进制',color:'#5c7080',glyph:'<span class="owGlyph">01</span>',test:()=>true,run:()=>hexScreen(f)},
 {label:'播放音乐',color:'#b0433f',glyph:icon('audio'),test:()=>mediaKind(f)==='audio',run:()=>playMedia(f,'audio')},
 {label:'播放视频',color:'#d07a2e',glyph:icon('video'),test:()=>mediaKind(f)==='video',run:()=>playMedia(f,'video')},
 {label:'XML 功能',color:'#3f6fb5',glyph:'<span class="owGlyph">&lt;/&gt;</span>',test:()=>fileType(f)==='xml',run:()=>openText(fileArgs(f))},
 {label:'DEX 功能',color:'#2e8b83',glyph:'<span class="owGlyph">01</span>',test:()=>fileType(f)==='dex',run:()=>chooseDex(f.archive?{path:f.archive}:fileArgs(f))},
 {label:'ARSC 功能',color:'#d07a2e',glyph:icon('arsc'),test:()=>fileType(f)==='arsc',run:()=>arscOpenWith(fileArgs(f),f.name)},
 {label:'APK 信息',color:'#5a9b4e',glyph:icon('apk'),test:()=>fileType(f)==='apk',run:()=>showApk(fileArgs(f))},
 {label:'APKS 功能',color:'#6f5d8a',glyph:icon('zip'),test:()=>fileType(f)==='apks',run:()=>apksOptions(f,state.active)},
 {label:'Linux 脚本',color:'#4a6da7',glyph:icon('terminal'),test:()=>/\.(sh|bash)$/i.test(f.name)&&!f.archive,run:()=>rpc('terminalOpen',{path:activePane().path,command:'sh '+JSON.stringify(f.path)})},
];}
async function openWithDialog(f,index=state.active){
 if(!f||f.dir)return;
 const media=mediaKind(f),network=activePane().mode==='network';
 const tabs=[{key:'builtin',label:'内置'},{key:'suggest',label:'系统建议'},{key:'text',label:'文本'}];
 if(media==='image')tabs.push({key:'image',label:'图片'});
 if(media==='video')tabs.push({key:'video',label:'视频'});
 if(media==='audio')tabs.push({key:'audio',label:'音频'});
 if(network)tabs.length=1;
 const d=dialog('打开方式…','<div class="owSearch" id="owSearch" hidden><input id="owSearchInput" placeholder="搜索打开方式…" aria-label="搜索打开方式"></div><div class="tabs owTabs" id="owTabs" role="tablist"></div><div class="owGrid" id="owGrid"></div><p class="owEmpty" id="owEmpty" hidden></p><p class="bundleHint owHint">「内置」为自带功能；其他标签列出可打开此文件的外部应用。</p>',[['关闭',closeModal]]);
 d.classList.add('owDialog');
 const head=document.createElement('span');head.className='owHeadBtns';
 const searchBtn=iconButton('search','搜索打开方式',()=>{const row=$('#owSearch');if(!row)return;row.hidden=!row.hidden;const input=$('#owSearchInput');if(input){if(!row.hidden){input.value=query;input.focus();}else{query='';input.value='';}}render();});
 const helpBtn=iconButton('info','打开方式说明',()=>alertBox('打开方式','「内置」页列出本应用自带的功能：编辑文本、浏览压缩包、查看图片、十六进制、播放音乐 / 视频、XML / DEX / ARSC / APK / APKS 功能与 Linux 脚本；灰色项表示与当前文件类型不匹配。\n\n「系统建议」按文件的真实类型列出手机上可打开它的应用；「文本 / 图片 / 视频 / 音频」页按 MIME 类型筛选外部应用，轻点任意图标即用所选方式打开。\n\n右上角放大镜可按名称搜索当前页；远程存储中的文件请先下载到本地再使用外部应用打开。'));
 head.append(searchBtn,helpBtn);
 const title=d.querySelector('h2');
 if(title){const label=document.createElement('span');label.className='owTitle';label.textContent=title.textContent;title.replaceChildren(label);title.append(head);}
 let current='builtin',query='',apps=null,resolved=null;
 const builtin=openWithBuiltinItems(f);
 const loadApps=async()=>{if(apps)return apps;if(network)return apps={};if(resolved===null)resolved=await rpc('resolve',fileArgs(f));apps=await rpc('openWithApps',{path:resolved});return apps;};
 function render(){
  const grid=$('#owGrid');if(!grid)return;grid.replaceChildren();
  const empty=$('#owEmpty');let items=[];
  const q=query.toLowerCase();
  if(current==='builtin'){items=builtin.filter(x=>x.label.toLowerCase().includes(q)).map(x=>({label:x.label,disabled:!x.test(),html:'<span class="owTile" style="background:'+x.color+'">'+x.glyph+'</span><span class="owLabel">'+esc(x.label)+'</span>',run:x.run}));}
  else{const list=(apps&&apps[current])||[];items=list.filter(x=>String(x.label).toLowerCase().includes(q)).map(x=>({label:x.label,disabled:false,html:'<span class="owTile owAppTile">'+(x.icon?'<img alt="" src="data:image/png;base64,'+x.icon+'">':'<span class="owGlyph">'+esc(String(x.label).charAt(0))+'</span>')+'</span><span class="owLabel">'+esc(x.label)+'</span>',run:async()=>{await rpc('openWithLaunch',{path:resolved,pkg:x.pkg,cls:x.cls,mime:(apps&&apps.mime)||'application/octet-stream'});toast('已交给 '+x.label+' 打开');}}));}
  for(const item of items){const b=document.createElement('button');b.type='button';b.className='owItem';b.disabled=!!item.disabled;b.setAttribute('aria-label',item.label);b.innerHTML=item.html;b.onclick=()=>act(async()=>{if(item.disabled)return;closeModal();await item.run();});grid.append(b);}
  if(empty){if(!items.length){empty.hidden=false;empty.textContent=current==='builtin'?'没有匹配的内置功能。':(network?'远程文件请先下载到本地，再使用外部应用打开。':'没有匹配的应用。');}else empty.hidden=true;}
 }
 function drawTabs(){const bar=$('#owTabs');if(!bar)return;bar.replaceChildren();for(const t of tabs){const b=document.createElement('button');b.type='button';b.className='tab'+(current===t.key?' active':'');b.setAttribute('role','tab');b.setAttribute('aria-selected',String(current===t.key));b.textContent=t.label;b.onclick=()=>act(async()=>{if(current===t.key)return;current=t.key;if(current!=='builtin')await loadApps();drawTabs();render();});bar.append(b);}}
 const searchInput=d.querySelector('#owSearchInput');
 if(searchInput)searchInput.oninput=e=>{query=e.target.value.trim();render();};
 drawTabs();render();
}
function galleryPage(){const p=activePane();if(p.mode==='network'||p.archive){alertBox('照片浏览','内置照片浏览仅支持本地文件夹，请先下载或解压。');return;}const images=photoGalleryItems();if(!images.length){alertBox('照片浏览','当前文件夹没有图片。');return;}const body=toolPage('照片浏览',`${images.length} 张图片 · ${p.path}`);body.innerHTML=`<div class="photoGrid">${images.map((f,i)=>`<button class="photoTile" data-photo-index="${i}"><span class="photoThumb"><span class="photoSpinner">…</span></span><span class="photoCaption">${esc(displayName(f))}</span></button>`).join('')}</div>`;const tiles=Array.from(body.querySelectorAll('[data-photo-index]'));tiles.forEach(b=>b.onclick=()=>openPhoto(images[Number(b.dataset.photoIndex)],state.active,images));loadGalleryThumbs(tiles,images);}
function loadGalleryThumbs(tiles,images){const queue=[];let active=0;const show=(tile,src)=>{if(!tile.isConnected)return;const holder=tile.querySelector('.photoThumb');if(!holder)return;const img=document.createElement('img');img.alt='';img.src=src;holder.replaceChildren(img);};async function loadTile(i){const tile=tiles[i],f=images[i];if(!tile||!f)return;const key=fileKey(f);if(mediaThumbCache.has(key))return show(tile,mediaThumbCache.get(key));try{const data=await rpc('mediaImage',{path:f.path,thumb:true,max:360});mediaThumbCache.set(key,data.data);if(mediaThumbCache.size>150)mediaThumbCache.delete(mediaThumbCache.keys().next().value);show(tile,data.data);}catch(e){const holder=tile.querySelector('.photoThumb');if(holder)holder.innerHTML='<span class="photoSpinner">!</span>';}}const pump=()=>{while(active<3&&queue.length){const i=queue.shift();active++;loadTile(i).finally(()=>{active--;pump();});}};const observe=i=>{queue.push(i);pump();};if('IntersectionObserver' in window){const io=new IntersectionObserver(entries=>{for(const entry of entries)if(entry.isIntersecting){io.unobserve(entry.target);observe(Number(entry.target.dataset.photoIndex));}},{rootMargin:'320px'});tiles.forEach(t=>io.observe(t));}else tiles.forEach((t,i)=>observe(i));}
function fileMenu(){const p=activePane();if(p.mode==='network')return menu([{icon:'refresh',label:'刷新',run:remoteRefresh},{icon:'plus',label:'新建文件夹',run:remoteMkdir},{icon:'export',label:'上传文件',run:remoteUpload},{icon:'settings',label:'连接设置',run:remoteSettings},{icon:'back',label:'离开远程存储',run:leaveRemote}]);if(p.mode==='recycle'){const sel=selectedFiles();if(sel.length)return menu([{icon:'undo',label:'还原 '+sel.length+' 项',run:()=>restoreRecycle(sel)},{icon:'trash',label:'永久删除 '+sel.length+' 项',run:()=>purgeRecycle(sel)},{icon:'close',label:'清除选择',run:clearSelection}]);return menu([{icon:'refresh',label:'刷新',run:async()=>{await loadPane(state.active);renderFiles();}},{icon:'trash',label:'清空回收站',run:emptyRecycle},{icon:'back',label:'返回上一页',run:()=>backPane()},{icon:'close',label:'离开回收站',run:async()=>{if(p.history.length)return backPane();await navigate(state.active,{path:state.info.home,mode:''});}}]);}menu([{icon:'network',label:'添加远程存储',run:addNetworkStorage},{icon:'check',label:'全选',run:()=>{activePane().selected=new Set(activePane().items.map(fileKey));renderFiles();}},{icon:'gallery',label:'照片浏览',run:galleryPage},{icon:'copy',label:'书签',run:bookmarks},{icon:'refresh',label:'刷新',run:refresh},{icon:'search',label:'筛选当前文件夹',run:async()=>{const q=await ask('查找文件','','文件名包含');if(q===null)return;await loadPane(state.active);activePane().items=activePane().items.filter(f=>f.name.toLowerCase().includes(q.toLowerCase()));renderFiles();}},{icon:'compass',label:'跳转到路径',run:()=>jumpDialog()},{icon:'check',label:'显示隐藏文件',end:pref.hidden?'已显示':'',run:async()=>{pref.hidden=!pref.hidden;savePref();await refresh();}},{icon:'settings',label:'设置',run:preferences},{icon:'info',label:'关于',run:about}]);}
function createMenu(){if(activePane().mode==='recycle'||activePane().mode==='network')return;const rows=[];if(!activePane().archive){rows.push({label:'新建文件夹',icon:'folder',run:()=>createFile(true)},{label:'新建文本文件',icon:'text',run:()=>createFile(false)});if(state.clipboard)rows.push({label:`粘贴 ${state.clipboard.name}`,icon:'copy',run:paste});}if(!rows.length)return toast('请先解压归档，才能在文件夹内创建文件。');options('新建 / 粘贴',rows);}
async function createFile(folder){const name=await ask(folder?'新建文件夹':'新建文件',folder?'新建文件夹':'未命名.txt');if(!name)return;const path=await rpc('create',{path:activePane().path,name,folder});await refresh();if(!folder)await openText({path});}
async function paste(){const c=state.clipboard;if(c.files)return transferFiles(c.files,c.move,activePane().path);const path=await rpc(c.move?'move':'copy',{...c.args,destination:activePane().path});state.clipboard=null;await refresh();toast('已保存 '+path);}
function fileKey(f){if(f.recycleKey)return f.recycleKey;return f.archive?f.archive+'!/'+f.entry:f.path;}
function selectedFiles(){const p=activePane();if(!p.selected)p.selected=new Set();return p.items.filter(f=>p.selected.has(fileKey(f)));}
function toggleFile(f,index){state.active=index;const p=activePane();if(!p.selected)p.selected=new Set();const key=fileKey(f);p.selected.has(key)?p.selected.delete(key):p.selected.add(key);renderFiles();}
function clearSelection(){const p=activePane();p.selected=new Set();p.appendMode=false;p.gestureAnchor=null;renderFiles();}
function selectionBar(){const bottom=$('#bottom'),p=activePane();if(p.mode==='recycle'){bottom.replaceChildren(iconButton('check','全选',()=>{p.selected=new Set(p.items.map(fileKey));renderFiles();}),iconButton('switch','反选文件',()=>{p.selected=new Set(p.items.filter(f=>!p.selected.has(fileKey(f))).map(fileKey));renderFiles();}),iconButton('close','清除选择',clearSelection),iconButton('more','选中文件操作',()=>recycleActions(selectedFiles()[0])),iconButton('info','选择项属性',()=>recycleProperties(selectedFiles())));return;}bottom.replaceChildren(iconButton('check','全选',()=>{activePane().selected=new Set(activePane().items.map(fileKey));renderFiles();}),iconButton('switch','反选文件',()=>{const p=activePane();p.selected=new Set(p.items.filter(f=>!p.selected.has(fileKey(f))).map(fileKey));renderFiles();}),iconButton('close','清除选择',clearSelection),iconButton('more','选中文件操作',()=>fileActions(selectedFiles()[0])),iconButton('info','选择项属性',()=>fileProperties(selectedFiles())));}
function fileActions(f){
 const selected=selectedFiles();const files=selected.length&&selected.some(x=>fileKey(x)===fileKey(f))?selected:[f];if(!files[0])return;const allLocal=files.every(x=>!x.archive),one=files.length===1;
 // 基础十项（与参考截图一致）：复制、移动、删除、重命名、工具、压缩、属性、分享、打开方式、添加书签；
 // 带 • 的项（复制/移动）可长按触发单窗口操作（先选中，再选目标粘贴）。
 const rows=[{label:(state.active===0?'→ ':'← ')+'复制',icon:'copy',run:()=>transferFiles(files,false),hold:()=>queueFiles(files,false)},{label:(state.active===0?'→ ':'← ')+'移动',icon:'edit',disabled:!allLocal,run:()=>transferFiles(files,true),hold:()=>queueFiles(files,true)},{label:'删除',icon:'trash',disabled:!allLocal,run:async()=>{if(pref.deleteWarning!==false&&!await confirmBox('删除所选文件？',files.map(x=>x.name).join(', ')+' 将移入回收站。','删除'))return;for(const x of files)await rpc('trash',{path:x.path});clearSelection();await refresh();}}, {label:'重命名',icon:'edit',disabled:!allLocal&&!(one&&f.archive),run:()=>one?renameFile(files[0]):batchRenameDialog(files)},
 {label:'工具',icon:'build',run:()=>toolsDialog(files)},{label:'压缩',icon:'download',disabled:!allLocal,run:()=>compressDialog(files)},
 {label:'属性',icon:'info',run:()=>fileProperties(files)},{label:'分享',icon:'export',disabled:files.some(x=>x.dir),run:async()=>{const paths=[];for(const x of files)paths.push(await rpc('resolve',fileArgs(x)));return rpc('shareMultiple',{paths});}},
 {label:'打开方式…',icon:'check',disabled:!one||f.dir,run:()=>openWithDialog(f)},{label:'添加书签',icon:'copy',disabled:!one,run:()=>addBookmarkFile(f)}];
 if(one&&fileType(f)==='apks')rows.push({label:'转换为 APK',icon:'build',run:()=>convertBundleDialog(f)});
 if(files.length>1&&files.every(x=>fileType(x)==='dex'))rows.push({label:'合并 DEX',icon:'layers',run:()=>dexMergeFiles(files)});
 if(files.length===2&&!files.some(x=>x.dir)){for(const mode of ['apk','dex','arsc','axml'])rows.push({label:'对比 '+mode,icon:'swap',run:()=>compareFiles(files,mode)});}
 if(one&&!f.dir&&/\.(7z|tar|tar\.(gz|xz|zst|bz2|lz4)|gz|gzip|001)$/i.test(f.name))rows.push({label:'解压到…',icon:'export',run:()=>extractDialog(f)});
 const layer=$('#menuLayer');clearLayer(layer,false);layer.innerHTML='<div class="overflowShade sheetShade"></div><section class="fileActionMenu" role="menu"><div class="actionHint">带 • 的菜单表示可以长按触发单窗口操作</div><div class="actionGrid"></div></section>';layer.firstChild.onclick=closeMenu;for(const row of rows){const b=document.createElement('button');b.setAttribute('role','menuitem');b.innerHTML=icon(row.icon)+'<span class="actionName">'+(row.hold?'<b class="menuDot" aria-hidden="true">•</b>':'')+esc(row.label)+'</span>';b.disabled=!!row.disabled;hold(b,async()=>{if(!row.hold)return;closeMenu();await row.hold();},async()=>{closeMenu();await row.run();});layer.querySelector('.actionGrid').append(b);}
}
function addBookmarkFile(f){if(!pref.bookmarks)pref.bookmarks=[];if(pref.bookmarks.some(x=>fileKey(x)===fileKey(f))){toast('已在书签中');return;}const entry={...f};const groups=pref.bookmarkGroups||[];
 const save=()=>{if(pref.bookmarkTop===false)pref.bookmarks.push(entry);else pref.bookmarks.unshift(entry);savePref();toast('书签已添加');};
 if(!groups.length)return save();
 options('选择分组',[{label:'未分组',icon:'folder',run:save},...groups.map(g=>({label:g,icon:'folder',run:()=>{entry.group=g;save();}}))]);}
function queueFiles(files,move){state.clipboard={files,args:fileArgs(files[0]),name:files.length===1?files[0].name:files.length+' 个文件',move};toast('请选择目标，然后点 + → 粘贴');}
async function transferFiles(files,move,destination){const destPane=state.panes[1-state.active];const target=destination||destPane.path;if(!destination&&destPane.mode==='recycle')throw Error('请先让另一窗格离开回收站。');if(!destination&&destPane.archive)throw Error('请在另一窗格中打开一个普通目标文件夹。');const list=[];for(const f of files){if(f.archive&&f.dir)throw Error('请先解压归档文件夹。');list.push({...fileArgs(f),name:f.name});}await rpc('batchTransfer',{files:list,destination:target,move});state.clipboard=null;activePane().selected=new Set();await refresh();toast((move?'已移动 ':'已复制 ')+files.length+' 到 '+target);}
async function renameFile(f){const buttons=[];if(f.name.endsWith('.bak'))buttons.push(['还原',async()=>{closeModal();const path=await rpc('restoreBackup',{path:f.path});await refresh();if(!pref.hideRestoreTip)dialog('提示','<p class="break">'+esc(f.name)+' 已还原为 '+esc(path.split('/').pop())+'. 如果原文件存在，它现在就是 .bak 文件。</p>',[['不再显示',()=>{pref.hideRestoreTip=true;savePref();closeModal();}],['关闭',closeModal]]);else toast('已还原备份');}]);buttons.push(['取消',closeModal],['确定',async()=>{const name=$('#renameName').value;if(!name||name===f.name){closeModal();return;}const result=await rpc('rename',{...fileArgs(f),name});closeModal();activePane().selected=new Set();if(f.archive){const p=activePane();if(p.archive===f.archive){await navigate(state.active,{path:p.path,archive:result,entry:p.entry});return;}await refresh();toast('已重命名，编辑副本：'+result);return;}await refresh();}]);const hint=f.archive?'<p class="bundleHint">重命名归档内条目会生成编辑副本（-edited），原包保持不变。</p>':'';const d=dialog('重命名',hint+'<input id="renameName" value="'+esc(f.name)+'" spellcheck="false">',buttons);d.classList.add('renameDialog');const input=d.querySelector('input');input.focus();const end=f.name.endsWith('.bak')?f.name.length-4:f.name.lastIndexOf('.');input.setSelectionRange(0,end>0?end:f.name.length);}
function batchRenameDialog(files){const d=dialog('重命名 '+files.length+' 个文件','<label>模式 · {name}、{ext}、{n}</label><input id="renamePattern" value="{name}_{n}{ext}"><pre id="renamePreview"></pre>',[['取消',closeModal],['确定',async()=>{const plans=preview();await rpc('batchRename',{files:plans});closeModal();activePane().selected=new Set();await refresh();}]]);function preview(){const pattern=$('#renamePattern').value,plans=files.map((f,i)=>{const dot=f.dir?-1:f.name.lastIndexOf('.'),name=dot>0?f.name.slice(0,dot):f.name,ext=dot>0?f.name.slice(dot):'';return {path:f.path,name:pattern.replace(/\{name\}/g,()=>name).replace(/\{ext\}/g,()=>ext).replace(/\{n\}/g,()=>String(i+1))};});$('#renamePreview').textContent=plans.map(x=>x.name).join('\n');return plans;}d.querySelector('input').oninput=preview;preview();}
async function fileProperties(files){const values=[];for(const f of files)values.push(await rpc('properties',fileArgs(f)));dialog('属性',values.map(p=>'<p class="break">'+esc(p.path)+'</p><p>大小：'+size(p.size)+'<br>修改时间：'+date(p.modified)+'<br>可读：'+(p.readable?'是':'否')+' · 可写：'+(p.writable?'是':'否')+'</p><p class="break">SHA-256<br>'+esc(p.sha256)+'</p>').join('<hr>'),[['复制路径',()=>rpc('clipboard',{text:values.map(x=>x.path).join('\n')}).then(()=>toast('已复制'))],['关闭',closeModal]]);}
function bookmarks(){const rows=pref.bookmarks||[];if(!rows.length)return alertBox('书签','长按文件或文件夹，然后选择添加书签。');options('书签',rows.map(f=>({label:f.name,icon:f.dir?'folder':'file',run:()=>options(f.name,[{label:'打开',icon:'play',run:()=>openFile(f)},{label:'移除书签',icon:'trash',run:()=>{pref.bookmarks=pref.bookmarks.filter(x=>fileKey(x)!==fileKey(f));savePref();bookmarks();}}])})));}
// 快速访问面板（参考截图）：底栏上滑打开，「最近 / 书签」两栏 + 过滤、添加分组、调整高度、手势说明、开启记录、清空记录。
let quickTab='recent',quickFilter='';
function recordRecentFolder(path){recordRecent({name:String(path).replace(/\/$/,'').split('/').pop()||String(path),path:String(path),dir:true});}
function recordRecentFile(f){recordRecent({name:f.name,path:f.path,dir:false});}
function recordRecent(entry){if(pref.recordRecent===false)return;if(!entry||!entry.path)return;const list=pref.recent||(pref.recent=[]);const i=list.findIndex(x=>x.path===entry.path);if(i>=0)list.splice(i,1);list.unshift({name:entry.name,path:entry.path,dir:entry.dir!==false,ts:Date.now()});if(list.length>80)list.length=80;savePref();}
function quickLayerNode(){let layer=$('#quickLayer');if(!layer){layer=document.createElement('div');layer.id='quickLayer';document.body.append(layer);}return layer;}
function closeQuick(){clearLayer(quickLayerNode(),false);}
function openQuickPanel(){closeMenu();closeDrawer();const layer=quickLayerNode();clearLayer(layer,false);quickTab=pref.quickTab==='bookmarks'?'bookmarks':'recent';renderQuickPanel();}
function renderQuickPanel(){const layer=$('#quickLayer');if(!layer)return;const height=Math.max(30,Math.min(90,Number(pref.quickHeight)||60));
 layer.innerHTML=`<div class="shade"></div><section class="quickPanel" style="height:${height}vh" role="dialog" aria-modal="true" aria-label="快速访问"><div class="quickHead"><div class="quickTabs" role="tablist"><button type="button" role="tab" data-quick-tab="recent" aria-selected="${quickTab==='recent'}">最近</button><button type="button" role="tab" data-quick-tab="bookmarks" aria-selected="${quickTab==='bookmarks'}">书签</button></div><button type="button" class="photoBtn" data-quick-menu aria-label="快速访问菜单">${icon('more')}</button></div><div class="quickBody"></div><div class="quickFoot"><button type="button" class="quickClose" data-quick-close aria-label="关闭快速访问">${icon('down')}</button></div></section>`;
 layer.querySelector('.shade').onclick=closeQuick;
 layer.querySelector('[data-quick-close]').onclick=closeQuick;
 layer.querySelectorAll('[data-quick-tab]').forEach(b=>b.onclick=()=>{quickTab=b.dataset.quickTab==='bookmarks'?'bookmarks':'recent';pref.quickTab=quickTab;savePref();syncQuickTabs();renderQuickBody();});
 layer.querySelector('[data-quick-menu]').onclick=quickPanelMenu;
 syncQuickTabs();renderQuickBody();}
function syncQuickTabs(){const layer=$('#quickLayer');if(!layer)return;layer.querySelectorAll('[data-quick-tab]').forEach(b=>{const active=b.dataset.quickTab===quickTab;b.classList.toggle('active',active);b.setAttribute('aria-selected',String(active));});}
function quickRowHtml(x,kind){const item=JSON.stringify({name:x.name,path:x.path,dir:!!x.dir});const type=x.dir?'folder':fileType({name:x.name,dir:false});return `<button type="button" class="quickRow" data-quick-kind="${kind}" data-quick-item="${esc(item)}"><span class="fileicon quickIcon ${type}">${icon(type)}</span><span class="quickMain"><span class="quickName">${esc(x.name)}</span><small>${esc(x.path)}</small></span></button>`;}
function renderQuickBody(){const body=$('#quickLayer .quickBody');if(!body)return;const q=quickFilter.toLowerCase();let html='';
 if(quickTab==='recent'){const list=(pref.recent||[]).filter(x=>!q||String(x.name).toLowerCase().includes(q)||String(x.path).toLowerCase().includes(q));
  html=list.length?list.map(x=>quickRowHtml(x,'recent')).join(''):'<div class="empty">暂无最近记录'+(q?'（过滤中）':'')+'</div>';
 }else{const groups=pref.bookmarkGroups||[];const list=(pref.bookmarks||[]).filter(x=>!q||String(x.name).toLowerCase().includes(q)||String(x.path).toLowerCase().includes(q));
  if(!list.length)html='<div class="empty">暂无书签</div>';
  else{const parts=[];const loose=list.filter(x=>!x.group);for(const g of groups){const items=list.filter(x=>x.group===g);if(!items.length)continue;parts.push('<div class="quickGroup">'+esc(g)+'</div>'+items.map(x=>quickRowHtml(x,'bookmark')).join(''));}if(loose.length)parts.push((groups.length?'<div class="quickGroup">未分组</div>':'')+loose.map(x=>quickRowHtml(x,'bookmark')).join(''));html=parts.join('');}}
 body.innerHTML=html;body.querySelectorAll('.quickRow').forEach(row=>{const item=JSON.parse(row.dataset.quickItem),kind=row.dataset.quickKind;hold(row,()=>quickItemMenu(item,kind),()=>openQuickItem(item));});}
function openQuickItem(item){closeQuick();if(item.dir)return act(()=>navigate(state.active,{path:item.path,archive:'',entry:''}));return act(()=>openFile({name:item.name,path:item.path,dir:false,size:0,time:0}));}
function quickItemMenu(item,kind){const rows=[{icon:'play',label:'打开',run:()=>openQuickItem(item)},{icon:'copy',label:'复制路径',run:async()=>{await rpc('clipboard',{text:item.path});toast('已复制路径');}}];
 if(kind==='recent')rows.push({icon:'pin',label:'添加书签',run:()=>addBookmarkFile({name:item.name,path:item.path,dir:item.dir})},{icon:'trash',label:'移除记录',run:()=>{pref.recent=(pref.recent||[]).filter(x=>x.path!==item.path);savePref();renderQuickBody();}});
 else rows.push({icon:'trash',label:'移除书签',run:()=>{pref.bookmarks=(pref.bookmarks||[]).filter(x=>fileKey(x)!==item.path);savePref();renderQuickBody();}});
 menu(rows);}
function quickPanelMenu(){menu([
 {icon:'search',label:'过滤',run:async()=>{const q=await ask('过滤快速访问',quickFilter,'名称或路径包含');if(q===null)return;quickFilter=q.trim();renderQuickBody();}},
 {icon:'plus',label:'添加分组',run:async()=>{const name=await ask('添加分组','','分组名称');if(name===null||!name.trim())return;const groups=pref.bookmarkGroups||(pref.bookmarkGroups=[]);const trimmed=name.trim();if(!groups.includes(trimmed))groups.push(trimmed);savePref();toast('分组已添加：'+trimmed);renderQuickBody();}},
 {icon:'up',label:'调整高度',run:quickHeightDialog},
 {icon:'info',label:'手势说明',run:()=>alertBox('手势说明','底部按键栏上滑打开快速访问；轻点面板上方的空白区域或面板底部的下拉箭头直接关闭；轻点条目直接打开文件夹或文件；长按条目显示更多操作；「最近」与「书签」可随时切换。')},
 {icon:'check',label:'开启记录',checked:pref.recordRecent!==false,run:()=>{pref.recordRecent=!(pref.recordRecent!==false);savePref();toast(pref.recordRecent?'已开启最近记录':'已停止记录新的访问');}},
 {icon:'trash',label:'清空记录',run:async()=>{if(!await confirmBox('清空最近记录？','将删除全部最近访问记录。','清空'))return;pref.recent=[];savePref();renderQuickBody();}}]);}
function quickHeightDialog(){const current=Math.max(30,Math.min(90,Number(pref.quickHeight)||60));
 const d=dialog('调整高度','<label>面板高度</label><input type="range" id="quickHeightRange" min="30" max="90" step="5" value="'+current+'"><p class="bundleHint" id="quickHeightValue">'+current+'%</p>',[['取消',closeModal],['确定',()=>{const v=Number($('#quickHeightRange').value)||60;pref.quickHeight=v;savePref();closeModal();renderQuickPanel();}]]);
 const range=d.querySelector('#quickHeightRange'),label=d.querySelector('#quickHeightValue');
 if(range&&label)range.oninput=()=>{label.textContent=range.value+'%';const panel=$('#quickLayer .quickPanel');if(panel)panel.style.height=range.value+'vh';};}
// 底部五个快捷键：上滑手势打开快速访问面板（参考 MP-Manager：上滑超过触
// 发阈值即确认为滑动；WebView 中滑动结束后仍会合成按钮 click，必须捕获吞
// 掉，否则上滑会误触「上级目录」等键导致顶部路径跳变）。
(function(){const bottom=$('#bottom');if(!bottom)return;let y0=null,x0=null,moved=false;
 bottom.addEventListener('pointerdown',e=>{y0=e.clientY;x0=e.clientX;moved=false;},{passive:true});
 bottom.addEventListener('pointermove',e=>{if(y0==null)return;const dy=y0-e.clientY,dx=e.clientX-x0;if(dy>16&&Math.abs(dy)>Math.abs(dx))moved=true;if(dy>50&&Math.abs(dy)>Math.abs(dx)){moved=true;y0=null;openQuickPanel();}},{passive:true});
 const end=()=>{y0=null;};bottom.addEventListener('pointerup',end,{passive:true});bottom.addEventListener('pointercancel',end,{passive:true});
 bottom.addEventListener('click',e=>{if(moved){moved=false;e.preventDefault();e.stopPropagation();}},true);})();
const archiveFormats=['zip','7z','tar','tar.gz','tar.xz','tar.zst','tar.bz2','tar.lz4','gzip'];
function compressDialog(files){
 let format='zip';const base=files.length===1?files[0].name.replace(/\.[^.]+$/,''):'归档';
 const d=dialog('创建归档','<label>文件名</label><input id="archiveName" value="'+esc(base+'.zip')+'" spellcheck="false"><div class="formColumns"><div><label>格式</label><select id="archiveFormat">'+archiveFormats.map(x=>'<option>'+x+'</option>').join('')+'</select></div><div><label>压缩级别</label><select id="archiveLevel"></select></div></div><label id="passwordLabel">密码（留空则不加密）</label><div class="passwordInput"><input id="archivePassword" type="password" autocomplete="off"><button id="passwordVisibility" aria-label="显示密码">•••</button></div><label>分卷大小</label><div class="formColumns"><select id="archiveSplit"><option value="0">无</option><option value="100">100 MB</option><option value="500">500 MB</option><option value="custom">自定义</option></select><div class="inlineInput"><input id="archiveSplitCustom" type="number" min="0.0625" step="any" placeholder="MB" disabled></div></div><label class="toggleRow"><span>压缩后删除源文件</span><input id="archiveDelete" type="checkbox"></label><label class="toggleRow"><span>压缩到另一窗格路径</span><input id="archiveOther" type="checkbox"></label><div class="archiveDestination muted"></div>',[['取消',closeModal],['确定',async()=>{
  const split=$('#archiveSplit').value;const bytes=split==='custom'?Math.round(Number($('#archiveSplitCustom').value)*1048576):Number(split)*1048576;if(!Number.isFinite(bytes)||bytes<0||split==='custom'&&bytes===0)throw Error('请输入有效的分卷大小。');
  const other=$('#archiveOther').checked,dest=state.panes[other?1-state.active:state.active];if(dest.archive)throw Error('请选择一个普通输出文件夹。');const args={paths:files.map(f=>f.path),format:$('#archiveFormat').value,name:$('#archiveName').value,level:$('#archiveLevel').value,password:$('#archivePassword').value,splitBytes:bytes,deleteSources:$('#archiveDelete').checked,destination:dest.path};
  d.querySelector('.actions button:last-child').disabled=true;try{const r=await rpc('compress',args);closeModal();activePane().selected=new Set();await refresh();alertBox('已创建归档',r.outputs.join('\n')+(r.cleanupErrors.length?'\n源文件清理：'+r.cleanupErrors.join('\n'):args.deleteSources?'\n源文件已移入回收站。':''));}finally{if(d.isConnected)d.querySelector('.actions button:last-child').disabled=false;}
 }]]);d.classList.add('archiveDialog');
 function levels(){const f=$('#archiveFormat').value,all=[['store','仅存储'],['fastest','最快'],['fast','快速'],['normal','标准'],['maximum','最大'],['ultra','极限'],['apk','APK 模式']];const values=f==='tar'?[all[0]]:f==='tar.lz4'?[['normal','默认']]:f==='zip'?all:['tar.bz2','tar.zst'].includes(f)?all.slice(1,-1):all.slice(0,-1);$('#archiveLevel').innerHTML=values.map(([v,t])=>'<option value="'+v+'">'+t+'</option>').join('');$('#archiveLevel').value=values.some(x=>x[0]==='maximum')?'maximum':values[0][0];const password=f==='zip'||f==='7z';$('#archivePassword').disabled=!password;if(!password)$('#archivePassword').value='';$('#passwordLabel').textContent=password?'密码（留空则不加密）':'此格式不支持密码';}
 $('#archiveFormat').onchange=()=>{const f=$('#archiveFormat').value,oldExt='.'+(format==='gzip'?'gz':format);const n=$('#archiveName');n.value=(n.value.endsWith(oldExt)?n.value.slice(0,-oldExt.length):n.value)+'.'+(f==='gzip'?'gz':f);format=f;levels();};$('#archiveSplit').onchange=()=>$('#archiveSplitCustom').disabled=$('#archiveSplit').value!=='custom';$('#passwordVisibility').onclick=()=>{const i=$('#archivePassword');i.type=i.type==='password'?'text':'password';$('#passwordVisibility').setAttribute('aria-label',i.type==='password'?'显示密码':'隐藏密码');};const destination=()=>d.querySelector('.archiveDestination').textContent=state.panes[$('#archiveOther').checked?1-state.active:state.active].path;$('#archiveOther').onchange=destination;levels();destination();
}
function extractDialog(f){const d=dialog('解压到…','<label>目标文件夹</label><input id="extractDestination" value="'+esc(state.panes[1-state.active].archive?state.info.workspace:state.panes[1-state.active].path)+'"><label>新文件夹名称</label><input id="extractName" value="'+esc(f.name.replace(/\.(tar\.(gz|xz|zst|bz2|lz4)|zip|apks|apk|7z|tar|gzip|gz)(\.001)?$/i,''))+'"><label>密码（如已加密）</label><input id="extractPassword" type="password" autocomplete="off">',[['取消',closeModal],['确定',async()=>{const args={...fileArgs(f),destination:$('#extractDestination').value,name:$('#extractName').value,password:$('#extractPassword').value};const out=await rpc('archiveExtract',args);closeModal();await refresh();toast('已解压到 '+out);}]]);}
function toolsDialog(files){
 const pane=activePane(),local=pane.mode!=='network'&&!pane.archive&&files.every(x=>!x.archive),one=files.length===1;
 options('工具',[
  {label:'文件分割',icon:'layers',disabled:!local||!one||files[0].dir,run:()=>splitDialog(files[0])},
  {label:'文件合并',icon:'copy',disabled:!local||files.length<2||files.some(x=>x.dir),run:()=>mergeDialog(files)},
  {label:'文件加密',icon:'lock',disabled:!local||files.some(x=>x.dir),run:()=>gpgEncryptDialog(files)},
  {label:'文件解密',icon:'unlock',disabled:!local||!one||files[0].dir,run:()=>gpgDecryptDialog(files[0])},
  {label:'交换文件名',icon:'swap',disabled:!local||files.length!==2||files.some(x=>x.dir),run:()=>swapNamesDialog(files)},
  {label:'创建软链接',icon:'jump',disabled:!local||!one,run:()=>symlinkDialog(files[0])},
  {label:'创建快捷方式',icon:'export',disabled:!local||!one,run:()=>shortcutDialog(files[0])},
  {label:'导出文件列表',icon:'text',disabled:!local||!one,run:()=>exportListDialog(files[0])}
 ]);
}
function parseSize(text){const m=/^\s*(\d+(?:\.\d+)?)\s*(b|kb|mb|gb|k|m|g)?\s*$/i.exec(String(text));if(!m)return 0;const u=(m[2]||'b').toLowerCase();return Math.round(parseFloat(m[1])*(u==='kb'||u==='k'?1024:u==='mb'||u==='m'?1048576:u==='gb'||u==='g'?1073741824:1));}
function splitDialog(f){
 const d=dialog('文件分割','<label>每卷大小（字节，可写 10MB / 512KB）</label><input id="splitSize" value="10MB" spellcheck="false"><p class="bundleHint">'+esc(f.name)+' · '+size(Number(f.size)||0)+' → '+esc(f.name)+'.part01、.part02…</p><label class="toggleRow"><span>分割后删除源文件（移入回收站）</span><input id="splitDelete" type="checkbox"></label>',[['取消',closeModal],['开始分割',async()=>{
  const bytes=parseSize($('#splitSize').value);if(!bytes)throw Error('请输入有效的分卷大小，例如 10MB 或 1048576。');
  const button=d.querySelector('.actions button:last-child');button.disabled=true;
  try{const r=await rpc('fileSplit',{path:f.path,chunkBytes:bytes,deleteSource:$('#splitDelete').checked});closeModal();clearSelection();await refresh();toast('已分割为 '+r.parts+' 个分卷（'+size(r.bytes)+'）');}finally{if(d.isConnected)button.disabled=false;}
 }]]);
}
function mergeDialog(files){
 dialog('文件合并','<p class="break">'+files.map(x=>esc(x.name)).join('<br>')+'</p><p class="bundleHint">将收集同文件夹中全部同名 .partNN 分卷，按顺序合并为去掉分卷后缀的文件。</p>',[['取消',closeModal],['合并',async()=>{
  const r=await rpc('fileMerge',{paths:files.map(x=>x.path)});closeModal();clearSelection();await refresh();toast('已合并 '+r.parts+' 个分卷 → '+String(r.path).split('/').pop());
 }]]);
}
function gpgKeySuggestion(exclude){return activePane().items.find(x=>!x.dir&&x.path&&x.path!==exclude&&/\.(asc|gpg|pgp)$/i.test(x.name))||null;}
function gpgEncryptDialog(files){
 const suggestion=gpgKeySuggestion(null);
 const d=dialog('GPG 加密','<div class="formColumns"><div><label>加密方式</label><select id="gpgMode"><option value="passphrase">口令（对称）</option><option value="publicKey">公钥</option></select></div><div><label>输出格式</label><select id="gpgFormat"><option value="gpg">二进制 .gpg</option><option value="asc">ASCII 装甲 .asc</option></select></div></div><div id="gpgPassRow"><label>口令（解密时需要，请牢记）</label><div class="passwordInput"><input id="gpgPassword" type="password" autocomplete="off"><button id="gpgVisibility" type="button" aria-label="显示口令">•••</button></div></div><div id="gpgKeyRow" style="display:none"><label>公钥文件（.asc / .gpg）</label><input id="gpgPublicKey" value="'+esc(suggestion?suggestion.path:'')+'" spellcheck="false"></div><label class="toggleRow"><span>加密后删除源文件（移入回收站）</span><input id="gpgDelete" type="checkbox"></label><p class="bundleHint">生成标准 OpenPGP 文件（AES-256，含完整性保护），桌面 GnuPG（gpg -d）与本应用均可解密。</p>',[['取消',closeModal],['开始加密',async()=>{
  const mode=$('#gpgMode').value,armor=$('#gpgFormat').value==='asc',password=$('#gpgPassword').value,publicKey=$('#gpgPublicKey').value.trim();
  if(mode==='passphrase'&&!password)throw Error('请输入加密口令。');
  if(mode==='publicKey'&&!publicKey)throw Error('请填写接收方公钥文件（.asc / .gpg）的路径。');
  const button=d.querySelector('.actions button:last-child');button.disabled=true;
  try{const r=await rpc('gpgEncrypt',{paths:files.map(x=>x.path),mode,password,publicKey,armor,deleteSources:$('#gpgDelete').checked});closeModal();clearSelection();await refresh();alertBox('已用 GPG 加密 '+r.outputs.length+' 个文件',r.outputs.join('\n')+(mode==='publicKey'?'\n\n接收方需用对应私钥解密。':'\n\n请牢记口令：口令丢失后无法恢复。'));}finally{if(d.isConnected)button.disabled=false;}
 }]]);
 d.querySelector('#gpgMode').onchange=()=>{const key=$('#gpgMode').value==='publicKey';d.querySelector('#gpgKeyRow').style.display=key?'':'none';d.querySelector('#gpgPassRow').style.display=key?'none':'';};
 d.querySelector('#gpgVisibility').onclick=()=>{const i=d.querySelector('#gpgPassword');i.type=i.type==='password'?'text':'password';d.querySelector('#gpgVisibility').setAttribute('aria-label',i.type==='password'?'显示口令':'隐藏口令');};
}
function gpgDecryptDialog(f){
 const suggestion=gpgKeySuggestion(f.path);
 const d=dialog('GPG 解密','<label>口令（对称口令或私钥保护口令）</label><div class="passwordInput"><input id="gpgDecPassword" type="password" autocomplete="off"><button id="gpgDecVisibility" type="button" aria-label="显示口令">•••</button></div><label>私钥文件（解公钥加密文件时填写，可留空）</label><input id="gpgSecretKey" value="'+esc(suggestion?suggestion.path:'')+'" spellcheck="false"><p class="bundleHint">'+esc(f.name)+' → 输出到同文件夹，自动去掉 .gpg / .asc 后缀。</p>',[['取消',closeModal],['解密',async()=>{
  const button=d.querySelector('.actions button:last-child');button.disabled=true;
  try{const out=await rpc('gpgDecrypt',{path:f.path,password:$('#gpgDecPassword').value,secretKey:$('#gpgSecretKey').value.trim()});closeModal();await refresh();toast('已解密 → '+String(out).split('/').pop());}finally{if(d.isConnected)button.disabled=false;}
 }]]);
 d.querySelector('#gpgDecVisibility').onclick=()=>{const i=d.querySelector('#gpgDecPassword');i.type=i.type==='password'?'text':'password';d.querySelector('#gpgDecVisibility').setAttribute('aria-label',i.type==='password'?'显示口令':'隐藏口令');};
}
function swapNamesDialog(files){
 dialog('交换文件名','<p class="break">A：'+esc(files[0].name)+'<br>B：'+esc(files[1].name)+'</p><p class="bundleHint">两个文件将在同一文件夹内互换名称。</p>',[['取消',closeModal],['交换',async()=>{await rpc('swapNames',{first:files[0].path,second:files[1].path});closeModal();clearSelection();await refresh();toast('已交换文件名');}]]);
}
async function symlinkDialog(f){
 const other=state.panes[1-state.active],dest=other.mode===''&&!other.archive?other.path:activePane().path;
 const name=await ask('创建软链接','链接-'+f.name,'链接名称（创建于 '+dest+'，指向 '+f.name+'）');
 if(!name)return;
 await rpc('createSymlink',{target:f.path,destination:dest,name});
 await refresh();toast('已创建软链接 → '+f.name);
}
async function shortcutDialog(f){
 if(!await confirmBox('创建快捷方式','将在桌面添加「'+f.name+'」的快捷方式，轻点后直接跳转到本应用中的该位置。','创建'))return;
 await rpc('createShortcut',{path:f.path});
 toast('已请求添加桌面快捷方式');
}
async function exportListDialog(f){
 const dir=f.dir?f.path:activePane().path;
 const name=await ask('导出文件列表','文件列表.txt','输出文件名（保存在 '+dir+'）');
 if(!name)return;
 const r=await rpc('exportFileList',{path:dir,name});
 await refresh();toast('已导出：'+r.files+' 个文件 / '+r.folders+' 个文件夹');
}
async function compareFiles(files,mode){const left=await rpc('resolve',fileArgs(files[0])),right=await rpc('resolve',fileArgs(files[1]));const r=await rpc('compare',{left,right,mode});const body=toolPage('对比 '+mode,files[0].name+' ↔ '+files[1].name);body.innerHTML='<p>'+r.added+' 新增 · '+r.removed+'删除 · '+r.changed+'修改 · '+r.same+'未变化'+(r.truncated?' · 前 2,000 处差异':'')+'</p><div id="compareRows"></div>';if(mode==='arsc')body.insertAdjacentHTML('afterbegin','<p>按索引比较资源块/头部校验和与字符串池。</p>');for(const row of r.rows){const b=document.createElement('button');b.className='listrow';b.innerHTML='<span class="main">'+esc(row.name)+'<small>'+row.status+'</small></span>';b.onclick=()=>dialog(row.name,'<h3>左侧</h3><pre class="diffText">'+esc(row.left)+'</pre><h3>右侧</h3><pre class="diffText">'+esc(row.right)+'</pre>',[['关闭',closeModal]]);$('#compareRows').append(b);}}
async function showApk(args,extract=false){
 const info=await rpc('apkInfo',args);
 const protection=info.protection||{};
 const protectionLevel=protection.level||info.protectionLevel||'未知';
 const fields=[['包名',info.package],['版本代码',info.versionCode],['文件大小',size(info.size)],['签名',info.signature],['防护等级',protectionLevel],['目标 SDK',sdkLabel(info.targetSdk)],['最低 SDK',sdkLabel(info.minSdk)]];
 const view=async()=>{closeModal();await navigate(state.active,{path:activePane().path,archive:info.path,entry:''});};
 const install=async()=>{closeModal();await installApk(info.path);};
 const actions=extract?[['提取 APK',()=>extractInstalledApk(info)],['查看',view],['安装',install]]:[['功能',()=>{closeModal();apkFeatureMenu(info);}],['查看',view],['安装',install]];
 const d=dialog('',`<div class="apkHead">${info.icon?`<img src="${esc(info.icon)}" width="48" height="48" alt="应用图标">`:'<div class="brandMark" aria-hidden="true">'+brandLogo()+'</div>'}<div class="apkTitle"><h2>${esc(info.name)}</h2><div class="subtitle">${esc(info.version)}</div></div></div><div class="dataGrid">${fields.map(([k,v])=>`<div>${k}</div><button class="value" title="${esc(v)}" data-detail="${esc(k)}" data-value="${esc(v)}">${esc(v)}</button>`).join('')}</div>`,actions);
 d.querySelector('h2').remove();d.classList.add('apkDialog');d.querySelectorAll('[data-detail]').forEach(b=>b.onclick=()=>{if(b.dataset.detail==='防护等级')return showProtection(info);alertBox(b.dataset.detail,b.dataset.value);});
}
function showProtection(info){
 const p=info.protection||{};const findings=p.findings||[];
 const lines=['等级：'+(p.level||'未知')+'（分数 '+(p.score||0)+'）','签名：'+(p.signature||info.signature||'未知'),'可调试：'+(p.debuggable?'是':'否'),'testOnly：'+(p.testOnly?'是':'否'),'native 库：'+(p.nativeLibs||0)+' 个','DEX：'+(p.dexFiles||0)+' 个'];
 if(p.packers)lines.push('加固特征：'+p.packers);
 if(findings.length)lines.push('','检测项：'+findings.map(x=>(x.severity==='danger'?'[!] ':x.severity==='warn'?'[?] ':'[i] ')+x.text).join('\n'));
 alertBox('防护等级评估',lines.join('\n'));
}
function apkFeatureMenu(info){options('APK 功能',[
 {icon:'save',label:'APK 签名',run:()=>apkSignDialog(info)},
 {icon:'build',label:'APK 优化',run:()=>apkOptimizeDialog(info)},
 {icon:'layers',label:'APK 共存',run:()=>apkCloneDialog(info)},
 {icon:'layers',label:'RES 资源精简',run:()=>apkResSlimDialog(info)},
 {icon:'build',label:'去除签名校验',run:()=>apkSigBypassDialog(info)},
 {icon:'build',label:'数据复用优化',run:()=>apkResDedupeDialog(info)},
 {icon:'text',label:'注入日志记录',run:()=>apkInjectLogDialog(info)},
 {icon:'layers',label:'注入文件提供器',run:()=>apkInjectProviderDialog(info)},
 {icon:'code',label:'XML 翻译模式',run:()=>apkXmlTranslateDialog(info)},
 {icon:'edit',label:'XML 批量替换',run:()=>apkXmlBatchDialog(info)},
 {icon:'layers',label:'RES 资源混淆',run:()=>apkResObfuscateDialog(info)},
 {icon:'layers',label:'RES 反资源混淆',run:()=>apkResDeobfuscateDialog(info)},
 {icon:'code',label:'DEX 字符串解密',run:()=>apkDexDecryptDialog(info)},
 {icon:'code',label:'DEX 混淆对抗',run:()=>apkDexDeobfuscateDialog(info)},
 {icon:'code',label:'DEX 重新划分',run:()=>apkDexRepartitionDialog(info)}
],true);}
async function runApkTool(info,action,args,title){
 const out=await rpc(action,Object.assign({path:info.path},args));
 await refresh();
 alertBox(title,out.path||'完成');
 return out;
}
function apkResSlimDialog(info){
 const d=dialog('RES 资源精简',
  '<label class="toggleRow"><span>删除未被引用的资源（扫描 DEX / AXML）</span><input id="slimUnused" type="checkbox" checked></label>'+
  '<label>移除语言配置（逗号分隔，如 fr,de；留空不删）</label><input id="slimLocales" placeholder="fr,de" spellcheck="false" autocomplete="off">'+
  '<label>移除资源类型（如 raw,anim）</label><input id="slimTypes" placeholder="raw,anim" spellcheck="false" autocomplete="off">'+
  '<label>按资源名删除（正则，留空不删）</label><input id="slimNames" placeholder="^debug_.*" spellcheck="false" autocomplete="off">'+
  '<p class="signHint">检测到 getIdentifier / getResourceName 时会自动保留全部资源。输出为新 APK，并用内置开发证书签名。</p>',
  [['取消',closeModal],['精简',async()=>{
   const unused=d.querySelector('#slimUnused').checked,locales=d.querySelector('#slimLocales').value.trim(),types=d.querySelector('#slimTypes').value.trim(),names=d.querySelector('#slimNames').value.trim();
   if(!unused&&!locales&&!types&&!names)return toast('至少选择一种精简方式。');
   closeModal();const out=await rpc('resSlim',{path:info.path,unused,locales,types,names});await refresh();
   alertBox('已精简资源',out.path+'\n条目 '+out.stats.entries+' · 配置 '+out.stats.configs+' · 文件 '+out.stats.files+(out.stats.allKept?'\n检测到动态资源查找，已保留全部资源。':''));
  }]]);
}
function apkResObfuscateDialog(info){
 dialog('RES 资源混淆',
  '<p class="signHint">把资源名与资源文件名改写为 a、b、c…，资源 ID 不变。DEX 通过 ID 引用资源时不受影响；若代码用 getIdentifier 按名称查找会失效。</p>'+
  '<label class="toggleRow"><span>强制混淆（忽略动态资源查找检测）</span><input id="resObsForce" type="checkbox"></label>',
  [['取消',closeModal],['混淆',async()=>{const force=document.querySelector('#resObsForce').checked;closeModal();const out=await rpc('resObfuscate',{path:info.path,force});await refresh();alertBox('已混淆资源',out.path+'\n资源名 '+out.stats.entries+' · 文件 '+out.stats.files);}]]);
}
function apkResDeobfuscateDialog(info){
 dialog('RES 反资源混淆',
  '<p class="signHint">把 a/b/c 这类混淆资源名改为 <类型>_<ID>，并把混淆的资源文件名改成可读名称。资源 ID 与引用保持不变。</p>',
  [['取消',closeModal],['反混淆',async()=>{closeModal();const out=await rpc('resDeobfuscate',{path:info.path});await refresh();alertBox('已反混淆资源',out.path+'\n资源名 '+out.stats.entries+' · 文件 '+out.stats.files);}]]);
}
function apkResDedupeDialog(info){
 dialog('数据复用优化',
  '<p class="signHint">按内容哈希合并完全相同的 res 文件，并让资源表指向同一份文件。适合清理重复图片/布局，不改变资源 ID。</p>',
  [['取消',closeModal],['优化',async()=>{closeModal();const out=await rpc('resDedupe',{path:info.path});await refresh();alertBox('已完成数据复用',out.path+'\n合并 '+out.stats.removed+' 个文件 · 节省 '+size(out.stats.savedBytes));}]]);
}
function apkXmlBatchDialog(info){
 const d=dialog('XML 批量替换',
  '<label>查找内容</label><input id="xmlFind" spellcheck="false" autocomplete="off">'+
  '<label>替换为</label><input id="xmlReplace" spellcheck="false" autocomplete="off">'+
  '<label class="toggleRow"><span>使用正则表达式</span><input id="xmlRegex" type="checkbox"></label>'+
  '<label class="toggleRow"><span>区分大小写</span><input id="xmlCase" type="checkbox" checked></label>'+
  '<label class="toggleRow"><span>同时处理 AndroidManifest.xml</span><input id="xmlManifest" type="checkbox"></label>'+
  '<p class="signHint">只替换文本节点和普通字符串属性；@资源 / ?主题 / 数字 / 颜色 / 布尔值不会被改写。</p>',
  [['取消',closeModal],['替换',async()=>{
   const find=d.querySelector('#xmlFind').value;if(!find)return toast('请填写查找内容。');
   const out=await rpc('apkXmlReplace',{path:info.path,find,replace:d.querySelector('#xmlReplace').value,regex:d.querySelector('#xmlRegex').checked,matchCase:d.querySelector('#xmlCase').checked,manifest:d.querySelector('#xmlManifest').checked});
   closeModal();await refresh();alertBox('已批量替换',out.path+'\n替换 '+out.stats.hits+' 处 · 文件 '+out.stats.files+' 个');
  }]]);
}
async function apkXmlTranslateDialog(info){
 let config={},languages=[];
 try{const data=await rpc('translateConfig');config=data.config||{};languages=(data.languages&&data.languages.languages)||[];}catch(e){}
 const current=config.target||'en';
 const optionsHtml=languages.map(x=>'<option value="'+esc(x.key)+'"'+(x.key===current?' selected':'')+'>'+esc(x.name)+'</option>').join('');
 const d=dialog('XML 翻译模式',
  (config.configured?'':'<p class="signHint">尚未配置翻译服务，填写凭据后才能自动翻译。</p><button class="primary" id="xmlGoCfg">前往配置翻译服务</button>')+
  '<label>目标语言</label><select id="xmlLang">'+optionsHtml+'</select>'+
  '<label>最多翻译条数</label><input id="xmlLimit" type="number" min="1" value="300" inputmode="numeric">'+
  '<p class="signHint">翻译 res/ 下的二进制 XML 文本属性（text/hint/title/summary 等），写回同一文件。</p>',
  [['取消',closeModal],['翻译',async()=>{
   const lang=document.querySelector('#xmlLang').value,limit=Math.max(1,Number(document.querySelector('#xmlLimit').value)||300);
   closeModal();const out=await rpc('apkXmlTranslate',{path:info.path,lang,limit});await refresh();
   let msg=out.path+'\n翻译 '+out.stats.translated+' 条 · 文件 '+out.stats.files+' 个';
   if((out.stats.errors||[]).length)msg+='\n错误：'+out.stats.errors.join(' | ');
   alertBox('已翻译 XML',msg);
  }]]);
 const goCfg=d.querySelector('#xmlGoCfg');if(goCfg)goCfg.onclick=()=>{closeModal();act(translatePage);};
}
function apkSigBypassDialog(info){
 const d=dialog('去除签名校验',
  '<label>校验方法匹配规则（方法名正则）</label><input id="sigPattern" spellcheck="false" autocomplete="off" placeholder="留空使用默认：check/verify/valid + sign/cert">'+
  '<div id="sigCandidates" class="signHint" style="max-height:160px;overflow:auto">正在扫描…</div>'+
  '<p class="signHint">只把返回 boolean/int 且读取 PackageManager / Signature / 证书字符串的方法替换为 return true。误伤风险自负，建议先看扫描结果。</p>',
  [['取消',closeModal],['重新扫描',async()=>{const pattern=d.querySelector('#sigPattern').value.trim();await scanSig(info,pattern,d);}],['去除校验',async()=>{
   const pattern=d.querySelector('#sigPattern').value.trim();closeModal();
   const out=await rpc('sigBypass',{path:info.path,pattern:pattern||undefined});await refresh();
   alertBox('已去除签名校验',out.path+'\n已替换 '+out.patched+' 个方法');
  }]]);
 scanSig(info,'',d);
}
async function scanSig(info,pattern,d){
 const box=d.querySelector('#sigCandidates');if(box)box.textContent='正在扫描…';
 try{
  const r=await rpc('sigScan',{path:info.path,pattern:pattern||undefined});
  const rows=(r.candidates||[]).map(x=>x.class.replace(/^L|;$/g,'').replace(/\//g,'.')+'->'+x.method+'  ('+x.reason+')');
  if(box)box.textContent=r.count?('发现 '+r.count+' 个候选：\n'+rows.slice(0,40).join('\n')+(r.count>40?'\n…':'')):'未发现明显的签名校验方法。';
 }catch(e){if(box)box.textContent='扫描失败：'+(e.message||e);}
}
function apkInjectLogDialog(info){
 dialog('注入日志记录',
  '<p class="signHint">在每个匹配方法开头插入零寄存器探针 LogProbe.d()，通过 Throwable 读取调用类/方法名并写入 Logcat（TAG=CyberHub）。建议限定类或方法，避免方法数暴涨。</p>'+
  '<label>类名规则（正则）</label><input id="logClass" value=".*" spellcheck="false" autocomplete="off">'+
  '<label>方法名规则（正则）</label><input id="logMethod" value=".*" spellcheck="false" autocomplete="off">'+
  '<label>排除类前缀（逗号分隔）</label><input id="logExclude" value="android.*,java.*,javax.*,kotlin.*,com.android.*" spellcheck="false" autocomplete="off">'+
  '<label>最多注入方法数</label><input id="logLimit" type="number" min="1" value="2000" inputmode="numeric">',
  [['取消',closeModal],['注入',async()=>{
   const cls=document.querySelector('#logClass').value.trim()||'.*',mtd=document.querySelector('#logMethod').value.trim()||'.*',exclude=document.querySelector('#logExclude').value.trim();
   const limit=Math.max(1,Number(document.querySelector('#logLimit').value)||2000);
   closeModal();const out=await rpc('injectLog',{path:info.path,classPattern:cls,methodPattern:mtd,exclude,limit});await refresh();
   alertBox('已注入日志',out.path+'\n注入 '+out.injected+' 个方法'+(out.probeAdded?' · 已加入 LogProbe':''));
  }]]);
}
function apkInjectProviderDialog(info){
 const pkg=info.package||'app';
 dialog('注入文件提供器',
  '<label>authority 后缀</label><input id="fpSuffix" value="cyberhub.fileprovider" spellcheck="false" autocomplete="off">'+
  '<p class="signHint">将新增 exported=false 的 ContentProvider：<br>content://'+esc(pkg)+'.<span id="fpPreview">cyberhub.fileprovider</span>/files/&lt;相对路径&gt;<br>支持 files/、cache/、external/ 三个根目录，并校验路径不能越界。接收方需要 grantUriPermissions。</p>',
  [['取消',closeModal],['注入',async()=>{
   const authority=document.querySelector('#fpSuffix').value.trim()||'cyberhub.fileprovider';
   closeModal();const out=await rpc('injectProvider',{path:info.path,authority});await refresh();
   alertBox('已注入文件提供器',out.path+'\n'+out.uri);
  }]]);
 const input=document.querySelector('#fpSuffix');if(input)input.oninput=()=>{const preview=document.querySelector('#fpPreview');if(preview)preview.textContent=input.value.trim()||'cyberhub.fileprovider';};
}
function apkDexRepartitionDialog(info){
 dialog('DEX 重新划分',
  '<label>划分方式</label><select id="splitMode"><option value="count">指定 DEX 数量</option><option value="classes">按每个 DEX 的类数</option></select>'+
  '<label id="splitCountLabel">目标 DEX 数量</label><input id="splitCount" type="number" min="1" value="2" inputmode="numeric">'+
  '<label id="splitClassesLabel" hidden>每个 DEX 最大类数</label><input id="splitClasses" type="number" min="1" value="2000" inputmode="numeric" hidden>'+
  '<p class="signHint">按原 DEX 顺序重排全部类，classes.dex 优先，然后写入 classes2.dex、classes3.dex…。不修改类内容，完成后自动签名。</p>',
  [['取消',closeModal],['划分',async()=>{
   const mode=document.querySelector('#splitMode').value;
   const args=mode==='classes'?{maxClasses:Math.max(1,Number(document.querySelector('#splitClasses').value)||2000)}:{count:Math.max(1,Number(document.querySelector('#splitCount').value)||2)};
   closeModal();const out=await rpc('dexRepartition',Object.assign({path:info.path},args));await refresh();
   alertBox('已重新划分 DEX',out.path+'\nDEX '+out.stats.dexes+' 个 · 类 '+out.stats.classes+' 个');
  }]]);
 const mode=document.querySelector('#splitMode');
 if(mode)mode.onchange=()=>{const isClasses=mode.value==='classes';document.querySelector('#splitCount').hidden=isClasses;document.querySelector('#splitCountLabel').hidden=isClasses;document.querySelector('#splitClasses').hidden=!isClasses;document.querySelector('#splitClassesLabel').hidden=!isClasses;};
}
async function extractInstalledApk(info){
 const dir=await ask('提取 APK 到',state.info.permission?state.info.home+'/下载':state.info.workspace);
 if(!dir)return;
 const name=await ask('APK 文件名',(info.package||'app')+'-'+(info.versionCode||'')+'.apk');
 if(!name)return;
 const out=await rpc('extractApk',{path:info.path,destination:dir,name});
 dialog('已提取 APK',`<p class="break">${esc(out)}</p>`,[['打开文件夹',async()=>{closeModal();state.screen='files';await navigate(state.active,{path:dir,archive:'',entry:''});}],['关闭',closeModal]]);
}
function sdkLabel(api){const versions={21:'5',22:'5.1',23:'6',24:'7',25:'7.1',26:'8',27:'8.1',28:'9',29:'10',30:'11',31:'12',32:'12L',33:'13',34:'14',35:'15',36:'16'};return versions[api]?'Android '+versions[api]+' (API '+api+')':'API '+api;}
function apkSignDialog(info){
 const schemes=[
  {id:'v1v2v3',label:'自定义 V1 + V2 + V3'},
  {id:'v1v2',label:'V1 + V2'},
  {id:'v1v3',label:'V1 + V3'},
  {id:'v1',label:'V1'},
  {id:'v2v3',label:'V2 + V3（Android 7.0+）'},
  {id:'v2',label:'V2（Android 7.0+）'},
  {id:'v3',label:'V3（Android 9.0+）'}
 ];
 const notes={
  v1v2v3:'兼容性最好，支持 Android 1.0 及以上。',
  v1v2:'同时生成 V1 与 V2。Android 7.0+ 使用 V2，旧系统使用 V1。',
  v1v3:'同时生成 V1 与 V3。Android 9.0+ 使用 V3，旧系统使用 V1。',
  v1:'仅生成 V1（JAR 签名）。支持全部版本，但校验较弱、易被篡改。',
  v2v3:'仅生成 V2 + V3（Android 7.0+）。较少方案更安全，但 Android 7.0 以下无法安装。',
  v2:'仅生成 V2（Android 7.0+）。Android 7.0 以下无法识别签名。',
  v3:'仅生成 V3（Android 9.0+）。Android 9.0 以下无法识别签名。'
 };
 const isV=s=>info.signature&&info.signature.includes('V'+s);
 const current=isV(1)&&isV(2)&&isV(3)?'v1v2v3':isV(1)&&isV(2)?'v1v2':isV(1)&&isV(3)?'v1v3':isV(2)&&isV(3)?'v2v3':isV(1)?'v1':isV(2)?'v2':'v1v2v3';
 const d=dialog('APK 签名','<label for="apkSignKey">签名密钥</label><select id="apkSignKey"><option value="default">默认（内置开发证书）</option></select><label for="apkSignScheme">签名方案</label><select id="apkSignScheme">'+schemes.map(s=>'<option value="'+s.id+'"'+(s.id===current?' selected':'')+'>'+esc(s.label)+'</option>').join('')+'</select><p class="signHint" id="apkSignNote"></p>',[['取消',closeModal],['确定',async()=>{
  const scheme=d.querySelector('#apkSignScheme').value;
  const buttons=d.querySelectorAll('.actions button');[].forEach.call(buttons,b=>b.disabled=true);
  try{
   closeModal();const out=await rpc('sign',{path:info.path,scheme});await refresh();alertBox('已签名 APK',out+'（保存于原文件同级目录）');
  }finally{[].forEach.call(buttons,b=>b.disabled=false);if(d.isConnected)closeModal(false);}
 }]]);
 d.classList.add('signDialog');
 const note=()=>{const n=notes[d.querySelector('#apkSignScheme').value];const cur=info.signature&&(info.signature==='未签名 / 未校验'||info.signature.startsWith('签名无效')||info.signature.startsWith('无法校验'))?'（原签名：'+info.signature+'）':'（当前签名 '+info.signature+'）';d.querySelector('#apkSignNote').textContent=n+'\n'+cur;};
 d.querySelector('#apkSignScheme').onchange=note;note();
}
function apkCloneDialog(info){
 const d=dialog('APK 共存','<label for="apkClonePackage">新包名</label><input id="apkClonePackage" value="'+esc(info.package+'')+'" spellcheck="false" autocomplete="off"><label class="toggleRow"><span>使用旧版共存方案</span><input id="apkCloneLegacy" type="checkbox"></label><p class="signHint">新包名需与当前（'+esc(info.package)+'）不同，例如在原包名后加 .clone。旧版方案仅修改清单，兼容性更好但不处理代码内包名字符串。</p>',[['取消',closeModal],['确定',async()=>{
  const newPackage=$('#apkClonePackage').value.trim(),legacy=$('#apkCloneLegacy').checked;
  if(!newPackage)return toast('请填写新包名。');
  if(newPackage===info.package)return toast('新包名不能与当前包名相同。');
  const buttons=d.querySelectorAll('.actions button');[].forEach.call(buttons,b=>b.disabled=true);
  try{
   closeModal();const out=await rpc('clone',{path:info.path,newPackage,legacy});await refresh();alertBox('共存 APK 已生成',out+'（保存于原文件同级目录）');
  }finally{[].forEach.call(buttons,b=>b.disabled=false);if(d.isConnected)closeModal(false);}
 }]]);
 const input=d.querySelector('#apkClonePackage');setTimeout(()=>{if(input&&input.isConnected){input.focus();input.select();}},30);
}
function apkOptimizeDialog(info){
 const d=dialog('APK 优化',
  '<label class="toggleRow"><span>深度优化（dex 重写 + 资源清理）</span><input id="optDeep" type="checkbox"></label>'+
  '<p class="signHint">深度模式会删除未使用的类/方法/字段并清理资源，显著减小体积，耗时较长，且无法完全保证兼容所有场景。基础模式仅重新压缩（清单/arsc 无压缩、res 图片无压缩、其余极限压缩）。优化完成后自动使用内置开发证书签名。</p>'+
  '<label for="optUltra">压缩级别</label><select id="optUltra"><option value="9">极限（-9）</option><option value="5">高（-5）</option></select>'+
  '<label for="optDelete">删除文件（每行一个后缀或正则，留空不删）</label><textarea id="optDelete" rows="2" placeholder="\\.png$" spellcheck="false"></textarea>',
  [['取消',closeModal],['确定',async()=>{
   const deep=$('#optDeep').checked;
   const ultraLevel=$('#optUltra').value=='9'?9:5;
   const delLines=$('#optDelete').value.split('\n').map(s=>s.trim()).filter(s=>s.length);
   const buttons=d.querySelectorAll('.actions button');[].forEach.call(buttons,b=>b.disabled=true);
   try{
    closeModal();
    const out=await rpc('optimize',{path:info.path,deep,ultraLevel,deleteFiles:delLines});
    await refresh();
    let msg=out.path+(out.removedFiles!=null?' · 删除条目 '+out.removedFiles:'');
    if(out.savedBytes>0)msg+=' · 节省 '+size(out.savedBytes);
    msg+='\n已用内置开发证书签名';
    alertBox('已优化 APK',msg);
   }finally{[].forEach.call(buttons,b=>b.disabled=false);if(d.isConnected)closeModal(false);}
  }]]);
}
function apkDexDecryptDialog(info){
 const d=dialog('DEX 字符串解密',
  '<label for="dexCustom">自定义解密函数（smali 签名，* 通配，留空自动识别）</label>'+
  '<input id="dexCustom" placeholder="Lcom/foo/Str;->a([B[B)Ljava/lang/String;" spellcheck="false" autocomplete="off">'+
  '<label class="toggleRow"><span>加强版算法（加载 so，可能崩溃）</span><input id="dexAdvanced" type="checkbox"></label>'+
  '<label for="dexClasses">需要解密的类（每行一个，留空全部）</label>'+
  '<textarea id="dexClasses" rows="2" placeholder="com.foo.*\n!com.foo.util.Bar" spellcheck="false"></textarea>'+
  '<p class="signHint">支持 JCE 块加密（AES/DES）与字节表 XOR。native JNI 解码无法纯静态处理，建议用 Frida 动态提取。</p>',
  [['取消',closeModal],['解密',async()=>{
   const custom=$('#dexCustom').value.trim();
   const advanced=$('#dexAdvanced').checked;
   const classes=$('#dexClasses').value.trim();
   const buttons=d.querySelectorAll('.actions button');[].forEach.call(buttons,b=>b.disabled=true);
   try{
    closeModal();
    const r=await rpc('dexDecrypt',{path:info.path,custom,advanced,classes});
    let msg='扫描 dex：候选解密器 '+(r.candidates||0)+'，调用点 '+(r.callSites||0)+'，解出 '+(r.decrypted||0)+'，跳过 '+(r.skipped||0);
    if((r.errors||[]).length)msg+='\n错误：'+(r.errors||[]).slice(0,3).join(' | ');
    if((r.samples||[]).length)msg+='\n示例：\n'+(r.samples||[]).slice(0,5).join('\n');
    alertBox('DEX 字符串解密',msg);
   }finally{[].forEach.call(buttons,b=>b.disabled=false);if(d.isConnected)closeModal(false);}
  }]]);
}
function apkDexDeobfuscateDialog(info){
 const d=dialog('DEX 混淆对抗',
  '<p class="signHint">把干扰符号/单字母/超长名/非拉丁符号重命名为可读的 abc 顺序名，保留内部类关系。不改动方法体。</p>'+
  '<label for="debPattern">自定义匹配规则（正则，留空用默认：单字母/干扰符号/非拉丁/超长名）</label>'+
  '<input id="debPattern" placeholder="^(oO0|l1I).*" spellcheck="false" autocomplete="off">'+
  '<label for="debClasses">需要处理的类（每行一个，留空全部；支持 包名.* / 包名.** / ! 排除）</label>'+
  '<textarea id="debClasses" rows="3" placeholder="com.foo.*\n!com.foo.util.Bar" spellcheck="false"></textarea>',
  [['取消',closeModal],['对抗',async()=>{
   const custom=$('#debPattern').value.trim();
   const lines=$('#debClasses').value.split('\n').map(s=>s.trim()).filter(s=>s.length);
   const buttons=d.querySelectorAll('.actions button');[].forEach.call(buttons,b=>b.disabled=true);
   try{
    closeModal();
    const r=await rpc('dexDeobfuscate',{path:info.path,custom,targetClasses:lines});
    let msg='重命名：类 '+(r.classesRenamed||0)+'，方法 '+(r.methodsRenamed||0)+'，字段 '+(r.fieldsRenamed||0);
    if((r.errors||[]).length)msg+='\n错误：'+(r.errors||[]).slice(0,3).join(' | ');
    if((r.renamedClasses||[]).length)msg+='\n示例：'+(r.renamedClasses||[]).slice(0,5).join('、');
    alertBox('DEX 混淆对抗',msg);
   }finally{[].forEach.call(buttons,b=>b.disabled=false);if(d.isConnected)closeModal(false);}
  }]]);
}
function settleOpenProject(){return new Promise(resolve=>{dialog('未保存的 DEX 工程','<p>打开另一个 APK 或 DEX 之前，先保存当前工程？</p>',[['取消',()=>{modalCancel=null;closeModal();resolve(false);}],['放弃',()=>{state.dex.modified=false;modalCancel=null;closeModal();resolve(true);}],['保存',async()=>{await saveProject();modalCancel=null;closeModal();resolve(true);}]],()=>resolve(false));});}
async function openDexProject(args,entries){const result=await rpc('dexOpen',{...args,entries});state.dex={prefix:'',tab:'explorer',history:[],source:args,mode:'auto',query:'',count:result.count,expanded:new Set(),treeCache:new Map(),scroll:0};state.screen='dex';await renderDex();return result;}
async function chooseDex(args,preferred='',after=''){if(state.dex.modified&&!await settleOpenProject())return;const names=await rpc('dexFiles',args);if(!names.length)throw Error('未找到 classes.dex 文件。');const ordered=preferred&&names.includes(preferred)?[preferred].concat(names.filter(n=>n!==preferred)):names;const d=dialog('DEX 工程',`<p class="dexPickHint">选择要加入工程的 DEX 文件，可多选。</p><div class="dexPickTools"><button type="button" data-pick="all">全选</button><button type="button" data-pick="invert">反选</button><span class="dexPickCount" aria-live="polite"></span></div><div class="dexPickList">${ordered.map(n=>`<label class="dexPickRow"><input type="checkbox" data-dex="${esc(n)}" checked><span class="dexPickIcon">${icon('dex')}</span><span class="dexPickName">${esc(n)}</span></label>`).join('')}</div>`,[['取消',closeModal],['打开工程',async()=>{const entries=Array.from(d.querySelectorAll('input:checked')).map(x=>x.dataset.dex);if(!entries.length)return toast('请至少选择一个 DEX');closeModal();await openDexProject(args,entries);if(after)await after();}]]);d.classList.add('multidexDialog');const boxes=Array.from(d.querySelectorAll('input[type=checkbox]')),count=d.querySelector('.dexPickCount'),open=d.querySelector('.actions button:last-child');const sync=()=>{const n=boxes.filter(x=>x.checked).length;count.textContent='已选 '+n+' / '+boxes.length;open.disabled=!n;};boxes.forEach(x=>x.onchange=sync);d.querySelector('[data-pick="all"]').onclick=()=>{boxes.forEach(x=>x.checked=true);sync();};d.querySelector('[data-pick="invert"]').onclick=()=>{boxes.forEach(x=>x.checked=!x.checked);sync();};sync();}
function dexDirectArgs(f){return f.archive?{path:f.archive,archive:f.archive,entry:f.entry}:{path:f.path};}
function dexFileOptions(f){
 const args=f.archive?{path:f.archive}:fileArgs(f);
 options(f.name,[
  {icon:'dex',label:'Dex 编辑器 Plus',run:()=>chooseDex(args,f.name)},
  {icon:'build',label:'修复 DEX',run:()=>dexRepair(f)},
  {icon:'info',label:'DEX 属性',run:()=>dexProperties(f)},
  {icon:'code',label:'DEX 转 Smali',run:()=>dexToSmali(f)},
  {icon:'zip',label:'DEX 转 JAR',run:()=>dexToJar(f)},
  {icon:'text',label:'翻译模式',run:()=>chooseDex(args,f.name,dexTranslation)},
  {icon:'edit',label:'替换字符串',run:()=>dexReplaceDialog(f)}]);
}
async function dexRows(prefix){if(!state.dex.treeCache)state.dex.treeCache=new Map();if(!state.dex.treeCache.has(prefix))state.dex.treeCache.set(prefix,await rpc('dexTree',{prefix}));return state.dex.treeCache.get(prefix);}
async function dexBranch(container,prefix){
 const rows=await dexRows(prefix);if(!container.isConnected)return;
 if(!rows.length){container.innerHTML='<div class="treeMessage">此包中没有类。</div>';return;}
 for(const row of rows){
  const b=document.createElement('button');b.className='treeRow'+(row.dir?'':' classRow');b.setAttribute('role','treeitem');
  if(row.dir){
   const expanded=state.dex.expanded.has(row.prefix);b.setAttribute('aria-expanded',String(expanded));b.dataset.prefix=row.prefix;
   b.innerHTML='<span class="chevron">'+icon('forward')+'</span><span class="fileicon">'+icon('folder')+'</span><span class="treename">'+esc(row.name)+'</span>';
   const children=document.createElement('div');children.className='treeChildren';children.setAttribute('role','group');children.hidden=!expanded;
   b.onclick=()=>act(async()=>{const open=b.getAttribute('aria-expanded')!=='true';b.setAttribute('aria-expanded',String(open));expandGroup(children,open);if(open){state.dex.expanded.add(row.prefix);if(!children.children.length){children.innerHTML='<div class="treeMessage">加载中…</div>';try{const list=await dexRows(row.prefix);children.replaceChildren();await dexBranch(children,row.prefix);if(b.getAttribute('aria-expanded')==='true')expandGroup(children,true);}catch(error){children.innerHTML='<div class="treeMessage">'+esc(error.message)+'</div>';throw error;}}}else state.dex.expanded.delete(row.prefix);});
   container.append(b,children);if(expanded)await dexBranch(children,row.prefix);
  }else{b.innerHTML='<span class="classicon">C</span><span class="treename">'+esc(row.name)+'</span>';b.onclick=()=>act(()=>openClass(row.class,'smali'));container.append(b);}
 }
}
async function renderDex(){
 state.screen='dex';if(!state.dex.expanded)state.dex.expanded=new Set();
 header('DEX 编辑器','临时工程',[{icon:'build',label:'保存工程',run:closeDex},...(state.dex.tab==='strings'?[{icon:'text',label:'翻译全部',run:dexTranslateAll}]:[]),{icon:'more',label:'DEX 菜单',run:()=>menu([{icon:'save',label:'保存工程',run:saveProject},{icon:'check',label:'保存并签名 APK',run:saveAndSignProject},{icon:'export',label:'导出 DEX / APK',run:exportDex},{icon:'settings',label:'设置',run:preferences},{icon:'close',label:'关闭工程',run:closeDex}])}]);
 const tabLabels={explorer:'浏览',history:'历史',search:'搜索',strings:'字符串'};
  $('#subbar').innerHTML='<div class="tabs" role="tablist">'+['explorer','history','search','strings'].map(t=>`<button role="tab" aria-selected="${state.dex.tab===t}" class="tab ${state.dex.tab===t?'active':''}" data-tab="${t}">${tabLabels[t]}</button>`).join('')+'</div>';
 document.querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>act(async()=>{const order=['explorer','history','search','strings'],direction=order.indexOf(b.dataset.tab)>order.indexOf(state.dex.tab)?1:-1;state.dex.tab=b.dataset.tab;await renderDex();animateContent($('.tree'),direction);}));$('#bottom').replaceChildren();$('#content').classList.remove('javaEditor');
 $('#content').innerHTML='<div class="tree"></div>';const tree=$('.tree');
 if(state.dex.tab==='explorer'){tree.setAttribute('role','tree');tree.setAttribute('aria-label','DEX 包与类');await dexBranch(tree,'');tree.scrollTop=state.dex.scroll||0;tree.onscroll=()=>state.dex.scroll=tree.scrollTop;return;}
 let rows;
 if(state.dex.tab==='history')rows=state.dex.history.map(x=>({name:x.substring(1,x.length-1).replace(/\//g,'.'),class:x}));
 else{const form=document.createElement('div');form.className='searchline';form.innerHTML=`<input aria-label="${state.dex.tab==='strings'?'筛选字符串':'搜索类、方法或字段'}" placeholder="${state.dex.tab==='strings'?'筛选字符串…':'类、方法或字段…'}" value="${esc(state.dex.query)}"><button>搜索</button>`;const search=()=>act(async()=>{state.dex.query=form.querySelector('input').value;await renderDex();});form.querySelector('button').onclick=search;form.querySelector('input').onkeydown=e=>{if(e.key==='Enter')search();};tree.append(form);if(state.dex.tab==='strings'){const hint=document.createElement('div');hint.className='bundleHint';hint.textContent='点击字符串即可翻译并写回 DEX；保存工程后生效。';tree.append(hint);}rows=await rpc(state.dex.tab==='strings'?'dexStrings':'dexSearch',{query:state.dex.query});}
 if(!tree.isConnected)return;if(!rows.length){tree.insertAdjacentHTML('beforeend','<div class="empty">'+(state.dex.tab==='history'?'已打开的类将显示在此处。':state.dex.query?'无结果':'在上方输入搜索内容。')+'</div>');return;}
 for(const row of rows){const b=document.createElement('button');b.className='treeRow classRow';if(typeof row==='string'){b.innerHTML='<span class="treename">'+esc(row)+'</span>';b.onclick=()=>act(()=>dexEditString(row));}else{b.innerHTML='<span class="classicon">C</span><span class="treename">'+esc(row.name)+'</span>';b.onclick=()=>act(()=>openClass(row.class,'smali'));}tree.append(b);}
}
async function saveProject(){const result=await rpc('dexCommit');state.dex.modified=false;toast('工程已保存。备份：'+result.backup);return result;}
async function saveAndSignProject(){
 if(state.screen==='editor'&&!await canLeave())return false;
 const source=state.dex.source&&(state.dex.source.archive||state.dex.source.path||state.dex.source);if(!source||source.endsWith('.dex')){await saveProject();return toast('DEX 已保存。打开其 APK 即可签名可安装的包。');}
 const result=await rpc('dexSaveSign');state.dex.modified=false;
 dialog('APK 已保存并校验','<p class="break">'+esc(result.path)+'</p><p>V1 + V2 + V3 签名已校验。</p><p class="muted">原始备份：'+esc(result.backup)+'</p>',[['关闭',closeModal],['安装',()=>installApk(result.path)]]);return true;
}
async function closeDex(){if(!state.dex.modified){state.screen='files';await refresh();return;}const d=dialog('保存工程','<p>将编译的更改保存到原文件？会保留备份。</p>',[['保存并签名 APK',async()=>{if(await saveAndSignProject()){state.screen='files';await refresh();}}],['保存并退出',async()=>{d.querySelectorAll('button').forEach(b=>b.disabled=true);try{await saveProject();closeModal();state.screen='files';await refresh();}finally{if(d.isConnected)d.querySelectorAll('button').forEach(b=>b.disabled=false);}}],['取消',closeModal],['直接退出',async()=>{state.dex.modified=false;closeModal();state.screen='files';await refresh();}]]);d.classList.add('stackedActions');}
async function exportDex(){const folder=await ask('导出到文件夹',state.info.permission?state.info.home+'/下载':state.info.workspace);if(!folder)return;const result=await rpc('dexExport',{destination:folder});alertBox('导出完成',result.path+(result.needsSigning?'\n安装前请先签名此 APK 副本。':''));}
function dexFolder(){return state.info&&state.info.permission?state.info.home+'/下载':state.info.workspace;}
async function dexRepair(f){
 const body=f.archive?'将用 dexlib2 重新生成并校验此 APK 中的 '+esc(f.entry)+'，写回后重新签名 APK，并保留 .bak 备份。':'将用 dexlib2 重新生成并校验此 DEX，直接写回并保留 .bak 备份。';
 if(!await confirmBox('修复 DEX',body,'修复'))return;
 const r=await rpc('dexDirectRepair',dexDirectArgs(f));
 if(state.screen==='files')await refresh();
 alertBox('DEX 已修复',r.path+'\n'+r.message+'\n文件：'+r.count+'\n'+(r.files||[]).map(x=>x.name+' · '+x.classes+' 个类').join('\n')+'\n备份：'+r.backup+(r.signed?'\nAPK 已重新签名并校验。':''));
}
async function dexProperties(f){
 const r=await rpc('dexDirectProperties',dexDirectArgs(f));
 const details=r.files.map(x=>x.name+' · DEX '+x.version+' · API '+(x.api>=0?x.api:'未知')+'\nchecksum '+x.checksum+'\nSHA-1 '+x.signature).join('\n\n');
 alertBox('DEX 属性',r.path+'\n大小：'+size(r.size)+'\n字符串：'+r.strings+'\n类型：'+r.types+'\n类：'+r.classes+'\n方法：'+r.methods+'\n字段：'+r.fields+'\n\n'+details);
}
async function dexToSmali(f){
 const folder=await ask('DEX 转 Smali',dexFolder(),'输出文件夹（需已存在）');if(!folder)return;
 const r=await rpc('dexDirectToSmali',{...dexDirectArgs(f),destination:folder});
 dialog('Smali 已导出','<p class="break">'+esc(r.path)+'</p><p>共 '+r.smali+' 个 .smali 文件。</p><p class="bundleHint">直接从所选的 DEX 导出，原文件未修改。</p>',[['关闭',closeModal],['打开文件夹',async()=>{closeModal();await navigate(state.active,{path:folder,archive:'',entry:''});}]]);
}
async function dexToJar(f){
 const folder=await ask('DEX 转 JAR',dexFolder(),'输出文件夹（需已存在）');if(!folder)return;
 const r=await rpc('dexDirectToJar',{...dexDirectArgs(f),destination:folder});
 dialog('JAR 已生成','<p class="break">'+esc(r.path)+'</p><p>'+r.classes+' 个 .class，可用 JD-GUI 等工具查看。</p><p class="bundleHint">直接从所选的 DEX 生成，原文件未修改。</p>',[['关闭',closeModal],['打开文件夹',async()=>{closeModal();await navigate(state.active,{path:folder,archive:'',entry:''});}]]);
}
async function dexTranslation(){
 if(!translateLanguages.length)loadTranslateConfig().catch(()=>{});
 const rows=await rpc('dexStrings',{query:''});
 const list=(rows||[]).filter(x=>typeof x==='string');
 if(!list.length)return toast('没有可翻译的字符串。');
 const dict=pref.dexDict||(pref.dexDict={});
 state.previous=state.screen==='tool'?state.previous:state.screen;
 state.dexTranslate={rows:list.map(s=>({source:s,target:dict[s]||''})),query:'',lang:(translateLanguages[0]&&translateLanguages[0].key)||'en',name:(state.dex.source&&(state.dex.source.name||state.dex.source.path||'').split('/').pop())||'DEX 工程',sel:new Set(),multi:false,rangeOn:false,anchor:null};
 await dexTranslateRender();
}
function dexTranslatePairs(){const t=state.dexTranslate;if(!t)return [];const q=t.query.toLowerCase();return t.rows.filter(r=>!q||r.source.toLowerCase().includes(q)||r.target.toLowerCase().includes(q));}
async function dexTranslateRender(){
 const t=state.dexTranslate;if(!t)return;
 state.screen='tool';
 const done=t.rows.filter(r=>r.target&&r.target!==r.source).length;
 header('翻译模式',(t.sel&&t.sel.size?t.sel.size+' 已选 / ':'')+done+'/'+t.rows.length,[{icon:'back',label:'退出',run:dexTranslateExit},{icon:'text',label:'翻译全部',run:dexTlTranslateAll},{icon:'more',label:'菜单',run:dexTlMenu}],true);
 $('#subbar').innerHTML='';
 const mb=iconButton(t.multi?'close':'plus',t.multi?'退出多选':'多选',dexTlToggleMulti),rb=iconButton(t.rangeOn?'close':'swap',t.rangeOn?'退出连选':'连选',dexTlToggleRange);
 if(t.multi)mb.classList.add('on');if(t.rangeOn)rb.classList.add('on');
 $('#bottom').replaceChildren(iconButton('check','全选',dexTlSelectAll),iconButton('switch','反选',dexTlInvertSel),mb,iconButton('text','翻译',dexTlTranslateSel),rb);
 $('#content').innerHTML=`<div class="toolpage dexTlPage">
  <div class="searchline"><input id="dexTlQuery" placeholder="过滤原文或译文…" value="${esc(t.query)}"><button id="dexTlGo">过滤</button></div>
  <table class="dexTlTable"><tbody id="dexTlRows"></tbody></table></div>`;
 const box=$('#dexTlRows');
 const pairs=dexTranslatePairs();
 if(!pairs.length)box.insertAdjacentHTML('beforeend','<tr><td colspan="2" class="empty">没有匹配的字符串</td></tr>');
 for(const r of pairs){
  const row=document.createElement('tr');
  if(t.sel&&t.sel.has(r))row.classList.add('tlSel');
  row.innerHTML=`<td class="tlSource">${esc(r.source)}</td><td class="tlTargetCell"><input class="tlTarget" value="${esc(r.target)}" placeholder="（未翻译）" spellcheck="false" autocapitalize="off" data-src="${esc(r.source)}"></td>`;
  const input=row.querySelector('input');
  input.oninput=()=>{r.target=input.value;};
  row.querySelector('.tlSource').onclick=()=>dexTlRowTap(r);
  box.append(row);
 }
 const go=()=>act(async()=>{t.query=$('#dexTlQuery').value.trim();await dexTranslateRender();});
 $('#dexTlGo').onclick=go;$('#dexTlQuery').onkeydown=e=>{if(e.key==='Enter')go();};
}
function dexTlRowTap(r){
 const t=state.dexTranslate;if(!t)return;
 if(!t.multi&&!t.rangeOn)return act(()=>dexTlEdit(r));
 const p=dexTranslatePairs();
 if(t.rangeOn){
  if(t.anchor==null){t.anchor=p.indexOf(r);t.sel=new Set([r]);}
  else{const b=p.indexOf(r);if(b<0)return;const lo=Math.min(t.anchor,b),hi=Math.max(t.anchor,b);t.sel=new Set(p.slice(lo,hi+1));t.anchor=null;toast('已选中 '+(hi-lo+1)+' 条');}
 }else{if(t.sel.has(r))t.sel.delete(r);else t.sel.add(r);}
 act(dexTranslateRender);
}
function dexTlSelectAll(){const t=state.dexTranslate;if(!t)return;t.sel=new Set(dexTranslatePairs());act(dexTranslateRender);}
function dexTlInvertSel(){const t=state.dexTranslate;if(!t)return;const n=new Set();for(const r of dexTranslatePairs())if(!t.sel.has(r))n.add(r);t.sel=n;act(dexTranslateRender);}
function dexTlToggleMulti(){const t=state.dexTranslate;if(!t)return;t.multi=!t.multi;if(t.multi)t.rangeOn=false;t.anchor=null;act(dexTranslateRender);toast(t.multi?'多选已开启：点原文选择/取消，＋变 ×':'多选已关闭');}
function dexTlToggleRange(){const t=state.dexTranslate;if(!t)return;t.rangeOn=!t.rangeOn;if(t.rangeOn)t.multi=false;t.anchor=null;act(dexTranslateRender);toast(t.rangeOn?'连选已开启：点第一项再点第二项，选中其之间全部':'连选已关闭');}
function dexTlMenu(){menu([
 {icon:'settings',label:'翻译服务',run:()=>act(translatePage)},
 {icon:'save',label:'保存',run:dexTlSave},
 {icon:'layers',label:'保存到字典',run:dexTlSaveDict},
 {icon:'plugin',label:'字典管理',run:dexTlDict},
 {icon:'search',label:'过滤',run:dexTlFilter},
 {icon:'swap',label:'替换',run:dexTlReplace},
 {icon:'check',label:'检查',run:dexTlCheck},
 {icon:'network',label:'语言',run:dexTlLanguage},
 {icon:'close',label:'退出',run:dexTranslateExit}]);}
async function dexTlEdit(r){
 const body=`<label>原文</label><pre class="diffText">${esc(r.source)}</pre><label>译文</label><textarea id="dexTlText" rows="4" spellcheck="false">${esc(r.target)}</textarea><p class="bundleHint">将替换工程 DEX 中相同的字符串字面量；「保存」后写入工程。</p>`;
 const d=dialog('编辑翻译',body,[['取消',closeModal],['保存',()=>{r.target=d.querySelector('#dexTlText').value;closeModal();act(dexTranslateRender);}]]);
 const actions=d.querySelector('.actions'),btn=document.createElement('button');
 btn.textContent='翻译';
 btn.onclick=()=>act(async()=>{const input=d.querySelector('#dexTlText');btn.disabled=true;btn.textContent='翻译中…';try{const text=await translateText(r.source,state.dexTranslate.lang);if(text)input.value=text,r.target=text;else toast('翻译结果为空');}catch(e){alertBox('无法完成',e.message);}finally{btn.disabled=false;btn.textContent='翻译';}});
 actions.insertBefore(btn,actions.firstChild);
 setTimeout(()=>{const ta=d.querySelector('textarea');if(ta&&ta.isConnected){ta.focus();ta.setSelectionRange(0,0);}},30);
}
async function dexTlSave(){
 const t=state.dexTranslate;if(!t)return;
 const map={};for(const r of t.rows)if(r.target&&r.target!==r.source)map[r.source]=r.target;
 const n=Object.keys(map).length;
 if(!n)return toast('没有修改过的译文。');
 const r=await rpc('dexReplaceStrings',{replacements:map});
 state.dex.modified=true;t.dirty=true;
 toast('已写回 '+n+' 条（替换 '+r.replaced+' 处），保存工程后生效。');
}
async function dexTlSaveDict(){
 const t=state.dexTranslate;if(!t)return;
 const dict=pref.dexDict||(pref.dexDict={});let n=0;
 for(const r of t.rows)if(r.target&&r.target!==r.source){if(dict[r.source]!==r.target){dict[r.source]=r.target;n++;}}
 savePref();toast(n?'已保存 '+n+' 条到字典。':'字典已是最新。');
}
async function dexTlDict(){
 const dict=pref.dexDict||{};const keys=Object.keys(dict);
 if(!keys.length)return toast('字典为空。先翻译并「保存到字典」。');
 const body=`<p class="bundleHint">共 ${keys.length} 条。译文会在打开翻译模式时自动填入相同原文。</p><div class="dexTlDictList">${keys.slice(0,300).map(k=>`<div class="dexTlDictRow" data-k="${esc(k)}"><span class="main">${esc(k)}<small>${esc(dict[k])}</small></span><button class="dexTlDictDel" data-k="${esc(k)}">删除</button></div>`).join('')}</div>`;
 const d=dialog('字典管理',body,[['清空字典',()=>{if(Object.keys(pref.dexDict||{}).length&&!pref.dexDictAsk){pref.dexDictAsk=true;closeModal();confirmBox('清空字典？','将删除全部 '+Object.keys(pref.dexDict).length+' 条译文。','清空').then(ok=>{if(ok){pref.dexDict={};savePref();toast('字典已清空');}});}else return toast('字典为空。');}],['关闭',closeModal]]);
 d.querySelectorAll('.dexTlDictDel').forEach(b=>b.onclick=()=>{delete pref.dexDict[b.dataset.k];savePref();b.closest('.dexTlDictRow').remove();toast('已删除');});
}
async function dexTlFilter(){
 const t=state.dexTranslate;if(!t)return;
 const q=await ask('过滤字符串',t.query,'包含的文本（原文或译文）');if(q===null)return;
 t.query=q.trim();await dexTranslateRender();
}
async function dexTlReplace(){
 const t=state.dexTranslate;if(!t)return;
 const d=dialog('批量替换译文','<label>查找</label><input id="dexTlFind" spellcheck="false"><label>替换为</label><input id="dexTlTo" spellcheck="false"><p class="bundleHint">只修改下方列表中的译文，不改原文。</p>',[['取消',closeModal],['替换',()=>{const find=d.querySelector('#dexTlFind').value,to=d.querySelector('#dexTlTo').value;if(!find)return toast('请输入查找内容');let n=0;for(const r of t.rows)if(r.target&&r.target.includes(find)){r.target=r.target.split(find).join(to);n++;}closeModal();toast('已替换 '+n+' 条译文');act(dexTranslateRender);}]]);
 setTimeout(()=>{const i=d.querySelector('#dexTlFind');if(i&&i.isConnected)i.focus();},30);
}
async function dexTlCheck(){
 const t=state.dexTranslate;if(!t)return;
 const total=t.rows.length,translated=t.rows.filter(r=>r.target&&r.target!==r.source).length,same=t.rows.filter(r=>r.target&&r.target===r.source).length;
 alertBox('翻译检查','总词条：'+total+'\n已翻译：'+translated+'\n未翻译：'+(total-translated-same)+'\n译文与原文相同：'+same+(state.dex.modified?'\n\n工程有未保存的修改，记得「保存」。':''));
}
async function dexTlLanguage(){
 const t=state.dexTranslate;if(!t)return;
 const langs=(translateLanguages||[]).length?translateLanguages:[{key:'en',name:'英语'},{key:'zh',name:'中文'},{key:'ja',name:'日语'}];
 options('目标语言',langs.map(l=>({icon:'network',label:l.name+(l.key===t.lang?' ✓':''),run:()=>{t.lang=l.key;toast('目标语言：'+l.name);}})));
}
async function dexTlAdd(){
 const t=state.dexTranslate;if(!t)return;
 const d=dialog('添加词条','<label>原文</label><input id="dexTlAddSrc" spellcheck="false"><label>译文</label><input id="dexTlAddTo" spellcheck="false"><p class="bundleHint">原文必须与 DEX 中的字符串完全一致才会被替换。</p>',[['取消',closeModal],['添加',()=>{const src=d.querySelector('#dexTlAddSrc').value,to=d.querySelector('#dexTlAddTo').value;if(!src)return toast('请输入原文');if(t.rows.some(r=>r.source===src))return toast('该原文已存在');t.rows.unshift({source:src,target:to});closeModal();act(dexTranslateRender);}]]);
 setTimeout(()=>{const i=d.querySelector('#dexTlAddSrc');if(i&&i.isConnected)i.focus();},30);
}
async function dexTlTranslateAll(){
 const t=state.dexTranslate;if(!t)return;
 const list=t.rows.filter(r=>!r.target);
 if(!list.length)return toast('没有未翻译的词条。');
 await dexTlRun(list);
}
async function dexTlTranslateSel(){
 const t=state.dexTranslate;if(!t)return;
 if(!t.sel||!t.sel.size)return toast('尚未选择词条。先「多选」或「连选」选中，再点翻译。');
 const list=[...t.sel].filter(r=>!r.target);
 if(!list.length)return toast('所选词条均已翻译。');
 await dexTlRun(list);
}
async function dexTlRun(list){
 const t=state.dexTranslate;if(!t)return;
 if(!await confirmBox('批量翻译？',`将翻译 ${list.length} 条字符串（目标语言：${translateTargetName(t.lang)}）。逐条调用翻译服务，可能较慢。`,'翻译'))return;
 let done=0,fail=0;
 for(const r of list){
  try{const text=await translateText(r.source,t.lang);if(!text)throw Error('空');r.target=text;done++;}
  catch(error){fail++;}
 }
 t.sel=new Set();t.anchor=null;t.multi=false;t.rangeOn=false;
 await dexTranslateRender();
 toast('已翻译 '+done+' 条'+(fail?'，失败 '+fail+' 条':''));
}
async function dexTranslateExit(){
 const t=state.dexTranslate;if(!t)return;
 if(t.dirty&&!await confirmBox('退出翻译？','译文已写回工程但工程尚未保存。','继续退出'))return;
 state.dexTranslate=null;
 state.screen='files';$('#content').classList.remove('javaEditor');$('#subbar').innerHTML='';await refresh();renderFiles();
}
async function dexTranslateAll(){
 const rows=await rpc('dexStrings',{query:state.dex.query});
 const list=(rows||[]).filter(x=>typeof x==='string'&&x.trim());
 if(!list.length)return toast('没有可翻译的字符串。');
 if(!await confirmBox('批量翻译？',`将翻译 ${list.length} 条字符串并写入当前工程，保存工程后生效。`,'翻译'))return;
 const map={};let done=0,fail=0;
 for(const original of list){
  try{const text=await translateText(original);if(!text)throw Error('空');map[original]=text;done++;}
  catch(error){fail++;}
 }
 if(Object.keys(map).length){await rpc('dexReplaceStrings',{replacements:map});state.dex.modified=true;}
 await renderDex();
 toast('已翻译 '+done+' 条'+(fail?'，失败 '+fail+' 条':''));
}
function dexEditString(original){
 const d=dialog('翻译字符串','<label>原始字符串</label><pre class="diffText">'+esc(original)+'</pre><label>修改后的字符串</label><textarea id="dexStringValue" rows="6" spellcheck="false">'+esc(original)+'</textarea><p class="bundleHint">将替换所有 DEX 中相同的字符串字面量；保存工程后写入原文件。</p>',[['取消',closeModal],['应用',async()=>{const now=d.querySelector('#dexStringValue').value;if(now===original){closeModal();return;}const r=await rpc('dexReplaceStrings',{replacements:{[original]:now}});state.dex.modified=true;closeModal();toast('已替换 '+r.replaced+' 处，涉及 '+r.classes+' 个方法');await renderDex();}]]);
 const actions=d.querySelector('.actions'),btn=document.createElement('button');
 btn.textContent='翻译';
 btn.onclick=()=>act(async()=>{const input=$('#dexStringValue');if(!input)return;btn.disabled=true;btn.textContent='翻译中…';try{const r=await translateText(original);if(r)input.value=r;else toast('翻译结果为空');}catch(e){alertBox('无法完成',e.message);}finally{btn.disabled=false;btn.textContent='翻译';}});
 actions.insertBefore(btn,actions.firstChild);
 setTimeout(()=>{const t=d.querySelector('textarea');if(t&&t.isConnected){t.focus();t.setSelectionRange(0,0);}},30);
}
function dexReplaceDialog(f){
 const d=dialog('替换字符串','<label>查找</label><input id="dexFind" spellcheck="false" autocapitalize="off"><label>替换为</label><input id="dexReplace" spellcheck="false" autocapitalize="off"><label class="checkrow"><input id="dexMatch" type="checkbox" checked><span>区分大小写</span></label><p class="bundleHint">只修改字符串字面量；直接写回所选 DEX 并保留 .bak 备份，APK 会重新签名。</p>',[['取消',closeModal],['替换',()=>act(async()=>{const find=d.querySelector('#dexFind').value,replace=d.querySelector('#dexReplace').value,matchCase=d.querySelector('#dexMatch').checked;if(!find)return toast('请输入查找内容');const r=await rpc('dexDirectReplaceStrings',{...dexDirectArgs(f),find,replace,matchCase});if(!r.changed){closeModal();return toast('未找到匹配的字符串。');}closeModal();if(state.screen==='files')await refresh();alertBox('字符串已替换',r.path+'\n替换 '+r.replaced+' 处，涉及 '+r.classes+' 个方法。\n备份：'+r.backup+(r.signed?'\nAPK 已重新签名并校验。':''));})]]);
 setTimeout(()=>{const t=d.querySelector('#dexFind');if(t&&t.isConnected)t.focus();},30);
}
async function dexMergeFiles(files){
 const dexFiles=files.filter(x=>fileType(x)==='dex');
 if(dexFiles.length<2)return toast('请至少选择两个 DEX 文件。');
 const folder=await ask('合并 DEX',dexFolder(),'输出文件夹（需已存在）');if(!folder)return;
 const local=dexFiles.filter(f=>!f.archive).map(f=>f.path);
 const packed=dexFiles.filter(f=>f.archive);
 const args={destination:folder};
 if(local.length)args.paths=local;
 if(packed.length){args.archive=packed[0].archive;args.entries=packed.map(f=>f.entry);}
 const r=await rpc('dexMerge',args);
 alertBox('DEX 已合并',r.path+'\n合并 '+r.dexes+' 个 DEX，共 '+r.classes+' 个类。');
}
const ARSC_TYPES=[[0,'字符串',3],[1,'引用',1],[2,'属性',2],[3,'颜色',28],[4,'整数',16],[5,'十六进制',17],[6,'布尔',18],[7,'浮点',4],[8,'尺寸',5],[9,'分数',6],[10,'动态引用',7]];
function arscHex(v){return '0x'+((Number(v)||0)>>>0).toString(16).padStart(8,'0');}
function arscTypeLabel(t){const x=ARSC_TYPES.find(a=>a[2]===t||(t>=28&&t<=31&&a[2]===28));return x?x[1]:'0x'+(Number(t)||0).toString(16);}
function arscChoice(t){let i=ARSC_TYPES.findIndex(a=>a[2]===t);if(i<0&&t>=28&&t<=31)i=3;return i<0?0:i;}
function arscOptions(selected){return ARSC_TYPES.map((a,i)=>`<option value="${i}" ${i===selected?'selected':''}>${a[1]}</option>`).join('');}
function arscOpenWith(args,name){
  const modes=[['plus','Arsc编辑器++'],['editor','Arsc编辑器'],['translate','翻译模式'],['query','资源查询']];
  const body='<div class="arscChooser">'+modes.map((m,i)=>`<label><input type="radio" name="arscOpenMode" value="${m[0]}" ${i===0?'checked':''}><span>${m[1]}</span></label>`).join('')+'</div>';
  dialog('打开方式…',body,[['取消',closeModal],['打开',()=>{
    const mode=document.querySelector('input[name="arscOpenMode"]:checked').value;
    closeModal();
    if(mode==='query')return showArscQuery(args,name);
    if(mode==='translate')return showArscTranslate(args,name);
    if(mode==='plus')return showArsc(args,name,'plus');
    return showArsc(args,name,'editor');
  }]]);
}
async function showArsc(args,name,mode){
  const s=await rpc('arscOpen',Object.assign({},args,{name:name||''}));
  state.arsc={session:s.session,name:s.name||name||'resources.arsc',size:s.size||0,archive:s.archive||'',entry:s.entry||'',packages:s.packages||[],stringCount:s.stringCount||0,level:'root',pkgId:-1,typeId:-1,pkgName:'',typeName:'',config:'',configLabel:'',offset:0,limit:200,query:'',rows:[],entryData:null,dirty:false,mode:mode||'editor',tab:'explore',treeOpen:{},treeConfigs:{},treeEntries:{},history:[],search:{query:'',type:'value',caseSensitive:false,regex:false,path:'/'},poolFilter:''};
  state.previous=state.screen==='tool'?state.previous:'files';state.screen='tool';
  await arscRender();
}
function arscHeader(){
  const a=state.arsc;if(!a)return;
  const plus=a.mode==='plus';
  let title=plus?'Arsc编辑器++':'Arsc编辑器',sub=a.name;
  if(plus&&a.level==='entry'&&a.entryData){title=a.entryData.name||'资源';sub=(a.entryData.package?a.entryData.package+':':'')+(a.entryData.type||'')+' · '+(a.entryData.configLabel||a.entryData.config||'默认');}
  if(!plus){
    if(a.level==='package'){title=a.pkgName;sub='包 '+arscHex(a.pkgId)+' · '+a.name;}
    else if(a.level==='type'){title=a.typeName;sub='类型 '+arscHex(a.typeId)+' · '+a.name;}
    else if(a.level==='config'){title=a.configLabel||'默认配置';sub=a.typeName+' · '+a.name;}
    else if(a.level==='entry'){title=a.entryData?a.entryData.name:'资源';sub=a.entryData?arscHex(a.entryData.id):a.name;}
    else if(a.level==='pool'){title='字符常量池';sub=a.name+' · '+a.stringCount+' 项';}
    else if(a.level==='search'){title='搜索结果';sub=a.query||'';}
  }
  const actions=plus
    ?[{icon:'back',label:'返回',run:arscBack},{icon:'build',label:'保存资源表',run:arscSave},{icon:'more',label:'更多',run:arscPlusMenu}]
    :[{icon:'back',label:a.level==='root'?'关闭资源表':'返回',run:arscBack},{icon:'search',label:'搜索资源',run:()=>options('搜索资源',[{icon:'search',label:'按资源名搜索',run:()=>arscSearchDialog('name')},{icon:'pin',label:'按资源 ID 搜索',run:()=>arscSearchDialog('id')},{icon:'text',label:'按值搜索',run:()=>arscSearchDialog('value')}])},{icon:'save',label:'保存资源表',run:arscSave},{icon:'more',label:'资源表菜单',run:arscMenu}];
  header(title,sub,actions,false);
  $('#subbar').innerHTML=a.mode==='plus'?arscPlusTabs(a):'';
  if(a.mode==='plus')document.querySelectorAll('[data-arsc-tab]').forEach(b=>b.onclick=()=>act(async()=>{a.tab=b.dataset.arscTab;a.level='root';a.entryData=null;a.offset=0;await arscRender();}));
  $('#bottom').innerHTML='';
  $('#content').innerHTML='<div class="toolpage" id="arscBody"></div>';
}
function arscPlusTabs(a){
  const tabs=[['explore','浏览'],['history','最近'],['search','搜索'],['constants','常量']];
  return '<div class="tabs" role="tablist">'+tabs.map(t=>`<button role="tab" aria-selected="${a.tab===t[0]}" class="tab ${a.tab===t[0]?'active':''}" data-arsc-tab="${t[0]}">${t[1]}</button>`).join('')+'</div>';
}
function arscTag(id){return '['+((Number(id)||0)>>>0).toString(16).toUpperCase().padStart(2,'0')+']';}
function arscTreeOpen(a,key,dflt){const v=a.treeOpen?a.treeOpen[key]:undefined;return v===undefined?!!dflt:!!v;}
function arscTouch(a){if(a){a.treeConfigs={};a.treeEntries={};}}
function arscTreeRow(kind,name,badge,onclick,expandable,expanded,glyph){
  const b=document.createElement('button');
  b.className='treeRow'+(expandable?'':' classRow');
  b.setAttribute('role','treeitem');
  if(expandable)b.setAttribute('aria-expanded',String(!!expanded));
  let h='';
  if(expandable)h+='<span class="chevron">'+icon('forward')+'</span>';
  h+=kind==='folder'?'<span class="fileicon">'+icon('folder')+'</span>':'<span class="classicon">'+esc(glyph||'C')+'</span>';
  h+='<span class="treename">'+esc(name)+'</span>';
  if(badge)h+='<span class="arscNodeBadge">'+esc(badge)+'</span>';
  b.innerHTML=h;b.onclick=()=>act(onclick);return b;
}
async function arscTreeConfigs(a,pkgId,typeId){const r=await rpc('arscBrowse',{session:a.session,packageId:pkgId,typeId:typeId,offset:0,limit:1});return r.configs||[];}
async function arscTreeEntries(a,pkgId,typeId,config){const r=await rpc('arscBrowse',{session:a.session,packageId:pkgId,typeId:typeId,config:config||'',offset:0,limit:500});return r.entries||[];}
function arscFilterTree(container,filter){
 const kids=[...container.children];let any=false;
 for(let i=0;i<kids.length;i++){
  const node=kids[i];
  if(!node.classList.contains('treeRow'))continue;
  const group=kids[i+1]&&kids[i+1].classList.contains('treeChildren')?kids[i+1]:null;
  const name=((node.querySelector('.treename')||{}).textContent||'').toLowerCase();
  const badge=((node.querySelector('.arscNodeBadge')||{}).textContent||'').toLowerCase();
  let visible=!filter||name.includes(filter)||badge.includes(filter);
  if(group&&arscFilterTree(group,filter))visible=true;
  node.style.display=visible?'':'none';
  if(group)group.style.display=visible?'':'none';
  if(visible)any=true;
 }
 return any;
}
async function arscRenderPlusTree(body){
  const a=state.arsc;
  const r=await rpc('arscBrowse',{session:a.session,packageId:-1,typeId:-1,offset:0,limit:1});
  const pkgs=r.packages||[];a.packages=pkgs;a.treeOpen=a.treeOpen||{};a.treeConfigs=a.treeConfigs||{};a.treeEntries=a.treeEntries||{};
  const filter=(a.treeFilter||'').trim().toLowerCase();
  body.className='toolpage arscTreePage';
  const search=document.createElement('div');search.className='searchline';
  search.innerHTML='<input id="arscTreeFilter" aria-label="筛选包 / 类型 / 资源" placeholder="筛选包 / 类型 / 资源…" value="'+esc(a.treeFilter||'')+'" autocomplete="off" spellcheck="false"><button>筛选</button>';
  const tree=document.createElement('div');tree.className='tree';tree.setAttribute('role','tree');tree.setAttribute('aria-label','ARSC 包、类型与资源');
  const message=t=>{const d=document.createElement('div');d.className='treeMessage';d.textContent=t;return d;};
  for(const p of pkgs){
   const pk='p'+p.id,openP=filter?true:arscTreeOpen(a,pk,true);
   tree.append(arscTreeRow('folder',p.name||'(未命名)',arscTag(p.id)+' · '+(p.count||0)+' 项',()=>{a.treeOpen[pk]=!openP;return arscRender();},true,openP));
   if(!openP)continue;
   const pKids=document.createElement('div');pKids.className='treeChildren';
   for(const t of p.types||[]){
    const tk=pk+'t'+t.id,openT=filter?true:arscTreeOpen(a,tk,false);
    pKids.append(arscTreeRow('folder',t.name||'(未命名)',arscTag(t.id)+' · '+(t.count||0)+' 项',()=>{a.treeOpen[tk]=!openT;return arscRender();},true,openT));
    if(!openT)continue;
     const tKids=document.createElement('div');tKids.className='treeChildren';
     let configs=a.treeConfigs[tk];
     if(!configs){configs=await arscTreeConfigs(a,p.id,t.id);a.treeConfigs[tk]=configs;}
     if(!configs.length)tKids.append(message('此类型没有资源。'));
     const def=configs.find(c=>!c.qualifiers);
     if(def){ // 默认配置不是文件夹：资源直接挂在类型下
      const dk=tk+'@default';
      let entries=a.treeEntries[dk];
      if(!entries){entries=await arscTreeEntries(a,p.id,t.id,'');a.treeEntries[dk]=entries;}
      if(!entries.length)tKids.append(message('此类型没有资源。'));
      for(const e of entries)tKids.append(arscTreeRow('leaf',e.name||'(未命名)',arscHex(e.id),()=>arscOpenEntryXml(p.name,t.name,e.id,''),false,false,'R'));
     }
     for(const c of configs){
      const qual=c.qualifiers||'';if(!qual)continue;
      const ck=tk+'c'+qual,openC=filter?true:arscTreeOpen(a,ck,false);
      const glyph=(qual.replace(/[^A-Za-z]/g,'').charAt(0)||'C').toUpperCase();
      tKids.append(arscTreeRow('group',c.label&&c.label!==qual?c.label:qual,qual+' · '+(c.count||0)+' 项',()=>{a.treeOpen[ck]=!openC;return arscRender();},true,openC,glyph));
      if(!openC)continue;
      const cKids=document.createElement('div');cKids.className='treeChildren';
      let entries=a.treeEntries[ck];
      if(!entries){entries=await arscTreeEntries(a,p.id,t.id,qual);a.treeEntries[ck]=entries;}
      if(!entries.length)cKids.append(message('此配置没有资源。'));
      for(const e of entries)cKids.append(arscTreeRow('leaf',e.name||'(未命名)',arscHex(e.id),()=>arscOpenEntryXml(p.name,t.name,e.id,qual),false,false,'R'));
      tKids.append(cKids);
    }
    pKids.append(tKids);
   }
   tree.append(pKids);
  }
  body.replaceChildren(search,tree);tree.scrollTop=a.treeScroll||0;tree.onscroll=()=>a.treeScroll=tree.scrollTop;
  const input=search.querySelector('input'),go=search.querySelector('button');
  if(filter)arscFilterTree(tree,filter);
  let timer=null;
  const commit=async()=>{clearTimeout(timer);const v=input.value;a.treeFilter=v;if(state.screen!=='tool'||!state.arsc)return;await arscRender();const n=$('#arscTreeFilter');if(n){n.focus();n.setSelectionRange(n.value.length,n.value.length);}};
  input.oninput=()=>{clearTimeout(timer);timer=setTimeout(()=>act(commit),260);};
  input.onkeydown=e=>{if(e.key==='Enter'){clearTimeout(timer);act(commit);}};
  go.onclick=()=>act(commit);
  symbolBar(text=>{input.value=input.value+text;commit();});
}
async function arscRender(){
  const a=state.arsc;if(!a)return;
  arscHeader();
  const body=$('#arscBody');if(!body)return;
  try{
    if(a.mode==='plus'&&a.tab==='explore'&&a.level==='root'){await arscRenderPlusTree(body);return;}
    if(a.mode==='plus'&&a.level!=='entry'){
      if(a.tab==='history'){
        body.innerHTML='<div class="sectionLabel">最近打开</div>';
        if(!a.history.length)body.insertAdjacentHTML('beforeend','<div class="empty">还没有打开过资源</div>');
        for(const h of a.history){const b=document.createElement('button');b.className='treeRow classRow';b.innerHTML=`<span class="classicon">H</span><span class="treename">${esc(h.name||'(未命名)')}</span><span class="arscNodeBadge">${arscHex(h.id)} · ${esc(h.config||'默认')}</span>`;b.onclick=()=>act(()=>arscOpenEntry(h.id,h.config||''));body.append(b);}
        return;
      }
      if(a.tab==='search'){
        const s=a.search||{};
        body.innerHTML=`<div class="sectionLabel">功能</div>
          <div class="arscSearchCard">
            <label>查找内容</label><input id="arscPlusQuery" value="${esc(s.query||'')}" autocomplete="off" spellcheck="false">
            <label>路径</label><input id="arscPlusPath" value="${esc(s.path||'/')}" autocomplete="off" spellcheck="false">
            <label>搜索类型</label><select id="arscPlusType"><option value="code">代码</option><option value="id">资源ID</option><option value="string">字符串</option><option value="int">整数</option><option value="color">颜色</option></select>
            <label class="check"><input type="checkbox" id="arscPlusCase" ${s.caseSensitive?'checked':''}>区分大小写</label>
            <label class="check"><input type="checkbox" id="arscPlusRegex" ${s.regex?'checked':''}>正则表达式</label>
            <button class="primary" id="arscPlusSearchBtn">搜索</button>
          </div>
          <div id="arscPlusResults"></div>`;
        $('#arscPlusType').value=s.type||'code';
        const results=$('#arscPlusResults');
        const rows=a.searchRows||[];
        results.innerHTML='<div class="sectionLabel">结果 · '+rows.length+(rows.length>=200?'+':'')+'</div>';
        for(const e of rows){const b=document.createElement('button');b.className='treeRow classRow';b.innerHTML=`<span class="classicon">R</span><span class="treename">${esc(e.name||'(未命名)')}</span><span class="arscNodeBadge">${arscHex(e.id)} · ${esc(e.value||'')}</span>`;b.onclick=()=>act(()=>arscOpenEntry(e.id,e.config||''));results.append(b);}
        $('#arscPlusSearchBtn').onclick=()=>act(async()=>{
          const q=$('#arscPlusQuery').value.trim();if(!q)return toast('请输入查找内容');
          a.search={query:q,path:$('#arscPlusPath').value.trim()||'/',type:$('#arscPlusType').value,caseSensitive:$('#arscPlusCase').checked,regex:$('#arscPlusRegex').checked};
          const r=await rpc('arscSearchEx',{session:a.session,mode:a.search.type,query:q,path:a.search.path,caseSensitive:a.search.caseSensitive,regex:a.search.regex,limit:200});
          a.searchRows=r.rows||[];await arscRender();if(!a.searchRows.length)toast('没有匹配项');
        });
        return;
      }
      if(a.tab==='constants'){
        const off=a.offset||0,lim=a.limit||200,r=await rpc('arscPool',{session:a.session,offset:off,limit:lim,query:a.poolFilter||''});
        body.innerHTML=`<div class="sectionLabel">功能</div>
          <button class="listrow" data-plus-reload><span class="main">重新加载<small>刷新字符常量池</small></span>${icon('refresh')}</button>
          <button class="listrow" data-plus-filter><span class="main">过滤<small>${esc(a.poolFilter||'显示全部字符串')}</small></span>${icon('search')}</button>
          <button class="listrow" data-plus-replace><span class="main">替换<small>批量替换字符串池</small></span>${icon('swap')}</button>
          <div class="sectionLabel"><span>字符常量池</span><span class="pill">${r.total}</span></div><div id="arscPlusPool"></div>`;
        const rows=$('#arscPlusPool');
        for(const x of r.rows||[]){const b=document.createElement('button');b.className='treeRow classRow';b.innerHTML=`<span class="classicon">S</span><span class="treename">${esc(x.text)}</span><span class="arscNodeBadge">#${x.index}</span>`;b.onclick=()=>act(()=>arscEditPool(x.index,x.text));rows.append(b);}
        arscPager(rows,r.total,a);
        body.querySelector('[data-plus-reload]').onclick=()=>act(async()=>{a.offset=0;await arscRender();});
        body.querySelector('[data-plus-filter]').onclick=()=>act(async()=>{const v=await ask('过滤字符常量池',a.poolFilter||'','包含文本（留空显示全部）');if(v===null)return;a.poolFilter=v.trim();a.offset=0;await arscRender();});
        body.querySelector('[data-plus-replace]').onclick=()=>act(arscBatchReplace);
        return;
      }
    }
    if(a.level==='root'){
      const r=await rpc('arscBrowse',{session:a.session,packageId:-1,typeId:-1,offset:0,limit:1});
      const pkgs=r.packages||[];
      if(a.mode==='plus'){
        body.innerHTML='<div class="sectionLabel">包</div>';
      }else{
        body.innerHTML=`<p>${pkgs.length} 个包 · 字符常量池 ${r.stringCount||a.stringCount} 项 · ${(a.size/1024).toFixed(1)} KB</p>
        <button class="listrow" data-arsc-pool><span class="main">字符常量池<small>查看、筛选、编辑全局字符串</small></span><span class="pill">${r.stringCount||a.stringCount}</span></button>
        <button class="listrow" data-arsc-search="value"><span class="main">搜索资源值<small>字符串值包含指定文本</small></span>${icon('search')}</button>
        <button class="listrow" data-arsc-search="id"><span class="main">ID定位资源<small>0x7f010001</small></span>${icon('pin')}</button>
        <div class="sectionLabel">包</div>`;
      }
      for(const p of pkgs){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(p.name)}<small>[${(p.id||0).toString(16).toUpperCase()}] · ${(p.types||[]).length} 个类型</small></span>${icon('arsc')}`;b.onclick=()=>act(async()=>{a.level='package';a.pkgId=p.id;a.pkgName=p.name;a.offset=0;await arscRender();});body.append(b);}
      if(a.mode!=='plus'){
        body.querySelector('[data-arsc-pool]').onclick=()=>act(async()=>{a.level='pool';a.offset=0;a.query='';await arscRender();});
        body.querySelectorAll('[data-arsc-search]').forEach(b=>b.onclick=()=>act(()=>arscSearchDialog(b.dataset.arscSearch)));
      }
      return;
    }
    if(a.level==='package'){
      const r=await rpc('arscBrowse',{session:a.session,packageId:a.pkgId,typeId:-1,offset:0,limit:1});
      body.innerHTML='<div class="sectionLabel">类型</div>';
      for(const t of r.types||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(t.name)}<small>${arscHex(t.id)} · ${t.count} 个条目 · ${t.configs} 个配置</small></span>${icon('layers')}`;b.onclick=()=>act(async()=>{a.level='type';a.typeId=t.id;a.typeName=t.name;a.offset=0;await arscRender();});body.append(b);}
      return;
    }
    if(a.level==='type'){
      const r=await rpc('arscBrowse',{session:a.session,packageId:a.pkgId,typeId:a.typeId,offset:0,limit:1});
      body.innerHTML='<div class="sectionLabel">配置</div>';
      for(const c of r.configs||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(c.label)}<small>${esc(c.qualifiers||'(默认)')} · ${c.count} 个条目</small></span>${icon('settings')}`;b.onclick=()=>act(async()=>{a.level='config';a.config=c.qualifiers;a.configLabel=c.label;a.offset=0;await arscRender();});body.append(b);}
      return;
    }
    if(a.level==='config'){
      const r=await rpc('arscBrowse',{session:a.session,packageId:a.pkgId,typeId:a.typeId,config:a.config,offset:a.offset,limit:a.limit});
      body.innerHTML=`<div class="sectionLabel"><span>${esc(a.typeName)} · ${esc(a.configLabel||'默认')}</span><span class="pill">${r.total} 项</span></div>`;
      for(const e of r.entries||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(e.name||'(未命名)')}<small>${arscHex(e.id)} · ${e.complex?'集合值':esc(e.type||arscTypeLabel(e.type))}</small></span><span class="pill">${esc(e.complex?'complex':(e.value||'').slice(0,28))}</span>`;b.onclick=()=>act(()=>arscOpenEntry(e.id,e.config));body.append(b);}
      arscPager(body,r.total,a);
      return;
    }
    if(a.level==='entry'){
      arscRenderEntry(body,a);
      return;
    }
    if(a.level==='pool'){
      const r=await rpc('arscPool',{session:a.session,offset:a.offset,limit:a.limit,query:a.query});
      body.innerHTML=`<div class="searchline"><input id="arscPoolSearch" placeholder="筛选字符串…" value="${esc(a.query)}"><button id="arscPoolGo">搜索</button><button id="arscPoolReplace">批量替换</button></div><div id="arscPoolRows"></div>`;
      const rows=$('#arscPoolRows');
      for(const s of r.rows||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<small>${s.index}</small><span class="main">${esc(s.text)}</span>`;b.onclick=()=>act(()=>arscEditPool(s.index,s.text));rows.append(b);}
      arscPager(rows,r.total,a);
      const go=()=>act(async()=>{a.query=$('#arscPoolSearch').value.trim();a.offset=0;await arscRender();});
      $('#arscPoolGo').onclick=go;$('#arscPoolSearch').onkeydown=e=>{if(e.key==='Enter')go();};
      $('#arscPoolReplace').onclick=()=>act(arscBatchReplace);
      return;
    }
    if(a.level==='search'){
      body.innerHTML=`<div class="sectionLabel">结果 · ${a.rows.length}${a.rows.length>=200?'+':''}</div>`;
      for(const e of a.rows){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(e.name||'(未命名)')}<small>${arscHex(e.id)} · ${esc(e.config||'默认')}</small></span><span class="pill">${esc((e.value||'').slice(0,28))}</span>`;b.onclick=()=>act(()=>arscOpenEntry(e.id,e.config));body.append(b);}
      return;
    }
  }catch(e){body.innerHTML=`<div class="empty">${esc(e.message||e)}</div>`;}
}
function arscPager(container,total,a){
  if(!total||total<=a.limit)return;
  const bar=document.createElement('div');bar.className='searchline';
  bar.innerHTML=`<button ${a.offset<=0?'disabled':''} data-page="prev">上一页</button><span class="muted" style="flex:1;text-align:center">${a.offset+1}–${Math.min(a.offset+a.limit,total)} / ${total}</span><button ${a.offset+a.limit>=total?'disabled':''} data-page="next">下一页</button>`;
  const p=bar.querySelector('[data-page="prev"]'),n=bar.querySelector('[data-page="next"]');
  if(p)p.onclick=()=>act(async()=>{a.offset=Math.max(0,a.offset-a.limit);await arscRender();});
  if(n)n.onclick=()=>act(async()=>{a.offset+=a.limit;await arscRender();});
  container.append(bar);
}
function arscXmlEscape(s){return String(s==null?'':s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function arscValueText(e){const t=e.type;if(t===1||t===2||t===5||t===6||t===7||t===8)return arscHex(e.data);return String(e.value==null?'':e.value);}
function arscEntryXml(e,pkgName,typeName){
 const name=e.name||'',id=arscHex(e.id),pkg=e.package||pkgName||'';
 const tag=/^[A-Za-z_][A-Za-z0-9_.-]*$/.test(typeName||'')?typeName:'item';
 const head='<?xml version="1.0" encoding="UTF-8"?>\n\n';
 if(e.complex){
  const kids=(e.values||[]).map(v=>'    <item index="'+v.index+'" type="'+((Number(v.type)||0))+'">'+arscXmlEscape(v.value)+'</item>').join('\n');
  return head+'<'+tag+' name="'+arscXmlEscape(name)+'" id="'+id+'" package="'+arscXmlEscape(pkg)+'" complex="true">\n'+(kids||'    <!-- 无集合值 -->')+'\n</'+tag+'>\n';
 }
 return head+'<'+tag+' name="'+arscXmlEscape(name)+'" id="'+id+'" package="'+arscXmlEscape(pkg)+'" type="'+arscXmlEscape(e.typeName||'')+'">'+arscXmlEscape(arscValueText(e))+'</'+tag+'>\n';
}
function arscParseXml(text){
 const doc=new DOMParser().parseFromString(text,'application/xml');
 const bad=doc.getElementsByTagName('parsererror')[0];
 if(bad)throw Error('XML 格式错误：'+String(bad.textContent||'').replace(/\s+/g,' ').slice(0,140));
 const root=doc.documentElement;if(!root)throw Error('XML 内容为空');
 return root;
}
function arscParseEntryXml(text,name){
 const root=arscParseXml(text);
 let el=root;
 if(name){const all=root.getElementsByTagName('*');for(const x of all)if(x.getAttribute('name')===name){el=x;break;}}
 return el.textContent==null?'':el.textContent.trim();
}
function arscParseComplexXml(text){
 const root=arscParseXml(text),out=[];
 const items=root.getElementsByTagName('item');
 for(const it of items){
  const index=Number(it.getAttribute('index'));
  if(!Number.isFinite(index)||index<0)continue;
  out.push({index:index,type:Number(it.getAttribute('type'))||3,value:it.textContent==null?'':it.textContent.trim()});
 }
 return out;
}
async function arscOpenEntryXml(pkgName,typeName,entryId,config){
 const a=state.arsc;if(!a)return;
 const e=await rpc('arscEntry',{session:a.session,id:entryId,config:config||''});
 const ref=(e.package?e.package+':':'')+(typeName||'')+'/'+(e.name||'');
 state.editor={kind:'arsc',session:a.session,id:entryId,config:config||'',entry:e,ref:ref,name:(e.name||'resource')+'.xml',text:arscEntryXml(e,pkgName,typeName),dirty:false,readOnly:false,returnScreen:'tool'};
 renderEditor();
}
async function arscOpenEntry(id,config){  const a=state.arsc;
  const e=await rpc('arscEntry',{session:a.session,id:id,config:config||''});
  a.entryData=e;a.level='entry';
  if(a.mode==='plus'){
    const h={id:e.id,config:e.config||'',name:e.name||'',type:e.type||'',value:e.complex?'[集合值]':(e.value||'')};
    a.history=(a.history||[]).filter(x=>!(x.id===h.id&&x.config===h.config));a.history.unshift(h);a.history=a.history.slice(0,50);
  }
  await arscRender();
}
function arscRenderEntry(body,a){
  const e=a.entryData;if(!e){body.innerHTML='<div class="empty">资源已不存在</div>';return;}
  body.innerHTML=`<div class="dataGrid"><span>资源名称</span><b>${esc(e.name||'')}</b><span>资源 ID</span><b>${arscHex(e.id)}</b><span>所属类型</span><b>${esc(e.package||'')}:${esc(e.type||'')}</b><span>配置</span><b>${esc(e.configLabel||e.config||'默认')}</b><span>值类型</span><b>${e.complex?'集合值':esc(e.typeName||arscTypeLabel(e.type))}</b><span>可见性</span><b>${e.public?'public':'private'}</b>${e.parentId?`<span>父样式</span><b>${arscHex(e.parentId)} ${esc(e.parent||'')}</b>`:''}</div>`;
  if(e.complex){
    body.insertAdjacentHTML('beforeend',`<div class="sectionLabel"><span>集合值</span><span class="pill">${(e.values||[]).length} 项</span></div>`);
    for(const v of e.values||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(v.name||arscHex(v.nameId))}<small>${esc(v.typeName)} · 名称 ID ${arscHex(v.nameId)}</small></span><span class="pill">${esc(v.value)}</span>`;b.onclick=()=>act(()=>arscValueEditor(a,e,v.index,false));body.append(b);}
  }else{
    body.insertAdjacentHTML('beforeend',`<div class="sectionLabel">值</div><button class="listrow" id="arscScalar"><span class="main">${esc(e.value||'(空)')}<small>${esc(e.typeName||arscTypeLabel(e.type))} · 0x${((Number(e.data)||0)>>>0).toString(16)}</small></span>${icon('edit')}</button>`);
    body.querySelector('#arscScalar').onclick=()=>act(()=>arscValueEditor(a,e,-1,false));
  }
  body.insertAdjacentHTML('beforeend',`<div class="sectionLabel">操作</div><div class="actionGrid" id="arscActions"></div>`);
  const acts=$('#arscActions');
  const add=(label,ic,fn)=>{const b=document.createElement('button');b.className='listrow';b.innerHTML=icon(ic)+`<span class="main">${label}</span>`;b.onclick=()=>act(fn);acts.append(b);};
  add('在编辑器中打开','edit',()=>arscOpenEntryXml(e.package||a.pkgName,a.typeName||'',e.id,e.config||''));
  add('重命名资源','edit',()=>arscRename(e));
  add('添加配置（复制当前值）','plus',()=>arscAddConfig(e));
  if(e.complex)add('添加集合值','plus',()=>arscValueEditor(a,e,-1,true));
  if(e.complex&&e.type==='style')add('编辑父样式','edit',()=>arscEditParent(e));
  add('删除值','trash',()=>arscDeleteValue(e));
  add('删除资源','trash',()=>arscDeleteEntry(e));
  add('复制资源 ID','copy',async()=>{await rpc('clipboard',{text:arscHex(e.id)});toast('已复制资源 ID');});
  add('复制资源名','copy',async()=>{await rpc('clipboard',{text:(e.package?e.package+':':'')+(e.type?e.type+'/':'')+e.name});toast('已复制资源名');});
  if(!e.complex)add('复制值','copy',async()=>{await rpc('clipboard',{text:e.value||''});toast('已复制');});
}
async function arscValueEditor(a,e,index,add){
  const value=(()=>{if(index>=0&&e.values&&e.values[index]){const v=e.values[index];return (v.type===1||v.type===2||v.type===7||v.type===8)?arscHex(v.data):String(v.value||'');}if(add)return '';if(e.type===1||e.type===2||e.type===7||e.type===8)return arscHex(e.data);if(e.type===5||e.type===6)return arscHex(e.data);return String(e.value||'');})();
  const type=(index>=0&&e.values&&e.values[index])?e.values[index].type:(e.type||3);
  const body=`<label>值类型</label><select id="arscValueType">${arscOptions(arscChoice(type))}</select><label>${add?'新值':'值'}</label><input id="arscValueText" value="${esc(value)}" autocomplete="off" spellcheck="false"><p class="muted">字符串直接输入；引用/属性填 0x7f010001、@string/name 或 ?attr/name；颜色填 #AARRGGBB；尺寸/分数填原始 32 位值（0x… 或十进制）。</p>${add?`<label>名称资源 ID</label><input id="arscNameId" value="0x01000000" autocomplete="off" spellcheck="false"><p class="muted">attr/array/style 等集合项的名称 ID。</p>`:''}`;
  dialog(add?'添加集合值':'编辑资源值',body,[['取消',closeModal],['保存',()=>arscCommitValue(a,e,index,add)]]);
}
async function arscCommitValue(a,e,index,add){
  const choice=Number($('#arscValueType').value),type=ARSC_TYPES[choice][2],value=$('#arscValueText').value;
  const args={session:a.session,op:add?'addValue':'setValue',id:e.id,config:e.config||'',valueIndex:index,type:type,value:value};
  if(add){const nid=$('#arscNameId').value.trim();args.nameId=nid||'0';}
  await rpc('arscEdit',args);a.dirty=true;arscTouch(a);closeModal();toast('资源值已更新');
  if(a.level==='entry'){await arscOpenEntry(e.id,e.config||'');}else{await arscRender();}
}
async function arscBatchReplace(){
  const a=state.arsc;
  const find=await ask('批量替换字符串','','查找文本（区分大小写）');if(find===null||!find)return;
  const replace=await ask('批量替换字符串','','替换为（可留空）');if(replace===null)return;
  if(!await confirmBox('批量替换',`将在全局字符串池中替换所有包含“${esc(find)}”的字符串。`,'替换'))return;
  const r=await rpc('arscEdit',{session:a.session,op:'replacePool',find:find,replace:replace});a.dirty=!!r.count;arscTouch(a);
  toast('已替换 '+r.count+' 条字符串');await arscRender();
}
async function arscEditPool(index,text){
  const a=state.arsc,v=await ask('编辑字符串',text,'字符串 '+index);
  if(v===null)return;
  await rpc('arscEdit',{session:a.session,op:'setPool',index:index,text:v});a.dirty=true;arscTouch(a);toast('字符串已更新');await arscRender();
}
async function arscRename(e){
  const a=state.arsc,v=await ask('重命名资源',e.name,'新名称');
  if(v===null||!v.trim())return;
  await rpc('arscEdit',{session:a.session,op:'rename',id:e.id,config:e.config||'',name:v.trim()});a.dirty=true;arscTouch(a);toast('已重命名');await arscOpenEntry(e.id,e.config||'');
}
async function arscAddConfig(e){
  const a=state.arsc,v=await ask('添加配置','','配置限定符（如 en-rUS、zh-rCN、night；留空为默认）');
  if(v===null)return;
  await rpc('arscEdit',{session:a.session,op:'importConfig',id:e.id,config:e.config||'',from:e.config||'',qualifiers:v.trim()});a.dirty=true;arscTouch(a);toast('已添加配置');await arscOpenEntry(e.id,v.trim());
}
async function arscEditParent(e){
  const a=state.arsc;
  const v=await ask('编辑父样式',e.parent||arscHex(e.parentId||0),'父样式 ID 或 [包:]style/名称');
  if(v===null)return;
  await rpc('arscEdit',{session:a.session,op:'setParent',id:e.id,config:e.config||'',parentId:v.trim()});a.dirty=true;arscTouch(a);toast('父样式已更新');await arscOpenEntry(e.id,e.config||'');
}
async function arscDeleteValue(e){
  const a=state.arsc;
  if(!e.complex)return toast('标量值请直接编辑');
  const v=await ask('删除集合值','','输入要删除的值序号（从 0 开始）');
  if(v===null)return;
  const index=Number(v);if(!Number.isInteger(index)||index<0)return toast('序号无效');
  if(!await confirmBox('删除集合值','确定删除第 '+index+' 项？','删除'))return;
  await rpc('arscEdit',{session:a.session,op:'removeValue',id:e.id,config:e.config||'',valueIndex:index});a.dirty=true;arscTouch(a);toast('已删除');await arscOpenEntry(e.id,e.config||'');
}
async function arscClose(){
  const a=state.arsc;if(!a)return window.goBack();
  if(a.dirty&&!await confirmBox('放弃修改？','资源表还有未保存的修改。','放弃并关闭'))return;
  await rpc('arscClose',{session:a.session});state.arsc=null;state.screen='files';await refresh();
}
async function arscDeleteEntry(e){
  const a=state.arsc;
  const all=await new Promise(resolve=>{dialog('删除资源',`<p>选择删除范围。此操作会清空资源值，保存后生效。</p><p class="muted">${esc(e.name||'')}</p>`,[['取消',()=>{closeModal();resolve(null);}],['当前配置',()=>{closeModal();resolve(false);}],['所有配置',()=>{closeModal();resolve(true);}]]);});
  if(all===null)return;
  await rpc('arscEdit',{session:a.session,op:'deleteEntry',id:e.id,config:e.config||'',all:all});a.dirty=true;arscTouch(a);toast('已删除');a.level='config';a.entryData=null;await arscRender();
}
async function arscAddEntry(){
  const a=state.arsc;
  const body=`<label>资源名称</label><input id="arscEntryName" autocomplete="off" spellcheck="false"><label>条目 ID（留空自动）</label><input id="arscEntryId" placeholder="自动分配" autocomplete="off" spellcheck="false"><label>值类型</label><select id="arscValueType">${arscOptions(0)}</select><label>值</label><input id="arscValueText" autocomplete="off" spellcheck="false"><p class="muted">新资源将添加到 ${esc(a.typeName)} · ${esc(a.configLabel||'默认')}。</p>`;
  dialog('添加资源',body,[['取消',closeModal],['添加',async()=>{
    const name=$('#arscEntryName').value.trim(),raw=$('#arscEntryId').value.trim(),choice=Number($('#arscValueType').value);
    if(!name)throw new Error('资源名不能为空');
    const entryId=raw?(raw.startsWith('0x')?parseInt(raw,16):Number(raw)):null;
    if(entryId!==null&&(!Number.isInteger(entryId)||entryId<0||entryId>65535))throw new Error('条目 ID 必须是 0–65535');
    const args={session:a.session,op:'addEntry',packageId:a.pkgId,typeId:a.typeId,typeName:a.typeName,config:a.config||'',name:name,type:ARSC_TYPES[choice][2],value:$('#arscValueText').value};
    if(entryId!==null)args.entryId=entryId;
    await rpc('arscEdit',args);a.dirty=true;arscTouch(a);closeModal();toast('已添加资源');await arscRender();
  }]]);
}
async function arscDeleteConfig(){
  const a=state.arsc;
  if(!await confirmBox('删除配置','将删除 '+a.typeName+' 的配置“'+(a.configLabel||'默认')+'”中的全部条目。','删除'))return;
  await rpc('arscEdit',{session:a.session,op:'deleteConfig',packageId:a.pkgId,typeId:a.typeId,config:a.config||''});a.dirty=true;arscTouch(a);toast('配置已删除');a.level='type';await arscRender();
}
async function arscSearchDialog(mode){
  const a=state.arsc;
  const label=mode==='id'?'资源 ID（0x7f010001）':mode==='value'?'值包含文本':'资源名（名称或 包:类型/名称）';
  const q=await ask('搜索资源','',label);if(q===null||!q.trim())return;
  const r=await rpc('arscSearch',{session:a.session,mode:mode,query:q.trim(),limit:200});
  if(!r.rows||!r.rows.length)return toast('没有匹配的资源');
  if(mode==='id'&&r.rows.length===1){await arscOpenEntry(r.rows[0].id,r.rows[0].config||'');return;}
  a.level='search';a.rows=r.rows;a.searchMode=mode;a.query=q.trim();a.offset=0;await arscRender();
}
async function arscSave(){
  const a=state.arsc;const r=await rpc('arscSave',{session:a.session});a.dirty=false;
  if(a.level==='pool')a.offset=0;
  await arscRender();toast('资源表已保存');
  const buttons=[['关闭',closeModal]];
  if(r.needsSigning)buttons.unshift(['签名 APK',async()=>{closeModal();const s=await rpc('sign',{path:r.path});alertBox('签名完成',s.path||r.path);}]);
  dialog('资源表已保存',`<p class="break">${esc(r.path)}</p><p>${r.size} 字节。${r.needsSigning?'APK 已修改，安装前需要重新签名。':'原文件已原子写回。'}</p>`,buttons);
}
function arscBack(){
  const a=state.arsc;if(!a)return window.goBack();
  if(a.mode==='plus'&&a.level==='entry'){a.level='root';a.entryData=null;return arscRender();}
  if(a.level==='entry'){a.level='config';a.entryData=null;return arscRender();}
  if(a.level==='config'){a.level='type';return arscRender();}
  if(a.level==='type'){a.level='package';return arscRender();}
  if(a.level==='package'||a.level==='pool'||a.level==='search'){a.level='root';a.offset=0;return arscRender();}
  if(a.level==='root')return arscClose();
  return window.goBack();
}
async function showArscTranslate(args,name,session){
  let sid=session;
  if(!sid){const s=await rpc('arscOpen',Object.assign({},args,{name:name||''}));sid=s.session;}
  state.arscTranslate={session:sid,name:name||'resources.arsc',config:'',configs:[],rows:[],total:0,offset:0,limit:100,query:'',dirty:false,opened:!session};
  state.previous=state.screen==='tool'?state.previous:'files';state.screen='tool';
  const first=await rpc('arscTranslate',{session:sid,config:'',offset:0,limit:1,query:''});
  const cfgs=first.configs||[];
  const pick=cfgs.find(c=>c.qualifiers);
  state.arscTranslate.configs=cfgs;state.arscTranslate.config=pick?pick.qualifiers:'';
  await arscTranslateRender();
}
async function arscTranslateRender(){
  const a=state.arscTranslate;if(!a)return;
  const r=await rpc('arscTranslate',{session:a.session,config:a.config,offset:a.offset,limit:a.limit,query:a.query});
  a.rows=r.rows||[];a.total=r.total||0;a.configs=r.configs||a.configs;
  header('翻译模式',a.name,[{icon:'back',label:'退出翻译',run:arscTranslateBack},{icon:'text',label:'翻译全部',run:arscTranslateAll},{icon:'save',label:'保存资源表',run:arscTranslateSave},{icon:'more',label:'更多',run:()=>menu([{icon:'refresh',label:'重新加载',run:()=>act(arscTranslateRender)},{icon:'close',label:'退出',run:arscTranslateBack}])}],true);
  $('#subbar').innerHTML='';$('#bottom').innerHTML='';
  $('#content').innerHTML=`<div class="toolpage">
    <div class="sectionLabel">目标语言</div>
    <div class="searchline"><select id="arscTransConfig" style="flex:1;background:transparent;color:var(--text);border:0;padding:10px 0">${a.configs.map(c=>`<option value="${esc(c.qualifiers)}" ${c.qualifiers===a.config?'selected':''}>${esc(c.label)}${c.qualifiers?' ('+esc(c.qualifiers)+')':'（源语言）'}</option>`).join('')}</select><button id="arscTransNew">新建语言</button></div>
    <div class="searchline"><input id="arscTransSearch" placeholder="筛选资源名或文本…" value="${esc(a.query)}"><button id="arscTransGo">搜索</button></div>
    <div id="arscTransRows"></div></div>`;
  const rows=$('#arscTransRows');
  rows.innerHTML=`<div class="sectionLabel"><span>字符串</span><span class="pill">${a.total}</span></div>`;
  if(!a.rows.length)rows.insertAdjacentHTML('beforeend','<div class="empty">没有可翻译的字符串</div>');
  for(const e of a.rows){
    const b=document.createElement('button');b.className='listrow';
    b.innerHTML=`<span class="main">${esc(e.name||'(未命名)')}<small>${arscHex(e.id)} · 源：${esc(e.source||'(空)')}</small><small>译：${esc(e.target||'（未翻译）')}</small></span><span class="pill">${e.hasTarget?'已译':'缺失'}</span>`;
    b.onclick=()=>act(()=>arscTranslateEdit(e));rows.append(b);
  }
  arscPager(rows,a.total,a);
  $('#arscTransConfig').onchange=e=>act(async()=>{a.config=e.target.value;a.offset=0;await arscTranslateRender();});
  $('#arscTransNew').onclick=()=>act(arscTranslateNew);
  const go=()=>act(async()=>{a.query=$('#arscTransSearch').value.trim();a.offset=0;await arscTranslateRender();});
  $('#arscTransGo').onclick=go;$('#arscTransSearch').onkeydown=e=>{if(e.key==='Enter')go();};
}
async function arscTranslateEdit(e){
  const a=state.arscTranslate;
  const body=`<p class="break">${esc(e.name||'')} · ${arscHex(e.id)}</p><label>源文本</label><div class="arscSourceText">${esc(e.source||'')}</div><label>译文（${esc(a.config||'默认')}）</label><textarea id="arscTransText" class="toolinput" rows="4">${esc(e.target||'')}</textarea>`;
  const d=dialog('编辑翻译',body,[['取消',closeModal],['保存',async()=>{
    const v=$('#arscTransText').value;
    await rpc('arscEdit',{session:a.session,op:'setValue',id:e.id,config:a.config,valueIndex:-1,type:3,value:v});
    a.dirty=true;arscTouch(a);closeModal();toast('译文已更新');await arscTranslateRender();
  }]]);
 const actions=d.querySelector('.actions'),btn=document.createElement('button');
 btn.textContent='翻译';
 btn.onclick=()=>act(async()=>{const input=$('#arscTransText');if(!input)return;btn.disabled=true;btn.textContent='翻译中…';try{const r=await translateText(e.source||'',arscLanguageKey(a.config));if(r)input.value=r;else toast('翻译结果为空');}catch(error){alertBox('无法完成',error.message);}finally{btn.disabled=false;btn.textContent='翻译';}});
 actions.insertBefore(btn,actions.firstChild);
}
async function arscLanguageKey(qualifier){
 const raw=String(qualifier||''),base=raw.split('-')[0].toLowerCase();
 const known=(translateLanguages||[]).map(l=>l.key);
 if(known.includes(base))return base;
 if(base==='zh'&&/hant|tw|hk|mo/.test(raw.toLowerCase()))return 'zhTW';
 return base||'en';
}
async function arscTranslateAll(){
 const a=state.arscTranslate;if(!a)return;
 const rows=(a.rows||[]).filter(r=>r.source&&!r.hasTarget);
 if(!rows.length)return toast('没有需要翻译的字符串。');
 if(!await confirmBox('批量翻译？',`将翻译 ${rows.length} 条字符串并写入 ${a.config||'默认'} 配置。`,'翻译'))return;
 let done=0,fail=0;
 for(const row of rows){
  try{
   const text=await translateText(row.source,arscLanguageKey(a.config));
   if(!text)throw Error('返回为空');
   await rpc('arscEdit',{session:a.session,op:'setValue',id:row.id,config:a.config,valueIndex:-1,type:3,value:text});
   done++;
  }catch(error){fail++;}
 }
 a.dirty=true;arscTouch(a);await arscTranslateRender();
 toast('已翻译 '+done+' 条'+(fail?'，失败 '+fail+' 条':''));
}
async function arscTranslateNew(){
  const a=state.arscTranslate;
  const q=await ask('新建语言','','语言配置限定符，例如 en-rUS、zh-rCN、ja');if(q===null||!q.trim())return;
  const r=await rpc('arscTranslateAdd',{session:a.session,qualifiers:q.trim()});
  a.dirty=true;arscTouch(a);a.config=r.qualifiers;a.offset=0;toast('已复制 '+r.count+' 条源字符串');await arscTranslateRender();
}
async function arscTranslateSave(){
  const a=state.arscTranslate;const r=await rpc('arscSave',{session:a.session});a.dirty=false;
  const buttons=[['关闭',closeModal]];if(r.needsSigning)buttons.unshift(['签名 APK',async()=>{closeModal();const s=await rpc('sign',{path:r.path});alertBox('签名完成',s.path||r.path);}]);
  dialog('资源表已保存',`<p class="break">${esc(r.path)}</p><p>${r.size} 字节。${r.needsSigning?'APK 已修改，安装前需要重新签名。':'原文件已原子写回。'}</p>`,buttons);
}
async function arscTranslateBack(){
  const a=state.arscTranslate;if(!a)return;
  if(a.dirty&&!await confirmBox('放弃修改？','翻译还有未保存的修改。','放弃并退出'))return;
  if(a.opened)await rpc('arscClose',{session:a.session});
  state.arscTranslate=null;state.screen=state.previous||'files';await refresh();
}
async function showArscQuery(args,name,session){
  let sid=session;
  if(!sid){const s=await rpc('arscOpen',Object.assign({},args,{name:name||''}));sid=s.session;}
  state.arscQuery={session:sid,name:name||'resources.arsc',opened:!session,fromPlus:!!session,result:null,last:''};
  state.previous=state.screen==='tool'?state.previous:'files';state.screen='tool';
  header('资源查询',state.arscQuery.name,[{icon:'back',label:'退出查询',run:arscQueryBack}],true);
  $('#subbar').innerHTML='';$('#bottom').innerHTML='';
  $('#content').innerHTML=`<div class="toolpage">
    <div class="searchline"><input id="arscQueryInput" placeholder="@string/app_name、0x7f120001、#FF2586D0、255" autocomplete="off"><button id="arscQueryGo">查询</button></div>
    <p class="muted">支持 @类型/名称、?attr/名称、[包:]资源名、资源 ID、颜色和二/八/十/十六进制整数。</p>
    <div id="arscQueryResult"></div></div>`;
  const run=()=>act(async()=>{
    const q=$('#arscQueryInput').value.trim();if(!q)return;
    try{state.arscQuery.result=await rpc('arscQuery',{session:state.arscQuery.session,query:q});state.arscQuery.last=q;arscQueryRender();}
    catch(e){$('#arscQueryResult').innerHTML='<div class="empty">'+esc(e.message||e)+'</div>';}
  });
  $('#arscQueryGo').onclick=run;$('#arscQueryInput').onkeydown=e=>{if(e.key==='Enter')run();};
  $('#arscQueryInput').focus();arscQueryRender();
}
function arscQueryRender(){
  const q=state.arscQuery;if(!q)return;
  const box=$('#arscQueryResult');if(!box)return;
  const r=q.result;
  if(!r){box.innerHTML='';return;}
  if(r.kind==='color'){
    box.innerHTML=`<div class="arscColor" style="background:${esc(r.argb)}"></div><div class="dataGrid"><span>ARGB</span><b>${esc(r.argb)}</b><span>RGBA</span><b>${esc(r.rgba)}</b><span>整数</span><b>${r.value}</b><span>Hex</span><b>${esc(r.hex)}</b><span>A / R / G / B</span><b>${r.alpha} / ${r.red} / ${r.green} / ${r.blue}</b></div>`;
    return;
  }
  if(r.kind==='number'){
    box.innerHTML=`<div class="dataGrid"><span>十进制</span><b>${esc(r.dec)}</b><span>十六进制</span><b>${esc(r.hex)}</b><span>八进制</span><b>${esc(r.oct)}</b><span>二进制</span><b class="break">${esc(r.bin)}</b><span>有符号 32 位</span><b>${esc(r.signed32)}</b><span>无符号 32 位</span><b>${esc(r.unsigned32)}</b></div>`;
    return;
  }
  if(r.kind==='resource'){
    const e=r.resource||{};
    box.innerHTML=`<div class="dataGrid"><span>资源名</span><b>${esc(e.name||r.name||'')}</b><span>资源 ID</span><b>${esc(r.hex||arscHex(r.id))}</b><span>类型</span><b>${esc(r.package||'')}:${esc(r.type||'')}</b><span>配置数</span><b>${r.configs}</b><span>当前值</span><b class="break">${esc(e.value||'')}</b></div><div class="sectionLabel">全部配置</div><div id="arscQueryValues"></div>`;
    const vals=$('#arscQueryValues');
    for(const v of r.values||[]){const b=document.createElement('button');b.className='listrow';b.innerHTML=`<span class="main">${esc(v.label||v.config||'默认')}<small>${esc(v.type||'')}</small></span><span class="pill">${esc((v.value||'').slice(0,32))}</span>`;vals.append(b);}
    return;
  }
  box.innerHTML='<div class="empty">没有结果</div>';
}
async function arscQueryBack(){
  const q=state.arscQuery;if(!q)return;
  if(q.opened)await rpc('arscClose',{session:q.session});
  state.arscQuery=null;
  if(q.fromPlus&&state.arsc){state.screen='tool';await arscRender();return;}
  state.screen=state.previous||'files';await refresh();
}
function arscPlusMenu(){
  const a=state.arsc,rows=[];
  if(a.level!=='root')rows.push({icon:'back',label:'返回上一级',run:arscBack});
  rows.push({icon:'search',label:'资源查询',run:()=>showArscQuery(null,a.name,a.session)},{icon:'save',label:'备份',run:async()=>{const r=await rpc('arscBackup',{session:a.session});toast('已备份到 '+r.path);}},{icon:'save',label:'保存资源表',run:arscSave},{icon:'close',label:'退出',run:arscClose});
  menu(rows);
}
function arscMenu(){
  const a=state.arsc,rows=[];
  if(a.level==='config')rows.push({icon:'plus',label:'添加资源',run:arscAddEntry},{icon:'trash',label:'删除此配置',run:arscDeleteConfig});
  if(a.level==='entry'&&a.entryData)rows.push({icon:'edit',label:'重命名资源',run:()=>arscRename(a.entryData)},{icon:'plus',label:'添加配置',run:()=>arscAddConfig(a.entryData)},{icon:'trash',label:'删除资源',run:()=>arscDeleteEntry(a.entryData)});
  if(a.level==='root'||a.level==='pool'||a.level==='search')rows.push({icon:'search',label:'搜索资源',run:()=>arscSearchDialog('name')});
  if(a.level==='pool')rows.push({icon:'swap',label:'批量替换字符串',run:arscBatchReplace});
  rows.push({icon:'save',label:'保存资源表',run:arscSave});
  rows.push({icon:'close',label:'关闭编辑器',run:arscClose});
  menu(rows);
}

async function showPool(args,name){const strings=await rpc('pool',args);state.previous=state.screen;state.screen='tool';header('字符串池',name,[{icon:'more',label:'字符串池菜单',run:()=>menu([{icon:'export',label:'导出字符串',run:()=>exportText(strings.join('\n'),name+'.strings.txt')},{icon:'close',label:'关闭',run:window.goBack}])}]);$('#subbar').innerHTML='';$('#bottom').innerHTML='';$('#content').innerHTML='<div class="toolpage"><div class="searchline"><input aria-label="筛选字符串池" placeholder="筛选字符串…"></div><div id="poolRows"></div></div>';const render=q=>{$('#poolRows').innerHTML=strings.map((s,i)=>({s,i})).filter(x=>x.s.toLowerCase().includes(q.toLowerCase())).slice(0,2000).map(x=>`<button class="listrow" data-pool="${x.i}"><small>${x.i}</small><span class="main">${esc(x.s)}</span></button>`).join('');document.querySelectorAll('[data-pool]').forEach(b=>b.onclick=()=>act(async()=>{await rpc('clipboard',{text:strings[Number(b.dataset.pool)]});toast('已复制');}));};$('#content input').oninput=e=>render(e.target.value);render('');}
// 导航 and editing operate on the live buffer, including unsaved text.
function smaliOutline(text){
 const rows=[],strings=[],lines=text.split('\n');let owner='',method=null;
 lines.forEach((line,n)=>{
  let m;if((m=line.match(/^\s*\.class\s+.*?(L[^\s;]+;)/))){owner=m[1];rows.push({kind:'class',name:owner.slice(owner.lastIndexOf('/')+1,-1),detail:owner,signature:owner,line:n,end:n,owner});}
  else if((m=line.match(/^\s*\.field\s+(.*?)\s*$/))){const f=m[1].match(/([^\s:]+):([^\s=]+)/);if(f)rows.push({kind:'field',name:f[1],detail:f[2],signature:owner+'->'+f[1]+':'+f[2],line:n,end:n,owner,static:/\bstatic\b/.test(m[1])});}
  else if((m=line.match(/^\s*\.method\s+(.*?)\s*$/))){const sig=m[1].match(/([^\s(]+)(\([^)]*\)\S+)/);if(sig){method={kind:'method',name:sig[1],detail:sig[2],signature:owner+'->'+sig[1]+sig[2],line:n,end:lines.length-1,owner,static:/\bstatic\b/.test(m[1])};rows.push(method);}}
  else if(/^\s*\.end method\b/.test(line)&&method){method.end=n;method=null;}
  if(/^\s*const-string(?:\/jumbo)?\b/.test(line)){const re=/"((?:[^"\\]|\\.)*)"/g;let s;while((s=re.exec(line)))strings.push({kind:'string',name:s[1],detail:'第 '+(n+1),line:n,ch:s.index+1,to:s.index+1+s[1].length,end:n,signature:s[1],owner});}
 });return {rows,strings,lines};
}
function jumpMember(row){closeModal();const cm=state.cm;cm.setSelection({line:row.line,ch:row.ch||0},{line:row.line,ch:row.to==null?row.ch||0:row.to});cm.scrollIntoView({line:row.line,ch:row.ch||0},90);updateEditorInfo();}
let navigationStrings=false;
function navigationDialog(query=''){
 if(state.editor.kind!=='dex'||state.editor.mode==='java')return jumpLine();
 const data=smaliOutline(state.cm.getValue()),rows=navigationStrings?data.strings:data.rows;
 const d=dialog('导航','<div class="navSearch" hidden><input aria-label="筛选导航" placeholder="搜索…" value="'+esc(query)+'"></div><div class="navRows"></div>',[['切换类型',()=>{navigationStrings=!navigationStrings;navigationDialog();}],['设置',navigationPreferences],['关闭',closeModal]]);d.classList.add('navigationDialog');
 const search=iconButton('search','搜索导航',()=>{const box=d.querySelector('.navSearch');box.hidden=!box.hidden;if(!box.hidden)box.querySelector('input').focus();});d.querySelector('h2').append(search);
 const draw=q=>{const list=d.querySelector('.navRows');list.replaceChildren();const filtered=rows.filter(r=>(r.name+' '+r.detail).toLowerCase().includes(q.toLowerCase()));for(const r of filtered){const b=document.createElement('button');b.className='navRow';b.innerHTML='<span class="memberIcon '+r.kind+'">'+({class:'C',field:'F',method:'M',string:'S'})[r.kind]+'</span><span class="navText">'+esc(r.name)+(r.kind==='string'?'':'<small>'+esc(r.detail)+'</small>')+'</span>';b.title=r.signature;hold(b,()=>memberActions(r),()=>jumpMember(r));list.append(b);}if(!filtered.length)list.innerHTML='<p class="muted">'+(navigationStrings?'未找到字符串出现位置。':'未找到成员。')+'</p>';};
 d.querySelector('input').oninput=e=>draw(e.target.value);draw(query);
}
function hold(button,longPress,click){let timer,held=false,x=0,y=0;button.onpointerdown=e=>{held=false;x=e.clientX;y=e.clientY;timer=setTimeout(()=>{held=true;act(longPress);},500);};button.onpointerup=button.onpointercancel=()=>clearTimeout(timer);button.onpointermove=e=>{if(Math.abs(e.clientX-x)>8||Math.abs(e.clientY-y)>8)clearTimeout(timer);};button.oncontextmenu=e=>{e.preventDefault();held=true;act(longPress);};button.onclick=()=>{if(held){held=false;return;}act(click);};}
// 文件/文件夹多选手势 —— 完全照搬 MP-Manager SwipeTouchListener + handleSwipe + handleMultiSelect
// 横滑一段距离后行跟随手指平移；松手时滑动够远即确认一次选择：
//   非多选 → 进入多选、设锚点 a、选中该行（规则一）
//   多选且有锚点 → 选中 锚点 a .. 当前行 的连续区间并重置锚点（规则三，可继续滑新区间）
//   多选且锚点已重置 → 重新设锚点
// 追加模式下单击某行 → toggle 追加/移除（选空则退出多选）；非追加模式单击 → 打开
function fileGestureBind(row,f,index,onOpen,onLongPress){
  const pane=state.panes[index];
  const key=fileKey(f);
  const pos=pane.items.indexOf(f);
  if(!pane.selected)pane.selected=new Set();
  if(pane.gestureAnchor===undefined)pane.gestureAnchor=null;
  let startX=0,startY=0,swiping=false,suppressClick=false,longTimer=null,paneEl=null,tx=0;
  const SLOP=12,CONFIRM=45; // 滑动起判阈值 / 确认阈值（px）
  // 参考 MP-Manager handleSwipe：横滑确认后触发一次选择
  const handleSwipe=()=>{
    state.active=index;
    if(!pane.appendMode){                 // 规则一：进入多选，锚点 = 当前行
      pane.appendMode=true;
      pane.gestureAnchor=pos;
      pane.selected=new Set([key]);
    } else if(pane.gestureAnchor!==null){ // 规则三：累加选中 锚点 a .. 当前行 区间（不清空既有选区），重置锚点
      const lo=Math.min(pane.gestureAnchor,pos),hi=Math.max(pane.gestureAnchor,pos);
      for(let j=lo;j<=hi;j++){const fj=pane.items[j];if(fj)pane.selected.add(fileKey(fj));} // 累加区间
      pane.gestureAnchor=null;
    } else {                              // 锚点已重置 → 重新设锚点 = 当前行
      pane.gestureAnchor=pos;
      pane.selected.add(key);
    }
    renderFiles();
  };
  // 参考 MP-Manager handleMultiSelect：追加模式下单击 → toggle 追加/移除；选空则退出多选
  const handleMultiSelect=()=>{
    state.active=index;
    if(pane.selected.has(key))pane.selected.delete(key);
    else pane.selected.add(key);
    if(!pane.selected.size){pane.appendMode=false;pane.gestureAnchor=null;}
    renderFiles();
  };
  row.addEventListener('pointerdown',e=>{
    activatePane(index);
    paneEl=row.closest('.pane');
    swiping=false;suppressClick=false;tx=0;
    startX=e.clientX;startY=e.clientY;
    longTimer=setTimeout(()=>{ // 长按不动 → 弹出文件操作菜单（对齐 MP：长按只弹菜单，不选中该行）
      if(!swiping){act(()=>onLongPress(f));suppressClick=true;}
    },500);
  },{passive:true});
  row.addEventListener('pointermove',e=>{
    const dx=e.clientX-startX,dy=e.clientY-startY;
    if(!swiping&&Math.abs(dx)>SLOP&&Math.abs(dx)>Math.abs(dy)){ // 判定为横向滑动
      swiping=true;clearTimeout(longTimer);
      paneEl.classList.add('dragging'); // 手势期间锁定窗格滚动
    }
    if(swiping){tx=dx;row.style.transform='translateX('+dx+'px)';} // 行跟随手指平移（视觉反馈）
  },{passive:true});
  const endGesture=e=>{
    clearTimeout(longTimer);
    if(!swiping)return;
    const confirmed=Math.abs(tx)>=CONFIRM;
    swiping=false;paneEl.classList.remove('dragging');
    row.style.transition='transform .18s ease';row.style.transform=''; // 回弹
    setTimeout(()=>{if(row.isConnected)row.style.transition='';},200);
    if(confirmed&&e.type==='pointerup'){suppressClick=true;handleSwipe();}
  };
  row.addEventListener('pointerup',endGesture,{passive:true});
  row.addEventListener('pointercancel',endGesture,{passive:true});
  row.addEventListener('click',()=>{
    if(suppressClick){suppressClick=false;return;} // 滑动手势后的合成 click，抑制一次
    if(pane.appendMode)handleMultiSelect();        // 多选模式 → 单击 toggle
    else onOpen(f,index);                        // 非多选模式 → 打开
  });
}
function fileGesture(row,f,index){fileGestureBind(row,f,index,openFile,fileActions);}
function recycleGesture(row,r,index){fileGestureBind(row,r,index,f=>recycleActions(f),recycleActions);}
const navigationOptions={class:[['usages','查找用法'],['subclasses','查找子类'],['signature','复制类签名'],['frida','复制为 Frida 片段'],['xposed','复制为 Xposed 片段']],field:[['usages','查找用法'],['signature','复制字段签名'],['get','复制字段 get 代码'],['put','复制字段 put 代码'],['frida','复制为 Frida 片段'],['xposed','复制为 Xposed 片段']],method:[['usages','查找用法'],['overrides','查找重写方法'],['signature','复制方法签名'],['code','复制方法代码'],['frida','复制为 Frida 片段'],['xposed','复制为 Xposed 片段']]};
function navigationPreferences(){const d=dialog('菜单',Object.keys(navigationOptions).map(kind=>'<h3>'+({class:'类',field:'字段',method:'方法'})[kind]+'</h3>'+navigationOptions[kind].map(([id,label])=>'<label class="toggleRow"><span>'+label+'</span><input type="checkbox" data-nav-option="'+kind+'.'+id+'" '+(!(pref.navigation&&pref.navigation[kind+'.'+id]===false)?'checked':'')+'></label>').join('')).join(''),[['取消',navigationDialog],['确定',()=>{pref.navigation={};d.querySelectorAll('[data-nav-option]').forEach(i=>pref.navigation[i.dataset.navOption]=i.checked);savePref();navigationDialog();}]]);d.classList.add('navigationPrefs');}
function javaTypes(descriptor){const result=[];let i=0;while(i<descriptor.length){let start=i;while(descriptor[i]==='[')i++;if(descriptor[i]==='L'){let end=descriptor.indexOf(';',i);if(end<0)break;i=end+1;}else i++;const raw=descriptor.slice(start,i),names={V:'void',Z:'boolean',B:'byte',S:'short',C:'char',I:'int',J:'long',F:'float',D:'double'};result.push(raw.startsWith('[')?raw.replace(/\//g,'.'):raw.startsWith('L')?raw.slice(1,-1).replace(/\//g,'.'):names[raw]||raw);}return result;}
function memberSnippet(row,kind){const cls=row.owner.slice(1,-1).replace(/\//g,'.'),q=JSON.stringify,name=JSON.stringify(row.name),params=row.kind==='method'?javaTypes(row.detail.slice(1,row.detail.indexOf(')'))):[];
 if(kind==='frida'){
  const head='Java.perform(function () {\n    const Target = Java.use('+q(cls)+');\n';
  if(row.kind==='class')return head+'    console.log(Target.class.getName());\n});';
  if(row.kind==='field')return head+(row.static?'    console.log(Target['+name+'].value);':'    Java.choose('+q(cls)+', {\n        onMatch: function (instance) { console.log(instance['+name+'].value); },\n        onComplete: function () {}\n    });')+'\n});';
  const member=row.name==='<init>'?'$init':row.name;return head+'    const method = Target['+q(member)+'].overload('+params.map(q).join(', ')+');\n    method.implementation = function () {\n        console.log('+q(cls+'.'+row.name)+');\n        return method.apply(this, arguments);\n    };\n});';
 }
 const head='类<?> target = XposedHelpers.findClass('+q(cls)+', lpparam.classLoader);\n';
 if(row.kind==='class')return head+'XposedBridge.log(target.getName());';
 if(row.kind==='field'){const type=({Z:'Boolean',B:'Byte',S:'Short',C:'Char',I:'Int',J:'Long',F:'Float',D:'Double'})[row.detail]||'Object';return head+(row.static?'XposedHelpers.getStatic'+type+'Field(target, '+name+');':'// instance must be an object of '+cls+'\nXposedHelpers.get'+type+'Field(instance, '+name+');');}
 return head+'XposedHelpers.'+(row.name==='<init>'?'findAndHookConstructor':'findAndHookMethod')+'(target, '+(row.name==='<init>'?'':name+', ')+params.map(p=>JSON.stringify(p)).concat(['new XC_MethodHook() {\n    @Override protected void beforeHookedMethod(MethodHookParam param) {\n        XposedBridge.log('+q(cls+'.'+row.name)+');\n    }\n}']).join(', ')+');';
}
function memberActions(row){if(row.kind==='string')return options(row.name,[{label:'跳转到出现位置',icon:'jump',run:()=>jumpMember(row)},{label:'复制字符串',icon:'copy',run:()=>rpc('clipboard',{text:row.name})}]);const rows=[{label:'跳转到定义',icon:'jump',run:()=>jumpMember(row)}];for(const [id,label]of navigationOptions[row.kind]){if(pref.navigation&&pref.navigation[row.kind+'.'+id]===false)continue;rows.push({label,icon:id==='usages'||id==='subclasses'||id==='overrides'?'search':'copy',run:async()=>{
 if(['usages','subclasses','overrides'].includes(id))return memberQuery(row,id);
 let text=row.signature;if(id==='code')text=state.cm.getValue().split('\n').slice(row.line,row.end+1).join('\n');
 if(id==='get'||id==='put'){const suffix=['J','D'].includes(row.detail)?'-wide':row.detail[0]==='L'||row.detail[0]==='['?'-object':({Z:'-boolean',B:'-byte',C:'-char',S:'-short'})[row.detail]||'';text=(row.static?'s':'i')+(id==='get'?'get':'put')+suffix+' v0, '+(row.static?'':'p0, ')+row.signature;}
 if(id==='frida'||id==='xposed')text=memberSnippet(row,id);await rpc('clipboard',{text});toast('已复制');}});}options(row.name,rows);}
async function memberQuery(row,op){
 if(state.editor.dirty){if(!await confirmBox('搜索前保存？','交叉引用使用已编译的 DEX 工程。请先保存此类。','保存'))return;await saveEditor();}
 const result=await rpc('dexQuery',{op,kind:row.kind,signature:row.signature,owner:row.owner});
 const d=dialog(op==='subclasses'?'子类':op==='overrides'?'重写方法':'查找用法','<p class="muted">'+esc(result.scope)+(result.truncated?' · 前 1,000 条结果':'')+'</p><div class="queryRows"></div>',[['关闭',closeModal]]);d.classList.add('navigationDialog');
 const list=d.querySelector('.queryRows');if(!result.rows.length)list.textContent='所选 DEX 文件中没有匹配项。';for(const r of result.rows){const b=document.createElement('button');b.className='navRow';b.innerHTML='<span class="memberIcon method">M</span><span class="navText">'+esc(r.class.slice(1,-1).replace(/\//g,'.'))+'<small>'+esc(r.method||r.reason)+'</small></span>';b.onclick=()=>act(async()=>{if(!await canLeave())return;closeModal();await openClass(r.class,'smali');const data=smaliOutline(state.cm.getValue());const m=data.rows.find(x=>x.signature===r.method);const start=m?m.line:0,end=m?m.end:data.lines.length-1;let line=start;for(let n=start;n<=end;n++)if(data.lines[n].includes(r.needle)){line=n;break;}jumpMember({line});});list.append(b);}
}
function lineRange(){const cm=state.cm,from=cm.getCursor('from'),to=cm.getCursor('to');const last=to.line>from.line&&to.ch===0?to.line-1:to.line;return {from:{line:from.line,ch:0},to:{line:last,ch:cm.getLine(last).length},first:from.line,last,text:Array.from({length:last-from.line+1},(_,i)=>cm.getLine(from.line+i)).join('\n')};}
const lineTools=[['copy','复制行','copy'],['cut','剪切行','edit'],['delete','删除行','trash'],['empty','空行','file'],['replace','替换行','download'],['duplicate','重复行','plus'],['upper','转换为大写','text'],['lower','转换为小写','text'],['indent','增加缩进','forward'],['outdent','减少缩进','back'],['comment','切换注释','code'],['format','重新格式化代码','edit'],['customize','自定义','settings'],['registers','分析寄存器','dex'],['increase','增加寄存器','export']];
function lineMenu(){if(state.editor.readOnly)return;const smali=fontKey()==='smali';menu(lineTools.filter(([id])=>id==='customize'||(!(pref.lineTools&&pref.lineTools[id]===false)&&(!['registers','increase'].includes(id)||smali))).map(([id,label,ic])=>({icon:ic,label,run:()=>editLines(id)})));$('#menuLayer .overflowMenu').classList.add('lineMenu');}
function customizeLines(){const d=dialog('自定义',lineTools.filter(x=>x[0]!=='customize').map(([id,label])=>'<label class="toggleRow"><span>'+label+'</span><input type="checkbox" data-line-tool="'+id+'" '+(!(pref.lineTools&&pref.lineTools[id]===false)?'checked':'')+'></label>').join(''),[['取消',closeModal],['确定',()=>{pref.lineTools={};d.querySelectorAll('[data-line-tool]').forEach(i=>pref.lineTools[i.dataset.lineTool]=i.checked);savePref();closeModal();}]]);}
async function editLines(op){const cm=state.cm,r=lineRange();if(op==='customize')return customizeLines();if(op==='registers')return analyzeRegisters();if(op==='increase')return increaseRegisters();
 if(op==='copy'||op==='cut'){await rpc('clipboard',{text:r.text});if(op==='copy')return toast('行已复制');}
 if(op==='delete'||op==='cut'){let from=r.from,to=r.last<cm.lastLine()?{line:r.last+1,ch:0}:r.to;if(r.last===cm.lastLine()&&r.first>0)from={line:r.first-1,ch:cm.getLine(r.first-1).length};cm.replaceRange('',from,to,'+delete');return;}
 if(op==='replace'){const d=dialog('替换行','<textarea id="replaceLines" rows="4" spellcheck="false">'+esc(r.text)+'</textarea>',[['取消',closeModal],['确定',()=>{const text=$('#replaceLines').value;closeModal();cm.replaceRange(text,r.from,r.to,'+input');}]]);d.querySelector('textarea').focus();return;}
 if(op==='duplicate'){cm.replaceRange('\n'+r.text,r.to,r.to,'+input');return;}
 if(op==='upper'||op==='lower'){const selected=cm.somethingSelected();const text=selected?cm.getSelection():r.text;const next=op==='upper'?text.toUpperCase():text.toLowerCase();if(selected)cm.replaceSelection(next,'around','+input');else cm.replaceRange(next,r.from,r.to,'+input');return;}
 let text=r.text;
 if(op==='empty')text=text.split('\n').map(()=>'').join('\n');
 if(op==='indent')text=text.split('\n').map(l=>'    '+l).join('\n');
 if(op==='outdent')text=text.split('\n').map(l=>l.replace(/^( {1,4}|\t)/,'')).join('\n');
 if(op==='comment'){const xml=fontKey()==='xml',prefix=fontKey()==='smali'?'#':'//',ls=text.split('\n');const remove=ls.every(l=>!l.trim()||(xml?/^\s*<!--.*-->\s*$/.test(l):l.trim().startsWith(prefix)));text=ls.map(l=>!l.trim()?l:remove?xml?l.replace(/^(\s*)<!--\s?(.*?)\s?-->\s*$/,'$1$2'):l.replace(new RegExp('^(\\s*)'+(prefix==='#'?'#':'//')+' ?'),'$1'):xml?'<!-- '+l+' -->':l.replace(/^(\s*)/,'$1'+prefix+' ')).join('\n');}
 if(op==='format'){
  if(fontKey()==='smali'){const all=cm.getValue().split('\n');let inside=false;const formatted=all.map(l=>{const t=l.trim();if(t.startsWith('.method '))inside=true;if(t==='.end method')inside=false;return t?((inside&&!t.startsWith('.method '))?'    ':'')+t:'';}).join('\n');cm.replaceRange(formatted,{line:0,ch:0},{line:cm.lastLine(),ch:cm.getLine(cm.lastLine()).length},'+input');return;}
  cm.operation(()=>{for(let n=r.first;n<=r.last;n++)cm.indentLine(n,'smart');});return;
 }
 cm.replaceRange(text,r.from,r.to,'+input');
}
function currentRegisters(){const data=smaliOutline(state.cm.getValue()),line=state.cm.getCursor().line,m=data.rows.find(r=>r.kind==='method'&&line>=r.line&&line<=r.end);if(!m)throw Error('请将光标置于方法内。');const types=javaTypes(m.detail.slice(1,m.detail.indexOf(')'))),params=(m.static?0:1)+types.reduce((n,t)=>n+(['long','double'].includes(t)?2:1),0);let decl=-1,count=0,kind='';for(let n=m.line+1;n<=m.end;n++){const match=data.lines[n].match(/^\s*\.(registers|locals)\s+(0x[\da-f]+|\d+)/i);if(match){decl=n;kind=match[1];count=Number(match[2]);break;}}if(decl<0)throw Error('此方法没有寄存器声明（抽象/原生方法没有代码）。');const locals=kind==='locals'?count:count-params;if(locals<0)throw Error('寄存器数量小于其参数数量。');return {data,m,params,locals,total:locals+params,decl,kind,count};}
function codeSegments(line,fn){return line.replace(/"(?:[^"\\]|\\.)*"|#.*|\b[vp]\d+(?=[\s,}.]|$)/g,(token,offset)=>token[0]==='"'||token[0]==='#'||offset>0&&/[\w$/:.]/.test(line[offset-1])?token:fn(token,offset));}
function analyzeRegisters(){const r=currentRegisters(),uses=new Map();for(let n=r.m.line+1;n<r.m.end;n++)codeSegments(r.data.lines[n],token=>{if(!uses.has(token))uses.set(token,[]);uses.get(token).push(n+1);return token;});dialog('分析寄存器','<p>'+esc(r.m.name+r.m.detail)+'</p><p>总计：'+r.total+' · 局部变量：'+r.locals+' · 参数词数：'+r.params+'</p><div class="registerList">'+Array.from(uses).sort((a,b)=>a[0].localeCompare(b[0],undefined,{numeric:true})).map(([reg,lines])=>'<p><b>'+reg+'</b> '+(reg[0]==='p'?'= v'+(r.locals+Number(reg.slice(1))):Number(reg.slice(1))>=r.locals?'= p'+(Number(reg.slice(1))-r.locals):'local')+'<small>第 '+Array.from(new Set(lines)).join(', ')+'</small></p>').join('')+'</div><p class="muted">本方法内的使用位置；这并非活性分析。</p>',[['关闭',closeModal]]);}
async function increaseRegisters(){const r=currentRegisters(),s=await ask('增加寄存器','1','额外的局部寄存器');if(s===null)return;const n=Number(s);if(!Number.isInteger(n)||n<1||n>255||r.total+n>65535)throw Error('请输入 1–255 个额外寄存器（不超过 DEX 上限）。');let lines=r.data.lines.slice(r.m.line,r.m.end+1);for(const l of lines){const range=l.match(/\{([vp])(\d+)\s*\.\.\s*([vp])(\d+)\}/);if(range&&(Number(range[2])+(range[1]==='p'?r.locals:0))<r.locals&&(Number(range[4])+(range[3]==='p'?r.locals:0))>=r.locals)throw Error('一个 invoke-range 跨越了局部寄存器与参数寄存器。增加寄存器前请先改写该范围。');}
 lines=lines.map(l=>codeSegments(l,t=>t[0]==='v'&&Number(t.slice(1))>=r.locals?'v'+(Number(t.slice(1))+n):t));const at=r.decl-r.m.line;lines[at]=lines[at].replace(/(\.(?:registers|locals)\s+)(0x[\da-f]+|\d+)/i,'$1'+String(r.count+n));state.cm.replaceRange(lines.join('\n'),{line:r.m.line,ch:0},{line:r.m.end,ch:r.data.lines[r.m.end].length},'+input');toast('寄存器数量已增加。保存将汇编并校验该方法。');}
function markSmaliMethods(){/* 方法 backgrounds come from the visible-line tokenizer. */}

function animateContent(el,direction=1){
 if(el&&el.animate&&motionEnabled())el.animate([{opacity:.6,transform:'translateX('+(direction*12)+'px)'},{opacity:1,transform:'translateX(0)'}],{duration:170,easing:'cubic-bezier(.2,0,0,1)'});
}

// Single-finger drags keep native scrolling. Only an intentional horizontal
// swipe from the left edge captures input; a second finger cancels this gesture.
function installDrawerGestures(){
 let gesture=null,suppressClick=0;
 const stop=e=>{if(e.cancelable)e.preventDefault();e.stopPropagation();};
 document.addEventListener('touchstart',e=>{
  if(e.touches.length!==1){if(gesture&&gesture.dragging)finish(true);gesture=null;return;}
  if($('#menuLayer').children.length||$('#modalLayer').children.length)return;
  const point=e.touches[0],drawer=$('#drawerLayer .drawer');
  if(!drawer&&(point.clientX>24||e.target.closest('input,textarea,select,.CodeMirror')))return;
  gesture={x:point.clientX,y:point.clientY,open:!!drawer,drawer,dragging:false,dx:0,width:0};
 },{passive:true,capture:true});
 document.addEventListener('touchmove',e=>{
  if(!gesture||e.touches.length!==1)return;const g=gesture,p=e.touches[0],dx=p.clientX-g.x,dy=p.clientY-g.y;
  if(!g.dragging){
   if(Math.abs(dy)>12&&Math.abs(dy)>Math.abs(dx)){gesture=null;return;}
   if(Math.abs(dx)<12)return;
   if(Math.abs(dx)<Math.abs(dy)*1.4||(!g.open&&dx<0)||(g.open&&dx>0)){gesture=null;return;}
   if(!g.open){act(()=>openDrawer(true));g.drawer=$('#drawerLayer .drawer');}
   if(!g.drawer){gesture=null;return;}g.width=g.drawer.getBoundingClientRect().width||Math.min(window.innerWidth*.82,360);g.dragging=true;g.drawer.classList.add('dragging');
  }
  stop(e);g.dx=dx;const progress=g.open?Math.max(0,Math.min(1,1+dx/g.width)):Math.max(0,Math.min(1,dx/g.width));
  g.progress=progress;g.drawer.style.transform='translateX('+((progress-1)*100)+'%)';const shade=$('#drawerLayer .drawerShade');if(shade){shade.style.animation='none';shade.style.opacity=String(progress);}
 },{passive:false,capture:true});
 function finish(cancel){
  const g=gesture;gesture=null;if(!g||!g.dragging)return;suppressClick=Date.now()+350;
  const open=cancel?g.open:g.open?g.progress>.65:g.progress>.25;
  if(!open){closeDrawer();return;}
  g.drawer.classList.remove('dragging');g.drawer.classList.add('settling');
  // Keep the current transform for one frame, then settle from the finger position.
  window.requestAnimationFrame(()=>{if(!g.drawer.isConnected)return;g.drawer.style.transform='translateX(0)';const shade=$('#drawerLayer .drawerShade');if(shade)shade.style.opacity='1';setTimeout(()=>g.drawer.classList.remove('settling'),220);});
 }
 document.addEventListener('touchend',e=>{if(gesture&&gesture.dragging)stop(e);finish(false);},{passive:false,capture:true});
 document.addEventListener('touchcancel',()=>finish(true),{passive:true,capture:true});
 document.addEventListener('click',e=>{if(Date.now()<suppressClick){e.preventDefault();e.stopImmediatePropagation();}},{capture:true});
}

// Font changes use the code position under the gesture as their scroll anchor.
// 捏合过程只改 CSS 字号（浏览器原生排版，文字实时清晰、零测量开销）；松手时一次性 refresh 重测量、按锚点校正滚动并持久化。
function setEditorFont(value,anchor=null,persist=true){
 const cm=state.cm;if(!cm)return;const n=Number(value);if(!Number.isFinite(n))return;
 const font=Math.round(Math.max(4,Math.min(32,n))*10)/10,key=fontKey();
 if(!anchor){const r=cm.getWrapperElement().getBoundingClientRect(),x=r.left+Math.min(r.width/2,120),y=r.top+Math.min(r.height/2,100);const pos=cm.coordsChar({left:x,top:y},'window'),coords=cm.charCoords(pos,'window');anchor={pos,x:coords.left,y:coords.top};}
 if(!pref.fonts||typeof pref.fonts!=='object'||Array.isArray(pref.fonts))pref.fonts={};pref.fonts[key]=font;
 cm.getWrapperElement().style.fontSize=font+'px';cm.refresh();
 const now=cm.charCoords(anchor.pos,'window'),scroll=cm.getScrollInfo();cm.scrollTo(Math.max(0,scroll.left+now.left-anchor.x),Math.max(0,scroll.top+now.top-anchor.y));
 if(persist)savePref();return font;
}
function installEditorZoom(cm,editor){
 const wrapper=cm.getWrapperElement(),host=wrapper.parentElement,badge=document.createElement('div');badge.className='zoomBadge';badge.setAttribute('aria-live','polite');badge.hidden=true;host.append(badge);
 let pinch=null,consuming=false,frame=0,pendingFont=null,hideTimer=0,suppressClick=0,firstSelection=null,disposed=false;
 const listeners=[];function on(type,fn,options){wrapper.addEventListener(type,fn,options);listeners.push([type,fn,options]);}
 function block(e){if(e.cancelable)e.preventDefault();e.stopImmediatePropagation();}
 function show(font){badge.textContent=Number(font.toFixed(1))+' px';badge.hidden=false;clearTimeout(hideTimer);hideTimer=setTimeout(()=>badge.hidden=true,850);}
 function flush(){frame=0;if(disposed||state.cm!==cm||state.editor!==editor||pendingFont===null||!pinch)return;const font=Math.round(Math.max(4,Math.min(32,pendingFont))*10)/10;wrapper.style.fontSize=font+'px';show(font);}
 function finish(){if(frame){window.cancelAnimationFrame(frame);frame=0;}if(pinch&&pendingFont!==null&&state.cm===cm&&!disposed)setEditorFont(pendingFont,pinch.anchor,true);pendingFont=null;pinch=null;wrapper.classList.remove('pinchZooming');}
 on('touchstart',e=>{
  if(pref.pinchZoom===false||disposed)return;
  if(e.touches.length===1&&!consuming){firstSelection=cm.listSelections();return;}
  if(e.touches.length<2)return;block(e);consuming=true;
  if(pinch)return;
  const a=e.touches[0],b=e.touches[1],x=(a.clientX+b.clientX)/2,y=(a.clientY+b.clientY)/2,distance=Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY);
  if(distance<4)return;
  const pos=cm.coordsChar({left:x,top:y},'window'),coords=cm.charCoords(pos,'window');
  pinch={distance,font:editorFont(),ids:[a.identifier,b.identifier],anchor:{pos,x:coords.left,y:coords.top}};
  const cancel=new Event('touchcancel',{bubbles:false});cancel.zoomReset=true;cm.getScrollerElement().dispatchEvent(cancel);if(firstSelection)cm.setSelections(firstSelection);wrapper.classList.add('pinchZooming');
 },{capture:true,passive:false});
 on('touchmove',e=>{
  if(!consuming)return;block(e);if(!pinch)return;
  const points=Array.from(e.touches),a=points.find(t=>t.identifier===pinch.ids[0]),b=points.find(t=>t.identifier===pinch.ids[1]);if(!a||!b)return;
  pendingFont=Math.max(4,Math.min(32,pinch.font*Math.hypot(a.clientX-b.clientX,a.clientY-b.clientY)/pinch.distance));if(!frame)frame=window.requestAnimationFrame(flush);
 },{capture:true,passive:false});
 function end(e){if(!consuming){firstSelection=null;return;}block(e);const points=Array.from(e.touches);if(pinch&&!pinch.ids.every(id=>points.some(t=>t.identifier===id)))finish();if(!e.touches.length){finish();consuming=false;firstSelection=null;suppressClick=Date.now()+350;}}
 on('touchend',end,{capture:true,passive:false});
 on('touchcancel',e=>{if(e.zoomReset)return;if(consuming)block(e);finish();consuming=false;firstSelection=null;suppressClick=Date.now()+350;},{capture:true,passive:false});
 on('click',e=>{if(consuming||Date.now()<suppressClick)block(e);},{capture:true});
 on('wheel',e=>{if(!(e.ctrlKey||e.metaKey))return;block(e);show(setEditorFont(editorFont()+(e.deltaY<0 ? .5 : -.5)));},{capture:true,passive:false});
 on('keydown',e=>{if(!(e.ctrlKey||e.metaKey)||!['+','=','-','0'].includes(e.key))return;block(e);show(setEditorFont(e.key==='0'?defaultFonts[fontKey()]:editorFont()+(e.key==='-'?-.5:.5)));},{capture:true});
 state.disposeEditor=()=>{finish();disposed=true;listeners.forEach(([name,fn,options])=>wrapper.removeEventListener(name,fn,options));clearTimeout(hideTimer);clearTimeout(editor.markTimer);clearTimeout(editor.infoTimer);badge.remove();state.disposeEditor=null;};
}
function zoomDialog(){
 const d=dialog('文字大小','<p class="muted">用两根手指捏合可调整代码大小。Java、Smali、XML 和文本大小分别记忆。</p><div class="zoomControls"><button id="zoomOut" aria-label="更小的文字">−</button><output id="zoomValue"></output><button id="zoomIn" aria-label="更大的文字">+</button></div><input id="zoomRange" aria-label="文字大小" type="range" min="4" max="32" step="0.5">',[['重置',()=>change(defaultFonts[fontKey()])],['完成',closeModal]]);
 function change(n){const font=setEditorFont(n);d.querySelector('#zoomValue').textContent=font+' px';d.querySelector('#zoomRange').value=String(font);}
 d.querySelector('#zoomOut').onclick=()=>change(editorFont()-.5);d.querySelector('#zoomIn').onclick=()=>change(editorFont()+.5);d.querySelector('#zoomRange').oninput=e=>change(e.target.value);change(editorFont());
}
installDrawerGestures();
if(window.matchMedia){const reduced=window.matchMedia('(prefers-reduced-motion: reduce)');if(reduced.addEventListener)reduced.addEventListener('change',syncMotion);else if(reduced.addListener)reduced.addListener(syncMotion);}

CodeMirror.defineMode('smali',()=>({token(stream){if(stream.eatSpace())return null;if(stream.match(/^#.*/))return 'comment';if(stream.match(/^"(?:[^"\\]|\\.)*"/))return 'string';if(stream.match(/^\.method\b/))return 'keyword line-background-smaliMethod';if(stream.match(/^\.[a-zA-Z_-]+/))return 'keyword';if(stream.match(/^L[^\s;]+;/))return 'type';if(stream.match(/^-?(?:0x[0-9a-f]+|\d+)(?:[tLsSfFdD])?/i))return 'number';if(stream.match(/^(?:public|private|protected|static|final|synthetic|abstract|constructor|native)\b/))return 'def';if(stream.match(/^:[\w_]+/))return 'meta';stream.next();return null;}}));
async function openText(args){const doc=await rpc('read',args);state.editor={...doc,args,kind:'file',dirty:false,readOnly:false,returnScreen:state.screen==='editor'?'files':state.screen};renderEditor();}
async function openClass(type,mode){const data=await rpc('dexCode',{class:type,mode,decompiler:state.dex.mode});if(!state.dex.history.includes(type))state.dex.history.unshift(type);state.editor={...data,kind:'dex',dirty:false,readOnly:false,returnScreen:'dex'};renderEditor();}
function updateEditorInfo(){if(!state.cm)return;const cm=state.cm,p=cm.getCursor(),e=state.editor;const el=$('#cursorInfo');let detail=e.binaryXml?'AXML':e.mode==='java'?'JAVA':e.kind==='dex'?'SMALI':e.kind==='arsc'?'ARSC':'';if(e.kind==='dex'&&e.mode!=='java'&&e.outline){const method=e.outline.rows.find(r=>r.kind==='method'&&p.line>=r.line&&p.line<=r.end);if(method)detail='['+(method.line+1)+' - '+(method.end+1)+'] : '+method.name+'()';}if(el)el.textContent=(p.line+1)+':'+(p.ch+1)+(cm.somethingSelected()?' ('+cm.getSelection().length+')':'')+(detail?' | '+detail:'');const name=$('#docName');if(name)name.textContent=(e.dirty?'*':'')+(e.kind==='dex'?e.name.replace(/\.(java|smali)$/,''):e.name);}

const defaultFonts={java:6,xml:7.5,smali:14,text:13};
function editorFont(){const key=fontKey(),value=Number(pref.fonts&&pref.fonts[key]);return Number.isFinite(value)&&value>0?Math.min(32,Math.max(4,value)):defaultFonts[key];}
function fontKey(){const e=state.editor,name=(e.name||'').toLowerCase();return e.mode==='java'||name.endsWith('.java')?'java':e.binaryXml||name.endsWith('.xml')?'xml':e.kind==='dex'||name.endsWith('.smali')?'smali':'text';}
function renderEditor(){if(state.disposeEditor)state.disposeEditor();state.screen='editor';const e=state.editor,bar=$('#toolbar');bar.replaceChildren(iconButton('menu','打开导航',openDrawer));if(e.readOnly){const t=document.createElement('div');t.className='titleblock';t.innerHTML=`<h1 class="title editorTitle">${esc(e.name)}</h1>`;bar.append(t,...(e.kind==='arsc'?[]:[iconButton('compass','查看 Smali',toggleJava)]),iconButton('more','编辑器菜单',editorMenu));}else{const actions=document.createElement('div');actions.className='editorActions';actions.append(iconButton(e.kind==='dex'?'compass':'pin',e.kind==='dex'?(e.mode==='java'?'查看 Smali':'导航'):'复制路径',e.kind==='dex'?(e.mode==='java'?toggleJava:navigationDialog):()=>rpc('clipboard',{text:e.kind==='arsc'?(e.ref||''):(e.path||'')}).then(()=>toast(e.kind==='arsc'?'资源引用已复制':'路径已复制'))),iconButton('undo','撤销',()=>state.cm.undo()),iconButton('redo','重做',()=>state.cm.redo()),iconButton('save','保存',saveEditor),iconButton('edit','行工具',lineMenu));bar.append(actions,iconButton('more','编辑器菜单',editorMenu));}
 $('#subbar').innerHTML=`<div class="editorInfo"><span id="docName">${esc(e.kind==='dex'?e.name.replace(/\.(java|smali)$/,''):e.name)}</span><span id="cursorInfo">1:1</span><button class="editorOverflow" aria-label="编辑器选项">${icon('more')}</button></div>`;$('.editorOverflow').onclick=editorMenu;$('#content').innerHTML='<div id="codehost" style="height:100%"></div>';$('#content').classList.toggle('javaEditor',fontKey()==='java');const mode=e.binaryXml||e.name.endsWith('.xml')?'xml':e.name.endsWith('.smali')?'smali':'text/x-java';const cm=CodeMirror($('#codehost'),{value:e.text,mode,theme:'cyber',lineNumbers:pref.lines,lineWrapping:pref.wrap,readOnly:e.readOnly,tabSize:4,indentUnit:4,viewportMargin:8,workTime:20,workDelay:100,inputStyle:'contenteditable',spellcheck:false,autocorrect:false,autocapitalize:false,extraKeys:{'Ctrl-S':()=>act(saveEditor),'Ctrl-F':findText,'Cmd-S':()=>act(saveEditor),'Cmd-F':findText}});state.cm=cm;cm.getWrapperElement().style.fontSize=editorFont()+'px';e.cleanGeneration=cm.changeGeneration(true);if(e.kind==='dex'&&e.mode!=='java')e.outline=smaliOutline(e.text);cm.on('change',()=>{e.dirty=!cm.isClean(e.cleanGeneration);e.outline=null;updateEditorInfo();});cm.on('cursorActivity',updateEditorInfo);cm.refresh();installEditorZoom(cm,e);updateEditorInfo();markSmaliMethods();animateContent($('#codehost'));const bottom=$('#bottom');bottom.replaceChildren();bottom.classList.add('editorBottom');if(!e.readOnly)symbolBar(text=>{cm.replaceSelection(text==='→'?'    ':text);cm.focus();});}
async function saveEditor(){
 const e=state.editor,cm=state.cm;if(e.saving)return e.saving;if(e.readOnly)return false;if(!e.dirty)return true;
 const text=cm.getValue(),generation=cm.changeGeneration(true);cm.setOption('readOnly',true);document.body.classList.add('savingCode');
 e.saving=(async()=>{try{
  if(e.kind==='arsc'){const entry=e.entry||{};if(entry.complex){const items=arscParseComplexXml(text);if(!items.length)throw Error('XML 中没有可保存的集合项');for(const it of items)await rpc('arscEdit',{session:e.session,op:'setValue',id:e.id,config:e.config||'',valueIndex:it.index,type:it.type,value:it.value});toast('已更新 '+items.length+' 个集合值');}else{const value=arscParseEntryXml(text,entry.name);await rpc('arscEdit',{session:e.session,op:'setValue',id:e.id,config:e.config||'',valueIndex:-1,type:entry.type||3,value:value});toast('资源值已更新');}if(state.arsc){state.arsc.dirty=true;arscTouch(state.arsc);}}
  else if(e.kind==='dex'){const r=await rpc(e.mode==='java'?'dexJavaSave':'dexSave',{class:e.class,text});state.dex.modified=true;toast(r.message);}
  else{const result=await rpc('save',{id:e.id,text,backup:pref.backupOnSave!==false});toast('已保存：'+result.path);if(e.args.archive){e.args={archive:result.path,entry:e.args.entry,path:result.path};alertBox('已保存编辑后的归档',result.path+(result.needsSigning?'\n安装前请先签名此 APK 副本。':''));}}
  e.text=text;e.cleanGeneration=generation;e.dirty=!cm.isClean(generation);updateEditorInfo();return !e.dirty;
 }catch(error){if(e.mode==='java'){
  const m=String(error.message).match(/at line (\d+)|\.java:(\d+)/),line=m?Number(m[1]||m[2]):null;
  const d=dialog('Java 无法编译','<pre class="compileError">'+esc(error.message)+'</pre>',[['继续编辑',closeModal],...(line?[['跳转到第 '+line+' 行',()=>{closeModal();cm.setCursor(Math.max(0,Math.min(cm.lastLine(),line-1)),0);cm.scrollIntoView(cm.getCursor(),80);cm.focus();}]]:[])]);d.classList.add('compileDialog');return false;
 }throw error;}finally{e.saving=null;cm.setOption('readOnly',!!e.readOnly);document.body.classList.remove('savingCode');}})();return e.saving;
}
async function canLeave(){if(state.editor&&state.editor.saving)return await state.editor.saving;if(!(state.editor&&state.editor.dirty))return true;if(state.editor.kind==='dex')return await saveEditor();return new Promise(resolve=>{dialog('未保存的更改','<p>离开之前保存更改？</p>',[['保存',async()=>{if(!await saveEditor())return;modalCancel=null;closeModal();resolve(true);}],['取消',()=>{modalCancel=null;closeModal();resolve(false);}],['放弃',()=>{modalCancel=null;closeModal();resolve(true);}]],()=>resolve(false));});}
async function toggleJava(){if(!await canLeave())return;await openClass(state.editor.class,state.editor.mode==='java'?'smali':'java');}
async function reloadEditor(){if(!await canLeave())return;const e=state.editor;if(e.kind==='dex')await openClass(e.class,e.mode);else if(e.kind==='arsc')await arscOpenEntryXml(e.entry&&e.entry.package,'',e.id,e.config||'');else await openText(e.args);}
function editorMenu(){const e=state.editor;const rows=[{icon:'save',label:e.mode==='java'?'编译并保存 Java':'保存',run:saveEditor},...(e.kind==='dex'?[{icon:'check',label:'保存并签名 APK',run:saveAndSignProject}]:[]),{icon:'refresh',label:'重新加载',run:reloadEditor}];if(e.kind==='dex'&&e.mode==='java')rows.push({icon:'compass',label:'查看 Smali',run:toggleJava});if(e.kind==='dex')rows.push({icon:'switch',label:e.mode==='java'?'切换反编译器':'查看 Java',run:e.mode==='java'?switchDecompiler:toggleJava});rows.push({icon:'search',label:'搜索',run:findText},{icon:'edit',label:'查找替换',run:replaceText},{icon:'jump',label:'跳转到行',run:jumpLine},{icon:'wrap',label:'自动换行',end:pref.wrap?'已开启':'已关闭',run:()=>{pref.wrap=!pref.wrap;savePref();state.cm.setOption('lineWrapping',pref.wrap);}},{icon:'export',label:'导出',run:()=>exportText(state.cm.getValue(),e.name)},{icon:'text',label:'文字大小 / 缩放',run:zoomDialog},{icon:'settings',label:'设置',run:preferences},{icon:'close',label:'关闭',run:window.goBack});menu(rows);}
function switchDecompiler(){options('JADX 反编译模式',[['auto','自动 — 结构化 Java'],['simple','简单 — 较少转换'],['fallback','回退 — 指令视图']].map(([mode,label])=>({label,run:async()=>{state.dex.mode=mode;await reloadEditor();}})));}
function replaceText(){const cm=state.cm;if(state.editor.readOnly)return;const d=dialog('查找替换','<label>查找</label><input id="replaceFind" spellcheck="false"><label>替换为</label><input id="replaceWith" spellcheck="false"><p id="replaceCount" class="muted">匹配完全一致的文本（区分大小写）。</p>',[['关闭',closeModal],['替换下一处',()=>replace(false)],['全部替换',()=>replace(true)]]);function replace(all){const query=d.querySelector('#replaceFind').value,value=d.querySelector('#replaceWith').value;if(!query)return;let count=0;cm.operation(()=>{const cursor=cm.getSearchCursor(query,all?{line:0,ch:0}:cm.getCursor('from'),false);while(cursor.findNext()){const from=cursor.from();cursor.replace(value);count++;if(!all){cm.setCursor(cursor.to());cm.scrollIntoView(from,60);break;}}});d.querySelector('#replaceCount').textContent=count+' 处替换';}}
let lastSearch='';function findText(){const d=dialog('搜索',`<input id="searchText" value="${esc(lastSearch)}" placeholder="搜索文本" spellcheck="false"><div id="searchResult" class="progresslabel"></div>`,[['关闭',closeModal],['上一处',()=>find(-1)],['下一处',()=>find(1)]]);function find(dir){const q=d.querySelector('input').value;if(!q)return;lastSearch=q;const cm=state.cm,cursor=cm.getSearchCursor(q,dir<0?cm.getCursor('from'):cm.getCursor('to'),{caseFold:true});let found=dir<0?cursor.findPrevious():cursor.findNext();let c=cursor;if(!found){c=cm.getSearchCursor(q,dir<0?{line:cm.lastLine(),ch:cm.getLine(cm.lastLine()).length}:{line:0,ch:0},{caseFold:true});found=dir<0?c.findPrevious():c.findNext();}if(found){cm.setSelection(c.from(),c.to());cm.scrollIntoView({from:c.from(),to:c.to()},70);$('#searchResult').textContent='第 '+(c.from().line+1);}else $('#searchResult').textContent='未找到匹配';}d.querySelector('input').onkeydown=e=>{if(e.key==='Enter')find(1);};d.querySelector('input').focus();}
async function jumpLine(){const n=await ask('跳转到行',String(state.cm.getCursor().line+1),`1–${state.cm.lineCount()}`);if(n===null)return;const line=Number(n);if(!Number.isInteger(line)||line<1||line>state.cm.lineCount())return toast('请输入有效的行号');state.cm.setCursor(line-1,0);state.cm.scrollIntoView({line:line-1,ch:0},100);state.cm.focus();}
async function exportText(text,name){const dir=await ask('导出到文件夹',state.info.permission?state.info.home+'/下载':state.info.workspace);if(!dir)return;const filename=await ask('文件名',name);if(filename){const path=await rpc('exportText',{path:dir,name:filename,text});alertBox('已导出',path);}}
let settingsNeedsRefresh=false;
function settingSwitch(key,title,subtitle,checked){return `<label class="settingsRow"><span class="settingsText"><span class="settingsTitle">${title}</span><span class="settingsSub">${subtitle}</span></span><input class="settingsSwitch" type="checkbox" data-setting="${key}" ${checked?'checked':''}></label>`;}
function closeSettings(){const layer=$('#settingsLayer');if(!layer)return;layer.remove();const needs=settingsNeedsRefresh;settingsNeedsRefresh=false;if(needs&&state.screen==='files')act(refresh);}
function applyEditorSettings(){if(state.screen==='editor'&&state.cm){state.cm.setOption('lineWrapping',pref.wrap);state.cm.setOption('lineNumbers',pref.lines);state.cm.refresh();}}
function applyFontSize(size){const value=Math.max(4,Math.min(32,Number(size)||13));const key=state.editor?fontKey():'text';if(!pref.fonts)pref.fonts={};pref.fonts[key]=value;pref.font=value;savePref();const label=$('#settingsFontValue');if(label)label.textContent=value;if(state.screen==='editor'&&state.cm){state.cm.getWrapperElement().style.fontSize=editorFont()+'px';state.cm.refresh();}}
function editFontSize(){const current=state.screen==='editor'?editorFont():(Number(pref.fonts&&pref.fonts.text)||pref.font);const d=dialog('编辑器字体大小',`<label>字体大小：<span id="fontSizeLabel">${current}</span> px</label><input id="fontSize" type="range" min="4" max="32" step="0.5" value="${current}"><p class="bundleHint">双指缩放可在编辑器内快速调整；确定后会应用到当前编辑器。</p>`,[['取消',closeModal],['确定',()=>{applyFontSize($('#fontSize').value);closeModal();}]]);const input=d.querySelector('#fontSize');input.oninput=()=>{d.querySelector('#fontSizeLabel').textContent=input.value;};}
function chooseTheme(){options('主题颜色',[{label:'浅色'+(pref.light?'（当前）':''),run:()=>{pref.light=true;savePref();const v=$('#settingsThemeValue');if(v)v.textContent='浅色';}},{label:'深色'+(pref.light?'':'（当前）'),run:()=>{pref.light=false;savePref();const v=$('#settingsThemeValue');if(v)v.textContent='深色';}}]);}
const SETTINGS_CHOICES={
 startLeft:{title:'启动路径 - 左窗口',values:['首页','工作区','根目录','下载']},
 startRight:{title:'启动路径 - 右窗口',values:['首页','工作区','根目录','下载']},
 autoService:{title:'自动启动服务',values:['未启用','远程管理','MCP 服务']},
 listSize:{title:'文件列表大小',values:['小','中','大']},
 nameLines:{title:'文件名最大行数',values:['1 行','2 行','3 行']},
 timeStyle:{title:'文件列表时间偏好',values:['完整时间','隐藏秒数，精简年份','相对时间']},
 dateFormat:{title:'时间日期格式',values:['yyyy-MM-dd HH:mm:ss','yyyy-MM-dd HH:mm','MM-dd HH:mm','HH:mm:ss']},
 sortBy:{title:'文件排序方式',values:['名称','大小','时间','类型']},
 autoClean:{title:'自动清理回收站文件',values:['禁用','7 天','30 天','90 天']},
 bottomMargin:{title:'底部工具栏下边距大小',values:['0 dp','8 dp','12 dp','16 dp','24 dp']}
};
function settingStatic(title,subtitle){return `<div class="settingsRow settingsStatic settingsDisabled"><span class="settingsText"><span class="settingsTitle">${title}</span><span class="settingsSub">${subtitle}</span></span></div>`;}
function settingLink(title,subtitle,key){return `<button class="settingsRow" data-link="${key}"><span class="settingsText"><span class="settingsTitle">${title}</span><span class="settingsSub">${subtitle}</span></span></button>`;}
function settingChoice(key,title,desc,current){const sub=desc?desc+' · '+current:current;return `<button class="settingsRow" data-choice="${key}"><span class="settingsText"><span class="settingsTitle">${title}</span><span class="settingsSub" id="choice-${key}">${esc(sub)}</span></span></button>`;}
function chooseSetting(key){
 const cfg=SETTINGS_CHOICES[key];if(!cfg)return;
 const cur=pref[key]||cfg.values[0];
 options(cfg.title,cfg.values.map(v=>({label:v+(v===cur?'（当前）':''),run:()=>{
  pref[key]=v;savePref();applyDisplayPrefs();
  const el=$('#choice-'+key);if(el)el.textContent=v;
  if(['listSize','nameLines','timeStyle','dateFormat','sortBy'].includes(key))settingsNeedsRefresh=true;
  if(key==='autoClean'&&v!=='禁用')maybeCleanRecycle();
 }})));
}
function preferences(){
 if($('#settingsLayer'))return;
 const currentFont=state.screen==='editor'?editorFont():(Number(pref.fonts&&pref.fonts.text)||pref.font);
 const layer=document.createElement('div');
 layer.id='settingsLayer';layer.className='settingsLayer';
 layer.innerHTML=`<header class="settingsBar"><button class="settingsBack" aria-label="返回">${icon('back')}</button><h1>设置</h1></header><div class="settingsScroll">
 <section class="settingsSection"><h2>启动</h2>${settingSwitch('startRoot','请求 Root 权限','启动时请求 Root 权限，修改后需重启生效',pref.startRoot)}${settingSwitch('startShell','请求 Shell 权限','未获取 Root 时尝试通过 Shizuku 获取 Shell 权限，修改后需重启生效',pref.startShell)}${settingLink('safData','授权 Android/data','通过系统文件夹选择器授权，无需 Root/Shizuku','safData')}${settingLink('safObb','授权 Android/obb','通过系统文件夹选择器授权，无需 Root/Shizuku','safObb')}${settingStatic('自定义 su 命令','当前版本使用系统 su，暂不支持自定义命令')}${settingLink('安装 Shizuku','获取 Shizuku（shizuku.rikka.app）','shizuku')}${settingChoice('startLeft','启动路径 - 左窗口','应用启动时左窗口打开的目录',pref.startLeft)}${settingChoice('startRight','启动路径 - 右窗口','应用启动时右窗口打开的目录',pref.startRight)}${settingChoice('autoService','自动启动服务','应用启动后在后台自动运行的服务',pref.autoService)}</section>
 <section class="settingsSection"><h2>外观</h2>${settingStatic('桌面图标','当前版本使用固定应用图标')}<button class="settingsRow" id="settingsTheme"><span class="settingsText"><span class="settingsTitle">主题颜色</span><span class="settingsSub" id="settingsThemeValue">${pref.light?'浅色':'深色'}</span></span></button>${settingChoice('listSize','文件列表大小','文件行的显示密度',pref.listSize)}${settingChoice('nameLines','文件名最大行数','文件名最多显示的行数',pref.nameLines)}${settingChoice('timeStyle','文件列表时间偏好','文件行中时间的显示方式',pref.timeStyle)}${settingChoice('dateFormat','时间日期格式','时间和日期使用的格式',pref.dateFormat)}${settingStatic('文件列表不显示权限','文件列表接口暂不提供权限字段')}${settingSwitch('showExtensions','显示文件扩展名','在文件列表中显示 .apk、.txt 等后缀',pref.showExtensions)}</section>
 <section class="settingsSection"><h2>编辑器</h2><button class="settingsRow" id="settingsFont"><span class="settingsText"><span class="settingsTitle">编辑器字体大小</span><span class="settingsSub"><span id="settingsFontValue">${currentFont}</span> px</span></span></button>${settingSwitch('wrap','自动换行','长行在编辑器宽度内换行',pref.wrap)}${settingSwitch('lines','行号','在代码左侧显示行号',pref.lines)}${settingSwitch('pinchZoom','编辑器内捏合缩放','双指缩放调整代码字号',pref.pinchZoom!==false)}</section>
 <section class="settingsSection"><h2>常规</h2>${settingSwitch('backupOnSave','生成备份文件','保存文件时自动将原文件重命名为 .bak 备份文件',pref.backupOnSave!==false)}${settingStatic('保留文件时间','复制与解压接口暂不提供时间保留选项')}${settingStatic('启用输入框收藏功能','当前输入框未提供历史收藏')}${settingStatic('文件菜单排序','文件操作菜单顺序当前固定')}${settingStatic('内置打开方式排序','打开方式顺序当前固定')}${settingStatic('自定义 MT2 目录','工作区目录由应用固定管理')}${settingSwitch('hidden','显示隐藏文件','在文件列表中显示以点开头的文件',pref.hidden)}${settingSwitch('foldersFirst','文件夹置顶','排序时文件夹始终排在文件前面',pref.foldersFirst)}${settingChoice('sortBy','文件排序方式','文件列表使用的排序依据',pref.sortBy)}${settingSwitch('sortAsc','升序排列','关闭后按降序排列',pref.sortAsc)}</section>
 <section class="settingsSection"><h2>回收站</h2>${settingStatic('启用回收站功能','当前版本始终启用回收站')}${settingStatic('默认移动到回收站','删除操作固定移入回收站')}${settingChoice('autoClean','自动清理回收站文件','启动时清理超过指定天数的回收站文件',pref.autoClean)}${settingSwitch('deleteWarning','显示删除警告','删除文件前显示确认提示，避免误删',pref.deleteWarning)}</section>
 <section class="settingsSection"><h2>安装</h2>${settingStatic('APK 安装验证','当前版本依赖系统安装器校验签名')}${settingStatic('APK 安装防自动删除','当前版本不修改系统安装器行为')}${settingSwitch('confirmInstall','安装 APK 前二次确认','调用系统安装器前再次确认',pref.confirmInstall)}${settingStatic('使用 Shizuku 安装 apk/apks/xapk','当前版本未接入 Shizuku 安装通道')}${settingStatic('使用 Dhizuku 安装 apk/apks/xapk','当前版本未接入 Dhizuku')}${settingStatic('使用 Root 安装 apk/apks/xapk','当前版本未接入 Root 静默安装')}${settingStatic('自定义安装来源','当前版本使用系统默认安装来源')}${settingStatic('自定义系统安装器应用','软件包安装程序（com.android.packageinstaller）')}${settingStatic('安装优先级说明','系统安装器 > 其他方式（当前仅实现系统安装器）')}</section>
 <section class="settingsSection"><h2>书签 &amp; 底栏</h2>${settingSwitch('bookmarksInDrawer','在侧拉栏显示书签','在侧拉栏新增书签分组，最多显示 8 项',pref.bookmarksInDrawer)}${settingSwitch('bookmarkTop','新增书签添加到顶部','新增书签将添加到书签列表顶部',pref.bookmarkTop)}${settingStatic('书签上滑手势区分左右','当前版本使用点击跳转')}${settingStatic('底部工具栏增加下边距','从底部工具栏上滑可拉出书签，可调整下方边距')}${settingChoice('bottomMargin','底部工具栏下边距大小','底部工具栏额外的下边距',pref.bottomMargin)}</section>
 <section class="settingsSection"><h2>外置存储</h2>${settingStatic('加载外置存储的缩略图','当前文件列表不加载外置存储缩略图')}${settingStatic('优化外置存储数据传输','当前版本使用系统文件接口，暂无可调参数')}</section>
 <section class="settingsSection"><h2>网络存储</h2>${settingStatic('加载缩略图','当前远程列表不加载图片与视频缩略图')}${settingStatic('仅在 WiFi 网络下加载','当前版本没有远程缩略图加载')}${settingStatic('图片大小限制','当前版本没有远程缩略图加载')}${settingStatic('加载时间限制','当前版本没有远程缩略图加载')}${settingLink('管理远程存储','添加、编辑或删除远程连接','network')}</section>
 <section class="settingsSection"><h2>云备份</h2>${settingStatic('备份到云端','未配置云服务；设置数据仅保存在本机')}${settingStatic('从云端恢复','未配置云服务；可在系统设置中清除应用数据后重新配置')}</section>
 <section class="settingsSection"><h2>其它</h2>${settingStatic('语言','当前仅提供简体中文（自动检测）')}${settingLink('检测更新','在 GitHub 查看最新版本','update')}${settingLink('特别鸣谢','为开发提供帮助的开源项目与贡献者','ack')}${settingStatic('备案编号','未备案 · 本地开源应用')}${settingLink('用户协议与隐私权政策','已阅读并同意','privacy')}</section>
 <section class="settingsSection"><h2>关于</h2><div class="settingsRow settingsStatic"><span class="settingsText"><span class="settingsTitle">版本</span><span class="settingsSub">0.1.13</span></span></div>${settingLink('用户指南','查看使用说明与常见问题','guide')}${settingLink('关于赛博中枢管家','用户信息、版本说明与开源致谢','about')}</section>
 </div>`;
 document.body.append(layer);
 layer.querySelector('.settingsBack').onclick=closeSettings;
 layer.querySelector('#settingsTheme').onclick=chooseTheme;
 layer.querySelector('#settingsFont').onclick=editFontSize;
 const links={safData:()=>rpc('safPermission',{which:'data'}).then(()=>toast('请在系统选择器中选择 Android/data 文件夹')),safObb:()=>rpc('safPermission',{which:'obb'}).then(()=>toast('请在系统选择器中选择 Android/obb 文件夹')),shizuku:()=>openExternal('https://shizuku.rikka.app/'),update:()=>openExternal('https://github.com/wumingzhinu/Cyber-Hub-Manager/releases'),network:()=>{closeSettings();addNetworkStorage();},ack:()=>alertBox('特别鸣谢','感谢 JADX、Termux、Shizuku、CodeMirror、Public Sans、JSch、jCIFS-ng、OkHttp、Bouncy Castle、ARSCLib、ECJ、ASM 等开源项目与贡献者。'),privacy:()=>alertBox('用户协议与隐私权政策','本应用的文件、APK、终端与远程存储操作均在本机或您指定的服务器上完成，不收集、不上传您的文件内容。\n\n使用本应用即表示您同意自行承担文件修改、删除、安装与提权操作的风险。请勿使用本应用处理您无权访问的数据。'),guide:()=>{closeSettings();guide();},about:()=>{closeSettings();about();}};
 layer.querySelectorAll('[data-link]').forEach(b=>{b.onclick=()=>links[b.dataset.link]&&links[b.dataset.link]();});
 layer.querySelectorAll('[data-choice]').forEach(b=>{b.onclick=()=>chooseSetting(b.dataset.choice);});
 layer.querySelectorAll('[data-setting]').forEach(input=>{input.onchange=()=>{const key=input.dataset.setting;pref[key]=input.checked;savePref();if(['hidden','showExtensions','foldersFirst','sortAsc'].includes(key))settingsNeedsRefresh=true;if(key==='wrap'||key==='lines')applyEditorSettings();};});
}
async function openDrawer(drag=false){closeMenu();const layer=$('#drawerLayer');if(layer.children.length){if(!drag)closeDrawer();return;}clearLayer(layer,false);layer.innerHTML=`<div class="drawerShade"></div><aside class="drawer" aria-label="导航" role="navigation"><div class="drawerHead"><div class="drawerTop"><div class="brandMark" aria-hidden="true">${brandLogo()}</div><div class="titleblock"><div class="title">赛博中枢管家</div><div class="subtitle">${esc(storage.get('cyber-name')||'本地工作区')}</div></div><button class="iconbtn" id="themeToggle" aria-label="切换主题">${icon('sun')}</button><button class="iconbtn" id="drawerMore" aria-label="导航菜单">${icon('more')}</button></div></div><div id="drawerDocument"></div><button class="sectionLabel" aria-expanded="true" data-group="storageRows">本地 ${icon('up')}</button><div class="drawerGroup" id="storageRows"></div><button class="sectionLabel" aria-expanded="true" data-group="networkRows">远程存储 ${icon('up')}</button><div class="drawerGroup" id="networkRows"></div>${pref.bookmarksInDrawer?'<button class="sectionLabel" aria-expanded="true" data-group="bookmarkRows">书签 '+icon('up')+'</button><div class="drawerGroup" id="bookmarkRows"></div>':''}<button class="sectionLabel" aria-expanded="true" data-group="backgroundRows">后台 ${icon('up')}</button><div class="drawerGroup" id="backgroundRows"></div><button class="sectionLabel" aria-expanded="true" data-group="toolRows">工具 ${icon('up')}</button><div class="drawerGroup" id="toolRows"></div></aside>`;if(drag)layer.querySelector('.drawer').classList.add('dragging');else{const surface=layer.querySelector('.drawer');surface.classList.add('entering');setTimeout(()=>surface.classList.remove('entering'),240);}layer.querySelector('.drawerShade').onclick=closeDrawer;$('#drawerMore').onclick=()=>state.screen==='editor'?editorMenu():drawerMenu();$('#themeToggle').onclick=()=>{pref.light=!pref.light;savePref();};layer.querySelector('.brandMark').onclick=about;layer.querySelectorAll('[data-group]').forEach(b=>b.onclick=()=>{const open=b.getAttribute('aria-expanded')!=='true';b.setAttribute('aria-expanded',String(open));expandGroup($('#'+b.dataset.group),open);});
 function row(parent,ic,label,fn,detail=''){const b=document.createElement('button');b.className='drawerRow';b.innerHTML=`<span class="circleicon">${icon(ic)}</span><span class="storage">${esc(label)}${detail}</span>`;b.onclick=()=>act(async()=>{closeDrawer();if(state.screen==='editor'&&!await canLeave())return;if(state.screen==='editor'&&state.disposeEditor)state.disposeEditor();await fn();});layer.querySelector(parent).append(b);return b;}
 const drawer=layer.querySelector('.drawer');
 if(state.screen==='editor'){
  const doc=document.createElement('button');doc.className='drawerRow currentDocument';doc.innerHTML=icon('edit')+'<span class="storage">'+esc((state.editor.dirty?'*':'')+state.editor.name)+'<small>'+esc(state.editor.mode==='java'?'Java · 可编辑':state.editor.kind==='dex'?'Smali · 可编辑':'文本编辑器')+'</small></span>';
  doc.onclick=closeDrawer;layer.querySelector('#drawerDocument').append(doc);
 }
 const rootRow=row('#storageRows','phone','根目录',()=>navigate(state.active,{path:'/',archive:'',entry:''}),'<div class="small">正在读取存储…</div>');
 const storageRow=row('#storageRows','sd','存储目录',()=>navigate(state.active,{path:state.info.home,archive:'',entry:''}),'<div class="small">正在读取存储…</div>');
 row('#storageRows','folder','应用工作区',()=>navigate(state.active,{path:state.info.workspace,archive:'',entry:''}));row('#networkRows','plus','添加远程存储',addNetworkStorage);
 for(const c of (Array.isArray(pref.networkStorages)?pref.networkStorages:[])){const b=document.createElement('button');b.className='drawerRow';b.innerHTML=`<span class="circleicon">${icon(networkIcon(c))}</span><span class="storage">${esc(c.name||networkProtocolLabel(c))}<div class="small">${esc(networkProtocolLabel(c)+' · '+networkTarget(c))}</div></span>`;hold(b,()=>networkStorageActions(c),()=>openRemoteStorage(c));layer.querySelector('#networkRows').append(b);}
 if(pref.bookmarksInDrawer){const bms=(pref.bookmarks||[]).slice(0,8);if(!bms.length){const b=document.createElement('div');b.className='drawerRow';b.innerHTML='<span class="circleicon">'+icon('copy')+'</span><span class="storage">暂无书签</span>';layer.querySelector('#bookmarkRows').append(b);}for(const f of bms)row('#bookmarkRows',f.dir?'folder':fileType(f),f.name,()=>openFile(f));row('#bookmarkRows','copy','查看全部书签',()=>bookmarks());}
 row('#backgroundRows','history',`${pending.size} 个当前操作`,backgroundPage);
 for(const [ic,label,fn]of [['trash','回收站',recyclePage],['plugin','插件管理器',pluginPage],['remote','远程管理',()=>servicePage('remote')],['layers','已安装应用',appsPage],['code','Smali 指令查询',smaliReferencePage],['text','翻译服务',translatePage],['edit','文本编辑器',newText],['terminal','Termux 终端',terminalPage],['search','屏幕取色',colorPickerTool],['switch','Root / Shizuku 授权',elevatedTool],['network','MCP 服务',()=>servicePage('mcp')],['history','活动记录',logPage],['more','更多工具',moreTools]])row('#toolRows',ic,label,fn);
 const fillDisk=async(target,label,path)=>{try{const disk=await rpc('disk',{path});if(!drawer.isConnected)return;target.querySelector('.storage').innerHTML=esc(label)+'<div class="meter"><span style="width:'+ (disk.total?(1-disk.free/disk.total)*100:0)+'%"></span></div><div class="small">'+size(disk.total-disk.free)+' 已用，'+size(disk.free)+' 可用</div>';}catch(error){if(drawer.isConnected){const small=target.querySelector('.small');if(small)small.textContent='无法获取存储信息';}}};
 await Promise.all([fillDisk(rootRow,'根目录','/'),fillDisk(storageRow,'存储目录',state.info.home)]);
}
function drawerMenu(){menu([{icon:'plus',label:'添加远程存储',run:addNetworkStorage},{icon:'copy',label:'书签',run:()=>{closeDrawer();bookmarks();}},{icon:'refresh',label:'刷新',run:async()=>{closeDrawer();if(state.screen==='files')await refresh();}},{icon:'settings',label:'设置',run:()=>{closeDrawer();preferences();}},{icon:'info',label:'关于',run:()=>{closeDrawer();about();}}]);}

function toolPage(title,subtitle=''){state.previous=state.screen==='dex'?'dex':'files';state.screen='tool';header(title,subtitle,[{icon:'close',label:'关闭工具页',run:window.goBack}]);$('#subbar').innerHTML='';$('#bottom').innerHTML='';$('#content').classList.remove('javaEditor');$('#content').innerHTML='<div class="toolpage" id="toolbody"></div>';return $('#toolbody');}
async function appsPage(){const body=toolPage('已安装应用','轻点应用以查看其 APK');body.innerHTML='<div class="searchline"><input placeholder="搜索已安装应用" aria-label="搜索已安装应用"></div><div id="appsRows"></div>';const apps=await rpc('apps');function render(q){$('#appsRows').innerHTML=apps.filter(x=>(x.name+' '+x.package).toLowerCase().includes(q.toLowerCase())).map((x)=>`<button class="listrow" data-app-path="${esc(x.path)}">${icon('apk')}<span class="main">${esc(x.name)}<small>${esc(x.package)}</small></span></button>`).join('');document.querySelectorAll('[data-app-path]').forEach(b=>b.onclick=()=>act(()=>showApk({path:b.dataset.appPath},true)));}body.querySelector('input').oninput=e=>render(e.target.value);render('');}
function recycleBucket(t){const now=new Date(),d=new Date(t),today=new Date(now.getFullYear(),now.getMonth(),now.getDate()).getTime(),day=new Date(d.getFullYear(),d.getMonth(),d.getDate()).getTime(),days=Math.round((today-day)/86400000);return days<=0?'今天':days===1?'昨天':days<7?'7 天内':days<30?'30 天内':'更早';}
function recycleGroups(rows){const groups=[];for(const r of rows.slice().sort((a,b)=>(b.time||0)-(a.time||0))){const label=recycleBucket(r.time||0),last=groups[groups.length-1];if(last&&last.label===label)last.items.push(r);else groups.push({label,items:[r]});}return groups;}
function recycleRow(r,index){const pane=state.panes[index],selected=pane.selected&&pane.selected.has(fileKey(r));const b=document.createElement('button');b.className='file recycleRow'+(selected?' selected':'');b.type='button';b.setAttribute('aria-selected',String(!!selected));b.dataset.index=String(pane.items.indexOf(r));const type=r.dir?'folder':fileType({name:r.name,dir:false});b.innerHTML='<span class="fileicon '+type+'">'+icon(type)+'</span><span class="fileinfo"><span class="fileline"><span class="filename">'+esc(r.name)+'</span></span><span class="filemeta">'+date(r.time||0)+'  '+(r.dir?'文件夹':size(Number(r.size)||0))+'</span></span>';recycleGesture(b,r,index);return b;}
function recycleActions(r){const p=activePane(),all=p.selected&&p.selected.size&&p.selected.has(fileKey(r))?p.items.filter(x=>p.selected.has(fileKey(x))):[r];const many=all.length>1;options(many?'已选 '+all.length+' 项':r.name,[{icon:'undo',label:many?'还原 '+all.length+' 项':'还原',run:()=>restoreRecycle(all)},{icon:'trash',label:many?'永久删除 '+all.length+' 项':'永久删除',run:()=>purgeRecycle(all)}]);}
async function restoreRecycle(items){for(const r of items)await rpc('restore',{id:r.id});await reloadRecycle();toast('已还原 '+items.length+' 项');}
async function purgeRecycle(items){if(!await confirmBox('永久删除？',items.length+' 个项目将被永久删除，无法还原。','删除'))return;for(const r of items)await rpc('purge',{id:r.id});await reloadRecycle();toast('已删除 '+items.length+' 项');}
function recycleProperties(items){const text=items.map(r=>'<p class="break">'+esc(r.path||r.name)+'</p><p>大小：'+(r.dir?'文件夹':size(Number(r.size)||0))+'<br>删除时间：'+date(r.time||0)+'</p>').join('<hr>');dialog('属性',text,[['复制路径',()=>rpc('clipboard',{text:items.map(x=>x.path||x.name).join('\n')}).then(()=>toast('已复制'))],['关闭',closeModal]]);}
async function reloadRecycle(){await Promise.all(state.panes.map((p,i)=>p.mode==='recycle'?loadPane(i):null));renderFiles();}
function recycleMenu(){const sel=selectedFiles(),rows=[];if(sel.length){rows.push({icon:'undo',label:'还原 '+sel.length+' 项',run:()=>restoreRecycle(sel)},{icon:'trash',label:'永久删除 '+sel.length+' 项',run:()=>purgeRecycle(sel)},{icon:'close',label:'清除选择',run:clearSelection});}else{rows.push({icon:'refresh',label:'刷新',run:async()=>{await loadPane(state.active);renderFiles();}},{icon:'trash',label:'清空回收站',run:emptyRecycle});}rows.push({icon:'back',label:'离开回收站',run:async()=>{if(activePane().history.length)return backPane();await navigate(state.active,{path:state.info.home,mode:''});}});options('回收站',rows);}
async function emptyRecycle(){if(!await confirmBox('清空回收站？','所有项目将被永久删除，无法还原。','清空'))return;const rows=await rpc('recycleList');for(const r of rows)await rpc('purge',{id:r.id});await reloadRecycle();}
async function recyclePage(){const i=state.active,p=state.panes[i];if(p.mode!=='recycle'){p.history.push(paneLocation(p));p.future=[];}Object.assign(p,{mode:'recycle',path:'回收站',archive:'',entry:'',scrollTop:0,selected:new Set(),appendMode:false,gestureAnchor:null});await loadPane(i);state.screen='files';renderFiles();animateContent($('.pane.active'));}
async function logPage(){const body=toolPage('活动记录');const rows=await rpc('log');body.innerHTML=rows.length?rows.map(x=>`<div class="listrow">${icon('history')}<div class="main">${esc(x.text)}<small>${date(x.time)}</small></div></div>`).join(''):'<div class="empty">文件更改和已完成的任务将显示在此处。</div>';}
async function newText(){const path=await ask('文件夹',state.info.workspace);if(!path)return;const name=await ask('新建文本文件','未命名.txt');if(!name)return;const file=await rpc('create',{path,name,folder:false});await openText({path:file});}
function terminalPage(){const path=state.info.permission?state.info.home:state.info.workspace;return rpc('terminalOpen',{path});}
async function pluginPage(){const body=toolPage('插件管理器','本地 shell 脚本插件');body.innerHTML='<p>导入 .sh 脚本。仅在你选择运行时才执行，使用本应用权限。</p><button class="primary" id="importPlugin">导入脚本</button><div id="pluginRows"></div>';$('#importPlugin').onclick=()=>act(async()=>{const path=await ask('脚本路径',state.info.home+'/下载/plugin.sh');if(path){await rpc('plugins',{op:'import',path});await pluginPage();}});const rows=await rpc('plugins');for(const name of rows){const b=document.createElement('button');b.className='listrow';b.innerHTML=icon('plugin')+`<span class="main">${esc(name)}</span>`;b.onclick=()=>options(name,[{icon:'play',label:'运行',run:async()=>{if(await confirmBox('运行脚本？',name,'运行')){const text=await rpc('plugins',{op:'run',name});alertBox('脚本输出',text);}}},{icon:'trash',label:'移除',run:async()=>{if(await confirmBox('移除插件？',name,'移除')){await rpc('plugins',{op:'remove',name});await pluginPage();}}}]);$('#pluginRows').append(b);}if(!rows.length)$('#pluginRows').innerHTML='<div class="empty">未导入脚本。</div>';}
async function servicePage(kind){if(kind==='remote')return remoteServicePage();const body=toolPage('MCP 服务','本地 HTTP · 只读方法');const status=await rpc('service',{kind:'mcp'});const current=status.servers.find(x=>x.kind==='mcp');body.innerHTML=`<div class="banner">在 localhost:8766/mcp 提供 list_files 与 read_text。请使用显示的 Bearer 令牌 连接。 应用进程关闭时服务停止。</div><p>共享文件夹</p><input class="toolinput" id="sharePath" value="${esc(current?current.root:activePane().archive?state.info.workspace:activePane().path)}" ${current?'disabled':''}>${current?`<p><span class="pill">运行中</span></p><p class="break">${current.urls.map(esc).join('<br>')||'没有可用的网络地址'}</p><p class="break">Bearer 令牌：${esc(current.token)}</p><button class="primary" id="copyService">复制连接</button>`:''}<button class="primary" id="toggleService">${current?'停止':'启动'}</button>`;$('#toggleService').onclick=()=>act(async()=>{await rpc('service',{kind:'mcp',op:current?'stop':'start',path:$('#sharePath').value});await servicePage('mcp');});if($('#copyService'))$('#copyService').onclick=()=>act(async()=>{await rpc('clipboard',{text:current.urls.join('\n')+'\nAuthorization: Bearer '+current.token});toast('连接已复制');});}
function remotePrefProtocol(){return pref.remoteProtocol==='ftp'?'ftp':'http';}
function remotePrefPort(){const p=Number(pref.remotePort);return Number.isInteger(p)&&p>=1024&&p<=65535?p:(remotePrefProtocol()==='ftp'?2121:8765);}
function remotePrefCode(){let code=String(pref.remoteCode||'');if(!/^[A-Za-z0-9_-]{4,32}$/.test(code)){code=String(Math.floor(100000+Math.random()*900000));pref.remoteCode=code;savePref();}return code;}
function remoteRunningHtml(s){const urls=s.urls&&s.urls.length?s.urls.map(esc).join('<br>'):'没有可用的网络地址';return `<p><span class="pill">运行中 · ${esc(String(s.protocol||'http').toUpperCase())} · 端口 ${s.port}</span></p><p class="break">${urls}</p><p>${s.protocol==='ftp'?'FTP 用户名任意，密码为配对码。':'打开链接即可浏览和下载，链接中的 code 即配对码。'}</p><p class="break">配对码：<strong>${esc(s.code)}</strong></p><button class="primary" id="copyService">复制连接</button><h3>设备管理</h3><div id="remoteDevices"></div>`;}
function renderRemoteDevices(s){const box=$('#remoteDevices');if(!box)return;const devices=s&&s.devices?s.devices:[];const blocked=s&&s.blocked?s.blocked:[];if(!devices.length&&!blocked.length){box.innerHTML='<div class="empty">暂无连接设备。</div>';return;}box.innerHTML=devices.map(d=>`<div class="listrow"><span class="main">${esc(d.ip)}<small>${esc(d.protocol)} · ${esc(d.agent||'未知客户端')} · ${esc(date(d.since))}</small></span><button class="mini" data-kick="${esc(d.ip)}">断开</button></div>`).join('')+blocked.map(ip=>`<div class="listrow"><span class="main">${esc(ip)}<small>已断开，禁止再次连接</small></span><button class="mini" data-unblock="${esc(ip)}">恢复</button></div>`).join('');box.querySelectorAll('[data-kick]').forEach(b=>b.onclick=()=>act(async()=>{await rpc('service',{kind:'remote',op:'kick',ip:b.dataset.kick});await remoteServicePage();}));box.querySelectorAll('[data-unblock]').forEach(b=>b.onclick=()=>act(async()=>{await rpc('service',{kind:'remote',op:'unblock',ip:b.dataset.unblock});await remoteServicePage();}));}
function startRemotePoll(body){const timer=setInterval(async()=>{if(!document.body.contains(body)||!document.getElementById('remoteDevices')){clearInterval(timer);return;}try{const status=await rpc('service',{kind:'remote'});const current=status.servers.find(x=>x.kind==='remote');if(current)renderRemoteDevices(current);else clearInterval(timer);}catch(e){clearInterval(timer);}},3000);}
async function remoteServicePage(){const body=toolPage('远程管理','HTTP / FTP · 自定义端口与配对码');const status=await rpc('service',{kind:'remote'});const current=status.servers.find(x=>x.kind==='remote');const protocol=current?current.protocol:remotePrefProtocol();const port=current?current.port:remotePrefPort();const code=current?current.code:remotePrefCode();const share=current?current.root:(activePane().archive?state.info.workspace:activePane().path);body.innerHTML=`<div class="banner">在同一网络的另一设备连接，只读浏览和下载共享文件夹。HTTP 使用链接中的配对码；FTP 用户名任意，密码为配对码。HTTP/FTP 不加密，仅限可信局域网；应用进程关闭时服务停止。</div><p>共享文件夹</p><input class="toolinput" id="sharePath" value="${esc(share)}" ${current?'disabled':''}><p>协议</p><select class="toolinput" id="remoteProtocol" ${current?'disabled':''}><option value="http"${protocol==='http'?' selected':''}>HTTP</option><option value="ftp"${protocol==='ftp'?' selected':''}>FTP</option></select><p>端口</p><input class="toolinput" id="remotePort" type="number" inputmode="numeric" min="1024" max="65535" value="${port}" ${current?'disabled':''}><p>配对码</p><input class="toolinput" id="remoteCode" value="${esc(code)}" maxlength="32" spellcheck="false" autocapitalize="off" ${current?'disabled':''}>${current?'':`<p>端口 1024-65535；配对码 4-32 位字母、数字、- 或 _。FTP 为只读共享。</p>`}${current?remoteRunningHtml(current):''}<button class="primary" id="toggleService">${current?'停止':'启动'}</button>`;const protocolSelect=$('#remoteProtocol');if(protocolSelect)protocolSelect.onchange=()=>{const p=$('#remotePort');if(p&&(p.value==='8765'||p.value==='2121'))p.value=protocolSelect.value==='ftp'?'2121':'8765';};$('#toggleService').onclick=()=>act(async()=>{if(current){await rpc('service',{kind:'remote',op:'stop'});await remoteServicePage();return;}const protocol=$('#remoteProtocol').value,port=Number($('#remotePort').value),code=$('#remoteCode').value.trim();if(!Number.isInteger(port)||port<1024||port>65535)throw new Error('端口需在 1024-65535 之间');if(!/^[A-Za-z0-9_-]{4,32}$/.test(code))throw new Error('配对码需为 4-32 位字母、数字、- 或 _');pref.remoteProtocol=protocol;pref.remotePort=port;pref.remoteCode=code;savePref();await rpc('service',{kind:'remote',op:'start',path:$('#sharePath').value,protocol,port,code});await remoteServicePage();});if(current){$('#copyService').onclick=()=>act(async()=>{const text=current.protocol==='ftp'?current.urls.join('\n')+'\n用户名：任意\n密码/配对码：'+current.code:current.urls.join('\n');await rpc('clipboard',{text});toast('连接已复制');});renderRemoteDevices(current);startRemotePoll(body);}}
function backgroundPage(){const body=toolPage('后台');body.innerHTML=`<p>${pending.size} 个当前原生操作。</p><p>文件与反编译任务在 UI 线程之外运行。远程/MCP 服务需要应用进程保持存活。</p><button class="primary" id="services">查看服务</button>`;$('#services').onclick=()=>options('服务',[{label:'远程管理',icon:'remote',run:()=>servicePage('remote')},{label:'MCP 服务',icon:'network',run:()=>servicePage('mcp')}]);}
const NETWORK_PROTOCOLS={
 webdav:{label:'WebDAV',icon:'network',hint:'填写完整 WebDAV 地址，例如 https://example.com/remote.php/dav/files/用户名。使用 Basic 认证。',fields:[
  {key:'url',label:'服务器地址',placeholder:'https://example.com/dav',required:true},
  {key:'user',label:'用户名'},{key:'password',label:'密码',type:'password'}]},
 ftp:{label:'FTP',icon:'remote',hint:'明文 FTP，建议只在可信局域网使用。',fields:[
  {key:'host',label:'服务器',placeholder:'192.168.1.10',required:true},
  {key:'port',label:'端口',type:'number',value:'21'},
  {key:'user',label:'用户名'},{key:'password',label:'密码',type:'password'},
  {key:'root',label:'根路径',placeholder:'/'},
  {key:'passive',label:'被动模式',type:'checkbox',value:true}]},
 ftps:{label:'FTPS',icon:'remote',hint:'显式 FTPS 通常使用 21 端口；隐式 FTPS 通常使用 990 端口。',fields:[
  {key:'host',label:'服务器',placeholder:'192.168.1.10',required:true},
  {key:'port',label:'端口',type:'number',value:'21'},
  {key:'user',label:'用户名'},{key:'password',label:'密码',type:'password'},
  {key:'root',label:'根路径',placeholder:'/'},
  {key:'implicit',label:'隐式 TLS',type:'checkbox',value:false},
  {key:'passive',label:'被动模式',type:'checkbox',value:true}]},
 sftp:{label:'SFTP',icon:'terminal',hint:'支持密码或私钥认证；当前不校验主机指纹。',fields:[
  {key:'host',label:'服务器',placeholder:'192.168.1.10',required:true},
  {key:'port',label:'端口',type:'number',value:'22'},
  {key:'user',label:'用户名'},{key:'password',label:'密码',type:'password'},
  {key:'root',label:'根路径',placeholder:'/'},
  {key:'privateKey',label:'私钥文件路径（可选）',placeholder:'/sdcard/id_rsa'},
  {key:'passphrase',label:'私钥密码（可选）',type:'password'}]},
 smb:{label:'SMB',icon:'layers',hint:'填写共享名，例如 share；域可留空。',fields:[
  {key:'host',label:'服务器',placeholder:'192.168.1.10',required:true},
  {key:'port',label:'端口',type:'number',value:'445'},
  {key:'domain',label:'域（可选）'},{key:'user',label:'用户名'},{key:'password',label:'密码',type:'password'},
  {key:'share',label:'共享名',placeholder:'share',required:true},
  {key:'root',label:'根路径',placeholder:'/'}]},
 s3:{label:'对象存储',icon:'layers',hint:'兼容 Amazon S3、MinIO、Cloudflare R2 等 S3 API。私有部署可开启路径风格。',fields:[
  {key:'endpoint',label:'Endpoint',placeholder:'https://s3.amazonaws.com',required:true},
  {key:'region',label:'区域',value:'us-east-1'},
  {key:'bucket',label:'Bucket',required:true},
  {key:'accessKey',label:'Access Key',required:true},
  {key:'secretKey',label:'Secret Key',type:'password',required:true},
  {key:'sessionToken',label:'Session Token（可选）',type:'password'},
  {key:'root',label:'前缀（可选）',placeholder:'folder/'},
  {key:'pathStyle',label:'路径风格（Path Style）',type:'checkbox',value:false}]}
};
function networkProtocolLabel(c){return (NETWORK_PROTOCOLS[c&&c.protocol]||{}).label||String((c&&c.protocol)||'').toUpperCase();}
function networkIcon(c){return (NETWORK_PROTOCOLS[c&&c.protocol]||{}).icon||'network';}
function networkTarget(c){if(!c)return '';if(c.protocol==='webdav')return c.url||'';if(c.protocol==='s3')return (c.bucket||'')+(c.endpoint?' @ '+c.endpoint:'');return [c.host,c.port,c.share,c.root].filter(Boolean).join(' · ');}
function netJoin(a,b){return [a,b].map(x=>String(x==null?'':x).replace(/^\/+|\/+$/g,'')).filter(Boolean).join('/');}
function validateNetworkConnection(c){
 if(!c.name)throw Error('请填写名称');
 const meta=NETWORK_PROTOCOLS[c.protocol];if(!meta)throw Error('请选择协议');
 for(const f of meta.fields)if(f.required&&!String(c[f.key]==null?'':c[f.key]).trim())throw Error('请填写'+f.label);
 if(c.port!==undefined&&(!Number.isFinite(Number(c.port))||Number(c.port)<1||Number(c.port)>65535))throw Error('端口无效');
}
function readNetworkForm(d,protocol,base){
 const c=Object.assign({},base,{protocol});
 d.querySelectorAll('[data-net]').forEach(i=>{c[i.dataset.net]=i.type==='checkbox'?i.checked:i.value.trim();});
 if(c.port===''||c.port==null)c.port=undefined;else c.port=Number(c.port);
 if(!c.id)c.id='net-'+Date.now().toString(36)+Math.random().toString(36).slice(2,6);
 return c;
}
function addNetworkStorage(){options('添加远程存储',Object.keys(NETWORK_PROTOCOLS).map(id=>({icon:NETWORK_PROTOCOLS[id].icon,label:NETWORK_PROTOCOLS[id].label,run:()=>networkStorageForm(id)})));}
function networkStorageForm(protocol,existing=null,done=null){
 const meta=NETWORK_PROTOCOLS[protocol];if(!meta)return;
 const data=Object.assign({},existing||{},{protocol});
 let html=`<p class="bundleHint">${esc(meta.hint)}</p><label>名称 *</label><input data-net="name" value="${esc(data.name||'')}" placeholder="例如：家庭 NAS" autocomplete="off">`;
 for(const f of meta.fields){const value=data[f.key]!==undefined?data[f.key]:f.value;if(f.type==='checkbox')html+=`<label class="toggleRow"><span>${esc(f.label)}</span><input type="checkbox" data-net="${f.key}" ${value?'checked':''}></label>`;else html+=`<label>${esc(f.label)}${f.required?' *':''}</label><input data-net="${f.key}" type="${f.type||'text'}" value="${esc(value==null?'':value)}" placeholder="${esc(f.placeholder||'')}" ${f.type==='password'?'autocomplete="new-password" spellcheck="false"':''} ${f.type==='number'?'inputmode="numeric"':''}>`;}
 const d=dialog((existing?'编辑 ':'添加 ')+meta.label,html,[
  ['取消',closeModal],
  ['测试连接',async()=>{const c=readNetworkForm(d,protocol,data);validateNetworkConnection(c);await rpc('netStorage',{op:'test',connection:c});toast('连接成功');}],
  [existing?'保存':'添加',async()=>{const c=readNetworkForm(d,protocol,data);validateNetworkConnection(c);const list=Array.isArray(pref.networkStorages)?pref.networkStorages.slice():[];const index=existing?list.findIndex(x=>x.id===existing.id):-1;if(index>=0)list[index]=c;else list.push(c);pref.networkStorages=list;savePref();if(existing)closeDrawer();closeModal();toast(existing?'网络存储已更新':'已添加 '+c.name);if(done)await done(c);}]
 ]);
 d.classList.add('networkStorageDialog');
}
function networkStorageActions(c){
 options(c.name||networkProtocolLabel(c),[
  {icon:'network',label:'打开',run:()=>openRemoteStorage(c)},
  {icon:'edit',label:'编辑连接',run:()=>networkStorageForm(c.protocol,c)},
  {icon:'trash',label:'删除连接',run:async()=>{const opened=activePane().mode==='network'&&activePane().net&&activePane().net.id===c.id;if(await confirmBox('删除远程存储？',c.name||networkProtocolLabel(c),'删除')){pref.networkStorages=(pref.networkStorages||[]).filter(x=>x.id!==c.id);savePref();closeDrawer();toast('已删除');if(opened)await navigate(state.active,{mode:'',net:null,path:state.info.home,archive:'',entry:''},false);}}}
 ]);
}
async function openRemoteStorage(c){
 closeDrawer();
 if(state.screen==='editor'&&!await canLeave())return;
 if(state.screen==='editor'&&state.disposeEditor)state.disposeEditor();
 const i=state.active,p=state.panes[i];
 p.history.push(paneLocation(p));p.future=[];
 Object.assign(p,{mode:'network',net:c,path:'',archive:'',entry:'',scrollTop:0,selected:new Set(),appendMode:false,gestureAnchor:null,error:''});
 state.screen='files';await loadPane(i);renderFiles();animateContent($('.pane.active'));
}
async function remoteNavigate(conn,path){await navigate(state.active,{mode:'network',net:conn,path:String(path||''),archive:'',entry:''});}
async function leaveRemote(){const p=activePane();while(p.history.length&&p.history[p.history.length-1].mode==='network')p.history.pop();if(p.history.length)return backPane();await navigate(state.active,{mode:'',net:null,path:state.info.home,archive:'',entry:''},false);}
async function remoteRefresh(){await loadPane(state.active);renderFiles();}
async function remoteMkdir(){const p=activePane();if(!p.net)return;const name=await ask('新建远程文件夹','');if(!name)return;await rpc('netStorage',{op:'mkdir',connection:p.net,path:netJoin(p.path,name)});await remoteRefresh();}
async function remoteUpload(){const p=activePane();if(!p.net)return;const local=await ask('上传本地文件',state.info.home+'/');if(!local)return;const name=local.split('/').pop();if(!name)return;await rpc('netStorage',{op:'upload',connection:p.net,path:netJoin(p.path,name),local});toast('已上传 '+name);await remoteRefresh();}
async function remoteDownload(conn,file){const local=await ask('下载到',state.info.workspace+'/'+file.name);if(!local)return;await rpc('netStorage',{op:'download',connection:conn,path:file.path,local});toast('已下载 '+local);}
function remoteSettings(){const p=activePane();if(!p.net)return;networkStorageForm(p.net.protocol,p.net,async updated=>{p.net=updated;await loadPane(state.active);renderFiles();});}
async function remoteRename(conn,file,index){const name=await ask('重命名',file.name);if(!name||name===file.name)return;const parent=String(file.path||'').replace(/\/+$/,'').split('/').slice(0,-1).join('/');await rpc('netStorage',{op:'rename',connection:conn,path:file.path,to:netJoin(parent,name),dir:file.dir});state.active=index;await remoteRefresh();}
async function remoteDelete(conn,file,index){if(!await confirmBox('删除远程项目？',file.name+' 将被永久删除。','删除'))return;await rpc('netStorage',{op:'delete',connection:conn,path:file.path,dir:file.dir});state.active=index;await remoteRefresh();}
function remoteEntryMenu(conn,file,index){
 options(file.name,[
  {icon:file.dir?'folder':'download',label:file.dir?'打开':'下载',run:()=>{state.active=index;return file.dir?remoteNavigate(conn,file.path):remoteDownload(conn,file);}},
  {icon:'edit',label:'重命名',run:()=>remoteRename(conn,file,index)},
  {icon:'trash',label:'删除',run:()=>remoteDelete(conn,file,index)}
 ]);
}
function networkRow(f,index){
 const p=state.panes[index],selected=p.selected&&p.selected.has(fileKey(f)),type=fileType(f);
 const b=document.createElement('button');b.className='file remoteRow'+(selected?' selected':'');b.type='button';b.dataset.index=String(index);
 b.innerHTML=`<span class="fileicon ${type}">${icon(type)}</span><span class="fileinfo"><span class="fileline"><span class="filename">${esc(displayName(f))}</span><span class="filesize">${f.dir?'':size(f.size)}</span></span><span class="filemeta">${f.time?date(f.time):'远程'}</span></span>`;
 hold(b,()=>{state.active=index;remoteEntryMenu(p.net,f,index);},()=>{state.active=index;return f.dir?remoteNavigate(p.net,f.path):remoteDownload(p.net,f);});
 return b;
}
function remoteMenu(){const p=activePane();if(!p.net)return;options(p.net.name||networkProtocolLabel(p.net),[
 {icon:'refresh',label:'刷新',run:remoteRefresh},
 {icon:'plus',label:'新建文件夹',run:remoteMkdir},
 {icon:'export',label:'上传文件',run:remoteUpload},
 {icon:'settings',label:'连接设置',run:remoteSettings},
 {icon:'back',label:'离开远程存储',run:leaveRemote}
]);}
async function colorPickerTool(){
  let status;try{status=await rpc('colorPicker',{op:'status'});}catch(e){return alertBox('屏幕取色不可用',e.message||String(e));}
  if(status.running)return options('屏幕取色',[{icon:'close',label:'停止取色',run:async()=>{await rpc('colorPicker',{op:'stop'});toast('屏幕取色已停止');}},{icon:'settings',label:'取色设置',run:colorPickerSettings}]);
  options('屏幕取色',[{icon:'search',label:'开始取色',run:async()=>{
    await rpc('colorPicker',{op:'start'});
    toast('请按系统提示授权：显示在其他应用上层、通知、屏幕录制');
    for(let i=0;i<20;i++){await new Promise(r=>setTimeout(r,1000));
      try{const s=await rpc('colorPicker',{op:'status'});if(s.running){toast('取色已启动：拖动准星取样，轻点放大镜上的色值或坐标即复制');return;}}catch(e){return;}}
  }},{icon:'settings',label:'取色设置',run:colorPickerSettings},{icon:'info',label:'使用说明',run:()=>alertBox('使用说明','1. 启动后拖动屏幕上的十字准星对准目标像素，放大镜实时显示放大图像。\n2. 放大镜外圈显示 HEX 色值与像素坐标，两者都可轻点，轻点即复制到剪贴板。\n3. 在放大镜圆环上滑动可 0.1 倍微调准星位置。\n4. 轻点放大镜上的 × 结束取色，或回到本应用侧边栏停止。\n5. 授权屏幕录制时若询问单个应用或整个屏幕，请选择整个屏幕。\n6. 取色以前台服务运行，通知栏可停止；手机卡顿可在设置中降低刷新速度。')}]);
}
async function colorPickerSettings(){
  let p;try{p=await rpc('colorPicker',{op:'get'});}catch(e){return alertBox('无法读取取色设置',e.message||String(e));}
  const sz={small:'小',medium:'中',large:'大'},sp={fast:'快',normal:'中',slow:'慢'};
  options('取色设置',[
    {icon:'search',label:'放大镜大小：'+(sz[p.magnifierSize]||p.magnifierSize),run:()=>colorPickerPick('magnifierSize',['small','medium','large'],['小','中','大'],p.magnifierSize)},
    {icon:'phone',label:'采样范围：'+(sz[p.captureRange]||p.captureRange),run:()=>colorPickerPick('captureRange',['small','medium','large'],['小','中','大'],p.captureRange)},
    {icon:'refresh',label:'刷新速度：'+(sp[p.captureSpeed]||p.captureSpeed),run:()=>colorPickerPick('captureSpeed',['fast','normal','slow'],['快','中','慢'],p.captureSpeed)},
    {icon:'check',label:'网格线：'+(p.showGridLines?'开':'关'),run:async()=>{await rpc('colorPicker',{op:'set',key:'showGridLines',value:!p.showGridLines});toast('网格线已'+(p.showGridLines?'关闭':'开启')+'（重启取色生效）');}}
  ]);
}
function colorPickerPick(key,values,labels,current){
  options(key==='magnifierSize'?'放大镜大小':key==='captureRange'?'采样范围':'刷新速度',values.map((v,i)=>({label:labels[i]+(v===current?'（当前）':''),run:async()=>{await rpc('colorPicker',{op:'set',key,value:v});toast('已保存（重启取色生效）');}})));
}
async function elevatedTool(){
  let s;try{s=await rpc('elevated',{op:'status'});}catch(e){return alertBox('授权管理不可用',e.message||String(e));}
  const rootLabel='Root：'+(s.rootGranted?'已授权':s.rootAvailable?'未授权 · 轻点申请':'未检测到 su');
  const shizukuLabel='Shizuku：'+(s.shizukuGranted?'已授权':!s.shizukuInstalled?'未安装':!s.shizukuRunning?'未运行':'未授权 · 轻点申请');
  options('Root / Shizuku 授权',[
    {icon:'switch',label:rootLabel,run:async()=>{
      if(s.rootGranted)return toast('Root 已授权');
      if(!s.rootAvailable)return alertBox('Root 不可用','未在设备上检测到 su。请先使用 Magisk / KernelSU 等方式获取 Root。');
      toast('正在请求 Root 授权，请在弹窗中允许…');
      const r=await rpc('elevated',{op:'requestRoot'});
      toast(r.granted?'Root 授权成功':'Root 授权被拒绝或超时');
      elevatedTool();
    }},
    {icon:'layers',label:shizukuLabel,run:async()=>{
      if(s.shizukuGranted)return toast('Shizuku 已授权');
      if(!s.shizukuInstalled)return alertBox('Shizuku 未安装','请先安装 Shizuku 应用，并通过 Root 或无线调试启动它。');
      if(!s.shizukuRunning)return alertBox('Shizuku 未运行','请打开 Shizuku 应用启动服务后重试。');
      toast('正在请求 Shizuku 授权，请在弹窗中允许…');
      const r=await rpc('elevated',{op:'requestShizuku'});
      toast(r.granted?'Shizuku 授权成功':'Shizuku 授权被拒绝或超时');
      elevatedTool();
    }},
    {icon:'search',label:'诊断提权链路',run:async()=>{
      const d=await rpc('elevated',{op:'diag'});
      alertBox('提权诊断',
        'rootGranted（已授权Root）: '+d.rootGranted+'\n'+
        'rootAvailable（检测到su）: '+d.rootAvailable+'\n'+
        'shizukuInstalled（已装Shizuku）: '+d.shizukuInstalled+'\n'+
        'shizukuRunning（服务运行中）: '+d.shizukuRunning+'\n'+
        'shizukuGranted（已授权）: '+d.shizukuGranted+'\n'+
        'shizukuVersion: '+d.shizukuVersion+'\n'+
        'elevatedId（提权执行id）: '+d.elevatedId+'\n'+
        'npId（Shizuku进程探测）: '+(d.npId===undefined?'':d.npId)+'\n'+
        'npErr（Shizuku进程报错）: '+(d.npErr===undefined?'':d.npErr)+'\n'+
        'rootListCount（根目录提权列出条数）: '+d.rootListCount+'\n\n把这一屏截图发给我');
    }},
    {icon:'info',label:'说明',run:()=>alertBox('授权说明','Root 通过 su 授权弹窗申请；Shizuku 需先安装并启动 Shizuku 应用。\n\n授权后浏览根目录或受限目录（如 /data）时，Root 经 su、Shizuku 经 shell 特权实际枚举内容，可看到并进入更多文件夹，与 MT 管理器一致；未授权时只显示内置标准清单。\n\n撤销授权请在 Magisk / Shizuku 中操作。')}
  ]);
}
const TRANSLATE_PROVIDERS=[['tencent','腾讯云翻译'],['baidu','百度翻译'],['aliyun','阿里云翻译'],['volc','火山引擎翻译'],['microsoft','微软翻译'],['ai','AI 大模型']];
let translateConfig=null,translateLanguages=[];
async function loadTranslateConfig(){
 const r=await rpc('translateConfig',{op:'get'});
 translateConfig=r.config||{};const lg=r.languages;translateLanguages=(lg&&Array.isArray(lg.languages)?lg.languages:Array.isArray(lg)?lg:[]);
 return translateConfig;
}
function translateProviderName(key){const row=TRANSLATE_PROVIDERS.find(p=>p[0]===key);return row?row[1]:key;}
function translateTargetName(key){const row=(translateLanguages||[]).find(l=>l.key===key);return row?row.name:key;}
async function translateText(text,to){
 if(!text||!text.trim())return '';
 if(!translateLanguages.length)await loadTranslateConfig();
 const r=await rpc('translate',{text:text,to:to||(translateConfig&&translateConfig.target)||''});
 return r.text||'';
}
function translateFields(provider){
 if(provider==='tencent')return [
  ['tencentSecretId','SecretId','text','腾讯云访问密钥 ID'],
  ['tencentSecretKey','SecretKey','password','腾讯云访问密钥'],
  ['tencentProjectId','项目 ID（可留空）','text','纯数字；留空使用默认项目 0，一般无需填写'],
  ['tencentRegion','地域（可留空）','text','留空默认 ap-guangzhou，例如 ap-shanghai']];
 if(provider==='baidu')return [
  ['baiduAppId','AppID','text','百度翻译应用 ID'],
  ['baiduAppKey','App Key','password','百度翻译密钥']];
 if(provider==='aliyun')return [
  ['aliyunAccessKeyId','AccessKey ID','text','阿里云 RAM 访问密钥 ID'],
  ['aliyunAccessKeySecret','AccessKey Secret','password','阿里云访问密钥 Secret']];
 if(provider==='volc')return [
  ['volcAccessKeyId','AccessKey ID','text','火山引擎访问密钥 ID'],
  ['volcSecretAccessKey','Secret Access Key','password','火山引擎私密密钥']];
 if(provider==='microsoft')return [
  ['microsoftKey','订阅密钥','password','Azure Translator 密钥'],
  ['microsoftRegion','资源区域','text','可选，例如 eastasia']];
 return [
  ['aiBaseUrl','接口地址','text','例如 https://api.openai.com/v1'],
  ['aiApiKey','API Key','password','模型服务密钥'],
  ['aiModel','模型','text','例如 gpt-4o-mini'],
  ['aiPrompt','提示词','textarea','{lang} 会被替换为目标语言']];
}
const SMALI_FORMAT_HELP='格式：v=寄存器，vAA=8 位寄存器，vAAAA=16 位寄存器，#=立即数，@=索引，+/-=分支偏移。';
const SMALI_CATEGORIES=['全部','常用','数据/常量','运算/转换','比较/跳转','数组/字段','方法/对象','返回/异常','负载/旧版'];
const SMALI_COMMON=new Set(["move","move-result","move-result-wide","move-result-object","move-exception","const/4","const/16","const","const-string","const-class","check-cast","instance-of","new-instance","new-array","filled-new-array","aget","aput","iget","iput","sget","sput","invoke-virtual","invoke-direct","invoke-static","invoke-interface","invoke-super","if-eq","if-eqz","if-ne","if-nez","goto","packed-switch","sparse-switch","return","return-void","return-object","cmp-long","cmpl-float","cmpg-float","add-int","sub-int","mul-int","div-int","rem-int","and-int","or-int","xor-int","shl-int","shr-int","ushr-int","add-int/lit8","int-to-long","int-to-byte","monitor-enter","monitor-exit","throw","array-length"]);
const SMALI_OPS=[
 {
  "op": "move",
  "fmt": "12x",
  "cat": "数据/常量",
  "desc": "将 vB 的值复制到 vA，vB 保持不变。",
  "ex": "move v0, v1"
 },
 {
  "op": "move/from16",
  "fmt": "22x",
  "cat": "数据/常量",
  "desc": "从 16 位寄存器 vB 复制到 vA。",
  "ex": "move/from16 v0, v16"
 },
 {
  "op": "move/16",
  "fmt": "32x",
  "cat": "数据/常量",
  "desc": "从 16 位寄存器编号复制到 vA，两个寄存器均使用 16 位编号。",
  "ex": "move/16 v0, v256"
 },
 {
  "op": "move-wide",
  "fmt": "12x",
  "cat": "数据/常量",
  "desc": "复制 64 位值，占用 vA/vA+1 与 vB/vB+1。",
  "ex": "move-wide v0, v2"
 },
 {
  "op": "move-wide/from16",
  "fmt": "22x",
  "cat": "数据/常量",
  "desc": "从 16 位寄存器复制 64 位值。",
  "ex": "move-wide/from16 v0, v16"
 },
 {
  "op": "move-wide/16",
  "fmt": "32x",
  "cat": "数据/常量",
  "desc": "使用 16 位寄存器编号复制 64 位值。",
  "ex": "move-wide/16 v0, v256"
 },
 {
  "op": "move-object",
  "fmt": "12x",
  "cat": "数据/常量",
  "desc": "复制对象引用。",
  "ex": "move-object v0, v1"
 },
 {
  "op": "move-object/from16",
  "fmt": "22x",
  "cat": "数据/常量",
  "desc": "从 16 位寄存器复制对象引用。",
  "ex": "move-object/from16 v0, v16"
 },
 {
  "op": "move-object/16",
  "fmt": "32x",
  "cat": "数据/常量",
  "desc": "使用 16 位寄存器编号复制对象引用。",
  "ex": "move-object/16 v0, v256"
 },
 {
  "op": "move-result",
  "fmt": "11x",
  "cat": "数据/常量",
  "desc": "把上一条 invoke 的 32 位返回值写入 vAA；必须紧随调用。",
  "ex": "move-result v0"
 },
 {
  "op": "move-result-wide",
  "fmt": "11x",
  "cat": "数据/常量",
  "desc": "把上一条 invoke 的 64 位返回值写入 vAA/vAA+1。",
  "ex": "move-result-wide v0"
 },
 {
  "op": "move-result-object",
  "fmt": "11x",
  "cat": "数据/常量",
  "desc": "把上一条 invoke 返回的对象引用写入 vAA。",
  "ex": "move-result-object v0"
 },
 {
  "op": "move-exception",
  "fmt": "11x",
  "cat": "数据/常量",
  "desc": "进入 catch 块时，把当前异常对象写入 vAA。",
  "ex": "move-exception v0"
 },
 {
  "op": "nop",
  "fmt": "10x",
  "cat": "数据/常量",
  "desc": "空操作，常用于对齐、占位或补丁。",
  "ex": "nop"
 },
 {
  "op": "breakpoint",
  "fmt": "10x",
  "cat": "数据/常量",
  "desc": "调试器断点指令，普通应用代码不应生成。",
  "ex": "breakpoint"
 },
 {
  "op": "const/4",
  "fmt": "11n",
  "cat": "数据/常量",
  "desc": "把 4 位有符号立即数写入 vA，范围 -8 到 7。",
  "ex": "const/4 v0, 0x1"
 },
 {
  "op": "const/16",
  "fmt": "21s",
  "cat": "数据/常量",
  "desc": "把 16 位有符号立即数写入 vAA。",
  "ex": "const/16 v0, 0x100"
 },
 {
  "op": "const",
  "fmt": "31i",
  "cat": "数据/常量",
  "desc": "把 32 位立即数写入 vAA。",
  "ex": "const v0, 0x12345678"
 },
 {
  "op": "const-wide/16",
  "fmt": "21s",
  "cat": "数据/常量",
  "desc": "把 16 位有符号立即数扩展到 64 位后写入 vAA。",
  "ex": "const-wide/16 v0, 0x100"
 },
 {
  "op": "const-wide/32",
  "fmt": "31i",
  "cat": "数据/常量",
  "desc": "把 32 位有符号立即数扩展到 64 位后写入 vAA。",
  "ex": "const-wide/32 v0, 0x12345678"
 },
 {
  "op": "const-wide",
  "fmt": "51l",
  "cat": "数据/常量",
  "desc": "把 64 位立即数写入 vAA/vAA+1。",
  "ex": "const-wide v0, 0x123456789abcdef0L"
 },
 {
  "op": "const/high16",
  "fmt": "21h",
  "cat": "数据/常量",
  "desc": "把 16 位立即数左移 16 位后写入 vAA。",
  "ex": "const/high16 v0, 0x4000"
 },
 {
  "op": "const-wide/high16",
  "fmt": "21h",
  "cat": "数据/常量",
  "desc": "把 16 位立即数左移 48 位后写入 vAA/vAA+1。",
  "ex": "const-wide/high16 v0, 0x4000"
 },
 {
  "op": "const-string",
  "fmt": "21c",
  "cat": "数据/常量",
  "desc": "把字符串常量引用写入 vAA。",
  "ex": "const-string v0, \"hello\""
 },
 {
  "op": "const-string/jumbo",
  "fmt": "31c",
  "cat": "数据/常量",
  "desc": "使用 32 位字符串索引写入常量，适用于超大 DEX。",
  "ex": "const-string/jumbo v0, \"hello\""
 },
 {
  "op": "const-class",
  "fmt": "21c",
  "cat": "数据/常量",
  "desc": "把类型引用写入 vAA。",
  "ex": "const-class v0, Ljava/lang/String;"
 },
 {
  "op": "const-method-handle",
  "fmt": "21c",
  "cat": "数据/常量",
  "desc": "把 MethodHandle 常量写入 vAA。",
  "ex": "const-method-handle v0, invoke-static@Lcom/example/Foo;->bar()V"
 },
 {
  "op": "const-method-type",
  "fmt": "21c",
  "cat": "数据/常量",
  "desc": "把 MethodType 常量写入 vAA。",
  "ex": "const-method-type v0, (I)V"
 },
 {
  "op": "neg-int",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 int 取负。",
  "ex": "neg-int v0, v1"
 },
 {
  "op": "not-int",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 int 按位取反。",
  "ex": "not-int v0, v1"
 },
 {
  "op": "neg-long",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 long 取负。",
  "ex": "neg-long v0, v1"
 },
 {
  "op": "not-long",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 long 按位取反。",
  "ex": "not-long v0, v1"
 },
 {
  "op": "neg-float",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 float 取负。",
  "ex": "neg-float v0, v1"
 },
 {
  "op": "neg-double",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "对 double 取负。",
  "ex": "neg-double v0, v1"
 },
 {
  "op": "int-to-long",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 符号扩展到 long。",
  "ex": "int-to-long v0, v1"
 },
 {
  "op": "int-to-float",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 转换为 float。",
  "ex": "int-to-float v0, v1"
 },
 {
  "op": "int-to-double",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 转换为 double。",
  "ex": "int-to-double v0, v1"
 },
 {
  "op": "long-to-int",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 long 截断为 int。",
  "ex": "long-to-int v0, v1"
 },
 {
  "op": "long-to-float",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 long 转换为 float。",
  "ex": "long-to-float v0, v1"
 },
 {
  "op": "long-to-double",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 long 转换为 double。",
  "ex": "long-to-double v0, v1"
 },
 {
  "op": "float-to-int",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 float 转换为 int，向零截断。",
  "ex": "float-to-int v0, v1"
 },
 {
  "op": "float-to-long",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 float 转换为 long，向零截断。",
  "ex": "float-to-long v0, v1"
 },
 {
  "op": "float-to-double",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 float 扩展为 double。",
  "ex": "float-to-double v0, v1"
 },
 {
  "op": "double-to-int",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 double 转换为 int，向零截断。",
  "ex": "double-to-int v0, v1"
 },
 {
  "op": "double-to-long",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 double 转换为 long，向零截断。",
  "ex": "double-to-long v0, v1"
 },
 {
  "op": "double-to-float",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 double 收窄为 float。",
  "ex": "double-to-float v0, v1"
 },
 {
  "op": "int-to-byte",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 截断为 byte 并符号扩展。",
  "ex": "int-to-byte v0, v1"
 },
 {
  "op": "int-to-char",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 截断为 char 并零扩展。",
  "ex": "int-to-char v0, v1"
 },
 {
  "op": "int-to-short",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "把 int 截断为 short 并符号扩展。",
  "ex": "int-to-short v0, v1"
 },
 {
  "op": "add-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 加法。",
  "ex": "add-int v0, v1, v2"
 },
 {
  "op": "sub-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 减法。",
  "ex": "sub-int v0, v1, v2"
 },
 {
  "op": "mul-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 乘法。",
  "ex": "mul-int v0, v1, v2"
 },
 {
  "op": "div-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 除法，除零抛 ArithmeticException。",
  "ex": "div-int v0, v1, v2"
 },
 {
  "op": "rem-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 取余。",
  "ex": "rem-int v0, v1, v2"
 },
 {
  "op": "and-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 按位与。",
  "ex": "and-int v0, v1, v2"
 },
 {
  "op": "or-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 按位或。",
  "ex": "or-int v0, v1, v2"
 },
 {
  "op": "xor-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 按位异或。",
  "ex": "xor-int v0, v1, v2"
 },
 {
  "op": "shl-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 左移，移位量取低 5 位。",
  "ex": "shl-int v0, v1, v2"
 },
 {
  "op": "shr-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 算术右移，移位量取低 5 位。",
  "ex": "shr-int v0, v1, v2"
 },
 {
  "op": "ushr-int",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "int 逻辑右移，移位量取低 5 位。",
  "ex": "ushr-int v0, v1, v2"
 },
 {
  "op": "add-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 加法。",
  "ex": "add-long v0, v1, v2"
 },
 {
  "op": "sub-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 减法。",
  "ex": "sub-long v0, v1, v2"
 },
 {
  "op": "mul-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 乘法。",
  "ex": "mul-long v0, v1, v2"
 },
 {
  "op": "div-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 除法，除零抛 ArithmeticException。",
  "ex": "div-long v0, v1, v2"
 },
 {
  "op": "rem-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 取余。",
  "ex": "rem-long v0, v1, v2"
 },
 {
  "op": "and-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 按位与。",
  "ex": "and-long v0, v1, v2"
 },
 {
  "op": "or-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 按位或。",
  "ex": "or-long v0, v1, v2"
 },
 {
  "op": "xor-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 按位异或。",
  "ex": "xor-long v0, v1, v2"
 },
 {
  "op": "shl-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 左移，移位量取低 6 位。",
  "ex": "shl-long v0, v1, v2"
 },
 {
  "op": "shr-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 算术右移，移位量取低 6 位。",
  "ex": "shr-long v0, v1, v2"
 },
 {
  "op": "ushr-long",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "long 逻辑右移，移位量取低 6 位。",
  "ex": "ushr-long v0, v1, v2"
 },
 {
  "op": "add-float",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "float 加法。",
  "ex": "add-float v0, v1, v2"
 },
 {
  "op": "sub-float",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "float 减法。",
  "ex": "sub-float v0, v1, v2"
 },
 {
  "op": "mul-float",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "float 乘法。",
  "ex": "mul-float v0, v1, v2"
 },
 {
  "op": "div-float",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "float 除法。",
  "ex": "div-float v0, v1, v2"
 },
 {
  "op": "rem-float",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "float 取余。",
  "ex": "rem-float v0, v1, v2"
 },
 {
  "op": "add-double",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "double 加法。",
  "ex": "add-double v0, v1, v2"
 },
 {
  "op": "sub-double",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "double 减法。",
  "ex": "sub-double v0, v1, v2"
 },
 {
  "op": "mul-double",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "double 乘法。",
  "ex": "mul-double v0, v1, v2"
 },
 {
  "op": "div-double",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "double 除法。",
  "ex": "div-double v0, v1, v2"
 },
 {
  "op": "rem-double",
  "fmt": "23x",
  "cat": "运算/转换",
  "desc": "double 取余。",
  "ex": "rem-double v0, v1, v2"
 },
 {
  "op": "add-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 加 16 位有符号立即数。",
  "ex": "add-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "rsub-int",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "计算 16 位有符号立即数减 vB。",
  "ex": "rsub-int v0, v1, 0x100"
 },
 {
  "op": "mul-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 乘 16 位有符号立即数。",
  "ex": "mul-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "div-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 除以 16 位有符号立即数。",
  "ex": "div-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "rem-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 对 16 位有符号立即数取余。",
  "ex": "rem-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "and-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 与 16 位立即数按位与。",
  "ex": "and-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "or-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 与 16 位立即数按位或。",
  "ex": "or-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "xor-int/lit16",
  "fmt": "22s",
  "cat": "运算/转换",
  "desc": "vB 与 16 位立即数按位异或。",
  "ex": "xor-int/lit16 v0, v1, 0x100"
 },
 {
  "op": "add-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 加 8 位有符号立即数。",
  "ex": "add-int/lit8 v0, v1, 0x1"
 },
 {
  "op": "rsub-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "计算 8 位有符号立即数减 vB。",
  "ex": "rsub-int/lit8 v0, v1, 0x1"
 },
 {
  "op": "mul-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 乘 8 位有符号立即数。",
  "ex": "mul-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "div-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 除以 8 位有符号立即数。",
  "ex": "div-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "rem-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 对 8 位有符号立即数取余。",
  "ex": "rem-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "and-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 与 8 位立即数按位与。",
  "ex": "and-int/lit8 v0, v1, 0x1"
 },
 {
  "op": "or-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 与 8 位立即数按位或。",
  "ex": "or-int/lit8 v0, v1, 0x1"
 },
 {
  "op": "xor-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 与 8 位立即数按位异或。",
  "ex": "xor-int/lit8 v0, v1, 0x1"
 },
 {
  "op": "shl-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 左移 8 位立即数指定的位数。",
  "ex": "shl-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "shr-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 算术右移 8 位立即数指定的位数。",
  "ex": "shr-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "ushr-int/lit8",
  "fmt": "22b",
  "cat": "运算/转换",
  "desc": "vB 逻辑右移 8 位立即数指定的位数。",
  "ex": "ushr-int/lit8 v0, v1, 0x2"
 },
 {
  "op": "cmpl-float",
  "fmt": "23x",
  "cat": "比较/跳转",
  "desc": "比较两个 float；vB < vC 返回 -1，相等返回 0，大于返回 1，NaN 返回 -1。",
  "ex": "cmpl-float v0, v1, v2"
 },
 {
  "op": "cmpg-float",
  "fmt": "23x",
  "cat": "比较/跳转",
  "desc": "比较两个 float；vB < vC 返回 -1，相等返回 0，大于返回 1，NaN 返回 1。",
  "ex": "cmpg-float v0, v1, v2"
 },
 {
  "op": "cmpl-double",
  "fmt": "23x",
  "cat": "比较/跳转",
  "desc": "比较两个 double；NaN 返回 -1。",
  "ex": "cmpl-double v0, v1, v2"
 },
 {
  "op": "cmpg-double",
  "fmt": "23x",
  "cat": "比较/跳转",
  "desc": "比较两个 double；NaN 返回 1。",
  "ex": "cmpg-double v0, v1, v2"
 },
 {
  "op": "cmp-long",
  "fmt": "23x",
  "cat": "比较/跳转",
  "desc": "比较两个 long；结果 -1、0 或 1 写入 vA。",
  "ex": "cmp-long v0, v1, v2"
 },
 {
  "op": "if-eq",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA == vB 则跳转到目标标签。",
  "ex": "if-eq v0, v1, :cond_0"
 },
 {
  "op": "if-ne",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA != vB 则跳转到目标标签。",
  "ex": "if-ne v0, v1, :cond_0"
 },
 {
  "op": "if-lt",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA < vB 则跳转到目标标签。",
  "ex": "if-lt v0, v1, :cond_0"
 },
 {
  "op": "if-ge",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA >= vB 则跳转到目标标签。",
  "ex": "if-ge v0, v1, :cond_0"
 },
 {
  "op": "if-gt",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA > vB 则跳转到目标标签。",
  "ex": "if-gt v0, v1, :cond_0"
 },
 {
  "op": "if-le",
  "fmt": "22t",
  "cat": "比较/跳转",
  "desc": "按有符号整数或引用比较，若 vA <= vB 则跳转到目标标签。",
  "ex": "if-le v0, v1, :cond_0"
 },
 {
  "op": "if-eqz",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA == 0 或 null 则跳转。",
  "ex": "if-eqz v0, :cond_0"
 },
 {
  "op": "if-nez",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA != 0 或非 null 则跳转。",
  "ex": "if-nez v0, :cond_0"
 },
 {
  "op": "if-ltz",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA < 0 则跳转。",
  "ex": "if-ltz v0, :cond_0"
 },
 {
  "op": "if-gez",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA >= 0 则跳转。",
  "ex": "if-gez v0, :cond_0"
 },
 {
  "op": "if-gtz",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA > 0 则跳转。",
  "ex": "if-gtz v0, :cond_0"
 },
 {
  "op": "if-lez",
  "fmt": "21t",
  "cat": "比较/跳转",
  "desc": "与零或 null 比较，若 vAA <= 0 则跳转。",
  "ex": "if-lez v0, :cond_0"
 },
 {
  "op": "goto",
  "fmt": "10t",
  "cat": "比较/跳转",
  "desc": "无条件跳转到标签，使用 8 位分支偏移。",
  "ex": "goto :goto_0"
 },
 {
  "op": "goto/16",
  "fmt": "20t",
  "cat": "比较/跳转",
  "desc": "无条件跳转，使用 16 位分支偏移。",
  "ex": "goto/16 :goto_0"
 },
 {
  "op": "goto/32",
  "fmt": "30t",
  "cat": "比较/跳转",
  "desc": "无条件跳转，使用 32 位分支偏移。",
  "ex": "goto/32 :goto_0"
 },
 {
  "op": "packed-switch",
  "fmt": "31t",
  "cat": "比较/跳转",
  "desc": "按连续整数索引跳转，数据来自 packed-switch-data 负载。",
  "ex": "packed-switch v0, :pswitch_data_0"
 },
 {
  "op": "sparse-switch",
  "fmt": "31t",
  "cat": "比较/跳转",
  "desc": "按稀疏整数键跳转，数据来自 sparse-switch-data 负载。",
  "ex": "sparse-switch v0, :sswitch_data_0"
 },
 {
  "op": "aget",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 int 数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-wide",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 long/double 数组元素，占用两个寄存器。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-object",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取对象数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-boolean",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 boolean 数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-byte",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 byte 数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-char",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 char 数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aget-short",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "从 vB 数组的 vC 索引读取，结果写入 vA；读取 short 数组元素。",
  "ex": "aget v0, v1, v2"
 },
 {
  "op": "aput",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 int 数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-wide",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 long/double 数组元素，占用两个寄存器。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-object",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入对象数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-boolean",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 boolean 数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-byte",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 byte 数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-char",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 char 数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "aput-short",
  "fmt": "23x",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 数组的 vC 索引；写入 short 数组元素。",
  "ex": "aput v0, v1, v2"
 },
 {
  "op": "array-length",
  "fmt": "12x",
  "cat": "数组/字段",
  "desc": "读取 vB 数组长度并写入 vA。",
  "ex": "array-length v0, v1"
 },
 {
  "op": "new-array",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "创建长度为 vB 的指定类型数组，引用写入 vA。",
  "ex": "new-array v0, v1, [I"
 },
 {
  "op": "filled-new-array",
  "fmt": "35c",
  "cat": "数组/字段",
  "desc": "用寄存器列表创建数组，结果通过 move-result-object 获取。",
  "ex": "filled-new-array {v0, v1}, [I"
 },
 {
  "op": "filled-new-array/range",
  "fmt": "3rc",
  "cat": "数组/字段",
  "desc": "用连续寄存器范围创建数组，结果通过 move-result-object 获取。",
  "ex": "filled-new-array/range {v0 .. v5}, [I"
 },
 {
  "op": "fill-array-data",
  "fmt": "31t",
  "cat": "数组/字段",
  "desc": "用静态数据负载填充数组，数组引用在 vAA。",
  "ex": "fill-array-data v0, :array_0"
 },
 {
  "op": "iget",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 int 实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-wide",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 long/double 实例字段，占用两个寄存器。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-object",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取对象实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-boolean",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 boolean 实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-byte",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 byte 实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-char",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 char 实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iget-short",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "从 vB 对象读取实例字段到 vA；读取 short 实例字段。",
  "ex": "iget v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 int 实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-wide",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 long/double 实例字段，占用两个寄存器。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-object",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入对象实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-boolean",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 boolean 实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-byte",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 byte 实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-char",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 char 实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "iput-short",
  "fmt": "22c",
  "cat": "数组/字段",
  "desc": "把 vA 写入 vB 对象的实例字段；写入 short 实例字段。",
  "ex": "iput v0, v1, Lcom/example/Foo;->value:I"
 },
 {
  "op": "sget",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 int 静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-wide",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 long/double 静态字段，占用两个寄存器。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-object",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取对象静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-boolean",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 boolean 静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-byte",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 byte 静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-char",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 char 静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sget-short",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "读取指定静态字段到 vAA；读取 short 静态字段。",
  "ex": "sget v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 int 静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-wide",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 long/double 静态字段，占用两个寄存器。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-object",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入对象静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-boolean",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 boolean 静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-byte",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 byte 静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-char",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 char 静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "sput-short",
  "fmt": "21c",
  "cat": "数组/字段",
  "desc": "把 vAA 写入指定静态字段；写入 short 静态字段。",
  "ex": "sput v0, Lcom/example/Foo;->count:I"
 },
 {
  "op": "new-instance",
  "fmt": "21c",
  "cat": "方法/对象",
  "desc": "分配指定类型对象，引用写入 vAA；之后必须调用构造函数 <init>。",
  "ex": "new-instance v0, Ljava/lang/StringBuilder;"
 },
 {
  "op": "check-cast",
  "fmt": "21c",
  "cat": "方法/对象",
  "desc": "检查 vAA 的对象能否转换为指定类型，失败抛 ClassCastException。",
  "ex": "check-cast v0, Ljava/lang/String;"
 },
 {
  "op": "instance-of",
  "fmt": "22c",
  "cat": "方法/对象",
  "desc": "判断 vB 是否为指定类型，结果 0 或 1 写入 vA。",
  "ex": "instance-of v0, v1, Ljava/lang/String;"
 },
 {
  "op": "invoke-virtual",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "调用普通实例方法，支持虚分派。参数为寄存器列表，返回值用 move-result* 获取。",
  "ex": "invoke-virtual {v0, v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-virtual/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "调用普通实例方法，支持虚分派。参数使用连续寄存器范围 vCCCC..vNNNN。",
  "ex": "invoke-virtual/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-super",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "调用父类的实例方法。参数为寄存器列表，返回值用 move-result* 获取。",
  "ex": "invoke-super {v0, v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-super/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "调用父类的实例方法。参数使用连续寄存器范围 vCCCC..vNNNN。",
  "ex": "invoke-super/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-direct",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "调用构造函数或私有实例方法。参数为寄存器列表，返回值用 move-result* 获取。",
  "ex": "invoke-direct {v0, v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-direct/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "调用构造函数或私有实例方法。参数使用连续寄存器范围 vCCCC..vNNNN。",
  "ex": "invoke-direct/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-static",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "调用静态方法。参数为寄存器列表，返回值用 move-result* 获取。",
  "ex": "invoke-static {v0, v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-static/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "调用静态方法。参数使用连续寄存器范围 vCCCC..vNNNN。",
  "ex": "invoke-static/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-interface",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "通过接口调用实例方法。参数为寄存器列表，返回值用 move-result* 获取。",
  "ex": "invoke-interface {v0, v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-interface/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "通过接口调用实例方法。参数使用连续寄存器范围 vCCCC..vNNNN。",
  "ex": "invoke-interface/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V"
 },
 {
  "op": "invoke-custom",
  "fmt": "35c",
  "cat": "方法/对象",
  "desc": "调用动态调用点（invokedynamic），参数为调用点及参数寄存器。",
  "ex": "invoke-custom {v0}, Lcall_site_0(\"name\", (I)V)Ljava/lang/Object;"
 },
 {
  "op": "invoke-custom/range",
  "fmt": "3rc",
  "cat": "方法/对象",
  "desc": "使用连续寄存器范围调用动态调用点。",
  "ex": "invoke-custom/range {v0 .. v2}, Lcall_site_0(\"name\", (I)V)Ljava/lang/Object;"
 },
 {
  "op": "invoke-polymorphic",
  "fmt": "45cc",
  "cat": "方法/对象",
  "desc": "调用 MethodHandle 的多态签名方法；示例末尾的 (I)V 是实际调用签名。",
  "ex": "invoke-polymorphic {v0, v1}, Ljava/lang/invoke/MethodHandle;->invokeExact([Ljava/lang/Object;)Ljava/lang/Object;, (I)V"
 },
 {
  "op": "invoke-polymorphic/range",
  "fmt": "4rcc",
  "cat": "方法/对象",
  "desc": "使用连续寄存器范围调用 MethodHandle 多态签名方法。",
  "ex": "invoke-polymorphic/range {v0 .. v1}, Ljava/lang/invoke/MethodHandle;->invokeExact([Ljava/lang/Object;)Ljava/lang/Object;, (I)V"
 },
 {
  "op": "return-void",
  "fmt": "10x",
  "cat": "返回/异常",
  "desc": "从 void 方法返回。",
  "ex": "return-void"
 },
 {
  "op": "return",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "返回 32 位整数或引用。",
  "ex": "return v0"
 },
 {
  "op": "return-wide",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "返回 64 位值，读取 vAA/vAA+1。",
  "ex": "return-wide v0"
 },
 {
  "op": "return-object",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "返回对象引用。",
  "ex": "return-object v0"
 },
 {
  "op": "monitor-enter",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "获取 vAA 对象监视器，必须与 monitor-exit 配对。",
  "ex": "monitor-enter v0"
 },
 {
  "op": "monitor-exit",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "释放 vAA 对象监视器，必须与 monitor-enter 配对。",
  "ex": "monitor-exit v0"
 },
 {
  "op": "throw",
  "fmt": "11x",
  "cat": "返回/异常",
  "desc": "抛出 vAA 引用的异常对象。",
  "ex": "throw v0"
 },
 {
  "op": "add-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "加法（2addr 形式）：vA = vA + vB；结果写回目标寄存器。",
  "ex": "add-int/2addr v0, v1"
 },
 {
  "op": "sub-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "减法（2addr 形式）：vA = vA - vB；结果写回目标寄存器。",
  "ex": "sub-int/2addr v0, v1"
 },
 {
  "op": "mul-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "乘法（2addr 形式）：vA = vA * vB；结果写回目标寄存器。",
  "ex": "mul-int/2addr v0, v1"
 },
 {
  "op": "div-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "除法（2addr 形式）：vA = vA / vB；除数为零时抛出 ArithmeticException。",
  "ex": "div-int/2addr v0, v1"
 },
 {
  "op": "rem-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "取余（2addr 形式）：vA = vA % vB；除数为零时抛出 ArithmeticException。",
  "ex": "rem-int/2addr v0, v1"
 },
 {
  "op": "and-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位与（2addr 形式）：vA = vA & vB；结果写回目标寄存器。",
  "ex": "and-int/2addr v0, v1"
 },
 {
  "op": "or-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位或（2addr 形式）：vA = vA | vB；结果写回目标寄存器。",
  "ex": "or-int/2addr v0, v1"
 },
 {
  "op": "xor-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位异或（2addr 形式）：vA = vA ^ vB；结果写回目标寄存器。",
  "ex": "xor-int/2addr v0, v1"
 },
 {
  "op": "shl-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "左移（2addr 形式）：vA = vA << vB；移位量分别取低 5 位或 6 位。",
  "ex": "shl-int/2addr v0, v1"
 },
 {
  "op": "shr-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "算术右移（2addr 形式）：vA = vA >> vB；移位量分别取低 5 位或 6 位。",
  "ex": "shr-int/2addr v0, v1"
 },
 {
  "op": "ushr-int/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "逻辑右移（2addr 形式）：vA = vA >>> vB；移位量分别取低 5 位或 6 位。",
  "ex": "ushr-int/2addr v0, v1"
 },
 {
  "op": "add-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "加法（2addr 形式）：vA = vA + vB；结果写回目标寄存器。",
  "ex": "add-long/2addr v0, v1"
 },
 {
  "op": "sub-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "减法（2addr 形式）：vA = vA - vB；结果写回目标寄存器。",
  "ex": "sub-long/2addr v0, v1"
 },
 {
  "op": "mul-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "乘法（2addr 形式）：vA = vA * vB；结果写回目标寄存器。",
  "ex": "mul-long/2addr v0, v1"
 },
 {
  "op": "div-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "除法（2addr 形式）：vA = vA / vB；除数为零时抛出 ArithmeticException。",
  "ex": "div-long/2addr v0, v1"
 },
 {
  "op": "rem-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "取余（2addr 形式）：vA = vA % vB；除数为零时抛出 ArithmeticException。",
  "ex": "rem-long/2addr v0, v1"
 },
 {
  "op": "and-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位与（2addr 形式）：vA = vA & vB；结果写回目标寄存器。",
  "ex": "and-long/2addr v0, v1"
 },
 {
  "op": "or-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位或（2addr 形式）：vA = vA | vB；结果写回目标寄存器。",
  "ex": "or-long/2addr v0, v1"
 },
 {
  "op": "xor-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "按位异或（2addr 形式）：vA = vA ^ vB；结果写回目标寄存器。",
  "ex": "xor-long/2addr v0, v1"
 },
 {
  "op": "shl-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "左移（2addr 形式）：vA = vA << vB；移位量分别取低 5 位或 6 位。",
  "ex": "shl-long/2addr v0, v1"
 },
 {
  "op": "shr-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "算术右移（2addr 形式）：vA = vA >> vB；移位量分别取低 5 位或 6 位。",
  "ex": "shr-long/2addr v0, v1"
 },
 {
  "op": "ushr-long/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "逻辑右移（2addr 形式）：vA = vA >>> vB；移位量分别取低 5 位或 6 位。",
  "ex": "ushr-long/2addr v0, v1"
 },
 {
  "op": "add-float/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "加法（2addr 形式）：vA = vA + vB；结果写回目标寄存器。",
  "ex": "add-float/2addr v0, v1"
 },
 {
  "op": "sub-float/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "减法（2addr 形式）：vA = vA - vB；结果写回目标寄存器。",
  "ex": "sub-float/2addr v0, v1"
 },
 {
  "op": "mul-float/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "乘法（2addr 形式）：vA = vA * vB；结果写回目标寄存器。",
  "ex": "mul-float/2addr v0, v1"
 },
 {
  "op": "div-float/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "除法（2addr 形式）：vA = vA / vB；除数为零时抛出 ArithmeticException。",
  "ex": "div-float/2addr v0, v1"
 },
 {
  "op": "rem-float/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "取余（2addr 形式）：vA = vA % vB；除数为零时抛出 ArithmeticException。",
  "ex": "rem-float/2addr v0, v1"
 },
 {
  "op": "add-double/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "加法（2addr 形式）：vA = vA + vB；结果写回目标寄存器。",
  "ex": "add-double/2addr v0, v1"
 },
 {
  "op": "sub-double/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "减法（2addr 形式）：vA = vA - vB；结果写回目标寄存器。",
  "ex": "sub-double/2addr v0, v1"
 },
 {
  "op": "mul-double/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "乘法（2addr 形式）：vA = vA * vB；结果写回目标寄存器。",
  "ex": "mul-double/2addr v0, v1"
 },
 {
  "op": "div-double/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "除法（2addr 形式）：vA = vA / vB；除数为零时抛出 ArithmeticException。",
  "ex": "div-double/2addr v0, v1"
 },
 {
  "op": "rem-double/2addr",
  "fmt": "12x",
  "cat": "运算/转换",
  "desc": "取余（2addr 形式）：vA = vA % vB；除数为零时抛出 ArithmeticException。",
  "ex": "rem-double/2addr v0, v1"
 },
 {
  "op": "iget-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取实例字段；普通 Smali 使用 iget。",
  "ex": "iget-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "iput-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入实例字段；普通 Smali 使用 iput。",
  "ex": "iput-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sget-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取静态字段；普通 Smali 使用 sget。",
  "ex": "sget-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sput-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入静态字段；普通 Smali 使用 sput。",
  "ex": "sput-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "iget-object-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取实例字段；普通 Smali 使用 iget-object。",
  "ex": "iget-object-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "iget-wide-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取实例字段；普通 Smali 使用 iget-wide。",
  "ex": "iget-wide-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "iput-wide-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入实例字段；普通 Smali 使用 iput-wide。",
  "ex": "iput-wide-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sget-wide-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取静态字段；普通 Smali 使用 sget-wide。",
  "ex": "sget-wide-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sput-wide-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入静态字段；普通 Smali 使用 sput-wide。",
  "ex": "sput-wide-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "throw-verification-error",
  "fmt": "20bc",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：抛出校验错误；普通 Smali 不应生成。",
  "ex": "throw-verification-error v0, kind@0",
  "legacy": true
 },
 {
  "op": "execute-inline",
  "fmt": "35mi",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：请求运行时内联执行指定方法。",
  "ex": "execute-inline {v0, v1}, Ljava/lang/Math;->abs(I)I",
  "legacy": true
 },
 {
  "op": "execute-inline/range",
  "fmt": "3rmi",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用连续寄存器范围请求运行时内联执行指定方法。",
  "ex": "execute-inline/range {v0 .. v1}, Ljava/lang/Math;->abs(I)I",
  "legacy": true
 },
 {
  "op": "invoke-direct-empty",
  "fmt": "35c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：调用空实现的直接方法。",
  "ex": "invoke-direct-empty {v0}, Ljava/lang/Object;-><init>()V",
  "legacy": true
 },
 {
  "op": "invoke-object-init/range",
  "fmt": "3rc",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用连续寄存器范围调用对象构造函数。",
  "ex": "invoke-object-init/range {v0 .. v0}, Ljava/lang/Object;-><init>()V",
  "legacy": true
 },
 {
  "op": "return-void-barrier",
  "fmt": "10x",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带内存屏障语义的 void 返回指令。",
  "ex": "return-void-barrier",
  "legacy": true
 },
 {
  "op": "return-void-no-barrier",
  "fmt": "10x",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：不带内存屏障的 void 返回指令。",
  "ex": "return-void-no-barrier",
  "legacy": true
 },
 {
  "op": "iget-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-wide-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-wide-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-object-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-object-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-wide-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-wide-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-object-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-object-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-boolean-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-boolean-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-byte-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-byte-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-char-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-char-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iput-short-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移写入实例字段。",
  "ex": "iput-short-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-boolean-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-boolean-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-byte-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-byte-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-char-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-char-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "iget-short-quick",
  "fmt": "22cs",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析偏移读取实例字段。",
  "ex": "iget-short-quick v0, v1, #0x8",
  "legacy": true
 },
 {
  "op": "invoke-virtual-quick",
  "fmt": "35ms",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析索引调用虚方法。",
  "ex": "invoke-virtual-quick {v0, v1}, Lcom/example/Foo;->bar(I)V",
  "legacy": true
 },
 {
  "op": "invoke-virtual-quick/range",
  "fmt": "3rms",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析索引调用虚方法。",
  "ex": "invoke-virtual-quick/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V",
  "legacy": true
 },
 {
  "op": "invoke-super-quick",
  "fmt": "35ms",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析索引调用父类。",
  "ex": "invoke-super-quick {v0, v1}, Lcom/example/Foo;->bar(I)V",
  "legacy": true
 },
 {
  "op": "invoke-super-quick/range",
  "fmt": "3rms",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：使用已解析索引调用父类。",
  "ex": "invoke-super-quick/range {v0 .. v1}, Lcom/example/Foo;->bar(I)V",
  "legacy": true
 },
 {
  "op": "iput-object-volatile",
  "fmt": "22c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入实例字段；普通 Smali 使用 iput-object。",
  "ex": "iput-object-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sget-object-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义读取静态字段；普通 Smali 使用 sget-object。",
  "ex": "sget-object-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "sput-object-volatile",
  "fmt": "21c",
  "cat": "负载/旧版",
  "desc": "ODEX 专用：带 volatile 语义写入静态字段；普通 Smali 使用 sput-object。",
  "ex": "sput-object-volatile v0, Lcom/example/Foo;->value:I",
  "legacy": true
 },
 {
  "op": "packed-switch-payload",
  "fmt": "PackedSwitchPayload",
  "cat": "负载/旧版",
  "desc": "负载格式，不是可执行指令；由 .packed-switch 指令生成。",
  "ex": ".packed-switch 0x0",
  "legacy": true
 },
 {
  "op": "sparse-switch-payload",
  "fmt": "SparseSwitchPayload",
  "cat": "负载/旧版",
  "desc": "负载格式，不是可执行指令；由 .sparse-switch 指令生成。",
  "ex": ".sparse-switch",
  "legacy": true
 },
 {
  "op": "array-payload",
  "fmt": "ArrayPayload",
  "cat": "负载/旧版",
  "desc": "负载格式，不是可执行指令；由 .array-data 指令生成。",
  "ex": ".array-data 4",
  "legacy": true
 }
];
function smaliReferencePage(){
 const body=toolPage('Smali 指令查询','JADX/dexlib2 指令表 · 格式、说明与示例');
 body.classList.add('smaliRef');
 let query='',category='全部';
 body.innerHTML=`<div class="searchline"><input id="smaliSearch" type="search" placeholder="搜索 opcode、格式或说明，例如 invoke-virtual" aria-label="搜索 Smali 指令" autocomplete="off" spellcheck="false"><button id="smaliClear" aria-label="清空搜索">清除</button></div><div class="smaliFilters" role="tablist" aria-label="指令分类"></div><div class="smaliCount" id="smaliCount"></div><div class="smaliRows" id="smaliRows"></div>`;
 const input=body.querySelector('#smaliSearch'),filters=body.querySelector('.smaliFilters'),rowsEl=body.querySelector('#smaliRows'),countEl=body.querySelector('#smaliCount');
 for(const name of SMALI_CATEGORIES){const b=document.createElement('button');b.className='smaliChip';b.textContent=name;b.setAttribute('role','tab');b.onclick=()=>{category=name;sync();};filters.append(b);}
 const rows=()=>{const terms=query.trim().toLowerCase().split(/\s+/).filter(Boolean);return SMALI_OPS.map((item,index)=>({item,index})).filter(({item})=>{if(category==='常用'&&!SMALI_COMMON.has(item.op))return false;if(category==='负载/旧版'){if(!item.legacy)return false;}else if(category!=='全部'&&category!=='常用'&&(item.legacy||item.cat!==category))return false;const hay=(item.op+' '+item.fmt+' '+item.cat+' '+item.desc+' '+item.ex).toLowerCase();return terms.every(term=>hay.includes(term));});};
 const sync=()=>{for(const b of filters.children){const on=b.textContent===category;b.classList.toggle('active',on);b.setAttribute('aria-selected',String(on));}const found=rows();const legacy=SMALI_OPS.filter(item=>item.legacy).length;countEl.textContent=(query||category!=='全部'?'找到 ':'共 ')+found.length+' 条指令'+(query||category!=='全部'?'':' · 含 '+legacy+' 条 ODEX/负载 · 轻点查看示例');rowsEl.innerHTML=found.length?found.map(({item,index})=>`<button class="smaliRow" data-smali="${index}"><span class="smaliLine"><code>${esc(item.op)}</code><span class="smaliFmt">${esc(item.fmt)}</span></span><span class="smaliDesc">${esc(item.desc)}</span><span class="smaliCat">${esc(item.cat)}</span></button>`).join(''):'<div class="empty">没有匹配的指令。试试 invoke、const、iget 或字段名。</div>';for(const b of rowsEl.querySelectorAll('[data-smali]'))b.onclick=()=>act(()=>smaliDetail(SMALI_OPS[Number(b.dataset.smali)]));};
 input.oninput=event=>{query=event.target.value;sync();};
 body.querySelector('#smaliClear').onclick=()=>{query='';input.value='';input.focus();sync();};
 sync();
}
function smaliDetail(item){
 const tags=(item.legacy?'<span>ODEX/旧版</span>':'')+(SMALI_COMMON.has(item.op)?'<span>常用</span>':'')+`<span>格式 ${esc(item.fmt)}</span><span>${esc(item.cat)}</span>`;
 dialog(item.op,`<p>${esc(item.desc)}</p><div class="smaliDetailMeta">${tags}</div><p class="smaliExampleLabel">示例</p><pre class="smaliExample">${esc(item.ex)}</pre><p class="smaliHint">${SMALI_FORMAT_HELP}</p>`,[['复制示例',()=>rpc('clipboard',{text:item.ex}).then(()=>toast('示例已复制'))],['关闭',closeModal]]);
}
async function translatePage(){
 const body=toolPage('翻译服务','腾讯云 / 百度 / 阿里云 / 火山引擎 / 微软 / AI 大模型');
 await loadTranslateConfig();
 const c=translateConfig||{},provider=c.provider||'tencent';
 const langs=(translateLanguages||[]);
 const render=()=>{
  const p=(translateConfig&&translateConfig.provider)||'tencent';
  const fields=translateFields(p).map(([key,label,type,hint])=>{
   const value=(translateConfig&&translateConfig[key])||'';
   if(type==='textarea')return `<label>${esc(label)}</label><textarea class="toolinput" id="tf_${key}" rows="3" spellcheck="false">${esc(value)}</textarea><p class="muted">${esc(hint)}</p>`;
   return `<label>${esc(label)}</label><input class="toolinput" id="tf_${key}" type="${type==='password'?'password':'text'}" value="${esc(value)}" autocomplete="off" spellcheck="false"><p class="muted">${esc(hint)}</p>`;
  }).join('');
  body.innerHTML=`<p>翻译请求从本机直接发出，密钥保存在应用私有存储中。</p>
   <label>翻译服务</label>
   <div class="searchline"><select id="tfProvider" style="flex:1;background:transparent;color:var(--text);border:0;padding:10px 0">${TRANSLATE_PROVIDERS.map(x=>`<option value="${x[0]}" ${x[0]===p?'selected':''}>${esc(x[1])}</option>`).join('')}</select></div>
   <label>目标语言</label>
   <div class="searchline"><select id="tfTarget" style="flex:1;background:transparent;color:var(--text);border:0;padding:10px 0">${langs.map(l=>`<option value="${esc(l.key)}" ${l.key===(translateConfig.target||'en')?'selected':''}>${esc(l.name)}</option>`).join('')}</select></div>
   ${fields}
   <button class="primary" id="tfSave">保存</button>
   <button class="primary" id="tfTest">测试翻译</button>`;
  body.querySelector('#tfProvider').onchange=async e=>{translateConfig.provider=e.target.value;render();};
  body.querySelector('#tfSave').onclick=()=>act(async()=>{
   const payload={op:'set',provider:body.querySelector('#tfProvider').value,target:body.querySelector('#tfTarget').value};
   for(const [key]of translateFields(payload.provider)){const el=body.querySelector('#tf_'+key);if(el)payload[key]=el.value;}
   const saved=await rpc('translateConfig',payload);
   translateConfig=saved.config||saved;render();toast('翻译服务已保存');
  });
  body.querySelector('#tfTest').onclick=()=>act(async()=>{
   const provider=body.querySelector('#tfProvider').value,target=body.querySelector('#tfTarget').value;
   const payload={op:'set',provider:provider,target:target};
   for(const [key]of translateFields(provider)){const el=body.querySelector('#tf_'+key);if(el)payload[key]=el.value;}
   await rpc('translateConfig',payload);
   await loadTranslateConfig();
   const result=await translateText('Hello world','');
   alertBox('测试翻译',result||'（返回为空）');
  });
 };
 render();
}
function translateButton(onResult){
 const b=document.createElement('button');
 b.textContent='翻译';
 b.onclick=()=>act(async()=>{
  const input=b.dataset.input?$(b.dataset.input):null;
  const text=input&&input.value!==undefined?input.value:(b.dataset.text||'');
  if(!text||!text.trim())return toast('没有可翻译的文本');
  b.disabled=true;b.textContent='翻译中…';
  try{
   const result=await translateText(text,b.dataset.to||'');
   if(result&&onResult)onResult(result);
   else toast(result||'（返回为空）');
  }finally{b.disabled=false;b.textContent='翻译';}
 });
 return b;
}

function moreTools(){options('更多工具',[{icon:'apk',label:'检查本应用的 APK',run:()=>showApk({path:state.info.ownApk})},{icon:'settings',label:'设置',run:preferences},{icon:'phone',label:'存储权限',run:()=>rpc('permission')},{icon:'info',label:'用户指南',run:guide},{icon:'info',label:'关于赛博中枢管家',run:about}]);}
function guide(){alertBox('用户指南','文件：轻点打开；长按打开文件操作菜单，「打开方式…」提供内置功能（文本、压缩包、图片、十六进制、音视频、XML / DEX / ARSC / APK / APKS、脚本，不匹配的项置灰）与外部应用选择（系统建议 / 文本 / 图片 / 视频 / 音频按类型筛选，可搜索）；菜单中的菜单中的「工具」提供文件分割/合并、GPG 加密/解密、交换文件名、创建软链接、创建快捷方式与导出文件列表（不满足条件的项会置灰）。使用 + 新建或粘贴。轻点图片/视频/音频直接用内置播放器打开：视频支持长按 2×/3× 倍速快进、左半屏上下滑动调亮度、右半屏上下滑动调音量；文件夹菜单中的「照片浏览」可按网格浏览当前文件夹的图片。底部按键栏上滑可打开「最近 / 书签」快速访问面板（过滤、分组、高度可调）；轻点顶部路径栏弹出「跳转」对话框（粘贴/取消/确定）。长按菜单为：复制、移动、删除、重命名、工具、压缩、属性、分享、打开方式、添加书签——带 • 的「复制/移动」长按可触发单窗口操作。\n\nGPG 加密生成标准 OpenPGP 文件：口令方式直接输入口令；公钥方式填写接收方公钥（.asc/.gpg）。输出 .gpg 或 .asc，可用桌面 GnuPG 或本应用解密。\n\nAPK → 查看 → classes.dex。打开一个类；编辑 Smali 或使用 查看 Java。Java 可直接编辑。保存 会将代码编译进临时 DEX 工程。返回 同样会编译未保存的代码；出现错误时编辑器保持打开。\n\n编辑器/DEX 菜单 → 保存并签名 APK 会写回源文件，保留备份，并校验 V1/V2/V3 后再提供 安装。保存工程 只写入未签名的更改。导出 生成一份独立副本。\n\n并非每个反编译出的 Java 文件都是有效源码。请修正编译错误或改用 Smali 编辑。包名/类名请勿修改。\n\n捏合可调整代码大小。文字大小 / 缩放 提供按钮、滑块和 重置。查找替换 直接编辑当前缓冲区，并支持 撤销。\n\n签名使用公开的开发证书。Android 权限与安装规则依然适用。');}
function about(){closeDrawer();options('赛博中枢管家 · 0.1.32',[{icon:'phone',label:'用户信息（本地）',run:async()=>{const name=await ask('本地用户名',storage.get('cyber-name')||'赛博中枢');if(name!==null){storage.set('cyber-name',name);toast('资料已更新');}}},{icon:'info',label:'用户指南',run:guide},{icon:'history',label:'版本说明',run:()=>alertBox('版本 0.1.32','0.1.32：修复 DEX 翻译模式退出导航——点「退出」或系统返回键现在直接回到文件列表，不再落回 Dex 编辑器 Plus；工程若有未保存修改仍会先确认。\n\n0.1.31：DEX 翻译模式底部五键改为选择操作——全选、反选、多选（＋，开启后点原文行选择/取消，图标变 ×）、翻译（只翻译所选未译词条，未选择时提示先选择）、连选（开启后点第一项再点第二项，自动选中两者之间全部条目）；选中行左侧蓝色竖条高亮，标题栏显示已选数量；原「检查/保存到字典/添加词条/过滤」仍在右上 ⋮ 菜单中。\n\n0.1.30：翻译服务新增阿里云翻译与火山引擎翻译两家机器翻译——阿里云填 AccessKey ID + AccessKey Secret（通用版 TranslateGeneral），火山引擎填 AccessKey ID + Secret Access Key（文本翻译 Translate，区域 cn-north-1）；配置页服务下拉新增两项，保存后可用「测试翻译」验证。\n\n0.1.29：修复腾讯云翻译配置——项目 ID 改为可留空（默认项目 0，不再强制填写），地域留空自动使用 ap-guangzhou（Region 为接口必填参数，此前留空会导致报 MissingParameter）；已用真实 SecretId/SecretKey 实测接口连通（签名通过，密钥有效）。\n\n0.1.28：DEX 翻译模式底部五个快捷键按 MT 参考截图重排——选择(检查)、复制(保存到字典)、添加词条、翻译(翻译全部)、列表(过滤)；语言选择移入右上 ⋮ 菜单。\n\n0.1.27：DEX 翻译模式按 MT 参考截图还原双栏布局——左栏白底显示原文，右栏整列灰底为译文编辑区（直接点按输入），行间横向分隔线、无表头、行高充足，两列严格等宽；深浅主题均保持对比。\n\n0.1.26：DEX 翻译模式改为线条画的表格——真正的两列表格（原文 / 译文），顶部表头行带下划线，每行横向分隔线，译文列虚线占位「（未翻译）」，聚焦时虚线变蓝；表格等宽两列、行高紧凑，与参考截图一致；修复此前列表行过高、译文输入框不可见的问题。\n\n0.1.25：修复翻译服务并调整入口——修复翻译服务页「目标语言」下拉为空、DEX/ARSC 翻译页语言列表失效的问题（后端返回的语言数据解析方式错误）；修复「测试翻译」误把 auto 当目标语言导致必定报「不支持的目标语言」的问题，现在使用已保存的目标语言；入口更直接：XML 翻译弹窗在未配置翻译服务时新增「前往配置翻译服务」按钮直达设置页，DEX 翻译模式右上 ⋮ 菜单新增「翻译服务」项，可随时跳转配置、关闭工具页即返回原页面。\n\nDEX 翻译模式改版（参考截图）：点 DEX 文件 → 翻译模式进入专用双语对照页——标题「翻译模式 (已译/总数)」，左侧原文、右侧译文输入框直接编辑；右上 ⋮ 菜单八项：保存、保存到字典、字典管理、过滤、替换、检查、语言、退出；底部功能键：过滤、添加词条、语言、保存到字典、检查；支持逐条/全部自动翻译（翻译服务）、字典自动回填相同原文、译文写回工程（保存工程后生效）。\n\n0.1.23：点击压缩包直接进入浏览：zip/7z/tar/tar.gz/tar.xz/tar.zst/tar.bz2/tar.lz4/gz 均可像文件夹一样点进去逐级查看（与截图二一致），不再直接弹「解压到…」；压缩包内文件可打开（文本/图片等会解出临时副本），长按菜单仍保留「解压到…」；分卷（.001）点击仍是解压对话框。原生侧新增通用归档目录列出与条目读取（zip4j + Commons Compress），此前仅 zip 支持浏览。\n\n0.1.22：视频播放器整体移植自 MP-Manager（Apache-2.0）：原生 MediaPlayer + SurfaceView 播放器界面，点视频文件直接进入；双击左 1/3 快退 10 秒、右 1/3 快进 10 秒、中间暂停/继续；水平拖动快进快退（带时间提示）；倍速调节（0.25×–4×）、音量滑杆、AB 循环（设 A/设 B/清除）、屏幕锁定、画幅切换（适应/填充/16:9/4:3 循环）、播放队列（自动载入同目录视频，按文件名排序，可从队列切换）；断点续播（暂停时记忆位置，重进接着播）；支持 srt/vtt/ass/ssa/sub 外挂字幕自动加载与手动选择，内嵌字幕轨道也可显示。文件列表点视频现在走此原生播放器，不再使用 WebView 内置播放器（音频仍用内置播放器）。\n\n0.1.21：修复本地视频打开卡在「正在打开…」的问题：部分 WebView 或编码下 loadedmetadata/error 事件都不触发，现在 canplay/playing 也会关闭加载提示；12 秒未就绪显示「用系统播放器打开」按钮、30 秒自动回退系统播放器，永不卡死；原生 /media/ 流媒体补上 HEAD 请求处理（此前返回 404 可能导致视频栈一直等待）。\n\n0.1.20：文件列表媒体预览：照片与视频的图标直接显示缩略图预览（照片解码缩放、视频提取首帧），滚动到可视区域才加载，带缓存不重复解码；视频缩略图右下角带播放角标；无法解码的格式（如部分 RAW）自动回退普通图标。\n\n0.1.19：快速访问面板改版（参考截图）：「最近 / 书签」标签移至左上角、激活标签带蓝色下划线，右上角保留菜单；移除顶部拖动把手；条目图标改为大号圆角方块样式；点击面板上方空白区域或底部新增的下拉箭头即可直接关闭面板。\n\n0.1.18：快速访问面板修复：面板在 0.1.17 中错误地收缩为内容宽度（两侧留空），现恢复全宽贴底；同时严格限制面板不超过屏幕宽度（上限 560px），超长文件名与路径显示省略号，修复超长路径把面板撑出屏幕、左侧文字被裁的问题。\n\n0.1.17：快速访问面板改为从底部弹出（此前错误地贴着屏幕顶部）。\n\n底栏上滑与顶部路径（参考 MP-Manager）：上滑超过阈值即确认为滑动并吞掉按钮合成点击，修复上滑误触「上级目录」导致顶部路径跳变；顶部路径过长时改为跑马灯滚动完整展示，不再截断为 …xxx；触碰另一窗格立即完整刷新路径与统计。\n\n修复：底栏五个快捷键上滑打开快速访问（补上 touch-action，手势不再被页面滚动吞掉）；内置视频/音频播放器改经同源 HTTPS（/media/）供流，修复因混合内容策略导致视频直接回退系统播放器的问题；左右切换文件窗格时顶部路径即时刷新；播放器内禁用长按文本选择。\n\n内置媒体播放器：轻点图片/视频/音频文件直接在应用内打开；图片查看器移除了「用其他应用打开」按钮；视频播放器支持长按 2×/3× 倍速快进、右半屏上下滑动调节音量、左半屏上下滑动调节亮度（音量与亮度会记忆）、拖动进度条跳转，轻点显示/隐藏控制；音频同样内置播放（轻点播放/暂停）。媒体经仅监听 127.0.0.1 的本地流媒体服务（支持 HTTP Range 断点续流）送入 WebView；无法解码的冷门格式自动回退系统播放器，仍失败可用「打开方式」。支持格式大幅扩充：图片含 ico/tiff/dng 与各品牌 RAW，视频含 mkv/ts/m2ts/flv/rmvb/asf/vob 等 30 余种，音频含 flac/opus/ape/ac3/dts/amr/mka 等 30 余种。\n\n长按菜单重构：与参考截图一致——复制、移动、删除、重命名、工具、压缩、属性、分享、打开方式、添加书签 十项竖排列表；移除「预览图片/播放视频/播放音频/用其他应用打开」（轻点文件即可预览）；带 • 的「复制/移动」可长按触发单窗口操作（先入队再选目标粘贴），提示语同步更新；归档文件保留「解压到…」。\n\n快速访问：底部五个快捷键上滑打开「最近 / 书签」面板；最近记录可关闭/清空，书签支持分组；面板菜单提供 过滤、添加分组、调整高度（30–90% 实时预览）、手势说明、开启记录、清空记录；轻点条目直接打开。\n\n跳转：轻点顶部路径栏（或文件菜单 → 跳转到路径）弹出「跳转」对话框：路径输入框 + 粘贴（读取系统剪贴板）/取消/确定。\n\n——以下为此前版本更新——\n\n打开方式重构：长按文件 →「打开方式…」改为与参考截图一致的 MT 风格对话框——顶部「打开方式…」标题带搜索（放大镜）与帮助（问号）按钮；下方为「内置 / 系统建议 / 文本 / 图片 / 视频 / 音频」标签页（按文件类型动态显示媒体标签，远程存储仅显示「内置」）。「内置」页为三列彩色圆角图标网格：编辑文本、浏览压缩包、查看图片、十六进制、播放音乐、播放视频、XML 功能、DEX 功能、ARSC 功能、APK 信息、APKS 功能、Linux 脚本（.sh/.bash 自动在终端执行），与当前文件类型不匹配的项自动置灰。「系统建议 / 文本 / 图片 / 视频 / 音频」页通过系统 PackageManager 查询可处理该文件（或对应 MIME 类型）的外部应用，显示真实应用图标（PNG）与名称，轻点即用所选应用显式打开（经 FileProvider 授权，压缩包内的文件会先解出临时副本）；右上角放大镜可搜索当前页条目，右下角「关闭」按钮关闭对话框。新增配套的「十六进制查看器」工具页（分页浏览、首页 / 上一页 / 下一页 / 末页、支持 0x 十六进制或十进制跳转偏移）。\n\n长按工具箱：长按文件 →「工具」新增 8 项文件工具：文件分割（自定义字节分卷，输出 原名.part01、.part02…）、文件合并（自动收集同文件夹全部同名分卷按序合并）、GPG 加密 / GPG 解密（标准 OpenPGP，AES-256 + 完整性保护；支持口令（对称）与公钥两种方式，输出 .gpg 或 ASCII 装甲 .asc，桌面 GnuPG（gpg -d）可直接解密，桌面 GnuPG 加密的文件也可在本应用解密）、交换文件名（同文件夹内两个文件互换名称）、创建软链接（可放入另一窗格文件夹）、创建快捷方式（固定到桌面，轻点直达本应用内该文件/文件夹）、导出文件列表（当前文件夹清单导出为文本）。不满足条件的工具项按当前选中数量自动置灰，与截图中的逻辑一致。原「计算 SHA-256 / 复制路径」入口仍可在「属性」与长按菜单中使用。\n\nWebDAV 修复：远程存储的 WebDAV 客户端改用 OkHttp 发送请求，修复 Android 的 HttpURLConnection 拒绝 PROPFIND、MKCOL、MOVE 等自定义方法而导致无法读取目录、新建文件夹和重命名的问题。已在 Koofr WebDAV 上完成连接、列目录、新建、上传、下载、重命名、删除和清理测试。\n\nSmali 指令查询：侧边栏「工具 → Smali 指令查询」提供 265 条 Dalvik/DEX 指令的离线检索（含 40 条 ODEX/负载项），指令名与格式依据 JADX 内置 dexlib2 Opcode 表补全。支持按 opcode、格式、说明和示例搜索，并可按常用、数据、运算、跳转、数组、字段、方法、返回、ODEX/负载等分类筛选。轻点指令可查看格式、说明和 Smali 示例，并可复制示例。\n\nAPK 提取：侧边栏「工具 → 已安装应用」点击应用后进入提取模式，只显示「提取 APK / 查看 / 安装」，不再显示 APK 签名、优化、共存等功能。提取支持选择目录和文件名。\n\n远程存储：侧边栏「远程存储」分组直接列出已保存的连接；打开后只替换当前一侧文件窗格，另一侧继续浏览本地文件。支持窗格内进入文件夹、上传、下载、新建文件夹、重命名和删除。\n\nRoot / Shizuku 授权：侧边栏「工具」新增授权入口，支持 su 授权弹窗申请与 Shizuku 授权状态管理。授权后浏览根目录或受限目录（如 /data）时，Root 经 su、Shizuku 经 shell 特权实际枚举内容，可看到并进入更多文件夹——与 MT 管理器一致。\n\n根目录修复：Android 10+ 禁止普通应用枚举根目录（SELinux 拒绝 readdir /），导致「无法读取此文件夹」。现回退到内置标准目录清单逐项探测存在性；未授权时 /data 等无权限项仍可见，能否进入取决于系统权限。\n\n屏幕取色：侧边栏「工具」新增屏幕取色——十字准星 + 实时放大镜，对准像素后轻点放大镜上的色值或坐标即复制；支持大小/范围/刷新速度/网格线设置（需授权显示在其他应用上层、通知与屏幕录制）。\n\nAPKS 转换：基座与配置分包合并为已签名、已校验的 APK。bundletool 变体分别选择；独立 APK 保留有效签名。APKS 本身保持不变。Play 资源包与不支持的分发类型会给出清晰的错误提示。\n\n性能与 Java 编辑更新：合成器捏合预览；输入时不再全缓冲区扫描；可复用的菜单动画；独立缩略图队列；可编辑 Java（含 ECJ/DEX 编译与诊断）；可撤销的查找替换；带备份的已校验保存并签名。此处不测量设备帧率。\n\n更早的交互更新：侧边栏溢出与边缘滑动；动画菜单、对话框、行反馈与包展开；编辑器侧选项；Java、Smali、XML 和文本中的捏合缩放（大小分别记忆）。打开侧边栏不会保存未保存的代码。\n\n此前的视频更新：成员/字符串导航；用法、子类与重写搜索；成员/代码/片段复制；行与寄存器工具；带 .bak 备份的保存并退出；还原替换；多选、对比与书签；9 种归档格式（含密码/分卷控制）。保留启动恢复与紧凑布局。\n\n功能：双窗格文件管理器；APK 浏览器、清单编辑器与签名；多 DEX、Smali 与 Java 编辑器；本地文件工具、终端、屏幕取色、Root/Shizuku 授权、FTP/FTPS 以及只读共享/MCP。\n\n为赛博中枢管家独立构建。不包含任何原应用代码。')},{icon:'layers',label:'开源致谢',run:()=>alertBox('开源致谢','JADX (Apache-2.0)\nsmali / baksmali / dexlib2 (BSD-3-Clause)\nCodeMirror 5 (MIT)\nPublic Sans (OFL-1.1) · U.S. Web Design System\nScreenColorPicker (MIT) · Ratul Hasan（屏幕取色组件）\nShizuku API (MIT) · Rikka（Root/Shizuku 授权组件）\nTermux terminal-emulator / terminal-view (GPLv3) · Termux（PTY 终端组件）\nJSch (BSD-3-Clause) · mwiede（SFTP 组件）\njCIFS-ng (LGPL-2.1) · AgNO3（SMB 组件）\nOkHttp (Apache-2.0) · Square（WebDAV HTTP 组件）\nBouncy Castle (MIT)（jCIFS-ng 依赖）\nBouncy Castle bcpg 1.70 (MIT)（GPG / OpenPGP 加解密组件）\nAndroid apksig (Apache-2.0)\nARSCLib 1.4.0 (Apache-2.0)\nECJ 4.6.1 (EPL-1.0) · AOSP dx (Apache-2.0) · ASM (BSD-3-Clause)\nApache Commons Net (Apache-2.0)\nGuava、Gson、Protobuf、ANTLR 与 SLF4J\n\n依赖版本与许可证说明包含在源码包中。')}]);}
window.goBack=async()=>{if(playerState){closePlayer();return;}if($('#quickLayer')&&$('#quickLayer').children.length){closeQuick();return;}if(photoState){closePhoto();return;}try{if($('#menuLayer').children.length){closeMenu();return;}if($('#modalLayer').children.length){closeModal();return;}if($('#drawerLayer').children.length){closeDrawer();return;}if($('#settingsLayer')){closeSettings();return;}if(state.screen==='editor'){if(!await canLeave())return;const back=state.editor.returnScreen,kind=state.editor.kind;if(state.disposeEditor)state.disposeEditor();state.cm=null;state.editor=null;$('#content').classList.remove('javaEditor');if(kind==='arsc'){state.screen='tool';await arscRender();return;}if(back==='dex')await renderDex();else await refresh();state.screen=back==='dex'?'dex':'files';if(back!=='dex')renderFiles();return;}if(state.screen==='tool'){if(state.dexTranslate){await dexTranslateExit();return;}if(state.previous==='dex')await renderDex();else{state.screen='files';await refresh();}return;}if(state.screen==='dex'){if(state.dex.tab!=='explorer'){state.dex.tab='explorer';await renderDex();}else await closeDex();return;}if(selectedFiles().length){clearSelection();return;}if(activePane().history.length)await backPane();else if(!activePane().archive&&['/',state.info.home,state.info.workspace].includes(activePane().path))await rpc('exit');else await up();}catch(e){alertBox('无法完成',e.message);}};
let initialized=false;window.onNativeResume=()=>{if(initialized)act(async()=>{const old=state.info.permission;state.info=await rpc('init');if(!old&&state.info.permission){state.panes[0].path=state.info.home;state.panes[1].path=state.info.home+'/下载';}if(state.screen==='files')await refresh();});};
function startPath(value){const home=state.info.permission?state.info.home:state.info.workspace;return {'首页':home,'工作区':state.info.workspace,'根目录':'/','下载':state.info.permission?home+'/下载':home}[value]||home;}
async function maybeRequestElevated(){try{if(!pref.startRoot&&!pref.startShell)return;const s=await rpc('elevated',{op:'status'});if(pref.startRoot&&!s.rootGranted&&s.rootAvailable)await rpc('elevated',{op:'requestRoot'});if(pref.startShell&&!s.rootGranted&&!s.shizukuGranted&&s.shizukuInstalled&&s.shizukuRunning)await rpc('elevated',{op:'requestShizuku'});}catch(e){}}
async function maybeAutoService(){const kind=pref.autoService==='MCP 服务'?'mcp':pref.autoService==='远程管理'?'remote':'';if(!kind)return;try{await rpc('service',{kind,op:'start',path:state.info.permission?state.info.home:state.info.workspace,protocol:remotePrefProtocol(),port:remotePrefPort(),code:remotePrefCode()});}catch(e){}}
async function maybeCleanRecycle(){const days={'7 天':7,'30 天':30,'90 天':90}[pref.autoClean]||0;if(!days)return;try{const rows=await rpc('recycleList'),cutoff=Date.now()-days*86400000;for(const r of rows)if((r.time||0)<cutoff)await rpc('purge',{id:r.id});}catch(e){}}
async function init(){try{state.info=await rpc('init');state.panes[0].path=startPath(pref.startLeft);state.panes[1].path=startPath(pref.startRight);await refresh();initialized=true;window.CyberBoot.ready();maybeRequestElevated();maybeAutoService();maybeCleanRecycle();}catch(e){window.CyberBoot.fail(e);}}
window.addEventListener('resize',()=>{const title=$('.pathTitle');if(title&&title.title)fitPath(title,title.title);if(state.screen==='editor'&&state.cm)state.cm.refresh();});
window.CyberBoot.onRuntimeError=message=>alertBox('界面错误',message);
window.cyberOpenPath=async path=>{try{await navigate(0,{path:String(path),archive:'',entry:''});}catch(e){toast('无法打开 '+(e.message||e));}};
init();
}());
