/*
 * Created on 2020-6-19 12:16:48 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask

/**
 * @author 刘振林
 */
abstract class BaseModel<Result>(context: Context) {

    protected val mContext: Context = context.applicationContext
    protected var mOnLoadListeners: MutableList<OnLoadListener<Result>>? = null

    fun addOnLoadListener(listener: OnLoadListener<Result>) {
        if (mOnLoadListeners == null)
            mOnLoadListeners = mutableListOf()
        if (!mOnLoadListeners!!.contains(listener))
            mOnLoadListeners!!.add(listener)
    }

    fun removeOnLoadListener(listener: OnLoadListener<Result>) =
            mOnLoadListeners?.remove(listener)

    abstract fun startLoader()
    abstract fun stopLoader()

    @SuppressLint("StaticFieldLeak")
    protected abstract inner class Loader<Params, Progress> : AsyncTask<Params, Progress, Result>() {
        override fun onPreExecute() {
            mOnLoadListeners?.let {
                for (listener in it.toTypedArray()) {
                    listener.onLoadStart()
                }
            }
        }

        override fun onPostExecute(result: Result) {
            mOnLoadListeners?.let {
                for (listener in it.toTypedArray()) {
                    listener.onLoadFinish(result)
                }
            }
        }

        override fun onCancelled(result: Result) {
            mOnLoadListeners?.let {
                for (listener in it.toTypedArray()) {
                    listener.onLoadCanceled(result)
                }
            }
        }
    }
}