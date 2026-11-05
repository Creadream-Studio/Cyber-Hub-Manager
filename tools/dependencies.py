#!/usr/bin/env python3
"""Fetch pinned Maven runtime dependencies without requiring Gradle/Maven."""
import urllib.request, xml.etree.ElementTree as ET, re, concurrent.futures, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; CACHE=ROOT/'.build/poms'; LIB=ROOT/'app/libs'
CACHE.mkdir(parents=True,exist_ok=True);LIB.mkdir(parents=True,exist_ok=True)
NS={'m':'http://maven.apache.org/POM/4.0.0'}
models={}
def download(g,a,v,ext):
 path=f"{g.replace('.','/')}/{a}/{v}/{a}-{v}.{ext}"
 bases=['https://dl.google.com/dl/android/maven2/'] if g.startswith('com.android') else ['https://repo.maven.apache.org/maven2/']
 for base in bases:
  try:return urllib.request.urlopen(base+path,timeout=90).read()
  except Exception as e:last=e
 raise last
def model(g,a,v):
 key=(g,a,v)
 if key in models:return models[key]
 path=CACHE/f'{g}.{a}-{v}.pom'
 if not path.exists():path.write_bytes(download(g,a,v,'pom'))
 root=ET.fromstring(path.read_bytes());props={};mgmt={}
 par=root.find('m:parent',NS)
 if par is not None:
  def pt(n):return par.findtext('m:'+n,namespaces=NS)
  pp,pm,_=model(pt('groupId'),pt('artifactId'),pt('version'));props.update(pp);mgmt.update(pm)
 props.update({'project.groupId':g,'project.artifactId':a,'project.version':v,'pom.version':v})
 pe=root.find('m:properties',NS)
 if pe is not None:
  for ch in pe:props[ch.tag.split('}')[-1]]=ch.text or ''
 def sub(s):
  for _ in range(12):
   newer=re.sub(r'\$\{([^}]+)\}',lambda m:props.get(m[1],m[0]),s or '')
   if newer==s:return newer
   s=newer
  return s
 def parse(dep):
  def t(n):return sub(dep.findtext('m:'+n,'',NS))
  dg,da=t('groupId'),t('artifactId');dv=t('version') or mgmt.get((dg,da),'')
  return dg,da,dv,t('scope'),t('optional'),t('type') or 'jar'
 for d in root.findall('m:dependencyManagement/m:dependencies/m:dependency',NS):
  dg,da,dv,scope,opt,typ=parse(d)
  if typ=='pom' and scope=='import':mgmt.update(model(dg,da,dv)[1])
  else:mgmt[dg,da]=dv
 deps=[]
 for d in root.findall('m:dependencies/m:dependency',NS):
  dg,da,dv,scope,opt,typ=parse(d)
  if scope not in ['test','provided','system'] and opt!='true' and typ=='jar':deps.append((dg,da,dv))
 result=(props,mgmt,deps);models[key]=result;return result
roots=[('io.github.reandroid','ARSCLib','1.4.0'),('org.eclipse.jdt.core.compiler','ecj','4.6.1'),('com.jakewharton.android.repackaged','dalvik-dx','14.0.0_r21'),('org.ow2.asm','asm','9.7.1'),('io.github.skylot','jadx-core','1.4.7'),('io.github.skylot','jadx-dex-input','1.4.7'),('org.bouncycastle','bcpg-jdk15on','1.70'),('org.smali','baksmali','2.5.2'),('org.smali','smali','2.5.2'),('commons-net','commons-net','3.10.0'),('org.slf4j','slf4j-nop','2.0.6'),('com.google.guava','guava','31.1-android'),('com.android.tools.build','apksig','8.2.2'),('org.apache.commons','commons-compress','1.28.0'),('org.tukaani','xz','1.10'),('commons-io','commons-io','2.20.0'),('org.apache.commons','commons-lang3','3.18.0'),('commons-codec','commons-codec','1.19.0'),('net.lingala.zip4j','zip4j','2.11.5'),('com.github.mwiede','jsch','0.2.20'),('com.squareup.okhttp3','okhttp','3.14.9'),('eu.agno3.jcifs','jcifs-ng','2.1.10')]
pins={(g,a):v for g,a,v in roots};pins['org.slf4j','slf4j-api']='2.0.6'
queue=list(roots);seen={}
while queue:
 g,a,v=queue.pop(0);v=pins.get((g,a),v)
 if (g,a) in seen or g in ('junit','org.hamcrest'):continue
 if not v or '${' in v:raise RuntimeError(f'Unresolved version: {g}:{a}:{v}')
 seen[g,a]=v;print('Resolve',g,a,v,flush=True);queue.extend(model(g,a,v)[2])
def jar(item):
 (g,a),v=item;p=LIB/f'{a}-{v}.jar'
 if not p.exists():p.write_bytes(download(g,a,v,'jar'))
 print('Fetched',p.name,flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(jar,seen.items()))
(ROOT/'DEPENDENCIES.json').write_text(json.dumps([{'group':g,'artifact':a,'version':v} for (g,a),v in seen.items()]+[{'group':'dev.rikka.shizuku','artifact':a,'version':'13.1.5'} for a in ('api','provider','aidl','shared')],indent=2)+'\n')

def fetch(url):
 for _ in range(3):
  try:return urllib.request.urlopen(url,timeout=120).read()
  except Exception as e:last=e
 raise last

# Android Zstandard JNI AAR; package its Java API and four Android ABIs.
# zstd-jni 1.5.7-6 is already 16 KB ELF-aligned and the build stores .so uncompressed.
import zipfile, io
g,a,v='com.github.luben','zstd-jni','1.5.7-6'
with zipfile.ZipFile(io.BytesIO(download(g,a,v,'aar'))) as z:
 for n in z.namelist():
  if n=='classes.jar':(LIB/f'{a}-{v}.jar').write_bytes(z.read(n))
  elif n.startswith('jni/') and n.endswith('.so'):
   p=ROOT/'app/src/main/jniLibs'/n[4:];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))

# Termux terminal-view/terminal-emulator Java APIs. The AAR's libtermux.so is
# 4 KB-aligned, so it is intentionally not extracted; the 16 KB rebuild is
# vendored in app/src/main/jniLibs and reproduced by tools/build-libtermux.sh.
# terminal-emulator jars go to app/libs (its AAR is excluded from Gradle);
# terminal-view's jar goes to tools/termux/libs for tools/build.py only, so it
# does not duplicate the classes and resources merged from the Gradle AAR.
VIEW_LIB=ROOT/'tools/termux/libs';VIEW_LIB.mkdir(parents=True,exist_ok=True)
for a,lib in (('terminal-emulator',LIB),('terminal-view',VIEW_LIB)):
 g,v='com.github.termux.termux-app','v0.118.3'
 url=f'https://jitpack.io/{g.replace(".","/")}/{a}/{v}/{a}-{v}.aar'
 with zipfile.ZipFile(io.BytesIO(fetch(url))) as z:
  (lib/f'{a}-{v}.jar').write_bytes(z.read('classes.jar'))
 print('Fetched',f'{a}-{v}.jar',flush=True)
# Rikka Shizuku API/provider/aidl/shared (AAR): extract classes.jar for the desktop fallback build.
# aidl 提供 moe.shizuku.server AIDL 接口, shared 提供 ShizukuApiConstants——缺失会导致运行时 NoClassDefFoundError。
for a in ('api','provider','aidl','shared'):
 g,v='dev.rikka.shizuku',a+'/13.1.5'
 url=f'https://repo.maven.apache.org/maven2/dev/rikka/shizuku/{a}/13.1.5/{a}-13.1.5.aar'
 with zipfile.ZipFile(io.BytesIO(fetch(url))) as z:
  (LIB/f'shizuku-{a}-13.1.5.jar').write_bytes(z.read('classes.jar'))
 print('Fetched',f'shizuku-{a}-13.1.5.jar',flush=True)

# The four vendored libtermux.so files must stay 16 KB ELF-aligned.
import struct
PT_LOAD,PT_GNU_RELRO=1,0x6474e552
def _verify_elf(path):
 d=path.read_bytes()
 if d[:4]!=b'\x7fELF':raise RuntimeError(f'Not an ELF file: {path}')
 if d[4]==2:
  phoff=struct.unpack_from('<Q',d,32)[0];entsize=struct.unpack_from('<H',d,54)[0];count=struct.unpack_from('<H',d,56)[0];po,pv,pm,pa=8,16,40,48;fmt='<Q'
 else:
  phoff=struct.unpack_from('<I',d,28)[0];entsize=struct.unpack_from('<H',d,42)[0];count=struct.unpack_from('<H',d,44)[0];po,pv,pm,pa=4,8,20,28;fmt='<I'
 loads=0;relro_end=0
 for i in range(count):
  o=phoff+i*entsize;t=struct.unpack_from('<I',d,o)[0]
  if t==PT_LOAD:
   off=struct.unpack_from(fmt,d,o+po)[0];va=struct.unpack_from(fmt,d,o+pv)[0];align=struct.unpack_from(fmt,d,o+pa)[0]
   if align<16384 or off%align!=va%align:raise RuntimeError(f'{path} has a LOAD segment below 16 KB alignment or not congruent')
   loads+=1
  elif t==PT_GNU_RELRO:
   relro_end=struct.unpack_from(fmt,d,o+pv)[0]+struct.unpack_from(fmt,d,o+pm)[0]
 if not loads:raise RuntimeError(f'No LOAD segment: {path}')
 if relro_end and relro_end%16384:raise RuntimeError(f'{path} RELRO ends at {relro_end:#x}, not 16 KB-aligned')
for p in (ROOT/'app/src/main/jniLibs').rglob('*.so'):_verify_elf(p)
for abi in ('arm64-v8a','armeabi-v7a','x86','x86_64'):
 if not (ROOT/'app/src/main/jniLibs'/abi/'libtermux.so').exists():raise RuntimeError(f'Missing libtermux.so for {abi}; run tools/build-libtermux.sh')
p=ROOT/'DEPENDENCIES.json';deps=json.loads(p.read_text())
deps=[d for d in deps if (d['group'],d['artifact'])!=('com.github.luben','zstd-jni')]
deps.append({'group':'com.github.luben','artifact':'zstd-jni','version':'1.5.7-6'})
for a in ('terminal-emulator','terminal-view'):
 deps=[d for d in deps if (d['group'],d['artifact'])!=('com.github.termux.termux-app',a)]
 deps.append({'group':'com.github.termux.termux-app','artifact':a,'version':'v0.118.3'})
p.write_text(json.dumps(deps,indent=2)+'\n')
