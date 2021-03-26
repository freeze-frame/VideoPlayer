/*
 * Created on 2019/10/19 7:08 PM.
 * Copyright © 2019–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.liuzhenlin.common.view.SwipeRefreshLayout
import com.liuzhenlin.slidingdrawerlayout.SlidingDrawerLayout
import com.liuzhenlin.swipeback.SwipeBackLayout
import com.liuzhenlin.videos.*

/**
 * @author 刘振林
 */
class LocalVideosFragment : Fragment(), ILocalVideosFragment, FragmentPartLifecycleCallback,
        LocalFoldedVideosFragment.InteractionCallback, LocalSearchedVideosFragment.InteractionCallback,
        View.OnClickListener, SwipeBackLayout.SwipeListener, SlidingDrawerLayout.OnDrawerScrollListener {

    private lateinit var mInteractionCallback: InteractionCallback

    private lateinit var mLocalVideoListFragment: LocalVideoListFragment
    private var mLocalFoldedVideosFragment: LocalFoldedVideosFragment? = null
    private var mLocalSearchedVideosFragment: LocalSearchedVideosFragment? = null

    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private var mSwipeBackScrollPercent = 0.0f

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        mInteractionCallback = when {
            parent is InteractionCallback -> parent
            context is InteractionCallback -> context
            parent != null -> throw RuntimeException("Neither $context nor $parent " +
                    "has implemented LocalVideosFragment.InteractionCallback")
            else -> throw RuntimeException(
                    "$context must implement LocalVideosFragment.InteractionCallback")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contentView = inflater.inflate(R.layout.fragment_local_videos, container, false)
        initViews(contentView)
        return contentView
    }

    private fun initViews(contentView: View) {
        mSwipeRefreshLayout = contentView.findViewById(R.id.swipeRefreshLayout)
        mSwipeRefreshLayout.setColorSchemeResources(R.color.pink, R.color.lightBlue, R.color.purple)
        mSwipeRefreshLayout.setOnRequestDisallowInterceptTouchEventCallback { true }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            mLocalVideoListFragment = LocalVideoListFragment()
            childFragmentManager.beginTransaction()
                    .add(R.id.container_child_fragments, mLocalVideoListFragment, TAG_LOCAL_VIDEO_LIST_FRAGMENT)
                    .commit()
        } else {
            val fm = childFragmentManager

            mLocalVideoListFragment = fm
                    .findFragmentByTag(TAG_LOCAL_VIDEO_LIST_FRAGMENT) as LocalVideoListFragment
            onFragmentAttached(mLocalVideoListFragment)

            mLocalFoldedVideosFragment = fm
                    .findFragmentByTag(TAG_LOCAL_FOLDED_VIDEOS_FRAGMENT) as LocalFoldedVideosFragment?
            if (mLocalFoldedVideosFragment != null) {
                onFragmentAttached(mLocalFoldedVideosFragment!!)
            }

            mLocalSearchedVideosFragment = fm
                    .findFragmentByTag(TAG_LOCAL_SEARCHED_VIDEOS_FRAGMENT) as LocalSearchedVideosFragment?
            if (mLocalSearchedVideosFragment != null) {
                onFragmentAttached(mLocalSearchedVideosFragment!!)
            }
        }
    }

    override fun onFragmentAttached(childFragment: Fragment) {
        // this::mLocalVideoListFragment.isInitialized用于判断我们是否已经初始化了子Fragment属性
        // 在Activity被系统销毁后自动创建时，子Fragment属性还未在此类的onViewCreated()方法中被初始化，
        // 但子Fragments已经attach到此Fragment了
        if (!this::mLocalVideoListFragment.isInitialized) return

        when {
            childFragment === mLocalVideoListFragment -> {
                mSwipeRefreshLayout.setOnRefreshListener(childFragment)
            }
            childFragment === mLocalFoldedVideosFragment -> {
                childFragment.setVideoOpCallback(mLocalVideoListFragment)
                mLocalVideoListFragment.model.addOnReloadVideosListener(childFragment)
                mSwipeRefreshLayout.setOnRefreshListener(childFragment)

                mInteractionCallback.setSideDrawerEnabled(false)
                mInteractionCallback.onLocalFoldedVideosFragmentAttached()
                mInteractionCallback.showTabItems(false)
                mInteractionCallback.setTabItemsEnabled(false)
            }
            childFragment === mLocalSearchedVideosFragment -> {
                childFragment.setVideoOpCallback(mLocalVideoListFragment)
                mLocalVideoListFragment.model.addOnReloadVideosListener(childFragment)
                mSwipeRefreshLayout.setOnRefreshListener(childFragment)

                mInteractionCallback.setSideDrawerEnabled(false)
                mInteractionCallback.onLocalSearchedVideosFragmentAttached()
                mInteractionCallback.showTabItems(false)
            }
        }
    }

    override fun onFragmentViewCreated(childFragment: Fragment) {
        if (childFragment === mLocalFoldedVideosFragment) {
            childFragment.swipeBackLayout.addSwipeListener(this)
        }
    }

    override fun onFragmentViewDestroyed(childFragment: Fragment) {}

    override fun onFragmentDetached(childFragment: Fragment) {
        when {
            childFragment === mLocalFoldedVideosFragment -> {
                mLocalVideoListFragment.model.removeOnReloadVideosListener(childFragment)
                mLocalFoldedVideosFragment = null
                mSwipeRefreshLayout.setOnRefreshListener(mLocalVideoListFragment)

                mInteractionCallback.setSideDrawerEnabled(true)
                mInteractionCallback.onLocalFoldedVideosFragmentDetached()
                mInteractionCallback.setTabItemsEnabled(true)
//                mInteractionCallback.showTabItems(true)
            }
            childFragment === mLocalSearchedVideosFragment -> {
                mLocalVideoListFragment.model.removeOnReloadVideosListener(childFragment)
                mLocalSearchedVideosFragment = null
                mSwipeRefreshLayout.setOnRefreshListener(mLocalVideoListFragment)

                mInteractionCallback.setSideDrawerEnabled(true)
                mInteractionCallback.onLocalSearchedVideosFragmentDetached()
                mInteractionCallback.showTabItems(true)
            }
        }
    }

    override fun goToLocalFoldedVideosFragment(args: Bundle) {
        mLocalFoldedVideosFragment = LocalFoldedVideosFragment()
        mLocalFoldedVideosFragment!!.arguments = args
        mLocalFoldedVideosFragment!!.setTargetFragment(mLocalVideoListFragment,
                REQUEST_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT)

        childFragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.anim_open_enter, R.anim.anim_open_exit,
                        R.anim.anim_close_enter, R.anim.anim_close_exit)
                .hide(mLocalVideoListFragment)
                .add(R.id.container_child_fragments, mLocalFoldedVideosFragment!!, TAG_LOCAL_FOLDED_VIDEOS_FRAGMENT)
                .addToBackStack(TAG_LOCAL_FOLDED_VIDEOS_FRAGMENT)
                .commit()
    }

    private fun goToLocalSearchedVideosFragment() {
        val args = Bundle()
        args.putParcelableArrayList(KEY_VIDEOS, mLocalVideoListFragment.allVideos)

        mLocalSearchedVideosFragment = LocalSearchedVideosFragment()
        mLocalSearchedVideosFragment!!.arguments = args
        mLocalSearchedVideosFragment!!.setTargetFragment(mLocalVideoListFragment,
                REQUEST_CODE_LOCAL_SEARCHED_VIDEOS_FRAGMENT)

        childFragmentManager.beginTransaction()
                .add(R.id.container_child_fragments, mLocalSearchedVideosFragment!!, TAG_LOCAL_SEARCHED_VIDEOS_FRAGMENT)
                .addToBackStack(TAG_LOCAL_SEARCHED_VIDEOS_FRAGMENT)
                .commit()
    }

    override fun onBackPressed(): Boolean {
        if (mLocalFoldedVideosFragment != null) {
            if (!mLocalFoldedVideosFragment!!.onBackPressed()) {
                mLocalFoldedVideosFragment!!.swipeBackLayout.scrollToFinishActivityOrPopUpFragment()
            }
            return true
        }
        return if (mLocalSearchedVideosFragment != null) {
            childFragmentManager.popBackStackImmediate()
        } else {
            mLocalVideoListFragment.onBackPressed()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_search -> goToLocalSearchedVideosFragment()
        }
    }

    override fun getActionBar(fragment: Fragment): ViewGroup =
            mInteractionCallback.getActionBar(
                    fragment === mLocalFoldedVideosFragment
                            || fragment === mLocalSearchedVideosFragment)

    override fun isRefreshLayoutEnabled() = mSwipeRefreshLayout.isEnabled

    override fun setRefreshLayoutEnabled(enabled: Boolean) {
        mSwipeRefreshLayout.isEnabled = enabled
    }

    override fun isRefreshLayoutRefreshing() = mSwipeRefreshLayout.isRefreshing

    override fun setRefreshLayoutRefreshing(refreshing: Boolean) {
        mSwipeRefreshLayout.isRefreshing = refreshing
    }

    override fun setOnRefreshLayoutChildScrollUpCallback(callback: SwipeRefreshLayout.OnChildScrollUpCallback?) {
        mSwipeRefreshLayout.setOnChildScrollUpCallback(callback)
    }

    override fun onScrollStateChange(edge: Int, state: Int) {
        when (state) {
            SwipeBackLayout.STATE_DRAGGING,
            SwipeBackLayout.STATE_SETTLING -> {
                mInteractionCallback.showActionBar(true)
                mInteractionCallback.showTabItems(true)
            }
            SwipeBackLayout.STATE_IDLE ->
                if (mSwipeBackScrollPercent == 0.0f) {
                    mInteractionCallback.showActionBar(false)
                    mInteractionCallback.showTabItems(false)
                }
        }
    }

    override fun onScrollPercentChange(edge: Int, percent: Float) {
        mSwipeBackScrollPercent = percent
        mInteractionCallback.setActionBarAlpha(percent)
        mInteractionCallback.setTmpActionBarAlpha(1 - percent)
    }

    override fun onDrawerOpened(parent: SlidingDrawerLayout, drawer: View) {
    }

    override fun onDrawerClosed(parent: SlidingDrawerLayout, drawer: View) {
    }

    override fun onScrollPercentChange(parent: SlidingDrawerLayout, drawer: View, percent: Float) {
        if (view == null) return
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

    private companion object {
        const val TAG_LOCAL_VIDEO_LIST_FRAGMENT = "LocalVideoListFragment"
        const val TAG_LOCAL_FOLDED_VIDEOS_FRAGMENT = "LocalFoldedVideosFragment"
        const val TAG_LOCAL_SEARCHED_VIDEOS_FRAGMENT = "LocalSearchedVideosFragment"
    }

    interface InteractionCallback : LocalVideoListFragment.InteractionCallback {
        fun getActionBar(tmp: Boolean): ViewGroup
        fun showActionBar(show: Boolean)
        fun setActionBarAlpha(alpha: Float)
        fun setTmpActionBarAlpha(alpha: Float)

        fun showTabItems(show: Boolean)
        fun setTabItemsEnabled(enabled: Boolean)

        fun onLocalSearchedVideosFragmentAttached()
        fun onLocalSearchedVideosFragmentDetached()

        fun onLocalFoldedVideosFragmentAttached()
        fun onLocalFoldedVideosFragmentDetached()
    }
}
