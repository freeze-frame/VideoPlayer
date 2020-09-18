/*
 * Created on 2020-3-21 12:27:58 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */
package com.liuzhenlin.texturevideoview;

import android.content.res.Resources;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.liuzhenlin.texturevideoview.bean.AudioTrackInfo;
import com.liuzhenlin.texturevideoview.bean.MediaTrackInfo;
import com.liuzhenlin.texturevideoview.bean.SubtitleTrackInfo;
import com.liuzhenlin.texturevideoview.bean.TrackInfo;
import com.liuzhenlin.texturevideoview.bean.VideoTrackInfo;
import com.liuzhenlin.texturevideoview.utils.Utils;

import java.util.Locale;

/** A default {@link TrackNameProvider}. */
public class DefaultTrackNameProvider implements TrackNameProvider {

    private final Resources resources;

    /** @param resources {@link Resources} from which to obtain strings. */
    public DefaultTrackNameProvider(@NonNull Resources resources) {
        this.resources = Assertions.checkNotNull(resources);
    }

    @NonNull
    @Override
    public String getTrackName(@NonNull TrackInfo info) {
        String trackName;
        switch (info.trackType) {
            case TrackInfo.TRACK_TYPE_VIDEO:
                VideoTrackInfo vinfo = (VideoTrackInfo) info;
                trackName =
                        joinWithSeparator(
                                vinfo.codec,
                                buildResolutionString(vinfo),
                                buildFrameRateString(vinfo),
                                buildBitrateString(vinfo));
                break;
            case TrackInfo.TRACK_TYPE_AUDIO:
                AudioTrackInfo ainfo = (AudioTrackInfo) info;
                trackName =
                        joinWithSeparator(
                                ainfo.codec,
                                buildLanguageString(ainfo),
                                buildAudioChannelString(ainfo),
                                buildSampleRateString(ainfo),
                                buildBitrateString(ainfo));
                break;
            case TrackInfo.TRACK_TYPE_SUBTITLE:
                trackName = buildLanguageString((SubtitleTrackInfo) info);
                break;
            default:
                trackName = "";
                break;
        }
        return trackName.isEmpty() ? resources.getString(R.string.track_unknown) : trackName;
    }

    private String buildResolutionString(VideoTrackInfo info) {
        int width = info.width;
        int height = info.height;
        return width > 0 && height > 0
                ? resources.getString(R.string.track_resolution, width, height)
                : "";
    }

    private String buildFrameRateString(VideoTrackInfo info) {
        float frameRate = info.frameRate;
        return frameRate > 0
                ? resources.getString(R.string.track_frameRate,
                /* formatArgs= */ Utils.roundDecimalUpTo2FractionDigitsString(frameRate))
                : "";
    }

    private String buildBitrateString(MediaTrackInfo info) {
        float bitrate = info.bitrate;
        return bitrate > 0
                ? resources.getString(R.string.track_bitrate,
                /* formatArgs= */ Utils.roundDecimalUpTo2FractionDigitsString(bitrate / 1000000f))
                : "";
    }

    private String buildAudioChannelString(AudioTrackInfo info) {
        int channelCount = info.channelCount;
        if (channelCount < 1) {
            return "";
        }
        switch (channelCount) {
            case 1:
                return resources.getString(R.string.track_mono);
            case 2:
                return resources.getString(R.string.track_stereo);
            case 6:
            case 7:
                return resources.getString(R.string.track_surround_5_point_1);
            case 8:
                return resources.getString(R.string.track_surround_7_point_1);
            default:
                return resources.getString(R.string.track_surround);
        }
    }

    private String buildLanguageString(AudioTrackInfo info) {
        return buildLanguageString(info.language);
    }

    private String buildLanguageString(SubtitleTrackInfo info) {
        return buildLanguageString(info.language);
    }

    private String buildLanguageString(String language) {
        if (TextUtils.isEmpty(language) || C.LANGUAGE_UNDETERMINED.equals(language)) {
            return "";
        }
        Locale locale = Util.SDK_INT >= 21 ? Locale.forLanguageTag(language) : new Locale(language);
        return locale.getDisplayName();
    }

    private String buildSampleRateString(AudioTrackInfo info) {
        float sampleRate = info.sampleRate;
        return sampleRate > 0
                ? resources.getString(R.string.track_sampleRate,
                /* formatArgs= */ Utils.roundDecimalUpTo2FractionDigitsString(sampleRate / 1000f))
                : "";
    }

    private String joinWithSeparator(String... items) {
        String itemList = "";
        for (String item : items) {
            if (!TextUtils.isEmpty(item)) {
                if (TextUtils.isEmpty(itemList)) {
                    itemList = item;
                } else {
                    itemList = resources.getString(R.string.item_list, itemList, item);
                }
            }
        }
        return itemList;
    }
}
