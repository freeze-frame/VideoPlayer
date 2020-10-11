/*
 * Created on 2019/5/27 8:55 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.io.IOException;

/**
 * @author 刘振林
 */
/*package*/ final class VideoClipPlayer implements IVideoClipPlayer {

    private final IVideoClipPlayer IMPL;

    public VideoClipPlayer(@NonNull Context context, @NonNull SurfaceHolder surfaceHolder,
                           @NonNull Uri videoUri, @NonNull String userAgent,
                           @Nullable MediaSourceFactory mediaSourceFactory) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            IMPL = new VideoClipPlayerApi17Impl(context, surfaceHolder, videoUri);
        } else*/ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            IMPL = new VideoClipPlayerApi16Impl(
                    context, surfaceHolder, videoUri, userAgent, mediaSourceFactory);
        } else {
            IMPL = new VideoClipPlayerBaseImpl(context, surfaceHolder, videoUri);
        }
    }

    @Override
    public boolean isPlaying() {
        return IMPL.isPlaying();
    }

    @Override
    public void play() {
        IMPL.play();
    }

    @Override
    public void pause() {
        IMPL.pause();
    }

    @Override
    public void seekTo(int positionMs) {
        IMPL.seekTo(positionMs);
    }

    @Override
    public int getCurrentPosition() {
        return IMPL.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return IMPL.getDuration();
    }

    @Override
    public void create() {
        IMPL.create();
    }

    @Override
    public void release() {
        IMPL.release();
    }
/*
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static final class VideoClipPlayerApi17Impl implements IVideoClipPlayer {
        final Context mContext;
        final SurfaceHolder mSurfaceHolder;
        final Uri mVideoUri;

        LibVLC mLibVLC;
        org.videolan.libvlc.MediaPlayer mVlcPlayer;
        int mSeekOnPlay;
        boolean mPlaying;

        VideoClipPlayerApi17Impl(@NonNull Context context, @NonNull SurfaceHolder surfaceHolder,
                                 @NonNull Uri videoUri) {
            mContext = context;
            mSurfaceHolder = surfaceHolder;
            mVideoUri = videoUri;

            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    if (mVlcPlayer != null) {
                        mVlcPlayer.getVLCVout().setWindowSize(width, height);
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }
            });
        }

        @Override
        public boolean isPlaying() {
            return mPlaying;
        }

        @Override
        public void play() {
            if (mVlcPlayer != null) {
                mVlcPlayer.play();
                if (mSeekOnPlay != 0) {
                    mVlcPlayer.setTime(mSeekOnPlay);
                    mSeekOnPlay = 0;
                }
                mPlaying = true;
            }
        }

        @Override
        public void pause() {
            if (mVlcPlayer != null) {
                mVlcPlayer.pause();
                mPlaying = false;
            }
        }

        @Override
        public void seekTo(int positionMs) {
            if (mVlcPlayer != null) {
                mVlcPlayer.setTime(positionMs);
            }
        }

        @Override
        public int getCurrentPosition() {
            if (mVlcPlayer != null) {
                return (int) mVlcPlayer.getTime();
            }
            return mSeekOnPlay;
        }

        @Override
        public int getDuration() {
            if (mVlcPlayer != null) {
                final long duration = mVlcPlayer.getLength();
                return duration == 0L ? -1 : (int) duration;
            }
            return -1;
        }

        @Override
        public void create() {
            if (mVlcPlayer == null) {
                final ArrayList<String> options = new ArrayList<>(1);
                options.add("-vvv");
                mLibVLC = new LibVLC(mContext, options);

                final Rect surfaceFrame = mSurfaceHolder.getSurfaceFrame();
                final int surfaceWidth = surfaceFrame.width();
                final int surfaceHeight = surfaceFrame.height();

                mVlcPlayer = new org.videolan.libvlc.MediaPlayer(mLibVLC);
                final IVLCVout vout = mVlcPlayer.getVLCVout();

                mVlcPlayer.setAspectRatio(null);
                mVlcPlayer.setScale(0);
                mVlcPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_BEST_FIT);
                vout.setWindowSize(surfaceWidth, surfaceHeight);
                vout.setVideoSurface(mSurfaceHolder.getSurface(), mSurfaceHolder);
                vout.attachViews();

                final String url = mVideoUri.toString();
                final Media media;
                if (URLUtils.isNetworkUrl(url)) {
                    media = new Media(mLibVLC, mVideoUri);
                } else {
                    media = new Media(mLibVLC, url);
                }
                mVlcPlayer.setMedia(media);
                media.release();
            }
        }

        @Override
        public void release() {
            if (mVlcPlayer != null) {
                mSeekOnPlay = (int) mVlcPlayer.getTime();
                mVlcPlayer.getVLCVout().detachViews();
                mVlcPlayer.stop();
                mVlcPlayer.release();
                mVlcPlayer = null;
                mLibVLC.release();
                mLibVLC = null;
                mPlaying = false;
            }
        }
    }
*/
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private static final class VideoClipPlayerApi16Impl implements IVideoClipPlayer {
        final Context mContext;
        final SurfaceHolder mSurfaceHolder;
        final MediaSource mMediaSource;

        SimpleExoPlayer mExoPlayer;
        int mSeekOnPlay;

        VideoClipPlayerApi16Impl(@NonNull Context context, @NonNull SurfaceHolder surfaceHolder,
                                 @NonNull Uri videoUri, @NonNull String userAgent,
                                 @Nullable MediaSourceFactory mediaSourceFactory) {
            mContext = context.getApplicationContext();
            mSurfaceHolder = surfaceHolder;
            if (mediaSourceFactory == null) {
                mediaSourceFactory = new ProgressiveMediaSource.Factory(
                        new DefaultDataSourceFactory(context, userAgent));
            }
            mMediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUri));
        }

        @Override
        public boolean isPlaying() {
            return mExoPlayer != null && mExoPlayer.getPlayWhenReady();
        }

        @Override
        public void play() {
            if (mExoPlayer != null) {
                mExoPlayer.setPlayWhenReady(true);
                if (mSeekOnPlay != 0) {
                    mExoPlayer.seekTo(mSeekOnPlay);
                    mSeekOnPlay = 0;
                }
            }
        }

        @Override
        public void pause() {
            if (mExoPlayer != null) {
                mExoPlayer.setPlayWhenReady(false);
            }
        }

        @Override
        public void seekTo(int positionMs) {
            if (mExoPlayer != null) {
                mExoPlayer.seekTo(positionMs);
            }
        }

        @Override
        public int getCurrentPosition() {
            if (mExoPlayer != null) {
                return (int) mExoPlayer.getCurrentPosition();
            }
            return mSeekOnPlay;
        }

        @Override
        public int getDuration() {
            if (mExoPlayer != null) {
                final long duration = mExoPlayer.getDuration();
                return duration == C.TIME_UNSET ? -1 : (int) duration;
            }
            return -1;
        }

        @Override
        public void create() {
            if (mExoPlayer == null) {
                mExoPlayer = new SimpleExoPlayer.Builder(mContext).build();
                mExoPlayer.setVideoSurfaceHolder(mSurfaceHolder);
                mExoPlayer.setAudioAttributes(VideoPlayer.sDefaultAudioAttrs, true);
                mExoPlayer.setMediaSource(mMediaSource);
                mExoPlayer.prepare();
            }
        }

        @Override
        public void release() {
            if (mExoPlayer != null) {
                mSeekOnPlay = (int) mExoPlayer.getCurrentPosition();
                mExoPlayer.stop();
                mExoPlayer.release();
                mExoPlayer = null;
            }
        }
    }

    private static final class VideoClipPlayerBaseImpl implements IVideoClipPlayer {
        final Context mContext;
        final SurfaceHolder mSurfaceHolder;
        final Uri mVideoUri;

        android.media.MediaPlayer mMediaPlayer;
        int mSeekOnPlay;
        boolean mPrepared;
        boolean mPlayWhenPrepared;
        boolean mPlaying;

        VideoClipPlayerBaseImpl(@NonNull Context context, @NonNull SurfaceHolder surfaceHolder,
                                @NonNull Uri videoUri) {
            mContext = context;
            mSurfaceHolder = surfaceHolder;
            mVideoUri = videoUri;
        }

        @Override
        public boolean isPlaying() {
            return mPlaying;
        }

        @Override
        public void play() {
            if (mMediaPlayer != null) {
                if (mPrepared) {
                    mMediaPlayer.start();
                    if (mSeekOnPlay != 0) {
                        seekTo(mSeekOnPlay);
                        mSeekOnPlay = 0;
                    }
                } else {
                    mPlayWhenPrepared = true;
                }
                mPlaying = true;
            }
        }

        @Override
        public void pause() {
            if (mMediaPlayer != null) {
                if (mPrepared) {
                    mMediaPlayer.pause();
                } else {
                    mPlayWhenPrepared = false;
                }
                mPlaying = false;
            }
        }

        @Override
        public void seekTo(int positionMs) {
            if (mMediaPlayer != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Precise seek with larger performance overhead compared to the default one.
                    // Slow! Really slow!
                    mMediaPlayer.seekTo(positionMs, android.media.MediaPlayer.SEEK_CLOSEST);
                } else {
                    mMediaPlayer.seekTo(positionMs /*, android.media.MediaPlayer.SEEK_PREVIOUS_SYNC*/);
                }
            }
        }

        @Override
        public int getCurrentPosition() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getCurrentPosition();
            }
            return mSeekOnPlay;
        }

        @Override
        public int getDuration() {
            if (mMediaPlayer != null) {
                return mMediaPlayer.getDuration();
            }
            return -1;
        }

        @Override
        public void create() {
            if (mMediaPlayer == null) {
                mMediaPlayer = new android.media.MediaPlayer();
                try {
                    mMediaPlayer.setDataSource(mContext, mVideoUri);
                } catch (IOException e) {
                    e.printStackTrace();
                    mMediaPlayer = null;
                    return;
                }
                mMediaPlayer.setDisplay(mSurfaceHolder);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mMediaPlayer.setAudioAttributes(VideoPlayer.sDefaultAudioAttrs.getAudioAttributesV21());
                } else {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
                mMediaPlayer.setOnPreparedListener(mp -> {
                    mPrepared = true;
                    if (mPlayWhenPrepared) {
                        play();
                    }
                });
                mMediaPlayer.prepareAsync();
            }
        }

        @Override
        public void release() {
            if (mMediaPlayer != null) {
                mSeekOnPlay = mMediaPlayer.getCurrentPosition();
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                mPrepared = false;
                mPlayWhenPrepared = false;
                mPlaying = false;
            }
        }
    }
}
