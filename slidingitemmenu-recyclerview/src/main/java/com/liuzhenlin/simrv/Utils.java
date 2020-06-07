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

    public static boolean isLayoutRtl(@NonNull View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
}
