#!/usr/bin/env bash
# Rebuild the Termux PTY JNI library with 16 KB ELF segment alignment.
# Source: termux-app v0.118.3 terminal-emulator/src/main/jni/termux.c (GPLv3).
# The prebuilt library in the JitPack AAR uses 4 KB LOAD alignment and cannot
# be patched in place: its RX and RW segments share the same 4 KB page.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION=r28c
SRC="$ROOT/tools/termux/termux.c"
NDK="${ANDROID_NDK_HOME:-$ROOT/.build/android-ndk-$VERSION}"
OUT="$ROOT/app/src/main/jniLibs"

if [ ! -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" ]; then
  mkdir -p "$ROOT/.build"
  ZIP="$ROOT/.build/android-ndk-$VERSION-linux.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null; then
      curl -fL --retry 3 -o "$ZIP" "https://dl.google.com/android/repository/android-ndk-$VERSION-linux.zip"
    else
      python3 -c "import urllib.request,sys;urllib.request.urlretrieve(sys.argv[1],sys.argv[2])" "https://dl.google.com/android/repository/android-ndk-$VERSION-linux.zip" "$ZIP"
    fi
  fi
  if command -v unzip >/dev/null; then
    unzip -q "$ZIP" -d "$ROOT/.build"
  else
    python3 - "$ZIP" "$ROOT/.build" <<'PY'
import os,stat,sys,zipfile
z=zipfile.ZipFile(sys.argv[1]);root=sys.argv[2]
for i in z.infolist():
 p=os.path.join(root,i.filename);mode=i.external_attr>>16
 if i.is_dir():os.makedirs(p,exist_ok=True);continue
 os.makedirs(os.path.dirname(p),exist_ok=True)
 if stat.S_ISLNK(mode):os.symlink(z.read(i).decode(),p)
 else:
  with z.open(i) as s,open(p,'wb') as d:
   while True:
    b=s.read(1<<20)
    if not b:break
    d.write(b)
  if mode&0o111:os.chmod(p,os.stat(p).st_mode|0o755)
PY
  fi
fi

BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
FLAGS=(-shared -fPIC -O2 -DNDEBUG -Wl,-soname,libtermux.so -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
for spec in \
  "aarch64-linux-android26-clang arm64-v8a" \
  "armv7a-linux-androideabi26-clang armeabi-v7a" \
  "i686-linux-android26-clang x86" \
  "x86_64-linux-android26-clang x86_64"; do
  set -- $spec
  mkdir -p "$OUT/$2"
  "$BIN/$1" "${FLAGS[@]}" -o "$OUT/$2/libtermux.so" "$SRC" -llog
  echo "built $OUT/$2/libtermux.so"
done

python3 - "$OUT" <<'PY'
import struct,sys
from pathlib import Path
page=16384
for p in sorted(Path(sys.argv[1]).glob('*/libtermux.so')):
 d=p.read_bytes()
 if d[4]==2:
  phoff=struct.unpack_from('<Q',d,32)[0];es=struct.unpack_from('<H',d,54)[0];n=struct.unpack_from('<H',d,56)[0];fmt='<Q';po,pv,pa=8,16,48
 else:
  phoff=struct.unpack_from('<I',d,28)[0];es=struct.unpack_from('<H',d,42)[0];n=struct.unpack_from('<H',d,44)[0];fmt='<I';po,pv,pa=4,8,28
 loads=0
 for i in range(n):
  o=phoff+i*es
  if struct.unpack_from('<I',d,o)[0]!=1:continue
  off=struct.unpack_from(fmt,d,o+po)[0];va=struct.unpack_from(fmt,d,o+pv)[0];al=struct.unpack_from(fmt,d,o+pa)[0]
  assert al>=page and off%al==va%al,(p,hex(off),hex(va),hex(al))
  loads+=1
 assert loads>=2,p
 print('16 KB OK',p.relative_to(sys.argv[1]))
PY
