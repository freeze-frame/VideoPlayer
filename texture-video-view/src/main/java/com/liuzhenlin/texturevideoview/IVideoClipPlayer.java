/*
 * Created on 2020-3-6 11:50:51 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

/*package*/ interface IVideoClipPlayer {
    boolean isPlaying();
    void play();
    void pause();

    void seekTo(int positionMs);

    int getCurrentPosition();
    int getDuration();

    void create();
    void release();
}