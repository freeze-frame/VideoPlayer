package com.liuzhenlin.swipeback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.fragment.app.Fragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SwipeBackLayout extends FrameLayout {

    @Nullable
    /*synthetic*/ ISwipeBackActivity mActivity;

    @Nullable
    /*synthetic*/ ISwipeBackFragment mFragment;

    /*synthetic*/ final ViewDragHelper mDragHelper;

    public static final int EDGE_LEFT = ViewDragHelper.EDGE_LEFT;
    public static final int EDGE_RIGHT = ViewDragHelper.EDGE_RIGHT;

    private static final int EDGE_MASK = 0b0000_0011;

    @IntDef(flag = true, value = {EDGE_LEFT, EDGE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    /*package*/ @interface Edge {
    }

    @Edge
    /*synthetic*/ int mTrackingEdge = NO_TRACKING_EDGE;
    private static final int NO_TRACKING_EDGE = -1;

    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollState {
    }

    /**
     * @see #getScrollThreshold()
     * @see #setScrollThreshold(float)
     */
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    /*synthetic*/ float mScrollThreshold = DEFAULT_SCROLL_THRESHOLD;
    /** Default threshold of scroll */
    public static final float DEFAULT_SCROLL_THRESHOLD = 1f / 3f;

    /**
     * Minimum velocity that will be detected as a fling, as measured in dips per second. We will
     * close the activity or pop up the fragment from the back stack that holds it when the speed
     * at which user lifts his/her finger over this value.
     */
    private static final int MIN_FLING_VELOCITY = 500; // dp/s

    /** The ratio of the current scrolling distance to the maximum scrollable distance */
    @FloatRange(from = 0.0, to = 1.0)
    /*synthetic*/ float mScrollPercent;

    /*synthetic*/ int mViewFlags = EDGE_LEFT | FLAG_ENABLED | FLAG_PREVIOUS_CONTENT_SCROLLABLE;

    /**
     * @see #isGestureEnabled()
     * @see #setGestureEnabled(boolean)
     */
    private static final int FLAG_ENABLED = 1 << 2;

    /**
     * @see #isPreviousContentScrollable()
     * @see #setPreviousContentScrollable(boolean)
     */
    private static final int FLAG_PREVIOUS_CONTENT_SCROLLABLE = 1 << 3;

    /**
     * The set of listeners to be sent events through
     *
     * @see SwipeListener
     */
    @Nullable
    /*synthetic*/ List<SwipeListener> mSwipeListeners;

    /** The content view that will be moved by user gestures */
    /*synthetic*/ View mContentView;

    /**
     * The rectangle used to measure the bounds of {@link #mContentView} (relative to current view)
     */
    private final Rect mTempRect = new Rect();

    /** The left position of {@link #mContentView} */
    /*synthetic*/ int mContentLeft;

    /** The shadow to be shown while the left edge of {@link #mContentView} is being dragged */
    @Nullable
    /*synthetic*/ Drawable mShadowLeft;
    /** The shadow to be shown while the right edge of {@link #mContentView} is being dragged */
    @Nullable
    /*synthetic*/ Drawable mShadowRight;

    /**
     * @see #getScrimColor()
     * @see #setScrimColor(int)
     */
    @ColorInt
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    @ColorInt
    public static final int DEFAULT_SCRIM_COLOR = 0x80000000;

    @FloatRange(from = 0.0, to = 1.0)
    /*synthetic*/ float mScrimOpacity;

    private static final int FULL_ALPHA = 255;

    protected final int mTouchSlop;
    private static Field sDraggerTouchSlopField;

    static {
        try {
            sDraggerTouchSlopField = ViewDragHelper.class.getDeclaredField("mTouchSlop");
            sDraggerTouchSlopField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public SwipeBackLayout(Context context) {
        this(context, null);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeBackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDragHelper = ViewDragHelper.create(this, new ViewDragCallback());
        mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * getResources().getDisplayMetrics().density);
        setEdgeShadow(R.drawable.shadow_left, EDGE_LEFT);
        setEdgeShadow(R.drawable.shadow_right, EDGE_RIGHT);
    }

    /**
     * Attach this layout to the given 'activity'
     */
    public void attachToActivity(ISwipeBackActivity activity) {
        mActivity = activity;
        TypedArray a = ((Activity) activity).getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.windowBackground
        });
        final int background = a.getResourceId(0, 0);
        a.recycle();

        ViewGroup decor = (ViewGroup) ((Activity) activity).getWindow().getDecorView();
        mContentView = decor.getChildAt(0);
        mContentView.setBackgroundResource(background);

        decor.removeView(mContentView);
        addView(mContentView);
        decor.addView(this);
    }

    /**
     * Attach the view created in fragment's
     * {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} method to this layout.
     */
    public void attachFragmentView(ISwipeBackFragment fragment, View view) {
        mFragment = fragment;
        mContentView = view;
        addView(view);
    }

    /**
     * @return whether swipe-back gesture is enabled or not
     * @see #setGestureEnabled(boolean)
     */
    public boolean isGestureEnabled() {
        return (mViewFlags & FLAG_ENABLED) != 0;
    }

    /**
     * Enables swipe-back gesture or not
     *
     * @see #isGestureEnabled()
     */
    public void setGestureEnabled(boolean enable) {
        if (enable) {
            mViewFlags |= FLAG_ENABLED;
        } else {
            mViewFlags &= ~FLAG_ENABLED;
        }
    }

    /**
     * @return the edges for which edge tracking is enabled
     * @see #setEnabledEdges(int)
     */
    @Edge
    public int getEnabledEdges() {
        return mViewFlags & EDGE_MASK;
    }

    /**
     * Enable edge tracking for the selected edges of the content view.
     * The callback's {@link ViewDragCallback#onEdgeTouched(int, int)} and
     * {@link ViewDragCallback#onEdgeDragStarted(int, int)} methods will only be invoked
     * for edges for which edge tracking has been enabled.
     *
     * @param edgeFlags Combination of edge flags describing the edges to watch
     * @see #EDGE_LEFT
     * @see #EDGE_RIGHT
     * @see #getEnabledEdges()
     */
    public void setEnabledEdges(@Edge int edgeFlags) {
        mViewFlags = (mViewFlags & ~EDGE_MASK) | (edgeFlags = edgeFlags & EDGE_MASK);
        mDragHelper.setEdgeTrackingEnabled(edgeFlags);
    }

    /**
     * @see ViewDragHelper#getEdgeSize()
     */
    public int getEdgeSize() {
        return mDragHelper.getEdgeSize();
    }

    /**
     * @return The sensitivity of the dragger between 0 and 1.
     *         Smaller values are less sensitive. 1.0f is normal.
     */
    public float getSensitivity() {
        if (sDraggerTouchSlopField != null) {
            try {
                return (float) mTouchSlop / sDraggerTouchSlopField.getInt(mDragHelper);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return 1.0f;
    }

    /**
     * Sets the sensitivity for the dragger.
     *
     * @param sensitivity value between 0 and 1, the final value for touchSlop is
     *                    {@code ViewConfiguration.get(context).getScaledTouchSlop * (1 / sensitivity)}
     */
    public void setSensitivity(float sensitivity) {
        if (sDraggerTouchSlopField != null) {
            sensitivity = Math.max(0f, Math.min(1.0f, sensitivity));
            try {
                sDraggerTouchSlopField.setInt(mDragHelper,
                        (int) (mTouchSlop * (1.0f / sensitivity) + 0.5f));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the threshold of scroll
     * @see #setScrollThreshold(float)
     */
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
    public float getScrollThreshold() {
        return mScrollThreshold;
    }

    /**
     * Threshold of scroll, we will close the activity or pop up the fragment from the back stack
     * that holds it when the scroll percentage over this value after the content view released.
     *
     * @see ViewDragCallback#onViewReleased(View, float, float)
     * @see #getScrollThreshold()
     */
    public void setScrollThreshold(
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
                    float threshold) {
        if (threshold >= 1 || threshold <= 0) {
            throw new IllegalArgumentException(
                    "Threshold value should be between 0 and 1.0, but your value is " + threshold);
        }
        mScrollThreshold = threshold;
    }

    /**
     * @return the color used to dim the previous content view while the current one is open
     * @see #setScrimColor(int)
     */
    @ColorInt
    public int getScrimColor() {
        return mScrimColor;
    }

    /**
     * Sets a color to use for the scrim that obscures previous content view
     * while the current one is open.
     *
     * @param color Color to use in 0xAARRGGBB format.
     * @see #getScrimColor()
     */
    public void setScrimColor(@ColorInt int color) {
        if (mScrimColor != color) {
            mScrimColor = color;
            if (mScrollPercent > 0 && mDragHelper.getViewDragState() != STATE_SETTLING) {
                // redraw scrim
                invalidate();
            }
        }
    }

    /**
     * Sets a drawable used for edge shadows.
     *
     * @param resId     Resource of drawable to use
     * @param edgeFlags Edge flags describing the edges to set
     * @see #EDGE_LEFT
     * @see #EDGE_RIGHT
     * @see #setEdgeShadow(Drawable, int)
     */
    public void setEdgeShadow(@DrawableRes int resId, @Edge int edgeFlags) {
        setEdgeShadow(ContextCompat.getDrawable(getContext(), resId), edgeFlags);
    }

    /**
     * Sets a drawable used for edge shadows.
     *
     * @param shadow    Drawable to use
     * @param edgeFlags Edge flags describing the edges to set
     * @see #EDGE_LEFT
     * @see #EDGE_RIGHT
     * @see #setEdgeShadow(int, int)
     */
    public void setEdgeShadow(@Nullable Drawable shadow, @Edge int edgeFlags) {
        boolean shadowChanged = false;
        if ((edgeFlags & EDGE_LEFT) != 0 && mShadowLeft != shadow) {
            mShadowLeft = shadow;
            shadowChanged = true;
        }
        if ((edgeFlags & EDGE_RIGHT) != 0 && mShadowRight != shadow) {
            mShadowRight = shadow;
            shadowChanged = true;
        }
        if (shadowChanged && mScrollPercent > 0 && mDragHelper.getViewDragState() != STATE_SETTLING) {
            // redraw edge shadow
            invalidate();
        }
    }

    /**
     * @return whether to scroll the previous view (the content view of the last activity or fragment)
     *         at the same time as the current layout scrolls.
     * @see #setPreviousContentScrollable(boolean)
     */
    public boolean isPreviousContentScrollable() {
        return (mViewFlags & FLAG_PREVIOUS_CONTENT_SCROLLABLE) != 0;
    }

    /**
     * When true, we moves the previous content view as the current layout scrolls.
     *
     * @see #isPreviousContentScrollable()
     */
    public void setPreviousContentScrollable(boolean scrollable) {
        if (scrollable) {
            mViewFlags |= FLAG_PREVIOUS_CONTENT_SCROLLABLE;
        } else {
            mViewFlags &= ~FLAG_PREVIOUS_CONTENT_SCROLLABLE;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mContentView != null) {
            mContentView.layout(mContentLeft, top,
                    mContentLeft + mContentView.getMeasuredWidth(), bottom);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final boolean issued = super.drawChild(canvas, child, drawingTime);

        if (child == mContentView && mScrollPercent > 0
                && mDragHelper.getViewDragState() != STATE_IDLE) {
            drawShadow(canvas, child);
            drawScrim(canvas, child);
        }

        return issued;
    }

    private void drawShadow(Canvas canvas, View child) {
        child.getHitRect(mTempRect);

        switch (mTrackingEdge) {
            case EDGE_LEFT:
                if (mShadowLeft != null) {
                    mShadowLeft.setBounds(
                            mTempRect.left - mShadowLeft.getIntrinsicWidth(), mTempRect.top,
                            mTempRect.left, mTempRect.bottom);
                    mShadowLeft.setAlpha((int) (mScrimOpacity * (float) FULL_ALPHA + 0.5f));
                    mShadowLeft.draw(canvas);
                }
                break;
            case EDGE_RIGHT:
                if (mShadowRight != null) {
                    mShadowRight.setBounds(mTempRect.right, mTempRect.top,
                            mTempRect.right + mShadowRight.getIntrinsicWidth(), mTempRect.bottom);
                    mShadowRight.setAlpha((int) (mScrimOpacity * (float) FULL_ALPHA + 0.5f));
                    mShadowRight.draw(canvas);
                }
                break;
        }
    }

    private void drawScrim(Canvas canvas, View child) {
        final int baseAlpha = (mScrimColor & 0xFF000000) >>> 24;
        final int alpha = (int) ((float) baseAlpha * mScrimOpacity + 0.5f);
        final int color = alpha << 24 | (mScrimColor & 0x00FFFFFF);

        switch (mTrackingEdge) {
            case EDGE_LEFT:
                canvas.clipRect(0, 0, child.getLeft(), getHeight());
                break;
            case EDGE_RIGHT:
                canvas.clipRect(child.getRight(), 0, getRight(), getHeight());
                break;
        }
        canvas.drawColor(color);
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if ((mViewFlags & FLAG_ENABLED) == 0) {
            return false;
        }
        try {
            if (mDragHelper.shouldInterceptTouchEvent(event)) {
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((mViewFlags & FLAG_ENABLED) == 0) {
            return false;
        }
        mDragHelper.processTouchEvent(event);
        return true;
    }

    private final class ViewDragCallback extends ViewDragHelper.Callback {
        ViewDragCallback() {
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (child != mContentView) {
                return false;
            }

            if (mDragHelper.isEdgeTouched(mViewFlags & EDGE_MASK, pointerId)) {
                if (mDragHelper.isEdgeTouched(EDGE_LEFT, pointerId)) {
                    mTrackingEdge = EDGE_LEFT;
                } else if (mDragHelper.isEdgeTouched(EDGE_RIGHT, pointerId)) {
                    mTrackingEdge = EDGE_RIGHT;
                }
                // Only when the distance in vertical the user's finger traveled is not more than
                // the distance to travel before a drag may begin can we capture it.
                if (mTrackingEdge == EDGE_LEFT || mTrackingEdge == EDGE_RIGHT) {
                    return !mDragHelper.checkTouchSlop(ViewDragHelper.DIRECTION_VERTICAL, pointerId);
                }
            }
            return false;
        }

        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            prepareForSlidingContents();
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            final float oldScrollPercent = mScrollPercent;
            mScrollPercent = Math.min(Math.abs((float) left / (float) mContentView.getWidth()), 1);
            if (mScrollPercent != oldScrollPercent) {
                if (mSwipeListeners != null) {
                    for (int i = mSwipeListeners.size() - 1; i >= 0; i--) {
                        mSwipeListeners.get(i).onScrollPercentChange(mTrackingEdge, mScrollPercent);
                    }
                }

                if ((mViewFlags & FLAG_PREVIOUS_CONTENT_SCROLLABLE) != 0) {
                    movePreviousContent();
                }

                mScrimOpacity = 1 - mScrollPercent;
                mContentLeft = left;
                // redraw scrim and shadow
                invalidate();
            }
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            if ((mViewFlags & (EDGE_LEFT | EDGE_RIGHT)) != 0 &&
                    (mFragment != null || mActivity != null && mActivity.canSwipeBackToFinish())) {
                return 1;
            }
            return 0;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            switch (mTrackingEdge) {
                case EDGE_LEFT:
                    return Math.min(child.getWidth(), Math.max(left, 0));
                case EDGE_RIGHT:
                    return Math.min(0, Math.max(left, -child.getWidth()));
            }
            return left;
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            int finalLeft = 0;
            switch (mTrackingEdge) {
                case EDGE_LEFT:
                    if (xvel > 0 || xvel == 0 && mScrollPercent > mScrollThreshold) {
                        finalLeft = releasedChild.getWidth() +
                                (mShadowLeft == null ? 0 : mShadowLeft.getIntrinsicWidth());
                    }
                    break;
                case EDGE_RIGHT:
                    if (xvel < 0 || xvel == 0 && mScrollPercent > mScrollThreshold) {
                        finalLeft = -(releasedChild.getWidth() +
                                (mShadowRight == null ? 0 : mShadowRight.getIntrinsicWidth()));
                    }
                    break;
            }
            mDragHelper.settleCapturedViewAt(finalLeft, 0);
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mSwipeListeners != null) {
                for (int i = mSwipeListeners.size() - 1; i >= 0; i--) {
                    mSwipeListeners.get(i).onScrollStateChange(mTrackingEdge, state);
                }
            }

            if (state == STATE_IDLE) {
                if (mScrollPercent == 1) {
                    // Disable the transition animations of the fragments while the current one
                    // is being popped up.
                    if (mFragment != null && !((Fragment) mFragment).isDetached()) {
                        ISwipeBackFragment prefragment = mFragment.getPreviousFragment();
                        if (prefragment != null)
                            prefragment.setTransitionEnabled(false);

                        mFragment.setTransitionEnabled(false);
                        ((Fragment) mFragment).requireFragmentManager().popBackStackImmediate();
                        mFragment.setTransitionEnabled(true);

                        if (prefragment != null)
                            prefragment.setTransitionEnabled(true);

                    } else if (mActivity != null && !((Activity) mActivity).isFinishing()) {
                        ((Activity) mActivity).finish();
                        ((Activity) mActivity).overridePendingTransition(0, 0);
                    }
                } else {
                    mTrackingEdge = NO_TRACKING_EDGE;
                    // If this view has been settled at its original position, lay the previous
                    // content view back to ensure it will be shown normally after user presses
                    // the return key to finish current activity.
                    layPreviousContentBack();
//                    if (mActivity != null) {
//                        Utils.convertActivityToOpaque((Activity) mActivity);
//                    }
                }
            }
        }
    }

    /*synthetic*/ void prepareForSlidingContents() {
        // If the current view is in the fragment, set the view of the previous fragment
        // to be visible before the scroll started.
        if (mFragment != null) {
            Fragment prefragment = (Fragment) mFragment.getPreviousFragment();
            if (prefragment != null && prefragment.getView() != null) {
                prefragment.getView().setVisibility(VISIBLE);
            }
            // Or else convert the background of the activity that contains it
            // to transparency, then the previous activity will be visible.
        } else {
            Utils.convertActivityToTranslucent((Activity) mActivity);
        }
    }

    /**
     * @return the previous content view of the activity {@link #mActivity} or
     *         the fragment {@link #mFragment}
     */
    @Nullable
    private View getPreviousContent() {
        if (mFragment != null) {
            ISwipeBackFragment prefragment = mFragment.getPreviousFragment();
            if (prefragment != null)
                return prefragment.getSwipeBackLayout();

        } else if (mActivity != null) {
            Activity preactivity = mActivity.getPreviousActivity();
            if (preactivity != null)
                return ((ViewGroup) preactivity.getWindow().getDecorView()).getChildAt(0);
        }
        return null;
    }

    /**
     * Translate the previous content view {@link #getPreviousContent()}
     */
    /*synthetic*/ void movePreviousContent() {
        View view = getPreviousContent();
        if (view != null) {
            float translationX = 0;
            switch (mTrackingEdge) {
                case EDGE_LEFT:
                    translationX = mScrollThreshold * (mScrollPercent - 1f) * view.getWidth();
                    break;
                case EDGE_RIGHT:
                    translationX = mScrollThreshold * (1f - mScrollPercent) * view.getWidth();
                    break;
            }
            view.setTranslationX(translationX);
        }
    }

    /**
     * Translate the previous content view {@link #getPreviousContent()} back to
     * its original position (0,0)
     */
    /*synthetic*/ void layPreviousContentBack() {
        View view = getPreviousContent();
        if (view != null) {
            view.setTranslationX(0);
        }
    }

    /**
     * Scroll out content view and finish the activity or pop up the fragment from
     * the back stack holding it
     */
    public void scrollToFinishActivityOrPopUpFragment() {
        prepareForSlidingContents();

        int left = 0;
        if ((mViewFlags & EDGE_LEFT) != 0) {
            left = mContentView.getWidth() +
                    (mShadowLeft == null ? 0 : mShadowLeft.getIntrinsicWidth());
            mTrackingEdge = EDGE_LEFT;
        } else if ((mViewFlags & EDGE_RIGHT) != 0) {
            left = -(mContentView.getWidth() +
                    (mShadowRight == null ? 0 : mShadowRight.getIntrinsicWidth()));
            mTrackingEdge = EDGE_RIGHT;
        }

        mDragHelper.smoothSlideViewTo(mContentView, left, 0);
        invalidate();
    }

    /**
     * Adds a callback to be invoked when a swipe event is sent to this view.
     *
     * @param listener the swipe listener {@link SwipeListener} to attach to this view
     */
    public void addSwipeListener(SwipeListener listener) {
        if (mSwipeListeners == null) {
            mSwipeListeners = new ArrayList<>(1);

        } else if (mSwipeListeners.contains(listener)) {
            return;
        }
        mSwipeListeners.add(listener);
    }

    /**
     * Removes a swipe listener from the set of listeners
     *
     * @param listener the swipe listener {@link SwipeListener} already attached to this view
     */
    public void removeSwipeListener(SwipeListener listener) {
        if (mSwipeListeners != null) {
            mSwipeListeners.remove(listener);
        }
    }

    public interface SwipeListener {
        /**
         * Invoked as the scroll state changes
         *
         * @param edge  edge flag describing the edge being dragged
         * @param state constant to describe scroll state
         * @see #STATE_IDLE
         * @see #STATE_DRAGGING
         * @see #STATE_SETTLING
         */
        void onScrollStateChange(@Edge int edge, @ScrollState int state);

        /**
         * Invoked as the scroll percentage changes
         *
         * @param edge    edge flag describing the edge being dragged
         * @param percent scroll percentage of the content view
         */
        void onScrollPercentChange(@Edge int edge, @FloatRange(from = 0.0, to = 1.0) float percent);
    }
}
