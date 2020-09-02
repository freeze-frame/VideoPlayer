/*
 * Created on 2019/3/18 8:45 PM.
 * Copyright © 2019–2020 刘振林. All rights reserved.
 */
@file:JvmName("Consts")

package com.liuzhenlin.videos

import androidx.core.content.ContextCompat

/**
 * @author 刘振林
 */

@Suppress("SimplifyBooleanWithConstants")
@JvmField
internal val DEBUG_APP_UPDATE = BuildConfig.DEBUG && false

internal const val KEY_DIRECTORY_PATH = "directoryPath"
internal const val KEY_VIDEODIR = "videodir"
internal const val KEY_VIDEO = "video"
internal const val KEY_VIDEOS = "videos"
internal const val KEY_VIDEO_TITLE = "videoTitle"
internal const val KEY_VIDEO_TITLES = "videoTitles"
internal const val KEY_VIDEO_URIS = "videoURIs"
internal const val KEY_SELECTION = "index"

internal const val REQUEST_CODE_PLAY_VIDEO = 1
internal const val RESULT_CODE_PLAY_VIDEO = 1

internal const val REQUEST_CODE_PLAY_VIDEOS = 2
internal const val RESULT_CODE_PLAY_VIDEOS = 2

internal const val REQUEST_CODE_LOCAL_SEARCHED_VIDEOS_FRAGMENT = 3
internal const val RESULT_CODE_LOCAL_SEARCHED_VIDEOS_FRAGMENT = 3

internal const val REQUEST_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT = 4
internal const val RESULT_CODE_LOCAL_FOLDED_VIDEOS_FRAGMENT = 4

internal const val REQUEST_CODE_ADD_PICTURE = 5
internal const val RESULT_CODE_ADD_PICTURE = 5

internal const val TOLERANCE_VIDEO_DURATION = 100 // ms

const val NO_ID = -1L

const val EMPTY_STRING = ""
@JvmField
val EMPTY_STRING_ARRAY = arrayOf<String>()

private val _COLOR_SELECTOR by lazy(LazyThreadSafetyMode.NONE) { ContextCompat.getColor(App.getInstanceUnsafe()!!, R.color.selectorColor) }
@JvmField
internal val COLOR_SELECTOR = _COLOR_SELECTOR

private val _COLOR_ACCENT by lazy(LazyThreadSafetyMode.NONE) { ContextCompat.getColor(App.getInstanceUnsafe()!!, R.color.colorAccent) }
@JvmField
internal val COLOR_ACCENT = _COLOR_ACCENT
