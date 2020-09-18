/*
 * Created on 2019/11/24 4:08 PM.
 * Copyright © 2019–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.texturevideoview.bean.TrackInfo;
import com.liuzhenlin.texturevideoview.receiver.HeadsetEventsReceiver;
import com.liuzhenlin.texturevideoview.receiver.MediaButtonEventHandler;
import com.liuzhenlin.texturevideoview.receiver.MediaButtonEventReceiver;
import com.liuzhenlin.texturevideoview.utils.FileUtils;
import com.liuzhenlin.texturevideoview.utils.TimeUtil;
import com.liuzhenlin.texturevideoview.utils.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation class to be extended, for you to create an {@link IVideoPlayer} component
 * that can be used for the {@link AbsTextureVideoView} widget to play media contents.
 *
 * @author 刘振林
 */
@MainThread
public abstract class VideoPlayer implements IVideoPlayer {

    protected final Context mContext; // the Application Context

    @Nullable
    protected AbsTextureVideoView mVideoView;

    protected int mInternalFlags;

    /** Set via {@link #setAudioAllowedToPlayInBackground(boolean)} */
    private static final int $FLAG_AUDIO_ALLOWED_TO_PLAY_IN_BACKGROUND = 1;

    /** Set via {@link #setSingleVideoLoopPlayback(boolean)} */
    private static final int $FLAG_SINGLE_VIDEO_LOOP_PLAYBACK = 1 << 1;

    /**
     * Set as the video buffering starts, happening as the player prepares for the video to be played
     * or more data need to be loaded for the playing video or a playback position seek requests.
     */
    /*package*/ static final int $FLAG_VIDEO_IS_BUFFERING = 1 << 2;

    /** Indicates that the duration of the video has been determined at least once by this player. */
    protected static final int $FLAG_VIDEO_DURATION_DETERMINED = 1 << 3;

    /** Indicates that the video is manually paused by the user. */
    protected static final int $FLAG_VIDEO_PAUSED_BY_USER = 1 << 4;

    /**
     * Listener to be notified whenever it is necessary to change the video played to
     * the previous or the next one in the playlist.
     */
    @Nullable
    /*package*/ OnSkipPrevNextListener mOnSkipPrevNextListener;

    /** The set of listeners for all the events related to video we publish. */
    @Nullable
    /*package*/ List<VideoListener> mVideoListeners;

    /** Listeners monitoring all state changes to the player or the playback of the video. */
    @Nullable
    /*package*/ List<OnPlaybackStateChangeListener> mOnPlaybackStateChangeListeners;

    /** The current state of the player or the playback of the video. */
    @PlaybackState
    private int mPlaybackState = PLAYBACK_STATE_IDLE;

    /** The Uri for the video to play, set in {@link #setVideoUri(Uri)}. */
    protected Uri mVideoUri;

    protected int mVideoWidth;
    protected int mVideoHeight;

    /** How long the playback will last for. */
    protected int mVideoDuration = TIME_UNSET;

    /** The string representation of the video duration. */
    /*package*/ String mVideoDurationString = DEFAULT_STRING_VIDEO_DURATION;
    /*package*/ static final String DEFAULT_STRING_VIDEO_DURATION = "00:00";

    /**
     * Caches the speed at which the player works.
     */
    protected float mPlaybackSpeed = DEFAULT_PLAYBACK_SPEED;
    /**
     * Caches the speed the user sets for the player at any time, even when the inner player
     * has not been created.
     * <p>
     * This may fail if the value is not supported by the framework.
     */
    protected float mUserPlaybackSpeed = DEFAULT_PLAYBACK_SPEED;

    /**
     * Recording the seek position used when playback is just started.
     * <p>
     * Normally this is requested by the user (e.g., dragging the video progress bar while
     * the player is not playing) or saved when the user leaves current UI.
     */
    protected int mSeekOnPlay = TIME_UNSET;

    /** The amount of time we are stepping forward or backward for fast-forward and fast-rewind. */
    public static final int FAST_FORWARD_REWIND_INTERVAL = 15000; // ms

    /**
     * Temporarily caches the selected track indices for the played video, consisting of a combination
     * of the video, audio, and subtitle track selections.
     */
    private int mTrackSelections;

    /** Means no video, audio or subtitle track was selected for the played video. */
    private static final int TRACK_SELECTION_NONE = INVALID_TRACK_INDEX; // -1

    /**
     * When set, no specific tracks need to be selected by us when the player is prepared for
     * the new video just set, i.e., the player will pick an available one for each track type.
     */
    private static final int TRACK_SELECTION_UNSPECIFIED = -2;

    // Bit shifts to get the cached video, audio, and subtitle track selections.
    private static final int VIDEO_TRACK_SELECTION_MASK_SHIFT = 0;
    private static final int AUDIO_TRACK_SELECTION_MASK_SHIFT = 8;
    private static final int SUBTITLE_TRACK_SELECTION_MASK_SHIFT = 16;

    // Masks for use with {@link #mTrackSelections} to get the video, audio, subtitle track selections
    // previously saved for the played video.
    private static final int VIDEO_TRACK_SELECTION_MASK = 0x000000ff;
    private static final int AUDIO_TRACK_SELECTION_MASK = 0x0000ff00;
    private static final int SUBTITLE_TRACK_SELECTION_MASK = 0xffff0000;

    /**
     * Maximum cache size in bytes.
     * This is the limit on the size of all files that can be kept on disk.
     */
    protected static final long DEFAULT_MAXIMUM_CACHE_SIZE = 1024 * 1024 * 1024; // 1GB

    protected final AudioManager mAudioManager;

    /**
     * Default attributes for audio playback, which configure the underlying platform.
     * <p>
     * To get a {@link android.media.AudioAttributes} first accessible on api 21, simply call
     * the method {@link AudioAttributes#getAudioAttributesV21()} of this property.
     */
    protected static final AudioAttributes sDefaultAudioAttrs =
            new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MOVIE)
                    .build();

    protected static ComponentName sMediaButtonEventReceiverComponent;

    protected HeadsetEventsReceiver mHeadsetEventsReceiver;

    public VideoPlayer(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (sMediaButtonEventReceiverComponent == null) {
            sMediaButtonEventReceiverComponent = new ComponentName(context, MediaButtonEventReceiver.class);
        }
        if (InternalConsts.DEBUG_LISTENER) {
            final String videoPlayerTextualRepresentation =
                    getClass().getName() + "@" + Integer.toHexString(hashCode());
            addOnPlaybackStateChangeListener((oldState, newState) -> {
                final String text = videoPlayerTextualRepresentation + ": "
                        + Utils.playbackStateIntToString(oldState) + " -> "
                        + Utils.playbackStateIntToString(newState);
                if (mVideoView != null) {
                    Utils.showUserCancelableSnackbar(mVideoView, text, Snackbar.LENGTH_LONG);
                } else {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Restore the video, audio and subtitle track selections for the player when it is prepared for
     * the same video again.
     */
    protected void restoreTrackSelections() {
        final int videoTrack = getVideoTrackSelection();
        switch (videoTrack) {
            case TRACK_SELECTION_UNSPECIFIED:
                break;
            case TRACK_SELECTION_NONE:
                deselectTrack(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_VIDEO));
                break;
            default:
                final int selectedVideoTrack = getSelectedTrackIndex(TrackInfo.TRACK_TYPE_VIDEO);
                if (selectedVideoTrack != videoTrack) {
                    selectTrack(videoTrack);
                }
                break;
        }

        final int audioTrack = getAudioTrackSelection();
        switch (audioTrack) {
            case TRACK_SELECTION_UNSPECIFIED:
                break;
            case TRACK_SELECTION_NONE:
                deselectTrack(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_AUDIO));
                break;
            default:
                final int selectedAudioTrack = getSelectedTrackIndex(TrackInfo.TRACK_TYPE_AUDIO);
                if (selectedAudioTrack != audioTrack) {
                    selectTrack(audioTrack);
                }
                break;
        }

        final int subtitleTrack = getSubtitleTrackSelection();
        switch (subtitleTrack) {
            case TRACK_SELECTION_UNSPECIFIED:
                break;
            case TRACK_SELECTION_NONE:
                deselectTrack(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_SUBTITLE));
                break;
            default:
                final int selectedSubtitleTrack = getSelectedTrackIndex(TrackInfo.TRACK_TYPE_SUBTITLE);
                if (selectedSubtitleTrack != subtitleTrack) {
                    selectTrack(subtitleTrack);
                }
                break;
        }
    }

    /**
     * Save the video, audio and subtitle track selections before the player is reset or released.
     */
    @SuppressLint("SwitchIntDef")
    protected void saveTrackSelections() {
        switch (getPlaybackState()) {
            case PLAYBACK_STATE_PREPARED:
            case PLAYBACK_STATE_PLAYING:
            case PLAYBACK_STATE_PAUSED:
            case PLAYBACK_STATE_COMPLETED:
            case PLAYBACK_STATE_ERROR:
                // Update the cached track selections only when there are available tracks for
                // each track type
                if (hasTrack(TrackInfo.TRACK_TYPE_VIDEO)) {
                    setVideoTrackSelection(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_VIDEO));
                }
                if (hasTrack(TrackInfo.TRACK_TYPE_AUDIO)) {
                    setAudioTrackSelection(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_AUDIO));
                }
                if (hasTrack(TrackInfo.TRACK_TYPE_SUBTITLE)) {
                    setSubtitleTrackSelection(getSelectedTrackIndex(TrackInfo.TRACK_TYPE_SUBTITLE));
                }
                break;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void setTrackSelections(int videoTrack, int audioTrack, int subtitleTrack) {
        mTrackSelections =
                ((videoTrack << VIDEO_TRACK_SELECTION_MASK_SHIFT) & VIDEO_TRACK_SELECTION_MASK)
                        | ((audioTrack << AUDIO_TRACK_SELECTION_MASK_SHIFT) & AUDIO_TRACK_SELECTION_MASK)
                        | ((subtitleTrack << SUBTITLE_TRACK_SELECTION_MASK_SHIFT) & SUBTITLE_TRACK_SELECTION_MASK);
    }

    private void setVideoTrackSelection(int selection) {
        mTrackSelections = (mTrackSelections & ~VIDEO_TRACK_SELECTION_MASK) |
                ((selection << VIDEO_TRACK_SELECTION_MASK_SHIFT) & VIDEO_TRACK_SELECTION_MASK);
    }

    private byte getVideoTrackSelection() {
        // Casting a video track selection from int to byte preserves the sign bit
        return (byte) ((mTrackSelections & VIDEO_TRACK_SELECTION_MASK)
                >> VIDEO_TRACK_SELECTION_MASK_SHIFT);
    }

    private void setAudioTrackSelection(int selection) {
        mTrackSelections = (mTrackSelections & ~AUDIO_TRACK_SELECTION_MASK) |
                ((selection << AUDIO_TRACK_SELECTION_MASK_SHIFT) & AUDIO_TRACK_SELECTION_MASK);
    }

    private byte getAudioTrackSelection() {
        // Casting an audio track selection from int to byte preserves the sign bit
        return (byte) ((mTrackSelections & AUDIO_TRACK_SELECTION_MASK)
                >> AUDIO_TRACK_SELECTION_MASK_SHIFT);
    }

    private void setSubtitleTrackSelection(int selection) {
        mTrackSelections = (mTrackSelections & ~SUBTITLE_TRACK_SELECTION_MASK) |
                ((selection << SUBTITLE_TRACK_SELECTION_MASK_SHIFT) & SUBTITLE_TRACK_SELECTION_MASK);
    }

    private short getSubtitleTrackSelection() {
        // Casting a subtitle track selection from int to short preserves the sign bit
        return (short) ((mTrackSelections & SUBTITLE_TRACK_SELECTION_MASK)
                >> SUBTITLE_TRACK_SELECTION_MASK_SHIFT);
    }

    /**
     * @return Base directory for storing generated cache files of the video(s) that will be
     *         downloaded from HTTP server onto disk.
     */
    @NonNull
    protected final File getBaseVideoCacheDirectory() {
        return new File(FileUtils.getAppCacheDir(mContext), "videos");
    }

    /**
     * Sets the {@link AbsTextureVideoView} on which the video will be displayed.
     * <p>
     * After setting it, you probably need to call {@link TextureVideoView#setVideoPlayer(VideoPlayer)}
     * with this player object as the function argument so as to synchronize the UI state.
     */
    public void setVideoView(@Nullable AbsTextureVideoView videoView) {
        mVideoView = videoView;
    }

    @Override
    public void setVideoUri(@Nullable Uri uri) {
        if (!ObjectsCompat.equals(uri, mVideoUri)) {
            onVideoUriChanged(uri);
            onVideoSizeChanged(0, 0);
            onVideoDurationChanged(TIME_UNSET);
            mInternalFlags &= ~$FLAG_VIDEO_DURATION_DETERMINED;
            setTrackSelections(TRACK_SELECTION_UNSPECIFIED, TRACK_SELECTION_UNSPECIFIED,
                    TRACK_SELECTION_UNSPECIFIED);
            if (isInnerPlayerCreated()) {
                restartVideo(false);
            } else {
                // Removes the $FLAG_VIDEO_PAUSED_BY_USER flag and resets mSeekOnPlay to TIME_UNSET
                // in case the inner player was previously released and has not been instantiated yet.
                mInternalFlags &= ~$FLAG_VIDEO_PAUSED_BY_USER;
                mSeekOnPlay = TIME_UNSET;
                if (uri == null) {
                    // Sets the playback state to idle directly when the inner player is not created
                    // and no video is set
                    setPlaybackState(PLAYBACK_STATE_IDLE);
                } else {
                    openVideo(true);
                }
            }
        }
    }

    protected void onVideoUriChanged(@Nullable Uri uri) {
        mVideoUri = uri;
        if (mVideoView != null) {
            mVideoView.onVideoUriChanged(uri);
        }
    }

    /**
     * Similar to {@link #restartVideo()}, but preserving the track selections all depends.
     */
    protected abstract void restartVideo(boolean restoreTrackSelections);

    /**
     * @return whether or not the inner player object is created for playing the video(s)
     */
    protected abstract boolean isInnerPlayerCreated();

    /**
     * Called when the surface used as a sink for the video portion of the media changes
     *
     * @param surface the new surface for videos to be drawing onto {@link AbsTextureVideoView},
     *                maybe {@code null} indicating no surface should be used to draw them.
     */
    protected abstract void onVideoSurfaceChanged(@Nullable Surface surface);

    /** See {@link #openVideo(boolean) openVideo(false)} */
    @Override
    public final void openVideo() {
        openVideo(false);
    }

    /**
     * Initialize the player object and prepare for the video playback.
     * Normally, you should invoke this method to resume video playback instead of {@link #play(boolean)}
     * whenever the Activity's restart() or resume() method is called unless the player won't
     * be released as the Activity's lifecycle changes.
     * <p>
     * <strong>NOTE:</strong> When the window the view is attached to leaves the foreground,
     * if the video has already been paused by the user, the player will not be instantiated
     * even if you call this method when the view is displayed in front of the user again and
     * only when the user manually clicks to play, will it be initialized (see {@link #play(boolean)}),
     * but you should still call this method as usual.
     *
     * @param replayIfCompleted whether to replay the video if it is over
     * @see #closeVideo()
     * @see #play(boolean)
     */
    public final void openVideo(boolean replayIfCompleted) {
        if (replayIfCompleted || mPlaybackState != PLAYBACK_STATE_COMPLETED) {
            openVideoInternal(mVideoView == null ? null : mVideoView.getSurface());
        }
    }

    protected abstract void openVideoInternal(@Nullable Surface surface);

    @Override
    public final void closeVideo() {
        if (!isAudioAllowedToPlayInBackground()) {
            closeVideoInternal(false /* ignored */);
        }
    }

    /**
     * The same as {@link #closeVideo()}, but closes the video in spite of the playback mode
     * (video or audio-only).
     *
     * @param fromUser `true` if the video is turned off by the user.
     */
    protected abstract void closeVideoInternal(boolean fromUser);

    @Override
    public void fastForward(boolean fromUser) {
        seekTo(getVideoProgress() + FAST_FORWARD_REWIND_INTERVAL, fromUser);
    }

    @Override
    public void fastRewind(boolean fromUser) {
        seekTo(getVideoProgress() - FAST_FORWARD_REWIND_INTERVAL, fromUser);
    }

    @Override
    public int getVideoDuration() {
        return mVideoDuration;
    }

    /**
     * Gets the video duration, replacing {@value #TIME_UNSET} with 0.
     */
    /*package*/ final int getNoNegativeVideoDuration() {
        return Math.max(0, mVideoDuration);
    }

    protected final int clampedPositionMs(int positionMs) {
        return Util.constrainValue(positionMs, 0, getNoNegativeVideoDuration());
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public float getPlaybackSpeed() {
        return isPlaying() ? mPlaybackSpeed : 0;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    @CallSuper
    @Override
    public void setPlaybackSpeed(float speed) {
        if (speed != mPlaybackSpeed) {
            mPlaybackSpeed = speed;
            if (mVideoView != null) {
                mVideoView.onPlaybackSpeedChanged(speed);
            }
        }
    }

    @Override
    public final boolean isAudioAllowedToPlayInBackground() {
        return (mInternalFlags & $FLAG_AUDIO_ALLOWED_TO_PLAY_IN_BACKGROUND) != 0;
    }

    @CallSuper
    @Override
    public void setAudioAllowedToPlayInBackground(boolean allowed) {
        if (allowed != isAudioAllowedToPlayInBackground()) {
            mInternalFlags = mInternalFlags & ~$FLAG_AUDIO_ALLOWED_TO_PLAY_IN_BACKGROUND
                    | (allowed ? $FLAG_AUDIO_ALLOWED_TO_PLAY_IN_BACKGROUND : 0);
            if (mVideoView != null) {
                mVideoView.onAudioAllowedToPlayInBackgroundChanged(allowed);
            }
        }
    }

    @Override
    public final boolean isSingleVideoLoopPlayback() {
        return (mInternalFlags & $FLAG_SINGLE_VIDEO_LOOP_PLAYBACK) != 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <strong>NOTE:</strong> This does not mean that the video played can not be changed,
     * which can be switched by the user when he/she clicks the 'skip next' button or
     * chooses another video from the playlist.
     */
    @CallSuper
    @Override
    public void setSingleVideoLoopPlayback(boolean looping) {
        if (looping != isSingleVideoLoopPlayback()) {
            mInternalFlags = mInternalFlags & ~$FLAG_SINGLE_VIDEO_LOOP_PLAYBACK
                    | (looping ? $FLAG_SINGLE_VIDEO_LOOP_PLAYBACK : 0);
            if (mVideoView != null) {
                mVideoView.onSingleVideoLoopPlaybackModeChanged(looping);
            }
        }
    }

    @PlaybackState
    @Override
    public final int getPlaybackState() {
        return mPlaybackState;
    }

    protected final void setPlaybackState(@PlaybackState int newState) {
        final int oldState = mPlaybackState;
        if (newState != oldState) {
            mPlaybackState = newState;
            if (hasOnPlaybackStateChangeListener()) {
                // Since onPlaybackStateChange() is implemented by the app, it could do anything,
                // including removing itself from {@link mOnPlaybackStateChangeListeners} — and
                // that could cause problems if an iterator is used on the ArrayList.
                // To avoid such problems, just march thru the list in the reverse order.
                for (@SuppressWarnings("ConstantConditions")
                     int i = mOnPlaybackStateChangeListeners.size() - 1; i >= 0; i--) {
                    mOnPlaybackStateChangeListeners.get(i).onPlaybackStateChange(oldState, newState);
                }
            }
        }
    }

    private boolean hasOnPlaybackStateChangeListener() {
        return mOnPlaybackStateChangeListeners != null && !mOnPlaybackStateChangeListeners.isEmpty();
    }

    @Override
    public void addOnPlaybackStateChangeListener(@Nullable OnPlaybackStateChangeListener listener) {
        if (listener != null) {
            if (mOnPlaybackStateChangeListeners == null) {
                mOnPlaybackStateChangeListeners = new ArrayList<>(1);
            }
            if (!mOnPlaybackStateChangeListeners.contains(listener)) {
                mOnPlaybackStateChangeListeners.add(listener);
            }
        }
    }

    @Override
    public void removeOnPlaybackStateChangeListener(@Nullable OnPlaybackStateChangeListener listener) {
        if (listener != null && hasOnPlaybackStateChangeListener()) {
            //noinspection ConstantConditions
            mOnPlaybackStateChangeListeners.remove(listener);
        }
    }

    private boolean hasVideoListener() {
        return mVideoListeners != null && !mVideoListeners.isEmpty();
    }

    @Override
    public void addVideoListener(@Nullable VideoListener listener) {
        if (listener != null) {
            if (mVideoListeners == null) {
                mVideoListeners = new ArrayList<>(1);
            }
            if (!mVideoListeners.contains(listener)) {
                mVideoListeners.add(listener);
            }
        }
    }

    @Override
    public void removeVideoListener(@Nullable VideoListener listener) {
        if (listener != null && hasVideoListener()) {
            //noinspection ConstantConditions
            mVideoListeners.remove(listener);
        }
    }

    protected void onVideoDurationChanged(int duration) {
        if (mVideoDuration != duration) {
            mVideoDuration = duration;
            mVideoDurationString = duration == TIME_UNSET ?
                    DEFAULT_STRING_VIDEO_DURATION : TimeUtil.formatTimeByColon(duration);

            if (mVideoView != null) {
                mVideoView.onVideoDurationChanged(duration);
            }

            if (hasVideoListener()) {
                for (@SuppressWarnings("ConstantConditions")
                     int i = mVideoListeners.size() - 1; i >= 0; i--) {
                    mVideoListeners.get(i).onVideoDurationChanged(duration);
                }
            }
        }
    }

    protected void onVideoSizeChanged(int width, int height) {
        if (mVideoWidth != width || mVideoHeight != height) {
            mVideoWidth = width;
            mVideoHeight = height;

            if (mVideoView != null) {
                mVideoView.onVideoSizeChanged(width, height);
            }

            if (hasVideoListener()) {
                for (@SuppressWarnings("ConstantConditions")
                     int i = mVideoListeners.size() - 1; i >= 0; i--) {
                    mVideoListeners.get(i).onVideoSizeChanged(width, height);
                }
            }
        }
    }

    protected void onVideoStarted() {
        setPlaybackState(PLAYBACK_STATE_PLAYING);

        if (mVideoView != null) {
            mVideoView.onVideoStarted();
        }

        if (hasVideoListener()) {
            for (@SuppressWarnings("ConstantConditions")
                 int i = mVideoListeners.size() - 1; i >= 0; i--) {
                mVideoListeners.get(i).onVideoStarted();
            }
        }
    }

    protected void onVideoStopped() {
        onVideoStopped(false /* uncompleted */);
    }

    /**
     * @param canSkipToNextOnCompletion `true` if we can skip the played video to the next one in
     *                                  the playlist (if any) when the current playback ends
     */
    private void onVideoStopped(boolean canSkipToNextOnCompletion) {
        final int oldState = mPlaybackState;
        final int currentState;
        if (oldState == PLAYBACK_STATE_PLAYING) {
            setPlaybackState(PLAYBACK_STATE_PAUSED);
            currentState = PLAYBACK_STATE_PAUSED;
        } else {
            currentState = oldState;
        }

        if (mVideoView != null) {
            mVideoView.onVideoStopped();
        }

        if (hasVideoListener()) {
            for (@SuppressWarnings("ConstantConditions")
                 int i = mVideoListeners.size() - 1; i >= 0; i--) {
                mVideoListeners.get(i).onVideoStopped();
            }
        }
        if (canSkipToNextOnCompletion
                // First, checks the completed playback state here to see if it was changed in
                // the above calls to the onVideoStopped() methods of the VideoListeners.
                && currentState == PLAYBACK_STATE_COMPLETED && currentState == mPlaybackState
                // Then, checks whether or not the inner player object is released (whether the closeVideo()
                // method was called unexpectedly by the client within the same calls as above).
                && isInnerPlayerCreated()) {
            // If all of the conditions above hold, skips to the next if possible.
            skipToNextIfPossible();
        }
    }

    /**
     * @return true if the video is closed, as scheduled by the user, when playback completes
     */
    protected boolean onPlaybackCompleted() {
        setPlaybackState(PLAYBACK_STATE_COMPLETED);

        if (mVideoView != null && mVideoView.willTurnOffWhenThisEpisodeEnds()) {
            mVideoView.onVideoTurnedOffWhenTheEpisodeEnds();

            closeVideoInternal(true);
            return true;
        } else {
            onVideoStopped(true);
            return false;
        }
    }

    protected void onVideoRepeat() {
        if (mVideoView != null) {
            mVideoView.onVideoRepeat();
        }
        if (hasVideoListener()) {
            for (@SuppressWarnings("ConstantConditions")
                 int i = mVideoListeners.size() - 1; i >= 0; i--) {
                mVideoListeners.get(i).onVideoRepeat();
            }
        }
    }

    protected void onVideoBufferingStateChanged(boolean buffering) {
        //noinspection DoubleNegation
        if (((mInternalFlags & $FLAG_VIDEO_IS_BUFFERING) != 0) != buffering) {
            mInternalFlags = buffering
                    ? mInternalFlags | $FLAG_VIDEO_IS_BUFFERING
                    : mInternalFlags & ~$FLAG_VIDEO_IS_BUFFERING;

            if (mVideoView != null) {
                mVideoView.onVideoBufferingStateChanged(buffering);
            }

            if (hasVideoListener()) {
                for (@SuppressWarnings("ConstantConditions")
                     int i = mVideoListeners.size() - 1; i >= 0; i--) {
                    mVideoListeners.get(i).onVideoBufferingStateChanged(buffering);
                }
            }
        }
    }

    protected boolean skipToPreviousIfPossible() {
        if (mVideoView != null && !mVideoView.canSkipToPrevious()) {
            return false;
        }

        if (mOnSkipPrevNextListener != null) {
            mOnSkipPrevNextListener.onSkipToPrevious();
        }
        return true;
    }

    protected boolean skipToNextIfPossible() {
        if (mVideoView != null && !mVideoView.canSkipToNext()) {
            return false;
        }

        if (mOnSkipPrevNextListener != null) {
            mOnSkipPrevNextListener.onSkipToNext();
        }
        return true;
    }

    public void setOnSkipPrevNextListener(@Nullable OnSkipPrevNextListener listener) {
        mOnSkipPrevNextListener = listener;
    }

    public interface OnSkipPrevNextListener {
        /**
         * Called when the previous video in the playlist (if any) needs to be played
         */
        void onSkipToPrevious();

        /**
         * Called when the next video in the playlist (if any) needs to be played
         */
        void onSkipToNext();
    }

    protected static class MsgHandler extends Handler {
        protected final WeakReference<VideoPlayer> videoPlayerRef;

        public MsgHandler(VideoPlayer videoPlayer) {
            videoPlayerRef = new WeakReference<>(videoPlayer);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            VideoPlayer videoPlayer = videoPlayerRef.get();
            if (videoPlayer == null) return;

            AbsTextureVideoView videoView = videoPlayer.mVideoView;
            if (!(videoView != null &&
                    (videoView.isInForeground() || videoPlayer.isAudioAllowedToPlayInBackground()))) {
                return;
            }

            switch (msg.what) {
                case MediaButtonEventHandler.MSG_PLAY_PAUSE_KEY_SINGLE_TAP:
                    videoPlayer.toggle(true);
                    break;
                // Consider double tap as the next.
                case MediaButtonEventHandler.MSG_PLAY_PAUSE_KEY_DOUBLE_TAP:
                case MediaButtonEventHandler.MSG_MEDIA_NEXT:
                    videoPlayer.skipToNextIfPossible();
                    break;
                // Consider triple tap as the previous.
                case MediaButtonEventHandler.MSG_PLAY_PAUSE_KEY_TRIPLE_TAP:
                case MediaButtonEventHandler.MSG_MEDIA_PREVIOUS:
                    videoPlayer.skipToPreviousIfPossible();
                    break;
            }
        }
    }

    public static final class Factory {
        @Nullable
        public static <VP extends VideoPlayer> VP newInstance(
                @NonNull Class<VP> vpClass, @NonNull Context context) {
            if (SystemVideoPlayer.class == vpClass) {
                //noinspection unchecked
                return (VP) new SystemVideoPlayer(context);
            }

            if (ExoVideoPlayer.class == vpClass) {
                if (Utils.canUseExoPlayer()) {
                    //noinspection unchecked
                    return (VP) new ExoVideoPlayer(context);
                }
                return null;
            }

            if (IjkVideoPlayer.class == vpClass) {
                //noinspection unchecked
                return (VP) new IjkVideoPlayer(context);
            }

            if (VlcVideoPlayer.class == vpClass) {
                if (Utils.canUseVlcPlayer()) {
                    //noinspection unchecked
                    return (VP) new VlcVideoPlayer(context);
                }
                return null;
            }

            //noinspection unchecked
            for (Constructor<VP> constructor : (Constructor<VP>[]) vpClass.getConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                // Try to find a constructor that takes a single parameter whose type is
                // the (super) type of the context's.
                if (paramTypes.length == 1 && paramTypes[0].isAssignableFrom(context.getClass())) {
                    try {
                        return constructor.newInstance(context);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }
}
