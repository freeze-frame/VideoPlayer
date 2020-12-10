/*
 * Created on 2020-12-8 9:48:45 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.liuzhenlin.videos.ExtentionsKt;
import com.liuzhenlin.videos.view.IView;

/**
 * @author 刘振林
 */
@SuppressWarnings("rawtypes")
public class Presenter<V extends IView> implements IPresenter<V> {

    protected V mView;
    protected Context mThemedContext;
    protected Context mContext; // The application Context

    @Override
    public void attachToView(@NonNull V view) {
        if (mView != null) {
            throw new IllegalStateException(
                    "This Presenter is already attached to a View [" + mView.toString() + "]");
        }
        mView = view;
        if (view instanceof Activity) {
            mThemedContext = (Activity) view;
        } else if (view instanceof androidx.fragment.app.Fragment) {
            mThemedContext = ExtentionsKt.getContextThemedFirst((androidx.fragment.app.Fragment) view);
        } else if (view instanceof android.app.Fragment) {
            mThemedContext = ((android.app.Fragment) view).getActivity();
        }
        mContext = mThemedContext.getApplicationContext();
    }

    @Override
    public void detachFromView(@NonNull V view) {
        if (mView != view) {
            return;
        }
        mView = null;
        mThemedContext = null;
    }
}
