package com.liuzhenlin.texturevideoview.utils;

import android.os.Build;
import android.transition.Transition;

import androidx.annotation.RequiresApi;

/**
 * This adapter class provides empty implementations of the methods from {@link
 * android.transition.Transition.TransitionListener}.
 * Any custom listener that cares only about a subset of the methods of this listener can
 * simply subclass this adapter class instead of implementing the interface directly.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
public interface TransitionListenerAdapter extends Transition.TransitionListener {

    @Override
    default void onTransitionStart(Transition transition) {
    }

    @Override
    default void onTransitionEnd(Transition transition) {
    }

    @Override
    default void onTransitionCancel(Transition transition) {
    }

    @Override
    default void onTransitionPause(Transition transition) {
    }

    @Override
    default void onTransitionResume(Transition transition) {
    }
}
