/*
 * Created on 2018/11/6 5:48 PM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.simrv;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

/**
 * @author 刘振林
 */
public class Utils {
    private Utils() {
    }

    /** Lightweight choice to {@link Math#round(float)} */
    public static int roundFloat(float value) {
        return (int) (value > 0 ? value + 0.5f : value - 0.5f);
    }

    /** Lightweight choice to {@link Math#round(double)} */
    public static long roundDouble(double value) {
        return (long) (value > 0 ? value + 0.5 : value - 0.5);
    }

    public static boolean isLayoutRtl(@NonNull View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
}
