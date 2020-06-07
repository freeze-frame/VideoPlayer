/*
 * Created on 2019/3/15 10:00 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * @author 刘振林
 */
public interface ActionBarCallback {
    @NonNull
    View getActionBar(@NonNull Fragment fragment);
}
