/*
 * Created on 2020-3-30 6:44:29 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.util.ObjectsCompat;

import com.bumptech.glide.util.Synthetic;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor;
import com.liuzhenlin.texturevideoview.utils.Singleton;
import com.liuzhenlin.videos.App;
import com.liuzhenlin.videos.BuildConfig;
import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.R;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 刘振林
 */
@MainThread
public final class MergeAppUpdateChecker {

    public interface OnResultListener {
        void onResult(boolean findNewVersion);
    }

    private static final String TAG = "AppUpdateChecker";

    private static final int TIMEOUT_CONNECTION = 10 * 1000; // ms
    private static final int TIMEOUT_READ = 30 * 1000; // ms

    private static final String LINK_APP_INFOS =
            "https://gitee.com/lzl_s/Videos-Server/raw/master/app/Android/app.json";

    @Synthetic String mAppName;
    @Synthetic String mVersionName;
    @Synthetic String[] mAppPartLinks;
    @Synthetic int mAppLength;
    @Synthetic String mAppSha1;
    @Synthetic StringBuilder mUpdateLog;
    @Synthetic String mPromptDialogAnchorActivityClsName;

    @Synthetic final Context mContext;
    @Synthetic final H mH;
    @Synthetic boolean mToastResult;
    private boolean mCheckInProgress;

    @Synthetic List<OnResultListener> mListeners;

    private static final String EXTRA_APP_NAME = "extra_appName";
    private static final String EXTRA_VERSION_NAME = "extra_versionName";
    private static final String EXTRA_APP_PART_LINKS = "extra_appPartLinks";
    private static final String EXTRA_APP_LENGTH = "extra_appLength";
    private static final String EXTRA_APP_SHA1 = "extra_appSha1";
    @Synthetic Intent mServiceIntent;

    private static final Singleton<Context, MergeAppUpdateChecker> sMergeAppUpdateCheckerSingleton =
            new Singleton<Context, MergeAppUpdateChecker>() {
                @SuppressLint("SyntheticAccessor")
                @NonNull
                @Override
                protected MergeAppUpdateChecker onCreate(Context... ctxs) {
                    return new MergeAppUpdateChecker(ctxs[0]);
                }
            };

    @NonNull
    public static MergeAppUpdateChecker getSingleton(@NonNull Context context) {
        return sMergeAppUpdateCheckerSingleton.get(context);
    }

    private MergeAppUpdateChecker(Context context) {
        mContext = context.getApplicationContext();
        mH = new H();
    }

    @Synthetic boolean hasOnResultListener() {
        return mListeners != null && !mListeners.isEmpty();
    }

    // 注：添加后请记得移除，以免引起内存泄漏
    public void addOnResultListener(@Nullable OnResultListener listener) {
        if (listener != null) {
            if (mListeners == null) {
                mListeners = new ArrayList<>(1);
            }
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    public void removeOnResultListener(@Nullable OnResultListener listener) {
        if (listener != null && hasOnResultListener()) {
            mListeners.remove(listener);
        }
    }

    public void checkUpdate() {
        checkUpdate(true);
    }

    @SuppressLint("StaticFieldLeak")
    public void checkUpdate(boolean toastResult) {
        mToastResult = toastResult;

        if (mCheckInProgress) return;
        mCheckInProgress = true;
        new AsyncTask<Void, Void, Integer>() {
            static final int RESULT_FIND_NEW_VERSION = 1;
            static final int RESULT_NO_NEW_VERSION = 2;
            static final int RESULT_CONNECTION_TIMEOUT = 3;
            static final int RESULT_READ_TIMEOUT = 4;

            @Override
            protected Integer doInBackground(Void... voids) {
                StringBuilder json = null;

                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL(LINK_APP_INFOS);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(TIMEOUT_CONNECTION);
                    conn.setReadTimeout(TIMEOUT_READ);

                    //noinspection CharsetObjectCanBeUsed
                    reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "utf-8"));
                    final char[] buffer = new char[1024];
                    int len;
                    while ((len = reader.read(buffer)) != -1) {
                        if (json == null) {
                            json = new StringBuilder(len);
                        }
                        json.append(buffer, 0, len);
                    }

                    // 连接服务器超时
                } catch (ConnectTimeoutException e) {
                    return RESULT_CONNECTION_TIMEOUT;

                    // 读取数据超时
                } catch (SocketTimeoutException e) {
                    return RESULT_READ_TIMEOUT;

                } catch (IOException e) {
                    e.printStackTrace();
                    return 0;

                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            //
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                //noinspection ConstantConditions
                JsonObject appInfos = JsonParser.parseString(json.toString()).getAsJsonObject()
                        .get("appInfos").getAsJsonObject();

                final boolean findNewVersion = Consts.DEBUG_APP_UPDATE
                        || appInfos.get("versionCode").getAsInt() > BuildConfig.VERSION_CODE;
                // 检测到版本更新
                if (findNewVersion) {
                    mAppName = appInfos.get("appName").getAsString();
                    mVersionName = appInfos.get("versionName").getAsString();

                    JsonArray links = appInfos.get("appPartLinks").getAsJsonArray();
                    mAppPartLinks = new String[links.size()];
                    for (int i = 0; i < mAppPartLinks.length; i++) {
                        mAppPartLinks[i] = links.get(i).getAsString();
                    }

                    mAppLength = appInfos.get("appLength").getAsInt();
                    mAppSha1 = appInfos.get("appSha1").getAsString();

                    mUpdateLog = new StringBuilder();
                    for (JsonElement log : appInfos.get("updateLogs").getAsJsonArray()) {
                        mUpdateLog.append(log.getAsString()).append("\n");
                    }
                    mUpdateLog.deleteCharAt(mUpdateLog.length() - 1);

                    mPromptDialogAnchorActivityClsName =
                            appInfos.get("promptDialogAnchorActivityClsName").getAsString();
                }
                return findNewVersion ? RESULT_FIND_NEW_VERSION : RESULT_NO_NEW_VERSION;
            }

            @Override
            protected void onPostExecute(Integer result) {
                switch (result) {
                    case RESULT_FIND_NEW_VERSION:
                        mH.sendEmptyMessage(H.MSG_FIND_NEW_VERSION);
                        showUpdatePromptDialog();
                        break;
                    case RESULT_NO_NEW_VERSION:
                        mH.sendEmptyMessage(H.MSG_NO_NEW_VERSION);
                        if (mToastResult) {
                            Toast.makeText(mContext,
                                    R.string.isTheLatestVersion, Toast.LENGTH_SHORT).show();
                        }
                        reset();
                        break;
                    case RESULT_CONNECTION_TIMEOUT:
                        if (mToastResult) {
                            Toast.makeText(mContext,
                                    R.string.connectionTimeout, Toast.LENGTH_SHORT).show();
                        }
                        reset();
                        break;
                    case RESULT_READ_TIMEOUT:
                        if (mToastResult) {
                            Toast.makeText(mContext,
                                    R.string.readTimeout, Toast.LENGTH_SHORT).show();
                        }
                    default:
                        reset();
                        break;
                }
            }
        }.executeOnExecutor(ParallelThreadExecutor.getSingleton());
    }

    /**
     * 弹出对话框，提醒用户更新
     */
    @Synthetic void showUpdatePromptDialog() {
        Activity anchorActivity = ActivityUtils.getActivityForName(mPromptDialogAnchorActivityClsName);
        if (anchorActivity == null || anchorActivity.isFinishing()) {
            Log.w(TAG, "The Activity in which the dialog should run does'nt exist, " +
                    "so it will not be showed at all and this check finishes.");
            reset();
            return;
        }

        final Dialog dialog = new AppCompatDialog(anchorActivity, R.style.DialogStyle_MinWidth_NoTitle);

        View view = View.inflate(anchorActivity, R.layout.dialog_app_update_prompt, null);

        view.<TextView>findViewById(R.id.text_updateLogTitle)
                .setText(mContext.getString(R.string.updateLog, mAppName, mVersionName));

        final TextView tv = view.findViewById(R.id.text_updateLog);
        tv.setText(mUpdateLog);
        tv.post(new Runnable() {
            @Override
            public void run() {
                TextViewUtils.setHangingIndents(tv, 4);
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    // 当点确定按钮时从服务器上下载新的apk，然后安装
                    case R.id.btn_confirm:
                        dialog.cancel();
                        if (FileUtils2.isExternalStorageMounted()) {
                            if (FileUtils2.hasEnoughStorageOnDisk(mAppLength)) {
                                mServiceIntent = new Intent(mContext, UpdateAppService.class)
                                        .putExtra(EXTRA_APP_NAME, mAppName)
                                        .putExtra(EXTRA_VERSION_NAME, mVersionName)
                                        .putExtra(EXTRA_APP_PART_LINKS, mAppPartLinks)
                                        .putExtra(EXTRA_APP_LENGTH, mAppLength)
                                        .putExtra(EXTRA_APP_SHA1, mAppSha1);
                                mContext.startService(mServiceIntent);
                            } else {
                                reset();
                                Toast.makeText(mContext, R.string.notHaveEnoughStorage,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            reset();
                            Toast.makeText(mContext, R.string.pleaseInsertSdCardOnYourDeviceFirst,
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    // 当点取消按钮时不做任何举动
                    case R.id.btn_cancel:
                        dialog.cancel();
                        reset();
                        break;
                }
            }
        };
        view.findViewById(R.id.btn_cancel).setOnClickListener(listener);
        view.findViewById(R.id.btn_confirm).setOnClickListener(listener);

//        dialog.getWindow().setType(
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
//                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Synthetic void reset() {
        mAppName = null;
        mVersionName = null;
        mAppPartLinks = null;
        mAppLength = 0;
        mAppSha1 = null;
        mUpdateLog = null;
        mPromptDialogAnchorActivityClsName = null;
        mCheckInProgress = false;
        mServiceIntent = null;
    }

    @SuppressLint("HandlerLeak")
    private final class H extends Handler {
        static final int MSG_STOP_UPDATE_APP_SERVICE = -1;
        static final int MSG_NO_NEW_VERSION = 0;
        static final int MSG_FIND_NEW_VERSION = 1;

        H() {
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            final int what = msg.what;
            switch (what) {
                case MSG_STOP_UPDATE_APP_SERVICE:
                    if (mServiceIntent != null) {
                        mContext.stopService(mServiceIntent);
                        reset();
                    }
                    break;
                case MSG_NO_NEW_VERSION:
                case MSG_FIND_NEW_VERSION:
                    if (hasOnResultListener()) {
                        for (int i = mListeners.size() - 1; i >= 0; i--) {
                            mListeners.get(i).onResult(what != MSG_NO_NEW_VERSION);
                        }
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @VisibleForTesting
    public static final class UpdateAppService extends Service {

        @SuppressLint("StaticFieldLeak")
        @Synthetic Context mContext;
        private String mPkgName;

        @Synthetic NotificationManager mNotificationManager;
        @Synthetic NotificationCompat.Builder mNotificationBuilder;
        private static final int ID_NOTIFICATION = 20200330;

        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        // We want at least 2 threads and at most 4 threads to download the new apk,
        // preferring to have 1 less than the CPU count to avoid saturating the CPU
        // with background work
        private static final int COUNT_DOWNLOAD_APP_TASK = Math.max(2, Math.min(CPU_COUNT - 1, 4));

        private ExecutorService mDownloadAppExecutor;
        @Synthetic List<DownloadAppPartTask> mDownloadAppPartTasks;
        @Synthetic String[] mAppPartLinks;
        @Synthetic File[] mApkParts;
        @Synthetic File mApk;
        @Synthetic int mApkLength;
        @Synthetic final AtomicInteger mProgress = new AtomicInteger();

        private CancelAppUpdateReceiver mReceiver;

        @Synthetic final AtomicBoolean mCanceled = new AtomicBoolean();
        private boolean mRunning;

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            mProgress.set(0);
            mCanceled.set(false);
            mRunning = true;

            mContext = getApplicationContext();
            mPkgName = getPackageName();

            mNotificationManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = NotificationChannelManager.getDownloadNotificationChannelId(mContext);
            RemoteViews nv = createNotificationView();
            mNotificationBuilder = new NotificationCompat.Builder(mContext, channelId)
                    .setSmallIcon(R.drawable.ic_media_app_notification)
                    .setTicker(mContext.getString(R.string.downloadingUpdates))
                    .setCustomContentView(nv)
                    .setCustomBigContentView(nv)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setColor(Consts.COLOR_ACCENT)
                    .setDefaults(0)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET);

            startForeground(ID_NOTIFICATION, mNotificationBuilder.build());

            mReceiver = new CancelAppUpdateReceiver();
            registerReceiver(mReceiver, new IntentFilter(CancelAppUpdateReceiver.ACTION));

            //noinspection ConstantConditions
            mApk = new File(App.getAppExternalFilesDir(),
                    intent.getStringExtra(EXTRA_APP_NAME) + " "
                            + intent.getStringExtra(EXTRA_VERSION_NAME).replace(".", "_")
                            + ".apk");
            mApkLength = intent.getIntExtra(EXTRA_APP_LENGTH, 0);
            if (mApk.exists()) {
                final String sha1 = intent.getStringExtra(EXTRA_APP_SHA1);
                // 如果应用已经下载过了，则直接弹出安装提示通知
                if (mApk.length() == mApkLength
                        && ObjectsCompat.equals(FileUtils2.getFileSha1(mApk), sha1)) {
                    stopService();
                    onAppDownloaded(mApk);
                    return START_REDELIVER_INTENT;
                    // 否则先删除旧的apk
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    mApk.delete();
                }
            }

            mAppPartLinks = intent.getStringArrayExtra(EXTRA_APP_PART_LINKS);
            //noinspection ConstantConditions
            mApkParts = new File[mAppPartLinks.length];
            mDownloadAppPartTasks = new ArrayList<>(mApkParts.length);
            mDownloadAppExecutor = Build.VERSION.SDK_INT > Build.VERSION_CODES.N
                    ? Executors.newWorkStealingPool(COUNT_DOWNLOAD_APP_TASK)
                    : Executors.newFixedThreadPool(COUNT_DOWNLOAD_APP_TASK);
            for (int i = 0; i < mApkParts.length; i++) {
                mApkParts[i] = new File(mApk.getPath().replace(".apk", i + ".apk"));
                mDownloadAppPartTasks.add(new DownloadAppPartTask());
                mDownloadAppPartTasks.get(i).executeOnExecutor(mDownloadAppExecutor, i);
            }

            return START_REDELIVER_INTENT;
        }

        @Synthetic RemoteViews createNotificationView() {
            RemoteViews nv = new RemoteViews(mPkgName, R.layout.notification_download_app);
            nv.setOnClickPendingIntent(R.id.btn_cancel_danv,
                    PendingIntent.getBroadcast(
                            mContext,
                            0,
                            new Intent(CancelAppUpdateReceiver.ACTION),
                            0));
            return nv;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            if (!mCanceled.getAndSet(true)) {
                if (mRunning) {
                    cancel();
                } else if (mDownloadAppExecutor != null) {
                    mDownloadAppExecutor.shutdown();
                }
            }

            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        void cancel() {
            if (mDownloadAppExecutor != null) {
                if (mDownloadAppPartTasks != null) {
                    for (DownloadAppPartTask task : mDownloadAppPartTasks) {
                        task.cancel(false);
                    }
                }
                mDownloadAppExecutor.shutdown();
            }

            deleteApkParts();

            stopService();
        }

        private void deleteApkParts() {
            if (mApkParts != null) {
                for (File apkPart : mApkParts) {
                    //noinspection ResultOfMethodCallIgnored
                    apkPart.delete();
                }
            }
        }

        @Synthetic void stopService() {
            mRunning = false;
            stopForeground(false);
            mNotificationManager.cancel(ID_NOTIFICATION);
            getHandler().sendEmptyMessage(H.MSG_STOP_UPDATE_APP_SERVICE);
        }

        private Handler getHandler() {
            return MergeAppUpdateChecker.getSingleton(mContext).mH;
        }

        @Synthetic void onConnectionTimeout() {
            if (!mCanceled.getAndSet(true)) {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        cancel();
                        Toast.makeText(mContext,
                                R.string.connectionTimeout, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Synthetic void onReadTimeout() {
            if (!mCanceled.getAndSet(true)) {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        cancel();
                        Toast.makeText(mContext,
                                R.string.readTimeout, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Synthetic void onDownloadError() {
            if (!mCanceled.getAndSet(true)) {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        cancel();
                        Toast.makeText(mContext,
                                R.string.downloadError, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @SuppressLint("StaticFieldLeak")
        private final class DownloadAppPartTask extends AsyncTask<Integer, Integer, Void> {
            final UpdateAppService mHost = UpdateAppService.this;

            DownloadAppPartTask() {
            }

            @Override
            protected Void doInBackground(Integer... indices) {
                final int index = indices[0];

                HttpURLConnection conn = null;
                InputStream in = null;
                OutputStream out = null;
                try {
                    URL url = new URL(mAppPartLinks[index]);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(TIMEOUT_CONNECTION);
//                    conn.setReadTimeout(TIMEOUT_READ);

                    // 从服务器请求全部资源返回 200 ok；从服务器请求部分资源返回 206 ok
                    // final int responseCode = conn.getResponseCode();

                    File apkPart = mApkParts[index];
                    if (apkPart.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        apkPart.delete();
                    }

                    in = conn.getInputStream();
                    out = new FileOutputStream(apkPart);

                    int len;
                    final byte[] buffer = new byte[8 * 1024];
                    while (!mCanceled.get() && (len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);

                        if (!mCanceled.get()) {
//                            publishProgress(len);
                            notifyProgressUpdated(mProgress.addAndGet(len));
                        }
                    }
                } catch (ConnectTimeoutException e) {
                    onConnectionTimeout();
                } catch (SocketTimeoutException e) {
                    onReadTimeout();
                } catch (IOException e) {
                    e.printStackTrace();
                    onDownloadError();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            //
                        }
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            //
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
//                notifyProgressUpdated(mProgress += values[0]);
            }

            private void notifyProgressUpdated(int progress) {
                RemoteViews nv = createNotificationView();
                nv.setProgressBar(R.id.progress, mApkLength, progress, false);
                nv.setTextViewText(R.id.text_percentProgress,
                        mContext.getString(R.string.percentProgress,
                                (float) progress / (float) mApkLength * 100f));
                nv.setTextViewText(R.id.text_charsequenceProgress,
                        mContext.getString(R.string.charsequenceProgress,
                                FileUtils2.formatFileSize(progress),
                                FileUtils2.formatFileSize(mApkLength)));

                synchronized (mHost) {
                    mNotificationBuilder.setCustomContentView(nv);
                    mNotificationBuilder.setCustomBigContentView(nv);

                    Notification n = mNotificationBuilder.build();

                    // 确保下载被取消后不再有任何通知被弹出...
                    if (!mCanceled.get()) {
                        mNotificationManager.notify(ID_NOTIFICATION, n);
                    }
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mDownloadAppPartTasks.remove(this);
                if (mDownloadAppPartTasks.isEmpty()) {
                    stopService();
                    FileUtils2.mergeFiles(mApkParts, mApk, true);
                    onAppDownloaded(mApk);
                }
            }
        }

        void onAppDownloaded(File apk) {
            if (apk == null || !apk.exists() || apk.length() != mApkLength) {
                Toast.makeText(mContext, R.string.theInstallationPackageHasBeenDamaged,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Intent it = Utils.createPackageInstaller(mContext, apk);

//            mContext.startActivity(it); // MIUI默认应用在后台时无法弹出界面

            String title = mContext.getString(R.string.newAppDownloaded);
            PendingIntent pi = PendingIntent.getActivity(mContext, 0, it, 0);
            mNotificationManager.notify(
                    ID_NOTIFICATION,
                    mNotificationBuilder
                            .setChannelId(NotificationChannelManager.getMessageNotificationChannelId(mContext))
                            .setTicker(title)
                            .setContentTitle(title)
                            .setContentText(mContext.getString(R.string.clickToInstallIt))
                            .setContentIntent(pi)
//                            .setFullScreenIntent(pi, true)
                            .setAutoCancel(true)
                            .setCustomContentView(null)
                            .setCustomBigContentView(null)
                            .setStyle(null)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                            .setPriority(NotificationCompat.PRIORITY_HIGH) // 高优先级以显示抬头式通知
                            .setCategory(NotificationCompat.CATEGORY_PROMO)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .build());
        }

        private static final class CancelAppUpdateReceiver extends BroadcastReceiver {
            static final String ACTION =
                    "action_MergeAppUpdateChecker$UpdateAppService$CancelAppUpdateReceiver";

            CancelAppUpdateReceiver() {
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                MergeAppUpdateChecker.getSingleton(context)
                        .mH.sendEmptyMessage(H.MSG_STOP_UPDATE_APP_SERVICE);
            }
        }
    }
}
