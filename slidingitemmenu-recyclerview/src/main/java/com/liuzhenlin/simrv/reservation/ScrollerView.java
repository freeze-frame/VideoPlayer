/*
 * Created on 2017/10/21.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.simrv.reservation;

/**
 * @author 刘振林
 */
public interface ScrollerView {

    /**
     * Start scrolling by providing the distance to travel and the duration of the scroll.
     *
     * @param dx       Horizontal distance to travel. Positive numbers will scroll the
     *                 content to the left.
     * @param dy       Vertical distance to travel. Positive numbers will scroll the
     *                 content up.
     * @param duration Duration of the scroll in milliseconds.
     */
    void smoothScrollBy(int dx, int dy, int duration);

    /**
     * Smoothly move the scrolled position of your view to (<code>x</code>, <code>y</code>).
     *
     * @param x        the x position to scroll to
     * @param y        the y position to scroll to
     * @param duration duration of the scroll in milliseconds.
     */
    void smoothScrollTo(int x, int y, int duration);
}
