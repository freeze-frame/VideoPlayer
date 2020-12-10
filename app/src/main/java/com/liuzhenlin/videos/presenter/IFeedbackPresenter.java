/*
 * Created on 2020-12-10 2:09:17 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.videos.view.activity.IFeedbackView;

/**
 * @author 刘振林
 */
public interface IFeedbackPresenter extends IPresenter<IFeedbackView> {

    void restoreData(@Nullable Bundle savedInstanceState);
    void saveData(@NonNull Bundle outState, @NonNull String text, @NonNull String contactWay);

    void sendFeedback(@NonNull String text, @NonNull String contactWay);
    void persistentlySaveUserFilledData(
            @NonNull String text, @NonNull String contactWay, boolean toastResultIfSaved);

    boolean hasDataChanged(@NonNull String text, @NonNull String contactWay);

    void addPicture(@Nullable String path);
    void removePictureAt(int index);
    void recyclePictures();

    @NonNull
    <T extends BaseAdapter & AdapterView.OnItemClickListener> T getPictureGridAdapter();

    @NonNull
    static IFeedbackPresenter newInstance() {
        return new FeedbackPresenter();
    }
}
