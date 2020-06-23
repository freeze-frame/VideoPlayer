/*
 * Created on 2020-3-10 9:41:35 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author 刘振林
 */
public abstract class TrackInfo implements Parcelable {
    @TrackType
    public final int trackType;

    @IntDef({TRACK_TYPE_VIDEO, TRACK_TYPE_AUDIO, TRACK_TYPE_SUBTITLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrackType {
    }

    public static final int TRACK_TYPE_VIDEO = 1;
    public static final int TRACK_TYPE_AUDIO = 2;
    public static final int TRACK_TYPE_SUBTITLE = 3;

    /*package*/ TrackInfo(@TrackType int trackType) {
        this.trackType = trackType;
    }

    /*package*/ TrackInfo(Parcel in) {
        trackType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(trackType);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
