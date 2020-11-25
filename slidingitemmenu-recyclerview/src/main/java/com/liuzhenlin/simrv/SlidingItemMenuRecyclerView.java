/*
 * Created on 2017/12/16.
 * Copyright © 2017–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.simrv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:2233788867@qq.com">刘振林</a>
 */
public class SlidingItemMenuRecyclerView extends RecyclerView {
    private static final String TAG = "SlidingItemMenuRecyclerView";

    private boolean mIsVerticalScrollBarEnabled;

    /**
     * @see #isItemDraggable()
     * @see #setItemDraggable(boolean)
     */
    private boolean mIsItemDraggable;

    /** True, if an item view is being dragged by the user. */
    private boolean mIsItemBeingDragged;

    /**
     * Whether or not some item view is fully open when this view receives the
     * {@link MotionEvent#ACTION_DOWN} event.
     */
    private boolean mHasItemFullyOpenOnActionDown;

    /** Distance to travel before drag may begin */
    protected final int mTouchSlop;

    private int mDownX;
    private int mDownY;

    private final float[] mTouchX = new float[2];
    private final float[] mTouchY = new float[2];

    private VelocityTracker mVelocityTracker;

    /** Minimum gesture speed along the x axis to automatically scroll item views */
    private final float mItemMinimumFlingVelocity; // 200 dp/s

    /**
     * The bounds of the currently touched item View {@link #mActiveItem} (relative to current view).
     */
    private final Rect mActiveItemBounds = new Rect();
    /**
     * The bounds of the currently touched item view's menu (relative to current view).
     */
    private final Rect mActiveItemMenuBounds = new Rect();

    /** The item view that is currently being touched or dragged by the user */
    private ViewGroup mActiveItem;

    /** The item view that is fully open or to be opened through the animator associated to it */
    private ViewGroup mFullyOpenedItem;

    /** The set of opened item views */
    private final List<ViewGroup> mOpenedItems = new LinkedList<>();

    /** Tag used to get the width of an item view's menu */
    private static final int TAG_ITEM_MENU_WIDTH = R.id.tag_itemMenuWidth;

    /** Tag used to get the widths of the menu items of an item view */
    private static final int TAG_MENU_ITEM_WIDTHS = R.id.tag_menuItemWidths;

    /** Tag used to get the animator of the item view to which it associated */
    private static final int TAG_ITEM_ANIMATOR = R.id.tag_itemAnimator;

    /**
     * Time interval in milliseconds of automatically scrolling item views
     *
     * @see #getItemScrollDuration()
     * @see #setItemScrollDuration(int)
     */
    private int mItemScrollDuration;

    /** Default value of {@link #mItemScrollDuration} if no value is set for it */
    public static final int DEFAULT_ITEM_SCROLL_DURATION = 500; // ms

    private static final Interpolator sViscousFluidInterpolator =
            new ViscousFluidInterpolator(6.66f);
    private static final Interpolator sOvershootInterpolator =
            new OvershootInterpolator(1.0f);

    /**
     * @deprecated Use {@link #isItemDraggable()} instead
     */
    @Deprecated
    public boolean isItemScrollingEnabled() {
        return isItemDraggable();
    }

    /**
     * @return whether it is enabled to scroll item views in touch mode or not
     */
    public boolean isItemDraggable() {
        return mIsItemDraggable;
    }

    /**
     * @deprecated Use {@link #setItemDraggable(boolean)} instead
     */
    @Deprecated
    public void setItemScrollingEnabled(boolean enabled) {
        setItemDraggable(enabled);
    }

    /**
     * Sets whether the item views can be dragged by user.
     * <p>
     * If unable to be dragged, they may be scrolled through the code like:
     * <code>simrv.openItemAtPosition(0, true);</code>
     */
    public void setItemDraggable(boolean draggable) {
        mIsItemDraggable = draggable;
    }

    /**
     * Gets the lasting time of the animator for opening/closing the item view to which
     * the animator associated.
     * The default duration is {@value DEFAULT_ITEM_SCROLL_DURATION} milliseconds.
     *
     * @return the duration of the animator
     */
    public int getItemScrollDuration() {
        return mItemScrollDuration;
    }

    /**
     * Sets the duration for the animators used to open/close the item views.
     *
     * @throws IllegalArgumentException if a negative 'duration' is passed in
     */
    public void setItemScrollDuration(int duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("The animators for opening/closing the item views " +
                    "cannot have negative duration: " + duration);
        }
        mItemScrollDuration = duration;
    }

    public SlidingItemMenuRecyclerView(Context context) {
        this(context, null);
    }

    public SlidingItemMenuRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingItemMenuRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mItemMinimumFlingVelocity = 200f * getResources().getDisplayMetrics().density;

        final TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.SlidingItemMenuRecyclerView, defStyle, 0);
        if (ta.hasValue(R.styleable.SlidingItemMenuRecyclerView_itemDraggable)) {
            setItemDraggable(ta.getBoolean(R.styleable
                    .SlidingItemMenuRecyclerView_itemDraggable, true));
        } else {
            // Libraries with version code prior to 5 use the itemScrollingEnabled attr only.
            setItemDraggable(ta.getBoolean(R.styleable
                    .SlidingItemMenuRecyclerView_itemScrollingEnabled /* deprecated */, true));
        }
        setItemScrollDuration(ta.getInteger(R.styleable
                .SlidingItemMenuRecyclerView_itemScrollDuration, DEFAULT_ITEM_SCROLL_DURATION));
        ta.recycle();
    }

    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        mIsVerticalScrollBarEnabled = verticalScrollBarEnabled;
        super.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
    }

    private boolean childHasMenu(ViewGroup itemView) {
        if (itemView.getVisibility() != VISIBLE) return false;

        final int itemChildCount = itemView.getChildCount();
        final View itemLastChild = itemView.getChildAt(itemChildCount >= 2 ?
                itemChildCount - 1 : 1);
        if (!(itemLastChild instanceof FrameLayout)) return false;

        final FrameLayout itemMenu = (FrameLayout) itemLastChild;
        final int menuItemCount = itemMenu.getChildCount();
        final int[] menuItemWidths = new int[menuItemCount];
        int itemMenuWidth = 0;
        for (int i = 0; i < menuItemCount; i++) {
            //@formatter:off
            menuItemWidths[i] = ((FrameLayout) itemMenu
                                .getChildAt(i))
                                .getChildAt(0)
                                .getWidth();
            //@formatter:on
            itemMenuWidth += menuItemWidths[i];
        }
        if (itemMenuWidth > 0) {
            itemView.setTag(TAG_ITEM_MENU_WIDTH, itemMenuWidth);
            itemView.setTag(TAG_MENU_ITEM_WIDTHS, menuItemWidths);
            return true;
        }
        return false;
    }

    private void resolveActiveItemMenuBounds() {
        final int itemMenuWidth = (int) mActiveItem.getTag(TAG_ITEM_MENU_WIDTH);
        final int left = Utils.isLayoutRtl(mActiveItem) ?
                0 : mActiveItem.getRight() - itemMenuWidth;
        final int right = left + itemMenuWidth;
        mActiveItemMenuBounds.set(left, mActiveItemBounds.top,
                right, mActiveItemBounds.bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        final int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            // Reset things for a new event stream, just in case we didn't get
            // the whole previous stream.
            resetTouch();
        }

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(e);

        boolean intercept = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = Utils.roundFloat(e.getX());
                mDownY = Utils.roundFloat(e.getY());
                markCurrTouchPoint(mDownX, mDownY);

                for (int i = getChildCount() - 1; i >= 0; i--) {
                    final View child = getChildAt(i);
                    if (!(child instanceof ViewGroup)) continue;

                    final ViewGroup itemView = (ViewGroup) child;
                    itemView.getHitRect(mActiveItemBounds);
                    if (!mActiveItemBounds.contains(mDownX, mDownY)) continue;

                    if (childHasMenu(itemView)) {
                        mActiveItem = itemView;
                    }
                    break;
                }

                if (mOpenedItems.size() == 0) break;
                // Disallow our parent Views to intercept the touch events so long as there is
                // at least one item view in the open or being closed state.
                requestParentDisallowInterceptTouchEvent();
                if (mFullyOpenedItem != null) {
                    mHasItemFullyOpenOnActionDown = true;
                    if (mActiveItem == mFullyOpenedItem) {
                        resolveActiveItemMenuBounds();
                        // If the user's finger downs on the completely opened itemView's menu area,
                        // do not intercept the subsequent touch events (ACTION_MOVE, ACTION_UP, etc.)
                        // as we receive the ACTION_DOWN event.
                        if (mActiveItemMenuBounds.contains(mDownX, mDownY)) {
                            break;
                            // If the user's finger downs on the fully opened itemView but not on
                            // its menu, then we need to intercept them.
                        } else if (mActiveItemBounds.contains(mDownX, mDownY)) {
                            return true;
                        }
                    }
                    // If 1) the fully opened itemView is not the current one or 2) the user's
                    // finger downs outside of the area in which this view displays the itemViews,
                    // make the itemView's menu hidden and intercept the subsequent touch events.
                    releaseItemViewInternal(mFullyOpenedItem, mItemScrollDuration);
                }
                // Intercept the next touch events as long as there exists some item view open
                // (full open is not necessary for it). This prevents the onClick() method of
                // the pressed child from being called in the pending ACTION_UP event.
                return true;

            case MotionEvent.ACTION_MOVE:
                markCurrTouchPoint(e.getX(), e.getY());

                intercept = tryHandleItemScrollingEvent();
                // If the user initially put his/her finger down on the fully opened itemView's menu,
                // disallow our parent class to intercept the touch events since we will do that
                // as the user tends to scroll the current touched itemView horizontally.
                if (mHasItemFullyOpenOnActionDown && mActiveItemMenuBounds.contains(mDownX, mDownY)) {
                    return intercept;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // If the user initially placed his/her finger on the fully opened itemView's menu
                // and has clicked it or has not scrolled that itemView, hide it as his/her last
                // finger touching the screen lifts.
                if (mHasItemFullyOpenOnActionDown && mActiveItemMenuBounds.contains(mDownX, mDownY)) {
                    releaseItemView(true);
                }
                clearTouch();
                break;
        }
        return intercept || super.onInterceptTouchEvent(e);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mIsVerticalScrollBarEnabled) {
            // Makes the vertical scroll bar disappear while an itemView is being dragged.
            super.setVerticalScrollBarEnabled(!mIsItemBeingDragged);
        }

        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(e);

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                if (mIsItemBeingDragged || mHasItemFullyOpenOnActionDown || mOpenedItems.size() > 0) {
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                markCurrTouchPoint(e.getX(), e.getY());

                if (!mIsItemDraggable && cancelTouch()) {
                    return true;
                }
                if (mIsItemBeingDragged) {
                    // Positive when the user's finger slides towards the right.
                    float dx = mTouchX[mTouchX.length - 1] - mTouchX[mTouchX.length - 2];
                    // Positive when the itemView scrolls towards the right.
                    final float translationX = mActiveItem.getChildAt(0).getTranslationX();
                    final boolean rtl = Utils.isLayoutRtl(mActiveItem);
                    final int finalXFromEndToStart = rtl
                            ? (int) mActiveItem.getTag(TAG_ITEM_MENU_WIDTH)
                            : -(int) (mActiveItem.getTag(TAG_ITEM_MENU_WIDTH));
                    // Swipe the itemView towards the horizontal start over the width of
                    // the itemView's menu.
                    if (!rtl && dx + translationX < finalXFromEndToStart
                            || rtl && dx + translationX > finalXFromEndToStart) {
                        dx = dx / 3f;
                        // Swipe the itemView towards the end of horizontal to (0,0).
                    } else if (!rtl && dx + translationX > 0 || rtl && dx + translationX < 0) {
                        dx = 0 - translationX;
                    }
                    translateItemViewXBy(mActiveItem, dx);

                    // Consume this touch event and do not invoke the method onTouchEvent(e) of
                    // the parent class to temporarily make this view unable to scroll up or down.
                    return true;
                } else {
                    // If there existed itemView whose menu was fully open when the user initially
                    // put his/her finger down, always consume the touch event and only when the item
                    // has a tend of scrolling horizontally will we handle the next events.
                    if (mHasItemFullyOpenOnActionDown | tryHandleItemScrollingEvent()) {
                        return true;
                    }
                    // Disallow current view to scroll while an/some item view(s) is/are scrolling.
                    if (mOpenedItems.size() > 0) {
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsItemDraggable && mIsItemBeingDragged) {
                    final boolean rtl = Utils.isLayoutRtl(mActiveItem);
                    final float translationX = mActiveItem.getChildAt(0).getTranslationX();
                    final int itemMenuWidth = (int) mActiveItem.getTag(TAG_ITEM_MENU_WIDTH);
                    //noinspection StatementWithEmptyBody
                    if (translationX == 0) { // itemView's menu is closed

                        // itemView's menu is totally opened
                    } else if (!rtl && translationX == -itemMenuWidth
                            || rtl && translationX == itemMenuWidth) {
                        mFullyOpenedItem = mActiveItem;

                    } else {
                        final float dx = rtl
                                ? mTouchX[mTouchX.length - 2] - mTouchX[mTouchX.length - 1]
                                : mTouchX[mTouchX.length - 1] - mTouchX[mTouchX.length - 2];
                        mVelocityTracker.computeCurrentVelocity(1000);
                        final float velocityX = Math.abs(mVelocityTracker.getXVelocity());
                        // If the speed at which the user's finger lifted is greater than 200 dp/s
                        // while user was scrolling itemView towards the horizontal start,
                        // make it automatically scroll to open and show its menu.
                        if (dx < 0 && velocityX >= mItemMinimumFlingVelocity) {
                            smoothTranslateItemViewXTo(
                                    mActiveItem,
                                    rtl ? itemMenuWidth : -itemMenuWidth,
                                    mItemScrollDuration);
                            mFullyOpenedItem = mActiveItem;
                            clearTouch();
                            cancelParentTouch(e);
                            return true;

                            // If the speed at which the user's finger lifted is greater than 200 dp/s
                            // while user was scrolling itemView towards the end of horizontal,
                            // make its menu hidden.
                        } else if (dx > 0 && velocityX >= mItemMinimumFlingVelocity) {
                            releaseItemView(true);
                            clearTouch();
                            cancelParentTouch(e);
                            return true;
                        }

                        final float middle = itemMenuWidth / 2f;
                        // If the sliding distance is less than half of its slidable distance,
                        // hide its menu,
                        if (Math.abs(translationX) < middle) {
                            releaseItemView(true);

                            // else open its menu.
                        } else {
                            smoothTranslateItemViewXTo(
                                    mActiveItem,
                                    rtl ? itemMenuWidth : -itemMenuWidth,
                                    mItemScrollDuration);
                            mFullyOpenedItem = mActiveItem;
                        }
                    }
                    clearTouch();
                    cancelParentTouch(e);
                    return true; // Returns true here in case of a fling started in this up event.
                }
            case MotionEvent.ACTION_CANCEL:
                cancelTouch();
                break;
        }

        return super.onTouchEvent(e);
    }

    private void markCurrTouchPoint(float x, float y) {
        System.arraycopy(mTouchX, 1, mTouchX, 0, mTouchX.length - 1);
        mTouchX[mTouchX.length - 1] = x;
        System.arraycopy(mTouchY, 1, mTouchY, 0, mTouchY.length - 1);
        mTouchY[mTouchY.length - 1] = y;
    }

    private boolean tryHandleItemScrollingEvent() {
        if (mActiveItem == null /* There's no scrollable itemView being touched by user */
                || !mIsItemDraggable /* Unable to scroll it */
                || getScrollState() != SCROLL_STATE_IDLE /* The list may be currently scrolling */) {
            return false;
        }
        // The layout's orientation may not be vertical.
        //noinspection ConstantConditions
        if (getLayoutManager().canScrollHorizontally()) {
            return false;
        }

        final float absDy = Math.abs(mTouchY[mTouchY.length - 1] - mDownY);
        if (absDy <= mTouchSlop) {
            final float dx = mTouchX[mTouchX.length - 1] - mDownX;
            if (mOpenedItems.size() == 0) {
                final boolean rtl = Utils.isLayoutRtl(mActiveItem);
                mIsItemBeingDragged = rtl && dx > mTouchSlop || !rtl && dx < -mTouchSlop;
            } else {
                mIsItemBeingDragged = Math.abs(dx) > mTouchSlop;
            }
            if (mIsItemBeingDragged) {
                requestParentDisallowInterceptTouchEvent();
                return true;
            }
        }
        return false;
    }

    private void requestParentDisallowInterceptTouchEvent() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private boolean cancelTouch() {
        return cancelTouch(true);
    }

    private boolean cancelTouch(boolean animate) {
        if (mIsItemBeingDragged) {
            releaseItemView(animate);
            clearTouch();
            return true;
        }
        // 1. If the itemView previously opened equals the current touched one and
        //    the user hasn't scrolled it since he/she initially put his/her finger down,
        //    hide it on the movements canceled.
        // 2. If the previously opened itemView differs from the one currently touched,
        //    and the current one has not been scrolled at all, set 'mActiveItem' to null.
        if (mHasItemFullyOpenOnActionDown) {
            if (mActiveItem == mFullyOpenedItem) {
                releaseItemView(animate);
            }
            clearTouch();
            return true;
        }
        return false;
    }

    private void clearTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        resetTouch();
    }

    private void resetTouch() {
        mActiveItem = null;
        mHasItemFullyOpenOnActionDown = false;
        mActiveItemBounds.setEmpty();
        mActiveItemMenuBounds.setEmpty();
        mIsItemBeingDragged = false;
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    private void cancelParentTouch(MotionEvent e) {
        final int action = e.getAction();
        e.setAction(MotionEvent.ACTION_CANCEL);
        super.onTouchEvent(e);
        e.setAction(action);
    }

    /**
     * Smoothly scrolls the current item view whose menu is open back to its original position.
     *
     * @see #releaseItemView(boolean)
     */
    public void releaseItemView() {
        releaseItemView(true);
    }

    /**
     * Scrolls the current item view whose menu is open back to its original position.
     *
     * @param animate whether this scroll should be smooth
     */
    public void releaseItemView(boolean animate) {
        releaseItemViewInternal(mIsItemBeingDragged ? mActiveItem : mFullyOpenedItem,
                animate ? mItemScrollDuration : 0);
    }

    private void releaseItemViewInternal(ViewGroup itemView, int duration) {
        if (itemView != null) {
            if (duration > 0) {
                smoothTranslateItemViewXTo(itemView, 0, duration);
            } else {
                translateItemViewXTo(itemView, 0);
            }
            if (mFullyOpenedItem == itemView) {
                mFullyOpenedItem = null;
            }
        }
    }

    /**
     * Smoothly opens the menu of the item view at the specified adapter position
     *
     * @param position the position of the item in the data set of the adapter
     * @return true if the menu of the child view that represents the given position can be opened;
     *         false if the position is not laid out or the item does not have a menu.
     * @see #openItemAtPosition(int, boolean)
     */
    public boolean openItemAtPosition(int position) {
        return openItemAtPosition(position, true);
    }

    /**
     * Opens the menu of the item view at the specified adapter position
     *
     * @param position the position of the item in the data set of the adapter
     * @param animate  whether this scroll should be smooth
     * @return true if the menu of the child view that represents the given position can be opened;
     *         false if the position is not laid out or the item does not have a menu.
     */
    public boolean openItemAtPosition(int position, boolean animate) {
        final LayoutManager lm = getLayoutManager();
        if (lm == null) return false;

        final View view = lm.findViewByPosition(position);
        if (!(view instanceof ViewGroup)) return false;

        final ViewGroup itemView = (ViewGroup) view;
        if (mFullyOpenedItem != itemView && childHasMenu(itemView)) {
            // First, cancels the item view being touched or previously fully opened (if any)
            if (!cancelTouch(animate)) {
                releaseItemView(animate);
            }

            smoothTranslateItemViewXTo(
                    itemView,
                    Utils.isLayoutRtl(itemView)
                            ? (int) itemView.getTag(TAG_ITEM_MENU_WIDTH)
                            : -(int) (itemView.getTag(TAG_ITEM_MENU_WIDTH)),
                    animate ? mItemScrollDuration : 0);
            mFullyOpenedItem = itemView;
            return true;
        }
        return false;
    }

    private void smoothTranslateItemViewXTo(ViewGroup itemView, float x, int duration) {
        smoothTranslateItemViewXBy(itemView, x - itemView.getChildAt(0).getTranslationX(),
                duration);
    }

    private void smoothTranslateItemViewXBy(ViewGroup itemView, float dx, int duration) {
        TranslateItemViewXAnimator animator =
                (TranslateItemViewXAnimator) itemView.getTag(TAG_ITEM_ANIMATOR);

        if (dx != 0 && duration > 0) {
            boolean canceled = false;
            if (animator == null) {
                animator = new TranslateItemViewXAnimator(this, itemView);
                itemView.setTag(TAG_ITEM_ANIMATOR, animator);

            } else if (animator.isRunning()) {
                animator.removeListener(animator.listener);
                animator.cancel();
                canceled = true;
            }
            animator.setFloatValues(0, dx);

            final boolean rtl = Utils.isLayoutRtl(itemView);
            final Interpolator interpolator = !rtl && dx < 0 || rtl && dx > 0 ?
                    sOvershootInterpolator : sViscousFluidInterpolator;

            animator.setInterpolator(interpolator);
            animator.setDuration(duration);
            animator.start();
            if (canceled) {
                animator.addListener(animator.listener);
            }
        } else {
            // Checks if there is an animator running for the given item view even if dx == 0
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            // If duration <= 0, then scroll the 'itemView' directly to prevent a redundant call
            // to the animator.
            baseTranslateItemViewXBy(itemView, dx);
        }
    }

    private void translateItemViewXTo(ViewGroup itemView, @SuppressWarnings("SameParameterValue") float x) {
        translateItemViewXBy(itemView, x - itemView.getChildAt(0).getTranslationX());
    }

    private void translateItemViewXBy(ViewGroup itemView, float dx) {
        final TranslateItemViewXAnimator animator =
                (TranslateItemViewXAnimator) itemView.getTag(TAG_ITEM_ANIMATOR);
        if (animator != null && animator.isRunning()) {
            // Cancels the running animator associated to the 'itemView' as we horizontally
            // scroll it to a position immediately to avoid inconsistencies in its translation X.
            animator.cancel();
        }

        baseTranslateItemViewXBy(itemView, dx);
    }

    /*
     * This method does not cancel the translation animator of the 'itemView', for which it is used
     * to update the item view's horizontal scrolled position.
     */
    /*synthetic*/ void baseTranslateItemViewXBy(ViewGroup itemView, float dx) {
        if (dx == 0) return;

        final float translationX = itemView.getChildAt(0).getTranslationX() + dx;
        final int itemMenuWidth = (int) itemView.getTag(TAG_ITEM_MENU_WIDTH);

        final boolean rtl = Utils.isLayoutRtl(itemView);
        if (!rtl && translationX > -itemMenuWidth * 0.05f
                || rtl && translationX < itemMenuWidth * 0.05f) {
            mOpenedItems.remove(itemView);

        } else if (!mOpenedItems.contains(itemView)) {
            mOpenedItems.add(itemView);
        }

        final int itemChildCount = itemView.getChildCount();
        for (int i = 0; i < itemChildCount; i++) {
            itemView.getChildAt(i).setTranslationX(translationX);
        }

        final FrameLayout itemMenu = (FrameLayout) itemView.getChildAt(itemChildCount - 1);
        final int[] menuItemWidths = (int[]) itemView.getTag(TAG_MENU_ITEM_WIDTHS);
        float menuItemFrameDx = 0;
        for (int i = 1, menuItemCount = itemMenu.getChildCount(); i < menuItemCount; i++) {
            final FrameLayout menuItemFrame = (FrameLayout) itemMenu.getChildAt(i);
            menuItemFrameDx -= dx * (float) menuItemWidths[i - 1] / (float) itemMenuWidth;
            menuItemFrame.setTranslationX(menuItemFrame.getTranslationX() + menuItemFrameDx);
        }
    }

    private static final class TranslateItemViewXAnimator extends ValueAnimator {
        final AnimatorListener listener;

        float cachedDeltaTransX;

        TranslateItemViewXAnimator(final SlidingItemMenuRecyclerView parent, final ViewGroup itemView) {
            listener = new AnimatorListenerAdapter() {
                final SimpleArrayMap<View, /* Layer Type */ Integer> childrenLayerTypes =
                        new SimpleArrayMap<>(0);

                void ensureChildrenLayerTypes() {
                    final int itemChildCount = itemView.getChildCount();
                    final ViewGroup itemMenu = (ViewGroup) itemView.getChildAt(
                            itemChildCount - 1);
                    final int menuItemCount = itemMenu.getChildCount();

                    // We do not know whether the cached children are valid or not, so just
                    // clear the Map and re-put some children into it, of which the layer types
                    // will also be up-to-date.
                    childrenLayerTypes.clear();
                    childrenLayerTypes.ensureCapacity(
                            itemChildCount - 1 + menuItemCount);
                    for (int i = 0; i < itemChildCount - 1; i++) {
                        final View itemChild = itemView.getChildAt(i);
                        childrenLayerTypes.put(itemChild, itemChild.getLayerType());
                    }
                    for (int i = 0; i < menuItemCount; i++) {
                        final View menuItemFrame = itemMenu.getChildAt(i);
                        childrenLayerTypes.put(menuItemFrame, menuItemFrame.getLayerType());
                    }
                }

                @SuppressLint("ObsoleteSdkInt")
                @Override
                public void onAnimationStart(Animator animation) {
                    ensureChildrenLayerTypes();
                    for (int i = childrenLayerTypes.size() - 1; i >= 0; i--) {
                        final View child = childrenLayerTypes.keyAt(i);
                        child.setLayerType(LAYER_TYPE_HARDWARE, null);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                                && ViewCompat.isAttachedToWindow(child)) {
                            child.buildLayer();
                        }
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    for (int i = childrenLayerTypes.size() - 1; i >= 0; i--) {
                        childrenLayerTypes.keyAt(i).setLayerType(
                                childrenLayerTypes.valueAt(i), null);
                    }
                }
            };
            addListener(listener);
            addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float deltaTransX = (float) animation.getAnimatedValue();
                    parent.baseTranslateItemViewXBy(itemView, deltaTransX - cachedDeltaTransX);
                    cachedDeltaTransX = deltaTransX;
                }
            });
        }

        @Override
        public void start() {
            // NOTE: 'cachedDeltaTransX' MUST be reset before super.start() is invoked
            // for the reason that 'onAnimationUpdate' will be called in the super method
            // on platforms prior to Nougat.
            cachedDeltaTransX = 0;
            super.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseItemViewInternal(mFullyOpenedItem, 0);
        if (mOpenedItems.size() > 0) {
            final ViewGroup[] openedItems = mOpenedItems.toArray(new ViewGroup[0]);
            for (ViewGroup openedItem : openedItems) {
                final Animator animator = (Animator) openedItem.getTag(TAG_ITEM_ANIMATOR);
                if (animator != null && animator.isRunning()) {
                    animator.end();
                }
            }
            mOpenedItems.clear();
        }
    }
}