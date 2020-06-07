/*
 * Created on 2019/12/29 11:30 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 刘振林
 */
public class ActivityUtils {
    private ActivityUtils() {
    }

    /**
     * 获取应用处于前台且为活跃状态（未被暂停）的Activity实例
     */
    @Nullable
    public static Activity getActiveActivity() {
        try {
            Map activities = getActivities();
            if (activities == null) {
                return null;
            }

            // public static final class ActivityClientRecord {...}
            Class activityRecordClass = null;
            // /*package*/ boolean paused;
            Field pausedField = null;
            for (Object activityRecord : activities.values()) {
                if (activityRecordClass == null) {
                    activityRecordClass = activityRecord.getClass();

                    pausedField = activityRecordClass.getDeclaredField("paused");
                    pausedField.setAccessible(true);
                }
                if (!pausedField.getBoolean(activityRecord)) {
                    // /*package*/ Activity activity;
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取一个指定类名的Activity实例
     */
    @Nullable
    public static Activity getActivityForName(@Nullable String activityClsName) {
        if (TextUtils.isEmpty(activityClsName)) return null;

        try {
            Map activities = getActivities();
            if (activities == null) {
                return null;
            }

            // /*package*/ Activity activity;
            Field activityField = null;
            for (Object activityRecord : activities.values()) {
                if (activityField == null) {
                    // public static final class ActivityClientRecord {...}
                    Class activityRecordClass = activityRecord.getClass();

                    activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                }
                Activity activity = (Activity) activityField.get(activityRecord);
                if (activity.getClass().getName().equals(activityClsName)) {
                    return activity;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定类名的所有Activity实例
     */
    @Nullable
    public static List<Activity> getActivitiesForName(@Nullable String activityClsName) {
        if (TextUtils.isEmpty(activityClsName)) return null;

        try {
            Map activities = getActivities();
            if (activities == null) {
                return null;
            }

            List<Activity> result = null;
            // /*package*/ Activity activity;
            Field activityField = null;
            for (Object activityRecord : activities.values()) {
                if (activityField == null) {
                    // public static final class ActivityClientRecord {...}
                    Class activityRecordClass = activityRecord.getClass();

                    activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                }
                Activity activity = (Activity) activityField.get(activityRecord);
                if (activity.getClass().getName().equals(activityClsName)) {
                    if (result == null) {
                        result = new ArrayList<>(1);
                    }
                    result.add(activity);
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map/*<IBinder, ActivityClientRecord>*/ getActivities() {
        try {
            // public final class ActivityThread...
            @SuppressLint("PrivateApi")
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

            // public static ActivityThread currentActivityThread() {...}
            Object currentActivityThread = activityThreadClass
                    .getMethod("currentActivityThread")
                    .invoke(activityThreadClass);

            // /*package*/ final ArrayMap<IBinder, ActivityClientRecord> mActivities
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            return (Map) activitiesField.get(currentActivityThread);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
