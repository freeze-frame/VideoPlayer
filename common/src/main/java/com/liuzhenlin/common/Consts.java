/*
 * Created on 2021-3-25 8:37:16 PM.
 * Copyright © 2021 刘振林. All rights reserved.
 */

package com.liuzhenlin.common;

import android.os.Handler;
import android.os.Looper;

public class Consts {
    private Consts() {
    }

    public static final long NO_ID = -1L;
    public static final String EMPTY_STRING = "";
    public static final String[] EMPTY_STRING_ARRAY = {};

    public static Handler getMainThreadHandler() {
        return NoPreloadHolder.MAIN_THREAD_HANDLER;
    }

    private static final class NoPreloadHolder {
        static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());
    }
}
