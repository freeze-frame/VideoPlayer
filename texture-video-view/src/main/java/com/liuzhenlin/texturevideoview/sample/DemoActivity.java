/*
 * Created on 2019/5/18 11:44 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.util.Synthetic;
import com.liuzhenlin.texturevideoview.IVideoPlayer;
import com.liuzhenlin.texturevideoview.R;
import com.liuzhenlin.texturevideoview.SystemVideoPlayer;
import com.liuzhenlin.texturevideoview.TextureVideoView;
import com.liuzhenlin.texturevideoview.VideoPlayer;
import com.liuzhenlin.texturevideoview.utils.ShareUtils;

import java.io.File;

/**
 * @author 刘振林
 */
public class DemoActivity extends AppCompatActivity {
    @Synthetic TextureVideoView mVideoView;
    @Synthetic VideoPlayer mVideoPlayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        // First, interrelates TextureVideoView with VideoPlayer
        mVideoView = findViewById(R.id.videoview);
        mVideoPlayer = new SystemVideoPlayer(this);
        mVideoPlayer.setVideoView(mVideoView);
        mVideoView.setVideoPlayer(mVideoPlayer);

        mVideoView.setTitle("Simplest Playback Demo for TextureVideoView");
        mVideoPlayer.setVideoUri(getIntent().getData());
        mVideoPlayer.addVideoListener(new IVideoPlayer.VideoListener() {
            @Override
            public void onVideoStarted() {
                // no-op
            }

            @Override
            public void onVideoStopped() {
                // no-op
            }

            @Override
            public void onVideoRepeat() {
                // no-op
            }

            @Override
            public void onVideoBufferingStateChanged(boolean buffering) {
                // no-op
            }

            @Override
            public void onVideoDurationChanged(int duration) {
                // no-op
            }

            @Override
            public void onVideoSizeChanged(int width, int height) {
                // no-op
            }
        });
        mVideoPlayer.setOnSkipPrevNextListener(new VideoPlayer.OnSkipPrevNextListener() {
            @Override
            public void onSkipToPrevious() {
                // no-op
            }

            @Override
            public void onSkipToNext() {
                // no-op
            }
        });
        mVideoView.setEventListener(new TextureVideoView.EventListener() {
            @Override
            public void onPlayerChange(@Nullable VideoPlayer videoPlayer) {
                mVideoPlayer = videoPlayer;
            }

            @Override
            public void onReturnClicked() {
                finish();
            }

            @Override
            public void onBackgroundPlaybackControllerClose() {
                finish();
            }

            @Override
            public void onViewModeChange(int oldMode, int newMode, boolean layoutMatches) {
                switch (newMode) {
                    case TextureVideoView.VIEW_MODE_MINIMUM:
                        //noinspection StatementWithEmptyBody
                        if (!layoutMatches) {
                            // do something like entering picture-in-picture mode
                        }
                        break;
                    case TextureVideoView.VIEW_MODE_DEFAULT:
                        mVideoView.setFullscreenMode(false, 0);
                        break;
                    case TextureVideoView.VIEW_MODE_FULLSCREEN:
                    case TextureVideoView.VIEW_MODE_LOCKED_FULLSCREEN:
                    case TextureVideoView.VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN:
                    case TextureVideoView.VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN:
                        mVideoView.setFullscreenMode(true, 0);
                        break;
                }
            }

            @Override
            public void onShareVideo() {
                // Place the code describing how to share the video here
            }

            @Override
            public void onShareCapturedVideoPhoto(@NonNull File photo) {
                ShareUtils.shareFile(DemoActivity.this, getPackageName() + ".provider",
                        photo, "image/*");
            }
        });
        mVideoView.setOpCallback(new TextureVideoView.OpCallback() {
            @NonNull
            @Override
            public Window getWindow() {
                return DemoActivity.this.getWindow();
            }

            @NonNull
            @Override
            public Class<? extends Activity> getHostActivityClass() {
                return DemoActivity.this.getClass();
            }

            // Optional, just returns null to use the default base output directory
            // (the primary external storage directory concatenating with this application name).
            @Nullable
            @Override
            public String getAppExternalFilesDir() {
                return null;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVideoPlayer.openVideo();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideoPlayer.closeVideo();
    }

    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
