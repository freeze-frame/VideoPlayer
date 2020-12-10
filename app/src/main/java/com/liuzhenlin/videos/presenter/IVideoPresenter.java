/*
 * Created on 2020-12-10 10:06:27 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.TextureVideoView;
import com.liuzhenlin.videos.view.activity.IVideoView;

import java.io.File;

/**
 * @author 刘振林
 */
public interface IVideoPresenter extends IPresenter<IVideoView> {

    int PLAYLIST_ADAPTER_PAYLOAD_VIDEO_PROGRESS_CHANGED = 1;
    int PLAYLIST_ADAPTER_PAYLOAD_HIGHLIGHT_ITEM_IF_SELECTED = 1 << 1;

    boolean initPlaylist(@Nullable Bundle savedInstanceState, @NonNull Intent intent);
    boolean initPlaylistAndRecordCurrentVideoProgress(@Nullable Bundle savedInstanceState, @NonNull Intent intent);
    void saveData(@NonNull Bundle outState);

    void playCurrentVideo();
    int getCurrentVideoPositionInList();
    int getPlaylistSize();

    void recordCurrVideoProgress();
    void recordCurrVideoProgressAndSetResult();

    void skipToPreviousVideo();
    void skipToNextVideo();

    void onCurrentVideoStarted();

    void shareCurrentVideo(@NonNull Context context);
    void shareCapturedVideoPhoto(@NonNull Context context, @NonNull File photo);

    @NonNull
    TextureVideoView.PlayListAdapter<? extends IVideoView.PlaylistViewHolder> newPlaylistAdapter();

    @NonNull
    static IVideoPresenter newInstance() {
        return new VideoPresenter();
    }
}
