package com.liuzhenlin.swipeback;

import androidx.annotation.Nullable;

public interface ISwipeBackFragment {

    /**
     * @return the instance of {@link SwipeBackLayout} of this Fragment
     */
    SwipeBackLayout getSwipeBackLayout();

    /**
     * @return whether popping up this Fragment from the back stack that holds it through
     *         swipe-back gesture is enabled or not.
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
     * @return whether the transition animation of this Fragment is enabled or not
     */
    boolean isTransitionEnabled();

    /**
     * Enables the transition animation of this Fragment or not.
     * <p>
     * <strong>NOTE:</strong> Both the transitions of the previous and the current Fragments
     * should be disabled while the current one is being popped up and after that is done,
     * you should enable the transition of the previous.
     */
    void setTransitionEnabled(boolean enabled);

    /**
     * @return the previous Fragment {@link ISwipeBackFragment} instance
     */
    @Nullable
    ISwipeBackFragment getPreviousFragment();
}
