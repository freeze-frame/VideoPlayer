/*
 * Created on 2020-3-10 10:18:49 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.bean;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

/**
 * @author 刘振林
 */
public class SubtitleTrackInfo extends TrackInfo implements Parcelable {
    public String language;

    public SubtitleTrackInfo() {
        super(TRACK_TYPE_SUBTITLE);
    }

    public SubtitleTrackInfo(String language) {
        super(TRACK_TYPE_SUBTITLE);
        this.language = language;
    }

    protected SubtitleTrackInfo(Parcel in) {
        super(in);
        language = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(language);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SubtitleTrackInfo> CREATOR = new Creator<SubtitleTrackInfo>() {
        @Override
        public SubtitleTrackInfo createFromParcel(Parcel in) {
            return new SubtitleTrackInfo(in);
        }

        @Override
        public SubtitleTrackInfo[] newArray(int size) {
            return new SubtitleTrackInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SubtitleTrackInfo that = (SubtitleTrackInfo) o;
        return ObjectsCompat.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(language);
    }

    @NonNull
    @Override
    public String toString() {
        return "SubtitleTrackInfo{" +
                "language='" + language + '\'' +
                '}';
    }
}
