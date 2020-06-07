package com.liuzhenlin.swipeback;

import android.app.Activity;

import androidx.annotation.Nullable;

public interface ISwipeBackActivity {

    /**
     * @return the instance of {@link SwipeBackLayout} of this Activity
     */
    SwipeBackLayout getSwipeBackLayout();

    /**
     * @return whether finishing this Activity through swipe-back gesture is enabled or not
     * @see #setSwipeBackEnabled(boolean)
     */
    boolean isSwipeBackEnabled();

    /**
     * Enables swipe-back gesture or not
     *
     * @see #isSwipeBackEnabled()
     */
    void setSwipeBackEnabled(boolean enabled);

    /**
     * @return whether this Activity can be finished through user's swipe-back gesture.
     * <p>
     * By default, if the count of Fragments in this Activity is less than or equal to 1, the
     * Activity will get the priority of being slid out and be finished, rather than its Fragment.
     */
    boolean canSwipeBackToFinish();

    /**
     * @return the previous Activity instance.
     */
    @Nullable
    Activity getPreviousActivity();
}
