/*
 * Created on 2019/12/4 5:35 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.liuzhenlin.videos.Files;

/**
 * @author 刘振林
 */
class DbOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_VIDEOS = "videos";
    public static final String VIDEOS_COL_ID = "_id";
    public static final String VIDEOS_COL_PROGRESS = "progress";
    public static final String VIDEOS_COL_IS_TOPPED = "isTopped";

    public static final String TABLE_VIDEODIRS = "videodirs";
    public static final String VIDEODIRS_COL_NAME = "name";
    public static final String VIDEODIRS_COL_PATH = "path";
    public static final String VIDEODIRS_COL_IS_TOPPED = "isTopped";

    public DbOpenHelper(@NonNull Context context) {
        super(context, Files.DB, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //@formatter:off
        db.execSQL("CREATE TABLE " + TABLE_VIDEOS + "("
                + VIDEOS_COL_ID + " long PRIMARY KEY, "
                + VIDEOS_COL_PROGRESS + " int NOT NULL DEFAULT 0, "
                + VIDEOS_COL_IS_TOPPED + " int NOT NULL DEFAULT 0" +
                        " CHECK(" + VIDEOS_COL_IS_TOPPED + " IN (0,1)))");
        db.execSQL("CREATE TABLE " + TABLE_VIDEODIRS + "("
                + VIDEODIRS_COL_NAME + " text NOT NULL" +
                        " CHECK(LENGTH(" + VIDEODIRS_COL_NAME + ") > 0), "
                + VIDEODIRS_COL_PATH + " text PRIMARY KEY COLLATE NOCASE, "
                + VIDEODIRS_COL_IS_TOPPED + " int NOT NULL DEFAULT 0" +
                        " CHECK(" + VIDEODIRS_COL_IS_TOPPED + " IN (0,1)))");
        //@formatter:on
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
