/*
 * Created on 2019/12/3 3:41 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.service;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.util.Synthetic;
import com.liuzhenlin.texturevideoview.InternalConsts;
import com.liuzhenlin.texturevideoview.R;
import com.liuzhenlin.texturevideoview.compat.RemoteViewsCompat;
import com.liuzhenlin.texturevideoview.notification.NotificationChannelManager;
import com.liuzhenlin.texturevideoview.notification.style.DecoratedMediaCustomViewStyle;
import com.liuzhenlin.texturevideoview.utils.BitmapUtils;
import com.liuzhenlin.texturevideoview.utils.ThemeUtils;

/**
 * @author 刘振林
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BackgroundPlaybackControllerService extends Service {

    private String mPlay;
    private String mPause;
    private String mPkgName;
    @Synthetic Bitmap mDefThumb;
    private int mThumbMaxWidth;
    private int mThumbMaxHeight;
    private int mNotificationActionIconTint;

    private Messenger mMessenger;

    @Synthetic String mMediaTitle;
    @Synthetic Bitmap mVideoThumb;
    @Synthetic boolean mIsPlaying;
    @Synthetic boolean mIsBuffering;
    @Synthetic boolean mCanSkipToPrevious;
    @Synthetic boolean mCanSkipToNext;
    @Synthetic long mMediaProgress;
    @Synthetic long mMediaDuration;

    @Synthetic NotificationManager mNotificationManager;
    @Synthetic NotificationCompat.Builder mNotificationBuilder;
    private static final int ID_NOTIFICATION = 20191203;

    private static final String EXTRA_CONTROLLER_ACTION = "extra_controllerAction";

    private static final int CONTROLLER_ACTION_PLAY = 1;
    private static final int CONTROLLER_ACTION_PAUSE = 2;
    private static final int CONTROLLER_ACTION_SKIP_TO_PREVIOUS = 3;
    private static final int CONTROLLER_ACTION_SKIP_TO_NEXT = 4;
    private static final int CONTROLLER_ACTION_CLOSE = 5;

    private static final int REQUEST_PLAY = 1;
    private static final int REQUEST_PAUSE = 2;
    private static final int REQUEST_SKIP_TO_PREVIOUS = 3;
    private static final int REQUEST_SKIP_TO_NEXT = 4;
    private static final int REQUEST_CLOSE = 5;

    public static final int MSG_PLAY = Integer.MAX_VALUE;
    public static final int MSG_PAUSE = Integer.MAX_VALUE - 1;
    public static final int MSG_SKIP_TO_PREVIOUS = Integer.MAX_VALUE - 2;
    public static final int MSG_SKIP_TO_NEXT = Integer.MAX_VALUE - 3;
    public static final int MSG_CLOSE = Integer.MAX_VALUE - 4;

    private final ControllerActionReceiver mReceiver = new ControllerActionReceiver();

    @Synthetic boolean mIsForeground;

    private final Target<Bitmap> mGlideTarget = new CustomTarget<Bitmap>() {
        @Override
        public void onResourceReady(@NonNull Bitmap icon, @Nullable Transition<? super Bitmap> transition) {
            mVideoThumb = icon;
            postNotificationIfForeground();
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder) {
            mVideoThumb = mDefThumb;
            postNotificationIfForeground();
        }
    };

    private final Runnable mPostNotificationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsForeground) {
                resetNotificationView();
                mNotificationManager.notify(ID_NOTIFICATION, mNotificationBuilder.build());
            }
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Resources res = getResources();
        mPlay = res.getString(R.string.play);
        mPause = res.getString(R.string.pause);
        mPkgName = getPackageName();
        //noinspection ConstantConditions
        mDefThumb = BitmapUtils.drawableToBitmap(
                ContextCompat.getDrawable(this, R.drawable.ic_default_thumb));
        mThumbMaxWidth = res.getDimensionPixelSize(R.dimen.notification_thumb_max_width);
        mThumbMaxHeight = res.getDimensionPixelSize(R.dimen.notification_thumb_max_height);
        mNotificationActionIconTint = getNotificationActionIconTint();
    }

    @Override
    public void onCreate() {
        String channelId = NotificationChannelManager.getPlaybackControlNotificationChannelId(this);
        mNotificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_media_app_notification)
                .setStyle(new DecoratedMediaCustomViewStyle())
                .setDefaults(0)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = intent.getParcelableExtra(InternalConsts.EXTRA_MESSENGER);
        //noinspection unchecked
        Class<Activity> playbackActivityClass = (Class<Activity>)
                intent.getSerializableExtra(InternalConsts.EXTRA_PLAYBACK_ACTIVITY_CLASS);
        Uri mediaUri = intent.getParcelableExtra(InternalConsts.EXTRA_MEDIA_URI);
        mMediaTitle = intent.getStringExtra(InternalConsts.EXTRA_MEDIA_TITLE);
        mIsPlaying = intent.getBooleanExtra(InternalConsts.EXTRA_IS_PLAYING, false);
        mIsBuffering = intent.getBooleanExtra(InternalConsts.EXTRA_IS_BUFFERING, false);
        mCanSkipToPrevious = intent.getBooleanExtra(InternalConsts.EXTRA_CAN_SKIP_TO_PREVIOUS, false);
        mCanSkipToNext = intent.getBooleanExtra(InternalConsts.EXTRA_CAN_SKIP_TO_NEXT, false);
        mMediaProgress = intent.getLongExtra(InternalConsts.EXTRA_MEDIA_PROGRESS, 0L);
        mMediaDuration = intent.getLongExtra(InternalConsts.EXTRA_MEDIA_DURATION, 0L);

        mNotificationBuilder.setTicker(mMediaTitle).setContentIntent(null);
        if (playbackActivityClass != null) {
            Intent it = new Intent(this, playbackActivityClass)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, it, 0));
        }
        loadMediaThumb(mediaUri);
        resetNotificationView();
        startForeground(ID_NOTIFICATION, mNotificationBuilder.build());
        mIsForeground = true;

        registerReceiver(mReceiver, new IntentFilter(ControllerActionReceiver.ACTION));

        return new Proxy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unregisterReceiver(mReceiver);
        stopForeground(true);
        mIsForeground = false;
        Glide.with(this).clear(mGlideTarget);
        return false;
    }

    @Synthetic void loadMediaThumb(Uri mediaUri) {
        RequestManager rm = Glide.with(this);
        if (mediaUri == null) {
            rm.clear(mGlideTarget);
        } else {
            rm
                    .asBitmap()
                    .load(mediaUri.toString())
                    .override(mThumbMaxWidth, mThumbMaxHeight)
                    .fitCenter()
                    .into(mGlideTarget);
        }
    }

    @Synthetic void postNotificationIfForeground() {
        Handler handler = InternalConsts.getMainThreadHandler();
        handler.removeCallbacks(mPostNotificationRunnable);
        if (mIsForeground) {
            // Use the Handler to post a delayed notification and remove any previously unpublished
            // notifications that exist in the message queue, of which the main purpose is
            // to spare no effort in posting notifications one at a time in serial order so as to
            // notably reduce situations where the controller and the player state are inconsistent.
            handler.postDelayed(mPostNotificationRunnable, 100);
        }
    }

    @Synthetic void resetNotificationView() {
        RemoteViews nv = createNotificationView();
        mNotificationBuilder.setCustomContentView(nv);
        mNotificationBuilder.setCustomBigContentView(nv);
    }

    private RemoteViews createNotificationView() {
        final boolean playing = mIsPlaying && !mIsBuffering;
        final int tint = mNotificationActionIconTint;

        RemoteViews nv = new RemoteViews(
                mPkgName, R.layout.notification_background_playback_controller);

        nv.setImageViewBitmap(R.id.image_videoThumb, mVideoThumb == null ? mDefThumb : mVideoThumb);

        nv.setTextViewText(R.id.text_mediaTitle, mMediaTitle);

        RemoteViewsCompat.setImageViewResourceWithTint(this, nv,
                R.id.btn_toggle,
                playing ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_white_24dp,
                tint);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            nv.setContentDescription(R.id.btn_toggle, playing ? mPause : mPlay);
        }
        nv.setOnClickPendingIntent(R.id.btn_toggle,
                createNotificationActionIntent(
                        playing ? CONTROLLER_ACTION_PAUSE : CONTROLLER_ACTION_PLAY,
                        playing ? REQUEST_PAUSE : REQUEST_PLAY));

        nv.setViewVisibility(R.id.btn_skipPrevious, mCanSkipToPrevious ? View.VISIBLE : View.GONE);
        if (mCanSkipToPrevious) {
            RemoteViewsCompat.setImageViewResourceWithTint(this, nv,
                    R.id.btn_skipPrevious, R.drawable.ic_skip_previous_white_24dp, tint);
            nv.setOnClickPendingIntent(R.id.btn_skipPrevious,
                    createNotificationActionIntent(
                            CONTROLLER_ACTION_SKIP_TO_PREVIOUS, REQUEST_SKIP_TO_PREVIOUS));
        }

        nv.setViewVisibility(R.id.btn_skipNext, mCanSkipToNext ? View.VISIBLE : View.GONE);
        if (mCanSkipToNext) {
            RemoteViewsCompat.setImageViewResourceWithTint(this, nv,
                    R.id.btn_skipNext, R.drawable.ic_skip_next_white_24dp, tint);
            nv.setOnClickPendingIntent(R.id.btn_skipNext,
                    createNotificationActionIntent(
                            CONTROLLER_ACTION_SKIP_TO_NEXT, REQUEST_SKIP_TO_NEXT));
        }

        RemoteViewsCompat.setImageViewResourceWithTint(this, nv,
                R.id.btn_close, R.drawable.ic_close_white_20dp, tint);
        nv.setOnClickPendingIntent(R.id.btn_close,
                createNotificationActionIntent(CONTROLLER_ACTION_CLOSE, REQUEST_CLOSE));

        // Chronometer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final long remaining = mMediaDuration - mMediaProgress;
            if (remaining > 0) {
                nv.setLong(R.id.countdownChronometer,
                        "setBase", SystemClock.elapsedRealtime() + remaining);
                nv.setBoolean(R.id.countdownChronometer, "setStarted", playing);
            }
        }

        return nv;
    }

    /**
     * Gets the notification action icon tint relying on the current theme. Do NOT cache statically!
     */
    private int getNotificationActionIconTint() {
        return ThemeUtils.getTextAppearanceDefaultTextColor(
                this, R.style.TextAppearance_Compat_Notification_Media);
    }

    private PendingIntent createNotificationActionIntent(int action, int requestCode) {
        return PendingIntent.getBroadcast(
                this,
                requestCode,
                new Intent(ControllerActionReceiver.ACTION)
                        .putExtra(EXTRA_CONTROLLER_ACTION, action),
                0);
    }

    @Synthetic void sendMsg(int what) {
        Message msg = Message.obtain(null, what);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public class Proxy extends Binder {

        public void onMediaTitleChange(@Nullable String title) {
            mMediaTitle = title;
            postNotificationIfForeground();
        }

        public void onMediaUriChange(@Nullable Uri uri) {
            mMediaProgress = 0L;
            mMediaDuration = 0L;
            loadMediaThumb(uri);
        }

        public void onMediaDurationChanged(long progress, long duration) {
            mMediaProgress = progress;
            mMediaDuration = duration;
            postNotificationIfForeground();
        }

        public void onMediaPlay(long progress) {
            mIsPlaying = true;
            mMediaProgress = progress;
            postNotificationIfForeground();
        }

        public void onMediaPause(long progress) {
            mIsPlaying = false;
            mMediaProgress = progress;
            postNotificationIfForeground();
        }

        public void onMediaRepeat() {
            mMediaProgress = 0;
            postNotificationIfForeground();
        }

        public void onMediaBufferingStateChanged(boolean buffering, long positionMs) {
            mIsBuffering = buffering;
            mMediaProgress = positionMs;
            postNotificationIfForeground();
        }

        public void onCanSkipToPreviousChange(boolean canSkipToPrevious) {
            mCanSkipToPrevious = canSkipToPrevious;
            postNotificationIfForeground();
        }

        public void onCanSkipToNextChange(boolean canSkipToNext) {
            mCanSkipToNext = canSkipToNext;
            postNotificationIfForeground();
        }
    }

    private final class ControllerActionReceiver extends BroadcastReceiver {
        static final String ACTION =
                "action_BackgroundPlaybackControllerService$ControllerActionReceiver";

        ControllerActionReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final int action = intent.getIntExtra(EXTRA_CONTROLLER_ACTION, 0);
            switch (action) {
                case CONTROLLER_ACTION_PLAY:
                    sendMsg(MSG_PLAY);
                    break;
                case CONTROLLER_ACTION_PAUSE:
                    sendMsg(MSG_PAUSE);
                    break;
                case CONTROLLER_ACTION_SKIP_TO_PREVIOUS:
                    sendMsg(MSG_SKIP_TO_PREVIOUS);
                    break;
                case CONTROLLER_ACTION_SKIP_TO_NEXT:
                    sendMsg(MSG_SKIP_TO_NEXT);
                    break;
                case CONTROLLER_ACTION_CLOSE:
                    sendMsg(MSG_CLOSE);
                    break;
            }
        }
    }
}