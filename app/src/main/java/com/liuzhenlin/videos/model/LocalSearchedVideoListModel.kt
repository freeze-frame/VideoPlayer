/*
 * Created on 2020-6-18 8:35:03 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

import android.annotation.SuppressLint
import android.content.Context
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.dao.VideoListItemDao
import com.liuzhenlin.videos.sortByElementName

/**
 * @author 刘振林
 */
class LocalSearchedVideoListModel(context: Context) : BaseModel<MutableList<Video>?>(context) {

    private var mLoader: LoadVideosTask? = null

    override fun startLoader() {
        // 不在加载视频时才加载
        if (mLoader == null) {
            mLoader = LoadVideosTask()
            mLoader!!.executeOnExecutor(ParallelThreadExecutor.getSingleton())
        }
    }

    override fun stopLoader() {
        val loader = mLoader
        if (loader != null) {
            mLoader = null
            loader.cancel(false)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadVideosTask : Loader<Void, Void>() {
        override fun doInBackground(vararg voids: Void): MutableList<Video>? {
            val dao = VideoListItemDao.getSingleton(mContext)

            var videos: MutableList<Video>? = null

            val videoCursor = dao.queryAllVideos() ?: return null
            while (!isCancelled && videoCursor.moveToNext()) {
                val video = dao.buildVideo(videoCursor)
                if (video != null) {
                    if (videos == null) videos = mutableListOf()
                    videos.add(video)
                }
            }
            videoCursor.close()

            videos.sortByElementName()
            return videos
        }

        override fun onPostExecute(result: MutableList<Video>?) {
            mLoader = null
            super.onPostExecute(result)
        }

        override fun onCancelled(result: MutableList<Video>?) {
            if (mLoader == null) {
                super.onCancelled(result)
            }
        }
    }
}