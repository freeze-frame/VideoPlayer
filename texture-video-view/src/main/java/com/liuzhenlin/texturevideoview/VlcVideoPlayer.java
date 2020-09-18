/*
 * Created on 2020-3-8 12:30:20 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.bean.TrackInfo;

/**
 * Stub of VlcVideoPlayer
 *
 * @author 刘振林
 */
public class VlcVideoPlayer extends VideoPlayer {

    public VlcVideoPlayer(@NonNull Context context) {
        super(context);
    }

    @Override
    protected boolean isInnerPlayerCreated() {
        return false;
    }

    @Override
    protected void onVideoSurfaceChanged(@Nullable Surface surface) {

    }

    @Override
    protected void openVideoInternal(@Nullable Surface surface) {

    }

    @Override
    protected void closeVideoInternal(boolean fromUser) {

    }

    @Override
    public void setVideoResourceId(int resId) {

    }

    @Override
    public void restartVideo() {
        restartVideo(true);
    }

    @Override
    protected void restartVideo(boolean restoreTrackSelections) {

    }

    @Override
    public void play(boolean fromUser) {

    }

    @Override
    public void pause(boolean fromUser) {

    }

    @Override
    public void seekTo(int positionMs, boolean fromUser) {

    }

    @Override
    public int getVideoProgress() {
        return 0;
    }

    @Override
    public int getVideoBufferProgress() {
        return 0;
    }

    @Override
    public boolean hasTrack(int trackType) {
        return false;
    }

    @NonNull
    @Override
    public TrackInfo[] getTrackInfos() {
        return EMPTY_TRACK_INFOS;
    }

    @Override
    public void selectTrack(int index) {

    }

    @Override
    public void deselectTrack(int index) {

    }

    @Override
    public int getSelectedTrackIndex(int trackType) {
        return INVALID_TRACK_INDEX;
    }

    @Override
    public void addSubtitleSource(@NonNull Uri uri, @NonNull String mimeType, @Nullable String language) {

    }
}
