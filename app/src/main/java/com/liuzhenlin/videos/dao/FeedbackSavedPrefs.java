/*
 * Created on 2018/04/13.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.dao;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kotlin.text.StringsKt;

/**
 * @author 刘振林
 */
public class FeedbackSavedPrefs {
    private final SharedPreferences mSP;
    private final SharedPreferences.Editor mEditor;

    private static final String TEXT = "text";
    private static final String CONTACT_WAY = "contactWay";
    private static final String PICTURE_PATHS = "picturePaths";

    private static final String DELIMITER_PICTURE_PATHS = File.separator + File.separator;
    private static final String[] DELIMITER_ARRAY_PICTURE_PATHS = {DELIMITER_PICTURE_PATHS};

    @SuppressLint("CommitPrefEdits")
    public FeedbackSavedPrefs(@NonNull Context context) {
        mSP = context.getSharedPreferences(Files.SAVED_FEEDBACK_PREFS, Context.MODE_PRIVATE);
        mEditor = mSP.edit();
    }

    public void saveText(@Nullable String text) {
        mEditor.putString(TEXT, text).apply();
    }

    @NonNull
    public String getText() {
        //noinspection ConstantConditions
        return mSP.getString(TEXT, Consts.EMPTY_STRING);
    }

    public void saveContactWay(@Nullable String contactWay) {
        mEditor.putString(CONTACT_WAY, contactWay).apply();
    }

    @NonNull
    public String getContactWay() {
        //noinspection ConstantConditions
        return mSP.getString(CONTACT_WAY, Consts.EMPTY_STRING);
    }

    public void savePicturePaths(@Nullable List<String> paths) {
        mEditor.putString(PICTURE_PATHS, combinePicturePaths(paths)).apply();
    }

    private String combinePicturePaths(List<String> paths) {
        if (paths != null) {
            final int size = paths.size();
            if (size > 0) {
                StringBuilder pathsString = new StringBuilder();

                for (int i = 0; i < size - 1; i++) {
                    pathsString.append(paths.get(i)).append(DELIMITER_PICTURE_PATHS);
                }
                pathsString.append(paths.get(size - 1));

                return pathsString.toString();
            }
        }
        return null;
    }

    @Nullable
    public List<String> getPicturePaths() {
        List<String> paths = null;

        String pathsString = null;
        try {
            pathsString = mSP.getString(PICTURE_PATHS, null);

            // 用于兼容1.6.3以前的旧版本
        } catch (ClassCastException e) {
            Set<String> pathSet = mSP.getStringSet(PICTURE_PATHS, null);
            if (pathSet != null) {
                pathsString = combinePicturePaths(new ArrayList<>(pathSet));

                mEditor.putString(PICTURE_PATHS, pathsString).apply();
            }
        }
        if (pathsString != null) {
            paths = new ArrayList<>(
                    StringsKt.split(pathsString, DELIMITER_ARRAY_PICTURE_PATHS, false, 0));
        }

        return paths;
    }

    public void clear() {
        mEditor.clear().apply();
    }
}
