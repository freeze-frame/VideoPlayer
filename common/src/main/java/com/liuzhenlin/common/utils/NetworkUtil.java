/*
 * Created on 2018/04/14.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

/**
 * @author 刘振林
 */
public class NetworkUtil {
    private NetworkUtil() {
    }

    /**
     * @return 当前网络是否可用
     */
    public static boolean isNetworkConnected(@NonNull Context context) {
        // 获取手机所有连接管理对象（包括对WIFI，移动数据网络等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            //noinspection deprecation
            NetworkInfo mobileNetInfo = connectivityManager.getActiveNetworkInfo();
            //noinspection deprecation
            return mobileNetInfo != null && mobileNetInfo.isConnected();
        }
        return false;
    }
}
