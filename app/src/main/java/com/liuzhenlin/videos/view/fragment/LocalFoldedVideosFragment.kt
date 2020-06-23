/*
 * Created on 2018/08/15.
 * Copyright © 2018–2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liuzhenlin.circularcheckbox.CircularCheckBox
import com.liuzhenlin.floatingmenu.DensityUtils
import com.liuzhenlin.simrv.SlidingItemMenuRecyclerView
import com.liuzhenlin.simrv.Utils
import com.liuzhenlin.swipeback.SwipeBackFragment
import com.liuzhenlin.swipeback.SwipeBackLayout
import com.liuzhenlin.videos.*
import com.liuzhenlin.videos.bean.Video
import com.liuzhenlin.videos.bean.VideoDirectory
import com.liuzhenlin.videos.dao.VideoListItemDao
import com.liuzhenlin.videos.model.LocalFoldedVideoListModel
import com.liuzhenlin.videos.model.OnLoadListener
import com.liuzhenlin.videos.model.OnReloadVideosListener
import com.liuzhenlin.videos.utils.FileUtils2
import com.liuzhenlin.videos.utils.VideoUtils2
import com.liuzhenlin.videos.view.fragment.PackageConsts.*
import com.liuzhenlin.videos.view.swiperefresh.SwipeRefreshLayout
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.min

/**
 * @author 刘振林
 */
class LocalFoldedVideosFragment : SwipeBackFragment(), View.OnClickListener, View.OnLongClickListener,
        OnReloadVideosListener, SwipeRefreshLayout.OnRefreshListener, OnBackPressedListener {

    private lateinit var mInteractionCallback: InteractionCallback
    private var mLifecycleCallback: FragmentPartLifecycleCallback? = null

    private var mVideoOpCallback: VideoListItemOpCallback<Video>? = null

    private lateinit var mRecyclerView: SlidingItemMenuRecyclerView
    private val mAdapter = VideoListAdapter()
    private inline val mVideoDir
        get() = arguments?.get(KEY_VIDEODIR) as? VideoDirectory
    private var _mVideos: MutableList<Video>? = null
    private inline val mVideos: MutableList<Video>
        get() {
            if (_mVideos == null) {
                _mVideos = mVideoDir?.videos ?: mutableListOf()
            }
            return _mVideos!!
        }
    private var mModel: LocalFoldedVideoListModel? = null

    private lateinit var mBackButton: ImageButton
    private lateinit var mCancelButton: Button
    private lateinit var mSelectAllButton: Button
    private lateinit var mVideoOptionsFrame: ViewGroup
    private lateinit var mDeleteButton: TextView
    private lateinit var mRenameButton: TextView
    private lateinit var mShareButton: TextView
    private lateinit var mDetailsButton: TextView

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

    fun setVideoOpCallback(callback: VideoListItemOpCallback<Video>?) {
        mVideoOpCallback = callback
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val parent = parentFragment

        mInteractionCallback = when {
            parent is InteractionCallback -> parent
            context is InteractionCallback -> context
            parent != null -> throw RuntimeException("Neither $parent nor $context " +
                    "has implemented LocalFoldedVideosFragment.InteractionCallback")
            else -> throw RuntimeException(
                    "$context must implement LocalFoldedVideosFragment.InteractionCallback")
        }

        if (parent is FragmentPartLifecycleCallback) {
            mLifecycleCallback = parent
        } else if (context is FragmentPartLifecycleCallback) {
            mLifecycleCallback = context
        }
        mLifecycleCallback?.onFragmentAttached(this)

        val videodir = mVideoDir
        if (videodir != null) {
            mModel = LocalFoldedVideoListModel(videodir, context)
            mModel!!.addOnLoadListener(object : OnLoadListener<MutableList<Video>?> {
                override fun onLoadStart() {
                    mRecyclerView.isItemDraggable = false
                    mRecyclerView.releaseItemView(false)
                }

                override fun onLoadFinish(result: MutableList<Video>?) {
                    onReloadDirectoryVideos(result)
                    mRecyclerView.isItemDraggable = true
                    mInteractionCallback.isRefreshLayoutRefreshing = false
                }

                override fun onLoadCanceled(result: MutableList<Video>?) {
                    mRecyclerView.isItemDraggable = true
                    mInteractionCallback.isRefreshLayoutRefreshing = false
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLifecycleCallback?.onFragmentViewCreated(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mLifecycleCallback?.onFragmentViewDestroyed(this)

        mModel?.stopLoader()
        if (mVideoOptionsFrame.visibility == View.VISIBLE) {
            for (video in mVideos) {
                video.isChecked = false
            }
            mInteractionCallback.isRefreshLayoutEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.getInstanceUnsafe()?.refWatcher?.watch(this)
    }

    override fun onDetach() {
        super.onDetach()
        mLifecycleCallback?.onFragmentDetached(this)

        targetFragment?.onActivityResult(targetRequestCode, RESULT_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT,
                Intent().putExtra(KEY_DIRECTORY_PATH, mVideoDir?.path)
                        .putParcelableArrayListExtra(KEY_VIDEOS,
                                mVideos as? ArrayList<Video> ?: ArrayList(mVideos)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_local_folded_videos, container, false)
        initViews(view)
        return attachViewToSwipeBackLayout(view)
    }

    private fun initViews(contentView: View) {
        val actionbar = mInteractionCallback.getActionBar(this)

        val titleText = actionbar.findViewById<TextView>(R.id.text_title)
        titleText.text = mVideoDir?.name

        mRecyclerView = contentView.findViewById(R.id.simrv_foldedVideoList)
        mRecyclerView.layoutManager = LinearLayoutManager(contentView.context)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.addItemDecoration(
                DividerItemDecoration(contentView.context, DividerItemDecoration.VERTICAL))
        mRecyclerView.setHasFixedSize(true)

        mBackButton = actionbar.findViewById(R.id.btn_back)
        mCancelButton = actionbar.findViewById(R.id.btn_cancel)
        mSelectAllButton = actionbar.findViewById(R.id.btn_selectAll)
        mVideoOptionsFrame = contentView.findViewById(R.id.frame_videoOptions)
        mDeleteButton = contentView.findViewById(R.id.btn_delete_videoListOptions)
        mRenameButton = contentView.findViewById(R.id.btn_rename)
        mShareButton = contentView.findViewById(R.id.btn_share)
        mDetailsButton = contentView.findViewById(R.id.btn_details)

        mBackButton.setOnClickListener(this)
        mCancelButton.setOnClickListener(this)
        mSelectAllButton.setOnClickListener(this)
        mDeleteButton.setOnClickListener(this)
        mRenameButton.setOnClickListener(this)
        mShareButton.setOnClickListener(this)
        mDetailsButton.setOnClickListener(this)

        contentView.post {
            if (Utils.isLayoutRtl(contentView)) {
                swipeBackLayout.enabledEdges = SwipeBackLayout.EDGE_RIGHT
            }
        }
    }

    override fun onBackPressed() =
            if (mVideoOptionsFrame.visibility == View.VISIBLE) {
                hideMultiselectVideoControls()
                true
            } else false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PLAY_VIDEO -> if (resultCode == RESULT_CODE_PLAY_VIDEO) {
                val video = data?.getParcelableExtra<Video>(KEY_VIDEO) ?: return
                if (video.id == NO_ID) return

                for ((i, v) in mVideos.withIndex()) {
                    if (v != video) continue
                    if (v.progress != video.progress) {
                        v.progress = video.progress
                        mAdapter.notifyItemChanged(i, PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION)
                    }
                    break
                }
            }
            REQUEST_CODE_PLAY_VIDEOS -> if (resultCode == RESULT_CODE_PLAY_VIDEOS) {
                val parcelables = data?.getParcelableArrayExtra(KEY_VIDEOS) ?: return
                val videos = Array(parcelables.size) { parcelables[it] as Video }
                for (video in videos) {
                    val index = mVideos.indexOf(video)
                    val v = if (index != -1) mVideos[index] else null
                    if (v != null && v.progress != video.progress) {
                        v.progress = video.progress
                        mAdapter.notifyItemChanged(index, PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION)
                    }
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_back -> swipeBackLayout.scrollToFinishActivityOrPopUpFragment()

            R.id.itemVisibleFrame -> {
                val position = v.tag as Int
                if (mVideoOptionsFrame.visibility == View.VISIBLE) {
                    val video = mVideos[position]
                    video.isChecked = !video.isChecked
                    mAdapter.notifyItemChanged(position, PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                    onVideoCheckedChange()
                } else {
                    if (mVideos.size == 1) {
                        playVideo(mVideos[0])
                    } else {
                        playVideos(*mVideos.toTypedArray(), selection = position)
                    }
                }
            }
            R.id.checkbox -> {
                val video = mVideos[v.tag as Int]
                video.isChecked = !video.isChecked
                onVideoCheckedChange()
            }
            R.id.btn_top -> {
                val index = v.tag as Int
                val video = mVideos[index]

                val topped = !video.isTopped
                video.isTopped = topped
                VideoListItemDao.getSingleton(v.context).setVideoListItemTopped(video, topped)

                val newIndex = mVideos.reordered().indexOf(video)
                if (newIndex == index) {
                    mAdapter.notifyItemChanged(index, PAYLOAD_CHANGE_ITEM_LPS_AND_BG)
                } else {
                    mVideos.add(newIndex, mVideos.removeAt(index))
                    mAdapter.notifyItemRemoved(index)
                    mAdapter.notifyItemInserted(newIndex)
                    mAdapter.notifyItemRangeChanged(min(index, newIndex),
                            abs(newIndex - index) + 1)
                }
            }
            R.id.btn_delete -> {
                val video = mVideos[v.tag as Int]
                mVideoOpCallback?.showDeleteItemDialog(video) {
                    onVideoDeleted(video)
                }
            }

            R.id.btn_cancel -> hideMultiselectVideoControls()
            R.id.btn_selectAll -> {
                if (mSelectAllButton.text == SELECT_ALL) {
                    for ((index, video) in mVideos.withIndex())
                        if (!video.isChecked) {
                            video.isChecked = true
                            mAdapter.notifyItemChanged(index, PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                        }
                } else {
                    for (video in mVideos) video.isChecked = false
                    mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount,
                            PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                }
                onVideoCheckedChange()
            }
            R.id.btn_delete_videoListOptions -> {
                val videos = checkedVideos ?: return
                if (videos.size == 1) {
                    mVideoOpCallback?.showDeleteItemsPopupWindow(videos[0]) {
                        hideMultiselectVideoControls()
                        onVideoDeleted(videos[0])
                    }
                } else {
                    mVideoOpCallback?.showDeleteItemsPopupWindow(*videos.toTypedArray()) {
                        hideMultiselectVideoControls()

                        var start = -1
                        var index = 0
                        val it = mVideos.iterator()
                        while (it.hasNext()) {
                            if (videos.contains(it.next())) {
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
            R.id.btn_rename -> {
                val video = checkedVideos?.get(0) ?: return

                hideMultiselectVideoControls()
                mVideoOpCallback?.showRenameItemDialog(video) {
                    val index = mVideos.indexOf(video)
                    val newIndex = mVideos.reordered().indexOf(video)
                    if (newIndex == index) {
                        mAdapter.notifyItemChanged(index, PAYLOAD_REFRESH_ITEM_NAME)
                    } else {
                        mVideos.add(newIndex, mVideos.removeAt(index))
                        mAdapter.notifyItemRemoved(index)
                        mAdapter.notifyItemInserted(newIndex)
                        mAdapter.notifyItemRangeChanged(min(index, newIndex),
                                abs(newIndex - index) + 1)
                    }
                }
            }
            R.id.btn_share -> {
                val video = checkedVideos?.get(0) ?: return

                hideMultiselectVideoControls()
                shareVideo(video)
            }
            R.id.btn_details -> {
                val video = checkedVideos?.get(0) ?: return

                hideMultiselectVideoControls()
                mVideoOpCallback?.showItemDetailsDialog(video)
            }
        }
    }

    override fun onLongClick(v: View) = when (v.id) {
        R.id.itemVisibleFrame ->
            if (mVideoOptionsFrame.visibility == View.VISIBLE
                    || mInteractionCallback.isRefreshLayoutRefreshing) {
                false
            } else {
                mBackButton.visibility = View.GONE
                mCancelButton.visibility = View.VISIBLE
                mSelectAllButton.visibility = View.VISIBLE
                mVideoOptionsFrame.visibility = View.VISIBLE
                mRecyclerView.isItemDraggable = false
                mInteractionCallback.isRefreshLayoutEnabled = false

                mRecyclerView.post {
                    val itemBottom = (v.parent as View).bottom
                    val listHeight = mRecyclerView.height
                    if (itemBottom > listHeight) {
                        mRecyclerView.scrollBy(0, itemBottom - listHeight)
                    }
                }

                val selection = v.tag as Int
                mAdapter.run {
                    notifyItemRangeChanged(0, selection,
                            PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)
                    notifyItemRangeChanged(selection + 1, itemCount - selection - 1,
                            PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)

                    mVideos[selection].isChecked = true
                    notifyItemChanged(selection,
                            PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR)
                    onVideoCheckedChange()
                }

                true
            }
        else -> false
    }

    private fun onVideoCheckedChange() {
        var checkedVideosCount = 0
        for (video in mVideos) {
            if (video.isChecked) checkedVideosCount++
        }
        when (checkedVideosCount) {
            mVideos.size -> mSelectAllButton.text = SELECT_NONE
            else -> mSelectAllButton.text = SELECT_ALL
        }
        mDeleteButton.isEnabled = checkedVideosCount > 0
        val enabled = checkedVideosCount == 1
        mRenameButton.isEnabled = enabled
        mShareButton.isEnabled = enabled
        mDetailsButton.isEnabled = enabled
    }

    private inline val checkedVideos: List<Video>?
        get() {
            var videos: MutableList<Video>? = null
            for (video in mVideos) {
                if (video.isChecked) {
                    if (videos == null) videos = mutableListOf()
                    videos.add(video)
                }
            }
            return videos
        }

    private fun hideMultiselectVideoControls() {
        mBackButton.visibility = View.VISIBLE
        mCancelButton.visibility = View.GONE
        mSelectAllButton.visibility = View.GONE
        mVideoOptionsFrame.visibility = View.GONE
        mRecyclerView.isItemDraggable = true
        mInteractionCallback.isRefreshLayoutEnabled = true

        for (video in mVideos) {
            video.isChecked = false
        }
        mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount,
                PAYLOAD_CHANGE_CHECKBOX_VISIBILITY or PAYLOAD_REFRESH_CHECKBOX)
    }

    private fun onVideoDeleted(video: Video) {
        val index = mVideos.indexOf(video)
        if (index != -1) {
            mVideos.removeAt(index)
            mAdapter.notifyItemRemoved(index)
            mAdapter.notifyItemRangeChanged(index, mAdapter.itemCount - index)
        }
    }

    override fun onReloadVideos(videos: MutableList<Video>?) =
            if (videos == null || videos.isEmpty()) {
                onReloadDirectoryVideos(null)
            } else {
                onReloadDirectoryVideos(
                        videos.filter {
                            it.path.substring(0, it.path.lastIndexOf(File.separatorChar))
                                    .equals(mVideoDir?.path, ignoreCase = true)
                        }.reordered())
            }

    private fun onReloadDirectoryVideos(videos: List<Video>?) {
        if (videos == null || videos.isEmpty()) {
            if (mVideos.isNotEmpty()) {
                mVideos.clear()
                mAdapter.notifyDataSetChanged()
                if (mVideoOptionsFrame.visibility == View.VISIBLE) {
                    hideMultiselectVideoControls()
                }
            }
        } else
            if (videos.size == mVideos.size) {
                var changedIndices: MutableList<Int>? = null
                for (i in videos.indices) {
                    if (!videos[i].allEqual(mVideos[i])) {
                        if (changedIndices == null) changedIndices = LinkedList()
                        changedIndices.add(i)
                    }
                }
                if (changedIndices != null) {
                    for (index in changedIndices) {
                        mVideos[index] = videos[index]
                        mAdapter.notifyItemChanged(index) // without payload
                    }
                    if (mVideoOptionsFrame.visibility == View.VISIBLE) {
                        hideMultiselectVideoControls()
                    }
                }
            } else {
                mVideos.set(videos)
                mAdapter.notifyDataSetChanged()
                if (mVideoOptionsFrame.visibility == View.VISIBLE) {
                    hideMultiselectVideoControls()
                }
            }
    }

    override fun onRefresh() {
        if (mVideoOpCallback?.isAsyncDeletingItems == true) { // 用户下拉刷新时，还有视频在被异步删除...
            mInteractionCallback.isRefreshLayoutRefreshing = false
            return
        }

        // 用户长按列表时可能又在下拉刷新，多选窗口会被弹出，需要隐藏
        if (mVideoOptionsFrame.visibility == View.VISIBLE) {
            hideMultiselectVideoControls()
        }

        mModel?.startLoader()
    }

    private inner class VideoListAdapter : RecyclerView.Adapter<VideoListAdapter.ViewHolder>() {

        override fun getItemCount() = mVideos.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.video_list_item_video, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
            } else {
                val video = mVideos[position]

                val payload = payloads[0] as Int
                if (payload and PAYLOAD_CHANGE_ITEM_LPS_AND_BG != 0) {
                    separateToppedItemsFromUntoppedOnes(holder, position)
                }
                if (payload and PAYLOAD_CHANGE_CHECKBOX_VISIBILITY != 0) {
                    holder.checkBox.visibility = mVideoOptionsFrame.visibility
                }
                if (payload and PAYLOAD_REFRESH_CHECKBOX != 0) {
                    holder.checkBox.isChecked = video.isChecked

                } else if (payload and PAYLOAD_REFRESH_CHECKBOX_WITH_ANIMATOR != 0) {
                    holder.checkBox.setChecked(video.isChecked, true)
                }
                if (payload and PAYLOAD_REFRESH_ITEM_NAME != 0) {
                    holder.videoNameText.text = video.name
                }
                if (payload and PAYLOAD_REFRESH_VIDEO_PROGRESS_DURATION != 0) {
                    holder.videoProgressAndDurationText.text =
                            VideoUtils2.concatVideoProgressAndDuration(video.progress, video.duration)
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.itemVisibleFrame.tag = position
            holder.checkBox.tag = position
            holder.topButton.tag = position
            holder.deleteButton.tag = position

            separateToppedItemsFromUntoppedOnes(holder, position)

            val video = mVideos[position]
            holder.checkBox.run {
                visibility = mVideoOptionsFrame.visibility
                isChecked = video.isChecked
            }
            VideoUtils2.loadVideoThumbIntoFragmentImageView(
                    this@LocalFoldedVideosFragment, holder.videoImage, video)
            holder.videoNameText.text = video.name
            holder.videoSizeText.text = FileUtils2.formatFileSize(video.size.toDouble())
            holder.videoProgressAndDurationText.text =
                    VideoUtils2.concatVideoProgressAndDuration(video.progress, video.duration)
        }

        private fun separateToppedItemsFromUntoppedOnes(holder: ViewHolder, position: Int) {
            val context = contextThemedFirst
            val lp = holder.topButton.layoutParams

            if (mVideos[position].isTopped) {
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

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val itemVisibleFrame: ViewGroup = itemView.findViewById(R.id.itemVisibleFrame)
            val checkBox: CircularCheckBox = itemView.findViewById(R.id.checkbox)
            val videoImage: ImageView = itemView.findViewById(R.id.image_video)
            val videoNameText: TextView = itemView.findViewById(R.id.text_videoName)
            val videoSizeText: TextView = itemView.findViewById(R.id.text_videoSize)
            val videoProgressAndDurationText: TextView = itemView.findViewById(R.id.text_videoProgressAndDuration)
            val topButton: TextView = itemView.findViewById(R.id.btn_top)
            val deleteButton: TextView = itemView.findViewById(R.id.btn_delete)

            init {
                itemVisibleFrame.setOnClickListener(this@LocalFoldedVideosFragment)
                checkBox.setOnClickListener(this@LocalFoldedVideosFragment)
                topButton.setOnClickListener(this@LocalFoldedVideosFragment)
                deleteButton.setOnClickListener(this@LocalFoldedVideosFragment)

                itemVisibleFrame.setOnLongClickListener(this@LocalFoldedVideosFragment)
            }
        }
    }

    interface InteractionCallback : ActionBarCallback, RefreshLayoutCallback
}
