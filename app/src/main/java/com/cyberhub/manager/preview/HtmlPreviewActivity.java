package com.cyberhub.manager.preview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.Locale;

/**
 * Sandboxed HTML preview: renders one local .html/.htm/.xhtml file in a
 * private WebView that never calls addJavascriptInterface, so previewed
 * pages cannot reach the CyberNative file bridge. Relative resources next
 * to the file keep working; every request is restricted to the file's own
 * folder, data: URIs stay inline, and any other scheme (http(s), content://,
 * file:// outside the folder) is refused or handed off: link clicks to
 * http(s) go to the external browser, everything else is blocked offline.
 */
public final class HtmlPreviewActivity extends Activity {
    private WebView web;
    private String basePrefix;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getStringExtra("preview_path");
        String title = getIntent().getStringExtra("preview_title");
        if (path == null) { finish(); return; }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            Toast.makeText(this, "网页文件未找到或无法读取", Toast.LENGTH_SHORT).show();
            finish(); return;
        }
        String parent = file.getParent();
        if (parent == null) parent = path;
        basePrefix = new File(parent).getAbsolutePath() + File.separator;

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xff141414);
        int barPad = dp(10);
        bar.setPadding(barPad, barPad, barPad, barPad);
        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(0xffdddddd);
        close.setTextSize(18);
        close.setPadding(dp(6), 0, dp(14), 0);
        close.setOnClickListener(v -> finish());
        TextView label = new TextView(this);
        label.setText(title == null || title.length() == 0 ? file.getName() : title);
        label.setTextColor(0xffdddddd);
        label.setTextSize(16);
        label.setSingleLine(true);
        label.setPadding(0, dp(2), 0, 0);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(close);
        bar.addView(label, labelLp);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);            // API 30+ 默认关闭；预览需要 file://
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);      // 页面脚本不得 XHR 相邻文件
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setGeolocationEnabled(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setTextZoom(100);
        web.setBackgroundColor(0xff26282d);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.US);
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
                    catch (Exception e) { Toast.makeText(HtmlPreviewActivity.this, "无法打开链接", Toast.LENGTH_SHORT).show(); }
                    return true; // 预览本身绝不联网导航
                }
                if ("file".equals(scheme)) {
                    File target = new File(u.getPath() == null ? "" : u.getPath());
                    if (target.getAbsolutePath().startsWith(basePrefix)) return false; // 文件夹内导航放行
                    Toast.makeText(HtmlPreviewActivity.this, "已阻止访问预览文件夹之外的文件", Toast.LENGTH_SHORT).show();
                }
                return true; // 其余 scheme 一律拦截
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.US);
                if ("data".equals(scheme)) return null; // 内联 data: 资源放行
                if ("file".equals(scheme)) {
                    File target = new File(u.getPath() == null ? "" : u.getPath());
                    if (target.getAbsolutePath().startsWith(basePrefix)) return null; // 同文件夹相对资源放行
                }
                // 网络与越界请求全部 404：预览完全离线，不泄露任何信息
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                        Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame())
                    Toast.makeText(HtmlPreviewActivity.this, "无法渲染此网页", Toast.LENGTH_SHORT).show();
            }
            @Override public boolean onRenderProcessGone(WebView v, android.webkit.RenderProcessGoneDetail d) {
                finish();
                return true;
            }
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff24262b);
        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(bar);
        root.addView(web, webLp);
        setContentView(root);
        web.loadUrl(Uri.fromFile(file).toString());
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) { web.goBack(); return; }
        super.onBackPressed();
    }

    @Override protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            View parent = (View) web.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
