/*
 * Created on 2019/3/11 7:49 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * @author 刘振林
 */
public interface FragmentPartLifecycleCallback {
    default void onFragmentAttached(@NonNull Fragment fragment) {
    }

    default void onFragmentViewCreated(@NonNull Fragment fragment) {
    }

    default void onFragmentViewDestroyed(@NonNull Fragment fragment) {
    }

    default void onFragmentDetached(@NonNull Fragment fragment) {
    }
}
