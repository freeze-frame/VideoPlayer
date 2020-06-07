/*
 * Created on 2019/10/19 7:13 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDialog
import androidx.core.util.AtomicFile
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.liuzhenlin.floatingmenu.FloatingMenu
import com.liuzhenlin.slidingdrawerlayout.SlidingDrawerLayout
import com.liuzhenlin.texturevideoview.utils.FileUtils
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor
import com.liuzhenlin.texturevideoview.utils.URLUtils
import com.liuzhenlin.videos.*
import com.liuzhenlin.videos.model.TV
import com.liuzhenlin.videos.model.TVGroup
import com.liuzhenlin.videos.utils.UiUtils
import com.liuzhenlin.videos.utils.Utils
import com.liuzhenlin.videos.view.swiperefresh.SwipeRefreshLayout
import java.io.*
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
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

    private var mLoadTVsAsyncTask: LoadTVsAsyncTask? = null

    private var mOpenVideoLinkDialog: Dialog? = null

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

        val task = mLoadTVsAsyncTask
        if (task != null) {
            mLoadTVsAsyncTask = null
            task.cancel(false)
        }
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
        if (mLoadTVsAsyncTask == null) {
            mLoadTVsAsyncTask = LoadTVsAsyncTask(this)
                    .executeOnExecutor(ParallelThreadExecutor.getSingleton(), contextRequired) as LoadTVsAsyncTask
        }
    }

    private class LoadTVsAsyncTask(fragment: OnlineVideosFragment) : AsyncTask<Context, Unit, Array<TVGroup>?>() {

        val fragmentRef = WeakReference(fragment)

        override fun onPreExecute() {
            fragmentRef.get()?.mSwipeRefreshLayout?.isRefreshing = true
        }

        override fun doInBackground(vararg ctxs: Context): Array<TVGroup>? {
            var json: StringBuilder? = null

            val jsonDirectory = File(FileUtils.getAppCacheDir(ctxs[0]), "data/json")
            if (!jsonDirectory.exists()) {
                jsonDirectory.mkdirs()
            }
            val jsonFile = AtomicFile(File(jsonDirectory, "tvs.json"))

            var ioException: IOException? = null

            var conn: HttpURLConnection? = null
            var reader: BufferedReader? = null
            var writer: BufferedWriter? = null
            var jsonFileOut: FileOutputStream? = null
            try {
                val url = URL(LINK_TVS_JSON)
                conn = url.openConnection() as HttpURLConnection
//                    conn.connectTimeout = TIMEOUT_CONNECTION;
//                    conn.readTimeout = TIMEOUT_READ;

                reader = BufferedReader(InputStreamReader(conn.inputStream, "utf-8"))
                jsonFileOut = jsonFile.startWrite()
                writer = BufferedWriter(OutputStreamWriter(jsonFileOut, "utf-8"))
                val buffer = CharArray(1024)
                var len: Int
                while (true) {
                    if (isCancelled) return null

                    len = reader.read(buffer)
                    if (len == -1) break

                    if (json == null) {
                        json = StringBuilder(len)
                    }
                    json.append(buffer, 0, len)
                    writer.write(buffer, 0, len)
                }
                writer.flush()
                jsonFile.finishWrite(jsonFileOut)

            } catch (e: IOException) {
                json = null
                ioException = e
            } finally {
                if (writer != null) {
                    try {
                        writer.close()
                    } catch (e: IOException) {
                        //
                    }
                }
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        //
                    }
                }
                conn?.disconnect()
            }

            if (ioException != null) {
                if (jsonFileOut != null) {
                    jsonFile.failWrite(jsonFileOut)
                }

                val jsonString = try {
                    jsonFile.readFully().toString(Charsets.UTF_8)
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
                if (jsonString?.isNotEmpty() == true) {
                    json = StringBuilder(jsonString.length).append(jsonString)
                }
            }

            if (json != null) {
                return Gson().fromJson(json.toString(), object : TypeToken<Array<TVGroup>>() {}.type)
            } else {
                when (ioException) {
                    null -> return null
                    // 连接服务器超时
                    is @kotlin.Suppress("DEPRECATION") org.apache.http.conn.ConnectTimeoutException ->
                        fragmentRef.get()?.view?.let {
                            UiUtils.showUserCancelableSnackbar(it,
                                    R.string.connectionTimeout, Snackbar.LENGTH_SHORT)
                        }
                    // 读取数据超时
                    is SocketTimeoutException ->
                        fragmentRef.get()?.view?.let {
                            UiUtils.showUserCancelableSnackbar(it,
                                    R.string.readTimeout, Snackbar.LENGTH_SHORT)
                        }
                    else /*is IOException*/ -> {
                        fragmentRef.get()?.view?.let {
                            UiUtils.showUserCancelableSnackbar(it,
                                    R.string.refreshError, Snackbar.LENGTH_SHORT)
                        }
                        ioException.printStackTrace()
                    }
                }
                cancel()
            }

            return null
        }

        private fun cancel() {
            fragmentRef.get()?.mLoadTVsAsyncTask = null
            cancel(false)
        }

        override fun onCancelled(result: Array<TVGroup>?) {
            fragmentRef.get()?.run {
                if (mLoadTVsAsyncTask == null) {
                    mSwipeRefreshLayout.isRefreshing = false
                }
            }
        }

        override fun onPostExecute(tvGroups: Array<TVGroup>?) {
            fragmentRef.get()?.run {
                mSwipeRefreshLayout.isRefreshing = false
                mLoadTVsAsyncTask = null

                for (i in 0 until mTvListAdapter.groupCount) {
                    mTvList.collapseGroup(i)
                }
                if (!Arrays.equals(tvGroups, mTvListAdapter.tvGroups)) {
                    mTvListAdapter.tvGroups = tvGroups
                    mTvListAdapter.notifyDataSetChanged()
                }
            }
        }
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

        override fun hasStableIds() = true

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {

            val groupView: View
            if (convertView == null) {
                groupView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_tv_group, parent, false)
                groupView.tag = GroupHolder(groupView)
            } else {
                groupView = convertView
            }

            val tvGroup = tvGroups!![groupPosition]

            val groupHolder = groupView.tag as GroupHolder
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

            val childView: View
            if (convertView == null) {
                childView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_tv, parent, false)
                childView.tag = ChildHolder(childView)
            } else {
                childView = convertView
            }

            val child = tvGroups!![groupPosition].tVs[childPosition]

            val childHolder = childView.tag as ChildHolder
            childHolder.nameText.text = child.name
            // 高亮长按后选中的childView
            if (child === selectedTV) {
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

        inner class GroupHolder(groupView: View) {
            val nameText: TextView = groupView.findViewById(R.id.text_tvGroupName)
            val childCountText: TextView = groupView.findViewById(R.id.text_childCount)
        }

        inner class ChildHolder(childView: View) {
            val nameText: TextView = childView.findViewById(R.id.text_tvName)
        }
    }

    private companion object {
        const val LINK_TVS_JSON = "https://gitee.com/lzl_s/Videos-Server/raw/master/tvs.json"
    }
}
