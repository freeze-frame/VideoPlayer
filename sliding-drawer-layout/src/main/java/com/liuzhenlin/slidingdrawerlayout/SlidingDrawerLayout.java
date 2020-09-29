/*
 * Created on 2018/05/16.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.slidingdrawerlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.ViewDragHelper;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A layout shows better than {@link androidx.drawerlayout.widget.DrawerLayout}, which can also
 * scroll its content as the user drags its drawer.
 *
 * @author <a href="mailto:2233788867@qq.com">刘振林</a>
 */
@SuppressLint("RtlHardcoded")
public class SlidingDrawerLayout extends ViewGroup {
    private static final String TAG = "SlidingDrawerLayout";

    /** The left child view covered by {@link #mContentView} */
    private View mLeftDrawer;

    /** The right child view covered by {@link #mContentView} */
    private View mRightDrawer;

    /** The content view in this layout that is always visible. */
    private View mContentView;

    /**
     * The drawer currently being shown, maybe one of {@link #mLeftDrawer}, {@link #mRightDrawer}
     * or <code>null</code>.
     */
    /*synthetic*/ View mShownDrawer;

    /**
     * Used to temporarily cache the drawer to be opened in the touch events.
     */
    private View mTmpDrawer;

    /**
     * Caches the layer type of the shown drawer, which can be one of {@link #LAYER_TYPE_NONE},
     * {@link #LAYER_TYPE_SOFTWARE} or {@link #LAYER_TYPE_HARDWARE}.
     */
    private int mShownDrawerLayerType = LAYER_TYPE_NONE;

    /**
     * @see #getLeftDrawerWidthPercent()
     * @see #setLeftDrawerWidthPercent(float)
     */
    private float mLeftDrawerWidthPercent = UNDEFINED_DRAWER_WIDTH_PERCENT;

    /**
     * @see #getRightDrawerWidthPercent()
     * @see #setRightDrawerWidthPercent(float)
     */
    private float mRightDrawerWidthPercent = UNDEFINED_DRAWER_WIDTH_PERCENT;

    /** Caches the width percentage of the start drawer. */
    private float mStartDrawerWidthPercent = UNDEFINED_DRAWER_WIDTH_PERCENT;

    /** Caches the width percentage of the end drawer. */
    private float mEndDrawerWidthPercent = UNDEFINED_DRAWER_WIDTH_PERCENT;

    /** Used for the drawer of which the width percentage is not defined. */
    private static final int UNDEFINED_DRAWER_WIDTH_PERCENT = -2;

    /**
     * Used for the drawer of which the width percentage is not resolved before
     * the layout direction of this view resolved.
     */
    public static final int UNRESOLVED_DRAWER_WIDTH_PERCENT = -1;

    /**
     * If set, the width of the relevant drawer will be measured as it is, within a percentage
     * ranging from {@value #MINIMUM_DRAWER_WIDTH_PERCENT} to {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}.
     */
    public static final int UNSPECIFIED_DRAWER_WIDTH_PERCENT = 0;

    /** The minimum percentage of the widths of the drawers relative to current view's width. */
    public static final float MINIMUM_DRAWER_WIDTH_PERCENT = 0.1f;
    /** The maximum percentage of the widths of the drawers relative to current view's width. */
    public static final float MAXIMUM_DRAWER_WIDTH_PERCENT = 1.0f;

    /*synthetic*/ int mFlags;

    /** No drawer is currently scrolling. */
    public static final int SCROLL_STATE_IDLE = 0;

    /** There is a drawer currently scrolling and being dragged by user. */
    public static final int SCROLL_STATE_TOUCH_SCROLL = 1;

    /**
     * A drawer is currently scrolling but not under outside control as a result of a fling
     * or a translation animation.
     */
    public static final int SCROLL_STATE_AUTO_SCROLL = 1 << 1;

    /** Mask for use with {@link #mFlags} to get the drawer scroll state. */
    private static final int SCROLL_STATE_MASK = 0b0000_0011;

    @IntDef({
            SCROLL_STATE_IDLE,
            SCROLL_STATE_TOUCH_SCROLL,
            SCROLL_STATE_AUTO_SCROLL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollState {
    }

    /** Indicates that the drawer currently showing has been fully opened. */
    private static final int FLAG_DRAWER_HAS_BEEN_OPENED = 1 << 2;

    /** If set, the drawer is currently being or scheduled to be opened via the animator. */
    private static final int FLAG_ANIMATING_DRAWER_OPENING = 1 << 3;

    /** If set, the drawer is currently being or scheduled to be closed via the animator. */
    private static final int FLAG_ANIMATING_DRAWER_CLOSURE = 1 << 4;

    /** Indicates that the left drawer {@link #mLeftDrawer} is enabled in the touch mode. */
    private static final int FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH = 1 << 5;

    /** Indicates that the right drawer {@link #mRightDrawer} is enabled in the touch mode. */
    private static final int FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH = 1 << 6;

    private static final int FLAG_START_DRAWER_ENABLED_IN_TOUCH = 1 << 7;
    private static final int FLAG_END_DRAWER_ENABLED_IN_TOUCH = 1 << 8;

    private static final int FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED = 1 << 9;
    private static final int FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED = 1 << 10;

    private static final int FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED = 1 << 11;
    private static final int FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED = 1 << 12;

    private static final int FLAG_START_DRAWER_TOUCH_ABILITY_RESOLVED = 1 << 13;
    private static final int FLAG_END_DRAWER_TOUCH_ABILITY_RESOLVED = 1 << 14;

    /**
     * Flag indicates that the touch ability of start/end drawer has been resolved to left/right
     * one for use in confirming which drawer in this layout is slidable. This is set by
     * {@link #resolveDrawerTouchAbilities(int)} and checked by {@link #onMeasure(int, int)}
     * to determine if any drawer touch ability needs to be resolved during measurement.
     */
    private static final int FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED = 1 << 15;

    /**
     * Flag indicates that the width percentage of start/end drawer has been resolved into
     * left/right one for use in measurement, layout, drawing, etc. This is set by
     * {@link #resolveDrawerWidthPercentages(int, boolean)} and checked by {@link #onMeasure(int, int)}
     * to determine if any drawer width percentage needs to be resolved during measurement.
     */
    private static final int FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED = 1 << 16;

    private static final int FLAG_START_DRAWER_WIDTH_PERCENTAGE_RESOLVED = 1 << 17;
    private static final int FLAG_END_DRAWER_WIDTH_PERCENTAGE_RESOLVED = 1 << 18;

    /**
     * Bit for {@link #mFlags}: <code>true</code> when the application is willing to support RTL
     * (right to left). All activities will inherit this value.
     * Set from the {@link android.R.attr#supportsRtl} attribute in the activity's manifest.
     * Default value is false (no support for RTL).
     */
    private static final int FLAG_SUPPORTS_RTL = 1 << 19;

    /**
     * Flag indicating whether the user's finger downs on the area of content view when this view
     * with a drawer open receives {@link MotionEvent#ACTION_DOWN}.
     */
    private static final int FLAG_FINGER_DOWNS_ON_CONTENT_WHEN_DRAWER_IS_OPEN = 1 << 20;

    /** When set, this ViewGroup should not intercept touch events. */
    private static final int FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT = 1 << 21;

    /** When true, indicates that the touch events are intercepted by this ViewGroup */
    private static final int FLAG_TOUCH_INTERCEPTED = 1 << 22;

//    /**
//     * Flag indicating whether we should close the drawer currently open when user presses
//     * the back button. By default, this is <code>true</code>.
//     */
//    private static final int FLAG_CLOSE_OPEN_DRAWER_ON_BACK_PRESSED_ENABLED = 1 << 23;

    /**
     * @see #getContentSensitiveEdgeSize()
     * @see #setContentSensitiveEdgeSize(int)
     */
    private int mContentSensitiveEdgeSize; // px

    /**
     * Default size in pixels for the touch-sensitive edges of the content view.
     */
    public static final int DEFAULT_EDGE_SIZE = 50; // dp

    /** Device independent pixel (dip/dp) */
    protected final float mDp;
    /** Distance to travel before drag may begin */
    protected final int mTouchSlop;

    /** Last known pointer id for touch events */
    private int mActivePointerId = ViewDragHelper.INVALID_POINTER;

    private float mDownX;
    private float mDownY;

    private final float[] mTouchX = new float[2];
    private final float[] mTouchY = new float[2];

    private VelocityTracker mVelocityTracker;

    /**
     * Minimum gesture speed along the x axis to automatically scroll the drawers,
     * as measured in dips per second.
     */
    private final float mMinimumFlingVelocity; // 500 dp/s

    /**
     * The ratio of the distance to scroll content view {@link #mContentView} to the distance
     * to scroll the drawer currently being dragged {@link #mShownDrawer}.
     * <p>
     * While that drawer is scrolling, we simultaneously make the content scroll at a higher speed
     * than the drawer's.
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    private static final int SCROLL_RATIO_CONTENT_TO_DRAWER = 3 / 1;

    /** @see #getScrollPercent() */
    @FloatRange(from = 0.0, to = 1.0)
    private float mScrollPercent;

    /**
     * Animator for scrolling the drawers ({@link #mLeftDrawer}, {@link #mRightDrawer}).
     *
     * @see DrawerAnimator
     */
    private DrawerAnimator mDrawerAnimator;

    /** Time interpolator used for {@link #mDrawerAnimator} */
    protected static final Interpolator sBezierCurveDecelerationInterpolator =
            new LinearOutSlowInInterpolator();

    /**
     * Time interval in milliseconds of automatically scrolling the drawers.
     *
     * @see #getDuration()
     * @see #setDuration(int)
     */
    /*synthetic*/ int mDuration;

    /**
     * Default duration of the animator used to open/close the drawers.
     * <p>
     * If no value for {@link #mDuration} is set, then this default one is used.
     */
    public static final int DEFAULT_DURATION = 256; // ms

    /**
     * Runnable to be run for opening the drawer represented by a ViewStub and not yet added
     * to this layout (even not being inflated).
     *
     * @see ViewStub
     * @see OpenStubDrawerRunnable
     */
    /*synthetic*/ OpenStubDrawerRunnable mOpenStubDrawerRunnable;

    /**
     * The Runnable to be executed for starting the drawer animator to normally open or close
     * the drawer it targets to, is scheduled as needed during a layout pass.
     */
    private DrawerRunnable mPostedDrawerRunnable;

    /**
     * The fade color used for the content view {@link #mContentView}, default is 50% black.
     *
     * @see #getContentFadeColor()
     * @see #setContentFadeColor(int)
     */
    @ColorInt
    private int mContentFadeColor;

    /**
     * Default fade color for the content view if no custom value is provided
     */
    @ColorInt
    public static final int DEFAULT_FADE_COLOR = 0x7F000000;

    @IntDef({Gravity.LEFT, Gravity.RIGHT, Gravity.START, Gravity.END})
    @Retention(RetentionPolicy.SOURCE)
    /*package*/ @interface EdgeGravity {
    }

    private final class DrawerAnimator extends ValueAnimator {
        final AnimatorListener listener = new AnimatorListenerAdapter() {
            boolean canceled;

            @Override
            public void onAnimationStart(Animator animation) {
                canceled = false;
                dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_AUTO_SCROLL);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFlags &= ~(FLAG_ANIMATING_DRAWER_OPENING | FLAG_ANIMATING_DRAWER_CLOSURE);
                if (canceled) {
                    return;
                }

                // Only when the drawer currently showing is not being dragged by user, i.e., this
                // animation normally ends, is the idle scroll state dispatched to the listeners.
                if ((mFlags & SCROLL_STATE_MASK) == SCROLL_STATE_AUTO_SCROLL) {
                    dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_IDLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }
        };

        DrawerAnimator() {
            setInterpolator(sBezierCurveDecelerationInterpolator);
            setDuration(mDuration);
            addListener(listener);
            addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    scrollDrawerTo(mShownDrawer, (int) animation.getAnimatedValue());
                }
            });
        }

        void cancel(boolean clearFlag) {
            if (clearFlag) {
                super.cancel();
            } else {
                final int flag = mFlags & (FLAG_ANIMATING_DRAWER_OPENING | FLAG_ANIMATING_DRAWER_CLOSURE);
                super.cancel();
                mFlags |= flag;
            }
        }
    }

    private final class DrawerRunnable implements Runnable {
        View drawer;
        boolean open;

        boolean isInMsgQueue;

        DrawerRunnable() {
        }

        void initForPost(View drawer, boolean open) {
            this.drawer = drawer;
            this.open = open;
            isInMsgQueue = true;
        }

        void initAndPostToQueue(View drawer, boolean open) {
            initForPost(drawer, open);
            post(this);
        }

        void resetAndRemoveFromQueue() {
            if (isInMsgQueue) {
                removeCallbacks(this);
                isInMsgQueue = false;
                drawer = null;
                open = false;
            }
        }

        @Override
        public void run() {
            if (isInMsgQueue) {
                isInMsgQueue = false;
                if (open) {
                    openDrawer(drawer, true);
                } else if (drawer == mShownDrawer) {
                    closeDrawer(true);
                }
                drawer = null;
                open = false;
            }
        }
    }

    private final class OpenStubDrawerRunnable implements Runnable {
        final View drawer;
        final boolean animate;

        OpenStubDrawerRunnable(View drawer, boolean animate) {
            this.drawer = drawer;
            this.animate = animate;
        }

        @Override
        public void run() {
            mOpenStubDrawerRunnable = null;
            openDrawer(drawer, animate);
        }

        void removeFromMsgQueue() {
            mOpenStubDrawerRunnable = null;
            removeCallbacks(this);
        }
    }

    public SlidingDrawerLayout(Context context) {
        this(context, null);
    }

    public SlidingDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDp = getResources().getDisplayMetrics().density;
        mMinimumFlingVelocity = 500f * mDp;

        ApplicationInfo ai = context.getApplicationInfo();
        if (ai.targetSdkVersion >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && (ai.flags & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0) {
            mFlags |= FLAG_SUPPORTS_RTL;
        }

        TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.SlidingDrawerLayout, defStyleAttr, 0);
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_widthPercent_leftDrawer)) {
            mLeftDrawerWidthPercent = getDrawerWidthPercentFromAttr(ta,
                    R.styleable.SlidingDrawerLayout_widthPercent_leftDrawer);
            checkDrawerWidthPercent(mLeftDrawerWidthPercent);
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_widthPercent_rightDrawer)) {
            mRightDrawerWidthPercent = getDrawerWidthPercentFromAttr(ta,
                    R.styleable.SlidingDrawerLayout_widthPercent_rightDrawer);
            checkDrawerWidthPercent(mRightDrawerWidthPercent);
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_widthPercent_startDrawer)) {
            mStartDrawerWidthPercent = getDrawerWidthPercentFromAttr(ta,
                    R.styleable.SlidingDrawerLayout_widthPercent_startDrawer);
            checkDrawerWidthPercent(mStartDrawerWidthPercent);
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_widthPercent_endDrawer)) {
            mEndDrawerWidthPercent = getDrawerWidthPercentFromAttr(ta,
                    R.styleable.SlidingDrawerLayout_widthPercent_endDrawer);
            checkDrawerWidthPercent(mEndDrawerWidthPercent);
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_enabledInTouch_leftDrawer)) {
            mFlags |= FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED;
            if (ta.getBoolean(R.styleable.SlidingDrawerLayout_enabledInTouch_leftDrawer, true)) {
                mFlags |= FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
            }
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_enabledInTouch_rightDrawer)) {
            mFlags |= FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED;
            if (ta.getBoolean(R.styleable.SlidingDrawerLayout_enabledInTouch_rightDrawer, true)) {
                mFlags |= FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
            }
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_enabledInTouch_startDrawer)) {
            mFlags |= FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED;
            if (ta.getBoolean(R.styleable.SlidingDrawerLayout_enabledInTouch_startDrawer, true)) {
                mFlags |= FLAG_START_DRAWER_ENABLED_IN_TOUCH;
            }
        }
        if (ta.hasValue(R.styleable.SlidingDrawerLayout_enabledInTouch_endDrawer)) {
            mFlags |= FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED;
            if (ta.getBoolean(R.styleable.SlidingDrawerLayout_enabledInTouch_endDrawer, true)) {
                mFlags |= FLAG_END_DRAWER_ENABLED_IN_TOUCH;
            }
        }
        setContentSensitiveEdgeSize(ta.getDimensionPixelSize(R.styleable
                .SlidingDrawerLayout_contentSensitiveEdgeSize, (int) (DEFAULT_EDGE_SIZE * mDp + 0.5f)));
        setContentFadeColor(ta.getColor(R.styleable
                .SlidingDrawerLayout_contentFadeColor, DEFAULT_FADE_COLOR));
        setDuration(ta.getInteger(R.styleable.SlidingDrawerLayout_duration, DEFAULT_DURATION));
//        setCloseOpenDrawerOnBackPressedEnabled(ta.getBoolean(R.styleable
//                .SlidingDrawerLayout_closeOpenDrawerOnBackPressedEnabled, true));
        ta.recycle();

//        // So that we can catch the back button
//        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        if (!CAN_HIDE_DESCENDANTS) {
            mChildAccessibilityDelegate = new ChildAccessibilityDelegate();
        }

        // Disable the splitting of MotionEvents to multiple children during touch event dispatch.
        setMotionEventSplittingEnabled(false);
    }

    private float getDrawerWidthPercentFromAttr(TypedArray attrs, int attrIndex) {
        final int attrType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            attrType = attrs.getType(attrIndex);
        } else {
            TypedValue value = new TypedValue();
            if (attrs.getValue(attrIndex, value)) {
                attrType = value.type;
            } else {
                attrType = TypedValue.TYPE_NULL;
            }
        }
        if (attrType == TypedValue.TYPE_FRACTION) {
            return attrs.getFraction(attrIndex, 1, 1, UNSPECIFIED_DRAWER_WIDTH_PERCENT);
        } else {
            return attrs.getFloat(attrIndex, UNSPECIFIED_DRAWER_WIDTH_PERCENT);
        }
    }

    private void checkDrawerWidthPercent(float percent) {
        if (percent != UNSPECIFIED_DRAWER_WIDTH_PERCENT
                && (percent < MINIMUM_DRAWER_WIDTH_PERCENT || percent > MAXIMUM_DRAWER_WIDTH_PERCENT)) {
            throw new IllegalArgumentException("Invalid percent for drawer's width. " +
                    "The value must be " + UNSPECIFIED_DRAWER_WIDTH_PERCENT + " or " +
                    "from " + MINIMUM_DRAWER_WIDTH_PERCENT + " to " + MAXIMUM_DRAWER_WIDTH_PERCENT +
                    ", but your is " + percent);
        }
    }

    /**
     * @return the percentage of the width of the left drawer relative to current view's within
     *         the range from {@value MINIMUM_DRAWER_WIDTH_PERCENT} to {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}
     *         or just {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} if no specific percentage has been
     *         applied to measuring its width or {@link #UNRESOLVED_DRAWER_WIDTH_PERCENT} if
     *         this cannot be resolved before the layout direction resolved.
     */
    public float getLeftDrawerWidthPercent() {
        if ((mFlags & FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED) == 0) {
            if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
                resolveDrawerWidthPercentages(ViewCompat.LAYOUT_DIRECTION_LTR, true);

            } else if ((mStartDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT
                    && mEndDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT)) {
                return mLeftDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                        UNSPECIFIED_DRAWER_WIDTH_PERCENT : mLeftDrawerWidthPercent;

            } else if (!resolveDrawerWidthPercentagesIfDirectionResolved(true)) {
                return UNRESOLVED_DRAWER_WIDTH_PERCENT;
            }
        }
        return mLeftDrawerWidthPercent;
    }

    /**
     * Sets the percentage of the width of the left drawer relative to current view's within
     * the range from {@value MINIMUM_DRAWER_WIDTH_PERCENT} to {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}
     * or pass in {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} to ignore it to use the usual measurement
     * with a valid width defined for that drawer such as {@link ViewGroup.LayoutParams#WRAP_CONTENT},
     * {@link ViewGroup.LayoutParams#MATCH_PARENT}.
     *
     * @throws IllegalArgumentException if the provided argument <code>percent</code> is outside of
     *                                  the above mentioned.
     */
    public void setLeftDrawerWidthPercent(float percent) {
        checkDrawerWidthPercent(percent);
        if (mLeftDrawerWidthPercent != percent) {
            mLeftDrawerWidthPercent = percent;

            if (isChildInLayout(mLeftDrawer)) {
                requestLayout();
            }
        }
    }

    /**
     * @return the percentage of the width of the right drawer relative to current view's within
     *         the range from {@value MINIMUM_DRAWER_WIDTH_PERCENT} to {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}
     *         or just {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} if no specific percentage has been
     *         applied to measuring its width or {@link #UNRESOLVED_DRAWER_WIDTH_PERCENT} if
     *         this cannot be resolved before the layout direction resolved.
     */
    public float getRightDrawerWidthPercent() {
        if ((mFlags & FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED) == 0) {
            if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
                resolveDrawerWidthPercentages(ViewCompat.LAYOUT_DIRECTION_LTR, true);

            } else if ((mStartDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT
                    && mEndDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT)) {
                return mRightDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                        UNSPECIFIED_DRAWER_WIDTH_PERCENT : mRightDrawerWidthPercent;

            } else if (!resolveDrawerWidthPercentagesIfDirectionResolved(true)) {
                return UNRESOLVED_DRAWER_WIDTH_PERCENT;
            }
        }
        return mRightDrawerWidthPercent;
    }

    /**
     * Sets the percentage of the width of the right drawer relative to current view's within
     * the range from {@value MINIMUM_DRAWER_WIDTH_PERCENT} to {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}
     * or pass in {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} to ignore it to use the usual measurement
     * with a valid width defined for that drawer such as {@link ViewGroup.LayoutParams#WRAP_CONTENT},
     * {@link ViewGroup.LayoutParams#MATCH_PARENT}.
     *
     * @throws IllegalArgumentException if the provided argument <code>percent</code> is outside of
     *                                  the above mentioned.
     */
    public void setRightDrawerWidthPercent(float percent) {
        checkDrawerWidthPercent(percent);
        if (mRightDrawerWidthPercent != percent) {
            mRightDrawerWidthPercent = percent;

            if (isChildInLayout(mRightDrawer)) {
                requestLayout();
            }
        }
    }

    /**
     * @return the width percentage of the start drawer depending on this view's resolved
     *         layout direction or just {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} if no specific
     *         percentage has been applied to measuring its width or {@link #UNRESOLVED_DRAWER_WIDTH_PERCENT}
     *         if this cannot be resolved before the layout direction resolved.
     */
    public float getStartDrawerWidthPercent() {
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        if ((mFlags & FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED) == 0) {
            if ((mFlags & FLAG_SUPPORTS_RTL) == 0 || Utils.isLayoutDirectionResolved(this)) {
                resolveDrawerWidthPercentages(layoutDirection, true);
            } else {
                return mStartDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                        UNRESOLVED_DRAWER_WIDTH_PERCENT : mStartDrawerWidthPercent;
            }
        }
        return layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                mLeftDrawerWidthPercent : mRightDrawerWidthPercent;
    }

    /**
     * Sets the width percentage (from {@value #MINIMUM_DRAWER_WIDTH_PERCENT} to
     * {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}) for the start drawer depending on this view's resolved
     * layout direction or pass in {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} to ignore it
     * to use the usual measurement with a valid width defined for that drawer such as
     * {@link ViewGroup.LayoutParams#WRAP_CONTENT}, {@link ViewGroup.LayoutParams#MATCH_PARENT}.
     *
     * @throws IllegalArgumentException if the provided argument <code>percent</code> is outside of
     *                                  the above mentioned.
     */
    public void setStartDrawerWidthPercent(float percent) {
        checkDrawerWidthPercent(percent);

        mStartDrawerWidthPercent = percent;
        mFlags &= ~(FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED
                | FLAG_START_DRAWER_WIDTH_PERCENTAGE_RESOLVED);
        resolveDrawerWidthPercentagesIfDirectionResolved(false);
    }

    /**
     * @return the width percentage of the end drawer depending on this view's resolved
     *         layout direction or just {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} if no specific
     *         percentage has been applied to measuring its width or {@link #UNRESOLVED_DRAWER_WIDTH_PERCENT}
     *         if this cannot be resolved before the layout direction resolved.
     */
    public float getEndDrawerWidthPercent() {
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        if ((mFlags & FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED) == 0) {
            if ((mFlags & FLAG_SUPPORTS_RTL) == 0 || Utils.isLayoutDirectionResolved(this)) {
                resolveDrawerWidthPercentages(layoutDirection, true);
            } else {
                return mEndDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                        UNRESOLVED_DRAWER_WIDTH_PERCENT : mEndDrawerWidthPercent;
            }
        }
        return layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                mRightDrawerWidthPercent : mLeftDrawerWidthPercent;
    }

    /**
     * Sets the width percentage (from {@value #MINIMUM_DRAWER_WIDTH_PERCENT} to
     * {@value #MAXIMUM_DRAWER_WIDTH_PERCENT}) for the end drawer depending on this view's resolved
     * layout direction or pass in {@link #UNSPECIFIED_DRAWER_WIDTH_PERCENT} to ignore it
     * to use the usual measurement with a valid width defined for that drawer such as
     * {@link ViewGroup.LayoutParams#WRAP_CONTENT}, {@link ViewGroup.LayoutParams#MATCH_PARENT}.
     *
     * @throws IllegalArgumentException if the provided argument <code>percent</code> is outside of
     *                                  the above mentioned.
     */
    public void setEndDrawerWidthPercent(float percent) {
        checkDrawerWidthPercent(percent);

        mEndDrawerWidthPercent = percent;
        mFlags &= ~(FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED
                | FLAG_END_DRAWER_WIDTH_PERCENTAGE_RESOLVED);
        resolveDrawerWidthPercentagesIfDirectionResolved(false);
    }

    /**
     * Sets whether the drawer on the specified side is enabled while in touch mode.
     *
     * @see #setDrawerEnabledInTouch(View, boolean)
     */
    public void setDrawerEnabledInTouch(@EdgeGravity int gravity, boolean enabled) {
        switch (gravity) {
            case Gravity.LEFT:
                if (enabled) {
                    mFlags |= FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags &= ~FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
                }
                mFlags |= FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED;
                break;

            case Gravity.RIGHT:
                if (enabled) {
                    mFlags |= FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags &= ~FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
                }
                mFlags |= FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED;
                break;

            case Gravity.START:
                if (enabled) {
                    mFlags |= FLAG_START_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags &= ~FLAG_START_DRAWER_ENABLED_IN_TOUCH;
                }
                //@formatter:off
                mFlags = (mFlags | FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED)
                                 & ~(FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED
                                        | FLAG_START_DRAWER_TOUCH_ABILITY_RESOLVED);
                //@formatter:on
                resolveDrawerTouchAbilitiesIfDirectionResolved();
                break;

            case Gravity.END:
                if (enabled) {
                    mFlags |= FLAG_END_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags &= ~FLAG_END_DRAWER_ENABLED_IN_TOUCH;
                }
                //@formatter:off
                mFlags = (mFlags | FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED)
                                 & ~(FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED
                                        | FLAG_END_DRAWER_TOUCH_ABILITY_RESOLVED);
                //@formatter:on
                resolveDrawerTouchAbilitiesIfDirectionResolved();
                break;
        }
    }

    /**
     * Sets whether the given drawer is enabled while in touch mode.
     *
     * @see #setDrawerEnabledInTouch(int, boolean)
     */
    public void setDrawerEnabledInTouch(@Nullable View drawer, boolean enabled) {
        if (drawer == null) {
            return;
        }
        if (drawer == mLeftDrawer) {
            setDrawerEnabledInTouch(Gravity.LEFT, enabled);
        } else if (drawer == mRightDrawer) {
            setDrawerEnabledInTouch(Gravity.RIGHT, enabled);
        }
    }

    /**
     * @return whether the drawer on the specified side is enabled in touch mode or the default
     *         <code>true</code> if its touch ability cannot be resolved before the layout direction
     *         resolved.
     * @see #isDrawerEnabledInTouch(View)
     */
    public boolean isDrawerEnabledInTouch(@EdgeGravity int gravity) {
        switch (gravity) {
            case Gravity.LEFT:
                if ((mFlags & FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED) == 0) {
                    if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
                        resolveDrawerTouchAbilities(ViewCompat.LAYOUT_DIRECTION_LTR);

                    } else if ((mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                            && (mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED) == 0) {
                        return (mFlags & FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                                || (mFlags & FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH) != 0;

                    } else if (!resolveDrawerTouchAbilitiesIfDirectionResolved()) {
                        return true;
                    }
                }
                return (mFlags & FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH) != 0;

            case Gravity.RIGHT:
                if ((mFlags & FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED) == 0) {
                    if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
                        resolveDrawerTouchAbilities(ViewCompat.LAYOUT_DIRECTION_LTR);

                    } else if ((mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                            && (mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED) == 0) {
                        return (mFlags & FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                                || (mFlags & FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH) != 0;

                    } else if (!resolveDrawerTouchAbilitiesIfDirectionResolved()) {
                        return true;
                    }
                }
                return (mFlags & FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH) != 0;

            case Gravity.START: {
                final int layoutDirection = ViewCompat.getLayoutDirection(this);

                if ((mFlags & FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED) == 0) {
                    if ((mFlags & FLAG_SUPPORTS_RTL) == 0 || Utils.isLayoutDirectionResolved(this)) {
                        resolveDrawerTouchAbilities(layoutDirection);
                    } else {
                        return (mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                                || (mFlags & FLAG_START_DRAWER_ENABLED_IN_TOUCH) != 0;
                    }
                }
                return isDrawerEnabledInTouch(layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                        Gravity.LEFT : Gravity.RIGHT);
            }

            case Gravity.END: {
                final int layoutDirection = ViewCompat.getLayoutDirection(this);

                if ((mFlags & FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED) == 0) {
                    if ((mFlags & FLAG_SUPPORTS_RTL) == 0 || Utils.isLayoutDirectionResolved(this)) {
                        resolveDrawerTouchAbilities(layoutDirection);
                    } else {
                        return (mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED) == 0
                                || (mFlags & FLAG_END_DRAWER_ENABLED_IN_TOUCH) != 0;
                    }
                }
                return isDrawerEnabledInTouch(layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR ?
                        Gravity.RIGHT : Gravity.LEFT);
            }
        }
        return false;
    }

    /**
     * @return whether the given drawer is enabled in touch mode or not
     * @see #isDrawerEnabledInTouch(int)
     */
    public boolean isDrawerEnabledInTouch(@Nullable View drawer) {
        if (drawer != null) {
            if (drawer == mLeftDrawer) {
                return isDrawerEnabledInTouch(Gravity.LEFT);
            }
            if (drawer == mRightDrawer) {
                return isDrawerEnabledInTouch(Gravity.RIGHT);
            }
        }
        return false;
    }

    /**
     * Returns whether the drawer on the specified side can be dragged by user or not. If you call
     * this method before the first layout measurement, at which moment the drawer's slidability
     * has not been resolved, then the default <code>false</code> is returned.
     * <p>
     * This is determined by both the drawer's touch ability and whether it is contained and has
     * been laid by the current view.
     *
     * @see #isDrawerSlidable(View)
     */
    public boolean isDrawerSlidable(@EdgeGravity int gravity) {
        switch (gravity) {
            case Gravity.LEFT:
                if (isStubDrawer(mLeftDrawer)) {
                    return (mFlags & FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH) != 0;
                }
                return (mFlags & FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH) != 0
                        && isChildInLayout(mLeftDrawer);

            case Gravity.RIGHT:
                if (isStubDrawer(mRightDrawer)) {
                    return (mFlags & FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH) != 0;
                }
                return (mFlags & FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH) != 0
                        && isChildInLayout(mRightDrawer);

            case Gravity.START:
            case Gravity.END:
                return isDrawerSlidable(Utils.getAbsoluteHorizontalGravity(this, gravity));
        }
        return false;
    }

    /**
     * @return whether the given drawer can be dragged by user or not.
     * @see #isDrawerSlidable(int)
     */
    public boolean isDrawerSlidable(@Nullable View drawer) {
        if (drawer != null) {
            if (drawer == mLeftDrawer) {
                return isDrawerSlidable(Gravity.LEFT);
            }
            if (drawer == mRightDrawer) {
                return isDrawerSlidable(Gravity.RIGHT);
            }
        }
        return false;
    }

    /**
     * @return the current state of the dragged drawer's scrolling, maybe one of
     *         {@link #SCROLL_STATE_IDLE},
     *         {@link #SCROLL_STATE_TOUCH_SCROLL},
     *         {@link #SCROLL_STATE_AUTO_SCROLL}
     */
    @SuppressLint("WrongConstant")
    @ScrollState
    public int getScrollState() {
        return mFlags & SCROLL_STATE_MASK;
    }

    /**
     * @return the current scroll percentage of the drawer being dragged
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getScrollPercent() {
        return mScrollPercent;
    }

    /**
     * @return <code>true</code> if a drawer is open
     */
    public boolean hasOpenedDrawer() {
        return mShownDrawer != null;
    }

    /**
     * Gets the lasting time of the animator for opening/closing the drawers.
     * The default duration is {@value DEFAULT_DURATION} milliseconds.
     *
     * @return the duration of the animator
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Sets the duration for the animator used to open/close the drawers.
     *
     * @throws IllegalArgumentException if a negative 'duration' is passed in
     */
    public void setDuration(int duration) {
        if (mDrawerAnimator != null) {
            mDrawerAnimator.setDuration(mDuration);
        }
        mDuration = duration;
    }

//    /**
//     * @return <code>true</code> if the drawer currently open can be closed when the user
//     * presses the back button.
//     */
//    public boolean isCloseOpenDrawerOnBackPressedEnabled() {
//        return (mFlags & FLAG_CLOSE_OPEN_DRAWER_ON_BACK_PRESSED_ENABLED) != 0;
//    }
//
//    /**
//     * Sets whether we should close the opened drawer when user presses the back button
//     */
//    public void setCloseOpenDrawerOnBackPressedEnabled(boolean enabled) {
//        if (enabled) {
//            mFlags |= FLAG_CLOSE_OPEN_DRAWER_ON_BACK_PRESSED_ENABLED;
//        } else {
//            mFlags &= ~FLAG_CLOSE_OPEN_DRAWER_ON_BACK_PRESSED_ENABLED;
//        }
//    }

    /**
     * @return the size of the touch-sensitive edges of the content view.
     *         This is the range in pixels along the edges of content view for which edge tracking
     *         is enabled to actively detect edge touches or drags.
     */
    public int getContentSensitiveEdgeSize() {
        return mContentSensitiveEdgeSize;
    }

    /**
     * Sets the size for the touch-sensitive edges of the content view.
     * This is the range in pixels along the edges of content view for which edge tracking is
     * enabled to actively detect edge touches or drags.
     *
     * @throws IllegalArgumentException if the provided argument <code>size</code> < 0
     */
    public void setContentSensitiveEdgeSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("The size for the touch-sensitive edges " +
                    "of content view must >= 0, but your is " + size);
        }
        mContentSensitiveEdgeSize = size;
    }

    /**
     * @return the fade color used for the content view
     */
    @ColorInt
    public int getContentFadeColor() {
        return mContentFadeColor;
    }

    /**
     * Sets the fade color used for the content view to obscure primary content while
     * a drawer is open.
     */
    public void setContentFadeColor(@ColorInt int color) {
        if (mContentFadeColor != color) {
            mContentFadeColor = color;
            if (mScrollPercent > 0 &&
                    (mFlags & (FLAG_ANIMATING_DRAWER_OPENING | FLAG_ANIMATING_DRAWER_CLOSURE)) == 0) {
                invalidate();
            }
        }
    }

    private boolean resolveDrawerWidthPercentagesIfDirectionResolved(boolean preventLayout) {
        final boolean directionResolved = Utils.isLayoutDirectionResolved(this);
        if (directionResolved) {
            resolveDrawerWidthPercentages(ViewCompat.getLayoutDirection(this), preventLayout);
        }
        return directionResolved;
    }

    private void resolveDrawerWidthPercentages(int layoutDirection, boolean preventLayout) {
        if ((mFlags & FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED) != 0) {
            return;
        }

        final float ldwp = mLeftDrawerWidthPercent;
        final float rdwp = mRightDrawerWidthPercent;

        if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
            // If left or right drawer width percentage is not defined and if we have the start
            // or end one defined then use the start or end percentage instead or else set it to
            // default 'UNSPECIFIED_DRAWER_WIDTH_PERCENT'.
            if (mLeftDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                mLeftDrawerWidthPercent =
                        mStartDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                                UNSPECIFIED_DRAWER_WIDTH_PERCENT : mStartDrawerWidthPercent;
            }
            if (mRightDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                mRightDrawerWidthPercent =
                        mEndDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT ?
                                UNSPECIFIED_DRAWER_WIDTH_PERCENT : mEndDrawerWidthPercent;
            }
        } else {
            final boolean ldrtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;

            if ((mFlags & FLAG_START_DRAWER_WIDTH_PERCENTAGE_RESOLVED) == 0) {
                if (mStartDrawerWidthPercent != UNDEFINED_DRAWER_WIDTH_PERCENT) {
                    if (ldrtl) {
                        mRightDrawerWidthPercent = mStartDrawerWidthPercent;
                    } else {
                        mLeftDrawerWidthPercent = mStartDrawerWidthPercent;
                    }
                } else {
                    if (ldrtl) {
                        if (mRightDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                            mRightDrawerWidthPercent = UNSPECIFIED_DRAWER_WIDTH_PERCENT;
                        }
                    } else {
                        if (mLeftDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                            mLeftDrawerWidthPercent = UNSPECIFIED_DRAWER_WIDTH_PERCENT;
                        }
                    }
                }
                mFlags |= FLAG_START_DRAWER_WIDTH_PERCENTAGE_RESOLVED;
            }
            if ((mFlags & FLAG_END_DRAWER_WIDTH_PERCENTAGE_RESOLVED) == 0) {
                if (mEndDrawerWidthPercent != UNDEFINED_DRAWER_WIDTH_PERCENT) {
                    if (ldrtl) {
                        mLeftDrawerWidthPercent = mEndDrawerWidthPercent;
                    } else {
                        mRightDrawerWidthPercent = mEndDrawerWidthPercent;
                    }
                } else {
                    if (ldrtl) {
                        if (mLeftDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                            mLeftDrawerWidthPercent = UNSPECIFIED_DRAWER_WIDTH_PERCENT;
                        }
                    } else {
                        if (mRightDrawerWidthPercent == UNDEFINED_DRAWER_WIDTH_PERCENT) {
                            mRightDrawerWidthPercent = UNSPECIFIED_DRAWER_WIDTH_PERCENT;
                        }
                    }
                }
                mFlags |= FLAG_END_DRAWER_WIDTH_PERCENTAGE_RESOLVED;
            }
        }

        mFlags |= FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED;

        if (!preventLayout) {
            if (mLeftDrawerWidthPercent != ldwp && isChildInLayout(mLeftDrawer)
                    || mRightDrawerWidthPercent != rdwp && isChildInLayout(mRightDrawer)) {
                requestLayout();
            }
        }
    }

    private boolean resolveDrawerTouchAbilitiesIfDirectionResolved() {
        final boolean directionResolved = Utils.isLayoutDirectionResolved(this);
        if (directionResolved) {
            resolveDrawerTouchAbilities(ViewCompat.getLayoutDirection(this));
        }
        return directionResolved;
    }

    private void resolveDrawerTouchAbilities(int layoutDirection) {
        if ((mFlags & FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED) != 0) {
            return;
        }

        if ((mFlags & FLAG_SUPPORTS_RTL) == 0) {
            // If left or right drawer ability is not defined and if we have the start or end one
            // defined then use the start or end ability instead or else set it to default 'true'.
            if ((mFlags & FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED) == 0) {
                if ((mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED) != 0
                        && (mFlags & FLAG_START_DRAWER_ENABLED_IN_TOUCH) == 0) {
                    mFlags &= ~FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags |= FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
                }
            }
            if ((mFlags & FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED) == 0) {
                if ((mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED) != 0
                        && (mFlags & FLAG_END_DRAWER_ENABLED_IN_TOUCH) == 0) {
                    mFlags &= ~FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
                } else {
                    mFlags |= FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
                }
            }
        } else {
            final boolean ldrtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;

            if ((mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_RESOLVED) == 0) {
                final int startDrawerEnabledFlag = ldrtl ?
                        FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH : FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH;
                if ((mFlags & FLAG_START_DRAWER_TOUCH_ABILITY_DEFINED) != 0) {
                    if ((mFlags & FLAG_START_DRAWER_ENABLED_IN_TOUCH) == 0) {
                        mFlags &= ~startDrawerEnabledFlag;
                    } else {
                        mFlags |= startDrawerEnabledFlag;
                    }
                } else {
                    if ((mFlags & (
                            ldrtl ? FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED
                                    : FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED)) != 0
                            && (mFlags & startDrawerEnabledFlag) == 0) {
                        mFlags &= ~startDrawerEnabledFlag;
                    } else {
                        mFlags |= startDrawerEnabledFlag;
                    }
                }
                mFlags |= FLAG_START_DRAWER_TOUCH_ABILITY_RESOLVED;
            }
            if ((mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_RESOLVED) == 0) {
                final int endDrawerEnabledFlag = ldrtl ?
                        FLAG_LEFT_DRAWER_ENABLED_IN_TOUCH : FLAG_RIGHT_DRAWER_ENABLED_IN_TOUCH;
                if ((mFlags & FLAG_END_DRAWER_TOUCH_ABILITY_DEFINED) != 0) {
                    if ((mFlags & FLAG_END_DRAWER_ENABLED_IN_TOUCH) == 0) {
                        mFlags &= ~endDrawerEnabledFlag;
                    } else {
                        mFlags |= endDrawerEnabledFlag;
                    }
                } else {
                    if ((mFlags & (
                            ldrtl ? FLAG_LEFT_DRAWER_TOUCH_ABILITY_DEFINED
                                    : FLAG_RIGHT_DRAWER_TOUCH_ABILITY_DEFINED)) != 0
                            && (mFlags & endDrawerEnabledFlag) == 0) {
                        mFlags &= ~endDrawerEnabledFlag;
                    } else {
                        mFlags |= endDrawerEnabledFlag;
                    }
                }
                mFlags |= FLAG_END_DRAWER_TOUCH_ABILITY_RESOLVED;
            }
        }

        mFlags |= FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED;
    }

    private void traverseAllChildren() {
        traverseAllChildren(getChildCount(), ViewCompat.getLayoutDirection(this));
    }

    private void traverseAllChildren(int childCount, int layoutDirection) {
        if (childCount > 3) {
            throw new IllegalStateException("SlidingDrawerLayout can host only three direct children.");
        }

        mContentView = mLeftDrawer = mRightDrawer = null;
        switch (childCount) {
            case 1:
                mContentView = getChildAt(0);
                break;
            case 2:
                traverseAllChildren2(childCount, layoutDirection);
                if (mContentView == null) {
                    if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR) {
                        mContentView = mRightDrawer;
                        mRightDrawer = null;
                    } else {
                        mContentView = mLeftDrawer;
                        mLeftDrawer = null;
                    }
                }

                if (mLeftDrawer == null && mRightDrawer == null) {
                    throw new IllegalStateException("Edge gravity with value Gravity#LEFT, " +
                            "Gravity#RIGHT, Gravity#START or Gravity#END must be set " +
                            "for the Drawer's LayoutParams to finalize the Drawer's placement.");
                }

                View drawer;
                if (mLeftDrawer != null) {
                    drawer = mLeftDrawer;
                } else /*if (mRightDrawer != null)*/ {
                    drawer = mRightDrawer;
                }
                if (getChildAt(0) != drawer) {
                    detachViewFromParent(1);
                    attachViewToParent(drawer, 0, drawer.getLayoutParams());
                }
                break;
            case 3:
                traverseAllChildren2(childCount, layoutDirection);

                if (mLeftDrawer == null || mRightDrawer == null) {
                    throw new IllegalStateException("Edge gravities need to be set for the Drawers' " +
                            "LayoutParams and each gravity is required to have a resolved value " +
                            "Gravity#LEFT or Gravity#RIGHT that is different from each other's " +
                            "to finalize the placements of the drawers.");
                }

                if (getChildAt(0) != mLeftDrawer) {
                    detachViewFromParent(mLeftDrawer);
                    attachViewToParent(mLeftDrawer, 0, mLeftDrawer.getLayoutParams());
                }
                if (getChildAt(1) != mRightDrawer) {
                    detachViewFromParent(mRightDrawer);
                    attachViewToParent(mRightDrawer, 1, mRightDrawer.getLayoutParams());
                }
                break;
        }

        // We only need delegates here if the framework doesn't understand
        // NO_HIDE_DESCENDANTS importance.
        if (!CAN_HIDE_DESCENDANTS) {
            if (mContentView != null) {
                ViewCompat.setAccessibilityDelegate(mContentView, mChildAccessibilityDelegate);
            }
            if (mLeftDrawer != null) {
                ViewCompat.setAccessibilityDelegate(mLeftDrawer, mChildAccessibilityDelegate);
            }
            if (mRightDrawer != null) {
                ViewCompat.setAccessibilityDelegate(mRightDrawer, mChildAccessibilityDelegate);
            }
        }

        if (mShownDrawer != null && mShownDrawer != mLeftDrawer && mShownDrawer != mRightDrawer) {
            dispatchDrawerScrollPercentChangeIfNeeded(0);

            final int state = mFlags & SCROLL_STATE_MASK;
            if (state == SCROLL_STATE_IDLE) {
                final View shownDrawer = mShownDrawer;
                mShownDrawer = null;
                shownDrawer.setLayerType(mShownDrawerLayerType, null);
                mShownDrawerLayerType = LAYER_TYPE_NONE;
//                shownDrawer.setVisibility(INVISIBLE);

                if (mContentView != null) {
                    LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
                    lp.finalLeft = lp.startLeft;
                }

                if ((mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) != 0) {
                    mFlags &= ~FLAG_DRAWER_HAS_BEEN_OPENED;
                    if (mOnDrawerScrollListeners != null) {
                        OnDrawerScrollListener[] listeners =
                                mOnDrawerScrollListeners.toArray(sEmptyOnDrawerScrollListenerArray);
                        for (OnDrawerScrollListener listener : listeners)
                            listener.onDrawerClosed(this, shownDrawer);
                    }

                    updateChildrenImportantForAccessibility(false);
                    if (hasWindowFocus()) {
                        View rootView = getRootView();
                        if (rootView != null) {
                            rootView.sendAccessibilityEvent(
                                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                        }
                    }
                }
            } else {
                if (state == SCROLL_STATE_TOUCH_SCROLL) {
                    clearTouch();
                } else {
                    mDrawerAnimator.cancel(true);
                }
                dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_IDLE);
            }
            // The children's accessibility importances are updated in the above cases.
        } else {
            // Update children's accessibility importances in case they are changed. This happens,
            // for example, a new drawer is added or an existing one is removed or some child is
            // replaced with another view.
            updateChildrenImportantForAccessibility((mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) != 0);
        }
    }

    private void traverseAllChildren2(int childCount, int layoutDirection) {
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            final int gravity = ((LayoutParams) child.getLayoutParams()).gravity;
            final int absGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
            if ((absGravity & Gravity.LEFT) == Gravity.LEFT) {
                if (mLeftDrawer == null) {
                    mLeftDrawer = child;
                    continue;
                }
            } else if ((absGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                if (mRightDrawer == null) {
                    mRightDrawer = child;
                    continue;
                }
            }
            mContentView = child;
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        mFlags &= ~FLAG_DRAWER_TOUCH_ABILITIES_RESOLVED
                | FLAG_START_DRAWER_TOUCH_ABILITY_RESOLVED
                | FLAG_END_DRAWER_TOUCH_ABILITY_RESOLVED
                | FLAG_DRAWER_WIDTH_PERCENTAGES_RESOLVED
                | FLAG_START_DRAWER_WIDTH_PERCENTAGE_RESOLVED
                | FLAG_END_DRAWER_WIDTH_PERCENTAGE_RESOLVED;
        resolveDrawerTouchAbilities(layoutDirection);
        resolveDrawerWidthPercentages(layoutDirection, true);
    }

    private boolean isChildInLayout(View child) {
        return child != null && child.getVisibility() != GONE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int childCount = getChildCount();
        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        traverseAllChildren(childCount, layoutDirection);

        int maxWidth = 0;
        int maxHeight = 0;
        int childrenState = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (isChildInLayout(child)) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
                maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
                childrenState = combineMeasuredStates(childrenState, child.getMeasuredState());
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against our minimum height and width
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check against our foreground's minimum height and width
            Drawable drawable = getForeground();
            if (drawable != null) {
                maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
                maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
            }
        }

        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, childrenState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childrenState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        measureChildWithMargins(child,
                parentWidthMeasureSpec, 0,
                parentHeightMeasureSpec, 0);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec,
                                           int widthUsed, int parentHeightMeasureSpec,
                                           int heightUsed) {
        // Child does not have any margin
        final int horizontalPaddings = getPaddingLeft() + getPaddingRight() + widthUsed;
        final int verticalPaddings = getPaddingTop() + getPaddingBottom() + heightUsed;

        int childWidthMeasureSpec;
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                verticalPaddings, child.getLayoutParams().height);

        if (child == mContentView) {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                    horizontalPaddings, child.getLayoutParams().width);
        } else {
            final int availableWidth = MeasureSpec.getSize(parentWidthMeasureSpec) - horizontalPaddings;

            float drawerWidthPercent = child == mLeftDrawer ?
                    mLeftDrawerWidthPercent : mRightDrawerWidthPercent;
            if (drawerWidthPercent == UNSPECIFIED_DRAWER_WIDTH_PERCENT) {
                final int minChildWidth = (int) (availableWidth * MINIMUM_DRAWER_WIDTH_PERCENT + 0.5f);
                final int maxChildWidth = (int) (availableWidth * MAXIMUM_DRAWER_WIDTH_PERCENT + 0.5f);

                childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                        horizontalPaddings, child.getLayoutParams().width);

                final int childMeasuredWidth = MeasureSpec.getSize(childWidthMeasureSpec);
                final int newChildMeasuredWidth = Math.min(
                        Math.max(childMeasuredWidth, minChildWidth), maxChildWidth);

                if (newChildMeasuredWidth != childMeasuredWidth) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            newChildMeasuredWidth, MeasureSpec.EXACTLY);
                }
            } else {
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (int) (availableWidth * drawerWidthPercent + 0.5f), MeasureSpec.EXACTLY);
            }
        }

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Delays the running animation to ensure the active drawer will open or close normally
        if (mDrawerAnimator != null && mDrawerAnimator.isRunning()) {
            final boolean open = (mFlags & FLAG_ANIMATING_DRAWER_OPENING) != 0;
            mDrawerAnimator.cancel(true);

            if (mPostedDrawerRunnable == null) {
                mPostedDrawerRunnable = new DrawerRunnable();
                mPostedDrawerRunnable.initAndPostToQueue(mShownDrawer, open);

            } else if (mPostedDrawerRunnable.isInMsgQueue) {
                mPostedDrawerRunnable.initForPost(mShownDrawer, open);
            } else {
                mPostedDrawerRunnable.initAndPostToQueue(mShownDrawer, open);
            }
        }

        final int parentLeft = getPaddingLeft();
        final int parentRight = right - left - getPaddingRight();
        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        final int parentWidth = parentRight - parentLeft;
        final int parentHeight = parentBottom - parentTop;

        final int layoutDirection = ViewCompat.getLayoutDirection(this);

        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            View child = getChildAt(i);
            if (!isChildInLayout(child)) {
                continue;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            final int childLeft;
            final int childTop;

            final int horizontalGravity = GravityCompat.getAbsoluteGravity(
                    lp.gravity, layoutDirection) & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (child == mContentView) {
                switch (horizontalGravity) {
                    case Gravity.LEFT:
                        lp.startLeft = parentLeft;
                        break;
                    case Gravity.RIGHT:
                        lp.startLeft = parentRight - childWidth;
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                    default:
                        lp.startLeft = Math.round(parentLeft + (parentWidth - childWidth) / 2f);
                        break;
                }
                if (mShownDrawer == null) {
                    lp.finalLeft = lp.startLeft;

                    childLeft = lp.startLeft;
                } else {
                    // Recalculates its finalLeft in case it changes.
                    lp.finalLeft = lp.startLeft +
                            (mShownDrawer == mLeftDrawer ?
                                    mShownDrawer.getMeasuredWidth() : -mShownDrawer.getMeasuredWidth());

                    childLeft = Math.round(lp.startLeft + (lp.finalLeft - lp.startLeft) * mScrollPercent);
                }
            } else {
                // We need to make sure of the opened drawer to be correctly displayed by this view,
                // i.e., make it fill its entire layout space and not covered by the content view,
                // so compute the start offset using the Math's ceil method to offset the drawer by
                // at most 1 pixel over the ones of the ideal offset.
                // For more, please refer to #scrollDrawerBy(View, int).
                final int offset = (int) Math.ceil(childWidth / (double) SCROLL_RATIO_CONTENT_TO_DRAWER);

                switch (horizontalGravity) {
                    case Gravity.LEFT:
                        lp.finalLeft = parentLeft;
                        lp.startLeft = lp.finalLeft - offset;
                        break;
                    case Gravity.RIGHT:
                        lp.finalLeft = parentRight - childWidth;
                        lp.startLeft = lp.finalLeft + offset;
                        break;
                }
                if (child == mShownDrawer) {
                    childLeft = Math.round(lp.startLeft + (lp.finalLeft - lp.startLeft) * mScrollPercent);
                } else {
                    childLeft = lp.startLeft;

                    if (child.getVisibility() != INVISIBLE) {
                        child.setVisibility(INVISIBLE);
                    }
                }
            }

            final int verticalGravity = lp.gravity == Gravity.NO_GRAVITY ?
                    Gravity.CENTER_VERTICAL : lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
            switch (verticalGravity) {
                case Gravity.TOP:
                    childTop = parentTop;
                    break;
                case Gravity.BOTTOM:
                    childTop = parentBottom - childHeight;
                    break;
                case Gravity.CENTER_VERTICAL:
                default:
                    childTop = Math.round(parentTop + (parentHeight - childHeight) / 2f);
                    break;
            }

            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        // Make sure of the closed drawer(s) to be invisible
        if (child != mContentView && child != mShownDrawer) {
            return false;
        }

        final boolean issued;

        if (child == mShownDrawer) {
            final int save = canvas.save();

            if (child == mLeftDrawer) {
                canvas.clipRect(child.getLeft(), child.getTop(),
                        mContentView.getLeft(), child.getBottom());
            } else {
                canvas.clipRect(mContentView.getRight(), child.getTop(),
                        child.getRight(), child.getBottom());
            }

            issued = super.drawChild(canvas, child, drawingTime);

            canvas.restoreToCount(save);

        } else {
            issued = super.drawChild(canvas, child, drawingTime);

            // Draw the content view's fading
            if (mScrollPercent > 0) {
                final int baseAlpha = (mContentFadeColor & 0xFF000000) >>> 24;
                final int alpha = (int) (baseAlpha * mScrollPercent + 0.5f);
                final int color = alpha << 24 | (mContentFadeColor & 0x00FFFFFF);

                if (mShownDrawer == mLeftDrawer) {
                    canvas.clipRect(mContentView.getLeft(), child.getTop(),
                            getRight() - getPaddingRight(), child.getBottom());
                    // mShownDrawer == mRightDrawer
                } else {
                    canvas.clipRect(getPaddingLeft(), child.getTop(),
                            mContentView.getRight(), child.getBottom());
                }
                canvas.drawColor(color);
            }
        }

        return issued;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelRunningAnimatorAndPendingActions();
        closeDrawer(false);
    }

    private void cancelRunningAnimatorAndPendingActions() {
        if (mOpenStubDrawerRunnable != null) {
            mOpenStubDrawerRunnable.removeFromMsgQueue();
        }
        if (mPostedDrawerRunnable != null && mPostedDrawerRunnable.isInMsgQueue) {
            mPostedDrawerRunnable.resetAndRemoveFromQueue();
        }
        if (mDrawerAnimator != null && mDrawerAnimator.isRunning()) {
            mDrawerAnimator.cancel(true);
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept == ((mFlags & FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT) != 0)) {
            return;
        }

        if (disallowIntercept) {
            if ((mFlags & FLAG_TOUCH_INTERCEPTED) == 0) {
                clearTouch();
            }
            mFlags |= FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT;
        } else {
            mFlags &= ~FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT;
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mFlags &= ~FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT;
                // Throw away all previous state when starting a new touch gesture.
                // The framework may have dropped the up or cancel event for the previous gesture
                // due to an app switch, ANR, or some other state change.
                resetTouch();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
//                if (mScrollPercent == 1 &&
//                        ((mFlags & FLAG_TOUCH_INTERCEPTED) != 0
//                                || (mFlags & FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT) == 0)) {
//                    final int actionIndex = ev.getActionIndex();
//                    final float x = ev.getX(actionIndex);
//
//                    final boolean handle;
//                    if (mShownDrawer == mLeftDrawer) {
//                        handle = x > mContentView.getLeft() && x <= getWidth() - getPaddingRight();
//                    } else /* if (mShownDrawer == mRightDrawer) */ {
//                        handle = x < mContentView.getRight() && x >= getPaddingLeft();
//                    }
//                    if (handle) {
//                        mActivePointerId = ev.getPointerId(actionIndex);
//                        mDownX = x;
//                        mDownY = ev.getY(actionIndex);
//                        markCurrTouchPoint(mDownX, mDownY);
//                        // This is a non-primary pointer falling on the content view after
//                        // the primary one placed outside of the content in the ACTION_DOWN event,
//                        // in which case, we should not call the dispatchTouchEvent() of the super
//                        // class so that no motion event is dispatched to the content view and
//                        // its children, also we are not supposed to intercept the event with an
//                        // ACTION_CANCEL dispatched to the child in the shown drawer that really
//                        // received an ACTION_DOWN, so just return 'true' instead.
//                        return true;
//                    }
//                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mShownDrawer == null) {
            if (!isDrawerSlidable(mLeftDrawer) && !isDrawerSlidable(mRightDrawer)) {
                clearTouch();
                return false;
            }
        } else if (!isDrawerSlidable(mShownDrawer)) {
            try {
                if ((mFlags & SCROLL_STATE_MASK) == SCROLL_STATE_TOUCH_SCROLL) {
                    closeDrawer(true);
                    return true;
                }

                // Intercept the events as appropriate only in the ACTION_DOWN event in which the
                // slidability of the shown drawer is disabled. If enabled, this snippet will not
                // be executed at all, and instead, we will intercept them as needed in our normal
                // touch processing logic below. However, those all guarantee that when a drawer is
                // open, the content view receives no touch event while we are touching on it.
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    final float x = ev.getX();

                    if (mShownDrawer == mLeftDrawer) {
                        return x > mContentView.getLeft() && x <= getWidth() - getPaddingRight();
                    }
                    // mShownDrawer == mRightDrawer
                    return x < mContentView.getRight() && x >= getPaddingLeft();
                }

                return false;
            } finally {
                clearTouch();
            }
        }

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);

        boolean intercept = false;
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onPointerDown(ev);

                if (mScrollPercent > 0) {
                    if (mShownDrawer == mLeftDrawer) {
                        if (mDownX > mContentView.getLeft()) {
                            if (mDownX <= getWidth() - getPaddingRight()) {
                                mFlags |= FLAG_FINGER_DOWNS_ON_CONTENT_WHEN_DRAWER_IS_OPEN;
                            }
                            intercept = true;
                            break;
                        }
                    } else if (/* mShownDrawer == mRightDrawer && */
                            mDownX < mContentView.getRight()) {
                        if (mDownX >= getPaddingLeft()) {
                            mFlags |= FLAG_FINGER_DOWNS_ON_CONTENT_WHEN_DRAWER_IS_OPEN;
                        }
                        intercept = true;
                        break;
                    }
                    intercept = mScrollPercent != 1;
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!onPointerMove(ev)) {
                    break;
                }

                intercept = tryHandleSlidingEvent();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                clearTouch();
                break;
        }
        if (intercept) {
            mFlags |= FLAG_TOUCH_INTERCEPTED;
        }
        return intercept;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mShownDrawer == null) {
            if (!isDrawerSlidable(mLeftDrawer) && !isDrawerSlidable(mRightDrawer)) {
                clearTouch();
                return false;
            }
        } else if (!isDrawerSlidable(mShownDrawer)) {
            try {
                if ((mFlags & SCROLL_STATE_MASK) == SCROLL_STATE_TOUCH_SCROLL) {
                    closeDrawer(true);
                    return true;
                }

                return false;
            } finally {
                clearTouch();
            }
        }

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!onPointerMove(event)) {
                    return false;
                }

                if (mTmpDrawer != null) {
                    mShownDrawer = mTmpDrawer;
                    mTmpDrawer = null;
                    dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_TOUCH_SCROLL);

                    cancelRunningAnimatorAndPendingActions();
                }
                if ((mFlags & SCROLL_STATE_MASK) == SCROLL_STATE_TOUCH_SCROLL) {
                    scrollDrawerBy(mShownDrawer,
                            Math.round((mTouchX[mTouchX.length - 1] - mTouchX[mTouchX.length - 2])
                                    / (float) SCROLL_RATIO_CONTENT_TO_DRAWER));
                    break;
                }

                // Check whether we should handle the subsequent touch events after requiring
                // to intercept them on down event as the user slides the drawer.
                tryHandleSlidingEvent();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                try {
                    if ((mFlags & SCROLL_STATE_MASK) == SCROLL_STATE_TOUCH_SCROLL) {
                        if (mScrollPercent == 1 || mScrollPercent == 0) {
                            dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_IDLE);
                            break;
                        }

                        mVelocityTracker.computeCurrentVelocity(1000);
                        final float vx = mVelocityTracker.getXVelocity(mActivePointerId);
                        if (mShownDrawer == mLeftDrawer && vx >= mMinimumFlingVelocity
                                || mShownDrawer == mRightDrawer && vx <= -mMinimumFlingVelocity) {
                            openDrawerInternal(mShownDrawer, true);
                            break;
                        } else if (mShownDrawer == mLeftDrawer && vx <= -mMinimumFlingVelocity
                                || mShownDrawer == mRightDrawer && vx >= mMinimumFlingVelocity) {
                            closeDrawer(true);
                            break;
                        }

                        if (mScrollPercent >= 0.5f) {
                            openDrawerInternal(mShownDrawer, true);
                        } else {
                            closeDrawer(true);
                        }

                        // Close the shown drawer even if it is being animated as user clicks
                        // the content area
                    } else if ((mFlags & FLAG_FINGER_DOWNS_ON_CONTENT_WHEN_DRAWER_IS_OPEN) != 0) {
                        closeDrawer(true);
                    }
                    break;
                } finally {
                    clearTouch();
                }
        }
        return true;
    }

    private boolean tryHandleSlidingEvent() {
        final float dx = mTouchX[mTouchX.length - 1] - mDownX;
        final float absDx = Math.abs(dx);

        if (absDx < mTouchSlop) {
            return false;
        }

        final float absDy = Math.abs(mTouchY[mTouchY.length - 1] - mDownY);

        boolean handle = false;
        if (mLeftDrawer != null && (mShownDrawer == null || mShownDrawer == mLeftDrawer)) {
            if (mScrollPercent == 0) {
                final int left = getPaddingLeft();
                if (mDownX >= left && mDownX <= left + mContentSensitiveEdgeSize) {
                    handle = dx > mTouchSlop && dx > absDy;
                }
            } else if (mScrollPercent == 1 && mDownX <= mContentView.getLeft()) {
                handle = dx < -mTouchSlop && dx < -absDy;
            } else {
                handle = true;
            }
            if (handle) {
                if (isStubDrawer(mLeftDrawer)) {
                    mLeftDrawer = inflateStubDrawer((ViewStub) mLeftDrawer);
                }
                mTmpDrawer = mLeftDrawer;

                requestParentDisallowInterceptTouchEvent();
                return true;
            }
        }
        if (mRightDrawer != null && (mShownDrawer == null || mShownDrawer == mRightDrawer)) {
            if (mScrollPercent == 0) {
                final int right = getWidth() - getPaddingRight();
                if (mDownX >= right - mContentSensitiveEdgeSize && mDownX <= right) {
                    handle = dx < -mTouchSlop && dx < -absDy;
                }
            } else if (mScrollPercent == 1 && mDownX >= mRightDrawer.getLeft()) {
                handle = dx > mTouchSlop && dx > absDy;
            } else {
                handle = true;
            }
            if (handle) {
                if (isStubDrawer(mRightDrawer)) {
                    mRightDrawer = inflateStubDrawer((ViewStub) mRightDrawer);
                }
                mTmpDrawer = mRightDrawer;

                requestParentDisallowInterceptTouchEvent();
                return true;
            }
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

    private void clearTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        resetTouch();
    }

    private void resetTouch() {
        mTmpDrawer = null;
        mActivePointerId = ViewDragHelper.INVALID_POINTER;
        // Clear all the touch flags except for FLAG_DISALLOW_INTERCEPT_TOUCH_EVENT
        mFlags &= ~(FLAG_FINGER_DOWNS_ON_CONTENT_WHEN_DRAWER_IS_OPEN
                | FLAG_TOUCH_INTERCEPTED);
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        // Only the views in the opened drawer are focusable.
        if ((mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) != 0) {
            mShownDrawer.addFocusables(views, direction, focusableMode);

            // If no drawer is open, call the content view's addFocusables() as needed.
        } else {
            if (mContentView == null) {
                traverseAllChildren();
            }
            if (mContentView != null /* In case there is no child added to this layout */
                    && mContentView.getVisibility() == VISIBLE) {
                mContentView.addFocusables(views, direction, focusableMode);
            }
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK && mShownDrawer != null) {
//            event.startTracking();
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK
//                && (mFlags & FLAG_CLOSE_OPEN_DRAWER_ON_BACK_PRESSED_ENABLED) != 0) {
//            closeDrawer(true);
//            return mShownDrawer != null;
//        }
//        return super.onKeyUp(keyCode, event);
//    }

    private boolean isStubDrawer(View drawer) {
        return drawer instanceof ViewStub;
    }

    private View inflateStubDrawer(ViewStub stub) {
        final int layoutResource = stub.getLayoutResource();
        if (layoutResource != 0) {
            LayoutInflater inflater = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                inflater = stub.getLayoutInflater();
            }
            if (inflater == null) {
                inflater = LayoutInflater.from(getContext());
            }
            final View view = inflater.inflate(layoutResource, this, false);
            final int inflatedId = stub.getInflatedId();
            if (inflatedId != NO_ID) {
                view.setId(inflatedId);
            }

            final int index = indexOfChild(stub);
            detachViewFromParent(index);
            addView(view, index, stub.getLayoutParams());

            return view;
        } else {
            throw new IllegalStateException("ViewStub " + stub + " must have a valid layoutResource");
        }
    }

    /**
     * Automatically open the drawer on the specified side.
     *
     * @param animate smoothly open it through animator or not
     * @see #openDrawer(View, boolean)
     * @see #closeDrawer(boolean)
     */
    public void openDrawer(@EdgeGravity int gravity, boolean animate) {
        final int absoluteGravity = Utils.getAbsoluteGravity(this, gravity);
        switch (absoluteGravity) {
            case Gravity.LEFT:
                openDrawer(mLeftDrawer, animate);
                break;
            case Gravity.RIGHT:
                openDrawer(mRightDrawer, animate);
                break;
        }
    }

    /**
     * Automatically open the given drawer.
     * <p>
     * <strong>NOTE:</strong> This will only work if there is no drawer open or the drawer
     * is the one currently being dragged.
     *
     * @param animate smoothly open it through animator or not
     * @see #openDrawer(int, boolean)
     * @see #closeDrawer(boolean)
     */
    public void openDrawer(@Nullable View drawer, boolean animate) {
        if (drawer == null) return;

        if (mShownDrawer == null) {
            if (drawer == mLeftDrawer || drawer == mRightDrawer) {
                if (isStubDrawer(drawer)) {
                    drawer = inflateStubDrawer((ViewStub) drawer);

                    if (mOpenStubDrawerRunnable != null) {
                        mOpenStubDrawerRunnable.removeFromMsgQueue();
                    }
                    mOpenStubDrawerRunnable = new OpenStubDrawerRunnable(drawer, animate);
                    post(mOpenStubDrawerRunnable);

                } else {
                    if (mOpenStubDrawerRunnable != null) {
                        if (mOpenStubDrawerRunnable.drawer == drawer) {
                            return;
                        }
                        mOpenStubDrawerRunnable.removeFromMsgQueue();
                    }

                    openDrawerInternal(drawer, animate);
                }
            }
        } else if (mShownDrawer == drawer) {
            if (mPostedDrawerRunnable != null && mPostedDrawerRunnable.isInMsgQueue) {
                if (animate) {
                    mPostedDrawerRunnable.initForPost(drawer, true);
                    return;
                } else {
                    mPostedDrawerRunnable.resetAndRemoveFromQueue();
                }
            }
            openDrawerInternal(drawer, animate);

        } else if (drawer == mLeftDrawer) {
            Log.w(TAG, "Can't open the left drawer while the right is open.");
        } else if (drawer == mRightDrawer) {
            Log.w(TAG, "Can't open the right drawer while the left is open.");
        }
    }

    private void openDrawerInternal(View drawer, boolean animate) {
        if (drawer == mLeftDrawer && isChildInLayout(mLeftDrawer)
                || drawer == mRightDrawer && isChildInLayout(mRightDrawer)) {
            LayoutParams lp = (LayoutParams) drawer.getLayoutParams();
            if (animate) {
                if (smoothScrollDrawerTo(drawer, lp.finalLeft)) {
                    mFlags |= FLAG_ANIMATING_DRAWER_OPENING;
                    mFlags &= ~FLAG_ANIMATING_DRAWER_CLOSURE;
                }
            } else {
                openOrCloseDrawerImmediately(drawer, lp.finalLeft - drawer.getLeft());
            }
        }
    }

    /**
     * Automatically close the opened drawer.
     *
     * @param animate to do that through animator or not
     * @see #openDrawer(int, boolean)
     * @see #openDrawer(View, boolean)
     */
    public void closeDrawer(boolean animate) {
        if (mShownDrawer != null) {
            LayoutParams lp = (LayoutParams) mShownDrawer.getLayoutParams();
            if (animate) {
                if (smoothScrollDrawerTo(mShownDrawer, lp.startLeft)) {
                    mFlags |= FLAG_ANIMATING_DRAWER_CLOSURE;
                    mFlags &= ~FLAG_ANIMATING_DRAWER_OPENING;
                }
            } else {
                openOrCloseDrawerImmediately(mShownDrawer, lp.startLeft - mShownDrawer.getLeft());
            }
        }
    }

    private void openOrCloseDrawerImmediately(View drawer, int dx) {
        if (dx != 0) {
            if (mDrawerAnimator != null && mDrawerAnimator.isRunning()) {
                mDrawerAnimator.cancel(true);
            } else {
                mShownDrawer = drawer;
                dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_AUTO_SCROLL);
            }
            scrollDrawerBy(mShownDrawer, dx);
            dispatchDrawerScrollStateChangeIfNeeded(SCROLL_STATE_IDLE);
        }
    }

    /**
     * Like {@link #scrollDrawerTo(View, int)}, but scroll smoothly instead of immediately.
     *
     * @param drawer the drawer to scroll
     * @param x      the position on the X axis for the drawer to reach
     * @return <code>true</code> if the scroll is actually started
     */
    private boolean smoothScrollDrawerTo(View drawer, int x) {
        final int left = drawer.getLeft();
        if (left == x) {
            return false;
        }

        mShownDrawer = drawer;

        if (mDrawerAnimator == null) {
            mDrawerAnimator = new DrawerAnimator();

        } else if (mDrawerAnimator.isRunning()) {
            mDrawerAnimator.cancel(false);
        }
        mDrawerAnimator.setIntValues(left, x);
        mDrawerAnimator.start();
        return true;
    }

    /**
     * Scrolls the given drawer to a horizontal position relative to current view.
     *
     * @param drawer the drawer to scroll
     * @param x      the position on the X axis for the drawer to scroll to
     */
    /*synthetic*/ void scrollDrawerTo(View drawer, int x) {
        scrollDrawerBy(drawer, x - drawer.getLeft());
    }

    /**
     * Moves the scrolled position of the given drawer. This will cause a call to
     * {@link OnDrawerScrollListener#onScrollPercentChange(SlidingDrawerLayout, View, float)}
     * and this view will be invalidated to redraw the content view's fading and to re-clip
     * the drawer's display area.
     *
     * <strong>NOTE:</strong> The content view will be simultaneously scrolled at
     * {@value #SCROLL_RATIO_CONTENT_TO_DRAWER} times the drawer speed.
     *
     * @param drawer the drawer to scroll
     * @param dx     the amount of pixels for the drawer to scroll by horizontally
     */
    private void scrollDrawerBy(View drawer, int dx) {
        if (drawer == null) {
            return;
        }

        dx = clampDx(drawer, dx);
        if (dx == 0) {
            return;
        }

        // Clamps the delta horizontal displacement of the content to make its left position within
        // its scrollable range, too. This is why we ceiled the drawer's original horizontal offset
        // (a floating-point pixel value).
        final int contentDx = clampDx(mContentView, dx * SCROLL_RATIO_CONTENT_TO_DRAWER);

        LayoutParams lp = (LayoutParams) drawer.getLayoutParams();

        drawer.offsetLeftAndRight(dx);
        mContentView.offsetLeftAndRight(contentDx);
        dispatchDrawerScrollPercentChangeIfNeeded(
                (float) (drawer.getLeft() - lp.startLeft) / (float) (lp.finalLeft - lp.startLeft));
        invalidate();
    }

    private int clampDx(View child, int dx) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();

        int left = child.getLeft();
        if (mShownDrawer == mLeftDrawer) {
            left = Math.max(lp.startLeft, Math.min(left + dx, lp.finalLeft));
        } else {
            left = Math.max(lp.finalLeft, Math.min(left + dx, lp.startLeft));
        }

        return left - child.getLeft();
    }

    // --------------- LayoutParams ------------------------

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        if (lp instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * The gravity to apply with the View to which these layout parameters are associated.
         * The default value is {@link Gravity#NO_GRAVITY}.
         */
        public int gravity = Gravity.NO_GRAVITY;

        /**
         * The initial position of the left of the View to which these layout parameters belong,
         * as computed in this View's {@link #onLayout(boolean, int, int, int, int)} method.
         */
        /*synthetic*/ int startLeft;

        /**
         * To a drawer: The position for its left to reach when it is completely opened.
         * To content View: The position for its left to reach when some drawer is completely opened.
         */
        /*synthetic*/ int finalLeft;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
            TypedArray ta = c.obtainStyledAttributes(attrs, R.styleable.SlidingDrawerLayout_Layout);
            gravity = ta.getInt(R.styleable.SlidingDrawerLayout_Layout_android_layout_gravity,
                    Gravity.NO_GRAVITY);
            ta.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width, height and weight.
         *
         * @param width   the width, either {@link #MATCH_PARENT},
         *                {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height  the height, either {@link #MATCH_PARENT},
         *                {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param gravity the gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clone the width, height, and gravity of the source.
         *
         * @param source The layout params to copy from
         */
        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.gravity = source.gravity;
        }
    }

    // --------------- OnDrawerScrollListeners ------------------------

    private List<OnDrawerScrollListener> mOnDrawerScrollListeners;

    private static final OnDrawerScrollListener[] sEmptyOnDrawerScrollListenerArray = {};

    public void addOnDrawerScrollListener(@NonNull OnDrawerScrollListener listener) {
        if (mOnDrawerScrollListeners == null) {
            mOnDrawerScrollListeners = new LinkedList<>();

        } else if (mOnDrawerScrollListeners.contains(listener)) {
            return;
        }
        mOnDrawerScrollListeners.add(listener);
    }

    public void removeOnDrawerScrollListener(@NonNull OnDrawerScrollListener listener) {
        if (mOnDrawerScrollListeners != null)
            mOnDrawerScrollListeners.remove(listener);
    }

//    public void clearOnDrawerScrollListeners() {
//        if (mOnDrawerScrollListeners != null)
//            mOnDrawerScrollListeners.clear();
//    }

    private void dispatchDrawerScrollPercentChangeIfNeeded(float percent) {
        if (percent == mScrollPercent) return;
        mScrollPercent = percent;

        if (mOnDrawerScrollListeners != null) {
            OnDrawerScrollListener[] listeners = mOnDrawerScrollListeners
                    .toArray(sEmptyOnDrawerScrollListenerArray);
            // After each loop, the count of OnDrawerScrollListener associated to this view
            // might have changed as addOnDrawerScrollListener, removeOnDrawerScrollListener or
            // clearOnDrawerScrollListeners method can be called during a callback to any listener,
            // in the case of which, a subsequent loop will throw an Exception.
            // For fear of that, here the above copied OnDrawerScrollListener set is used.
            for (OnDrawerScrollListener listener : listeners) {
                listener.onScrollPercentChange(this, mShownDrawer, mScrollPercent);
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    /*synthetic*/ void dispatchDrawerScrollStateChangeIfNeeded(@ScrollState int state) {
        final int old = mFlags & SCROLL_STATE_MASK;
        if (state == old) return;
        mFlags = (mFlags & ~SCROLL_STATE_MASK) | state;

        final View shownDrawer = mShownDrawer;

        OnDrawerScrollListener[] listeners = null;
        if (mOnDrawerScrollListeners != null) {
            listeners = mOnDrawerScrollListeners.toArray(sEmptyOnDrawerScrollListenerArray);

            for (OnDrawerScrollListener listener : listeners)
                listener.onScrollStateChange(this, shownDrawer, state);
        }

        switch (state) {
            case SCROLL_STATE_TOUCH_SCROLL:
            case SCROLL_STATE_AUTO_SCROLL:
                if (old == SCROLL_STATE_IDLE) {
                    if (mScrollPercent == 0) {
                        shownDrawer.setVisibility(VISIBLE);

                        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
                        lp.finalLeft = lp.startLeft +
                                (shownDrawer == mLeftDrawer ?
                                        shownDrawer.getWidth() : -shownDrawer.getWidth());
                    }

                    mShownDrawerLayerType = shownDrawer.getLayerType();
                    shownDrawer.setLayerType(LAYER_TYPE_HARDWARE, null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                            && ViewCompat.isAttachedToWindow(shownDrawer)) {
                        shownDrawer.buildLayer();
                    }
                }
                break;

            case SCROLL_STATE_IDLE:
                shownDrawer.setLayerType(mShownDrawerLayerType, null);

                if (mScrollPercent == 1) {
                    if ((mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) == 0) {
                        mFlags |= FLAG_DRAWER_HAS_BEEN_OPENED;
                        if (listeners != null) {
                            for (OnDrawerScrollListener listener : listeners)
                                listener.onDrawerOpened(this, shownDrawer);
                        }

                        updateChildrenImportantForAccessibility(true);

                        // Only send WINDOW_STATE_CHANGE if the host has window focus.
                        if (hasWindowFocus()) {
                            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                        }
                    }
                } else if (mScrollPercent == 0) {
                    mShownDrawer = null;
                    mShownDrawerLayerType = LAYER_TYPE_NONE;
                    shownDrawer.setVisibility(INVISIBLE);

                    if (mContentView != null) {
                        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
                        lp.finalLeft = lp.startLeft;
                    }

                    if ((mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) != 0) {
                        mFlags &= ~FLAG_DRAWER_HAS_BEEN_OPENED;
                        if (listeners != null) {
                            for (OnDrawerScrollListener listener : listeners)
                                listener.onDrawerClosed(this, shownDrawer);
                        }

                        updateChildrenImportantForAccessibility(false);

                        // Only send WINDOW_STATE_CHANGE if the host has window focus. This may
                        // change if support for multiple foreground windows (e.g. IME) improves.
                        if (hasWindowFocus()) {
                            View rootView = getRootView();
                            if (rootView != null) {
                                rootView.sendAccessibilityEvent(
                                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Classes that wish to monitor the events of the drawers' scrolling should implement
     * this interface.
     */
    public interface OnDrawerScrollListener {
        /**
         * Callback that will be called on the dragged drawer opened.
         *
         * @param parent the layout holds that drawer
         * @param drawer the drawer currently being dragged
         */
        void onDrawerOpened(@NonNull SlidingDrawerLayout parent, @NonNull View drawer);

        /**
         * Callback that will be called on the dragged drawer closed.
         *
         * @param parent the layout that drawer belongs to
         * @param drawer the drawer currently being dragged
         */
        void onDrawerClosed(@NonNull SlidingDrawerLayout parent, @NonNull View drawer);

        /**
         * Callback to be called when the scroll percentage of the dragged drawer changes.
         *
         * @param parent  the current layout
         * @param drawer  the drawer currently being dragged
         * @param percent the scroll percentage of the dragged drawer
         */
        void onScrollPercentChange(@NonNull SlidingDrawerLayout parent, @NonNull View drawer,
                                   @FloatRange(from = 0.0, to = 1.0) float percent);

        /**
         * Callback to be called when the scroll state ({@code mFlags & SCROLL_STATE_MASK})
         * of the dragged drawer changes.
         *
         * @param parent the current layout
         * @param drawer the drawer currently being dragged
         * @param state  the scroll state of the dragged drawer
         */
        void onScrollStateChange(@NonNull SlidingDrawerLayout parent, @NonNull View drawer,
                                 @ScrollState int state);
    }

    /**
     * Stub/No-op implementations of all methods of {@link OnDrawerScrollListener}.
     * Override this if you only care about a few of the available callback methods.
     */
    public static abstract class SimpleOnDrawerScrollListener implements OnDrawerScrollListener {
        @Override
        public void onDrawerOpened(@NonNull SlidingDrawerLayout parent, @NonNull View drawer) {
        }

        @Override
        public void onDrawerClosed(@NonNull SlidingDrawerLayout parent, @NonNull View drawer) {
        }

        @Override
        public void onScrollPercentChange(@NonNull SlidingDrawerLayout parent,
                                          @NonNull View drawer, float percent) {
        }

        @Override
        public void onScrollStateChange(@NonNull SlidingDrawerLayout parent,
                                        @NonNull View drawer, int state) {
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

        if (ss.openDrawerGravity != Gravity.NO_GRAVITY) {
            // Wait for the drawer on the specified side to be correctly resolved by this view
            // as it may depends on the current layout direction.
            post(new Runnable() {
                @SuppressLint("WrongConstant")
                @Override
                public void run() {
                    openDrawer(ss.openDrawerGravity, false);
                }
            });
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);

        final boolean opened = (mFlags & FLAG_DRAWER_HAS_BEEN_OPENED) != 0;
        final boolean hasPostedRunnableInQueue = mPostedDrawerRunnable != null
                && mPostedDrawerRunnable.isInMsgQueue;
        // Is the shown drawer fully opened (that is, not closing)?
        final boolean isOpenedAndNotClosing = opened
                && !(hasPostedRunnableInQueue && !mPostedDrawerRunnable.open)
                && (mFlags & FLAG_ANIMATING_DRAWER_CLOSURE) == 0;
        // Is the shown drawer opening?
        final boolean isClosedAndOpening = !opened &&
                (hasPostedRunnableInQueue && mPostedDrawerRunnable.open
                        || (mFlags & FLAG_ANIMATING_DRAWER_OPENING) != 0);
        if (isOpenedAndNotClosing || isClosedAndOpening) {
            // If one of the conditions above holds, save the drawer's gravity so that
            // we open that drawer during state restore.
            ss.openDrawerGravity = ((LayoutParams) mShownDrawer.getLayoutParams()).gravity;
        }

        return ss;
    }

    /**
     * State persisted across instances
     */
    @SuppressWarnings({"WeakerAccess", "deprecation"})
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public static class SavedState extends AbsSavedState {
        int openDrawerGravity = Gravity.NO_GRAVITY;

        protected SavedState(Parcelable superState) {
            super(superState);
        }

        protected SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            openDrawerGravity = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(openDrawerGravity);
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

    // --------------- Accessibility ------------------------

    /** Whether we can use NO_HIDE_DESCENDANTS accessibility importance. */
    /*synthetic*/ static final boolean CAN_HIDE_DESCENDANTS =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private ChildAccessibilityDelegate mChildAccessibilityDelegate;

    private CharSequence mTitleLeft;
    private CharSequence mTitleRight;

    /**
     * Returns the title of the drawer with the given gravity.
     *
     * @param gravity Gravity.LEFT, RIGHT, START or END. Expresses which drawer
     *                to return the title for.
     * @return The title of the drawer, or null if none set.
     */
    @Nullable
    public CharSequence getDrawerTitle(@EdgeGravity int gravity) {
        final int absGravity = Utils.getAbsoluteGravity(this, gravity);
        if (absGravity == Gravity.LEFT) {
            return mTitleLeft;
        } else if (absGravity == Gravity.RIGHT) {
            return mTitleRight;
        }
        return null;
    }

    /**
     * Returns the title of the given drawer.
     *
     * @see #getDrawerTitle(int)
     */
    @Nullable
    public CharSequence getDrawerTitle(@Nullable View drawer) {
        if (drawer != null) {
            if (drawer == mLeftDrawer) {
                return getDrawerTitle(Gravity.LEFT);
            } else if (drawer == mRightDrawer) {
                return getDrawerTitle(Gravity.RIGHT);
            }
        }
        return null;
    }

    /**
     * Sets the title for the drawer with the given gravity.
     * <p>
     * When accessibility is turned on, this is the title that will be used to identify the drawer
     * to the active accessibility service.
     *
     * @param gravity Gravity.LEFT, RIGHT, START or END. Expresses which drawer to set the title for.
     * @param title   The title for the drawer.
     */
    public void setDrawerTitle(@EdgeGravity int gravity, @Nullable CharSequence title) {
        final int absGravity = Utils.getAbsoluteGravity(this, gravity);
        if (absGravity == Gravity.LEFT) {
            mTitleLeft = title;
        } else if (absGravity == Gravity.RIGHT) {
            mTitleRight = title;
        }
    }

    /**
     * Sets the title for the given drawer.
     *
     * @see #setDrawerTitle(View, CharSequence)
     */
    public void setDrawerTitle(@Nullable View drawer, @Nullable CharSequence title) {
        if (drawer != null) {
            if (drawer == mLeftDrawer) {
                setDrawerTitle(Gravity.LEFT, title);
            } else if (drawer == mRightDrawer) {
                setDrawerTitle(Gravity.RIGHT, title);
            }
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SlidingDrawerLayout.class.getName();
    }

    private void updateChildrenImportantForAccessibility(boolean isDrawerOpen) {
        if (mContentView == null /* No child is added to this layout */) {
            return;
        }
        if (isDrawerOpen) {
            ViewCompat.setImportantForAccessibility(mShownDrawer,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

            ViewCompat.setImportantForAccessibility(mContentView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

            final View otherDrawer = mShownDrawer == mLeftDrawer ? mRightDrawer : mLeftDrawer;
            if (otherDrawer != null) {
                ViewCompat.setImportantForAccessibility(otherDrawer,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        } else {
            ViewCompat.setImportantForAccessibility(mContentView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

            if (mLeftDrawer != null) {
                ViewCompat.setImportantForAccessibility(mLeftDrawer,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
            if (mRightDrawer != null) {
                ViewCompat.setImportantForAccessibility(mRightDrawer,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }
        }
    }

    private final class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect tmpRect = new Rect();

        AccessibilityDelegate() {
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            if (CAN_HIDE_DESCENDANTS) {
                super.onInitializeAccessibilityNodeInfo(host, info);
            } else {
                // Obtain a node for the host, then manually generate the list of children
                // to only include non-obscured views.
                final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
                super.onInitializeAccessibilityNodeInfo(host, superNode);
                copyNodeInfoNoChildren(info, superNode);
                superNode.recycle();

                info.setSource(host);
                final ViewParent parent = ViewCompat.getParentForAccessibility(host);
                if (parent instanceof View) {
                    info.setParent((View) parent);
                }

                addChildrenForAccessibility(info, (ViewGroup) host);
            }

            info.setClassName(getAccessibilityClassName());

//            // This view reports itself as focusable so that it can intercept the back button, but
//            // we should prevent it from reporting itself as focusable to accessibility services.
//            info.setFocusable(false);
//            info.setFocused(false);
//            info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_FOCUS);
//            info.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLEAR_FOCUS);
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);

            event.setClassName(getAccessibilityClassName());
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            // Special case to handle window state change events. As far as accessibility services
            // are concerned, state changes from SlidingDrawerLayout invalidate the entire contents
            // of the screen (like an Activity or Dialog) and they should announce the title of the
            // new content.
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (mShownDrawer != null) {
                    final List<CharSequence> eventText = event.getText();
                    final CharSequence title = getDrawerTitle(mShownDrawer);
                    if (title != null) {
                        eventText.add(title);
                    }
                }
                return true;
            }

            return super.dispatchPopulateAccessibilityEvent(host, event);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                                                       AccessibilityEvent event) {
            if (CAN_HIDE_DESCENDANTS || includeChildForAccessibility(child)) {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
            return false;
        }

        /*
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                                            AccessibilityNodeInfoCompat src) {
            //noinspection deprecation
            src.getBoundsInParent(tmpRect);
            //noinspection deprecation
            dest.setBoundsInParent(tmpRect);

            src.getBoundsInScreen(tmpRect);
            dest.setBoundsInScreen(tmpRect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());
        }

        private void addChildrenForAccessibility(AccessibilityNodeInfoCompat info, ViewGroup v) {
            for (int i = 0, childCount = v.getChildCount(); i < childCount; i++) {
                final View child = v.getChildAt(i);
                if (includeChildForAccessibility(child)) {
                    info.addChild(child);
                }
            }
        }
    }

    /*
     * If the child is not important for accessibility we make sure this hides the entire subtree
     * rooted at it as the IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS is not supported on
     * platforms prior to KITKAT but we want to hide the entire content and the unopened drawer
     * (if any) if a drawer is open.
     */
    /*synthetic*/ static boolean includeChildForAccessibility(View child) {
        final int ifa = ViewCompat.getImportantForAccessibility(child);
        return ifa != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                && ifa != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
    }

    private static final class ChildAccessibilityDelegate extends AccessibilityDelegateCompat {
        ChildAccessibilityDelegate() {
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View child,
                                                      AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(child, info);

            if (!includeChildForAccessibility(child)) {
                // If we are ignoring the sub-tree rooted at the child, break the connection to
                // the rest of the node tree. For details refer to includeChildForAccessibility.
                info.setParent(null);
            }
        }
    }
}