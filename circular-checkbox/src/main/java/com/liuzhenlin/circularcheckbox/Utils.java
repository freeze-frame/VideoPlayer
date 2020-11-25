package com.liuzhenlin.circularcheckbox;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

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

    public static int dp2px(@NonNull Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return roundFloat(dipValue * scale);
    }

    public static int getGradientColor(@ColorInt int startColor, @ColorInt int endColor,
                                       @FloatRange(from = 0.0, to = 1.0) float percent) {
        final int startA = Color.alpha(startColor);
        final int startR = Color.red(startColor);
        final int startG = Color.green(startColor);
        final int startB = Color.blue(startColor);

        final int endA = Color.alpha(endColor);
        final int endR = Color.red(endColor);
        final int endG = Color.green(endColor);
        final int endB = Color.blue(endColor);

        final int currentA = roundFloat(startA * (1f - percent) + endA * percent);
        final int currentR = roundFloat(startR * (1f - percent) + endR * percent);
        final int currentG = roundFloat(startG * (1f - percent) + endG * percent);
        final int currentB = roundFloat(startB * (1f - percent) + endB * percent);
        return Color.argb(currentA, currentR, currentG, currentB);
    }
}
