/*
 * Created on 2019/3/23 5:33 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.observer;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.utils.ScreenUtils;

/**
 * 观察屏幕旋转的设置是否改变，类似于注册动态广播的监听机制
 *
 * @author 刘振林
 */
public abstract class RotationObserver extends ContentObserver {

    private final Context mContext;

    public RotationObserver(@Nullable Handler handler, @NonNull Context context) {
        super(handler);
        mContext = context.getApplicationContext();
    }

    // 屏幕旋转设置改变时调用
    @Override
    public void onChange(boolean selfChange) {
        onRotationChange(selfChange, ScreenUtils.isRotationEnabled(mContext));
    }

    public void startObserver() {
        onChange(true);

        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, this);
    }

    public void stopObserver() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    protected abstract void onRotationChange(boolean selfChange, boolean enabled);
}