/*
 * Created on 2019/5/6 1:11 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.liuzhenlin.texturevideoview.bean.TrackInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An interface that can be implemented by classes that wish to support a group of basic audio/video
 * playback operations exposable to external clients.
 *
 * @author 刘振林
 */
public interface IVideoPlayer {

    int INVALID_TRACK_INDEX = -1;

    /** An empty array of track information. */
    TrackInfo[] EMPTY_TRACK_INFOS = {};

    /** Special constant representing an unset or unknown time or duration in milliseconds. */
    int TIME_UNSET = -1;

    float DEFAULT_PLAYBACK_SPEED = 1.0f;

    /** A fatal player error occurred that paused the playback. */
    int PLAYBACK_STATE_ERROR = -1;

    /** The player does not have any video to play. */
    int PLAYBACK_STATE_IDLE = 0;

    /** The player is currently preparing for the video playback asynchronously. */
    int PLAYBACK_STATE_PREPARING = 1;

    /** The video is prepared to be started. */
    int PLAYBACK_STATE_PREPARED = 2;

    /** The video is currently playing. */
    int PLAYBACK_STATE_PLAYING = 3;

    /** The video is temporarily paused. */
    int PLAYBACK_STATE_PAUSED = 4;

    /** The playback of the video is ended. */
    int PLAYBACK_STATE_COMPLETED = 5;

    @IntDef({
            PLAYBACK_STATE_ERROR,
            PLAYBACK_STATE_IDLE,
            PLAYBACK_STATE_PREPARING, PLAYBACK_STATE_PREPARED,
            PLAYBACK_STATE_PLAYING, PLAYBACK_STATE_PAUSED, PLAYBACK_STATE_COMPLETED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface PlaybackState {
    }

    /**
     * A listener to monitor all state changes to the player or the playback of the video.
     */
    interface OnPlaybackStateChangeListener {
        /**
         * Called when the state of the player or the playback state of the video changes.
         *
         * @param oldState the old state of the player or the playback of the video
         * @param newState the new state of the player or the playback of the video
         * @see PlaybackState
         */
        void onPlaybackStateChange(@PlaybackState int oldState, @PlaybackState int newState);
    }

    /**
     * Monitors all events related to the video playback.
     */
    interface VideoListener {

        /** Called when the video is started or resumed. */
        default void onVideoStarted() {
        }

        /** Called when the video is paused or finished. */
        default void onVideoStopped() {
        }

        /** Called when the video played repeats from its beginning. */
        default void onVideoRepeat() {
        }

        /**
         * Called when video buffering starts or stops.
         * <p>
         * Generally, video buffering starts as the player prepares for the video to be played
         * or more data need to be loaded for the playing video
         * or a playback position seek requests.
         *
         * @param buffering true for buffering start, false otherwise.
         */
        default void onVideoBufferingStateChanged(boolean buffering) {
        }

        /**
         * Called to indicate the video duration (in milliseconds), which could be
         * {@value #TIME_UNSET} if not available (e.g., streaming live content).
         */
        default void onVideoDurationChanged(int duration) {
        }

        /**
         * Called to indicate the video size (width and height), which could be 0 if there was
         * no video set or the value was not determined yet.
         * <p>
         * This is useful for deciding whether to perform some layout changes.
         *
         * @param width  intrinsic width of the video
         * @param height intrinsic height of the video
         */
        default void onVideoSizeChanged(int width, int height) {
        }
    }

    /**
     * Sets the raw resource ID of the video to play.
     */
    void setVideoResourceId(@RawRes int resId);

    /**
     * Sets the file path of the video to play.
     */
    default void setVideoPath(@Nullable String path) {
        setVideoUri(TextUtils.isEmpty(path) ? null : Uri.parse(path));
    }

    /**
     * Sets the Uri for the video to play.
     */
    void setVideoUri(@Nullable Uri uri);

    /**
     * Initialize the player object and prepare for the video playback.
     * Normally, you should invoke this method to resume video playback instead of {@link #play(boolean)}
     * whenever the Activity's restart() or resume() method is called unless the player won't
     * be released as the Activity's lifecycle changes.
     *
     * @see #play(boolean)
     * @see #closeVideo()
     */
    void openVideo();

    /**
     * Pauses playback and releases resources associated with it.
     * Usually, whenever an Activity of an application is paused (its onPaused() method is called),
     * or stopped (its onStop() method is called), this method should be invoked to release
     * the player object, unless the application has a special need to keep the object around.
     *
     * @see #openVideo()
     */
    void closeVideo();

    /**
     * Restarts playback of the video.
     */
    void restartVideo();

    /**
     * @return the current state of the player or the playback of the video.
     */
    @PlaybackState
    int getPlaybackState();

    /**
     * Checks whether the video is playing.
     *
     * @return true if currently playing, false otherwise
     */
    default boolean isPlaying() {
        return getPlaybackState() == PLAYBACK_STATE_PLAYING;
    }

    /**
     * Starts or resumes playback.
     * If previously paused, playback will continue from where it was paused.
     * If never started before, playback will start at the beginning.
     *
     * @param fromUser whether the playback is triggered by the user
     * @see #pause(boolean)
     */
    void play(boolean fromUser);

    /**
     * Pauses playback. Call {@link #play(boolean)} to resume.
     *
     * @param fromUser whether the video is paused by the user
     * @see #play(boolean)
     */
    void pause(boolean fromUser);

    /**
     * Switches the playback state between playing and non-playing.
     */
    default void toggle(boolean fromUser) {
        if (isPlaying()) {
            pause(fromUser);
        } else {
            play(fromUser);
        }
    }

    /**
     * Skips video to the specified time position.
     *
     * @param fromUser whether the playback position change is initiated by the user
     */
    void seekTo(int positionMs, boolean fromUser);

    /**
     * Fast-forward the video.
     *
     * @param fromUser whether the video is forwarded by the user
     */
    void fastForward(boolean fromUser);

    /**
     * Fast-rewind the video.
     *
     * @param fromUser whether the video is rewound by the user
     */
    void fastRewind(boolean fromUser);

    /**
     * @return the current playback position of the video, in milliseconds.
     */
    int getVideoProgress();

    /**
     * @return an estimate of the position in the current content window up to which data is
     *         buffered, in milliseconds.
     */
    int getVideoBufferProgress();

    /**
     * Gets the duration of the video.
     *
     * @return the duration in milliseconds, if no duration is available (for example,
     *         if streaming live content or the duration is not determined yet),
     *         then {@value TIME_UNSET} is returned.
     */
    int getVideoDuration();

    /**
     * @return the width of the video, or 0 if there is no video or the width has not been
     *         determined yet.
     */
    int getVideoWidth();

    /**
     * @return the height of the video, or 0 if there is no video or the height has not been
     *         determined yet.
     */
    int getVideoHeight();

    /**
     * @return the current playback speed of the video.
     */
    float getPlaybackSpeed();

    /**
     * Sets the playback speed for the video player.
     */
    void setPlaybackSpeed(float speed);

    /**
     * @return true if the audio portion of the video source is allowed to play in the background.
     */
    boolean isAudioAllowedToPlayInBackground();

    /**
     * Sets whether the audio decoded from the video source is allowed to be played even after
     * the application switched to the background.
     */
    void setAudioAllowedToPlayInBackground(boolean allowed);

    /**
     * @return whether or not the player is looping through a single video.
     */
    boolean isSingleVideoLoopPlayback();

    /**
     * Sets the player to be looping through a single video or not.
     */
    void setSingleVideoLoopPlayback(boolean looping);

    /**
     * @return true if there is a track for the given track type
     */
    boolean hasTrack(@TrackInfo.TrackType int trackType);

    /**
     * Returns an array of track information.
     *
     * @return Array of track info. The total number of tracks is the array length.
     * @see com.liuzhenlin.texturevideoview.bean.VideoTrackInfo
     * @see com.liuzhenlin.texturevideoview.bean.AudioTrackInfo
     * @see com.liuzhenlin.texturevideoview.bean.SubtitleTrackInfo
     */
    @NonNull
    TrackInfo[] getTrackInfos();

    /**
     * Selects a track.
     *
     * @param index The index of the track to be selected. The valid range of the index is
     *              0..total number of tracks - 1. The total number of tracks as well as the type of
     *              each individual track can be found by calling {@link #getTrackInfos()} method.
     */
    void selectTrack(int index);

    /**
     * Deselects a track.
     *
     * @param index The index of the track to be deselected. The valid range of the index is
     *              0..total number of tracks - 1. The total number of tracks as well as the type of
     *              each individual track can be found by calling {@link #getTrackInfos()} method.
     */
    void deselectTrack(int index);

    /**
     * Returns the index of the video, audio, or subtitle track currently selected for playback.
     * The return value is an index into the array returned by {@link #getTrackInfos()},
     * and can be used in calls to {@link #selectTrack(int)} or {@link #deselectTrack(int)}.
     *
     * @param trackType should be one of
     *                  {@link TrackInfo#TRACK_TYPE_VIDEO},
     *                  {@link TrackInfo#TRACK_TYPE_AUDIO} or
     *                  {@link TrackInfo#TRACK_TYPE_SUBTITLE}.
     * @return index of the video, audio, or subtitle track currently selected for playback;
     *         {@value #INVALID_TRACK_INDEX} is returned when there is no selected track for {@code trackType}
     *         or when {@code trackType} is not one of video, audio, or subtitle.
     */
    int getSelectedTrackIndex(@TrackInfo.TrackType int trackType);

    /**
     * Adds an external subtitle source file (Uri),
     * e.g., a SubRip with the file extension .srt, case insensitive.
     * <p>
     * Note that you need call {@link #getTrackInfos()} again to see
     * what additional track becomes available after this method is invoked.
     *
     * @param uri      The Content URI of the data you want to play.
     * @param mimeType The mime type of the file.
     * @param language The language as an IETF BCP 47 conformant tag, or {@code null} if unknown.
     */
    void addSubtitleSource(@NonNull Uri uri, @NonNull String mimeType, @Nullable String language);

    /**
     * Adds a listener that will be informed of any events related to the video playback.
     * See {@link VideoListener}.
     *
     * <p>Component that adds a listener should take care of removing it when finished
     * via {@link #removeVideoListener(VideoListener)}.
     *
     * @param listener listener to add
     */
    void addVideoListener(@Nullable VideoListener listener);

    /**
     * Removes a listener that was previously added via {@link #addVideoListener(VideoListener)}.
     *
     * @param listener listener to remove
     */
    void removeVideoListener(@Nullable VideoListener listener);

    /**
     * Adds a {@link OnPlaybackStateChangeListener} that will be invoked whenever the state of
     * the player or the playback of the video changes.
     *
     * <p>Components that add listeners are responsible for
     * {@link #removeOnPlaybackStateChangeListener(OnPlaybackStateChangeListener) removing} them
     * when finished.
     *
     * @param listener listener to add
     */
    void addOnPlaybackStateChangeListener(@Nullable OnPlaybackStateChangeListener listener);

    /**
     * Removes the given {@link OnPlaybackStateChangeListener} that was previously added via
     * {@link #addOnPlaybackStateChangeListener(OnPlaybackStateChangeListener)}.
     *
     * @param listener listener to remove
     */
    void removeOnPlaybackStateChangeListener(@Nullable OnPlaybackStateChangeListener listener);
}
