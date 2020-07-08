/*
 * Created on 2020-6-18 8:42:51 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

/**
 * @author 刘振林
 */
interface OnLoadListener<R> {
    fun onLoadStart() {}

    fun onLoadFinish(result: R) {}

    fun onLoadCanceled(/*result: R*/) {}

    fun onLoadError(cause: Throwable) {}
}
