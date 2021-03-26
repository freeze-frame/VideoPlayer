/*
 * Created on 2019/12/8 6:38 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liuzhenlin.common.R;

/**
 * @author 刘振林
 */
public class NotificationChannelManager {

    private static String sSilentNotificationChannelId;
    private static String sDownloadNotificationChannelId;
    private static String sMsgNotificationChannelId;
    private static String sPlaybackControlNotificationChannelId;

    private NotificationChannelManager() {
    }

    @NonNull
    public static String getSilentNotificationChannelId(@NonNull Context context) {
        getSilentNotificationChannel(context);
        return sSilentNotificationChannelId;
    }

    @NonNull
    public static String getDownloadNotificationChannelId(@NonNull Context context) {
        getDownloadNotificationChannel(context);
        return sDownloadNotificationChannelId;
    }

    @NonNull
    public static String getMessageNotificationChannelId(@NonNull Context context) {
        getMessageNotificationChannel(context);
        return sMsgNotificationChannelId;
    }

    @NonNull
    public static String getPlaybackControlNotificationChannelId(@NonNull Context context) {
        getPlaybackControlNotificationChannel(context);
        return sPlaybackControlNotificationChannelId;
    }

    @Nullable
    public static NotificationChannel getSilentNotificationChannel(@NonNull Context context) {
        NotificationChannel channel = null;
        if (sSilentNotificationChannelId == null) {
            sSilentNotificationChannelId =
                    context.getString(R.string.silentNotificationChannelId, context.getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                        sSilentNotificationChannelId,
                        context.getString(R.string.silentNotificationChannelName),
                        NotificationManager.IMPORTANCE_LOW);
                channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(false);
                getNotificationManager(context).createNotificationChannel(channel);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = getNotificationManager(context)
                    .getNotificationChannel(sSilentNotificationChannelId);
            // 通知渠道的名称需要随着系统语言环境改变而改变...
            channel.setName(context.getString(R.string.silentNotificationChannelName));
        }
        return channel;
    }

    @Nullable
    public static NotificationChannel getDownloadNotificationChannel(@NonNull Context context) {
        NotificationChannel channel = null;
        if (sDownloadNotificationChannelId == null) {
            sDownloadNotificationChannelId =
                    context.getString(R.string.downloadNotificationChannelId, context.getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                        sDownloadNotificationChannelId,
                        context.getString(R.string.downloadNotificationChannelName),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(false);
                getNotificationManager(context).createNotificationChannel(channel);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = getNotificationManager(context)
                    .getNotificationChannel(sDownloadNotificationChannelId);
            // 通知渠道的名称需要随着系统语言环境改变而改变...
            channel.setName(context.getString(R.string.downloadNotificationChannelName));
        }
        return channel;
    }

    @Nullable
    public static NotificationChannel getMessageNotificationChannel(@NonNull Context context) {
        NotificationChannel channel = null;
        if (sMsgNotificationChannelId == null) {
            sMsgNotificationChannelId =
                    context.getString(R.string.msgNotificationChannelId, context.getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel = new NotificationChannel(
                        sMsgNotificationChannelId,
                        context.getString(R.string.msgNotificationChannelName),
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI,
                        Notification.AUDIO_ATTRIBUTES_DEFAULT);
                channel.enableVibration(false);
                channel.enableLights(true);
                channel.setShowBadge(true);
                getNotificationManager(context).createNotificationChannel(channel);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = getNotificationManager(context)
                    .getNotificationChannel(sMsgNotificationChannelId);
            // 通知渠道的名称需要随着系统语言环境改变而改变...
            channel.setName(context.getString(R.string.msgNotificationChannelName));
        }
        return channel;
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
