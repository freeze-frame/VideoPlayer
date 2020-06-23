/*
 * Created on 2020-3-21 4:50:41 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author 刘振林
 */
public abstract class MediaTrackInfo extends TrackInfo implements Parcelable {
    public String codec;
    public int bitrate;

    /*package*/ MediaTrackInfo(@TrackType int trackType) {
        super(trackType);
    }

    /*package*/ MediaTrackInfo(@TrackType int trackType, String codec, int bitrate) {
        super(trackType);
        this.codec = codec;
        this.bitrate = bitrate;
    }

    /*package*/ MediaTrackInfo(Parcel in) {
        super(in);
        codec = in.readString();
        bitrate = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(codec);
        dest.writeInt(bitrate);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
