/*
 * Created on 2017/11/12.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * @author 刘振林
 */
public class ScreenUtils {
    private ScreenUtils() {
    }

    /**
     * 获取系统屏幕亮度
     */
    @IntRange(from = 0, to = 255)
    public static int getScreenBrightness(@NonNull Context context) {
        int brightness = 0;
        try {
            brightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return brightness;
    }

    /**
     * 设置系统屏幕亮度
     */
    public void setScreenBrightness(@NonNull Context context, int brightness) {
        if (getScreenBrightness(context) != brightness) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
            Settings.System.putInt(resolver,
                    Settings.System.SCREEN_BRIGHTNESS, Math.max(0, Math.min(brightness, 255)));
            // 通知改变
            resolver.notifyChange(uri, null);
        }
    }

    /**
     * 获取当前Window的亮度
     */
    @IntRange(from = -1, to = 255)
    public static int getWindowBrightness(@NonNull Window window) {
        final float brightness = window.getAttributes().screenBrightness;
        if (brightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
            return (int) brightness;
        }
        return (int) (brightness * 255f + 0.5f);
    }

    /**
     * 改变当前Window的亮度
     */
    public static void setWindowBrightness(@NonNull Window window, int brightness) {
        if (getWindowBrightness(window) != brightness) {
            WindowManager.LayoutParams lp = window.getAttributes();
            if (brightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                // 跟随系统屏幕亮度
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            } else {
                lp.screenBrightness = (float) Math.max(0, Math.min(brightness, 255)) / 255f;
            }
            window.setAttributes(lp);
        }
    }

    /**
     * 判断屏幕能否自动旋转
     */
    public static boolean isRotationEnabled(@NonNull Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 设置屏幕能否自动旋转
     */
    public static void setRotationEnabled(@NonNull Context context, boolean enabled) {
        if (isRotationEnabled(context) != enabled) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION);
            Settings.System.putInt(resolver,
                    Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
            // 通知改变
            resolver.notifyChange(uri, null);
        }
    }
}
