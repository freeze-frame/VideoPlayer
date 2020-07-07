/*
 * Created on 2019/12/10 9:12 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.texturevideoview.R;

/**
 * @author 刘振林
 */
public class NotificationChannelManager {

    private static String sPlaybackControlNotificationChannelId;

    private NotificationChannelManager() {
    }

    @NonNull
    public static String getPlaybackControlNotificationChannelId(@NonNull Context context) {
        getPlaybackControlNotificationChannel(context);
        return sPlaybackControlNotificationChannelId;
    }

    @Nullable
    public static NotificationChannel getPlaybackControlNotificationChannel(@NonNull Context context) {
        NotificationChannel channel = null;
        if (sPlaybackControlNotificationChannelId == null) {
            sPlaybackControlNotificationChannelId =
                    context.getString(R.string.playbackControlNotificationChannelId, context.getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                        sPlaybackControlNotificationChannelId,
                        context.getString(R.string.playbackControlNotificationChannelName),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(false);
                getNotificationManager(context).createNotificationChannel(channel);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = getNotificationManager(context)
                    .getNotificationChannel(sPlaybackControlNotificationChannelId);
            // In case system language locale changed
            channel.setName(context.getString(R.string.playbackControlNotificationChannelName));
        }
        return channel;
    }

    private static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
