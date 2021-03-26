/*
 * Created on 2019/4/16 10:11 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.liuzhenlin.texturevideoview.IVideoPlayer;
import com.liuzhenlin.texturevideoview.bean.TrackInfo;

import tv.danmaku.ijk.media.player.IjkMediaMeta;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

/**
 * @author 刘振林
 */
public class Utils {
    private Utils() {
    }

    /**
     * Converts a playback state constant defined for {@link IVideoPlayer.PlaybackState} to a
     * specified string
     *
     * @param playbackState one of the constant defined for {@link IVideoPlayer.PlaybackState}
     * @return the string representation of the playback state
     */
    @NonNull
    public static String playbackStateIntToString(@IVideoPlayer.PlaybackState int playbackState) {
        switch (playbackState) {
            case IVideoPlayer.PLAYBACK_STATE_ERROR:
                return "ERROR";
            case IVideoPlayer.PLAYBACK_STATE_IDLE:
                return "IDLE";
            case IVideoPlayer.PLAYBACK_STATE_PREPARING:
                return "PREPARING";
            case IVideoPlayer.PLAYBACK_STATE_PREPARED:
                return "PREPARED";
            case IVideoPlayer.PLAYBACK_STATE_PLAYING:
                return "PLAYING";
            case IVideoPlayer.PLAYBACK_STATE_PAUSED:
                return "PAUSED";
            case IVideoPlayer.PLAYBACK_STATE_COMPLETED:
                return "COMPLETED";
            default:
                throw new IllegalArgumentException("the `playbackState` must be one of the constant"
                        + " defined for IVideoPlayer.PlaybackState");
        }
    }

    /**
     * Maps a {@link TrackInfo.TrackType} to a track type constant of {@link MediaPlayer}'s,
     * as defined by various {@code MEDIA_TRACK_TYPE_*} constants in {@link MediaPlayer.TrackInfo}.
     *
     * @param trackType One of the {@code TRACK_TYPE_*} constants defined in class {@link TrackInfo}.
     * @return The mapped media track type for MediaPlayer or
     *         {@link MediaPlayer.TrackInfo#MEDIA_TRACK_TYPE_UNKNOWN}
     *         for an illegal input TrackType constant.
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    public static int getTrackTypeForMediaPlayer(@TrackInfo.TrackType int trackType) {
        switch (trackType) {
            case TrackInfo.TRACK_TYPE_VIDEO:
                return MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO;
            case TrackInfo.TRACK_TYPE_AUDIO:
                return MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO;
            case TrackInfo.TRACK_TYPE_SUBTITLE:
                return MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT;
            default:
                return MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
        }
    }

    /**
     * Maps a {@link TrackInfo.TrackType} to a track type constant of {@link IjkMediaPlayer}'s,
     * as defined by various {@code MEDIA_TRACK_TYPE_*} constants in {@link IjkTrackInfo}.
     *
     * @param trackType One of the {@code TRACK_TYPE_*} constants defined in class {@link TrackInfo}.
     * @return The mapped media track type for IjkPlayer or
     *         {@link IjkTrackInfo#MEDIA_TRACK_TYPE_UNKNOWN}
     *         for an illegal input TrackType constant.
     */
    public static int getTrackTypeForIjkPlayer(@TrackInfo.TrackType int trackType) {
        switch (trackType) {
            case TrackInfo.TRACK_TYPE_VIDEO:
                return IjkTrackInfo.MEDIA_TRACK_TYPE_VIDEO;
            case TrackInfo.TRACK_TYPE_AUDIO:
                return IjkTrackInfo.MEDIA_TRACK_TYPE_AUDIO;
            case TrackInfo.TRACK_TYPE_SUBTITLE:
                return IjkTrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT;
            default:
                return IjkTrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
        }
    }

    /**
     * Maps a {@link TrackInfo.TrackType} to a track type constant of {@link ExoPlayer}'s,
     * as defined by various {@code TRACK_TYPE_*} constants in {@link C}.
     *
     * @param trackType One of the {@code TRACK_TYPE_*} constants defined in class {@link TrackInfo}.
     * @return The mapped media track type for ExoPlayer or
     *         {@link C#TRACK_TYPE_UNKNOWN} for an illegal input TrackType constant.
     */
    public static int getTrackTypeForExoPlayer(@TrackInfo.TrackType int trackType) {
        switch (trackType) {
            case TrackInfo.TRACK_TYPE_VIDEO:
                return C.TRACK_TYPE_VIDEO;
            case TrackInfo.TRACK_TYPE_AUDIO:
                return C.TRACK_TYPE_AUDIO;
            case TrackInfo.TRACK_TYPE_SUBTITLE:
                return C.TRACK_TYPE_TEXT;
            default:
                return C.TRACK_TYPE_UNKNOWN;
        }
    }

    /**
     * Gets the short name of {@link com.google.android.exoplayer2.Format#codecs}
     */
    @Nullable
    public static String getExoTrackShortCodec(@Nullable String codecs) {
        if (TextUtils.isEmpty(codecs)) {
            return null;
        }

        //noinspection ConstantConditions
        final int endIndex = codecs.indexOf('.');
        return endIndex == -1 ? codecs : codecs.substring(0, endIndex);
    }

    /**
     * Returns the audio track channel count for the given channel layout constant starting with the
     * {@code AV_CH_LAYOUT_} prefix as defined in {@link IjkMediaMeta}, or 0 if an invalid argument
     * is passed in.
     */
    public static int getIjkAudioTrackChannelCount(long channelLayout) {
        int channelCount = 0;
        if ((channelLayout & IjkMediaMeta.AV_CH_FRONT_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_FRONT_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_FRONT_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_LOW_FREQUENCY) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_BACK_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_BACK_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_FRONT_LEFT_OF_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_FRONT_RIGHT_OF_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_BACK_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_SIDE_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_SIDE_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_FRONT_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_FRONT_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_FRONT_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_BACK_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_BACK_CENTER) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_TOP_BACK_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_STEREO_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_STEREO_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_WIDE_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_WIDE_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_SURROUND_DIRECT_LEFT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_SURROUND_DIRECT_RIGHT) != 0) {
            channelCount += 1;
        }
        if ((channelLayout & IjkMediaMeta.AV_CH_LOW_FREQUENCY_2) != 0) {
            channelCount += 1;
        }
        return channelCount;
    }

    /**
     * Returns whether the playback speed of {@link MediaPlayer} can be adjusted
     * while it is being used, depending on the the system version of the user hardware device.
     */
    public static boolean isMediaPlayerPlaybackSpeedAdjustmentSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Returns whether audio/video/subtitle track selection is supported for {@link MediaPlayer},
     * depending on the the system version of the user hardware device.
     */
    public static boolean isMediaPlayerTrackSelectionSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Returns whether the {@link ExoPlayer} can be used on the system of the user device.
     */
    public static boolean canUseExoPlayer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Returns whether the {@link rg.videolan.libvlc.MediaPlayer} can be used on
     * the system of the user device.
     */
    @SuppressWarnings("JavadocReference")
    public static boolean canUseVlcPlayer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }
}
