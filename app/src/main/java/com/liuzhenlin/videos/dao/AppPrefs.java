/*
 * Created on 2018/06/26.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.dao;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.utils.Singleton;
import com.liuzhenlin.videos.Files;

/**
 * @author 刘振林
 */
public final class AppPrefs {

    private final SharedPreferences mSP;

    private static final String DRAWER_BACKGROUND_PATH = "drawerBackgroundPath";
    private static final String IS_LIGHT_DRAWER_STATUS = "isLightDrawerStatus";
    private static final String IS_LIGHT_DRAWER_LIST_FOREGROUND = "isLightDrawerListForeground";

    private static final Singleton<Context, AppPrefs> sAppPrefsSingleton =
            new Singleton<Context, AppPrefs>() {
                @SuppressLint("SyntheticAccessor")
                @NonNull
                @Override
                protected AppPrefs onCreate(Context... ctxs) {
                    return new AppPrefs(ctxs[0]);
                }
            };

    public static AppPrefs getSingleton(@NonNull Context context) {
        return sAppPrefsSingleton.get(context);
    }

    private AppPrefs(Context context) {
        context = context.getApplicationContext();
        mSP = context.getSharedPreferences(Files.SHARED_PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    public String getDrawerBackgroundPath() {
        return mSP.getString(DRAWER_BACKGROUND_PATH, null);
    }

    public void setDrawerBackgroundPath(@Nullable String path) {
        mSP.edit().putString(DRAWER_BACKGROUND_PATH, path).apply();
    }

    public boolean isLightDrawerStatus() {
        return mSP.getBoolean(IS_LIGHT_DRAWER_STATUS, true);
    }

    public void setLightDrawerStatus(boolean light) {
        mSP.edit().putBoolean(IS_LIGHT_DRAWER_STATUS, light).apply();
    }

    public boolean isLightDrawerListForeground() {
        return mSP.getBoolean(IS_LIGHT_DRAWER_LIST_FOREGROUND, false);
    }

    public void setLightDrawerListForeground(boolean light) {
        mSP.edit().putBoolean(IS_LIGHT_DRAWER_LIST_FOREGROUND, light).apply();
    }
}
