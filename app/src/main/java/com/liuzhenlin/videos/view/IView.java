/*
 * Created on 2020-12-10 10:04:20 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.videos.App;
import com.liuzhenlin.videos.presenter.IPresenter;

@SuppressWarnings("rawtypes")
public interface IView<P extends IPresenter> {

    default void showUserCancelableSnackbar(@NonNull CharSequence text, @Snackbar.Duration int duration) {
        throw new RuntimeException("Stub!");
    }

    default void showUserCancelableSnackbar(@StringRes int resId, @Snackbar.Duration int duration) {
        Context context = App.getInstanceUnsafe();
        showUserCancelableSnackbar(context.getText(resId), duration);
    }

    default void showToast(@NonNull Context context, @NonNull CharSequence text, int duration) {
        Toast.makeText(context, text, duration).show();
    }

    default void showToast(@NonNull Context context, @StringRes int resId, int duration) {
        showToast(context, context.getText(resId), duration);
    }
}