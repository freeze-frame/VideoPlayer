/*
 * Created on 2020-9-28 9:42:03 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.compat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.liuzhenlin.texturevideoview.utils.BitmapUtils;

/**
 * @author 刘振林
 */
public class RemoteViewsCompat {
    private RemoteViewsCompat() {
    }

    public static void setImageViewResource(
            @NonNull Context ctx,
            @NonNull RemoteViews remoteViews,
            @IdRes int viewId,
            @DrawableRes int resId) {
        setImageViewResourceWithTintList(ctx, remoteViews, viewId, resId, null);
    }

    public static void setImageViewResourceWithTint(
            @NonNull Context ctx,
            @NonNull RemoteViews remoteViews,
            @IdRes int viewId,
            @DrawableRes int resId,
            @ColorInt int tint) {
        setImageViewResourceWithTintList(ctx, remoteViews, viewId, resId, ColorStateList.valueOf(tint));
    }

    public static void setImageViewResourceWithTintList(
            @NonNull Context ctx,
            @NonNull RemoteViews remoteViews,
            @IdRes int viewId,
            @DrawableRes int resId,
            @Nullable ColorStateList tintList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remoteViews.setImageViewIcon(viewId,
                    IconCompat.createWithResource(ctx, resId)
                            .setTintList(tintList)
                            .toIcon(ctx));
        } else {
            // Creates a bitmap from a tinted retrieved drawable instead,
            // for compatibility of vector drawable resource that can not be directly created
            // via BitmapFactory.decodeResource(Resources, int)
            @SuppressWarnings("ConstantConditions")
            Drawable drawable = DrawableCompat.wrap(AppCompatResources.getDrawable(ctx, resId));
            if (tintList != null) {
                DrawableCompat.setTintList(drawable.mutate(), tintList);
            }
            Bitmap bitmap = BitmapUtils.drawableToBitmap(drawable);
            remoteViews.setImageViewBitmap(viewId, bitmap);
        }
    }
}
