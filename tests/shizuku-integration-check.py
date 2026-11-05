#!/usr/bin/env python3
"""Shizuku 集成自检：manifest 的 ShizukuProvider 必须以「包名.shizuku」注册且属性齐全。

背景（0.1.54 根因修复）：本项目用 aapt2 直接打包，无 Gradle manifest merger，
manifest 里的 ${applicationId} 占位符不会被替换。Shizuku 客户端运行时按
content://<packageName>.shizuku 查找 provider 拿 binder——若 authorities 是
字面量 ${applicationId}.shizuku，则 binder 永远无法送达，Shizuku 全部检测失败，
设备上表现为「服务明明在运行，应用却一直显示 Shizuku 未运行」。
"""
import re, sys, xml.etree.ElementTree as ET
from pathlib import Path

P = Path(__file__).resolve().parents[1]
MANIFEST = P / 'app/src/main/AndroidManifest.xml'
PKG = 'com.cyberhub.manager'

text = MANIFEST.read_text(encoding='utf-8')
errors = []

# 1. 任何地方都不允许出现未替换的占位符
for m in re.finditer(r'\$\{[^}]*\}', text):
    errors.append(f'未替换的构建占位符: {m.group(0)}')

root = ET.fromstring(text)
ns = {'android': 'http://schemas.android.com/apk/res/android'}
providers = [p for p in root.iter('provider')
             if p.get('{%s}name' % ns['android']) == 'rikka.shizuku.ShizukuProvider']
if not providers:
    errors.append('未找到 rikka.shizuku.ShizukuProvider 声明')
else:
    p = providers[0]
    a = lambda k: p.get('{%s}%s' % (ns['android'], k))
    if a('authorities') != f'{PKG}.shizuku':
        errors.append(f"authorities 应为 {PKG}.shizuku，实际为 {a('authorities')!r}")
    if a('exported') != 'true':
        errors.append('exported 必须为 true（ShizukuProvider.attachInfo 强制要求）')
    if a('multiprocess') != 'false':
        errors.append('multiprocess 必须为 false（ShizukuProvider.attachInfo 强制要求）')
    if a('enabled') in (None, 'false'):
        errors.append('enabled 必须为 true')
    if a('process') is not None:
        errors.append(
            "禁止设置 android:process（当前 %r）——官方模板不使用独立进程：provider 必须运行在主进程，"
            "Shizuku 服务端把 binder 经 call('sendBinder') 推送到 provider 所在进程；若 provider 在 :shizuku "
            "独立进程，binder 只会送进该进程，主进程（Shizuku 类的静态 binder 字段所在处）永远收不到，"
            "pingBinder() 恒为 false（0.1.54 的实际故障）" % a('process'))
    if a('permission') != 'android.permission.INTERACT_ACROSS_USERS_FULL':
        errors.append('permission 应为 android.permission.INTERACT_ACROSS_USERS_FULL')

# 2. queries 声明（可见性）
if 'moe.shizuku.privileged.api' not in text:
    errors.append('缺少 <queries> 声明 moe.shizuku.privileged.api')

# 3. 四个 Shizuku 构件必须都在 libs
libs = P / 'app/libs'
for jar in ('shizuku-api-13.1.5.jar', 'shizuku-provider-13.1.5.jar',
            'shizuku-aidl-13.1.5.jar', 'shizuku-shared-13.1.5.jar'):
    if not (libs / jar).exists():
        errors.append(f'缺少构件 {jar}（会导致 NoClassDefFoundError）')

# 4. ElevatedAccess 必须包含 provider 注册检测与 binder 等待重试
src = (P / 'app/src/main/java/com/cyberhub/manager/ElevatedAccess.java').read_text(encoding='utf-8')
for token in ('shizukuProviderRegistered', 'shizukuRunningWait', '"shizukuProviderRegistered"'):
    if token not in src:
        errors.append(f'ElevatedAccess.java 缺少 {token}')

if errors:
    print('FAIL')
    for e in errors:
        print(' -', e)
    sys.exit(1)
print('PASS: Shizuku 集成自检 6 项全部通过（占位符/authorities/属性/queries/构件/诊断字段）')
