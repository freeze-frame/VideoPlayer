/*
 * Created on 2020-6-18 8:34:30 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.bean.VideoListItem
import com.liuzhenlin.videos.dao.VideoListItemDao
import com.liuzhenlin.videos.deepCopy
import com.liuzhenlin.videos.sortByElementName
import com.liuzhenlin.videos.toVideoListItems
import com.liuzhenlin.videos.utils.Executors
import java.util.*

/**
 * @author 刘振林
 */
class LocalVideoListModel(context: Context) : BaseModel<Nothing, MutableList<VideoListItem>?>(context) {

    private var mOnReloadVideosListeners: MutableList<OnReloadVideosListener>? = null

    fun addOnReloadVideosListener(listener: OnReloadVideosListener) {
        if (mOnReloadVideosListeners == null)
            mOnReloadVideosListeners = mutableListOf()
        if (!mOnReloadVideosListeners!!.contains(listener))
            mOnReloadVideosListeners!!.add(listener)
    }

    fun removeOnReloadVideosListener(listener: OnReloadVideosListener) =
            mOnReloadVideosListeners?.remove(listener)

    private fun notifyListenersOnReloadVideos(videos: ArrayList<Video>?) =
            mOnReloadVideosListeners?.let {
                if (it.isEmpty()) return@let

                for (i in it.size - 1 downTo 0) {
                    @Suppress("UNCHECKED_CAST")
                    val copy = videos?.clone() as? MutableList<Video>
                    copy?.deepCopy(videos)
                    it[i].onReloadVideos(copy)
                }
            }

    override fun createAndStartLoader(): AsyncTask<*, *, *> {
        val loader = LoadVideosTask()
        loader.executeOnExecutor(Executors.THREAD_POOL_EXECUTOR)
        return loader
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadVideosTask : AsyncTask<Void, Void, Array<*>?>() {

        override fun onPreExecute() = onLoadStart()

        override fun doInBackground(vararg voids: Void): Array<*>? {
            val dao = VideoListItemDao.getSingleton(mContext)

            var videos: MutableList<Video>? = null

            val videoCursor = dao.queryAllVideos() ?: return null
            while (!isCancelled && videoCursor.moveToNext()) {
                val video = dao.buildVideo(videoCursor)
                if (video != null) {
                    if (videos == null) videos = LinkedList()
                    videos.add(video)
                }
            }
            videoCursor.close()

            videos.sortByElementName()
            val items = videos.toVideoListItems() ?: return null

            val videodirCursor = dao.queryAllVideoDirs()
            if (videodirCursor != null) {
                while (!isCancelled && videodirCursor.moveToNext()) {
                    val videodir = dao.buildVideoDir(videodirCursor)
                    if (!items.contains(videodir)) {
                        dao.deleteVideoDir(videodir.path)
                    }
                }
                videodirCursor.close()
            }

            return arrayOf(items, arrayListOf(*videos!!.toTypedArray()))
        }

        override fun onPostExecute(result: Array<*>?) {
            @Suppress("UNCHECKED_CAST")
            onLoadFinish(result?.get(0) as MutableList<VideoListItem>?)

            @Suppress("UNCHECKED_CAST")
            notifyListenersOnReloadVideos(result?.get(1) as ArrayList<Video>?)
        }
    }
}