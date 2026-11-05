/*
 * 屏幕取色服务：MediaProjection 持续截屏 + 悬浮窗十字准星与放大镜。
 * 核心逻辑移植自开源项目 ScreenColorPicker（作者：Ratul Hasan，MIT 许可）。
 */
package com.cyberhub.manager.color;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public final class ColorPickerService extends Service implements MagnifierView.OnInteractionListener {

    private WindowManager windowManager;
    private ClipboardManager clipboard;

    private DisplayMetrics displayMetrics;

    // 悬浮窗
    private FrameLayout targetLayout;
    private WindowManager.LayoutParams targetParams;
    private TargetView targetView;
    private FrameLayout magnifierLayout;
    private WindowManager.LayoutParams magnifierParams;
    private MagnifierView magnifierView;

    // 屏幕捕获
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isCapturing = false;

    private Bitmap screenBitmap;

    // 逻辑变量
    private int scanX = 0;
    private int scanY = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;

    private final int minGapBetweenEdges = 50;
    private final int maxGapBetweenEdges = 100;

    // 偏好
    private final int targetSizeDp = 40;
    private long captureDelayMs = 50L;
    private SharedPreferences prefs;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (sp, key) -> {
                if (key == null) return;
                switch (key) {
                    case ColorPicker.PREF_MAGNIFIER_SIZE:
                        if (targetLayout != null && magnifierLayout != null) {
                            removeWindows();
                            setupWindows();
                        }
                        break;
                    case ColorPicker.PREF_CAPTURE_SPEED:
                        captureDelayMs = getCaptureDelayMs();
                        break;
                    case ColorPicker.PREF_SHOW_GRID_LINES:
                        if (magnifierView != null) {
                            magnifierView.setShowGridLines(getShowGridLines());
                        }
                        break;
                    case ColorPicker.PREF_CAPTURE_RANGE:
                        if (targetView != null) {
                            targetView.setCaptureRange(getCaptureRange());
                        }
                        break;
                    default:
                        break;
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        prefs = getSharedPreferences(ColorPicker.PREFS, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        captureDelayMs = getCaptureDelayMs();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        int resultCode = intent != null
                ? intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                : Activity.RESULT_CANCELED;
        Intent resultData = intent != null
                ? intent.getParcelableExtra(EXTRA_RESULT_DATA)
                : null;

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            setupWindows();
            startScreenCapture(resultCode, resultData);
            ColorPicker.setRunning(true);
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupWindows() {
        displayMetrics = getResources().getDisplayMetrics();
        Rect bounds = new Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        screenWidth = bounds.width();
        screenHeight = bounds.height();

        int targetSizePx = ColorPicker.dp2px(this, targetSizeDp);
        int magnifierSizePx = ColorPicker.dp2px(this, getMagnifierSizeDp());

        // 十字准星窗口
        targetLayout = new FrameLayout(this);
        targetView = new TargetView(this);
        targetView.setCaptureRange(getCaptureRange());
        targetLayout.addView(targetView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        targetParams = createWindowLayoutParams();
        targetParams.width = targetSizePx;
        targetParams.height = targetSizePx;
        targetParams.x = (screenWidth - targetSizePx) / 2;
        targetParams.y = (screenHeight - targetSizePx) / 2;

        // 放大镜窗口
        magnifierLayout = new FrameLayout(this);
        magnifierView = new MagnifierView(this);
        magnifierView.setListener(this);
        magnifierView.setShowGridLines(getShowGridLines());
        magnifierLayout.addView(magnifierView,
                new FrameLayout.LayoutParams(magnifierSizePx, magnifierSizePx));
        magnifierParams = createWindowLayoutParams();
        magnifierParams.x = (screenWidth - magnifierSizePx) / 2;
        magnifierParams.y = Math.max(0, targetParams.y - minGapBetweenEdges);

        addTargetDragListener();
        addMagnifierFineTuneListener();

        windowManager.addView(targetLayout, targetParams);
        windowManager.addView(magnifierLayout, magnifierParams);

        targetView.post(() -> {
            updateScanCoordinates();
            float tRadius = targetSizePx / 2f;
            repositionMagnifier(
                    targetParams.x + tRadius,
                    targetParams.y + tRadius,
                    tRadius,
                    magnifierSizePx
            );
            windowManager.updateViewLayout(magnifierLayout, magnifierParams);
        });
    }

    private void removeWindows() {
        if (targetLayout != null) windowManager.removeView(targetLayout);
        if (magnifierLayout != null) windowManager.removeView(magnifierLayout);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTargetDragListener() {
        final int[] init = new int[2];
        final float[] touch = new float[2];

        targetLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    init[0] = targetParams.x;
                    init[1] = targetParams.y;
                    touch[0] = event.getRawX();
                    touch[1] = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    targetParams.x = init[0] + (int) (event.getRawX() - touch[0]);
                    targetParams.y = init[1] + (int) (event.getRawY() - touch[1]);
                    boolean magChanged = checkDistanceAndRules();
                    windowManager.updateViewLayout(targetLayout, targetParams);
                    if (magChanged) {
                        windowManager.updateViewLayout(magnifierLayout, magnifierParams);
                    }
                    updateScanCoordinates();
                    return true;

                default:
                    return false;
            }
        });
    }

    private boolean checkDistanceAndRules() {
        int tSize = targetLayout.getWidth();
        int mSize = magnifierLayout.getWidth();
        if (tSize <= 0 || mSize <= 0) return false;

        float tx = targetParams.x + tSize / 2f;
        float ty = targetParams.y + tSize / 2f;
        float mx = magnifierParams.x + mSize / 2f;
        float my = magnifierParams.y + mSize / 2f;
        float dx = mx - tx;
        float dy = my - ty;
        float centerDistance = (float) Math.sqrt(dx * dx + dy * dy);
        float tRadius = tSize / 2f;
        float mRadius = mSize / 2f;
        float currentGap = centerDistance - tRadius - mRadius;

        int oldMagX = magnifierParams.x;
        int oldMagY = magnifierParams.y;

        // 规则一：间距过远时把放大镜拉回
        if (currentGap > maxGapBetweenEdges) {
            double angle = Math.atan2(dy, dx);
            float allowedDist = tRadius + mRadius + maxGapBetweenEdges;
            float newMx = (float) (tx + Math.cos(angle) * allowedDist);
            float newMy = (float) (ty + Math.sin(angle) * allowedDist);
            magnifierParams.x = (int) (newMx - mRadius);
            magnifierParams.y = (int) (newMy - mRadius);
        }

        // 规则二：间距过近时跳到新位置
        if (currentGap < minGapBetweenEdges) {
            repositionMagnifier(tx, ty, tRadius, mSize);
        }

        return magnifierParams.x != oldMagX || magnifierParams.y != oldMagY;
    }

    private void repositionMagnifier(float tx, float ty, float tRadius, int mSize) {
        float mRadius = mSize / 2f;
        int[][] directions = {
                {-1, 0}, // 左
                {0, -1}, // 上
                {1, 0},  // 右
                {0, 1}   // 下
        };
        int[] gapsToCheck = {maxGapBetweenEdges, minGapBetweenEdges};

        for (int gap : gapsToCheck) {
            float distFromCenter = tRadius + gap + mRadius;
            for (int[] dir : directions) {
                int pLeft;
                int pTop;
                if (dir[0] != 0) {
                    float pCenterX = tx + (dir[0] * distFromCenter);
                    pLeft = (int) (pCenterX - mRadius);
                    pTop = clamp((int) (ty - mRadius), 0, screenHeight - mSize);
                } else {
                    float pCenterY = ty + (dir[1] * distFromCenter);
                    pTop = (int) (pCenterY - mRadius);
                    pLeft = clamp((int) (tx - mRadius), 0, screenWidth - mSize);
                }

                boolean fitsHorizontally = (pLeft >= 0) && ((pLeft + mSize) <= screenWidth);
                boolean fitsVertically = (pTop >= 0) && ((pTop + mSize) <= screenHeight);
                if (fitsHorizontally && fitsVertically) {
                    magnifierParams.x = pLeft;
                    magnifierParams.y = pTop;
                    return;
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addMagnifierFineTuneListener() {
        final float[] touch = new float[2];
        final float[] accumulated = new float[2];

        magnifierView.setOnTouchListener((view, event) -> {
            view.onTouchEvent(event); // 允许点击环形按钮

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch[0] = event.getRawX();
                    touch[1] = event.getRawY();
                    accumulated[0] = 0f;
                    accumulated[1] = 0f;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - touch[0];
                    float dy = event.getRawY() - touch[1];
                    accumulated[0] += dx * 0.1f;
                    accumulated[1] += dy * 0.1f;
                    int moveX = (int) accumulated[0];
                    int moveY = (int) accumulated[1];
                    accumulated[0] -= moveX;
                    accumulated[1] -= moveY;
                    targetParams.x += moveX;
                    targetParams.y += moveY;
                    boolean magChanged = checkDistanceAndRules();
                    windowManager.updateViewLayout(targetLayout, targetParams);
                    if (magChanged) {
                        windowManager.updateViewLayout(magnifierLayout, magnifierParams);
                    }
                    updateScanCoordinates();
                    touch[0] = event.getRawX();
                    touch[1] = event.getRawY();
                    return true;

                default:
                    return true;
            }
        });
    }

    private WindowManager.LayoutParams createWindowLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        return params;
    }

    private void updateScanCoordinates() {
        PointF offset = targetView.getScanOffset();
        scanX = (int) offset.x;
        scanY = (int) offset.y;
    }

    @SuppressLint("WrongConstant")
    private void startScreenCapture(int resultCode, Intent resultData) {
        MediaProjectionManager mpManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopSelf();
            }
        }, handler);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int density = displayMetrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
        );
        isCapturing = true;
        captureLoop();
    }

    private void captureLoop() {
        if (!isCapturing) return;
        try {
            Image image = imageReader != null ? imageReader.acquireLatestImage() : null;
            if (image != null) {
                processImage(image);
            }
        } catch (Exception e) {
            Log.e("ColorPickerService", "Failed to process image", e);
        }
        handler.postDelayed(this::captureLoop, captureDelayMs);
    }

    private void processImage(Image image) {
        try (Image img = image) {
            Image.Plane[] planes = img.getPlanes();
            java.nio.ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * img.getWidth();
            int requiredWidth = img.getWidth() + rowPadding / pixelStride;
            int requiredHeight = img.getHeight();

            Bitmap bitmap = screenBitmap;
            if (bitmap == null || bitmap.getWidth() != requiredWidth
                    || bitmap.getHeight() != requiredHeight) {
                bitmap = Bitmap.createBitmap(requiredWidth, requiredHeight, Bitmap.Config.ARGB_8888);
                if (screenBitmap != null) {
                    screenBitmap.recycle();
                }
                screenBitmap = bitmap;
            }

            bitmap.copyPixelsFromBuffer(buffer);

            int safeX = clamp(scanX, 0, bitmap.getWidth() - 1);
            int safeY = clamp(scanY, 0, bitmap.getHeight() - 1);
            int pixelColor = bitmap.getPixel(safeX, safeY);
            String hexColor = String.format("#%06X", (0xFFFFFF & pixelColor));

            int cropSize = targetView.getSafeCropSize();
            int cropX = clamp(safeX - cropSize / 2, 0, Math.max(0, bitmap.getWidth() - cropSize));
            int cropY = clamp(safeY - cropSize / 2, 0, Math.max(0, bitmap.getHeight() - cropSize));

            Bitmap crop = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
            int[] pixels = new int[cropSize * cropSize];
            bitmap.getPixels(pixels, 0, cropSize, cropX, cropY, cropSize, cropSize);
            crop.setPixels(pixels, 0, cropSize, 0, 0, cropSize, cropSize);

            handler.post(() -> magnifierView.updateContent(crop, hexColor, safeX, safeY));
        }
    }

    @Override
    public void onCloseClicked() {
        stopSelf();
    }

    @Override
    public void onHexClicked(String hex) {
        copyToClipboard("颜色", hex);
    }

    @Override
    public void onCoordsClicked(String coords) {
        copyToClipboard("坐标", coords);
    }

    private void copyToClipboard(String label, String text) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, "已复制" + label + "：" + text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCapturing = false;
        handler.removeCallbacksAndMessages(null);
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (screenBitmap != null) {
            screenBitmap.recycle();
            screenBitmap = null;
        }
        if (targetLayout != null) windowManager.removeView(targetLayout);
        if (magnifierLayout != null) windowManager.removeView(magnifierLayout);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        ColorPicker.setRunning(false);
    }

    private Notification createNotification() {
        String channelId = "ColorPickerChannel";
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel chan = new NotificationChannel(
                channelId, "屏幕取色", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(chan);
        return new Notification.Builder(this, channelId)
                .setContentTitle("屏幕取色运行中")
                .setContentText("轻点放大镜外圈的色值或坐标即可复制；点 × 结束。")
                .setSmallIcon(android.R.drawable.ic_menu_crop)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    // 偏好读取
    private int getMagnifierSizeDp() {
        String size = prefs.getString(ColorPicker.PREF_MAGNIFIER_SIZE, "small");
        if (size == null) size = "small";
        switch (size) {
            case "medium":
                return 200;
            case "large":
                return 250;
            default:
                return 150;
        }
    }

    private long getCaptureDelayMs() {
        String speed = prefs.getString(ColorPicker.PREF_CAPTURE_SPEED, "normal");
        if (speed == null) speed = "normal";
        switch (speed) {
            case "fast":
                return 25L;
            case "slow":
                return 100L;
            default:
                return 50L;
        }
    }

    private boolean getShowGridLines() {
        return prefs.getBoolean(ColorPicker.PREF_SHOW_GRID_LINES, true);
    }

    private String getCaptureRange() {
        String range = prefs.getString(ColorPicker.PREF_CAPTURE_RANGE, "small");
        return range != null ? range : "small";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final int NOTIFICATION_ID = 2;
}
