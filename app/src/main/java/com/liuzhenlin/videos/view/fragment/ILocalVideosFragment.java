/*
 * Created on 2019/11/6 12:13 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;

/**
 * @author 刘振林
 */
public interface ILocalVideosFragment extends OnBackPressedListener {
    void goToLocalFoldedVideosFragment(@NonNull Bundle args);
}
