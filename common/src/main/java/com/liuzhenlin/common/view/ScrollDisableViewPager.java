/*
 * Created on 2019/10/19 9:37 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * @author 刘振林
 */
public class ScrollDisableViewPager extends ViewPager {

    private boolean mScrollEnabled = true;
    private boolean mOverScrollEnabled = true;

    private float mDownX;
    private float mDownY;
    private int mActivePointerId;

    protected final int mPagingTouchSlop;

    private int mPageOffsetPixels;

    public ScrollDisableViewPager(@NonNull Context context) {
        this(context, null);
    }

    public ScrollDisableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    public void setScrollEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    public boolean isOverScrollEnabled() {
        return mOverScrollEnabled;
    }

    public void setOverScrollEnabled(boolean enabled) {
        mOverScrollEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return shouldParentHandleTouchEvent(ev) && super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return shouldParentHandleTouchEvent(ev) && super.onTouchEvent(ev);
    }

    private boolean shouldParentHandleTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                final int actionIndex = ev.getActionIndex();
                mDownX = ev.getX(actionIndex);
                mDownY = ev.getY(actionIndex);
                mActivePointerId = ev.getPointerId(actionIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mScrollEnabled && !mOverScrollEnabled) {
                    PagerAdapter adapter = getAdapter();
                    if (adapter == null) return false;

                    final int pageCount = adapter.getCount();
                    if (pageCount <= 1) return false;

                    if (mPageOffsetPixels == 0) {
                        final int currentPage = getCurrentItem();
                        if (currentPage == 0) {
                            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                            if (pointerIndex == -1) {
                                return false;
                            }

                            final float dx = ev.getX(pointerIndex) - mDownX;
                            if (dx >= mPagingTouchSlop) {
                                return false;
                            }
                        } else if (currentPage == pageCount - 1) {
                            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                            if (pointerIndex == -1) {
                                return false;
                            }

                            final float dx = ev.getX(pointerIndex) - mDownX;
                            if (dx <= -mPagingTouchSlop) {
                                return false;
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        return mScrollEnabled;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up.
            // Choose a new active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownX = ev.getX(newPointerIndex);
            mDownY = ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        mPageOffsetPixels = offsetPixels;
    }
}
