/*
 * Created on 2020-12-10 2:09:51 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.activity;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.videos.presenter.IFeedbackPresenter;
import com.liuzhenlin.videos.view.IView;

import java.util.List;

/**
 * @author 刘振林
 */
public interface IFeedbackView extends IView<IFeedbackPresenter> {

    void toastResultOnUserFilledDataSaved();

    void refreshCurrTexts(@NonNull String text, @NonNull String contactWay);

    void pickPicture();

    void showPicturePreviewDialog(@NonNull List<Bitmap> pictures, int currentItem);
    void hidePicturePreviewDialog();

    @NonNull
    PictureGridViewHolder getPictureGridViewHolder(
            int position, @Nullable View convertView, @NonNull ViewGroup parent,
            @NonNull Bitmap picture, int pictureCount);

    abstract class PictureGridViewHolder {
        @NonNull
        public final View itemView;

        protected PictureGridViewHolder(@NonNull View itemView) {
            this.itemView = itemView;
        }
    }
}
