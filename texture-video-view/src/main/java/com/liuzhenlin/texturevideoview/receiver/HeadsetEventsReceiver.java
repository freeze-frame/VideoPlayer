/*
 * Created on 2019/3/22 5:08 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.receiver;

import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 刘振林
 */
public abstract class HeadsetEventsReceiver extends BroadcastReceiver {
    public static final String ACTION_HEADSET_PLUG =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    AudioManager.ACTION_HEADSET_PLUG : Intent.ACTION_HEADSET_PLUG;

    private final Context mContext;

    public HeadsetEventsReceiver(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        final String action = intent.getAction();
        if (ACTION_HEADSET_PLUG.equals(action)) {
            switch (intent.getIntExtra("state", -1)) {
                case 0:
                    onHeadsetPluggedOut();
                    break;
                case 1:
                    onHeadsetPluggedIn();
                    break;
            }
        } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
            onHeadsetPluggedOutOrBluetoothDisconnected();

        } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
            switch (intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, 0)) {
                case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                    onBluetoothConnected();
                    break;
                case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                    onBluetoothDisconnected();
                    break;
            }
        }
    }

    public void register() {
        IntentFilter filter = new IntentFilter(ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void register(@Nullable String... actions) {
        if (actions == null || actions.length == 0) {
            register();
            return;
        }

        IntentFilter filter = new IntentFilter();
        for (String action : actions)
            if (ACTION_HEADSET_PLUG.equals(action)
                    || AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)
                    || BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                filter.addAction(action);
            }
        mContext.registerReceiver(this, filter);
    }

    public void unregister() {
        mContext.unregisterReceiver(this);
    }

    protected void onHeadsetPluggedIn() {}
    protected void onHeadsetPluggedOut() {}
    protected void onHeadsetPluggedOutOrBluetoothDisconnected() {}
    protected void onBluetoothConnected() {}
    protected void onBluetoothDisconnected() {}
}