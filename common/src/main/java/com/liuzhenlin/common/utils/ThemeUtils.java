/*
 * Created on 2020-9-28 6:23:14 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

/**
 * @author 刘振林
 */
public class ThemeUtils {
    private ThemeUtils() {
    }

    private static int[] sTextAppearanceStyleable;
    private static int sTextAppearanceStyleableTextColorIndex;
    private static boolean sTextAppearanceStyleableFetched;
    private static boolean sTextAppearanceStyleableTextColorIndexFetched;

    @SuppressLint("PrivateApi")
    private static void ensureTextAppearanceStyleableFetched(Context context, ClassLoader loader) {
        if (!sTextAppearanceStyleableFetched) {
            sTextAppearanceStyleableFetched = true;
            try {
                if (loader == null) {
                    loader = context.getClassLoader();
                }
                Class<?> styleable = loader.loadClass("com.android.internal.R$styleable");
                sTextAppearanceStyleable = (int[]) styleable.getField("TextAppearance").get(styleable);
            } catch (Exception e) {
                e.printStackTrace();
                sTextAppearanceStyleable = null;
            }
        }
    }

    @SuppressLint("PrivateApi")
    private static void ensureTextAppearanceStyleableTextColorIndexFetched(Context context, ClassLoader loader) {
        if (!sTextAppearanceStyleableTextColorIndexFetched) {
            sTextAppearanceStyleableTextColorIndexFetched = true;
            try {
                if (loader == null) {
                    loader = context.getClassLoader();
                }
                Class<?> styleable = loader.loadClass("com.android.internal.R$styleable");
                sTextAppearanceStyleableTextColorIndex =
                        styleable.getField("TextAppearance_textColor").getInt(styleable);
            } catch (Exception e) {
                e.printStackTrace();
                sTextAppearanceStyleableTextColorIndex = 0;
            }
        }
    }

    public static int getTextAppearanceDefaultTextColor(@NonNull Context context, @StyleRes int appearance) {
        ClassLoader loader = context.getClassLoader();
        ensureTextAppearanceStyleableFetched(context, loader);
        if (sTextAppearanceStyleable != null) {
            ensureTextAppearanceStyleableTextColorIndexFetched(context, loader);
            if (sTextAppearanceStyleableTextColorIndex != 0) {
                TypedArray ta = context.obtainStyledAttributes(appearance, sTextAppearanceStyleable);
                try {
                    ColorStateList csl = ta.getColorStateList(sTextAppearanceStyleableTextColorIndex);
                    if (csl != null) {
                        return csl.getDefaultColor();
                    }
                } finally {
                    ta.recycle();
                }
            }
        }
        return 0;
    }

    @DrawableRes
    public static int getWindowBackground(@NonNull Context context) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        final int background = a.getResourceId(0, 0);
        a.recycle();
        return background;
    }
}
