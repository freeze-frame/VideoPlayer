/*
 * Created on 2018/06/23.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.text.TextUtils;

/**
 * 设备系统辅助类
 *
 * @author 刘振林
 */
public class OSHelper {
    private static final String PROPERTY_EMUI_VERSION = "ro.build.version.emui";
    private static final String PROPERTY_MIUI_VERSION = "ro.miui.ui.version.name";
    private static final String PROPERTY_COLOR_OS_VERSION = "ro.build.version.opporom";
    private static final String PROPERTY_FUNTOUCH_OS_VERSION = "ro.vivo.os.version";

    private OSHelper() {
    }

    /** 判断是否为“华为 Emotion UI” */
    public static boolean isEMUI() {
        return !TextUtils.isEmpty(
                SystemProperties.getString(PROPERTY_EMUI_VERSION)); // e.g. "EmotionUI_1.6"
    }

    /** 判断是否为“小米 MIUI” */
    public static boolean isMIUI() {
        return !TextUtils.isEmpty(
                SystemProperties.getString(PROPERTY_MIUI_VERSION)); // e.g. "V7"
    }

    /** 判断是否为“魅族 Flyme” */
    public static boolean isFlyme() {
        final String property = SystemProperties.getString("ro.build.display.id");
        return !TextUtils.isEmpty(property)
                && property.startsWith("Flyme OS "); // e.g. "Flyme OS 5.1.2.0U"
    }

    /** 判断是否为“OPPO Color OS” */
    public static boolean isColorOS() {
        return !TextUtils.isEmpty(SystemProperties.getString(PROPERTY_COLOR_OS_VERSION));
    }

    /** 判断是否为“vivo Funtouch OS” */
    public static boolean isFuntouchOS() {
        return !TextUtils.isEmpty(SystemProperties.getString(PROPERTY_FUNTOUCH_OS_VERSION));
    }

    /** 获取“小米 MIUI”的版本号 */
    public static int getMiuiVersion() {
        final String versionName = SystemProperties.getString(PROPERTY_MIUI_VERSION);
        if (versionName != null) {
            try {
                return Integer.parseInt(versionName.replace("V", ""));
            } catch (NumberFormatException e) {
                //
            }
        }
        return 0;
    }

    /**
     * 判断是否为“魅族 Flyme4”及以上
     */
    public static boolean isFlyme4OrLater() {
        final String property = SystemProperties.getString("ro.build.display.id");
        if (!TextUtils.isEmpty(property)) {
            final int beginIndex = "Flyme OS ".length();
            try {
                return Integer.parseInt(property.substring(beginIndex, beginIndex + 1)) >= 4;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                //
            }
        }
        return false;
    }
}
