package com.liuzhenlin.circularcheckbox;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

public class Utils {
    private Utils() {
    }

    public static int dp2px(@NonNull Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
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

        final int currentA = (int) (startA * (1f - percent) + endA * percent + 0.5f);
        final int currentR = (int) (startR * (1f - percent) + endR * percent + 0.5f);
        final int currentG = (int) (startG * (1f - percent) + endG * percent + 0.5f);
        final int currentB = (int) (startB * (1f - percent) + endB * percent + 0.5f);
        return Color.argb(currentA, currentR, currentG, currentB);
    }
}
