BUILD AND TEST

Build with tools/build.py first. Run python tests/run-core.py for 121 native JVM
checks (Core, APK, tree, video features, Java editing/signing, 34 APKS merge/rejection checks). Optionally pass a
resource ZIP as the first argument; a generated fixture is included.

Use Node.js + Happy DOM 15.11.7. Set HAPPY_DOM_PATH to its lib/index.js if it is
not at ../../toolchain/test-node/node_modules/happy-dom/lib/index.js.
Run startup-smoke.cjs, dom-smoke.cjs, layout-smoke.cjs, video-ui-smoke.cjs,
motion-zoom-smoke.cjs, java-edit-ui-smoke.cjs and apks-ui-smoke.cjs for 95 checks.
Run open-with-ui-smoke.cjs for 8 checks of the long-press Open With dialog
(tabs, builtin tile grid, dynamic greying, app queries, explicit launch,
search, hex viewer). open-with-screenshot.cjs renders reference PNGs with
Playwright when a browser is available (CHROME_BIN style paths).

Run file-icons-smoke.cjs for 8 checks of the 0.1.58/0.1.59 file type icons:
fileType mapping (dex/bak/archive/xml/arec/so/bin/hap/iap/ipa plus image,
audio, video, text, unknown and split-part fallbacks), rich SVG icon markup,
list row icon nodes, real .apk icon replacement, .apks android robot plus
badge overlay, apkIcon RPC arguments, front-end cache on re-render, and Open
With category gating. 0.1.59 adds so (purple gear badge), bin (dark slate
with green binary digits), the installer badges hap (HarmonyOS blue with
cyan orbit), iap (teal with install arrow), ipa (silver with dark arrow),
exe (Microsoft blue with a white four-pane window plus EXE badge) and deb
(Debian pink with a white spiral swirl plus DEB badge) coverage, and turns
the .apk placeholder/fallback icon into a deep-green software badge (white
robot head plus APK text) while keeping the real .apk icon lazy replacement.

performance-profile.cjs counts full-buffer reads, line-mark visits and editor
refreshes on a 15,003-line file. UI_SCRIPT can point to an older app.js for
comparison. It does not measure paint, GPU, Android frame pacing or device FPS.

ui-smoke.cjs is an optional Playwright rendering harness. It was not run
successfully here. No physical Android test result is claimed.

make-apks-fixtures.py creates base/French splits with aapt2. ApksSmokeTest
adds generated Smali classes and feature manifests, verifies resource and DEX
retention, TOC variant isolation, signatures, original preservation and rollback.
No proprietary or user-supplied APK is bundled in these fixtures.
