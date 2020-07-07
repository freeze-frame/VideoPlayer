/*
 * Created on 2019/5/19 10:43 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.util.Synthetic;
import com.google.android.exoplayer2.util.Util;
import com.liuzhenlin.texturevideoview.utils.BitmapUtils;
import com.liuzhenlin.texturevideoview.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 刘振林
 */
public class VideoClipView extends FrameLayout {
    private static final String TAG = "VideoClipView";

    private final RecyclerView mThumbGallery;
    private final ThumbsAdapter mThumbsAdapter = new ThumbsAdapter();

    /** @see #getThumbGalleryWidth() */
    private int mThumbGalleryWidth;
    /** @see #getThumbDisplayHeight() */
    @Synthetic final int mThumbDisplayHeight;

    private final Drawable mClipBackwards;
    private final Drawable mClipBackwardsDark;
    private final Drawable mClipForward;
    private final Drawable mClipForwardDark;

    private final int mDrawableWidth;

    /**
     * The offset to the left of the selection frame relative to the left of the horizontal gallery
     * as a percentage of the gallery width.
     */
    private float mFrameLeftOffset = Float.NaN; // [0, 1)
    /**
     * The offset (always positive) to the right of the selection frame relative to the right of
     * the horizontal gallery as a percentage of the gallery width.
     */
    private float mFrameRightOffset = Float.NaN; // [0, 1)

    /** Minimum spacing between 'Clip Backwards' and 'Clip Forward' buttons. */
    private float mMinimumClipBackwardsForwardGap;
    /** Maximum spacing between 'Clip Backwards' and 'Clip Forward' buttons. */
    private float mMaximumClipBackwardsForwardGap;

    public static final int DEFAULT_MIN_CLIP_DURATION = 3 * 1000; // ms
    public static final int DEFAULT_MAX_CLIP_DURATION = 120 * 1000; // ms
    public static final int DEFAULT_MIN_UNSELECTED_CLIP_DURATION = 8 * 1000; // ms

    /**
     * @see #getMinimumClipDuration()
     * @see #setMinimumClipDuration(int)
     */
    private int mMinimumClipDuration = DEFAULT_MIN_CLIP_DURATION;
    /**
     * @see #getMaximumClipDuration()
     * @see #setMaximumClipDuration(int)
     */
    private int mMaximumClipDuration = DEFAULT_MAX_CLIP_DURATION;
    /**
     * @see #getMinimumUnselectedClipDuration()
     * @see #setMinimumUnselectedClipDuration(int)
     */
    private int mMinimumUnselectedClipDuration = DEFAULT_MIN_UNSELECTED_CLIP_DURATION;

    private final int[] mTmpSelectionInterval = sNoSelectionInterval.clone();
    @Synthetic static final int[] sNoSelectionInterval = {0, 0};

    private boolean mFirstLayout = true;
    private boolean mInLayout;

    private int mLayoutDirection = ViewCompat.LAYOUT_DIRECTION_LTR;

    private final Paint mFrameBarPaint;
    private final int mFrameBarHeight;
    private final int mFrameBarColor;
    private final int mFrameBarDarkColor;

    private final Paint mProgressPaint;
    private final float mProgressStrokeWidth;
    private final float mProgressHeaderFooterStrokeWidth;
    private final float mProgressHeaderFooterLength;

    /** The selected millisecond position as a percentage of the selectable time interval. */
    private float mProgressPercent = Float.NaN; // [0, 1]

    /**
     * Offset between the touch point x coordinate and the horizontal center position of the
     * progress cursor while it is being dragged by the user.
     */
    private float mProgressMoveOffset = Float.NaN;

    protected final float mDip;
    protected final int mTouchSlop;

    private int mActivePointerId;
    private float mDownX;
    private float mDownY;
    private final float[] mTouchX = new float[2];
    private final float[] mTouchY = new float[2];

    private int mTouchFlags;
    private static final int TFLAG_FRAME_LEFT_BEING_DRAGGED = 1;
    private static final int TFLAG_FRAME_RIGHT_BEING_DRAGGED = 1 << 1;
    private static final int TFLAG_FRAME_BEING_DRAGGED = 0b0011;
    private static final int TFLAG_PROGRESS_BEING_DRAGGED = 1 << 2;
    private static final int TOUCH_MASK = 0b0111;

    private List<OnSelectionChangeListener> mOnSelectionChangeListeners;

    /**
     * Lister for monitoring all the changes to the selection or selection interval of the video clip.
     */
    public interface OnSelectionChangeListener {
        /**
         * Notification that the user has started a touch gesture that could change the current
         * selection or/and selection interval of the video clip.
         */
        default void onStartTrackingTouch() {
        }

        /**
         * Gets notified when the selection interval of the video clip changes.
         *
         * @param start    start position in millisecond of the selected time interval
         * @param end      end position in millisecond of the selected time interval
         * @param fromUser true if the selection interval change was initiated by the user
         */
        default void onSelectionIntervalChange(int start, int end, boolean fromUser) {
        }

        /**
         * Gets notified when the selection of the video clip changes.
         *
         * @param start     start position in millisecond of the selected time interval
         * @param end       end position in millisecond of the selected time interval
         * @param selection position of the selected millisecond within the selection interval
         * @param fromUser  true if the selection change was initiated by the user
         */
        default void onSelectionChange(int start, int end, int selection, boolean fromUser) {
        }

        /**
         * Notification that the user has finished a touch gesture that could have changed the
         * selection or/and selection interval of the video clip.
         */
        default void onStopTrackingTouch() {
        }
    }

    public void addOnSelectionChangeListener(@NonNull OnSelectionChangeListener listener) {
        if (mOnSelectionChangeListeners == null) {
            mOnSelectionChangeListeners = new ArrayList<>(1);
        }
        if (!mOnSelectionChangeListeners.contains(listener)) {
            mOnSelectionChangeListeners.add(listener);
        }
    }

    public void removeOnSelectionChangeListener(@Nullable OnSelectionChangeListener listener) {
        if (hasOnSelectionChangeListener()) {
            mOnSelectionChangeListeners.remove(listener);
        }
    }

    private boolean hasOnSelectionChangeListener() {
        return mOnSelectionChangeListeners != null && !mOnSelectionChangeListeners.isEmpty();
    }

    private void notifyListenersWhenSelectionDragStarts() {
        if (hasOnSelectionChangeListener()) {
            // Since onStartTrackingTouch() is implemented by the app, it could do anything,
            // including removing itself from {@link mOnSelectionChangeListeners} — and that could
            // cause problems if an iterator is used on the ArrayList {@link mOnSelectionChangeListeners}.
            // To avoid such problems, just march thru the list in the reverse order.
            for (int i = mOnSelectionChangeListeners.size() - 1; i >= 0; i--) {
                mOnSelectionChangeListeners.get(i).onStartTrackingTouch();
            }
        }
    }

    private void notifyListenersWhenSelectionDragStops() {
        if (hasOnSelectionChangeListener()) {
            // Since onStopTrackingTouch() is implemented by the app, it could do anything,
            // including removing itself from {@link mOnSelectionChangeListeners} — and that could
            // cause problems if an iterator is used on the ArrayList {@link mOnSelectionChangeListeners}.
            // To avoid such problems, just march thru the list in the reverse order.
            for (int i = mOnSelectionChangeListeners.size() - 1; i >= 0; i--) {
                mOnSelectionChangeListeners.get(i).onStopTrackingTouch();
            }
        }
    }

    private void notifyListenersOfSelectionIntervalChange(boolean fromUser) {
        if (hasOnSelectionChangeListener()) {
            final int[] interval = mTmpSelectionInterval;
            getSelectionInterval(interval);
            // Since onSelectionIntervalChange() is implemented by the app, it could do anything,
            // including removing itself from {@link mOnSelectionChangeListeners} — and that could
            // cause problems if an iterator is used on the ArrayList {@link mOnSelectionChangeListeners}.
            // To avoid such problems, just march thru the list in the reverse order.
            for (int i = mOnSelectionChangeListeners.size() - 1; i >= 0; i--) {
                mOnSelectionChangeListeners.get(i)
                        .onSelectionIntervalChange(interval[0], interval[1], fromUser);
            }
        }
    }

    private void notifyListenersOfSelectionChange(boolean fromUser) {
        if (hasOnSelectionChangeListener()) {
            final int[] interval = mTmpSelectionInterval;
            getSelectionInterval(interval);
            final int selection = getSelection();
            // Since onSelectionChange() is implemented by the app, it could do anything,
            // including removing itself from {@link mOnSelectionChangeListeners} — and that could
            // cause problems if an iterator is used on the ArrayList {@link mOnSelectionChangeListeners}.
            // To avoid such problems, just march thru the list in the reverse order.
            for (int i = mOnSelectionChangeListeners.size() - 1; i >= 0; i--) {
                mOnSelectionChangeListeners.get(i)
                        .onSelectionChange(interval[0], interval[1], selection, fromUser);
            }
        }
    }

    public VideoClipView(@NonNull Context context) {
        this(context, null);
    }

    public VideoClipView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoClipView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDip = getResources().getDisplayMetrics().density;
        mFrameBarHeight = (int) (2.5f * mDip + 0.5f);
        mProgressStrokeWidth = 3.0f * mDip;
        mProgressHeaderFooterStrokeWidth = 1.8f * mDip;
        mProgressHeaderFooterLength = 8.0f * mDip;

        mClipBackwards = ContextCompat.getDrawable(context, R.drawable.ic_clip_backwards);
        mClipBackwardsDark = ContextCompat.getDrawable(context, R.drawable.ic_clip_backwards_dark);
        mClipForward = ContextCompat.getDrawable(context, R.drawable.ic_clip_forward);
        mClipForwardDark = ContextCompat.getDrawable(context, R.drawable.ic_clip_forward_dark);

        //noinspection ConstantConditions
        mDrawableWidth = mClipBackwards.getIntrinsicWidth();
        mThumbDisplayHeight = mClipBackwards.getIntrinsicHeight();

        mFrameBarColor = BitmapUtils.getDominantColorOrThrow(BitmapUtils.drawableToBitmap(mClipBackwards));
        //noinspection ConstantConditions
        mFrameBarDarkColor = BitmapUtils.getDominantColorOrThrow(BitmapUtils.drawableToBitmap(mClipBackwardsDark));

        mFrameBarPaint = new Paint();
        mFrameBarPaint.setStyle(Paint.Style.FILL);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        mProgressPaint.setStyle(Paint.Style.STROKE);

        final int verticalEndPadding = (int) (4.0f * mDip + 0.5f);
        super.setPadding(mDrawableWidth, verticalEndPadding, mDrawableWidth, verticalEndPadding);

        View.inflate(context, R.layout.view_videoclip, this);
        mThumbGallery = findViewById(R.id.rv_videoclip_thumbs);
        mThumbGallery.setMinimumHeight(mThumbDisplayHeight);
        mThumbGallery.setHasFixedSize(true);
        mThumbGallery.setAdapter(mThumbsAdapter);

        if (InternalConsts.DEBUG_LISTENER) {
            addOnSelectionChangeListener(new OnSelectionChangeListener() {
                @Override
                public void onStartTrackingTouch() {
                    Log.d(TAG, "---------- onStartTrackingTouch ----------");
                }

                @Override
                public void onSelectionIntervalChange(int start, int end, boolean fromUser) {
                    Log.d(TAG, "onSelectionIntervalChange: " + start + "    " + end + "    " + fromUser);
                }

                @Override
                public void onSelectionChange(int start, int end, int selection, boolean fromUser) {
                    Log.d(TAG, "onSelectionChange: " +
                            start + "    " + end + "    " + selection + "    " + fromUser);
                }

                @Override
                public void onStopTrackingTouch() {
                    Log.d(TAG, "---------- onStopTrackingTouch ----------");
                }
            });
        }
    }

    /**
     * @return the unified display height for the thumbnails inside the horizontal gallery
     */
    public int getThumbDisplayHeight() {
        return mThumbDisplayHeight;
    }

    /**
     * Gets the width of the horizontal gallery used for displaying the thumbnails retrieved from
     * the video being clipped or 0 if this view is not laid-out, through which you can decide
     * the minimum count of thumbnails that can full fill the gallery.
     */
    public int getThumbGalleryWidth() {
        return mThumbGalleryWidth;
    }

    /** @return minimum lasting time for a selected clip */
    public int getMinimumClipDuration() {
        return mMinimumClipDuration;
    }

    /** Sets the minimum lasting time for a selected clip. */
    public void setMinimumClipDuration(int duration) {
        duration = Util.constrainValue(duration, 0, mMaximumClipDuration);
        if (mMinimumClipDuration != duration) {
            mMinimumClipDuration = duration;
            onSetClipDuration();
        }
    }

    /** @return maximum lasting time for a selected clip */
    public int getMaximumClipDuration() {
        return mMaximumClipDuration;
    }

    /** Sets the maximum lasting time for a selected clip. */
    public void setMaximumClipDuration(int duration) {
        duration = Math.max(duration, mMinimumClipDuration);
        if (mMaximumClipDuration != duration) {
            mMaximumClipDuration = duration;
            onSetClipDuration();
        }
    }

    /** @return minimum duration for the clip(s) outside the selected time interval */
    public int getMinimumUnselectedClipDuration() {
        return mMinimumUnselectedClipDuration;
    }

    /** Sets the minimum duration of the clip(s) outside the selected time interval. */
    public void setMinimumUnselectedClipDuration(int duration) {
        duration = Math.max(duration, 0);
        if (mMinimumUnselectedClipDuration != duration) {
            mMinimumUnselectedClipDuration = duration;
            onSetClipDuration();
        }
    }

    private void onSetClipDuration() {
        resolveFrameOffsets();
        resetProgressPercent(false);

        final boolean laidout = ViewCompat.isLaidOut(this);
        if (!laidout && !mFirstLayout || laidout && !mInLayout) {
            requestLayout();
            invalidate();
        }
    }

    private void resolveFrameOffsets() {
        final int[] oldInterval = mTmpSelectionInterval.clone();
        // Not to take the selection interval upon the first resolution of the offsets to ensure
        // the OnSelectionChangeListeners to get notified for the interval change.
        if (!Arrays.equals(oldInterval, sNoSelectionInterval)) {
            getSelectionInterval(oldInterval);
        }

        // Prefers assuming a video clip selected with half its maximum duration the first time
        // the view shows to the user to choosing another one using the minimum period of time.
        final float percent = 1.0f -
                Math.max(mMinimumClipDuration, mMaximumClipDuration / 2f)
                        / (mMaximumClipDuration + mMinimumUnselectedClipDuration);
        if (Utils.isLayoutRtl(this)) {
            mFrameLeftOffset = percent;
            mFrameRightOffset = 0;
        } else {
            mFrameLeftOffset = 0;
            mFrameRightOffset = percent;
        }

        getSelectionInterval(mTmpSelectionInterval);
        // Note that exact floating point equality may not be guaranteed for a theoretically
        // idempotent operation; for example, there are many cases where a + b - b != a.
        if (!Arrays.equals(mTmpSelectionInterval, oldInterval)) {
            notifyListenersOfSelectionIntervalChange(false);
        }
    }

    @Override
    public final void setPadding(int left, int top, int right, int bottom) {
        // no-op
    }

    @Override
    public final void setPaddingRelative(int start, int top, int end, int bottom) {
        // no-op
    }

    @Override
    public final void setClipToPadding(boolean clipToPadding) {
        // no-op
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Float.isNaN(mFrameLeftOffset)) {
            resolveFrameOffsets();
            resetProgressPercent(false);
        } else // Layout direction changes between left-to-right and right-to-left
            if (mLayoutDirection != layoutDirection) {
                mLayoutDirection = layoutDirection;
                // Swap the frame left offset with the right one
                final float tmp = mFrameRightOffset;
                mFrameRightOffset = mFrameLeftOffset;
                mFrameLeftOffset = tmp;
            }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mInLayout = true;
        super.onLayout(changed, left, top, right, bottom);
        mInLayout = false;
        mFirstLayout = false;

        mThumbGalleryWidth = mThumbGallery.getWidth();
        mMaximumClipBackwardsForwardGap = mThumbGalleryWidth *
                (float) mMaximumClipDuration / (mMaximumClipDuration + mMinimumUnselectedClipDuration);
        mMinimumClipBackwardsForwardGap = mMaximumClipBackwardsForwardGap *
                (float) mMinimumClipDuration / mMaximumClipDuration;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawFrame(canvas);
        // Skip drawing the progress cursor if some drawable is being dragged
        if ((mTouchFlags & TFLAG_FRAME_BEING_DRAGGED) == 0) {
            drawProgressCursor(canvas);
        }
    }

    private void drawFrame(Canvas canvas) {
        final boolean rtl = Utils.isLayoutRtl(this);
        final int width = getWidth();
        final int childTop = getPaddingTop();
        final int childBottom = getHeight() - getPaddingBottom();

        final float frameLeftOffsetPixels = mThumbGalleryWidth * mFrameLeftOffset;
        final float frameRightOffsetPixels = mThumbGalleryWidth * mFrameRightOffset;
        final float framebarLeft = mDrawableWidth + frameLeftOffsetPixels;
        final float framebarRight = width - mDrawableWidth - frameRightOffsetPixels;
//        final float framebarWidth = framebarRight - framebarLeft;
        final int leftDrawableLeft = (int) (frameLeftOffsetPixels + 0.5f);
        final int leftDrawableRight = (int) (framebarLeft + 0.5f);
        final int rightDrawableLeft = (int) (framebarRight + 0.5f);
        final int rightDrawableRight = (int) (width - frameRightOffsetPixels + 0.5f);

        final boolean dark;
        Drawable leftDrawable;
        Drawable rightDrawable;

//        if (Utils.areEqualIgnorePrecisionError(framebarWidth, mMinimumClipBackwardsForwardGap)
//                || Utils.areEqualIgnorePrecisionError(framebarWidth, mMaximumClipBackwardsForwardGap)) {
        final int[] interval = mTmpSelectionInterval;
        getSelectionInterval(interval);
        final int duration = interval[1] - interval[0];
        if (duration == mMinimumClipDuration || duration == mMaximumClipDuration) {
            dark = true;
            leftDrawable = rtl ? mClipForwardDark : mClipBackwardsDark;
            rightDrawable = rtl ? mClipBackwardsDark : mClipForwardDark;
        } else {
            dark = false;
            leftDrawable = rtl ? mClipForward : mClipBackwards;
            rightDrawable = rtl ? mClipBackwards : mClipForward;
        }

        leftDrawable.setBounds(leftDrawableLeft, childTop, leftDrawableRight, childBottom);
        rightDrawable.setBounds(rightDrawableLeft, childTop, rightDrawableRight, childBottom);
        // Draw left & right drawables
        leftDrawable.draw(canvas);
        rightDrawable.draw(canvas);

        // Draw top & bottom frame bars
        mFrameBarPaint.setColor(dark ? mFrameBarDarkColor : mFrameBarColor);
        canvas.drawRect(leftDrawableRight, childTop,
                rightDrawableLeft, childTop + mFrameBarHeight, mFrameBarPaint);
        canvas.drawRect(leftDrawableRight, childBottom - mFrameBarHeight,
                rightDrawableLeft, childBottom, mFrameBarPaint);
    }

    private void drawProgressCursor(Canvas canvas) {
        final int height = getHeight();

        final float progressCenterX = progressPercentToProgressCenterX(mProgressPercent);
        //noinspection SuspiciousNameCombination,UnnecessaryLocalVariable
        final float progressTop = mProgressHeaderFooterStrokeWidth;
        final float progressBottom = height - mProgressHeaderFooterStrokeWidth;
        // Draw the progress
        mProgressPaint.setStrokeWidth(mProgressStrokeWidth);
        canvas.drawLine(progressCenterX, progressTop, progressCenterX, progressBottom, mProgressPaint);

        final float headerFooterStart = progressCenterX - mProgressHeaderFooterLength / 2f;
        final float headerFooterEnd = headerFooterStart + mProgressHeaderFooterLength;
        final float halfOfProgressHeaderFooterStrokeWidth = mProgressHeaderFooterStrokeWidth / 2f;
        //noinspection SuspiciousNameCombination,UnnecessaryLocalVariable
        final float headerCenterY = halfOfProgressHeaderFooterStrokeWidth;
        final float footerCenterY = height - halfOfProgressHeaderFooterStrokeWidth;
        // Draw header & footer of the progress
        mProgressPaint.setStrokeWidth(mProgressHeaderFooterStrokeWidth);
        canvas.drawLine(headerFooterStart, headerCenterY, headerFooterEnd, headerCenterY, mProgressPaint);
        canvas.drawLine(headerFooterStart, footerCenterY, headerFooterEnd, footerCenterY, mProgressPaint);
    }

    private float progressCenterXToProgressPercent(float progressCenterX) {
        final float range = mThumbGalleryWidth;
        final float hopsw = mProgressStrokeWidth / 2f;
        final float min = mDrawableWidth + mFrameLeftOffset * range + hopsw;
        final float max = mDrawableWidth + (1.0f - mFrameRightOffset) * range - hopsw;
        if (Utils.isLayoutRtl(this)) {
            if (progressCenterX < min) {
                // Calculates the percentage to the left side of the progress cursor located
                // next to the left drawable in right-to-left layout direction.
                return (range - (min - hopsw - mDrawableWidth)) / range;
            } else {
                progressCenterX = Util.constrainValue(progressCenterX, min, max);
                return (range - (progressCenterX + hopsw - mDrawableWidth)) / range;
            }
        } else {
            if (progressCenterX > max) {
                // Calculates the percentage to the right side of the progress cursor located
                // next to the right drawable in left-to-right layout direction.
                return (max + hopsw - mDrawableWidth) / range;
            } else {
                progressCenterX = Util.constrainValue(progressCenterX, min, max);
                return (progressCenterX - hopsw - mDrawableWidth) / range;
            }
        }
    }

    private float progressPercentToProgressCenterX(float progressPercent) {
        final float range = mThumbGalleryWidth;
        final float hopsw = mProgressStrokeWidth / 2f;
        if (Utils.isLayoutRtl(this)) {
            return Math.max(mDrawableWidth + (1.0f - progressPercent) * range - hopsw,
                    mDrawableWidth + mFrameLeftOffset * range + hopsw);
        } else {
            return Math.min(mDrawableWidth + progressPercent * range + hopsw,
                    mDrawableWidth + (1.0f - mFrameRightOffset) * range - hopsw);
        }
    }

    private void resetProgressPercent(boolean fromUser) {
        if (Utils.isLayoutRtl(this)) {
            setProgressPercent(mFrameRightOffset, fromUser);
        } else {
            setProgressPercent(mFrameLeftOffset, fromUser);
        }
    }

    private void setProgressPercent(float percent, boolean fromUser) {
        if (Float.isNaN(mFrameLeftOffset)) {
            resolveFrameOffsets();
        }
        final boolean rtl = Utils.isLayoutRtl(this);
        final float min = rtl ? mFrameRightOffset : mFrameLeftOffset;
        final float max = 1.0f - (rtl ? mFrameLeftOffset : mFrameRightOffset);
        percent = Util.constrainValue(percent, min, max);
        if (!Utils.areEqualIgnorePrecisionError(mProgressPercent, percent)) {
            mProgressPercent = percent;
            notifyListenersOfSelectionChange(fromUser);
        }
    }

    /**
     * @return position of the selected millisecond within the selectable time interval
     */
    public int getSelection() {
        return (int) ((mMaximumClipDuration + mMinimumUnselectedClipDuration) * mProgressPercent + 0.5f);
    }

    /**
     * Sets the selection for the video clip.
     *
     * @param selection millisecond position within the selectable time interval
     */
    public void setSelection(int selection) {
        final float old = mProgressPercent;
        final float percent = (float) selection / (mMaximumClipDuration + mMinimumUnselectedClipDuration);
        setProgressPercent(percent, false);
        if (!Utils.areEqualIgnorePrecisionError(mProgressPercent, old)) {
            invalidate(); // Redraw progress cursor
        }
    }

    /**
     * Gets the time interval in millisecond of the selected video clip by providing an array
     * of two integers that will hold the start and end value in that order.
     */
    public void getSelectionInterval(int[] outInterval) {
        if (outInterval == null || outInterval.length < 2) {
            throw new IllegalArgumentException("outInterval must be an array of two integers");
        }
        // The frame offset properties may have not been determined yet
        if (Float.isNaN(mFrameLeftOffset)) {
            outInterval[0] = 0;
            outInterval[1] = Math.max((int) (mMaximumClipDuration / 2f + 0.5f), mMinimumClipDuration);
        } else {
            final boolean rtl = Utils.isLayoutRtl(this);
            final float duration = mMaximumClipDuration + mMinimumUnselectedClipDuration;
            outInterval[0] = (int) (0.5f + duration *
                    (rtl ? mFrameRightOffset : mFrameLeftOffset));
            outInterval[1] = (int) (0.5f + duration *
                    (1.0f - (rtl ? mFrameLeftOffset : mFrameRightOffset)));
        }
    }

    /**
     * Sets the time interval selected for video clip purpose
     *
     * @param start start value in millisecond of the interval
     * @param end   end value in millisecond of the interval
     */
    public void setSelectionInterval(int start, int end) {
        final int interval = end - start;
        final int duration = mMaximumClipDuration + mMinimumUnselectedClipDuration;

        // Checks the desired selection interval and its start and end values
        if (end < start) {
            throw new IllegalArgumentException("Interval end value is less than the interval start value");
        }
        if (start < 0) {
            throw new IllegalArgumentException("Start millisecond of the desired selection interval " +
                    "cannot be less than 0");
        }
        if (end > duration) {
            throw new IllegalArgumentException("End millisecond of the desired selection interval " +
                    "is out of the selectable time interval range");
        }
        if (interval < mMinimumClipDuration) {
            throw new IllegalArgumentException("Desired selection interval cannot be less than " +
                    "the minimum clip duration");
        }
        if (interval > mMaximumClipDuration) {
            throw new IllegalArgumentException("Desired selection interval cannot be greater than " +
                    "the maximum clip duration");
        }

        final float frameLeftOffset = mFrameLeftOffset;
        final float frameRightOffset = mFrameRightOffset;
        if (Utils.isLayoutRtl(this)) {
            mFrameRightOffset = (float) start / duration;
            mFrameLeftOffset = 1.0f - (float) end / duration;
        } else {
            mFrameLeftOffset = (float) start / duration;
            mFrameRightOffset = 1.0f - (float) end / duration;
        }
        if (!Utils.areEqualIgnorePrecisionError(mFrameLeftOffset, frameLeftOffset)
                || !Utils.areEqualIgnorePrecisionError(mFrameRightOffset, frameRightOffset)) {
            notifyListenersOfSelectionIntervalChange(false);
            resetProgressPercent(false);
            invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!onPointerMove(ev)) {
                    break;
                }

                if (tryHandleTouchEvent()) {
                    notifyListenersWhenSelectionDragStarts();
                    return true;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if ((mTouchFlags & TOUCH_MASK) != 0) {
                    notifyListenersWhenSelectionDragStops();
                }
                resetTouch();
                break;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!onPointerMove(event)) {
                    return false;
                }

                if ((mTouchFlags & TOUCH_MASK) == 0) {
                    if (tryHandleTouchEvent()) {
                        notifyListenersWhenSelectionDragStarts();
                    }
                } else {
                    final float x = mTouchX[mTouchX.length - 1];
                    final float lastX = mTouchX[mTouchX.length - 2];
                    final float deltaX = x - lastX;

                    boolean invalidateNeeded = false;
                    if ((mTouchFlags & TFLAG_PROGRESS_BEING_DRAGGED) != 0) {
                        final float old = mProgressPercent;
                        final float percent = progressCenterXToProgressPercent(x + mProgressMoveOffset);
                        setProgressPercent(percent, true);
                        invalidateNeeded = !Utils.areEqualIgnorePrecisionError(mProgressPercent, old);

                    } else if ((mTouchFlags & TFLAG_FRAME_BEING_DRAGGED) != 0) {
                        final float frameLeftOffset = mFrameLeftOffset;
                        final float frameRightOffset = mFrameRightOffset;

//                        final float frameLeftOffsetPixels = mThumbGalleryWidth * frameLeftOffset;
//                        final float frameRightOffsetPixels = mThumbGalleryWidth * frameRightOffset;
//                        final float framebarLeft = mDrawableWidth + frameLeftOffsetPixels;
//                        final float framebarRight = getWidth() - mDrawableWidth - frameRightOffsetPixels;
//                        final float framebarWidth = framebarRight - framebarLeft;
//                        final boolean framebarShortest =
//                                Utils.areEqualIgnorePrecisionError(
//                                        framebarWidth, mMinimumClipBackwardsForwardGap);
                        final int[] interval = mTmpSelectionInterval;
                        getSelectionInterval(interval);
                        final boolean framebarShortest =
                                interval[1] - interval[0] == mMinimumClipDuration;
                        if (deltaX > 0 && !framebarShortest || deltaX < 0 && framebarShortest) {
                            // Order is important here:
                            // 1) when touch goes from left to right: if the length of
                            // the selection frame has reached the maximum, its right can only
                            // be moved after the left offset increases;
                            // 2) when touch goes from right to left: if the length of
                            // the selection frame has been the shortest, its right can only
                            // be moved after the left offset decreases.
                            if ((mTouchFlags & TFLAG_FRAME_LEFT_BEING_DRAGGED) != 0) {
                                clampFrameLeftOffset(deltaX);
                            }
                            if ((mTouchFlags & TFLAG_FRAME_RIGHT_BEING_DRAGGED) != 0) {
                                clampFrameRightOffset(-deltaX);
                            }
                        } else if (deltaX < 0 && !framebarShortest || deltaX > 0 && framebarShortest) {
                            // Order is important here:
                            // 1) when touch goes from right to left: if the length of
                            // the selection frame has reached the maximum, its left can only
                            // be moved after the right offset increases;
                            // 2) when touch goes from left to right: if the length of
                            // the selection frame has been the shortest, its left can only
                            // be moved after the right offset decreases.
                            if ((mTouchFlags & TFLAG_FRAME_RIGHT_BEING_DRAGGED) != 0) {
                                clampFrameRightOffset(-deltaX);
                            }
                            if ((mTouchFlags & TFLAG_FRAME_LEFT_BEING_DRAGGED) != 0) {
                                clampFrameLeftOffset(deltaX);
                            }
                        }
                        if ((mTouchFlags & TFLAG_FRAME_BEING_DRAGGED) == TFLAG_FRAME_BEING_DRAGGED &&
                                !Utils.areEqualIgnorePrecisionError(
                                        mFrameLeftOffset - frameLeftOffset,
                                        frameRightOffset - mFrameRightOffset)) {
                            // Not allow inconsistent delta side offsets to the selection frame
                            // when it is being dragged overall.
                            mFrameLeftOffset = frameLeftOffset;
                            mFrameRightOffset = frameRightOffset;
                            break;
                        }

                        if (!Utils.areEqualIgnorePrecisionError(mFrameLeftOffset, frameLeftOffset)
                                || !Utils.areEqualIgnorePrecisionError(mFrameRightOffset, frameRightOffset)) {
                            invalidateNeeded = true;
                            notifyListenersOfSelectionIntervalChange(true);
                            // Resets the position of the progress cursor, in case it is out of our range.
                            resetProgressPercent(true);
                        }
                    }
                    if (invalidateNeeded) {
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if ((mTouchFlags & TOUCH_MASK) != 0) {
                    notifyListenersWhenSelectionDragStops();
                }
                resetTouch();
                break;
        }
        return true;
    }

    private void clampFrameLeftOffset(float deltaX) {
        final int originalRange = mThumbGalleryWidth;
        final float range = originalRange * (1.0f - mFrameRightOffset);
        mFrameLeftOffset = Util.constrainValue(
                mFrameLeftOffset * originalRange + deltaX,
                Math.max(0, range - mMaximumClipBackwardsForwardGap),
                range - mMinimumClipBackwardsForwardGap) / originalRange;
    }

    private void clampFrameRightOffset(float deltaX) {
        final float originalRange = mThumbGalleryWidth;
        final float range = originalRange * (1.0f - mFrameLeftOffset);
        mFrameRightOffset = Util.constrainValue(
                mFrameRightOffset * originalRange + deltaX,
                Math.max(0, range - mMaximumClipBackwardsForwardGap),
                range - mMinimumClipBackwardsForwardGap) / originalRange;
    }

    private boolean tryHandleTouchEvent() {
        final float frameLeft = mFrameLeftOffset * mThumbGalleryWidth;
        final float frameRight = getWidth() - mFrameRightOffset * mThumbGalleryWidth;
        if (mDownX >= frameLeft && mDownX <= frameRight) {
            if (mDownX <= /* leftDrawableRight */ frameLeft + mDrawableWidth) {
                if (checkTouchSlop()) {
                    mTouchFlags |= TFLAG_FRAME_LEFT_BEING_DRAGGED;
                    requestParentDisallowInterceptTouchEvent();
                    return true;
                }
            } else if (mDownX >= /* rightDrawableLeft */ frameRight - mDrawableWidth) {
                if (checkTouchSlop()) {
                    mTouchFlags |= TFLAG_FRAME_RIGHT_BEING_DRAGGED;
                    requestParentDisallowInterceptTouchEvent();
                    return true;
                }
            } else if (checkTouchSlop()) {
                mProgressMoveOffset = progressPercentToProgressCenterX(mProgressPercent) - mDownX;
                if (!Float.isNaN(mProgressMoveOffset)) {
                    final float absPMO = Math.abs(mProgressMoveOffset);
                    if (absPMO >= 0 && absPMO <= 25f * mDip) {
                        mTouchFlags |= TFLAG_PROGRESS_BEING_DRAGGED;
                    } else {
                        mTouchFlags |= TFLAG_FRAME_BEING_DRAGGED;
                    }
                    requestParentDisallowInterceptTouchEvent();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkTouchSlop() {
        final float absDx = Math.abs(mTouchX[mTouchX.length - 1] - mDownX);
        if (absDx > mTouchSlop) {
            final float absDy = Math.abs(mTouchY[mTouchY.length - 1] - mDownY);
            return absDx > absDy;
        }
        return false;
    }

    private void requestParentDisallowInterceptTouchEvent() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void onPointerDown(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        mActivePointerId = e.getPointerId(actionIndex);
        mDownX = e.getX(actionIndex);
        mDownY = e.getY(actionIndex);
        markCurrTouchPoint(mDownX, mDownY);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean onPointerMove(MotionEvent e) {
        final int pointerIndex = e.findPointerIndex(mActivePointerId);
        if (pointerIndex < 0) {
            Log.e(TAG, "Error processing scroll; pointer index for id "
                    + mActivePointerId + " not found. Did any MotionEvents get skipped?");
            return false;
        }
        markCurrTouchPoint(e.getX(pointerIndex), e.getY(pointerIndex));
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent e) {
        final int pointerIndex = e.getActionIndex();
        final int pointerId = e.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up.
            // Choose a new active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = e.getPointerId(newPointerIndex);
            mDownX = e.getX(newPointerIndex);
            mDownY = e.getY(newPointerIndex);
            markCurrTouchPoint(mDownX, mDownY);
        }
    }

    private void markCurrTouchPoint(float x, float y) {
        System.arraycopy(mTouchX, 1, mTouchX, 0, mTouchX.length - 1);
        mTouchX[mTouchX.length - 1] = x;
        System.arraycopy(mTouchY, 1, mTouchY, 0, mTouchY.length - 1);
        mTouchY[mTouchY.length - 1] = y;
    }

    private void resetTouch() {
        mTouchFlags &= ~TOUCH_MASK;
        mActivePointerId = MotionEvent.INVALID_POINTER_ID;
        mProgressMoveOffset = Float.NaN;
    }

    public void addThumbnail(@Nullable Bitmap thumb) {
        addThumbnail(mThumbsAdapter.thumbnails.size(), thumb);
    }

    public void addThumbnail(int index, @Nullable Bitmap thumb) {
        mThumbsAdapter.thumbnails.add(index, thumb);
        mThumbsAdapter.notifyItemInserted(index);
        mThumbsAdapter.notifyItemRangeChanged(index, mThumbsAdapter.getItemCount() - index);
    }

    public void setThumbnail(int index, @Nullable Bitmap thumb) {
        mThumbsAdapter.thumbnails.set(index, thumb);
        mThumbsAdapter.notifyItemChanged(index);
    }

    public void removeThumbnail(@Nullable Bitmap thumb) {
        final int index = mThumbsAdapter.thumbnails.indexOf(thumb);
        if (index != -1) {
            removeThumbnail(index);
        }
    }

    public void removeThumbnail(int index) {
        mThumbsAdapter.thumbnails.remove(index);
        mThumbsAdapter.notifyItemRemoved(index);
        mThumbsAdapter.notifyItemRangeChanged(index, mThumbsAdapter.getItemCount() - index);
    }

    public void clearThumbnails() {
        final int itemCount = mThumbsAdapter.getItemCount();
        mThumbsAdapter.thumbnails.clear();
        mThumbsAdapter.notifyItemRangeRemoved(0, itemCount);
    }

    private final class ThumbsAdapter extends RecyclerView.Adapter<ThumbsAdapter.ViewHolder> {

        final List<Bitmap> thumbnails = new ArrayList<>();

        ThumbsAdapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_rv_videoclip_thumbs, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bitmap thumb = thumbnails.get(position);
            if (thumb != null) {
                final int thumbWidth = thumb.getWidth();
                final int thumbHeight = thumb.getHeight();

                ViewGroup.LayoutParams lp = holder.thumbImage.getLayoutParams();
                if (thumbWidth != 0 && thumbHeight != 0) {
                    lp.height = mThumbDisplayHeight;
                    lp.width = (int) (lp.height * (float) thumbWidth / thumbHeight + 0.5f);
                } else {
                    lp.width = lp.height = 0;
                }
//                holder.thumbImage.setLayoutParams(lp);
            }
            holder.thumbImage.setImageBitmap(thumb);
        }

        @Override
        public int getItemCount() {
            return thumbnails.size();
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView thumbImage;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                thumbImage = itemView.findViewById(R.id.image_videoThumb);
            }
        }
    }

    // --------------- Saved Instance State ------------------------

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        try {
            setSelectionInterval(ss.selectionInterval[0], ss.selectionInterval[1]);
            setSelection(ss.selection);
        } catch (IllegalArgumentException e) {
            // This may be thrown by our code, e.g., the expected selection interval out of
            // the selectable time interval range due to clip duration change(s).
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        ss.selection = getSelection();
        getSelectionInterval(ss.selectionInterval);

        return ss;
    }

    /**
     * This is the persistent state saved by {@link VideoClipView}.
     * Only needed if you are creating a subclass of VideoClipView that must save its own state.
     */
    @SuppressWarnings({"WeakerAccess", "deprecation"})
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public static class SavedState extends AbsSavedState {
        int selection;
        final int[] selectionInterval = sNoSelectionInterval.clone();

        // Called by onSaveInstanceState()
        protected SavedState(Parcelable superState) {
            super(superState);
        }

        // Called by CREATOR
        protected SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            selection = in.readInt();
            in.readIntArray(selectionInterval);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(selection);
            dest.writeIntArray(selectionInterval);
        }

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
    }
}
