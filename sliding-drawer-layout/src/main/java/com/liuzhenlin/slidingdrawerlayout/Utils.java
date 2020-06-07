/*
 * Created on 2018/06/16.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.slidingdrawerlayout;

import android.os.Build;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

/**
 * @author 刘振林
 */
public class Utils {
    private Utils() {
    }

    public static int getAbsoluteHorizontalGravity(@NonNull View parent, int gravity) {
        return getAbsoluteGravity(parent, gravity) & Gravity.HORIZONTAL_GRAVITY_MASK;
    }

    public static int getAbsoluteGravity(@NonNull View parent, int gravity) {
        final int layoutDirection = ViewCompat.getLayoutDirection(parent);
        return GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
    }

    /**
     * @return true if the view's layout direction has been resolved.
     */
    public static boolean isLayoutDirectionResolved(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return view.isLayoutDirectionResolved();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // FIXME: returns the exact result as above
            return view.getWindowToken() != null;
        }
        // No support for RTL in SDKs below 17
        return true;
    }

    /**
     * Indicates whether or not the view's layout direction is right-to-left.
     * This is resolved from layout attribute and/or the inherited value from its parent
     *
     * @return <code>true</code> if the layout direction is right-to-left
     */
    public static boolean isLayoutRtl(@NonNull View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }
}
