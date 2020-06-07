/*
 * Created on 2019/3/23 5:06 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.observer;

import android.content.Context;
import android.view.OrientationEventListener;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

/**
 * @author 刘振林
 */
public abstract class OnOrientationChangeListener {
    @ScreenOrientation
    private int mOrientation;
    private final OrientationEventListener mListener;

    @IntDef({
            SCREEN_ORIENTATION_PORTRAIT,
            SCREEN_ORIENTATION_LANDSCAPE,
            SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenOrientation {
    }

    public OnOrientationChangeListener(@NonNull Context context) {
        this(context, SCREEN_ORIENTATION_PORTRAIT);
    }

    public OnOrientationChangeListener(
            @NonNull Context context, @ScreenOrientation int initialOrientation) {
        mOrientation = initialOrientation;
        mListener = new OrientationEventListener(context.getApplicationContext()) {
            @Override
            public void onOrientationChanged(int rotation) {
                // 手机平放时，检测不到有效的角度
                if (rotation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }

                // 只检测是否有四个角度的改变
                if (rotation > 345 || rotation < 15) {
                    // 0度 ——> 竖屏
                    if (mOrientation != SCREEN_ORIENTATION_PORTRAIT) {
                        mOrientation = SCREEN_ORIENTATION_PORTRAIT;
                        onOrientationChange(mOrientation);
                    }
                } else if (rotation > 75 && rotation < 105) {
                    // 90度 ——> 反向横屏
                    if (mOrientation != SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                        mOrientation = SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                        onOrientationChange(mOrientation);
                    }
                } else if (rotation > 165 && rotation < 195) {
                    // 180度 ——> 反向竖屏
                    if (mOrientation != SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                        mOrientation = SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                        onOrientationChange(mOrientation);
                    }
                } else if (rotation > 255 && rotation < 285) {
                    // 270度 ——> 横屏
                    if (mOrientation != SCREEN_ORIENTATION_LANDSCAPE) {
                        mOrientation = SCREEN_ORIENTATION_LANDSCAPE;
                        onOrientationChange(mOrientation);
                    }
                }
            }
        };
    }

    public OnOrientationChangeListener setEnabled(boolean enabled) {
        if (enabled) {
            mListener.enable();
        } else {
            mListener.disable();
        }
        return this;
    }

    public void setOrientation(@ScreenOrientation int orientation) {
        mOrientation = orientation;
    }

    @ScreenOrientation
    public int getOrientation() {
        return mOrientation;
    }

    protected abstract void onOrientationChange(@ScreenOrientation int orientation);
}
