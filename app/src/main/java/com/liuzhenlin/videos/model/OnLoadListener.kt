/*
 * Created on 2020-6-18 8:42:51 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

/**
 * @author 刘振林
 */
interface OnLoadListener<Progress, Result> {
    fun onLoadStart() {}

    fun onLoadFinish(result: Result) {}

    fun onLoadCanceled(/*result: Result*/) {}

    fun onLoadError(cause: Throwable) {}

    fun onLoadingProgressUpdate(progress: Progress) {}
}
