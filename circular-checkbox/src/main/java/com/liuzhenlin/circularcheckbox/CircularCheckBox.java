/*
 * Created on 2018/08/09.
 * Copyright © 2018–2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.circularcheckbox;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Checkable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A circular CheckBox with animation for Android
 *
 * @author 刘振林
 */
public class CircularCheckBox extends View implements Checkable {
    private static final String KEY_INSTANCE_STATE = "InstanceState";

    /*synthetic*/ boolean mIsChecked;

    private final int mDefaultDrawingSize; // px
    private static final int DEF_DRAWING_SIZE = 20; // dp

    private final PointF mRawCenterPoint = new PointF();
    private final PointF mCenterPoint = new PointF();

    private static final int COLOR_STROKE = 0xFF_DFDFDF;
    private static final int COLOR_SOLID_UNCHECKED = Color.WHITE;
    private static final int COLOR_RING_CHECKED = 0xFF_FF4081;
    private static final int COLOR_TICK = Color.WHITE;

    private final Paint mRingPaint;
    //private final RectF mRingBounds = new RectF();
    private float mDrawingRingOuterCircleScale = 1.0f;
    private float mDrawingRingInnerCircleScale;
    private float mStrokeInnerCircleScale; // Ring inner circle scale when this view is unchecked
    private float mStrokeWidth;  // Ring width when this view is unchecked
    private int mCheckedRingColor;
    private int mStrokeColor; // Ring color when this check box is unchecked
    private int mRingColor;

    private final Paint mUncheckedSolidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PointF[] mTickKeyPoints = {
            new PointF(), new PointF(), new PointF()
    };
    private final Path mTickPath = new Path();
    private float mTickLeftPartLength, mTickRightPartLength, mTickLength;
    private float mTickStrokeWidth;
    private boolean mNeedDrawTick;

    private ValueAnimator mAnimator;
    protected static final Interpolator sLinearInterpolator = new LinearInterpolator();
    private static final String PROPERTY_DRAWING_RING_OUTER_CIRCLE_SCALE = "drocs";
    private static final String PROPERTY_DRAWING_RING_INNER_CIRCLE_SCALE = "drics";
    private int mDuration; // ms
    private static final int DEF_DURATION = 256; // ms

    private final Runnable mDrawTickRunnable = () -> {
        mNeedDrawTick = true;
        invalidate();
    };
    private final Runnable mRedrawRunnable = this::invalidate;

    private Runnable mPostedSetCheckedRunnable;

    private OnCheckedChangeListener mListener;

    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener l) {
        mListener = l;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChange(@NonNull CircularCheckBox checkBox, boolean checked);
    }

    @ColorInt
    public int getStrokeColor() {
        return mStrokeColor;
    }

    public void setStrokeColor(@ColorInt int color) {
        if (mStrokeColor != color) {
            mStrokeColor = color;
            if (!mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                mRingColor = mStrokeColor;
                invalidate();
            }
        }
    }

    @ColorInt
    public int getUncheckedSolidColor() {
        return mUncheckedSolidPaint.getColor();
    }

    public void setUncheckedSolidColor(@ColorInt int color) {
        if (mUncheckedSolidPaint.getColor() != color) {
            mUncheckedSolidPaint.setColor(color);
            if (!mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                invalidate();
            }
        }
    }

    @ColorInt
    public int getCheckedRingColor() {
        return mCheckedRingColor;
    }

    public void setCheckedRingColor(@ColorInt int color) {
        if (mCheckedRingColor != color) {
            mCheckedRingColor = color;
            if (mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                mRingColor = mCheckedRingColor;
                invalidate();
            }
        }
    }

    @ColorInt
    public int getTickColor() {
        return mTickPaint.getColor();
    }

    public void setTickColor(@ColorInt int color) {
        if (mTickPaint.getColor() != color) {
            mTickPaint.setColor(color);
            if (mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                invalidate();
            }
        }
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    public void setStrokeWidth(float width) {
        setStrokeWidthInternal(width, true);
    }

    /*package*/ void setStrokeWidthInternal(float width, boolean invalidate) {
        if (mCenterPoint.x > 0) {
            final float old = mStrokeWidth;
            mStrokeWidth = Math.max(1.0f, Math.min(width, mCenterPoint.x / 5f));

            mStrokeInnerCircleScale = (mCenterPoint.x - mStrokeWidth) / mCenterPoint.x;

            if (!mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                mDrawingRingInnerCircleScale = mStrokeInnerCircleScale;
                if (invalidate && mStrokeWidth != old) {
                    invalidate();
                }
            }
        } else {
            mStrokeWidth = width;
        }
    }

    public float getTickStrokeWidth() {
        return mTickStrokeWidth;
    }

    public void setTickStrokeWidth(float width) {
        setTickStrokeWidthInternal(width, true);
    }

    /*package*/ void setTickStrokeWidthInternal(float width, boolean invalidate) {
        if (mCenterPoint.x > 0) {
            mTickStrokeWidth = Math.max(3.0f, Math.min(width, mCenterPoint.x / 2.5f));

            if (mTickPaint.getStrokeWidth() != mTickStrokeWidth) {
                mTickPaint.setStrokeWidth(mTickStrokeWidth);
                if (invalidate && mIsChecked && (mAnimator == null || !mAnimator.isRunning())) {
                    invalidate();
                }
            }
        } else {
            mTickStrokeWidth = width;
        }
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        if (mAnimator != null) {
            mAnimator.setDuration(duration);
        }
        mDuration = duration;
    }

    public CircularCheckBox(Context context) {
        this(context, null);
    }

    public CircularCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CircularCheckBox);
        mStrokeColor = ta.getColor(R.styleable
                .CircularCheckBox_color_stroke, COLOR_STROKE);
        mUncheckedSolidPaint.setColor(ta.getColor(R.styleable
                .CircularCheckBox_color_solid_unchecked, COLOR_SOLID_UNCHECKED));
        mCheckedRingColor = ta.getColor(R.styleable
                .CircularCheckBox_color_ring_checked, COLOR_RING_CHECKED);
        mTickPaint.setColor(ta.getColor(R.styleable.CircularCheckBox_color_tick, COLOR_TICK));
        mStrokeWidth = ta.getDimensionPixelSize(R.styleable
                .CircularCheckBox_strokeWidth, Utils.dp2px(context, 1));
        mTickStrokeWidth = ta.getDimensionPixelSize(R.styleable
                .CircularCheckBox_tickStrokeWidth, Utils.dp2px(context, 2));
        mDuration = ta.getInt(R.styleable.CircularCheckBox_duration, DEF_DURATION);
        ta.recycle();

        mRingColor = mStrokeColor;
        mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRingPaint.setStyle(Paint.Style.STROKE);

        mUncheckedSolidPaint.setStyle(Paint.Style.FILL);

        mTickPaint.setStyle(Paint.Style.STROKE);
        mTickPaint.setStrokeCap(Paint.Cap.ROUND);

        mDefaultDrawingSize = Utils.dp2px(context, DEF_DRAWING_SIZE);

        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putBoolean(KEY_INSTANCE_STATE, isChecked());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            setChecked(bundle.getBoolean(KEY_INSTANCE_STATE));
            state = bundle.getParcelable(KEY_INSTANCE_STATE);
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    public boolean performClick() {
        toggle(true);

        final boolean handled = super.performClick();
        if (!handled) {
            // View only makes a sound effect if the onClickListener was called, so we'll need
            // to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK);
        }

        return handled;
    }

    /**
     * Change the checked state of this view to the inverse of its current state
     * through animator or not.
     */
    public void toggle(boolean animate) {
        setChecked(!isChecked(), animate);
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mIsChecked != checked) {
            mIsChecked = checked;
            if (mListener != null) {
                mListener.onCheckedChange(this, checked);
            }

            cancelRunningAnimationAndRemoveDrawTickPendingActions();

            reset();
            if (mStrokeInnerCircleScale > 0) {
                invalidate();
            }
        }
    }

    /**
     * Change the checked state of this view
     *
     * @param checked the new checked state
     * @param animate whether change with animation or not
     */
    public void setChecked(boolean checked, boolean animate) {
        if (mIsChecked != checked) {
            if (animate) {
                mIsChecked = checked;
                if (mListener != null) {
                    mListener.onCheckedChange(this, checked);
                }

                mNeedDrawTick = false;
                mTickLength = 0;
                if (checked) {
                    if (mStrokeInnerCircleScale > 0) {
                        startCheckedAnimation();

                    } else if (mPostedSetCheckedRunnable == null) {
                        mPostedSetCheckedRunnable = () -> {
                            mPostedSetCheckedRunnable = null;
                            startCheckedAnimation();
                        };
                        post(mPostedSetCheckedRunnable);
                    }
                } else {
                    if (mPostedSetCheckedRunnable != null) {
                        removeCallbacks(mPostedSetCheckedRunnable);
                        mPostedSetCheckedRunnable = null;
                    }
                    startUncheckedAnimation();
                }
            } else {
                setChecked(checked);
            }
        }
    }

    private void reset() {
        if (mIsChecked) {
            mRingColor = mCheckedRingColor;
            mDrawingRingOuterCircleScale = 1.0f;
            mDrawingRingInnerCircleScale = 0;
            mNeedDrawTick = true;
            mTickLength = mTickLeftPartLength + mTickRightPartLength;
        } else {
            mRingColor = mStrokeColor;
            mDrawingRingOuterCircleScale = 1.0f;
            mDrawingRingInnerCircleScale = mStrokeInnerCircleScale;
            mNeedDrawTick = false;
            mTickLength = 0;
        }
    }

    private int measureSize(int measureSpec) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                return Math.min(mDefaultDrawingSize, specSize);
            case MeasureSpec.EXACTLY:
                return specSize;
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int measuredWidth = measureSize(widthMeasureSpec);
        int measuredHeight = measureSize(heightMeasureSpec);
        final int measuredSize = Math.min(measuredWidth, measuredHeight);
        measuredWidth = measuredSize + getPaddingLeft() + getPaddingRight();
        measuredHeight = measuredSize + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        final int width = right - left;
        final int height = bottom - top;
        final int availableWidth = width - paddingLeft - paddingRight;
        final int availableHeight = height - paddingTop - paddingBottom;

        mCenterPoint.x = availableWidth / 2f;
        mCenterPoint.y = availableHeight / 2f;
        mRawCenterPoint.x = paddingLeft + mCenterPoint.x;
        mRawCenterPoint.y = paddingTop + mCenterPoint.x;

        setStrokeWidthInternal(mStrokeWidth, false);

        setTickStrokeWidthInternal(mTickStrokeWidth, false);

        mTickKeyPoints[0].x = paddingLeft + availableWidth / 30f * 7f;
        mTickKeyPoints[0].y = paddingTop + availableHeight / 30f * 14f;
        mTickKeyPoints[1].x = paddingLeft + availableWidth / 30f * 13f;
        mTickKeyPoints[1].y = paddingTop + availableHeight / 30f * 20f;
        mTickKeyPoints[2].x = paddingLeft + availableWidth / 30f * 22f;
        mTickKeyPoints[2].y = paddingTop + availableHeight / 30f * 10f;

        mTickLeftPartLength = (float) Math.sqrt(
                Math.pow(mTickKeyPoints[1].x - mTickKeyPoints[0].x, 2)
                        + Math.pow(mTickKeyPoints[1].y - mTickKeyPoints[0].y, 2));
        mTickRightPartLength = (float) Math.sqrt(
                Math.pow(mTickKeyPoints[2].x - mTickKeyPoints[1].x, 2)
                        + Math.pow(mTickKeyPoints[2].y - mTickKeyPoints[1].y, 2));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawRing(canvas);
        drawUncheckedSolid(canvas);
        drawTick(canvas);
    }

    private void drawRing(Canvas canvas) {
        final float strokeWidth = mCenterPoint.x *
                (mDrawingRingOuterCircleScale - mDrawingRingInnerCircleScale);
        final float strokeCenterToPadding = strokeWidth / 2f
                + (1.0f - mDrawingRingOuterCircleScale) * mCenterPoint.x;
//        final float left = strokeCenterToPadding + getPaddingLeft();
//        final float top = strokeCenterToPadding + getPaddingTop();
//        final float right = getWidth() - strokeCenterToPadding - getPaddingRight();
//        final float bottom = getHeight() - strokeCenterToPadding - getPaddingBottom();
//        mRingBounds.set(left, top, right, bottom);

        mRingPaint.setColor(mRingColor);
        mRingPaint.setStrokeWidth(strokeWidth);
//        canvas.drawArc(mRingBounds, 0, 360, false, mRingPaint);
        canvas.drawCircle(mRawCenterPoint.x, mRawCenterPoint.y,
                mCenterPoint.x - strokeCenterToPadding,
                mRingPaint);
    }

    private void drawUncheckedSolid(Canvas canvas) {
        if (mDrawingRingInnerCircleScale > 0) {
            canvas.drawCircle(mRawCenterPoint.x, mRawCenterPoint.y,
                    mDrawingRingInnerCircleScale * mCenterPoint.x,
                    mUncheckedSolidPaint);
        }
    }

    private void drawTick(Canvas canvas) {
        if (!mNeedDrawTick) {
            return;
        }

        // Draw left of the tick
        //noinspection IfStatementWithIdenticalBranches
        if (mTickLength < mTickLeftPartLength) {
            final float step = Math.max(mCenterPoint.x / 10f, 5f);
            mTickLength += step;
            if (mTickLength > mTickLeftPartLength) {
                mTickLength = mTickLeftPartLength;
            }

            final float stopX = mTickKeyPoints[0].x + (mTickKeyPoints[1].x - mTickKeyPoints[0].x) *
                    mTickLength / mTickLeftPartLength;
            final float stopY = mTickKeyPoints[0].y + (mTickKeyPoints[1].y - mTickKeyPoints[0].y) *
                    mTickLength / mTickLeftPartLength;

            mTickPath.reset();
            mTickPath.moveTo(mTickKeyPoints[0].x, mTickKeyPoints[0].y);
            mTickPath.lineTo(stopX, stopY);
            canvas.drawPath(mTickPath, mTickPaint);

            // Draw right of the tick
        } else {
            mTickPath.reset();
            mTickPath.moveTo(mTickKeyPoints[0].x, mTickKeyPoints[0].y);
            mTickPath.lineTo(mTickKeyPoints[1].x, mTickKeyPoints[1].y);
            canvas.drawPath(mTickPath, mTickPaint);

            final float step = Math.max(mCenterPoint.x / 10f, 5f);
            mTickLength += step;
            if (mTickLength > mTickLeftPartLength + mTickRightPartLength) {
                mTickLength = mTickLeftPartLength + mTickRightPartLength;
            }

            final float stopX = mTickKeyPoints[1].x + (mTickKeyPoints[2].x - mTickKeyPoints[1].x) *
                    (mTickLength - mTickLeftPartLength) / mTickRightPartLength;
            final float stopY = mTickKeyPoints[1].y - (mTickKeyPoints[1].y - mTickKeyPoints[2].y) *
                    (mTickLength - mTickLeftPartLength) / mTickRightPartLength;

            mTickPath.reset();
            mTickPath.moveTo(mTickKeyPoints[1].x, mTickKeyPoints[1].y);
            mTickPath.lineTo(stopX, stopY);
            canvas.drawPath(mTickPath, mTickPaint);
        }

        // Continue drawing the tick
        if (mTickLength < mTickLeftPartLength + mTickRightPartLength) {
            postDelayed(mRedrawRunnable, 10);
        }
    }

    private void startCheckedAnimation() {
        cancelRunningAnimationAndRemoveDrawTickPendingActions();

        initAnimatorIfNeeded();
        mAnimator.start();

        // Delays to draw the tick
        postDelayed(mDrawTickRunnable, mDuration);
    }

    private void startUncheckedAnimation() {
        cancelRunningAnimationAndRemoveDrawTickPendingActions();

        initAnimatorIfNeeded();
        mAnimator.start();
    }

    private void initAnimatorIfNeeded() {
        if (mAnimator != null) return;

        final float[] defValues = {
                mStrokeInnerCircleScale, 0.5f, 0, 0 // Use 2/3 duration of the animation
        };
        final PropertyValuesHolder holder = PropertyValuesHolder.ofFloat(
                PROPERTY_DRAWING_RING_INNER_CIRCLE_SCALE, defValues);
        mAnimator = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofFloat(
                        PROPERTY_DRAWING_RING_OUTER_CIRCLE_SCALE, 1.0f, 0.8f, 1.0f),
                holder
        );
        mAnimator.setInterpolator(sLinearInterpolator);
        mAnimator.setDuration(mDuration);
        final Animator.AnimatorListener listener = new AnimatorListenerAdapter() {
            boolean firstStart = true;

            @Override
            public void onAnimationStart(Animator animation) {
                if (mIsChecked) {
                    if (!firstStart) {
                        holder.setFloatValues(defValues);
                    }
                    firstStart = false;
                } else {
                    holder.setFloatValues(defValues[defValues.length - 1], defValues[0]);
                }
            }
        };
        mAnimator.addListener(listener);
        mAnimator.addUpdateListener(animation -> {
            if (defValues[0] != mStrokeInnerCircleScale) {
                defValues[0] = mStrokeInnerCircleScale;
                // Update values holder
                listener.onAnimationStart(animation);
            }

            mDrawingRingOuterCircleScale = (float) animation.getAnimatedValue(
                    PROPERTY_DRAWING_RING_OUTER_CIRCLE_SCALE);
            mDrawingRingInnerCircleScale = (float) animation.getAnimatedValue(
                    PROPERTY_DRAWING_RING_INNER_CIRCLE_SCALE);
            if (mIsChecked) {
                mRingColor = Utils.getGradientColor(mStrokeColor, mCheckedRingColor,
                        1.0f - mDrawingRingInnerCircleScale / defValues[0]);
            } else {
                mRingColor = Utils.getGradientColor(mCheckedRingColor, mStrokeColor,
                        mDrawingRingInnerCircleScale / defValues[0]);
            }
            invalidate();
        });
    }

    private void cancelRunningAnimationAndRemoveDrawTickPendingActions() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        removeCallbacks(mRedrawRunnable);
        removeCallbacks(mDrawTickRunnable);
    }

//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        cancelRunningAnimationAndRemoveDrawTickPendingActions();
//        mIsChecked = false;
//        reset();
//    }
}