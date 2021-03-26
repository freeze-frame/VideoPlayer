/*
 * Created on 2019/3/15 8:51 PM.
 * Copyright © 2019–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos

import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.fragment.app.Fragment
import com.liuzhenlin.common.utils.FileUtils
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.bean.VideoDirectory
import com.liuzhenlin.videos.bean.VideoListItem
import com.liuzhenlin.videos.dao.IVideoDirectoryDao
import com.liuzhenlin.videos.dao.VideoListItemDao
import java.io.File
import java.util.*

/**
 * @author 刘振林
 */

public val Fragment.contextThemedFirst get() = activity ?: contextRequired
public val Fragment.contextRequired get() = context ?: App.getInstanceUnsafe()!!

private val sVideoListItemComparator = Comparator<VideoListItem> { item, item2 ->
    if (item is Video) {
        if (item2 !is Video) {
            return@Comparator 1
        }
    } else if (item2 is Video) {
        return@Comparator -1
    }

    val result = item.name.compareTo(item2.name, ignoreCase = true)
    if (result == 0) {
        item.path.compareTo(item2.path, ignoreCase = true)
    } else {
        result
    }
}

@JvmName("sortVideoListItemsByName")
fun <T : VideoListItem> List<T>?.sortByElementName() {
    Collections.sort(this ?: return, sVideoListItemComparator)
}

@JvmName("reorderedVideoListItems")
fun <T : VideoListItem> List<T>.reordered(): MutableList<T> {
    val items = toMutableList()
    items.sortByElementName()

    var toppedItems: MutableList<T>? = null
    for (item in items) {
        if (item.isTopped) {
            if (toppedItems == null) toppedItems = LinkedList()
            toppedItems.add(item)
        }
    }

    if (toppedItems != null) {
        items.removeAll(toppedItems)
        items.addAll(0, toppedItems)
    }

    return items
}

fun <T : VideoListItem> List<T>?.allEqual(other: List<T>?): Boolean {
    this ?: return other == null

    if (other == null) return size == 0

    if (other.size != size) return false

    for (i in indices) {
        if (!this[i].allEqual(other[i])) return false
    }

    return true
}

fun <T> MutableList<T>.set(src: List<T>?) {
    when {
        src == null -> clear()
        size == src.size -> for (i in indices) this[i] = src[i]
        else -> {
            clear()
            addAll(src)
        }
    }
}

fun <T : VideoListItem> MutableList<T>.deepCopy(src: List<T>) {
    if (size == src.size) {
        for (i in indices) this[i] = src[i].deepCopy()
    } else {
        clear()
        addAll(src.map { it.deepCopy() as T })
    }
}

fun IVideoDirectoryDao.insertVideoDir(directory: String): VideoDirectory {
    val videodir = VideoDirectory()
    videodir.name = FileUtils.getFileNameFromFilePath(directory)
    videodir.path = directory

    insertVideoDir(videodir)

    return videodir
}

fun Collection<Video>?.toVideoListItems(): MutableList<VideoListItem>? {
    this ?: return null
    if (size == 0) return null

    val videosMap = ArrayMap<String, Any>()
    for (video in this) {
        val path = video.path
        // Directory of the video file
        val videodir = path.substring(0, path.lastIndexOf(File.separatorChar))

        var videos: MutableList<Video>? = null
        for (k in videosMap.keys)
            if (k.equals(videodir, ignoreCase = true)) {
                @Suppress("UNCHECKED_CAST")
                videos = videosMap[k] as MutableList<Video>
                break
            }
        if (videos == null) {
            videos = mutableListOf()

            videosMap[videodir] = videos
        }
        videos.add(video)
    }
    var index = 0
    while (index < videosMap.size) {
        @Suppress("UNCHECKED_CAST")
        val videos = videosMap.valueAt(index) as MutableList<Video>
        if (videos.size == 1) {
            videosMap.setValueAt(index, videos[0])
        } else {
            val dao = VideoListItemDao.getSingleton(App.getInstanceUnsafe()!!)

            val dirPath = videosMap.keyAt(index)
            var videodir = dao.queryVideoDirByPath(dirPath)
            val videodirAlreadyExists = videodir != null
            if (!videodirAlreadyExists) {
                videodir = dao.insertVideoDir(dirPath)
            }
            videodir!!.size = videos.allVideoSize()
            videodir.videos =
                    if (videodirAlreadyExists && videos.any { it.isTopped }) {
                        videos.reordered()
                    } else {
                        if (!videodirAlreadyExists) {
                            for (video in videos)
                                if (video.isTopped) {
                                    video.isTopped = false
                                    dao.setVideoListItemTopped(video, false)
                                }
                        }
                        videos.sortByElementName()
                        videos
                    }

            videosMap.setValueAt(index, videodir)
        }
        index++
    }

    var items: MutableList<VideoListItem>? = null
    var toppedItems: MutableList<VideoListItem>? = null
    index = 0
    while (index < videosMap.size) {
        if (items == null) items = mutableListOf()

        val item = videosMap.valueAt(index) as VideoListItem
        if (item.isTopped) {
            if (toppedItems == null) toppedItems = LinkedList()
            toppedItems.add(item)
        } else {
            items.add(item)
        }

        index++
    }
    if (items != null) {
        items.sortByElementName()
        if (toppedItems != null) {
            toppedItems.sortByElementName()
            items.addAll(0, toppedItems)
        }
    }

    return items
}

fun Collection<Video>?.allVideoSize(): Long {
    var size = 0L
    for (video in this ?: return size) {
        size += video.size
    }
    return size
}

/**
 * Returns the view at [index].
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
operator fun ViewGroup.get(index: Int) =
        getChildAt(index) ?: throw IndexOutOfBoundsException("Index: $index, Size: $childCount")
