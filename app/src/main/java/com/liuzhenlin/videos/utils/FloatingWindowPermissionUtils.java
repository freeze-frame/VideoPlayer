/*
 * Created on 2019/11/8 2:46 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.liuzhenlin.videos.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 刘振林
 */
public class FloatingWindowPermissionUtils {
    private FloatingWindowPermissionUtils() {
    }

    public static boolean hasPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return checkOp(context, /* OP_SYSTEM_ALERT_WINDOW */24);
        }
        return true;
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private static boolean checkOp(Context context, int op) {
        AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        try {
            Class<AppOpsManager> clazz = AppOpsManager.class;
            //noinspection JavaReflectionMemberAccess
            Method method = clazz.getMethod("checkOp", int.class, int.class, String.class);
            return (int) method.invoke(aom, op, Binder.getCallingUid(), context.getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void applyForPermission(@NonNull final Activity activity, final int requestCode) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        applyForPermission$(activity, requestCode);

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            }
        };
        new AlertDialog.Builder(activity, R.style.DialogStyle_MinWidth_NoTitle)
                .setMessage(R.string.rationale_askFloatingWindowPermission)
                .setNegativeButton(R.string.notOpenNow, listener)
                .setPositiveButton(R.string.goNow, listener)
                .show();
    }

    private static void applyForPermission$(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.startActivityForResult(
                    createManageOverlayPermissionActionIntent(activity), requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.startActivityForResult(
                    createApplicationDetailsSettingsActionIntent(activity), requestCode);
        }
    }

    public static void applyForPermission(@NonNull final Fragment fragment, final int requestCode) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        applyForPermission$(fragment, requestCode);

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.cancel();
                        break;
                }
            }
        };
        new AlertDialog.Builder(obtainFragmentContext(fragment), R.style.DialogStyle_MinWidth_NoTitle)
                .setMessage(R.string.rationale_askFloatingWindowPermission)
                .setNegativeButton(R.string.notOpenNow, listener)
                .setPositiveButton(R.string.goNow, listener)
                .show();
    }

    private static void applyForPermission$(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.startActivityForResult(
                    createManageOverlayPermissionActionIntent(obtainFragmentContext(fragment)),
                    requestCode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            fragment.startActivityForResult(
                    createApplicationDetailsSettingsActionIntent(obtainFragmentContext(fragment)),
                    requestCode);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static Intent createManageOverlayPermissionActionIntent(Context context) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:" + context.getPackageName()));
    }

    private static Intent createApplicationDetailsSettingsActionIntent(Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + context.getPackageName()));
    }

    private static Context obtainFragmentContext(Fragment fragment) {
        Context context = fragment.getActivity();
        if (context == null) {
            context = fragment.requireContext();
        }
        return context;
    }
}
