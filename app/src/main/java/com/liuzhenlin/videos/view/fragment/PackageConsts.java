/*
 * Created on 2019/5/11 11:50 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment;

/**
 * @author 刘振林
 */
public class PackageConsts {

    static final int PAYLOAD_CHANGE_ITEM_LPS_AND_BG = 1;
    static final int PAYLOAD_CHANGE_CHECKBOX_VISIBILITY = 1 << 1;
    static final int PAYLOAD_REFRESH_CHECKBOX = 1 << 2;
    static final int PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR = 1 << 3;
    static final int PAYLOAD_REFRESH_ITEM_NAME = 1 << 4;
    static final int PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION = 1 << 5;

    private PackageConsts() {
    }
}
