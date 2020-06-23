/*
 * Created on 2020-6-18 8:34:50 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

import android.annotation.SuppressLint
import android.content.Context
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.bean.VideoDirectory
import com.liuzhenlin.videos.dao.VideoListItemDao
import com.liuzhenlin.videos.reordered
import java.util.*

/**
 * @author 刘振林
 */
class LocalFoldedVideoListModel(private val videodir: VideoDirectory, context: Context)
    : BaseModel<MutableList<Video>?>(context) {

    private var mLoader: LoadDirectoryVideosTask? = null

    override fun startLoader() {
        // 不在加载视频时才加载
        if (mLoader == null) {
            mLoader = LoadDirectoryVideosTask()
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
    private inner class LoadDirectoryVideosTask : Loader<Void, Void>() {
        override fun doInBackground(vararg params: Void?): MutableList<Video>? {
            val dao = VideoListItemDao.getSingleton(mContext)

            var videos: MutableList<Video>? = null

            val videoCursor = dao.queryAllVideosInDirectory(videodir.path) ?: return null
            while (!isCancelled && videoCursor.moveToNext()) {
                val video = dao.buildVideo(videoCursor) ?: continue
                if (videos == null)
                    videos = LinkedList()
                videos.add(video)
            }
            videoCursor.close()

            return videos?.reordered()
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