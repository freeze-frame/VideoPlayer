/*
 * Created on 2017/12/07.
 * Copyright © 2017–2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.dao;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.utils.FileUtils;
import com.liuzhenlin.texturevideoview.utils.Singleton;
import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.bean.Video;
import com.liuzhenlin.videos.bean.VideoDirectory;
import com.liuzhenlin.videos.bean.VideoListItem;

import java.io.File;

import kotlin.text.StringsKt;

import static com.liuzhenlin.videos.dao.DbOpenHelper.TABLE_VIDEODIRS;
import static com.liuzhenlin.videos.dao.DbOpenHelper.TABLE_VIDEOS;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEODIRS_COL_IS_TOPPED;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEODIRS_COL_NAME;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEODIRS_COL_PATH;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEOS_COL_ID;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEOS_COL_IS_TOPPED;
import static com.liuzhenlin.videos.dao.DbOpenHelper.VIDEOS_COL_PROGRESS;

/**
 * @author 刘振林
 */
public final class VideoListItemDao implements IVideoListItemDao {

    private final ContentResolver mContentResolver;
    private final SQLiteDatabase mDB;

    private static final String[] PROJECTION_VIDEO_URI =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q //@formatter:off
                    ? new String[]{
                            VIDEO_ID,
                            VIDEO_NAME,
                            VIDEO_PATH,
                            VIDEO_SIZE,
                            VIDEO_DURATION,
                            VIDEO_RESOLUTION,
                            VIDEO_ORIENTATION
                    }
                    : new String[]{
                            VIDEO_ID,
                            VIDEO_NAME,
                            VIDEO_PATH,
                            VIDEO_SIZE,
                            VIDEO_DURATION,
                            VIDEO_RESOLUTION
                    }; //@formatter:on

    private static String sResolutionSeparator;
    private static final String SEPARATOR_LOWERCASE_X = "x";
    private static final String SEPARATOR_MULTIPLE_SIGN = "×";

    private static final Singleton<Context, VideoListItemDao> sVideoListItemDaoSingleton =
            new Singleton<Context, VideoListItemDao>() {
                @SuppressLint("SyntheticAccessor")
                @NonNull
                @Override
                protected VideoListItemDao onCreate(Context... ctxs) {
                    return new VideoListItemDao(ctxs[0]);
                }
            };

    public static VideoListItemDao getSingleton(@NonNull Context context) {
        return sVideoListItemDaoSingleton.get(context);
    }

    private VideoListItemDao(Context context) {
        context = context.getApplicationContext();
        mContentResolver = context.getContentResolver();
        mDB = new DbOpenHelper(context).getWritableDatabase();
    }

    private void ensureResolutionSeparator() {
        if (sResolutionSeparator == null) {
            Cursor cursor = mContentResolver.query(VIDEO_URI,
                    new String[]{VIDEO_RESOLUTION},
                    VIDEO_DURATION + " IS NOT NULL", null,
                    "NULL LIMIT 1");
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    if (cursor.getString(0).contains(SEPARATOR_LOWERCASE_X)) {
                        sResolutionSeparator = SEPARATOR_LOWERCASE_X;
                    } else {
                        sResolutionSeparator = SEPARATOR_MULTIPLE_SIGN;
                    }
                }
                cursor.close();
            }
            if (sResolutionSeparator == null) {
                sResolutionSeparator = SEPARATOR_LOWERCASE_X;
            }
        }
    }

    @Override
    public boolean insertVideo(@Nullable Video video) {
        if (video == null) return false;

        ContentValues values = new ContentValues(5);
        values.put(VIDEO_NAME, video.getName());
        values.put(VIDEO_PATH, video.getPath());
        values.put(VIDEO_SIZE, video.getSize());
        values.put(VIDEO_DURATION, video.getDuration());
        ensureResolutionSeparator();
        values.put(VIDEO_RESOLUTION, video.getWidth() + sResolutionSeparator + video.getHeight());

        if (mContentResolver.insert(VIDEO_URI, values) == null) {
            return false;
        }

        values.clear();
        values.put(VIDEOS_COL_ID, video.getId());
        values.put(VIDEOS_COL_PROGRESS, video.getProgress());
        values.put(VIDEOS_COL_IS_TOPPED, video.isTopped() ? 1 : 0);
        return mDB.insert(TABLE_VIDEOS, null, values) != Consts.NO_ID;
    }

    @Override
    public boolean deleteVideo(long id) {
        mDB.delete(TABLE_VIDEOS, VIDEOS_COL_ID + "=" + id, null);
        return mContentResolver.delete(VIDEO_URI, VIDEO_ID + "=" + id, null) == 1;
    }

    @Override
    public boolean updateVideo(@Nullable Video video) {
        if (video == null) return false;

        ContentValues values = new ContentValues(5);
        values.put(VIDEO_NAME, video.getName());
        values.put(VIDEO_PATH, video.getPath());
        values.put(VIDEO_SIZE, video.getSize());
        values.put(VIDEO_DURATION, video.getDuration());
        ensureResolutionSeparator();
        values.put(VIDEO_RESOLUTION, video.getWidth() + sResolutionSeparator + video.getHeight());

        final long id = video.getId();
        if (mContentResolver.update(VIDEO_URI, values, VIDEO_ID + "=" + id, null) == 1) {
            values.clear();
            values.put(VIDEOS_COL_PROGRESS, video.getProgress());
            values.put(VIDEOS_COL_IS_TOPPED, video.isTopped() ? 1 : 0);

            if (mDB.update(TABLE_VIDEOS, values, VIDEOS_COL_ID + "=" + id, null) == 1) {
                return true;
            }

            values.put(VIDEOS_COL_ID, id);
            return mDB.insert(TABLE_VIDEOS, null, values) != Consts.NO_ID;
        }
        return false;
    }

    @Nullable
    @Override
    public Video queryVideoById(long id) {
        Cursor cursor = mContentResolver.query(VIDEO_URI,
                PROJECTION_VIDEO_URI,
                VIDEO_ID + "=" + id, null,
                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return buildVideo(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Video queryVideoByPath(@Nullable String path) {
        if (path == null) return null;

        Cursor cursor = mContentResolver.query(VIDEO_URI,
                PROJECTION_VIDEO_URI,
                VIDEO_PATH + "='" + escapedComparisionString(path) + "' COLLATE NOCASE", null,
                null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return buildVideo(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private String escapedComparisionString(String string) {
        return StringsKt.replace(string, "'", "''", false);
    }

    @Nullable
    @Override
    public Cursor queryAllVideos() {
        return mContentResolver.query(VIDEO_URI, PROJECTION_VIDEO_URI, null, null, null);
    }

    @Nullable
    @Override
    public Cursor queryAllVideosInDirectory(@Nullable String directory) {
        if (directory == null) return null;

        final int strlength = directory.length();
        return mContentResolver.query(VIDEO_URI,
                PROJECTION_VIDEO_URI,
                "SUBSTR(" + VIDEO_PATH + ",1," + strlength + ")='" +
                        escapedComparisionString(directory) + "' COLLATE NOCASE " +
                        "AND SUBSTR(" + VIDEO_PATH + "," + (strlength + 2) + ") " +
                        "NOT LIKE '%" + File.separator + "%'", null,
                null);
    }

    @Override
    public boolean insertVideoDir(@Nullable VideoDirectory videodir) {
        if (videodir == null) return false;

        ContentValues values = new ContentValues(3);
        values.put(VIDEODIRS_COL_NAME, videodir.getName());
        values.put(VIDEODIRS_COL_PATH, videodir.getPath());
        values.put(VIDEODIRS_COL_IS_TOPPED, videodir.isTopped() ? 1 : 0);
        return mDB.insert(TABLE_VIDEODIRS, null, values) != Consts.NO_ID;
    }

    @Override
    public boolean deleteVideoDir(@Nullable String directory) {
        if (directory == null) return false;

        return mDB.delete(TABLE_VIDEODIRS,
                VIDEODIRS_COL_PATH + "='" + escapedComparisionString(directory) + "'", null)
                == 1;
    }

    @Override
    public boolean updateVideoDir(@Nullable VideoDirectory videodir) {
        if (videodir == null) return false;

        ContentValues values = new ContentValues(2);
        values.put(VIDEODIRS_COL_NAME, videodir.getName());
        values.put(VIDEODIRS_COL_IS_TOPPED, videodir.isTopped() ? 1 : 0);
        return mDB.update(TABLE_VIDEODIRS,
                values,
                VIDEODIRS_COL_PATH + "='" + escapedComparisionString(videodir.getPath()) + "'", null)
                == 1;
    }

    @Nullable
    @Override
    public VideoDirectory queryVideoDirByPath(@Nullable String path) {
        if (path == null) return null;

        Cursor cursor = mDB.rawQuery("SELECT * FROM " + TABLE_VIDEODIRS +
                " WHERE " + VIDEODIRS_COL_PATH + "='" + escapedComparisionString(path) + "'", null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return buildVideoDir(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Cursor queryAllVideoDirs() {
        return mDB.rawQuery("SELECT * FROM " + TABLE_VIDEODIRS, null);
    }

    @NonNull
    public VideoDirectory buildVideoDir(@NonNull Cursor cursor) {
        VideoDirectory videodir = new VideoDirectory();
        videodir.setName(cursor.getString(cursor.getColumnIndexOrThrow(VIDEODIRS_COL_NAME)));
        videodir.setPath(cursor.getString(cursor.getColumnIndexOrThrow(VIDEODIRS_COL_PATH)));
        videodir.setTopped(cursor.getInt(cursor.getColumnIndexOrThrow(VIDEODIRS_COL_IS_TOPPED)) != 0);
        return videodir;
    }

//    @RecentlyNonNull
    @SuppressLint("NewApi")
    public Video buildVideo(@NonNull Cursor cursor) {
        Video video = new Video();

        final String[] columnNames = cursor.getColumnNames();
        for (int i = 0; i < columnNames.length; i++)
            switch (columnNames[i]) {
                case VIDEO_ID:
                    video.setId(cursor.getLong(i));
                    break;
                case VIDEO_NAME:
                    final String name = cursor.getString(i);
                    if (name != null) {
                        video.setName(name);
                    }
                    break;
                case VIDEO_PATH:
                    final String path = cursor.getString(i);
                    video.setPath(path);
                    if (video.getName().isEmpty()) {
                        video.setName(FileUtils.getFileNameFromFilePath(path));
                    }
                    break;
                case VIDEO_SIZE:
                    video.setSize(cursor.getLong(i));
                    break;
                case VIDEO_DURATION:
                    video.setDuration((int) cursor.getLong(i));
                    break;
                case VIDEO_RESOLUTION:
                    final String resolution = cursor.getString(i);
                    if (resolution != null) {
                        int separatorIndex;
                        if (sResolutionSeparator == null) {
                            separatorIndex = resolution.indexOf(SEPARATOR_LOWERCASE_X);
                            if (separatorIndex != -1) {
                                sResolutionSeparator = SEPARATOR_LOWERCASE_X;
                            } else {
                                sResolutionSeparator = SEPARATOR_MULTIPLE_SIGN;
                                separatorIndex = resolution.indexOf(SEPARATOR_MULTIPLE_SIGN);
                            }
                        } else {
                            separatorIndex = resolution.indexOf(sResolutionSeparator);
                        }
                        video.setWidth(Integer.parseInt(resolution.substring(0, separatorIndex)));
                        video.setHeight(Integer.parseInt(resolution.substring(separatorIndex + 1)));
                    }
                    break;
                case VIDEO_ORIENTATION:
                    final int orientation = cursor.getInt(i);
                    if (orientation == 90 || orientation == 270) {
                        int swap = video.getWidth();
                        video.setWidth(video.getHeight());
                        video.setHeight(swap);
                    }
                    break;
            }
        if (video.getDuration() <= 0 || video.getWidth() <= 0 || video.getHeight() <= 0) {
            if (invalidateVideoDurationAndResolution(video)) {
                updateVideo(video);
            } /*else {
                return null;
            }*/
        }

        Cursor cursor2 = mDB.rawQuery(
                "SELECT " + VIDEOS_COL_PROGRESS + "," + VIDEOS_COL_IS_TOPPED +
                        " FROM " + TABLE_VIDEOS +
                        " WHERE " + VIDEOS_COL_ID + "=" + video.getId(), null);
        if (cursor2 != null) {
            if (cursor2.moveToFirst()) {
                video.setProgress(cursor2.getInt(0));
                video.setTopped(cursor2.getInt(1) != 0);
            }
            cursor2.close();
        }

        return video;
    }

    public boolean invalidateVideoDurationAndResolution(@NonNull Video video) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(video.getPath());

            final int duration = Integer.parseInt(
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            int width, height;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                width = Integer.parseInt(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                height = Integer.parseInt(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                final int rotation = Integer.parseInt(
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                if (rotation == 90 || rotation == 270) {
                    int swap = width;
                    //noinspection SuspiciousNameCombination
                    width = height;
                    height = swap;
                }
            } else {
                Bitmap frame = mmr.getFrameAtTime();
                width = frame.getWidth();
                height = frame.getHeight();
                frame.recycle();
            }

            video.setDuration(duration);
            video.setWidth(width);
            video.setHeight(height);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        } finally {
            mmr.release();
        }
    }

    public boolean setVideoProgress(long id, int progress) {
        ContentValues values = new ContentValues(1);
        values.put(VIDEOS_COL_PROGRESS, progress);

        if (mDB.update(TABLE_VIDEOS, values, VIDEOS_COL_ID + "=" + id, null) == 1) {
            return true;
        }

        values.put(VIDEOS_COL_ID, id);
        return mDB.insert(TABLE_VIDEOS, null, values) != Consts.NO_ID;
    }

    public int getVideoProgress(long id) {
        Cursor cursor = mDB.rawQuery("SELECT " + VIDEOS_COL_PROGRESS +
                " FROM " + TABLE_VIDEOS +
                " WHERE " + VIDEOS_COL_ID + "=" + id, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    public boolean setVideoListItemTopped(@NonNull VideoListItem item, boolean topped) {
        ContentValues values = new ContentValues(1);
        if (item instanceof Video) {
            final long id = ((Video) item).getId();

            values.put(VIDEOS_COL_IS_TOPPED, topped ? 1 : 0);
            if (mDB.update(TABLE_VIDEOS, values, VIDEOS_COL_ID + "=" + id, null) == 1) {
                return true;
            }

            values.put(VIDEOS_COL_ID, id);
            return mDB.insert(TABLE_VIDEOS, null, values) != Consts.NO_ID;
        } else /* if (item instanceof VideoDirectory) */ {
            values.put(VIDEODIRS_COL_IS_TOPPED, topped ? 1 : 0);
            return mDB.update(TABLE_VIDEODIRS,
                    values,
                    VIDEODIRS_COL_PATH + "='" + escapedComparisionString(item.getPath()) + "'", null)
                    == 1;
        }
    }

    public boolean isVideoListItemTopped(@NonNull VideoListItem item) {
        Cursor cursor;
        if (item instanceof Video) {
            cursor = mDB.rawQuery("SELECT " + VIDEOS_COL_IS_TOPPED + " FROM " + TABLE_VIDEOS +
                    " WHERE " + VIDEOS_COL_ID + "=" + ((Video) item).getId(), null);
        } else {
            cursor = mDB.rawQuery("SELECT " + VIDEODIRS_COL_IS_TOPPED + " FROM " + TABLE_VIDEODIRS +
                    " WHERE " + VIDEODIRS_COL_PATH + "='" + escapedComparisionString(item.getPath()) + "'", null);
        }
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0) != 0;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }
}
