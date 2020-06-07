/*
 * Created on 2018/06/26.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;

/**
 * @author 刘振林
 */
public class ColorUtils {
    private ColorUtils() {
    }

    public static boolean isLightColor(@ColorInt int color) {
        return getColorGrayLevel(color) >= 192;
    }

    public static float getColorGrayLevel(@ColorInt int color) {
        final int r = Color.red(color);
        final int g = Color.green(color);
        final int b = Color.blue(color);
        return r * 0.299f + g * 0.578f + b * 0.114f;
    }
}
