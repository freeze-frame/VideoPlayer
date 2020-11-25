/*
 * Created on 2018/9/29 4:29 PM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.liuzhenlin.texturevideoview.utils.Utils;

/**
 * @author 刘振林
 */
public class DisplayCutoutUtils {

    public static final String HUAWEI_DISPLAY_NOTCH_STATUS = "display_notch_status";

    public static final String XIAOMI_DISPLAY_NOTCH_STATUS = "force_black";

    public static final int VIVO_FLAG_HAS_NOTCH_IN_SCREEN = 0x00000020;
    public static final int VIVO_FLAG_HAS_SCREEN_FILLETS = 0x00000008;

    private DisplayCutoutUtils() {
    }

    /**
     * 判断华为手机是否有刘海
     */
    public static boolean hasNotchInScreenForEMUI(@NonNull Context context) {
        try {
            Class<?> hwNotchSizeUtil = context.getClassLoader()
                    .loadClass("com.huawei.android.util.HwNotchSizeUtil");
            return (boolean) hwNotchSizeUtil
                    .getMethod("hasNotchInScreen")
                    .invoke(hwNotchSizeUtil);
        } catch (Exception e) {
            //
        }
        return false;
    }

    /**
     * 获取华为手机刘海的 宽、高
     */
    @NonNull
    public static int[] getNotchSizeForEMUI(@NonNull Context context) {
        try {
            Class<?> HwNotchSizeUtil = context.getClassLoader()
                    .loadClass("com.huawei.android.util.HwNotchSizeUtil");
            //noinspection ConstantConditions
            return (int[]) HwNotchSizeUtil
                    .getMethod("getNotchSize")
                    .invoke(HwNotchSizeUtil);
        } catch (Exception e) {
            //
        }
        return new int[]{0, 0};
    }

    /**
     * 检查华为手机是否开启了“隐藏屏幕刘海”
     *
     * @return 0表示“默认”，1表示“隐藏刘海区域”
     */
    public static boolean isNotchHiddenForEMUI(@NonNull Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(), HUAWEI_DISPLAY_NOTCH_STATUS, 0) == 1;
    }

    /**
     * 设置应用窗口是否在华为刘海屏手机中使用刘海区
     *
     * @param window 应用页面window对象
     * @param in     如果为{@code true}, 应用窗口将显示在刘海区
     */
    public static void setLayoutInDisplayCutoutForEMUI(@NonNull Window window, boolean in) {
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        try {
            Class<?> layoutParamsExCls = Class.forName("com.huawei.android.view.LayoutParamsEx");
            Object layoutParamsExObj = layoutParamsExCls
                    .getConstructor(WindowManager.LayoutParams.class)
                    .newInstance(layoutParams);
            layoutParamsExCls
                    .getMethod(in ? "addHwFlags" : "clearHwFlags", int.class)
                    .invoke(layoutParamsExObj, 0x00010000);
        } catch (Exception e) {
            return;
        }
        View decor = window.getDecorView();
        window.getWindowManager().updateViewLayout(decor, decor.getLayoutParams());
    }

    /**
     * 判断OPPO手机是否有刘海
     */
    public static boolean hasNotchInScreenForColorOS(@NonNull Context context) {
        return context.getPackageManager().hasSystemFeature(
                "com.oppo.feature.screen.heteromorphism");
    }

    /**
     * 获取OPPO手机刘海的 宽、高
     */
    @NonNull
    public static int[] getNotchSizeForColorOS() {
        final String notchLocation = getNotchLocationOnScreenForColorOS();
        if (!TextUtils.isEmpty(notchLocation)) {
            //noinspection ConstantConditions
            final int firstCommaIndex = notchLocation.indexOf(",");
            final int lastCommaIndex = notchLocation.lastIndexOf(",");
            final int colonIndex = notchLocation.indexOf(":");
            final int notchWidth =
                    Integer.parseInt(notchLocation.substring(colonIndex + 1, lastCommaIndex))
                            - Integer.parseInt(notchLocation.substring(0, firstCommaIndex));
            final int notchHeight =
                    Integer.parseInt(notchLocation.substring(lastCommaIndex + 1))
                            - Integer.parseInt(notchLocation.substring(firstCommaIndex + 1, colonIndex));
            return new int[]{
                    notchWidth, notchHeight
            };
        }
        return new int[]{0, 0};
    }

    /**
     * 获取OPPO手机刘海在屏幕中的位置，例如 378,0:702,80
     */
    @Nullable
    public static String getNotchLocationOnScreenForColorOS() {
        return SystemProperties.getString("ro.oppo.screen.heteromorphism", null);
    }

    /**
     * 判断vivo手机是否有刘海
     */
    public static boolean hasNotchInScreenForFuntouchOS(@NonNull Context context) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> ftFeature = context.getClassLoader()
                    .loadClass("android.util.FtFeature");
            return (boolean) ftFeature
                    .getMethod("isFeatureSupport", int.class)
                    .invoke(ftFeature, VIVO_FLAG_HAS_NOTCH_IN_SCREEN);
        } catch (Exception e) {
            //
        }
        return false;
    }

    /**
     * 获取vivo手机刘海的高度 (27dp)
     */
    public static int getNotchHeightForFuntouchOS(@NonNull Context context) {
        return Utils.roundFloat(context.getResources().getDisplayMetrics().density * 27f);
    }

    /**
     * 判断小米手机是否有刘海
     */
    public static boolean hasNotchInScreenForMIUI() {
        return SystemProperties.getInt("ro.miui.notch", 0) == 1;
    }

    /**
     * 获取小米手机刘海的高度
     */
    public static int getNotchHeightForMIUI(@NonNull Context context) {
        final int resourceId = context.getResources().getIdentifier(
                "notch_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 检查小米手机是否开启了“隐藏屏幕刘海”
     *
     * @return 0表示“默认”，1表示“隐藏刘海区域”
     */
    public static boolean isNotchHiddenForMIUI(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Global.getInt(
                    context.getContentResolver(), XIAOMI_DISPLAY_NOTCH_STATUS, 0) == 1;
        }
        return false;
    }

    /**
     * 设置应用窗口是否在小米刘海屏手机中使用刘海区
     *
     * @param window 应用页面window对象
     * @param in     如果为{@code true}, 应用窗口将显示在刘海区
     */
    public static void setLayoutInDisplayCutoutForMIUI(@NonNull Window window, boolean in) {
        // 0x00000100 | 0x00000200              竖屏绘制到刘海区
        // 0x00000100 | 0x00000400              横屏绘制到刘海区
        // 0x00000100 | 0x00000200 | 0x00000400 横竖屏都绘制到刘海区
        final int flag = 0x00000100 | 0x00000200 | 0x00000400;
        try {
            Window.class
                    .getMethod(in ? "addExtraFlags" : "clearExtraFlags", int.class)
                    .invoke(window, flag);
        } catch (Exception e) {
            //
        }
    }

    /**
     * 设置应用窗口是否在搭载 Android P 系统的刘海屏手机中使用刘海区
     */
    @RequiresApi(Build.VERSION_CODES.P)
    public static void setLayoutInDisplayCutoutSinceP(@NonNull Window window, boolean in) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.layoutInDisplayCutoutMode = in
                ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
        window.setAttributes(lp);
    }
}
