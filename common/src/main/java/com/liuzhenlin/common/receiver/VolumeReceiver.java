/*
 * Created on 2018/9/18 11:59 PM.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author 刘振林
 */
public abstract class VolumeReceiver extends BroadcastReceiver {
    /**
     * Broadcast intent when the volume for a particular stream type changes.
     * Includes the stream, the new and the previous volumes.
     *
     * @see #EXTRA_VOLUME_STREAM_TYPE
     * @see #EXTRA_PREV_VOLUME_STREAM_VALUE
     * @see #EXTRA_VOLUME_STREAM_VALUE
     */
    public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    /** The stream type for the volume changed intent. */
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    /** The previous volume associated with the stream for the volume changed intent. */
    private static final String EXTRA_PREV_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    /** The volume associated with the stream for the volume changed intent. */
    private static final String EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE";

    @IntDef({
            AudioManager.RINGER_MODE_NORMAL,
            AudioManager.RINGER_MODE_SILENT,
            AudioManager.RINGER_MODE_VIBRATE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RingerMode {
    }

    private final Context mContext;

    public VolumeReceiver(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        final String action = intent.getAction();
        if (VOLUME_CHANGED_ACTION.equals(action)) {
            final int volumeType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
            final int prevolume = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
            final int volume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1);
            switch (volumeType) {
                case AudioManager.STREAM_SYSTEM:
                    onSystemVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_MUSIC:
                    onMusicVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_RING:
                    onRingVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_VOICE_CALL:
                    onVoiceCallVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_ALARM:
                    onAlarmVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_NOTIFICATION:
                    onNotificationVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_DTMF:
                    onDtmfVolumeChange(prevolume, volume);
                    break;
                case AudioManager.STREAM_ACCESSIBILITY:
                    onAccessibilityVolumeChange(prevolume, volume);
                    break;
            }
        } else if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
            onRingerModeChange(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1));
        }
    }

    public void register() {
        IntentFilter filter = new IntentFilter(VolumeReceiver.VOLUME_CHANGED_ACTION);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        mContext.registerReceiver(this, filter);
    }

    public void register(@Nullable String... actions) {
        if (actions == null || actions.length == 0) {
            register();
            return;
        }

        IntentFilter filter = new IntentFilter();
        for (String action : actions)
            if (VolumeReceiver.VOLUME_CHANGED_ACTION.equals(action)
                    || AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                filter.addAction(action);
            }
        mContext.registerReceiver(this, filter);
    }

    public void unregister() {
        mContext.unregisterReceiver(this);
    }

    protected void onSystemVolumeChange(int prevolume, int volume) {}
    protected void onMusicVolumeChange(int prevolume, int volume) {}
    protected void onRingVolumeChange(int prevolume, int volume) {}
    protected void onVoiceCallVolumeChange(int prevolume, int volume) {}
    protected void onAlarmVolumeChange(int prevolume, int volume) {}
    protected void onNotificationVolumeChange(int prevolume, int volume) {}
    protected void onDtmfVolumeChange(int prevolume, int volume) {}
    protected void onAccessibilityVolumeChange(int prevolume, int volume) {}
    protected void onRingerModeChange(@RingerMode int mode) {}
}
