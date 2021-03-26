/*
 * Created on 2019/3/23 5:07 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.observer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.common.utils.DisplayCutoutUtils;

/**
 * 监听华为/小米手机的“隐藏屏幕刘海”开关
 *
 * @author 刘振林
 */
public abstract class ScreenNotchSwitchObserver extends ContentObserver {

    private final Context mContext;
    private final boolean mNotchSupportOnEMUI;
    private final boolean mNotchSupportOnMIUI;

    public ScreenNotchSwitchObserver(@Nullable Handler handler, @NonNull Context context,
                                     boolean notchSupportOnEMUI, boolean notchSupportOnMIUI) {
        super(handler);
        mContext = context.getApplicationContext();
        mNotchSupportOnEMUI = notchSupportOnEMUI;
        mNotchSupportOnMIUI = notchSupportOnMIUI;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (mNotchSupportOnEMUI) {
            onNotchChange(selfChange, DisplayCutoutUtils.isNotchHiddenForEMUI(mContext));
        } else if (mNotchSupportOnMIUI) {
            onNotchChange(selfChange, DisplayCutoutUtils.isNotchHiddenForMIUI(mContext));
        }
    }

    @SuppressLint("NewApi")
    public void startObserver() {
        onChange(true);

        Uri uri = null;
        if (mNotchSupportOnEMUI) {
            uri = Settings.Secure.getUriFor(DisplayCutoutUtils.HUAWEI_DISPLAY_NOTCH_STATUS);
        } else if (mNotchSupportOnMIUI) {
            uri = Settings.Global.getUriFor(DisplayCutoutUtils.XIAOMI_DISPLAY_NOTCH_STATUS);
        }
        if (uri != null) {
            mContext.getContentResolver().registerContentObserver(uri, false, this);
        }
    }

    public void stopObserver() {
        mContext.getContentResolver().unregisterContentObserver(this);
    }

    protected abstract void onNotchChange(boolean selfChange, boolean hidden);
}