/*
 * Created on 2020-12-8 9:29:19 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import androidx.annotation.NonNull;

import com.liuzhenlin.videos.view.IView;

/**
 * @author 刘振林
 */
@SuppressWarnings("rawtypes")
public interface IPresenter<V extends IView> {
    void attachToView(@NonNull V view);
    void detachFromView(@NonNull V view);
}
