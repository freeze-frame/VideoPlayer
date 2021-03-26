/*
 * Created on 2017/09/30.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

/**
 * @author 刘振林
 */
public class TimeUtil {
    private TimeUtil() {
    }

    public static String formatTimeByColon(int timeMs) {
        final int totalSeconds = timeMs / 1000;

        final int seconds = totalSeconds % 60;
        final int minutes = (totalSeconds / 60) % 60;
        final int hours = totalSeconds / 3600;

        if (hours == 0) {
            if (minutes >= 0 && minutes < 10) {
                if (seconds >= 0 && seconds < 10) {
                    return "0" + minutes + ":" + "0" + seconds;
                } else {
                    return "0" + minutes + ":" + seconds;
                }
            } else {
                if (seconds >= 0 && seconds < 10) {
                    return minutes + ":" + "0" + seconds;
                } else {
                    return minutes + ":" + seconds;
                }
            }
        } else if (hours > 0 && hours < 10) {
            if (minutes >= 0 && minutes < 10) {
                if (seconds >= 0 && seconds < 10) {
                    return "0" + hours + ":" + "0" + minutes + ":" + "0" + seconds;
                } else {
                    return "0" + hours + ":" + "0" + minutes + ":" + seconds;
                }
            } else {
                if (seconds >= 0 && seconds < 10) {
                    return "0" + hours + ":" + minutes + ":" + "0" + seconds;
                } else {
                    return "0" + hours + ":" + minutes + ":" + seconds;
                }
            }
        } else if (minutes >= 0 && minutes < 10) {
            if (seconds >= 0 && seconds < 10) {
                return hours + ":" + "0" + minutes + ":" + "0" + seconds;
            } else {
                return hours + ":" + "0" + minutes + ":" + seconds;
            }
        } else {
            if (seconds >= 0 && seconds < 10) {
                return hours + ":" + minutes + ":" + "0" + seconds;
            } else {
                return hours + ":" + minutes + ":" + seconds;
            }
        }
    }
}
