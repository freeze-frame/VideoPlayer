package com.liuzhenlin.swipeback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

public class Utils {

    private Utils() {
    }

    /**
     * Convert a translucent themed Activity {@link android.R.attr#windowIsTranslucent} to
     * a fullscreen opaque Activity.
     * <p>
     * Call this whenever the background of a translucent Activity has changed to become opaque.
     * Doing so will allow the {@link android.view.Surface} of the Activity behind to be released.
     * <p>
     * This call has no effect on non-translucent activities or on activities
     * with the {@link android.R.attr#windowIsFloating} attribute.
     *
     * @see #convertActivityToTranslucent(Activity)
     */
    public static void convertActivityToOpaque(Activity activity) {
        try {
            //noinspection JavaReflectionMemberAccess
            Activity.class
                    .getMethod("convertFromTranslucent")
                    .invoke(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert a translucent themed Activity {@link android.R.attr#windowIsTranslucent} back from
     * opaque to translucent following a call to {@link #convertActivityToOpaque(Activity)}.
     * <p>
     * Calling this allows the Activity behind this one to be seen again...
     * <p>
     * This call has no effect on non-translucent activities or on activities with the
     * {@link android.R.attr#windowIsFloating} attribute.
     *
     * @see #convertActivityToTranslucentBeforeL(Activity)
     * @see #convertActivityToTranslucentSinceL(Activity)
     * @see #convertActivityToOpaque(Activity)
     */
    public static void convertActivityToTranslucent(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                convertActivityToTranslucentSinceL(activity);
            } else {
                convertActivityToTranslucentBeforeL(activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calling the method {@link #convertActivityToTranslucent(Activity)} on platforms
     * before Android 5.0
     */
    private static void convertActivityToTranslucentBeforeL(Activity activity) throws Exception {
        Class<?> translucentConversionListener = null;
        for (Class clazz : Activity.class.getClasses()) {
            if ("TranslucentConversionListener".equals(clazz.getSimpleName())) {
                translucentConversionListener = clazz;
                break;
            }
        }
        if (translucentConversionListener != null) {
            //noinspection JavaReflectionMemberAccess, JavaReflectionInvocation
            Activity.class
                    .getMethod("convertToTranslucent", translucentConversionListener)
                    .invoke(activity, (Object) null);
        }
    }

    /**
     * Calling the method {@link #convertActivityToTranslucent(Activity)} on platforms
     * since Android 5.0
     *
     * @since Android 5.0
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static void convertActivityToTranslucentSinceL(Activity activity) throws Exception {
        Class<?> translucentConversionListener = null;
        for (Class clazz : Activity.class.getClasses()) {
            if ("TranslucentConversionListener".equals(clazz.getSimpleName())) {
                translucentConversionListener = clazz;
                break;
            }
        }
        if (translucentConversionListener != null) {
            @SuppressLint("DiscouragedPrivateApi")
            Method getActivityOptions = Activity.class.getDeclaredMethod("getActivityOptions");
            getActivityOptions.setAccessible(true);

            //noinspection JavaReflectionInvocation
            Activity.class
                    .getMethod("convertToTranslucent",
                            translucentConversionListener, ActivityOptions.class)
                    .invoke(activity,
                            null, getActivityOptions.invoke(activity));
        }
    }
}
