/*
 * Created on 2019/3/15 1:53 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;

import androidx.annotation.Nullable;

import com.liuzhenlin.common.view.SwipeRefreshLayout;

/**
 * @author 刘振林
 */
public interface RefreshLayoutCallback {
    boolean isRefreshLayoutEnabled();

    void setRefreshLayoutEnabled(boolean enabled);

    boolean isRefreshLayoutRefreshing();

    void setRefreshLayoutRefreshing(boolean refreshing);

    void setOnRefreshLayoutChildScrollUpCallback(@Nullable SwipeRefreshLayout.OnChildScrollUpCallback callback);
}
