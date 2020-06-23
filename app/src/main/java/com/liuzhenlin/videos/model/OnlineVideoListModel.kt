/*
 * Created on 2020-6-18 10:49:58 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.util.AtomicFile
import com.google.gson.Gson
import com.liuzhenlin.texturevideoview.InternalConsts
import com.liuzhenlin.texturevideoview.utils.FileUtils
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor
import com.liuzhenlin.videos.bean.TVGroup
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author 刘振林
 */
class OnlineVideoListModel(context: Context) : BaseModel<Array<TVGroup>?>(context) {

    @Volatile
    private var mLoader: LoadTVsAsyncTask? = null

    override fun startLoader() {
        // 不在加载时才加载
        if (mLoader == null) {
            val loader = LoadTVsAsyncTask()
            mLoader = loader
            loader.executeOnExecutor(ParallelThreadExecutor.getSingleton(), mContext)
        }
    }

    override fun stopLoader() {
        val loader = mLoader
        if (loader != null) {
            mLoader = null
            loader.cancel()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadTVsAsyncTask : Loader<Context, Unit>() {
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
//                conn.connectTimeout = TIMEOUT_CONNECTION;
//                conn.readTimeout = TIMEOUT_READ;

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
                return Gson().fromJson(json.toString(), Array<TVGroup>::class.java)
            } else {
                if (ioException != null) {
                    InternalConsts.getMainThreadHandler().post {
                        mOnLoadListeners?.let {
                            for (listener in it.toTypedArray()) {
                                listener.onLoadError(ioException)
                            }
                        }
                    }
                }
                cancel()
            }

            return null
        }

        fun cancel() {
            mLoader = null
            cancel(false)
        }

        override fun onCancelled(result: Array<TVGroup>?) {
            if (mLoader == null) {
                super.onCancelled(result)
            }
        }

        override fun onPostExecute(result: Array<TVGroup>?) {
            mLoader = null
            super.onPostExecute(result)
        }
    }

    private companion object {
        const val LINK_TVS_JSON = "https://gitee.com/lzl_s/Videos-Server/raw/master/tvs.json"
    }
}