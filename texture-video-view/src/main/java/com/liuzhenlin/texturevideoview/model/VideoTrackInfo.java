/*
 * Created on 2020-3-10 8:29:46 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

/**
 * @author 刘振林
 */
public class VideoTrackInfo extends MediaTrackInfo implements Parcelable {
    public int width;
    public int height;
    public float frameRate;

    public VideoTrackInfo() {
        super(TRACK_TYPE_VIDEO);
    }

    public VideoTrackInfo(String codec, int width, int height, float frameRate, int bitrate) {
        super(TRACK_TYPE_VIDEO, codec, bitrate);
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
    }

    protected VideoTrackInfo(Parcel in) {
        super(in);
        width = in.readInt();
        height = in.readInt();
        frameRate = in.readFloat();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeFloat(frameRate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoTrackInfo> CREATOR = new Creator<VideoTrackInfo>() {
        @Override
        public VideoTrackInfo createFromParcel(Parcel in) {
            return new VideoTrackInfo(in);
        }

        @Override
        public VideoTrackInfo[] newArray(int size) {
            return new VideoTrackInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        VideoTrackInfo that = (VideoTrackInfo) o;
        return width == that.width &&
                height == that.height &&
                frameRate == that.frameRate &&
                bitrate == that.bitrate &&
                ObjectsCompat.equals(codec, that.codec);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(codec, width, height, frameRate, bitrate);
    }

    @NonNull
    @Override
    public String toString() {
        return "VideoTrackInfo{" +
                "codec='" + codec + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", frameRate=" + frameRate +
                ", bitrate=" + bitrate +
                '}';
    }
}
