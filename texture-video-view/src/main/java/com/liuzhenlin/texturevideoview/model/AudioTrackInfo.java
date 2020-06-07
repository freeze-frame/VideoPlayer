/*
 * Created on 2020-3-10 8:30:07 PM.
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
public class AudioTrackInfo extends MediaTrackInfo implements Parcelable {
    public String language;
    public int channelCount;
    public int sampleRate;

    public AudioTrackInfo() {
        super(TRACK_TYPE_AUDIO);
    }

    public AudioTrackInfo(String codec, String language, int channelCount, int sampleRate, int bitrate) {
        super(TRACK_TYPE_AUDIO, codec, bitrate);
        this.language = language;
        this.channelCount = channelCount;
        this.sampleRate = sampleRate;
    }

    protected AudioTrackInfo(Parcel in) {
        super(in);
        language = in.readString();
        channelCount = in.readInt();
        sampleRate = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(language);
        dest.writeInt(channelCount);
        dest.writeInt(sampleRate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AudioTrackInfo> CREATOR = new Creator<AudioTrackInfo>() {
        @Override
        public AudioTrackInfo createFromParcel(Parcel in) {
            return new AudioTrackInfo(in);
        }

        @Override
        public AudioTrackInfo[] newArray(int size) {
            return new AudioTrackInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AudioTrackInfo that = (AudioTrackInfo) o;
        return channelCount == that.channelCount &&
                sampleRate == that.sampleRate &&
                bitrate == that.bitrate &&
                ObjectsCompat.equals(codec, that.codec) &&
                ObjectsCompat.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(codec, language, channelCount, sampleRate, bitrate);
    }

    @NonNull
    @Override
    public String toString() {
        return "AudioTrackInfo{" +
                "codec='" + codec + '\'' +
                ", language='" + language + '\'' +
                ", channelCount=" + channelCount +
                ", sampleRate=" + sampleRate +
                ", bitrate=" + bitrate +
                '}';
    }
}
