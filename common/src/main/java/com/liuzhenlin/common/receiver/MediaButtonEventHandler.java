/*
 * Created on 2019/11/9 4:47 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.receiver;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

import com.liuzhenlin.common.Consts;

/**
 * @author 刘振林
 */
public class MediaButtonEventHandler implements Parcelable {

    public static final int MSG_PLAY_PAUSE_KEY_SINGLE_TAP = 1;
    public static final int MSG_PLAY_PAUSE_KEY_DOUBLE_TAP = 2;
    public static final int MSG_PLAY_PAUSE_KEY_TRIPLE_TAP = 3;
    public static final int MSG_MEDIA_PREVIOUS = 4;
    public static final int MSG_MEDIA_NEXT = 5;

    private final Messenger mMessenger;

    private int mPlayPauseKeyTappedTime;

    private final Runnable mPlayPauseKeyTimeoutRunnable =
            this::handlePlayPauseKeySingleOrDoubleTapAsNeeded;

    public MediaButtonEventHandler(@NonNull Messenger messenger) {
        mMessenger = messenger;
    }

    protected MediaButtonEventHandler(Parcel in) {
        mMessenger = in.readParcelable(Messenger.class.getClassLoader());
    }

    protected boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        final int keyCode = keyEvent.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (keyEvent.getRepeatCount() > 0) {
                    // Consider long-press as a single tap.
                    handlePlayPauseKeySingleOrDoubleTapAsNeeded();

                } else {
                    Handler handler = Consts.getMainThreadHandler();
                    switch (mPlayPauseKeyTappedTime) {
                        case 0:
                            mPlayPauseKeyTappedTime = 1;
                            handler.postDelayed(mPlayPauseKeyTimeoutRunnable,
                                    ViewConfiguration.getDoubleTapTimeout());
                            break;

                        case 1:
                            mPlayPauseKeyTappedTime = 2;
                            handler.removeCallbacks(mPlayPauseKeyTimeoutRunnable);
                            handler.postDelayed(mPlayPauseKeyTimeoutRunnable,
                                    ViewConfiguration.getDoubleTapTimeout());
                            break;

                        case 2:
                            mPlayPauseKeyTappedTime = 0;
                            handler.removeCallbacks(mPlayPauseKeyTimeoutRunnable);

                            sendMsg(MSG_PLAY_PAUSE_KEY_TRIPLE_TAP);
                            break;
                    }
                }
                return true;
            default:
                // If another key is pressed within double tap timeout, consider the pending
                // play/pause as a single/double tap to handle media keys in order.
                handlePlayPauseKeySingleOrDoubleTapAsNeeded();
                break;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                sendMsg(MSG_MEDIA_PREVIOUS);
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                sendMsg(MSG_MEDIA_NEXT);
                return true;
        }
        return false;
    }

    private void handlePlayPauseKeySingleOrDoubleTapAsNeeded() {
        final int tappedTime = mPlayPauseKeyTappedTime;
        if (tappedTime == 0) return;

        mPlayPauseKeyTappedTime = 0;
        Consts.getMainThreadHandler().removeCallbacks(mPlayPauseKeyTimeoutRunnable);

        switch (tappedTime) {
            case 1:
                sendMsg(MSG_PLAY_PAUSE_KEY_SINGLE_TAP);
                break;
            case 2:
                sendMsg(MSG_PLAY_PAUSE_KEY_DOUBLE_TAP);
                break;
        }
    }

    protected final void sendMsg(int what) {
        Message msg = Message.obtain(null, what);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mMessenger, flags);
    }

    public static final Creator<MediaButtonEventHandler> CREATOR = new Creator<MediaButtonEventHandler>() {
        @Override
        public MediaButtonEventHandler createFromParcel(Parcel in) {
            return new MediaButtonEventHandler(in);
        }

        @Override
        public MediaButtonEventHandler[] newArray(int size) {
            return new MediaButtonEventHandler[size];
        }
    };
}
