/*
 * Created on 2019/12/4 5:26 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.dao;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.liuzhenlin.videos.bean.Video;

/**
 * @author 刘振林
 */
public interface IVideoDao {
    Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    String VIDEO_ID = MediaStore.Video.Media._ID;
    String VIDEO_NAME = MediaStore.Video.Media.DISPLAY_NAME;
    String VIDEO_PATH = MediaStore.Video.Media.DATA;
    String VIDEO_SIZE = MediaStore.Video.Media.SIZE;
    String VIDEO_DURATION = MediaStore.Video.Media.DURATION;
    String VIDEO_RESOLUTION = MediaStore.Video.Media.RESOLUTION;
    @RequiresApi(Build.VERSION_CODES.Q)
    String VIDEO_ORIENTATION = MediaStore.Video.Media.ORIENTATION;

    boolean insertVideo(@Nullable Video video);

    boolean deleteVideo(long id);

    boolean updateVideo(@Nullable Video video);

    @Nullable
    Video queryVideoById(long id);

    @Nullable
    Video queryVideoByPath(@Nullable String path);

    @Nullable
    Cursor queryAllVideos();

    @Nullable
    Cursor queryAllVideosInDirectory(@Nullable String directory /* directory path */);
}
