/*
 * Created on 2019/3/23 6:28 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.palette.graphics.Palette;

import java.io.IOException;

/**
 * @author 刘振林
 */
public class BitmapUtils {
    private BitmapUtils() {
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

    @NonNull
    public static Bitmap createRoundCornerBitmap(@NonNull Bitmap src, /* px */ float connerRadius, boolean recycleInput) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 设置矩形大小
        Rect rect = new Rect(0, 0, out.getWidth(), out.getHeight());
        RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, connerRadius, connerRadius, paint); // 画圆角
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN)); // 取两层绘制，显示上层
        canvas.drawBitmap(src, rect, rect, paint);

        if (recycleInput && out != src) {
            src.recycle();
        }
        return out;
    }

    @NonNull
    public static Drawable bitmapToDrawable(@NonNull Resources res, @NonNull Bitmap bitmap) {
        return new BitmapDrawable(res, bitmap);
    }

    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // 取 drawable 的长宽
        final int w = drawable.getIntrinsicWidth();
        final int h = drawable.getIntrinsicHeight();

        // 取 drawable 的颜色格式
        @SuppressWarnings("deprecation")
        Bitmap.Config config = drawable.getOpacity() == PixelFormat.OPAQUE ?
                Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        // 把 drawable 内容画到画布中
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    @ColorInt
    public static int getDominantColorOrThrow(@NonNull Bitmap bitmap) {
        Palette.Swatch swatch = new Palette.Builder(bitmap)
                .maximumColorCount(24)
                .clearFilters()
                .addFilter(DEFAULT_FILTER)
                .generate()
                .getDominantSwatch();
        if (swatch != null) {
            return swatch.getRgb();
        }
        throw new IllegalArgumentException("No dominant color found in the given bitmap.");
    }

    @ColorInt
    public static int getDominantColor(@NonNull Bitmap bitmap, @ColorInt int defaultColor) {
        return new Palette.Builder(bitmap)
                .maximumColorCount(24)
                .clearFilters()
                .addFilter(DEFAULT_FILTER)
                .generate()
                .getDominantColor(defaultColor);
    }

    private static final Palette.Filter DEFAULT_FILTER = new Palette.Filter() {
//        private static final float BLACK_MAX_LIGHTNESS = 0.05f;
//        private static final float WHITE_MIN_LIGHTNESS = 0.95f;

        @Override
        public boolean isAllowed(int rgb, @NonNull float[] hsl) {
            return /*!isWhite(hsl) && !isBlack(hsl) &&*/ !isNearRedILine(hsl);
        }

//        /**
//         * @return true if the color represents a color which is close to black.
//         */
//        private boolean isBlack(float[] hslColor) {
//            return hslColor[2] <= BLACK_MAX_LIGHTNESS;
//        }
//
//        /**
//         * @return true if the color represents a color which is close to white.
//         */
//        private boolean isWhite(float[] hslColor) {
//            return hslColor[2] >= WHITE_MIN_LIGHTNESS;
//        }

        /**
         * @return true if the color lies close to the red side of the I line.
         */
        private boolean isNearRedILine(float[] hslColor) {
            return hslColor[0] >= 10f && hslColor[0] <= 37f && hslColor[1] <= 0.82f;
        }
    };
}
