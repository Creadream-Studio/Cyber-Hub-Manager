const {chromium}=require('/home/z/my-project/node_modules/playwright-core');
const path=require('path'),fs=require('fs');
(async()=>{
const browser=await chromium.launch({headless:true,executablePath:'/home/z/my-project/toolchain/browser/chrome-headless-shell-linux64/chrome-headless-shell',args:['--no-sandbox','--disable-dev-shm-usage']});
const context=await browser.newContext({viewport:{width:398,height:768},deviceScaleFactor:2});
const page=await context.newPage();
page.on('pageerror',e=>console.log('PAGEERROR',e.message));
await page.addInitScript(()=>{
 const root='/storage/emulated/0',now=1792250000000;
 const files=[{name:'notes.txt',path:root+'/notes.txt',dir:false,size:200,time:now},{name:'photo.jpg',path:root+'/photo.jpg',dir:false,size:9000,time:now},{name:'sample.apk',path:root+'/sample.apk',dir:false,size:340000,time:now}];
 window.__calls=[];
 window.CyberNative={request(id,action,raw){const a=JSON.parse(raw);window.__calls.push({action,args:a});setTimeout(()=>{let data;try{
  switch(action){
   case'init':data={permission:true,home:root,workspace:root+'/CyberHub',version:'0.1.13',ownApk:root+'/self.apk'};break;
   case'disk':data={total:237888888888,free:170000000000};break;
   case'list':data=files.map(x=>({...x,total:237888888888,free:170000000000}));break;
   case'resolve':data=a.path;break;
   case'openWithApps':data={mime:'text/plain',suggest:[{label:'查看器',pkg:'com.t.viewer',cls:'com.t.viewer.V',icon:'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='}],text:[{label:'编辑器 A',pkg:'com.t.a',cls:'com.t.a.E',icon:''},{label:'编辑器 B',pkg:'com.t.b',cls:'com.t.b.E',icon:''},{label:'代码查看器',pkg:'com.t.c',cls:'com.t.c.E',icon:''}],image:[{label:'相册',pkg:'com.t.g',cls:'com.t.g.P',icon:''}],video:[],audio:[]};break;
   case'openWithLaunch':data=true;break;
   case'hexView':{const off=Math.max(0,a.offset||0),len=Math.min(a.length||4096,96);let hex='',ascii='';for(let i=0;i<len;i++){const v=(off+i)&0xff;hex+=v.toString(16).padStart(2,'0');ascii+=(v>=32&&v<127)?String.fromCharCode(v):'.';}data={path:a.path,name:'notes.txt',size:128,offset:off,length:len,hex,ascii};break;}
   case'terminalOpen':data=true;break;
   default:data=true;
  }window.receive({id,ok:true,data});}catch(e){window.receive({id,ok:false,error:e.message});}},5);}};
});
const assets=path.resolve(__dirname,'../app/src/main/assets');
await page.route('https://appassets.androidplatform.net/assets/**',async route=>{const name=new URL(route.request().url()).pathname.slice('/assets/'.length);let body=fs.readFileSync(path.join(assets,name));if(name==='app.js')body=Buffer.from(body.toString().replace('\ninit();','\nwindow.__test={state,pref,fileActions,openWithDialog};\ninit();'));await route.fulfill({body,contentType:name.endsWith('.js')?'application/javascript':name.endsWith('.css')?'text/css':'text/html'});});
await page.goto('https://appassets.androidplatform.net/assets/app.html');
await page.waitForSelector('.pane .file');
// 1) 打开方式：内置页
await page.evaluate(()=>window.__test.fileActions(window.__test.state.panes[0].items.find(x=>x.name==='notes.txt')));
await page.waitForSelector('.fileActionMenu');
await page.evaluate(()=>{const b=[...document.querySelectorAll('[role=menuitem]')].find(x=>x.textContent.trim()==='打开方式…');b.click();});
await page.waitForSelector('.owDialog');
await page.waitForTimeout(200);
await page.screenshot({path:'/tmp/render/ow-builtin.png'});
// 2) 系统建议页
await page.evaluate(()=>{const b=[...document.querySelectorAll('.owTabs .tab')].find(x=>x.textContent.trim()==='系统建议');b.click();});
await page.waitForTimeout(200);
await page.screenshot({path:'/tmp/render/ow-suggest.png'});
// 3) 搜索
await page.evaluate(()=>document.querySelector('.owHeadBtns [aria-label="搜索打开方式"]').click());
await page.waitForTimeout(150);
await page.screenshot({path:'/tmp/render/ow-search.png'});
// 4) 图片文件的标签
await page.evaluate(()=>{document.querySelector('.owDialog .actions button').click();});
await page.waitForTimeout(200);
await page.evaluate(()=>window.__test.openWithDialog(window.__test.state.panes[0].items.find(x=>x.name==='photo.jpg')));
await page.waitForSelector('.owDialog');
await page.waitForTimeout(200);
await page.screenshot({path:'/tmp/render/ow-image.png'});
// 5) 十六进制查看器
await page.evaluate(()=>{const b=[...document.querySelectorAll('.owItem')].find(x=>x.getAttribute('aria-label')==='十六进制');b.click();});
await page.waitForSelector('.hexBody');
await page.waitForTimeout(300);
await page.screenshot({path:'/tmp/render/hex.png'});
console.log('HEXTEXT>>>');const hexText=await page.evaluate(()=>document.querySelector('.hexBody').textContent);console.log('HEXROW1>>>'+hexText.split('\n')[0]+'<<<');console.log('HEXROW4>>>'+hexText.split('\n')[3]+'<<<');console.log('<<<HEXTEXT');console.log('SHOTS DONE');
await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
