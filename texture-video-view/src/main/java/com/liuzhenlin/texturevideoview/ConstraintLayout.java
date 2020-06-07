/*
 * Created on 2019/3/29 5:15 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 刘振林
 */
/*package*/ final class ConstraintLayout extends androidx.constraintlayout.widget.ConstraintLayout {
    private TouchInterceptor mTouchInterceptor;

    public interface TouchInterceptor {
        boolean shouldInterceptTouchEvent(@NonNull MotionEvent ev);
    }

    public void setTouchInterceptor(@Nullable TouchInterceptor interceptor) {
        mTouchInterceptor = interceptor;
    }

    public ConstraintLayout(Context context) {
        super(context);
    }

    public ConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mTouchInterceptor != null) {
            return mTouchInterceptor.shouldInterceptTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }
}
