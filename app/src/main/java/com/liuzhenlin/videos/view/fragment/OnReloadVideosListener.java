/*
 * Created on 2018/09/07.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;


import androidx.annotation.Nullable;

import com.liuzhenlin.videos.model.Video;

import java.util.List;

/**
 * @author 刘振林
 */
public interface OnReloadVideosListener {
    void onReloadVideos(@Nullable List<Video> videos);
}