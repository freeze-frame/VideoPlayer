/*
 * Created on 2019/11/5 10:46 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * @author 刘振林
 */
public class MediaButtonEventReceiver extends BroadcastReceiver {

    @Nullable
    private static MediaButtonEventHandler sHandler;

//    private static final String EXTRA_MEDIA_BUTTON_EVENT_HANDLER = "extra_mediaButtonEventHandler";

    @Override
    public void onReceive(Context context, Intent intent) {
//        MediaButtonEventHandler handler = intent.getParcelableExtra(EXTRA_MEDIA_BUTTON_EVENT_HANDLER);
//        if (handler != null) {
//            handler.onMediaButtonEvent(intent);
//        }

        if (sHandler != null) {
            sHandler.onMediaButtonEvent(intent);
        }
    }

    public static void setMediaButtonEventHandler(@Nullable MediaButtonEventHandler handler) {
        sHandler = handler;
    }
}
