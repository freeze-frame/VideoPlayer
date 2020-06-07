/*
 * Created on 2017/10/11.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.floatingmenu;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * @author 刘振林
 */
public class DensityUtils {
    private DensityUtils() {
    }

    public static int dp2px(@NonNull Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dp(@NonNull Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int px2sp(@NonNull Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int sp2px(@NonNull Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static int getScreenWidth(@NonNull Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    public static int getScreenHeight(@NonNull Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getRealScreenWidth(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            WindowManager wm = (WindowManager)
                    context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Point size = new Point();
                wm.getDefaultDisplay().getRealSize(size);
                return size.x;
            }
        }
        return getScreenWidth(context);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int getRealScreenHeight(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            WindowManager wm = (WindowManager)
                    context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Point size = new Point();
                wm.getDefaultDisplay().getRealSize(size);
                return size.y;
            }
        }
        return getScreenHeight(context);
    }
}
