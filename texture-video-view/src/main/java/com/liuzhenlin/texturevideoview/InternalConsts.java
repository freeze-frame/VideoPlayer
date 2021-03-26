/*
 * Created on 2018/7/1 11:22 AM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

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
    public static final String EXTRA_MESSENGER = "messenger";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_PLAYBACK_ACTIVITY_CLASS = "playbackActivityClass";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_URI = "mediaUri";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_TITLE = "mediaTitle";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_IS_PLAYING = "isPlaying";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_IS_BUFFERING = "isBuffering";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_CAN_SKIP_TO_PREVIOUS = "canSkipToPrevious";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_CAN_SKIP_TO_NEXT = "canSkipToNext";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_PROGRESS = "mediaProgress";
    @RestrictTo(LIBRARY)
    public static final String EXTRA_MEDIA_DURATION = "mediaDuration";
}
