/*
 * Created on 2018/04/12.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.liuzhenlin.texturevideoview.utils.Utils;

import java.io.IOException;

/**
 * @author 刘振林
 */
public class BitmapUtils2 {
    private BitmapUtils2() {
    }

    @NonNull
    public static Bitmap tintedBitmap(@NonNull Bitmap bitmap, @ColorInt int tint) {
        if (!bitmap.isMutable()) {
            bitmap = bitmap.copy(bitmap.getConfig(), true);
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColorFilter(new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN));
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmap;
    }

    @NonNull
    public static Bitmap createScaledBitmap(@NonNull Bitmap src, int reqWidth, int reqHeight, boolean recycleInput) {
        // 记录src的宽高
        final int width = src.getWidth();
        final int height = src.getHeight();

        if (reqWidth == width && reqHeight == height) {
            return src;
        }

        // 计算缩放比例
        final float widthScale = (float) reqWidth / width;
        final float heightScale = (float) reqHeight / height;
        // 创建一个matrix容器
        Matrix matrix = new Matrix();
        // 缩放
        matrix.postScale(widthScale, heightScale);
        // 创建缩放后的图片
        Bitmap out = Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);

        if (recycleInput && out != src) {
            src.recycle();
        }
        return out;
    }

    public static Bitmap createScaledBitmap(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return createScaledBitmap(BitmapFactory.decodeFile(path, options), reqWidth, reqHeight, true);
    }

    /**
     * 计算图片的缩放值
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            final int widthRatio = Utils.roundFloat((float) width / reqWidth);
            final int heightRatio = Utils.roundFloat((float) height / reqHeight);
            inSampleSize = Math.min(widthRatio, heightRatio);
        }
        return inSampleSize;
    }

    /**
     * 返回根据给定路径图片文件的旋转角度旋转后的位图
     */
    @Nullable
    public static Bitmap decodeRotatedBitmapFormFile(@NonNull String path) {
        return decodeRotatedBitmapFormFile(path, null);
    }

    @Nullable
    public static Bitmap decodeRotatedBitmapFormFile(@NonNull String path, @Nullable BitmapFactory.Options opts) {
        Bitmap raw = BitmapFactory.decodeFile(path, opts);

        if (raw != null) {
            final int degrees = readPictureRotation(path);
            if (degrees != 0) {
                return rotatedBitmap(raw, degrees, true);
            }
        }

        return raw;
    }

    /**
     * 读取图片旋转角度
     */
    public static int readPictureRotation(@NonNull String path) {
        try {
            switch (new ExifInterface(path)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 旋转图片
     */
    @NonNull
    public static Bitmap rotatedBitmap(@NonNull Bitmap src, float degrees, boolean recycleInput) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap out = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);

        if (recycleInput && out != src) {
            src.recycle();
        }
        return out;
    }
}
