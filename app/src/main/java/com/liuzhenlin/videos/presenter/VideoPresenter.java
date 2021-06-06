/*
 * Created on 2020-12-10 10:08:15 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.util.Synthetic;
import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.common.utils.FileUtils;
import com.liuzhenlin.common.utils.ShareUtils;
import com.liuzhenlin.texturevideoview.TextureVideoView;
import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.Files;
import com.liuzhenlin.videos.R;
import com.liuzhenlin.videos.bean.Video;
import com.liuzhenlin.videos.dao.VideoListItemDao;
import com.liuzhenlin.videos.utils.VideoUtils2;
import com.liuzhenlin.videos.view.activity.IVideoView;
import com.liuzhenlin.videos.view.fragment.VideoListItemOpsKt;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static com.liuzhenlin.common.Consts.NO_ID;

/**
 * @author 刘振林
 */
class VideoPresenter extends Presenter<IVideoView> implements IVideoPresenter {

    private static final String KEY_VIDEO_INDEX = "kvi";

    @Synthetic Video[] mVideos;
    @Synthetic int mVideoIndex = -1;

    @Override
    public boolean initPlaylistAndRecordCurrentVideoProgress(
            @Nullable Bundle savedInstanceState, @NonNull Intent intent) {
        Video video = mVideos == null ? null : mVideos[mVideoIndex];
        boolean initialized = initPlaylist(savedInstanceState, intent);
        if (video != null) {
            recordVideoProgress(video);
        }
        return initialized;
    }

    @Override
    public boolean initPlaylist(@Nullable Bundle savedInstanceState, @NonNull Intent intent) {
        final boolean stateRestore = savedInstanceState != null;
        Video video;

        Parcelable[] parcelables = intent.getParcelableArrayExtra(Consts.KEY_VIDEOS);
        if (parcelables != null) {
            final int length = parcelables.length;
            if (length > 0) {
                mVideos = new Video[length];
                for (int i = 0; i < length; i++) {
                    video = (Video) parcelables[i];
                    if (stateRestore) {
                        video.setProgress(
                                VideoListItemDao.getSingleton(mContext).getVideoProgress(video.getId()));
                    }
                    mVideos[i] = video;
                }
                if (stateRestore) {
                    mVideoIndex = savedInstanceState.getInt(KEY_VIDEO_INDEX);
                } else {
                    mVideoIndex = intent.getIntExtra(Consts.KEY_SELECTION, 0);
                    if (mVideoIndex < 0 || mVideoIndex >= length) {
                        mVideoIndex = 0;
                    }
                }
                return true;
            }
            return false;
        }

        video = intent.getParcelableExtra(Consts.KEY_VIDEO);
        if (video != null) {
            if (stateRestore) {
                video.setProgress(
                        VideoListItemDao.getSingleton(mContext).getVideoProgress(video.getId()));
            }
            mVideos = new Video[]{video};
            mVideoIndex = 0;
            return true;
        }

        Parcelable[] videoUriParcels = (Parcelable[]) intent.getSerializableExtra(Consts.KEY_VIDEO_URIS);
        Serializable[] videoTitleSerials = (Serializable[]) intent.getSerializableExtra(Consts.KEY_VIDEO_TITLES);
        if (videoUriParcels != null) {
            final int length = videoUriParcels.length;
            if (length > 0) {
                mVideos = new Video[length];
                for (int i = 0; i < length; i++) {
                    video = buildVideoForUri((Uri) videoUriParcels[i],
                            (String) (videoTitleSerials != null ? videoTitleSerials[i] : null));
                    if (stateRestore && video.getId() != NO_ID) {
                        video.setProgress(
                                VideoListItemDao.getSingleton(mContext).getVideoProgress(video.getId()));
                    }
                    mVideos[i] = video;
                }
                if (stateRestore) {
                    mVideoIndex = savedInstanceState.getInt(KEY_VIDEO_INDEX);
                } else {
                    mVideoIndex = intent.getIntExtra(Consts.KEY_SELECTION, 0);
                    if (mVideoIndex < 0 || mVideoIndex >= length) {
                        mVideoIndex = 0;
                    }
                }
                return true;
            }
            return false;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null) {
                CharSequence uriCharSequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (uriCharSequence != null) {
                    uri = Uri.parse(uriCharSequence.toString());
                }
            }
        }
        if (uri != null) {
            video = buildVideoForUri(uri, intent.getStringExtra(Consts.KEY_VIDEO_TITLE));
            if (stateRestore && video.getId() != NO_ID) {
                video.setProgress(
                        VideoListItemDao.getSingleton(mContext).getVideoProgress(video.getId()));
            }
            mVideos = new Video[]{video};
            mVideoIndex = 0;
            return true;
        }

        return false;
    }

    private Video buildVideoForUri(Uri uri, String videoTitle) {
        String videoUrl = FileUtils.UriResolver.getPath(mContext, uri);
        if (videoUrl == null) {
            videoUrl = uri.toString();
        }

        Video video = VideoListItemDao.getSingleton(mContext).queryVideoByPath(videoUrl);
        if (video == null) {
            video = new Video();
            video.setId(NO_ID);
            video.setPath(videoUrl);
            if (videoTitle != null) {
                video.setName(videoTitle);
            } else {
                video.setName(FileUtils.getFileNameFromFilePath(videoUrl));
            }
        }
        return video;
    }

    @Override
    public void saveData(@NonNull Bundle outState) {
        outState.putInt(KEY_VIDEO_INDEX, mVideoIndex);
    }

    @Override
    public void playCurrentVideo() {
        if (mView != null && mVideos != null) {
            mView.setVideoToPlay(mVideos[mVideoIndex]);
        }
    }

    @Override
    public int getCurrentVideoPositionInList() {
        return mVideoIndex;
    }

    @Override
    public int getPlaylistSize() {
        return mVideos == null ? 0 : mVideos.length;
    }

    @Override
    public void recordCurrVideoProgress() {
        if (mVideos != null) {
            recordVideoProgress(mVideos[mVideoIndex]);
        }
    }

    private void recordVideoProgress(Video video) {
        if (mView != null) {
            video.setProgress(mView.getPlayingVideoProgress());

            final long id = video.getId();
            if (id != NO_ID) {
                VideoListItemDao.getSingleton(mContext).setVideoProgress(id, video.getProgress());
            }
        }
    }

    @Override
    public void recordCurrVideoProgressAndSetResult() {
        if (mVideos != null) {
            recordCurrVideoProgress();
            if (mView != null) {
                if (mVideos.length == 1) {
                    mView.setResult(Consts.RESULT_CODE_PLAY_VIDEO,
                            new Intent().putExtra(Consts.KEY_VIDEO, mVideos[0]));
                } else {
                    mView.setResult(Consts.RESULT_CODE_PLAY_VIDEOS,
                            new Intent().putExtra(Consts.KEY_VIDEOS, mVideos));
                }
            }
        }
    }

    @Override
    public void skipToPreviousVideo() {
        recordCurrVideoProgress();
        if (mVideos != null) {
            final int oldVideoIndex = mVideoIndex;
            if (oldVideoIndex == 0) {
                mVideoIndex = mVideos.length - 1;
            } else {
                --mVideoIndex;
            }
            if (mView != null) {
                mView.setVideoToPlay(mVideos[mVideoIndex]);
                mView.notifyPlaylistSelectionChanged(oldVideoIndex, mVideoIndex, true);
            }
        }
    }

    @Override
    public void skipToNextVideo() {
        recordCurrVideoProgress();
        if (mVideos != null) {
            final int oldVideoIndex = mVideoIndex;
            if (oldVideoIndex == mVideos.length - 1) {
                mVideoIndex = 0;
            } else {
                ++mVideoIndex;
            }
            if (mView != null) {
                mView.setVideoToPlay(mVideos[mVideoIndex]);
                mView.notifyPlaylistSelectionChanged(oldVideoIndex, mVideoIndex, true);
            }
        }
    }

    @Override
    public void onCurrentVideoStarted() {
        Video video = mVideos[mVideoIndex];
        int progress = video.getProgress();
        if (progress > 0 && progress < video.getDuration() - Consts.TOLERANCE_VIDEO_DURATION) {
            if (mView != null) {
                mView.seekPositionOnVideoStarted(progress);
            }
            video.setProgress(0);
        }
    }

    @Override
    public void shareCurrentVideo(@NonNull Context context) {
        if (mVideos != null) {
            VideoListItemOpsKt.shareVideo(context, mVideos[mVideoIndex]);
        }
    }

    @Override
    public void shareCapturedVideoPhoto(@NonNull Context context, @NonNull File photo) {
        ShareUtils.shareFile(context, Files.PROVIDER_AUTHORITY, photo, "image/*");
    }

    @NonNull
    @Override
    public TextureVideoView.PlayListAdapter<? extends IVideoView.PlaylistViewHolder> newPlaylistAdapter() {
        return new VideoEpisodesAdapter();
    }

    private final class VideoEpisodesAdapter
            extends TextureVideoView.PlayListAdapter<IVideoView.PlaylistViewHolder> {
        VideoEpisodesAdapter() {
        }

        @NonNull
        @Override
        public IVideoView.PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return mView.newPlaylistViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull IVideoView.PlaylistViewHolder holder,
                                     int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                for (Object payload : payloads) {
                    if (!(payload instanceof Integer)) {
                        continue;
                    }
                    boolean selected = position == mVideoIndex;
                    int payloadInt = (Integer) payload;
                    if ((payloadInt & PLAYLIST_ADAPTER_PAYLOAD_VIDEO_PROGRESS_CHANGED) != 0) {
                        Video video = mVideos[position];
                        if (selected) {
                            holder.setVideoProgressAndDurationText(mContext.getString(R.string.watching));
                        } else {
                            if (video.getId() != NO_ID) {
                                holder.setVideoProgressAndDurationText(
                                        VideoUtils2.concatVideoProgressAndDuration(
                                                video.getProgress(), video.getDuration()));
                            } else {
                                holder.setVideoProgressAndDurationText(null);
                            }
                        }
                    }
                    if ((payloadInt & PLAYLIST_ADAPTER_PAYLOAD_HIGHLIGHT_ITEM_IF_SELECTED) != 0) {
                        holder.highlightItemIfSelected(selected);
                    }
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull IVideoView.PlaylistViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            boolean selected = position == mVideoIndex;
            holder.highlightItemIfSelected(selected);

            Video video = mVideos[position];
            holder.setVideoTitle(video.getName());
            if (selected) {
                holder.setVideoProgressAndDurationText(mContext.getString(R.string.watching));
            } else {
                if (video.getId() != NO_ID) {
                    holder.setVideoProgressAndDurationText(
                            VideoUtils2.concatVideoProgressAndDuration(
                                    video.getProgress(), video.getDuration()));
                } else {
                    holder.setVideoProgressAndDurationText(null);
                }
            }
        }

        @Override
        public void loadItemImages(@NonNull IVideoView.PlaylistViewHolder holder) {
            Video video = mVideos[holder.getBindingAdapterPosition()];
            holder.loadVideoThumb(video);
        }

        @Override
        public void cancelLoadingItemImages(@NonNull IVideoView.PlaylistViewHolder holder) {
            holder.cancelLoadingVideoThumb();
        }

        @Override
        public int getItemCount() {
            return mVideos.length;
        }

        @Override
        public void onItemClick(@NonNull View view, int position) {
            if (mVideoIndex == position) {
                mView.showUserCancelableSnackbar(R.string.theVideoIsPlaying, Snackbar.LENGTH_SHORT);
            } else {
                recordCurrVideoProgress();

                final int oldPosition = mVideoIndex;
                mVideoIndex = position;
                mView.setVideoToPlay(mVideos[position]);
                mView.notifyPlaylistSelectionChanged(oldPosition, position, false);
            }
        }
    }
}
