/*
 * Created on 2018/11/16 3:27 PM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

/**
 * @author 刘振林
 */
public class ScrollDisableListView extends ListView {
    private int mPressPosition;
    private boolean mScrollEnabled = true;
    private boolean mScrollEnabledOnTouchMoveAfterDisabled;

    public ScrollDisableListView(Context context) {
        super(context);
    }

    public ScrollDisableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollDisableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ScrollDisableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean isScrollEnabled() {
        return mScrollEnabled;
    }

    public void setScrollEnabled(boolean enabled) {
        mScrollEnabled = enabled;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPressPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                if (mScrollEnabled) {
                    mScrollEnabledOnTouchMoveAfterDisabled = mPressPosition == INVALID_POSITION;
                    break;
                }

                if (mScrollEnabledOnTouchMoveAfterDisabled ||
                        mPressPosition != INVALID_POSITION
                                && mPressPosition != pointToPosition((int) ev.getX(), (int) ev.getY())) {
                    mScrollEnabledOnTouchMoveAfterDisabled = false;
                    mPressPosition = INVALID_POSITION;

                    ev.setAction(MotionEvent.ACTION_CANCEL);
                    final boolean consumed = super.onTouchEvent(ev);
                    ev.setAction(MotionEvent.ACTION_MOVE);
                    return consumed;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mScrollEnabledOnTouchMoveAfterDisabled &&
                        !mScrollEnabled && mPressPosition == INVALID_POSITION) {
                    return true;
                }

                mPressPosition = INVALID_POSITION;
                mScrollEnabledOnTouchMoveAfterDisabled = false;
                break;
        }
        return super.onTouchEvent(ev);
    }
}
