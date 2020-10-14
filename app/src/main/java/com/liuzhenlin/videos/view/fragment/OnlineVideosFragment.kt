/*
 * Created on 2019/10/19 7:13 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.liuzhenlin.floatingmenu.FloatingMenu
import com.liuzhenlin.slidingdrawerlayout.SlidingDrawerLayout
import com.liuzhenlin.texturevideoview.utils.URLUtils
import com.liuzhenlin.videos.COLOR_SELECTOR
import com.liuzhenlin.videos.R
import com.liuzhenlin.videos.bean.TV
import com.liuzhenlin.videos.bean.TVGroup
import com.liuzhenlin.videos.contextThemedFirst
import com.liuzhenlin.videos.get
import com.liuzhenlin.videos.model.OnLoadListener
import com.liuzhenlin.videos.model.OnlineVideoListModel
import com.liuzhenlin.videos.utils.UiUtils
import com.liuzhenlin.videos.utils.Utils
import com.liuzhenlin.videos.view.swiperefresh.SwipeRefreshLayout
import java.net.SocketTimeoutException
import java.util.*

/**
 * @author 刘振林
 */
class OnlineVideosFragment : Fragment(), View.OnClickListener,
        SlidingDrawerLayout.OnDrawerScrollListener {

    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private lateinit var mTvList: ExpandableListView
    private lateinit var mTvListAdapter: TvListAdapter
    private var mDownX = 0
    private var mDownY = 0

    private lateinit var mModel: OnlineVideoListModel

    private var mOpenVideoLinkDialog: Dialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mModel = OnlineVideoListModel(context)
        mModel.addOnLoadListener(object : OnLoadListener<Nothing, Array<TVGroup>?> {
            override fun onLoadStart() {
                mSwipeRefreshLayout.isRefreshing = true
            }

            override fun onLoadFinish(result: Array<TVGroup>?) {
                mSwipeRefreshLayout.isRefreshing = false
                for (i in 0 until mTvListAdapter.groupCount) {
                    mTvList.collapseGroup(i)
                }
                if (!Arrays.equals(result, mTvListAdapter.tvGroups)) {
                    mTvListAdapter.tvGroups = result
                    mTvListAdapter.notifyDataSetChanged()
                }
            }

            override fun onLoadCanceled() {
                mSwipeRefreshLayout.isRefreshing = false
            }

            override fun onLoadError(cause: Throwable) {
                mSwipeRefreshLayout.isRefreshing = false
                when (cause) {
                    // 连接服务器超时
                    is @kotlin.Suppress("DEPRECATION") org.apache.http.conn.ConnectTimeoutException ->
                        UiUtils.showUserCancelableSnackbar(view!!,
                                R.string.connectionTimeout, Snackbar.LENGTH_SHORT)
                    // 读取数据超时
                    is SocketTimeoutException ->
                        UiUtils.showUserCancelableSnackbar(view!!,
                                R.string.readTimeout, Snackbar.LENGTH_SHORT)
                    else -> {
                        UiUtils.showUserCancelableSnackbar(view!!,
                                R.string.refreshError, Snackbar.LENGTH_SHORT)
                        cause.printStackTrace()
                    }
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_online_videos, container, false)
        initViews(view)
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews(contentView: View) {
        mSwipeRefreshLayout = contentView.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout.setColorSchemeResources(R.color.pink, R.color.lightBlue, R.color.purple)
//        mSwipeRefreshLayout.setOnRequestDisallowInterceptTouchEventCallback { true }
        mSwipeRefreshLayout.setOnRefreshListener {
            loadTvs()
        }

        mTvListAdapter = TvListAdapter()

        mTvList = contentView.findViewById(R.id.list_tv)
        mTvList.setAdapter(mTvListAdapter)
        mTvList.setOnChildClickListener(mTvListAdapter)
        mTvList.onItemLongClickListener = mTvListAdapter
        mTvList.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                mDownX = event.rawX.toInt()
                mDownY = event.rawY.toInt()
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mOpenVideoLinkDialog?.dismiss()

        mModel.stopLoader()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_link -> showOpenVideoLinkDialog()

            R.id.btn_cancel_openVideoLinkDialog -> mOpenVideoLinkDialog!!.cancel()
            R.id.btn_ok_openVideoLinkDialog -> {
                val linkTil = mOpenVideoLinkDialog!!.window!!.decorView.tag as TextInputLayout
                val link = linkTil.editText!!.text.trim().toString()
                if (link.isEmpty()) {
                    Toast.makeText(v.context,
                            R.string.pleaseInputVideoLinkFirst, Toast.LENGTH_SHORT).show()
                    return
                }
                if (link.matches(URLUtils.PATTERN_WEB_URL.toRegex())) {
                    v.context.playVideo(link,
                            (linkTil.tag as TextInputLayout).editText!!.text.trim().toString()
                                    .run {
                                        if (isEmpty()) null else this
                                    })
                } else {
                    Toast.makeText(v.context, R.string.illegalInputLink, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showOpenVideoLinkDialog() {
        val dialog = AppCompatDialog(contextThemedFirst, R.style.DialogStyle_MinWidth_NoTitle)
        dialog.setContentView(R.layout.dialog_open_video_link)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        val til_videoTitle = dialog.findViewById<TextInputLayout>(R.id.textinput_videoTitle)!!
        til_videoTitle.postDelayed({ UiUtils.showSoftInput(til_videoTitle) }, 256)

        val til_videoLink = dialog.findViewById<TextInputLayout>(R.id.textinput_videoLink)!!
        til_videoLink.tag = til_videoTitle

        dialog.findViewById<View>(R.id.btn_cancel_openVideoLinkDialog)!!.setOnClickListener(this)
        dialog.findViewById<View>(R.id.btn_ok_openVideoLinkDialog)!!.setOnClickListener(this)
        dialog.window!!.decorView.tag = til_videoLink
        dialog.setOnDismissListener { mOpenVideoLinkDialog = null }
        mOpenVideoLinkDialog = dialog
    }

    override fun onDrawerOpened(parent: SlidingDrawerLayout, drawer: View) {
    }

    override fun onDrawerClosed(parent: SlidingDrawerLayout, drawer: View) {
    }

    override fun onScrollPercentChange(parent: SlidingDrawerLayout, drawer: View, percent: Float) {
    }

    override fun onScrollStateChange(parent: SlidingDrawerLayout, drawer: View, state: Int) {
        if (view == null) return

        when (state) {
            SlidingDrawerLayout.SCROLL_STATE_TOUCH_SCROLL,
            SlidingDrawerLayout.SCROLL_STATE_AUTO_SCROLL -> {
                mSwipeRefreshLayout[0] /* fragment container */
                        .setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            SlidingDrawerLayout.SCROLL_STATE_IDLE -> {
                mSwipeRefreshLayout[0] /* fragment container */
                        .setLayerType(View.LAYER_TYPE_NONE, null)
            }
        }
    }

    private fun loadTvs() {
        mModel.startLoader()
    }

    private inner class TvListAdapter : BaseExpandableListAdapter(),
            ExpandableListView.OnChildClickListener, AdapterView.OnItemLongClickListener {

        var tvGroups: Array<TVGroup>? = null
        var selectedTV: TV? = null

        init {
            loadTvs()
        }

        override fun getGroup(groupPosition: Int) = tvGroups!![groupPosition]

        override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true

        override fun hasStableIds() = false

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
            val groupHolder: GroupViewHolder
            val groupView: View

            if (convertView == null) {
                groupHolder = GroupViewHolder(parent)
                groupView = groupHolder.groupView
                groupView.tag = groupHolder
            } else {
                groupView = convertView
                groupHolder = groupView.tag as GroupViewHolder
            }

            val tvGroup = tvGroups!![groupPosition]
            groupHolder.nameText.text = tvGroup.name
            groupHolder.childCountText.text = tvGroup.tVs.size.toString()

            return groupView
        }

        override fun getChildrenCount(groupPosition: Int) =
                tvGroups!![groupPosition].tVs.size

        override fun getChild(groupPosition: Int, childPosition: Int) =
                tvGroups!![groupPosition].tVs[childPosition]!!

        override fun getGroupId(groupPosition: Int): Long =
                groupPosition.toLong()

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
            val childHolder: ChildViewHolder
            val childView: View

            if (convertView == null) {
                childHolder = ChildViewHolder(parent)
                childView = childHolder.childView
                childView.tag = childHolder
            } else {
                childView = convertView
                childHolder = childView.tag as ChildViewHolder
            }

            val tv = tvGroups!![groupPosition].tVs[childPosition]
            childHolder.nameText.text = tv.name
            // 高亮长按后选中的childView
            if (tv === selectedTV) {
                childView.setBackgroundColor(COLOR_SELECTOR)
            } else {
                ViewCompat.setBackground(childView, null)
            }

            return childView
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long =
                childPosition.toLong()

        override fun getGroupCount() = tvGroups?.size ?: 0

        override fun onChildClick(parent: ExpandableListView, v: View, groupPosition: Int, childPosition: Int, id: Long): Boolean {
            val tvs = tvGroups!![groupPosition].tVs
            val tvNames = arrayOfNulls<String>(tvs.size)
            val tvUrls = arrayOfNulls<String>(tvs.size)
            for (i in tvs.indices) {
                tvNames[i] = tvs[i].name
                tvUrls[i] = tvs[i].url
            }
            @Suppress("UNCHECKED_CAST")
            v.context.playVideos(tvUrls as Array<String>, tvNames, childPosition)
            return true
        }

        override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
            if (ExpandableListView.getPackedPositionType(id)
                    == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                val packedPos = (parent as ExpandableListView).getExpandableListPosition(position)
                val groupPosition = ExpandableListView.getPackedPositionGroup(packedPos)
                val childPosition = ExpandableListView.getPackedPositionChild(packedPos)
                val child = tvGroups!![groupPosition].tVs[childPosition]

                selectedTV = child
                getChildView(groupPosition, childPosition, false /* ignored */, view, parent)

                val fm = FloatingMenu(view)
                fm.items(getString(R.string.copyURL))
                fm.show(mDownX, mDownY)
                fm.setOnItemClickListener { _, _ ->
                    Utils.copyPlainTextToClipboard(parent.context, child.name, child.url)
                }
                fm.setOnDismissListener {
                    selectedTV = null
                    getChildView(groupPosition, childPosition, false /* ignored */, view, parent)
                }

                return true
            }
            return false
        }

        inner class GroupViewHolder(adapterView: ViewGroup) {
            val groupView: View =
                    LayoutInflater.from(adapterView.context)
                            .inflate(R.layout.item_tv_group, adapterView, false)
            val nameText: TextView = groupView.findViewById(R.id.text_tvGroupName)
            val childCountText: TextView = groupView.findViewById(R.id.text_childCount)
        }

        inner class ChildViewHolder(adapterView: ViewGroup) {
            val childView: View =
                    LayoutInflater.from(adapterView.context)
                            .inflate(R.layout.item_tv, adapterView, false)
            val nameText: TextView = childView.findViewById(R.id.text_tvName)
        }
    }
}
