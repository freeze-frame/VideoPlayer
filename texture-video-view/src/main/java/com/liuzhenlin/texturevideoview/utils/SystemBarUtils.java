/*
 * Created on 2017/10/12.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author 刘振林
 */
public class SystemBarUtils {
    private SystemBarUtils() {
    }

    @Px
    public static int getStatusHeight(@NonNull Context context) {
        final int resId = context.getResources().getIdentifier("status_bar_height",
                "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return 0;
    }

    @Px
    public static int getNavigationHeight(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final int resId = context.getResources().getIdentifier("navigation_bar_height",
                    "dimen", "android");
            if (resId > 0) {
                return context.getResources().getDimensionPixelSize(resId);
            }
        }
        return 0;
    }

    /**
     * 判断是否有虚拟按键
     */
    @SuppressLint({"ObsoleteSdkInt", "PrivateApi"})
    public static boolean hasNavigationBar(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return false;
        }

        boolean hasNavBar = false;
        final int resId = context.getResources().getIdentifier("config_showNavigationBar",
                "bool", "android");
        if (resId > 0) {
            hasNavBar = context.getResources().getBoolean(resId);
        }
        if (hasNavBar) {
            try {
                Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
                Method get = systemPropertiesClass.getMethod("get", String.class);
                String navBarOverride = (String) get.invoke(
                        systemPropertiesClass, "qemu.hw.mainkeys");

                if ("1".equals(navBarOverride)) {
                    hasNavBar = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hasNavBar;
    }

    /**
     * 设置显示或隐藏状态栏和虚拟按键
     * <p>
     * {@link View#SYSTEM_UI_FLAG_LAYOUT_STABLE}：使View的布局不变，隐藏状态栏或导航栏后，View不会被拉伸。
     * {@link View#SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN}：让decorView全屏显示，
     * 但状态栏不会被隐藏覆盖，状态栏依然可见，decorView顶端布局部分会被状态遮住。
     * {@link View#SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION}：让decorView全屏显示，
     * 但导航栏不会被隐藏覆盖，导航栏依然可见，decorView底端布局部分会被导航栏遮住。
     */
    public static void showSystemBars(@NonNull Window window, final boolean show) {
        final View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    // The above 3 flags make the content appear under the system bars
                    // so that the content doesn't resize when the system bars hide and show.
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide navigation bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            // The IMMERSIVE_STICKY flag to prevent the flags of hiding navigation bar and
            // hiding status bar from being force-cleared by the system on any user interaction.
            if (show) {
                // This snippet shows the system bars.
                // It does this by removing all the flags.
                // Make the content appear below status bar and above navigation bar(if the device has).
                flags = (decorView.getSystemUiVisibility() & ~flags);
            } else {
                // This snippet hides the system bars.
                flags |= decorView.getSystemUiVisibility();
            }
            decorView.setSystemUiVisibility(flags);
        } else {
            final int flags = window.getAttributes().flags;
            final int fullscreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            if (show) {
                if ((flags & fullscreenFlag) != 0) {
                    window.clearFlags(fullscreenFlag);
                }
            } else {
                if (flags != (flags | fullscreenFlag)) {
                    window.addFlags(fullscreenFlag);
                }
            }

            // FIXME: to hide navigation Permanently.
            final int visibility = decorView.getSystemUiVisibility();
            final int hideNavFlag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            if (show) {
                decorView.setSystemUiVisibility(visibility & ~hideNavFlag);
            } else {
                decorView.setSystemUiVisibility(visibility | hideNavFlag);
            }
        }
    }

    /**
     * 设置 半透明状态栏
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static void setTranslucentStatus(@NonNull Window window, boolean translucent) {
        final int flags = window.getAttributes().flags;
        final int statusFlag = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (translucent) {
            if ((flags & statusFlag) == 0) {
                window.addFlags(statusFlag);
            }
        } else {
            if ((flags & statusFlag) != 0) {
                window.clearFlags(statusFlag);
            }
        }
    }

    /**
     * 设置 半透明虚拟按键
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static void setTranslucentNavigation(@NonNull Window window, boolean translucent) {
        final int flags = window.getAttributes().flags;
        final int navFlag = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        if (translucent) {
            if ((flags & navFlag) == 0) {
                window.addFlags(navFlag);
            }
        } else {
            if ((flags & navFlag) != 0) {
                window.clearFlags(navFlag);
            }
        }
    }

    /**
     * 设置状态栏的颜色
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setStatusBackgroundColor(@NonNull Window window, @ColorInt int color) {
        if (window.getStatusBarColor() != color) {
            final int flag = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            if ((window.getAttributes().flags & flag) == 0) {
                window.addFlags(flag);
            }
            window.setStatusBarColor(color);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setStatusBackgroundColorRes(@NonNull Window window, @ColorRes int colorId) {
        Context context = window.getContext();
        setStatusBackgroundColor(window, ContextCompat.getColor(context, colorId));
    }

    /**
     * 设置导航栏的颜色
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setNavigationBackgroundColor(@NonNull Window window, @ColorInt int color) {
        if (window.getNavigationBarColor() != color) {
            final int flag = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            if ((window.getAttributes().flags & flag) == 0) {
                window.addFlags(flag);
            }
            window.setNavigationBarColor(color);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setNavigationBackgroundColorRes(@NonNull Window window, @ColorRes int colorId) {
        Context context = window.getContext();
        setNavigationBackgroundColor(window, ContextCompat.getColor(context, colorId));
    }

    /**
     * 设置透明状态栏
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setTransparentStatus(@NonNull Window window) {
        setStatusBackgroundColor(window, Color.TRANSPARENT);

        // Place the decorView under the status bar
        View decor = window.getDecorView();
        int visibility = decor.getSystemUiVisibility();
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decor.setSystemUiVisibility(visibility);
    }

    /**
     * 改变状态栏字体颜色（黑/白）
     *
     * @see <a href="https://developer.android.com/reference/android/R.attr.html#windowLightStatusBar"></a>
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public static void setLightStatus(@NonNull Window window, boolean light) {
        View decorView = window.getDecorView();
        int flags = decorView.getSystemUiVisibility();
        if (light) {
            // Make sure the required flag is added
            final int flag = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            if ((window.getAttributes().flags & flag) == 0) {
                window.addFlags(flag);
            }
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(flags);
    }

    /**
     * 改变小米手机的状态栏字体颜色, 要求MIUI6 ~ MIUI8
     */
    public static void setLightStatusForMIUI(@NonNull Window window, boolean light) {
        try {
            @SuppressLint("PrivateApi")
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            final int darkModeFlag =
                    layoutParams
                            .getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                            .getInt(layoutParams);

            //noinspection JavaReflectionMemberAccess
            Window.class
                    .getMethod("setExtraFlags", int.class, int.class)
                    .invoke(window, light ? darkModeFlag : 0, darkModeFlag);
        } catch (Exception e) {
            //
        }
    }

    /**
     * 改变魅族手机的状态栏字体颜色，要求Flyme4及以上
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    public static void setLightStatusForFlyme(@NonNull Window window, boolean light) {
        WindowManager.LayoutParams lp = window.getAttributes();

        Class<?> wmlpClass = WindowManager.LayoutParams.class;

        try {
            Field meizuFlags = wmlpClass.getDeclaredField("meizuFlags");
            meizuFlags.setAccessible(true);
            final int origin = meizuFlags.getInt(lp);

            Field darkFlag = wmlpClass.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
            darkFlag.setAccessible(true);
            final int flag = darkFlag.getInt(null);

            final int flags;
            if (light) {
                flags = origin | flag;
            } else {
                flags = origin & ~flag;
            }
            if (flags != origin) {
                meizuFlags.setInt(lp, flags);
                window.setAttributes(lp);
            }
        } catch (Exception e) {
            //
        }
    }
}