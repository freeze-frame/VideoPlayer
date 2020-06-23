/*
 * Created on 2019/4/16 10:11 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.texturevideoview.IVideoPlayer;
import com.liuzhenlin.texturevideoview.R;
import com.liuzhenlin.texturevideoview.bean.TrackInfo;

import java.math.RoundingMode;
import java.text.NumberFormat;

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
     * Judges if two floating-point numbers (float) are equal, ignoring very small precision errors.
     */
    public static boolean areEqualIgnorePrecisionError(float value1, float value2) {
        return Math.abs(value1 - value2) < 0.0001f;
    }

    /**
     * Judges if two floating-point numbers (double) are equal, ignoring very small precision errors.
     */
    public static boolean areEqualIgnorePrecisionError(double value1, double value2) {
        return Math.abs(value1 - value2) < 0.0001d;
    }

    /**
     * Returns the string representation of a floating point number rounded up to 2 fraction digits.
     */
    public static String roundDecimalUpTo2FractionDigitsString(double value) {
        return roundDecimalToString(value, 2);
    }

    /**
     * @see #roundDecimalToString(double, int, int, boolean)
     */
    public static String roundDecimalToString(double value, int maxFractionDigits) {
        return roundDecimalToString(value, 0, maxFractionDigits);
    }

    /**
     * @see #roundDecimalToString(double, int, int, boolean)
     */
    public static String roundDecimalToString(double value, int minFractionDigits, int maxFractionDigits) {
        return roundDecimalToString(value, minFractionDigits, maxFractionDigits, false);
    }

    /**
     * Rounds a floating point number up to {@code maxFractionDigits} fraction digits and at least
     * {@code minFractionDigits} digits, then returns it as a string.
     *
     * @param value             the decimal to be rounded half up
     * @param minFractionDigits see the parameter of {@link NumberFormat#setMinimumFractionDigits(int)}
     * @param maxFractionDigits see the parameter of {@link NumberFormat#setMaximumFractionDigits(int)}
     * @param groupingUsed      see the parameter of {@link NumberFormat#setGroupingUsed(boolean)}
     * @return the equivalent string representation of the rounded decimal.
     */
    public static String roundDecimalToString(double value,
                                              int minFractionDigits, int maxFractionDigits,
                                              boolean groupingUsed) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(groupingUsed);
        nf.setMinimumFractionDigits(minFractionDigits);
        nf.setMaximumFractionDigits(maxFractionDigits);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return nf.format(value);
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
    public static boolean canUseVlcPlayer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * Creates a new MotionEvent with {@link MotionEvent#ACTION_CANCEL} action being performed,
     * filling in a subset of the basic motion values. Those not specified here are:
     * <ul>
     * <li>down time (current milliseconds since boot)</li>
     * <li>event time (current milliseconds since boot)</li>
     * <li>x and y coordinates of this event (always 0)</li>
     * <li>
     * The state of any meta/modifier keys that were in effect when the event was generated (always 0)
     * </li>
     * </ul>
     */
    @NonNull
    public static MotionEvent obtainCancelEvent() {
        final long now = SystemClock.uptimeMillis();
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
    }

    /**
     * Walks up the hierarchy for the given `view` to determine if it is inside a scrolling container.
     */
    public static boolean isInScrollingContainer(@NonNull View view) {
        ViewParent p = view.getParent();
        while (p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /**
     * Indicates whether or not the view's layout direction is right-to-left.
     * This is resolved from layout attribute and/or the inherited value from its parent
     *
     * @return true if the layout direction is right-to-left
     */
    public static boolean isLayoutRtl(@NonNull View view) {
        return ViewCompat.getLayoutDirection(view) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Converts script specific gravity to absolute horizontal values,
     * leaving the vertical values unchanged.
     * <p>
     * if horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
     * if horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
     *
     * @param parent  The parent view where to get the layout direction.
     * @param gravity The gravity to convert to absolute values.
     * @return gravity converted to absolute horizontal & original vertical values.
     */
    public static int getAbsoluteGravity(@NonNull View parent, int gravity) {
        final int layoutDirection = ViewCompat.getLayoutDirection(parent);
        return GravityCompat.getAbsoluteGravity(gravity, layoutDirection);
    }

    /**
     * Converts script specific gravity to absolute horizontal values.
     * <p>
     * if horizontal direction is LTR, then START will set LEFT and END will set RIGHT.
     * if horizontal direction is RTL, then START will set RIGHT and END will set LEFT.
     *
     * @param parent  The parent view where to get the layout direction.
     * @param gravity The gravity to convert to absolute horizontal values.
     * @return gravity converted to absolute horizontal values.
     */
    public static int getAbsoluteHorizontalGravity(@NonNull View parent, int gravity) {
        return getAbsoluteGravity(parent, gravity) & Gravity.HORIZONTAL_GRAVITY_MASK;
    }

    /**
     * Includes a set of children of the given `parent` ViewGroup (not necessary to be the root of
     * the transition) for the given Transition object to skip the others while it is running on a
     * view hierarchy.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static void includeChildrenForTransition(
            @NonNull Transition transition, @NonNull ViewGroup parent, @Nullable View... children) {
        outsider:
        for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (children != null) {
                for (View child2 : children) {
                    if (child2 == child) continue outsider;
                }
            }
            transition.excludeTarget(child, true);
        }
    }

    public static void showUserCancelableSnackbar(@NonNull View view, @StringRes int resId,
                                                  @Snackbar.Duration int duration) {
        showUserCancelableSnackbar(view, resId, false, duration);
    }

    public static void showUserCancelableSnackbar(@NonNull View view, @StringRes int resId,
                                                  boolean shownTextSelectable,
                                                  @Snackbar.Duration int duration) {
        showUserCancelableSnackbar(view, view.getResources().getText(resId), shownTextSelectable, duration);
    }

    public static void showUserCancelableSnackbar(@NonNull View view, @NonNull CharSequence text,
                                                  @Snackbar.Duration int duration) {
        showUserCancelableSnackbar(view, text, false, duration);
    }

    public static void showUserCancelableSnackbar(@NonNull View view, @NonNull CharSequence text,
                                                  boolean shownTextSelectable,
                                                  @Snackbar.Duration int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);

        TextView snackbarText = snackbar.getView().findViewById(R.id.snackbar_text);
        snackbarText.setMaxLines(Integer.MAX_VALUE);
        snackbarText.setTextIsSelectable(shownTextSelectable);

        snackbar.setAction(R.string.undo, v -> snackbar.dismiss());
        snackbar.show();
    }
}
