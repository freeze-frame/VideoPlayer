/*
 * Created on 2019/5/6 2:55 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Util;
import com.liuzhenlin.common.utils.Utils;

import java.util.List;

/**
 * @author 刘振林
 */
public abstract class AbsTextureVideoView extends DrawerLayout {

    protected final Context mContext;
    protected final Resources mResources;

    /*package*/ final String mAppName;
    /**
     * A user agent string based on the application name resolved from this view's context object
     * and the `exoplayer-core` library version.
     */
    /*package*/ final String mExoUserAgent;

    public AbsTextureVideoView(@NonNull Context context) {
        this(context, null);
    }

    public AbsTextureVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsTextureVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mResources = getResources();

        mAppName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
        mExoUserAgent = Util.getUserAgent(context, mAppName);
    }

    /**
     * Shows a subtitle on the video content.
     *
     * @param text       The characters as a CharSequence object in the subtitle.
     *                   Passing null to render nothing so as to clear the previous shown text.
     * @param textBounds The rectangle area or region to render the characters in the subtitle.
     *                   Maybe null to render the text at the center bottom of the video.
     */
    public abstract void showSubtitle(@Nullable CharSequence text, @Nullable Rect textBounds);

    /**
     * Shows some subtitles on the video content.
     *
     * @param cues The {@link Cue}s to display, or null to clear the showing Cues.
     */
    public abstract void showSubtitles(@Nullable List<Cue> cues);

    /**
     * If the video seek bar is being dragged, calling this method will cause
     * the video preview thumbnail displayed in the center top of this view to be hided.
     *
     * @param seekPlaybackPosition true to additionally move the current playback position
     *                             to the position where the video seek bar is dragged,
     *                             false to leave it out.
     */
    public abstract void cancelDraggingVideoSeekBar(boolean seekPlaybackPosition);

    /** @return true if we can skip the video played to the previous one */
    public abstract boolean canSkipToPrevious();

    /** @return true if we can skip the video played to the next one */
    public abstract boolean canSkipToNext();

    /**
     * @return whether or not this view is in the foreground
     */
    /*package*/ boolean isInForeground() {
        return getWindowVisibility() == VISIBLE;
    }

    /**
     * @return the {@link Surface} onto which video will be rendered.
     */
    /*package*/ abstract @Nullable Surface getSurface();

    /*package*/ abstract void onVideoUriChanged(@Nullable Uri uri);

    /*package*/ abstract void onVideoDurationChanged(int duration);

    /*package*/ abstract void onVideoSizeChanged(int width, int height);

    /*package*/ abstract void onVideoStarted();

    /*package*/ abstract void onVideoStopped();

    /*package*/ abstract void onVideoRepeat();

    /*package*/ abstract void onVideoBufferingStateChanged(boolean buffering);

    /*package*/ abstract void onPlaybackSpeedChanged(float speed);

    /*package*/ abstract void onAudioAllowedToPlayInBackgroundChanged(boolean allowed);

    /*package*/ abstract void onSingleVideoLoopPlaybackModeChanged(boolean looping);

    /*package*/ abstract boolean willTurnOffWhenThisEpisodeEnds();

    /*package*/ abstract void onVideoTurnedOffWhenTheEpisodeEnds();

    @Override
    public int getDrawerLockMode(@NonNull View drawerView) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        checkDrawerView(drawerView, Utils.getAbsoluteHorizontalGravity(this, lp.gravity));
        return getDrawerLockModeInternal(drawerView);
    }

    /*package*/ final int getDrawerLockModeInternal(@NonNull View drawerView) {
        return getDrawerLockMode(
                ((LayoutParams) drawerView.getLayoutParams()).gravity
                        & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK);
    }

    @Override
    public void setDrawerLockMode(int lockMode, @NonNull View drawerView) {
        LayoutParams lp = (LayoutParams) drawerView.getLayoutParams();
        checkDrawerView(drawerView, Utils.getAbsoluteHorizontalGravity(this, lp.gravity));
        setDrawerLockModeInternal(lockMode, drawerView);
    }

    /*package*/ final void setDrawerLockModeInternal(int lockMode, @NonNull View drawerView) {
        setDrawerLockMode(lockMode,
                ((LayoutParams) drawerView.getLayoutParams()).gravity
                        & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK);
    }

    @SuppressLint("RtlHardcoded")
    private void checkDrawerView(View drawerView, int absHG) {
        if ((absHG & Gravity.LEFT) != Gravity.LEFT && (absHG & Gravity.RIGHT) != Gravity.RIGHT) {
            throw new IllegalArgumentException(
                    "View " + drawerView + " is not a drawer with appropriate layout_gravity");
        }
    }
}
