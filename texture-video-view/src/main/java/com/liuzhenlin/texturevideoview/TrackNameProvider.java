/*
 * Created on 2020-3-21 12:27:58 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import androidx.annotation.NonNull;

import com.liuzhenlin.texturevideoview.bean.TrackInfo;

/** Converts {@link TrackInfo}s to user readable track names. */
public interface TrackNameProvider {

    /** Returns a user readable track name for the given {@link TrackInfo}. */
    @NonNull
    String getTrackName(@NonNull TrackInfo info);
}