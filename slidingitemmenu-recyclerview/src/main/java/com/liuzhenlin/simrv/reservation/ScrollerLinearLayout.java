/*
 * Created on 2017/10/21.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.simrv.reservation;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

/**
 * @author 刘振林
 */
public class ScrollerLinearLayout extends LinearLayout implements ScrollerView {
    private final Scroller mScroller;

    public ScrollerLinearLayout(Context context) {
        this(context, null);
    }

    public ScrollerLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
    }

    public Scroller getScroller() {
        return mScroller;
    }

    @Override
    public void smoothScrollBy(int dx, int dy, int duration) {
        if (dx == 0 && dy == 0) {
            // Nothing to do. Cancel animation
            mScroller.abortAnimation();
        } else {
            mScroller.startScroll(getScrollX(), getScrollY(), dx, dy, duration);
            invalidate();
        }
    }

    @Override
    public void smoothScrollTo(int x, int y, int duration) {
        final int scrollX = getScrollX();
        final int scrollY = getScrollY();

        final boolean finished = mScroller.isFinished();
        if (finished && (scrollX != x || scrollY != y) ||
                !finished && (mScroller.getFinalX() != x || mScroller.getFinalY() != y)) {

            final int deltaX = x - scrollX;
            final int deltaY = y - scrollY;
            smoothScrollBy(deltaX, deltaY, duration);
        }
    }

    @Override
    public void computeScroll() {
        // Override to implement the smooth scrolling logic
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
}