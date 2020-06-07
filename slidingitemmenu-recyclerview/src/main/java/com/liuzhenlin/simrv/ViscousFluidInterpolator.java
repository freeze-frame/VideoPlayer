package com.liuzhenlin.simrv;

import android.view.animation.Interpolator;

public class ViscousFluidInterpolator implements Interpolator {
    /** Controls the viscous fluid effect (how much of it). */
    private final float mViscousFluidScale;

    private final float mViscousFluidNormalize;
    private final float mViscousFluidOffset;

    public ViscousFluidInterpolator() {
        this(8.0f);
    }

    public ViscousFluidInterpolator(float viscousFluidScale) {
        mViscousFluidScale = viscousFluidScale;
        // must be set to 1.0 (used in viscousFluid())
        mViscousFluidNormalize = 1.0f / viscousFluid(1.0f);
        // account for very small floating-point error
        mViscousFluidOffset = 1.0f - mViscousFluidNormalize * viscousFluid(1.0f);
    }

    private float viscousFluid(float x) {
        x *= mViscousFluidScale;
        if (x < 1.0f) {
            x -= (1.0f - (float) Math.exp(-x));
        } else {
            float start = 0.36787944117f;   // 1/e == exp(-1)
            x = 1.0f - (float) Math.exp(1.0f - x);
            x = start + x * (1.0f - start);
        }
        return x;
    }

    @Override
    public float getInterpolation(float input) {
        final float interpolated = mViscousFluidNormalize * viscousFluid(input);
        if (interpolated > 0) {
            return interpolated + mViscousFluidOffset;
        }
        return interpolated;
    }
}