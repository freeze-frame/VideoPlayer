/*
 * Created on 2018/08/15.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.liuzhenlin.circularcheckbox.CircularCheckBox
import com.liuzhenlin.floatingmenu.DensityUtils
import com.liuzhenlin.simrv.SlidingItemMenuRecyclerView
import com.liuzhenlin.swipeback.SwipeBackFragment
import com.liuzhenlin.texturevideoview.utils.FileUtils
import com.liuzhenlin.videos.*
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.bean.VideoDirectory
import com.liuzhenlin.videos.bean.VideoListItem
import com.liuzhenlin.videos.dao.IVideoDao
import com.liuzhenlin.videos.dao.VideoListItemDao
import com.liuzhenlin.videos.model.LocalVideoListModel
import com.liuzhenlin.videos.model.OnLoadListener
import com.liuzhenlin.videos.utils.BitmapUtils2
import com.liuzhenlin.videos.utils.FileUtils2
import com.liuzhenlin.videos.utils.UiUtils
import com.liuzhenlin.videos.utils.VideoUtils2
import com.liuzhenlin.videos.view.fragment.PackageConsts.*
import com.liuzhenlin.videos.view.swiperefresh.SwipeRefreshLayout
import java.util.*
import kotlin.math.abs
import kotlin.math.min

/**
 * @author 刘振林
 */
class LocalVideoListFragment : SwipeBackFragment(),
        VideoListItemOpCallback<VideoListItem>, SwipeRefreshLayout.OnRefreshListener,
        View.OnClickListener, View.OnLongClickListener, OnBackPressedListener {

    private lateinit var mInteractionCallback: InteractionCallback
    private var mLifecycleCallback: FragmentPartLifecycleCallback? = null

    private lateinit var mRecyclerView: SlidingItemMenuRecyclerView
    private val mAdapter = VideoListAdapter()

    private var mItemOptionsWindow: PopupWindow? = null
    private var mDeleteItemsWindow: PopupWindow? = null
    private var mDeleteItemDialog: Dialog? = null
    private var mRenameItemDialog: Dialog? = null
    private var mItemDetailsDialog: Dialog? = null

    private var mTitleWindowFrame: FrameLayout? = null
    private var mSelectAllButton: TextView? = null

    private var mTitleText_IOW: TextView? = null
    private var mDeleteButton_IOW: TextView? = null
    private var mRenameButton_IOW: TextView? = null
    private var mShareButton_IOW: TextView? = null
    private var mDetailsButton_IOW: TextView? = null

    private var mNeedReloadVideos = false
    private var mVideoObserver: VideoObserver? = null
    private val mVideoListItems = mutableListOf<VideoListItem>()
    internal val model: LocalVideoListModel = LocalVideoListModel(App.getInstanceUnsafe()!!)

    internal val allVideos: ArrayList<Video>?
        get() {
            var videos: ArrayList<Video>? = null
            for (item in mVideoListItems) {
                if (videos == null) videos = ArrayList()
                when (item) {
                    is Video -> videos.add(item)
                    is VideoDirectory -> videos.addAll(item.videos)
                }
            }
            return videos?.apply {
                deepCopy(videos)
                sortByElementName()
            }
        }

    private inline val checkedItems: List<VideoListItem>?
        get() {
            var checkedItems: MutableList<VideoListItem>? = null
            for (item in mVideoListItems) {
                if (item.isChecked) {
                    if (checkedItems == null) {
                        checkedItems = mutableListOf()
                    }
                    checkedItems.add(item)
                }
            }
            return checkedItems
        }

    private var _TOP: String? = null
    private inline val TOP: String
        get() {
            if (_TOP == null) {
                _TOP = getString(R.string.top)
            }
            return _TOP!!
        }
    private var _CANCEL_TOP: String? = null
    private inline val CANCEL_TOP: String
        get() {
            if (_CANCEL_TOP == null) {
                _CANCEL_TOP = getString(R.string.cancelTop)
            }
            return _CANCEL_TOP!!
        }
    private var _SELECT_ALL: String? = null
    private inline val SELECT_ALL: String
        get() {
            if (_SELECT_ALL == null) {
                _SELECT_ALL = getString(R.string.selectAll)
            }
            return _SELECT_ALL!!
        }
    private var _SELECT_NONE: String? = null
    private inline val SELECT_NONE: String
        get() {
            if (_SELECT_NONE == null) {
                _SELECT_NONE = getString(R.string.selectNone)
            }
            return _SELECT_NONE!!
        }

    private inline val miniThumbSize
        get() = (512f * (resources.displayMetrics.widthPixels / 1080f) + 0.5f).toInt()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val parent = parentFragment

        mInteractionCallback = when {
            parent is InteractionCallback -> parent
            context is InteractionCallback -> context
            parent != null -> throw RuntimeException("Neither $context nor $parent " +
                    "has implemented LocalVideoListFragment.InteractionCallback")
            else -> throw RuntimeException(
                    "$context must implement LocalVideoListFragment.InteractionCallback")
        }

        if (parent is FragmentPartLifecycleCallback) {
            mLifecycleCallback = parent
        } else if (context is FragmentPartLifecycleCallback) {
            mLifecycleCallback = context
        }
        mLifecycleCallback?.onFragmentAttached(this)

        model.addOnLoadListener(object : OnLoadListener<MutableList<VideoListItem>?> {
            override fun onLoadStart() {
                mRecyclerView.releaseItemView(false)
                mRecyclerView.isItemDraggable = false
            }

            override fun onLoadFinish(result: MutableList<VideoListItem>?) {
                onReloadVideoListItems(result)
                mRecyclerView.isItemDraggable = true
                mInteractionCallback.isRefreshLayoutRefreshing = false
            }

            override fun onLoadCanceled() {
                mRecyclerView.isItemDraggable = true
                mInteractionCallback.isRefreshLayoutRefreshing = false
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentView = inflater.inflate(R.layout.fragment_local_video_list, container, false)
        mRecyclerView = contentView.findViewById(R.id.simrv_videoList)
        mRecyclerView.layoutManager = LinearLayoutManager(contentView.context)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.addItemDecoration(
                DividerItemDecoration(contentView.context, DividerItemDecoration.VERTICAL))
        mRecyclerView.setHasFixedSize(true)

        isSwipeBackEnabled = false
        return attachViewToSwipeBackLayout(contentView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLifecycleCallback?.onFragmentViewCreated(this)

        autoLoadVideos()
    }

    override fun onStart() {
        super.onStart()
        mVideoObserver?.stopWatching()
        if (mNeedReloadVideos) {
            mNeedReloadVideos = false
            autoLoadVideos()
        }
    }

    override fun onStop() {
        super.onStop()
        val activity = contextThemedFirst as? Activity
        if (activity?.isFinishing == false && !isRemoving && !isDetached) {
            (mVideoObserver ?: VideoObserver(view!!.rootView.handler)).startWatching()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mLifecycleCallback?.onFragmentViewDestroyed(this)

        mItemOptionsWindow?.dismiss()
        mDeleteItemsWindow?.dismiss()
        mDeleteItemDialog?.dismiss()
        mRenameItemDialog?.dismiss()
        mItemDetailsDialog?.dismiss()

        mVideoObserver?.stopWatching()
        mNeedReloadVideos = false

        model.stopLoader()
//        mVideoListItems.clear()
//        notifyListenersOnReloadVideos()
    }

    override fun onDetach() {
        super.onDetach()
        mLifecycleCallback?.onFragmentDetached(this)
    }

    override fun onBackPressed(): Boolean {
        mItemOptionsWindow?.dismiss() ?: return false
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PLAY_VIDEO -> if (resultCode == RESULT_CODE_PLAY_VIDEO) {
                val video = data?.getParcelableExtra<Video>(KEY_VIDEO) ?: return
                if (video.id == NO_ID) return

                for (i in mVideoListItems.indices) {
                    val item = mVideoListItems[i] as? Video ?: continue
                    if (item != video) continue

                    if (item.progress != video.progress) {
                        item.progress = video.progress
                        mAdapter.notifyItemChanged(i, PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION)
                    }
                    break
                }
            }
            REQUEST_CODE_LOCAL_SEARCHED_VIDEOS_FRAGMENT ->
                if (resultCode == RESULT_CODE_LOCAL_SEARCHED_VIDEOS_FRAGMENT) {
                    val videos = data?.getParcelableArrayListExtra<Video>(KEY_VIDEOS) ?: return
                    if (!videos.allEqual(allVideos)) {
                        autoLoadVideos()
                    }
                }
            REQUEST_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT ->
                if (resultCode == RESULT_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT) {
                    val dirPath = data?.getStringExtra(KEY_DIRECTORY_PATH) ?: return
                    val videos = data.getParcelableArrayListExtra<Video>(KEY_VIDEOS) ?: return
                    loop@ for ((i, item) in mVideoListItems.withIndex()) {
                        if (item.path != dirPath) continue@loop

                        val dao = VideoListItemDao.getSingleton(contextRequired)
                        when (videos.size) {
                            0 -> {
                                if (item is VideoDirectory) {
                                    dao.deleteVideoDir(item.path)
                                }

                                mVideoListItems.removeAt(i)
                                mAdapter.notifyItemRemoved(i)
                                mAdapter.notifyItemRangeChanged(i, mAdapter.itemCount - i)
                            }
                            1 -> {
                                if (item is VideoDirectory) {
                                    dao.deleteVideoDir(item.path)
                                }

                                val video = videos[0]
                                if (video.isTopped) {
                                    video.isTopped = false
                                    dao.setVideoListItemTopped(video, false)
                                }

                                mVideoListItems[i] = video
                                val newIndex = mVideoListItems.reordered().indexOf(video)
                                if (newIndex == i) {
                                    mAdapter.notifyItemChanged(i) // without payload
                                } else {
                                    mVideoListItems.add(newIndex, mVideoListItems.removeAt(i))
                                    mAdapter.notifyItemRemoved(i)
                                    mAdapter.notifyItemInserted(newIndex)
                                    mAdapter.notifyItemRangeChanged(min(i, newIndex),
                                            abs(newIndex - i) + 1)
                                }
                            }
                            else -> {
                                if (item is Video) {
                                    var videodir = dao.queryVideoDirByPath(dirPath)
                                    if (videodir == null) {
                                        videodir = dao.insertVideoDir(dirPath)
                                    } else
                                        if (videodir.isTopped) {
                                            videodir.isTopped = false
                                            dao.setVideoListItemTopped(videodir, false)
                                        }
                                    videodir.videos = videos
                                    videodir.size = videos.allVideoSize()

                                    mVideoListItems[i] = videodir
                                    val newIndex = mVideoListItems.reordered().indexOf(videodir)
                                    if (newIndex == i) {
                                        mAdapter.notifyItemChanged(i) // without payload
                                    } else {
                                        mVideoListItems.add(newIndex, mVideoListItems.removeAt(i))
                                        mAdapter.notifyItemRemoved(i)
                                        mAdapter.notifyItemInserted(newIndex)
                                        mAdapter.notifyItemRangeChanged(min(i, newIndex),
                                                abs(newIndex - i) + 1)
                                    }
                                } else if (item is VideoDirectory) {
                                    val oldVideos = item.videos
                                    val oldSize = item.size
                                    val oldVideoCount = oldVideos.size
                                    if (!oldVideos.allEqual(videos)) {
                                        item.videos = videos
                                        item.size = videos.allVideoSize()

                                        var payloads = 0
                                        if (!oldVideos[0].allEqual(videos[0])) {
                                            payloads = payloads or PAYLOAD_REFRESH_VIDEODIR_THUMB
                                        }
                                        if (oldSize != item.size || oldVideoCount != videos.size) {
                                            payloads = payloads or PAYLOAD_REFRESH_VIDEODIR_SIZE_AND_VIDEO_COUNT
                                        }
                                        if (payloads != 0) {
                                            mAdapter.notifyItemChanged(i, payloads)
                                        }
                                    }
                                }
                            }
                        }
                        break@loop
                    }
                }
        }
    }

    private fun onReloadVideoListItems(items: List<VideoListItem>?) {
        if (items == null || items.isEmpty()) {
            if (mVideoListItems.isNotEmpty()) {
                mVideoListItems.clear()
                mAdapter.notifyDataSetChanged()
            }
        } else if (items.size == mVideoListItems.size) {
            var changedIndices: MutableList<Int>? = null
            for (i in items.indices) {
                if (!items[i].allEqual(mVideoListItems[i])) {
                    if (changedIndices == null) changedIndices = LinkedList()
                    changedIndices.add(i)
                }
            }
            if (changedIndices != null) {
                for (index in changedIndices) {
                    mVideoListItems[index] = items[index]
                    mAdapter.notifyItemChanged(index) // without payload
                }
            }
        } else {
            mVideoListItems.set(items)
            mAdapter.notifyDataSetChanged()
        }
    }

    private fun autoLoadVideos() {
        mInteractionCallback.isRefreshLayoutRefreshing = true
        queryAllVideos()
    }

    override fun onRefresh() = queryAllVideos()

    private fun queryAllVideos() {
        if (isAsyncDeletingItems) { // 页面自动刷新或用户手动刷新时，还有视频在被异步删除...
            mInteractionCallback.isRefreshLayoutRefreshing = false
            return
        }

        /*
         * 1）自动刷新时隐藏弹出的多选窗口
         * 2）用户长按列表时可能又在下拉刷新，多选窗口会被弹出，需要隐藏
         */
        mItemOptionsWindow?.dismiss()

        model.startLoader()
    }

    private inner class VideoObserver(handler: Handler) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean) {
            // 此页面放入后台且数据有变化，标记为需要刷新数据
            // 以在此页面重新显示在前台时刷新列表
            mNeedReloadVideos = true
        }

        fun startWatching() {
            mVideoObserver = this
            contextRequired.applicationContext.contentResolver
                    .registerContentObserver(IVideoDao.VIDEO_URI, false, this)
        }

        fun stopWatching() {
            mVideoObserver = null
            contextRequired.applicationContext.contentResolver.unregisterContentObserver(this)
        }
    }

    private inner class VideoListAdapter : RecyclerView.Adapter<VideoListAdapter.VideoListViewHolder>() {

        override fun getItemCount() = mVideoListItems.size

        override fun getItemViewType(position: Int) =
                when (mVideoListItems[position]) {
                    is Video -> VIEW_TYPE_VIDEO
                    is VideoDirectory -> VIEW_TYPE_VIDEODIR
                    else -> throw IllegalArgumentException("Unknown itemView type")
                }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder =
                when (viewType) {
                    VIEW_TYPE_VIDEO -> VideoViewHolder(
                            LayoutInflater.from(parent.context)
                                    .inflate(R.layout.video_list_item_video, parent, false))
                    VIEW_TYPE_VIDEODIR -> VideoDirViewHolder(
                            LayoutInflater.from(parent.context)
                                    .inflate(R.layout.video_list_item_videodir, parent, false))
                    else -> throw IllegalArgumentException("Unknown itemView type")
                }

        override fun onBindViewHolder(holder: VideoListViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
            } else {
                val item = mVideoListItems[position]

                val payload = payloads[0] as Int
                if (payload and PAYLOAD_CHANGE_ITEM_LPS_AND_BG != 0) {
                    separateToppedItemsFromUntoppedOnes(holder, position)
                }
                if (payload and PAYLOAD_CHANGE_CHECKBOX_VISIBILITY != 0) {
                    if (mItemOptionsWindow == null) {
                        holder.checkBox.visibility = View.GONE
                    } else {
                        holder.checkBox.visibility = View.VISIBLE
                    }
                }
                if (payload and PAYLOAD_REFRESH_CHECKBOX != 0) {
                    holder.checkBox.isChecked = item.isChecked
                } else if (payload and PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR != 0) {
                    holder.checkBox.setChecked(item.isChecked, true)
                }
                if (payload and PAYLOAD_REFRESH_ITEM_NAME != 0) {
                    when (holder) {
                        is VideoViewHolder -> holder.videoNameText.text = item.name
                        is VideoDirViewHolder -> holder.videodirNameText.text = item.name
                    }
                }
                if (payload and PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION != 0) {
                    val (_, _, _, _, _, progress, duration) = item as Video
                    (holder as VideoViewHolder).videoProgressAndDurationText.text =
                            VideoUtils2.concatVideoProgressAndDuration(progress, duration)
                }
                if (payload and PAYLOAD_REFRESH_VIDEODIR_THUMB != 0) {
                    val vh = holder as VideoDirViewHolder
                    val videos = (item as VideoDirectory).videos
                    VideoUtils2.loadVideoThumbIntoFragmentImageView(
                            this@LocalVideoListFragment, vh.videodirImage, videos[0])
                }
                if (payload and PAYLOAD_REFRESH_VIDEODIR_SIZE_AND_VIDEO_COUNT != 0) {
                    val vh = holder as VideoDirViewHolder
                    val videos = (item as VideoDirectory).videos
                    vh.videodirSizeText.text = FileUtils2.formatFileSize(item.size.toDouble())
                    vh.videoCountText.text = getString(R.string.aTotalOfSeveralVideos, videos.size)
                }
            }
        }

        override fun onBindViewHolder(holder: VideoListViewHolder, position: Int) {
            holder.itemVisibleFrame.tag = position
            holder.checkBox.tag = position
            holder.topButton.tag = position
            holder.deleteButton.tag = position

            separateToppedItemsFromUntoppedOnes(holder, position)

            val item = mVideoListItems[position]
            if (mItemOptionsWindow == null) {
                holder.checkBox.visibility = View.GONE
            } else {
                holder.checkBox.visibility = View.VISIBLE
                holder.checkBox.isChecked = item.isChecked
            }
            when (holder.itemViewType) {
                VIEW_TYPE_VIDEO -> {
                    val vh = holder as VideoViewHolder
                    val video = item as Video

                    VideoUtils2.loadVideoThumbIntoFragmentImageView(
                            this@LocalVideoListFragment, vh.videoImage, video)
                    vh.videoNameText.text = item.name
                    vh.videoSizeText.text = FileUtils2.formatFileSize(item.size.toDouble())
                    vh.videoProgressAndDurationText.text =
                            VideoUtils2.concatVideoProgressAndDuration(video.progress, video.duration)
                }
                VIEW_TYPE_VIDEODIR -> {
                    val vh = holder as VideoDirViewHolder
                    val videos = (item as VideoDirectory).videos

                    VideoUtils2.loadVideoThumbIntoFragmentImageView(
                            this@LocalVideoListFragment, vh.videodirImage, videos[0])
                    vh.videodirNameText.text = item.name
                    vh.videodirSizeText.text = FileUtils2.formatFileSize(item.size.toDouble())
                    vh.videoCountText.text = getString(R.string.aTotalOfSeveralVideos, videos.size)
                }
            }
        }

        fun separateToppedItemsFromUntoppedOnes(holder: VideoListViewHolder, position: Int) {
            val context = contextThemedFirst
            val lp = holder.topButton.layoutParams

            if (mVideoListItems[position].isTopped) {
                ViewCompat.setBackground(holder.itemVisibleFrame,
                        ContextCompat.getDrawable(context, R.drawable.selector_topped_recycler_item))

                lp.width = DensityUtils.dp2px(context, 120f)
                holder.topButton.layoutParams = lp
                holder.topButton.text = CANCEL_TOP
            } else {
                ViewCompat.setBackground(holder.itemVisibleFrame,
                        ContextCompat.getDrawable(context, R.drawable.default_selector_recycler_item))

                lp.width = DensityUtils.dp2px(context, 90f)
                holder.topButton.layoutParams = lp
                holder.topButton.text = TOP
            }
        }

        open inner class VideoListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemVisibleFrame: ViewGroup = itemView.findViewById(R.id.itemVisibleFrame)
            val checkBox: CircularCheckBox = itemView.findViewById(R.id.checkbox)
            val topButton: TextView = itemView.findViewById(R.id.btn_top)
            val deleteButton: TextView = itemView.findViewById(R.id.btn_delete)

            init {
                itemVisibleFrame.setOnClickListener(this@LocalVideoListFragment)
                checkBox.setOnClickListener(this@LocalVideoListFragment)
                topButton.setOnClickListener(this@LocalVideoListFragment)
                deleteButton.setOnClickListener(this@LocalVideoListFragment)

                itemVisibleFrame.setOnLongClickListener(this@LocalVideoListFragment)
            }
        }

        inner class VideoViewHolder(itemView: View) : VideoListViewHolder(itemView) {
            val videoImage: ImageView = itemView.findViewById(R.id.image_video)
            val videoNameText: TextView = itemView.findViewById(R.id.text_videoName)
            val videoSizeText: TextView = itemView.findViewById(R.id.text_videoSize)
            val videoProgressAndDurationText: TextView = itemView.findViewById(R.id.text_videoProgressAndDuration)
        }

        inner class VideoDirViewHolder(itemView: View) : VideoListViewHolder(itemView) {
            val videodirImage: ImageView = itemView.findViewById(R.id.image_videodir)
            val videodirNameText: TextView = itemView.findViewById(R.id.text_videodirName)
            val videodirSizeText: TextView = itemView.findViewById(R.id.text_videodirSize)
            val videoCountText: TextView = itemView.findViewById(R.id.text_videoCount)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.itemVisibleFrame -> {
                val position = v.tag as Int
                val item = mVideoListItems[position]

                if (mItemOptionsWindow == null) {
                    when (item) {
                        is Video -> playVideo(item) // 播放视频
                        is VideoDirectory -> { // 显示指定目录的视频
                            val args = Bundle()
                            args.putParcelable(KEY_VIDEODIR, item.deepCopy())
                            mInteractionCallback.goToLocalFoldedVideosFragment(args)
                        }
                    }
                } else {
                    item.isChecked = !item.isChecked
                    mAdapter.notifyItemChanged(position, PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                    onItemCheckedChange()
                }
            }

            R.id.checkbox -> {
                val item = mVideoListItems[v.tag as Int]
                item.isChecked = !item.isChecked
                onItemCheckedChange()
            }

            // 置顶或取消置顶视频（目录）
            R.id.btn_top -> {
                val position = v.tag as Int
                val item = mVideoListItems[position]

                val topped = !item.isTopped
                item.isTopped = topped
                VideoListItemDao.getSingleton(v.context).setVideoListItemTopped(item, topped)

                val newPosition = mVideoListItems.reordered().indexOf(item)
                if (newPosition == position) {
                    mAdapter.notifyItemChanged(position, PAYLOAD_CHANGE_ITEM_LPS_AND_BG)
                } else {
                    mVideoListItems.add(newPosition, mVideoListItems.removeAt(position))
                    mAdapter.notifyItemRemoved(position)
                    mAdapter.notifyItemInserted(newPosition)
                    mAdapter.notifyItemRangeChanged(min(position, newPosition),
                            abs(newPosition - position) + 1)
                }
            }

            // 删除视频
            R.id.btn_delete -> showDeleteItemDialog(mVideoListItems[v.tag as Int])
            R.id.btn_confirm_deleteVideoListItemDialog -> {
                val window = mDeleteItemDialog!!.window!!
                val decorView = window.decorView as ViewGroup
                val item = decorView.tag as VideoListItem
                @Suppress("UNCHECKED_CAST")
                val onDeleteAction = decorView[0].tag as (() -> Unit)?

                mDeleteItemDialog!!.cancel()

                deleteItems(item)

                if (onDeleteAction != null) {
                    onDeleteAction()
                } else {
                    val index = mVideoListItems.indexOf(item)
                    if (index != -1) {
                        mVideoListItems.removeAt(index)
                        mAdapter.notifyItemRemoved(index)
                        mAdapter.notifyItemRangeChanged(index, mAdapter.itemCount - index)
                    }
                }
            }
            R.id.btn_cancel_deleteVideoListItemDialog -> mDeleteItemDialog!!.cancel()

            // 删除（多个）视频
            R.id.btn_delete_vlow -> {
                val items = checkedItems ?: return
                showDeleteItemsPopupWindow(*items.toTypedArray())
            }
            R.id.btn_confirm_deleteItemsWindow -> {
                val view = mDeleteItemsWindow!!.contentView as ViewGroup
                @Suppress("UNCHECKED_CAST")
                val items = view.tag as Array<VideoListItem>
                @Suppress("UNCHECKED_CAST")
                val onDeleteAction = view[0].tag as (() -> Unit)?

                mDeleteItemsWindow!!.dismiss()
                mItemOptionsWindow?.dismiss()

                if (items.size == 1) {
                    val item = items[0]

                    deleteItems(item)

                    if (onDeleteAction != null) {
                        onDeleteAction()
                    } else {
                        val index = mVideoListItems.indexOf(item)
                        if (index != -1) {
                            mVideoListItems.removeAt(index)
                            mAdapter.notifyItemRemoved(index)
                            mAdapter.notifyItemRangeChanged(index, mAdapter.itemCount - index)
                        }
                    }
                } else {
                    deleteItems(*items)

                    if (onDeleteAction != null) {
                        onDeleteAction()
                    } else {
                        var start = -1
                        var index = 0
                        val it = mVideoListItems.iterator()
                        while (it.hasNext()) {
                            if (items.contains(it.next())) {
                                if (start == -1) {
                                    start = index
                                }
                                it.remove()
                                mAdapter.notifyItemRemoved(index)
                                index--
                            }
                            index++
                        }
                        mAdapter.notifyItemRangeChanged(start, mAdapter.itemCount - start)
                    }
                }
            }
            R.id.btn_cancel_deleteItemsWindow -> mDeleteItemsWindow!!.dismiss()

            // 重命名视频或给视频目录取别名
            R.id.btn_rename -> {
                showRenameItemDialog(checkedItems?.get(0) ?: return)
                mItemOptionsWindow!!.dismiss()
            }
            R.id.btn_complete_renameVideoListItemDialog -> {
                val editText = v.tag as EditText
                val text = editText.text.toString().trim()
                val newName =
                        if (text.isEmpty()) editText.hint.toString()
                        else text + editText.tag as String

                val window = mRenameItemDialog!!.window!!
                val decorView = window.decorView as ViewGroup
                val item = decorView.tag as VideoListItem
                @Suppress("UNCHECKED_CAST")
                val onRenameAction = decorView[0].tag as (() -> Unit)?

                mRenameItemDialog!!.cancel()

                if (renameItem(item, newName, view)) {
                    if (onRenameAction != null) {
                        onRenameAction()
                    } else {
                        val position = mVideoListItems.indexOf(item)
                        if (position != -1) {
                            val newPosition = mVideoListItems.reordered().indexOf(item)
                            if (newPosition == position) {
                                mAdapter.notifyItemChanged(position, PAYLOAD_REFRESH_ITEM_NAME)
                            } else {
                                mVideoListItems.add(newPosition, mVideoListItems.removeAt(position))
                                mAdapter.notifyItemRemoved(position)
                                mAdapter.notifyItemInserted(newPosition)
                                mAdapter.notifyItemRangeChanged(min(position, newPosition),
                                        abs(newPosition - position) + 1)
                            }
                        }
                    }
                }
            }
            R.id.btn_cancel_renameVideoListItemDialog -> mRenameItemDialog!!.cancel()

            // 分享
            R.id.btn_share -> {
                shareVideo(checkedItems?.get(0) as? Video ?: return)
                mItemOptionsWindow!!.dismiss()
            }

            // 显示视频（目录）详情
            R.id.btn_details -> {
                showItemDetailsDialog(checkedItems?.get(0) ?: return)
                mItemOptionsWindow!!.dismiss()
            }
            R.id.btn_ok_videoListItemDetailsDialog -> mItemDetailsDialog!!.cancel()

            R.id.btn_cancel_vlow -> mItemOptionsWindow!!.dismiss()
            // 全（不）选
            R.id.btn_selectAll -> {
                // 全选
                if (SELECT_ALL == mSelectAllButton!!.text.toString()) {
                    for (i in mVideoListItems.indices) {
                        val item = mVideoListItems[i]
                        if (!item.isChecked) {
                            item.isChecked = true
                            mAdapter.notifyItemChanged(i, PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                        }
                    }
                    // 全不选
                } else {
                    for (item in mVideoListItems) {
                        item.isChecked = false
                    }
                    mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount,
                            PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                }
                onItemCheckedChange()
            }
        }
    }

    override fun onLongClick(v: View): Boolean {
        when (v.id) {
            R.id.itemVisibleFrame -> {
                if (mItemOptionsWindow != null || mInteractionCallback.isRefreshLayoutRefreshing) {
                    return false
                }

                mTitleWindowFrame = View.inflate(v.context,
                        R.layout.popup_window_main_title, null) as FrameLayout
                mTitleWindowFrame!!.post(object : Runnable {
                    init {
                        mTitleWindowFrame!!.findViewById<View>(R.id.btn_cancel_vlow)
                                .setOnClickListener(this@LocalVideoListFragment)

                        mSelectAllButton = mTitleWindowFrame!!.findViewById(R.id.btn_selectAll)
                        mSelectAllButton!!.setOnClickListener(this@LocalVideoListFragment)
                    }

                    override fun run() = mTitleWindowFrame?.run {
                        val statusHeight = App.getInstance(v.context).statusHeightInPortrait
                        layoutParams.height = height + statusHeight
                        setPadding(paddingLeft, paddingTop + statusHeight, paddingRight, paddingBottom)
                    } ?: Unit
                })
                val titleWindow = PopupWindow(mTitleWindowFrame,
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                titleWindow.isClippingEnabled = false
                titleWindow.animationStyle = R.style.WindowAnimations_TopPopupWindow
                titleWindow.showAtLocation(v, Gravity.TOP, 0, 0)

                val iowcv = View.inflate(v.context, R.layout.popup_window_videolist_options, null)
                mTitleText_IOW = iowcv.findViewById(R.id.text_title)
                mDeleteButton_IOW = iowcv.findViewById(R.id.btn_delete_vlow)
                mDeleteButton_IOW!!.setOnClickListener(this)
                mRenameButton_IOW = iowcv.findViewById(R.id.btn_rename)
                mRenameButton_IOW!!.setOnClickListener(this)
                mShareButton_IOW = iowcv.findViewById(R.id.btn_share)
                mShareButton_IOW!!.setOnClickListener(this)
                mDetailsButton_IOW = iowcv.findViewById(R.id.btn_details)
                mDetailsButton_IOW!!.setOnClickListener(this)

                mItemOptionsWindow = PopupWindow(iowcv,
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                mItemOptionsWindow!!.animationStyle = R.style.WindowAnimations_BottomPopupWindow
                mItemOptionsWindow!!.showAtLocation(v, Gravity.BOTTOM, 0, 0)

                iowcv.post(object : Runnable {
                    val selection = v.tag as Int

                    init {
                        mInteractionCallback.setLightStatus(true)
                        mInteractionCallback.setSideDrawerEnabled(false)
                        mInteractionCallback.isRefreshLayoutEnabled = false
                        mRecyclerView.isItemDraggable = false
                    }

                    override fun run() {
                        val location = IntArray(2)

                        iowcv.getLocationOnScreen(location)
                        val iowTop = location[1]

                        val rvParent = mRecyclerView.parent as View
                        rvParent.getLocationOnScreen(location)
                        val rvpBottom = location[1] + rvParent.height

                        val rvpLP = rvParent.layoutParams
                        rvpLP.height = rvParent.height - (rvpBottom - iowTop)
                        rvParent.layoutParams = rvpLP

                        val itemBottom = (v.parent as View).bottom
                        if (itemBottom <= rvpLP.height) {
                            notifyItemsToShowCheckBoxes()
                        } else {
                            mRecyclerView.post {
                                // 使长按的itemView在RecyclerView高度改变后可见
                                mRecyclerView.scrollBy(0, itemBottom - rvpLP.height)

                                notifyItemsToShowCheckBoxes()
                            }
                        }
                    }

                    fun notifyItemsToShowCheckBoxes() = mAdapter.run {
                        notifyItemRangeChanged(0, selection,
                                PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)
                        notifyItemRangeChanged(selection + 1, itemCount - selection - 1,
                                PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)
                        // 勾选当前长按的itemView
                        mVideoListItems[selection].isChecked = true
                        notifyItemChanged(selection,
                                PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                        onItemCheckedChange()
                    }
                })
                mItemOptionsWindow!!.setOnDismissListener {
                    mTitleWindowFrame = null
                    mSelectAllButton = null
                    titleWindow.dismiss()

                    mItemOptionsWindow = null
                    mTitleText_IOW = null
                    mDeleteButton_IOW = null
                    mRenameButton_IOW = null
                    mShareButton_IOW = null
                    mDetailsButton_IOW = null

                    val parent = mRecyclerView.parent as View
                    val lp = parent.layoutParams
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT
                    parent.layoutParams = lp

                    for (item in mVideoListItems) {
                        item.isChecked = false
                    }
                    mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount,
                            PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)

                    mInteractionCallback.setLightStatus(false)
                    mInteractionCallback.setSideDrawerEnabled(true)
                    mInteractionCallback.isRefreshLayoutEnabled = true
                    mRecyclerView.isItemDraggable = true
                }
                return true
            }
        }
        return false
    }

    private fun onItemCheckedChange() {
        mItemOptionsWindow ?: return

        var firstCheckedItem: VideoListItem? = null
        var checkedItemCount = 0
        for (item in mVideoListItems) {
            if (item.isChecked) {
                checkedItemCount++
                if (checkedItemCount == 1) {
                    firstCheckedItem = item
                }
            }
        }

        if (checkedItemCount == mVideoListItems.size) {
            mSelectAllButton!!.text = SELECT_NONE
        } else {
            mSelectAllButton!!.text = SELECT_ALL
        }

        var aVideoCheckedOnly = false
        mTitleText_IOW!!.text = when (checkedItemCount) {
            0 -> EMPTY_STRING
            1 -> {
                val item = firstCheckedItem!!
                aVideoCheckedOnly = item is Video

                if (aVideoCheckedOnly) item.name else getString(R.string.someDirectory, item.name)
            }
            else -> getString(R.string.severalItemsHaveBeenSelected, checkedItemCount)
        }

        mDeleteButton_IOW!!.isEnabled = checkedItemCount >= 1
        val enabled = checkedItemCount == 1
        mRenameButton_IOW!!.isEnabled = enabled
        mShareButton_IOW!!.isEnabled = aVideoCheckedOnly
        mDetailsButton_IOW!!.isEnabled = enabled
    }

    override fun showDeleteItemDialog(item: VideoListItem, onDeleteAction: (() -> Unit)?) {
        val context = contextThemedFirst
        val view = View.inflate(context, R.layout.dialog_message, null)

        view.findViewById<TextView>(R.id.text_message).text = when (item) {
            is Video -> getString(R.string.areYouSureToDeleteSth, item.name)
            else /* is VideoDirectory */ -> getString(R.string.areYouSureToDeleteSomeVideoDir, item.name)
        }

        val cancel = view.findViewById<TextView>(R.id.btn_cancel)
        cancel.id = R.id.btn_cancel_deleteVideoListItemDialog
        cancel.setOnClickListener(this)
        val lp = cancel.layoutParams as RelativeLayout.LayoutParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            lp.addRule(RelativeLayout.START_OF, R.id.btn_confirm_deleteVideoListItemDialog)
        } else {
            lp.addRule(RelativeLayout.LEFT_OF, R.id.btn_confirm_deleteVideoListItemDialog)
        }

        val confirm = view.findViewById<TextView>(R.id.btn_ok)
        confirm.id = R.id.btn_confirm_deleteVideoListItemDialog
        confirm.setOnClickListener(this)

        mDeleteItemDialog = AppCompatDialog(context, R.style.DialogStyle_MinWidth_NoTitle)
        mDeleteItemDialog!!.setContentView(view)
        mDeleteItemDialog!!.setOnDismissListener { mDeleteItemDialog = null }
        mDeleteItemDialog!!.show()

        val window = mDeleteItemDialog!!.window!!
        val decorView = window.decorView as ViewGroup
        decorView.tag = item
        decorView[0].tag = onDeleteAction
    }

    override fun showDeleteItemsPopupWindow(vararg items: VideoListItem, onDeleteAction: (() -> Unit)?) {
        if (items.isEmpty()) return

        val view = View.inflate(contextThemedFirst,
                R.layout.popup_window_delete_video_list_items, null) as ViewGroup
        view.findViewById<TextView>(R.id.text_message).text = if (items.size == 1) {
            when (items[0]) {
                is Video -> getString(R.string.areYouSureToDeleteSth, items[0].name)
                else /* is VideoDirectory */ -> getString(R.string.areYouSureToDeleteSomeVideoDir, items[0].name)
            }
        } else {
            getString(R.string.areYouSureToDelete)
        }
        view.findViewById<View>(R.id.btn_confirm_deleteItemsWindow).setOnClickListener(this)
        view.findViewById<View>(R.id.btn_cancel_deleteItemsWindow).setOnClickListener(this)
        view.tag = items
        view[0].tag = onDeleteAction

        val fadedContentView = this.view!!.rootView as FrameLayout // DecorView

        mDeleteItemsWindow = PopupWindow(view,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mDeleteItemsWindow!!.isTouchable = true
        mDeleteItemsWindow!!.isFocusable = true
        // 这是必须的，否则'setFocusable'将无法在Android 6.0以下运行
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mDeleteItemsWindow!!.setBackgroundDrawable(BitmapDrawable(resources, null as Bitmap?))
        }
        mDeleteItemsWindow!!.showAtLocation(fadedContentView, Gravity.BOTTOM, 0, 0)
        mDeleteItemsWindow!!.setOnDismissListener {
            mDeleteItemsWindow = null

            mTitleWindowFrame?.foreground = null
            fadedContentView.foreground = null
        }

        mTitleWindowFrame?.foreground = ColorDrawable(0x7F000000)
        fadedContentView.foreground = ColorDrawable(0x7F000000)
    }

    override fun showRenameItemDialog(item: VideoListItem, onRenameAction: (() -> Unit)?) {
        val name = item.name
        val postfix: String = when (item) {
            is Video -> {
                val index = name.lastIndexOf(".")
                if (index == -1) EMPTY_STRING else name.substring(index)
            }
            else /* is VideoDirectory */ -> EMPTY_STRING
        }

        val context = contextThemedFirst
        val res = context.resources

        val view = View.inflate(context, R.layout.dialog_rename_video_list_item, null)

        val titleText = view.findViewById<TextView>(R.id.text_title)
        titleText.setText(
                if (item is Video) R.string.renameVideo
                else /* if (item is VideoDirectory) */ R.string.renameDirectory)

        val thumbImage = view.findViewById<ImageView>(R.id.image_videoListItem)
        val thumbSize = miniThumbSize
        val placeholder = BitmapUtils2.createScaledBitmap(
                BitmapFactory.decodeResource(res, R.drawable.ic_default_thumb),
                thumbSize, (thumbSize * 9f / 16f + 0.5f).toInt(),
                true)
        val glideRequestManager = Glide.with(context.applicationContext)
        glideRequestManager
                .load((item as? Video ?: (item as VideoDirectory).videos[0]).path)
                .override(thumbSize)
                .fitCenter()
                .placeholder(BitmapDrawable(res, placeholder))
                .into(thumbImage)

        val editText = view.findViewById<EditText>(R.id.editor_rename)
        editText.hint = name
        editText.setText(name.replace(postfix, EMPTY_STRING))
        editText.setSelection(editText.text.length)
        editText.post { UiUtils.showSoftInput(editText) }
        editText.tag = postfix

        val cancelButton = view.findViewById<TextView>(R.id.btn_cancel_renameVideoListItemDialog)
        cancelButton.setOnClickListener(this)

        val completeButton = view.findViewById<TextView>(R.id.btn_complete_renameVideoListItemDialog)
        completeButton.setOnClickListener(this)
        completeButton.tag = editText

        mRenameItemDialog = AppCompatDialog(context, R.style.DialogStyle_MinWidth_NoTitle)
        mRenameItemDialog!!.setContentView(view)
        mRenameItemDialog!!.show()
        mRenameItemDialog!!.setCancelable(true)
        mRenameItemDialog!!.setCanceledOnTouchOutside(false)
        mRenameItemDialog!!.setOnDismissListener {
            mRenameItemDialog = null
            glideRequestManager.clear(thumbImage)
        }

        val window = mRenameItemDialog!!.window!!
        val decorView = window.decorView as ViewGroup
        decorView.tag = item
        decorView[0].tag = onRenameAction
    }

    override fun showItemDetailsDialog(item: VideoListItem) {
        val context = contextThemedFirst
        val view: View
        val thumbTextView: TextView
        val video: Video

        val colon = getString(R.string.colon)
        if (item is Video) {
            view = View.inflate(context, R.layout.dialog_video_details, null)

            video = item

            thumbTextView = view.findViewById(R.id.text_videoName)
            var ss = SpannableString(getString(R.string.name, item.name))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            thumbTextView.text = ss

            val videoSizeText = view.findViewById<TextView>(R.id.text_videoSize)
            ss = SpannableString(getString(
                    R.string.size, FileUtils2.formatFileSize(item.size.toDouble())))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videoSizeText.text = ss

            val videoResolutionText = view.findViewById<TextView>(R.id.text_videoResolution)
            ss = SpannableString(getString(
                    R.string.resolution, VideoUtils2.formatVideoResolution(item.width, item.height)))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videoResolutionText.text = ss

            val videoPathText = view.findViewById<TextView>(R.id.text_videoPath)
            ss = SpannableString(getString(R.string.path, item.path))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videoPathText.text = ss
        } else /* if (item is VideoDirectory) */ {
            view = View.inflate(context, R.layout.dialog_videodir_details, null)

            val videos = (item as VideoDirectory).videos
            video = item.videos[0]

            val path = item.path
            val dirname = FileUtils.getFileNameFromFilePath(path)

            thumbTextView = view.findViewById(R.id.text_videodirName)
            var ss = SpannableString(getString(
                    if (item.name.equals(dirname, ignoreCase = true)) R.string.name
                    else R.string.alias
                    , item.name))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            thumbTextView.text = ss

            val videodirSizeText = view.findViewById<TextView>(R.id.text_videodirSize)
            ss = SpannableString(getString(
                    R.string.size, FileUtils2.formatFileSize(item.size.toDouble())))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videodirSizeText.text = ss

            val videoCountText = view.findViewById<TextView>(R.id.text_videoCount)
            ss = SpannableString(getString(R.string.videoCount, videos.size))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videoCountText.text = ss

            val videodirPathText = view.findViewById<TextView>(R.id.text_videodirPath)
            ss = SpannableString(getString(R.string.path, path))
            ss.setSpan(ForegroundColorSpan(Color.BLACK),
                    0, ss.indexOf(colon) + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            videodirPathText.text = ss
        }

        val okButton = view.findViewById<TextView>(R.id.btn_ok_videoListItemDetailsDialog)
        okButton.setOnClickListener(this)

        val thumbSize = miniThumbSize
        val thumbTextViewTarget = object : CustomViewTarget<TextView, Drawable>(thumbTextView) {
            val placeholder = ContextCompat.getDrawable(context, R.drawable.ic_default_thumb)!!

            fun showPlaceHolderDrawable() {
                placeholder.setBounds(0, 0, thumbSize, (thumbSize * 9f / 16f + 0.5f).toInt())
                thumbTextView.setCompoundDrawables(null, placeholder, null, null)
            }

            override fun onResourceLoading(placeholder: Drawable?) {
                showPlaceHolderDrawable()
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                showPlaceHolderDrawable()
            }

            override fun onResourceCleared(placeholder: Drawable?) {
                showPlaceHolderDrawable()
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                thumbTextView.setCompoundDrawablesWithIntrinsicBounds(null, resource, null, null)
            }
        }
        val glideRequestManager = Glide.with(context.applicationContext)
        glideRequestManager
                .load(video.path)
                .override(thumbSize)
                .fitCenter()
                .into(thumbTextViewTarget)

        mItemDetailsDialog = AppCompatDialog(context, R.style.DialogStyle_MinWidth_NoTitle)
        mItemDetailsDialog!!.setContentView(view)
        mItemDetailsDialog!!.show()
        mItemDetailsDialog!!.setOnDismissListener {
            mItemDetailsDialog = null
            glideRequestManager.clear(thumbTextViewTarget)
        }
    }

    private companion object {
        const val PAYLOAD_REFRESH_VIDEODIR_THUMB =
                PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION shl 1
        const val PAYLOAD_REFRESH_VIDEODIR_SIZE_AND_VIDEO_COUNT =
                PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION shl 2

        const val VIEW_TYPE_VIDEODIR = 1
        const val VIEW_TYPE_VIDEO = 2
    }

    interface InteractionCallback : RefreshLayoutCallback {
        fun setLightStatus(light: Boolean)

        fun setSideDrawerEnabled(enabled: Boolean)

        fun goToLocalFoldedVideosFragment(args: Bundle)
    }
}