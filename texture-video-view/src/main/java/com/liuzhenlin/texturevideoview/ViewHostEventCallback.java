/*
 * Created on 2019/6/8 10:57 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.app.Activity;
import android.app.Dialog;

/**
 * @author 刘振林
 */
public interface ViewHostEventCallback {
    /**
     * Call this when the view host (Activity for instance) has detected the user's press of the back key.
     *
     * @return true if the back key event is handled by the view
     * @see Activity#onBackPressed()
     * @see Dialog#onBackPressed()
     */
    boolean onBackPressed();

    /**
     * Call this when the host of the view or just itself changes to and from minimization mode
     *
     * @param minimized true if the host or the view is in minimization mode
     * @see Activity#onPictureInPictureModeChanged(boolean)
     */
    void onMinimizationModeChange(boolean minimized);
}
