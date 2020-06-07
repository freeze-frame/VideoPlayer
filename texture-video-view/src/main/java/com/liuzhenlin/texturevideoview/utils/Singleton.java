/*
 * Created on 2019/12/6 3:09 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Singleton helper class for lazily initialization.
 *
 * @author 刘振林
 */
public abstract class Singleton<Params, Result> {

    private volatile Result mInstance;

    @SuppressWarnings("unchecked")
    @NonNull
    protected abstract Result onCreate(Params... params);

    @SafeVarargs
    @NonNull
    public final Result get(Params... params) {
        if (mInstance == null) {
            synchronized (this) {
                if (mInstance == null) {
                    mInstance = onCreate(params);
                }
            }
        }
        return mInstance;
    }

    @Nullable
    public final Result getNoCreate() {
        return mInstance;
    }
}
