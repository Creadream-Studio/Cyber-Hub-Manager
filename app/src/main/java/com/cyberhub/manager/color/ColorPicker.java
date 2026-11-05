/*
 * 屏幕取色的辅助常量与全局运行状态。
 * 取色功能移植自开源项目 ScreenColorPicker（作者：Ratul Hasan，MIT 许可）。
 */
package com.cyberhub.manager.color;

import android.content.Context;
import android.util.TypedValue;

public final class ColorPicker {

    public static final String PREFS = "color_picker";
    public static final String PREF_MAGNIFIER_SIZE = "magnifier_size";
    public static final String PREF_CAPTURE_SPEED = "capture_speed";
    public static final String PREF_SHOW_GRID_LINES = "show_grid_lines";
    public static final String PREF_CAPTURE_RANGE = "capture_range";

    private static volatile boolean running = false;

    private ColorPicker() {
    }

    public static boolean isRunning() {
        return running;
    }

    static void setRunning(boolean value) {
        running = value;
    }

    public static int dp2px(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void stopService(Context context) {
        running = false;
        context.stopService(new android.content.Intent(context, ColorPickerService.class));
    }
}
