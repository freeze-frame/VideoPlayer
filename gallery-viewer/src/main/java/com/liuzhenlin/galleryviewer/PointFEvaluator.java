package com.liuzhenlin.galleryviewer;

import android.animation.TypeEvaluator;
import android.graphics.PointF;

/**
 * @author 刘振林
 */
public class PointFEvaluator implements TypeEvaluator<PointF> {
    private final PointF mInterpolation;

    public PointFEvaluator() {
        mInterpolation = new PointF();
    }

    public PointFEvaluator(PointF interpolation) {
        mInterpolation = interpolation;
    }

    @Override
    public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        final float currX = startValue.x + fraction * (endValue.x - startValue.x);
        final float currY = startValue.y + fraction * (endValue.y - startValue.y);
        mInterpolation.set(currX, currY);
        return mInterpolation;
    }
}