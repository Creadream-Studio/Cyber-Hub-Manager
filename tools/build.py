#!/usr/bin/env python3
"""Build a development APK with JDK 17, Android SDK 34, and the included jars."""
import os, sys, subprocess, zipfile, shutil, urllib.request, argparse, uuid
from pathlib import Path
P=Path(__file__).resolve().parents[1];B=P/'.build';B.mkdir(exist_ok=True);ROOT=P.parent
ap=argparse.ArgumentParser();ap.add_argument('--sdk',default=os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')));ap.add_argument('--tools',default='');args=ap.parse_args()
if args.sdk:
 SDK=Path(args.sdk);ANDROID=SDK/'platforms/android-34/android.jar';BT=Path(args.tools) if args.tools else SDK/'build-tools/35.0.0'
else:ANDROID=ROOT/'toolchain/android-34/android.jar';BT=ROOT/'toolchain/android-14'
if not ANDROID.exists() or not BT.exists():sys.exit('Install Android platform 34 and build-tools 35.0.0, then set ANDROID_HOME or pass --sdk.')
e=dict(os.environ);jvm=Path('/usr/lib/jvm/java-17-openjdk-amd64')
if jvm.exists():e['LD_LIBRARY_PATH']=str(jvm/'lib')+':'+str(jvm/'lib/server')+(':'+e['LD_LIBRARY_PATH'] if e.get('LD_LIBRARY_PATH') else '')
(B/'tmp').mkdir(exist_ok=True)
def run(cmd):
 print('Running',Path(cmd[0]).name,' '.join(str(x) for x in cmd[1:3]),flush=True)
 subprocess.run(list(map(str,cmd)),check=True,cwd=P,env=e)
def native(tool,*args):
 exe=BT/tool
 # Managed workspaces may be mounted noexec; Linux's dynamic linker can run SDK tools.
 loader=Path('/lib64/ld-linux-x86-64.so.2')
 return [str(loader),str(exe),*map(str,args)] if loader.exists() else [str(exe),*map(str,args)]
def java(*a):return ['java','-Xmx2g','-Djava.io.tmpdir='+str(B/'tmp'),*map(str,a)]
LIB=sorted((P/'app/libs').glob('*.jar'));VIEW_LIB=sorted((P/'tools/termux/libs').glob('*.jar'))
if not LIB or not VIEW_LIB:run([sys.executable,P/'tools/dependencies.py']);LIB=sorted((P/'app/libs').glob('*.jar'));VIEW_LIB=sorted((P/'tools/termux/libs').glob('*.jar'))
LIB=sorted(LIB+VIEW_LIB)
api=B/'android-api.jar'
if not api.exists():
 with zipfile.ZipFile(ANDROID) as src,zipfile.ZipFile(api,'w',zipfile.ZIP_DEFLATED) as out:
  for n in src.namelist():
   if not n.startswith(('java/','javax/','org/w3c/','org/xml/','org/ietf/')):out.writestr(n,src.read(n))
ecj=ROOT/'toolchain/ecj.jar'
if not ecj.exists():
 ecj=B/'ecj.jar'
 if not ecj.exists():urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.37.0/ecj-3.37.0.jar',ecj)
resources=B/'resources.zip';run(native('aapt2','compile','--dir',P/'app/src/main/res','-o',resources))
gen=B/'gen';shutil.rmtree(gen,ignore_errors=True);gen.mkdir()
base=B/'base.apk';run(native('aapt2','link','-o',base,'-I',ANDROID,'--java',gen,'--manifest',P/'app/src/main/AndroidManifest.xml','-A',P/'app/src/main/assets','--auto-add-overlay','--version-code','27','--version-name','0.1.31',resources))
classes=B/'classes';shutil.rmtree(classes,ignore_errors=True);classes.mkdir()
cp=os.pathsep.join(map(str,[api,*LIB]))
run(java('-jar',ecj,'-11','-proc:none','-nowarn','-encoding','UTF-8','-classpath',cp,'-d',classes,gen/'com/cyberhub/manager/R.java',*sorted((P/'app/src/main/java').rglob('*.java'))))
key=P/'app/src/main/assets/development-signing.p12'
if not key.exists():
 run(['keytool','-J-Dkeystore.pkcs12.legacy','-genkeypair','-keystore',key,'-storetype','PKCS12','-storepass','cyberhub-development','-keypass','cyberhub-development','-alias','cyberhub-dev','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=Cyber Hub Manager Development, O=Cyber Hub, C=IN'])
classjar=B/'app-classes.jar'
with zipfile.ZipFile(classjar,'w',zipfile.ZIP_DEFLATED) as z:
 for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
dex=B/'dex';shutil.rmtree(dex,ignore_errors=True);dex.mkdir()
r8=ROOT/'toolchain/r8-8.9.35.jar'
if not r8.exists():
 r8=B/'r8-8.9.35.jar'
 if not r8.exists():urllib.request.urlretrieve('https://dl.google.com/dl/android/maven2/com/android/tools/r8/8.9.35/r8-8.9.35.jar',r8)
run(java('-cp',r8,'com.android.tools.r8.D8','--release','--min-api','26','--lib',ANDROID,'--output',dex,classjar,*LIB))
unsigned=B/'unsigned.apk';shutil.copy(base,unsigned)
# Preserve service providers and JADX's classpath model/resources. Exclude JVM class files.
entries={};services={}
for jar in LIB:
 with zipfile.ZipFile(jar) as z:
  for n in z.namelist():
   if n.endswith('/') or n.endswith('.class') or n.startswith('META-INF/versions/') or n.upper().endswith(('.SF','.RSA','.DSA')) or n in ['META-INF/MANIFEST.MF','META-INF/INDEX.LIST']:continue
   if n.startswith('META-INF/services/'):services.setdefault(n,set()).update(z.read(n).decode().splitlines())
   elif n not in entries:entries[n]=z.read(n)
with zipfile.ZipFile(unsigned,'a',zipfile.ZIP_DEFLATED) as z:
 for f in dex.glob('*.dex'):z.write(f,f.name)
 for f in (P/'app/src/main/jniLibs').rglob('*.so'):z.write(f,'lib/'+str(f.relative_to(P/'app/src/main/jniLibs')),compress_type=zipfile.ZIP_STORED)
 for n,b in entries.items():z.writestr(n,b)
 for n,lines in services.items():z.writestr(n,'\n'.join(sorted(lines))+'\n')
aligned=B/'aligned.apk';run(native('zipalign','-P','16','-f','4',unsigned,aligned))
def verify_native_alignment(apk):
 import struct
 page=16384
 with zipfile.ZipFile(apk) as z:
  for info in z.infolist():
   if not (info.filename.startswith('lib/') and info.filename.endswith('.so')):continue
   if info.compress_type!=zipfile.ZIP_STORED:raise RuntimeError('Native library must be stored uncompressed: '+info.filename)
   z.fp.seek(info.header_offset);h=z.fp.read(30)
   if h[:4]!=b'PK\x03\x04':raise RuntimeError('Bad local header: '+info.filename)
   n,e=struct.unpack_from('<HH',h,26);dataoff=info.header_offset+30+n+e
   if dataoff%page:raise RuntimeError(f'{info.filename} zip data offset {dataoff:#x} is not 16 KB aligned')
   d=z.read(info)
   if d[:4]!=b'\x7fELF':raise RuntimeError('Not an ELF file: '+info.filename)
   if d[4]==2:
    phoff=struct.unpack_from('<Q',d,32)[0];entsize=struct.unpack_from('<H',d,54)[0];count=struct.unpack_from('<H',d,56)[0];fmt='<Q';po,pv,pm,pa=8,16,40,48
   else:
    phoff=struct.unpack_from('<I',d,28)[0];entsize=struct.unpack_from('<H',d,42)[0];count=struct.unpack_from('<H',d,44)[0];fmt='<I';po,pv,pm,pa=4,8,20,28
   loads=0;relro_end=0
   for i in range(count):
    o=phoff+i*entsize;t=struct.unpack_from('<I',d,o)[0]
    if t==1:
     off=struct.unpack_from(fmt,d,o+po)[0];va=struct.unpack_from(fmt,d,o+pv)[0];align=struct.unpack_from(fmt,d,o+pa)[0]
     if align<page or off%align!=va%align:raise RuntimeError(f'{info.filename} LOAD off={off:#x} va={va:#x} align={align:#x} fails 16 KB alignment')
     loads+=1
    elif t==0x6474e552:relro_end=struct.unpack_from(fmt,d,o+pv)[0]+struct.unpack_from(fmt,d,o+pm)[0]
   if not loads:raise RuntimeError('No LOAD segments: '+info.filename)
   if relro_end and relro_end%page:raise RuntimeError(f'{info.filename} RELRO ends at {relro_end:#x}, not 16 KB-aligned')
   print('16 KB OK',info.filename,'zip',hex(dataoff),'loads',loads)
verify_native_alignment(aligned)
output=P/'dist';output.mkdir(exist_ok=True);apk=output/'Cyber-Hub-Manager-0.1.31.apk'
signed=B/('signed-'+uuid.uuid4().hex+'.apk')
run(java('-jar',BT/'lib/apksigner.jar','sign','--ks',key,'--ks-pass','pass:cyberhub-development','--key-pass','pass:cyberhub-development','--out',signed,aligned))
run(java('-jar',BT/'lib/apksigner.jar','verify','--verbose',signed))
with zipfile.ZipFile(signed) as z:
 if z.testzip() is not None:raise RuntimeError('Signed APK CRC check failed')
 for f in (P/'app/src/main/assets').rglob('*'):
  if f.is_file() and z.read('assets/'+str(f.relative_to(P/'app/src/main/assets')))!=f.read_bytes():raise RuntimeError('Packaged asset mismatch: '+str(f))
# Publish the fully closed/verified artifact, never sign over a previous release.
shutil.copyfile(signed,apk)
signed.unlink()
print('BUILT',apk,'bytes',apk.stat().st_size)
