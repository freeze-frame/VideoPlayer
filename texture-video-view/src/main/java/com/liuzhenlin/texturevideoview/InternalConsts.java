/*
 * Created on 2018/7/1 11:22 AM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

/**
 * @author 刘振林
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class InternalConsts {

    private InternalConsts() {
    }

    /*package*/ static final boolean DEBUG = false;

    @SuppressWarnings("PointlessBooleanExpression")
    /*package*/ static final boolean DEBUG_LISTENER = DEBUG && false;

    @RestrictTo(LIBRARY)
    public static final String EXTRA_MESSENGER = "extra_messenger";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_PLAYBACK_ACTIVITY_CLASS = "extra_playbackActivityClass";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_URI = "extra_mediaUri";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_TITLE = "extra_mediaTitle";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_IS_PLAYING = "extra_isPlaying";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_IS_BUFFERING = "extra_isBuffering";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_CAN_SKIP_TO_PREVIOUS = "extra_canSkipToPrevious";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_CAN_SKIP_TO_NEXT = "extra_canSkipToNext";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_PROGRESS = "extra_mediaProgress";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_DURATION = "extra_mediaDuration";

    public static Handler getMainThreadHandler() {
        return NoPreloadHolder.MAIN_THREAD_HANDLER;
    }

    private static final class NoPreloadHolder {
        static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());
    }
}
