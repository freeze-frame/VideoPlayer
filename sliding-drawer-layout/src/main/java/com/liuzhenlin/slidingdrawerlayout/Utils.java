/*
 * Created on 2018/06/16.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.slidingdrawerlayout;

import android.os.Build;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 刘振林
 */
public class Utils {
    private Utils() {
    }

    private static Method sIsLayoutDirectionResolvedMethod;
    private static boolean sIsLayoutDirectionResolvedMethodFetched;

    public static int dimColor(int color, @FloatRange(from = 0.0f, to = 1.0f) float amount) {
        return roundFloat(((color & 0xff000000) >>> 24) * (1 - amount)) << 24
                | color & 0x00ffffff;
    }

    /** Lightweight choice to {@link Math#round(float)} */
    public static int roundFloat(float value) {
        return (int) (value > 0 ? value + 0.5f : value - 0.5f);
    }

    /** Lightweight choice to {@link Math#round(double)} */
    public static long roundDouble(double value) {
        return (long) (value > 0 ? value + 0.5 : value - 0.5);
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
            ensureIsLayoutDirectionResolvedMethodFetched();
            if (sIsLayoutDirectionResolvedMethod != null) {
                try {
                    Boolean ret = (Boolean) sIsLayoutDirectionResolvedMethod.invoke(view);
                    return ret != null && ret;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        // No support for RTL in SDKs below 17
        return true;
    }

    private static void ensureIsLayoutDirectionResolvedMethodFetched() {
        if (!sIsLayoutDirectionResolvedMethodFetched) {
            try {
                sIsLayoutDirectionResolvedMethod =
                        View.class.getDeclaredMethod("isLayoutDirectionResolved");
                sIsLayoutDirectionResolvedMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            sIsLayoutDirectionResolvedMethodFetched = true;
        }
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
