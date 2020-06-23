package com.liuzhenlin.videos.bean

import android.os.Parcel
import android.os.Parcelable
import com.liuzhenlin.videos.deepCopy

/**
 * @author 刘振林
 */

abstract class VideoListItem(open var name: String,
                             open var path: String,
                             open var size: Long,
                             open var isTopped: Boolean) : Parcelable {
    var isChecked = false

    abstract fun allEqual(other: Any?): Boolean

    abstract fun <T : VideoListItem> deepCopy(): T
}

data class VideoDirectory(override var name: String = "",
                          override var path: String = "",
                          override var size: Long = 0L,
                          override var isTopped: Boolean = false,
                          var videos: MutableList<Video> = mutableListOf())
    : VideoListItem(name, path, size, isTopped) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoDirectory

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

    override fun allEqual(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoDirectory

        if (!(name == other.name && path == other.path && size == other.size && isTopped == other.isTopped)) return false
        if (videos.size != other.videos.size) return false
        for (i in videos.indices) {
            if (!videos[i].allEqual(other.videos[i])) return false
        }

        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : VideoListItem> deepCopy(): T =
            copy(videos = videos.toMutableList()).apply { videos.deepCopy(videos) } as T

    constructor(source: Parcel) : this(
            source.readString()!!,
            source.readString()!!,
            source.readLong(),
            1.toByte() == source.readByte(),
            ArrayList<Video>().apply { source.readList(this as List<*>, Video::class.java.classLoader) }
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(path)
        writeLong(size)
        writeByte((if (isTopped) 1.toByte() else 0.toByte()))
        writeList(videos as List<*>)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VideoDirectory> = object : Parcelable.Creator<VideoDirectory> {
            override fun createFromParcel(source: Parcel): VideoDirectory = VideoDirectory(source)
            override fun newArray(size: Int): Array<VideoDirectory?> = arrayOfNulls(size)
        }
    }
}

data class Video(var id: Long = 0L,
                 override var name: String = "",
                 override var path: String = "",
                 override var size: Long = 0L,
                 override var isTopped: Boolean = false,
                 var progress: Int = 0,
                 var duration: Int = 0,
                 var width: Int = 0,
                 var height: Int = 0)
    : VideoListItem(name, path, size, isTopped) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Video

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id.hashCode()

    override fun allEqual(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Video

        return id == other.id
                && name == other.name
                && path == other.path
                && size == other.size
                && isTopped == other.isTopped
                && progress == other.progress
                && duration == other.duration
                && width == other.width
                && height == other.height
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : VideoListItem> deepCopy() = copy() as T

    constructor(source: Parcel) : this(
            source.readLong(),
            source.readString()!!,
            source.readString()!!,
            source.readLong(),
            1.toByte() == source.readByte(),
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeString(name)
        writeString(path)
        writeLong(size)
        writeByte((if (isTopped) 1.toByte() else 0.toByte()))
        writeInt(progress)
        writeInt(duration)
        writeInt(width)
        writeInt(height)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Video> = object : Parcelable.Creator<Video> {
            override fun createFromParcel(source: Parcel): Video = Video(source)
            override fun newArray(size: Int): Array<Video?> = arrayOfNulls(size)
        }
    }
}