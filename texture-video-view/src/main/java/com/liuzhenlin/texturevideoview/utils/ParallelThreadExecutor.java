/*
 * Created on 2019/12/20 4:45 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.util.Synthetic;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An {@link Executor} that can be used to execute tasks in parallel.
 *
 * @author 刘振林
 */
public final class ParallelThreadExecutor extends ThreadPoolExecutor {

    private static final String TAG = "ParallelThreadExecutor";

    // We keep only a single pool thread around all the time.
    // We let the pool grow to a fairly large number of threads if necessary,
    // but let them time out quickly. In the unlikely case that we run out of threads,
    // we fall back to a simple unbounded-queue executor.
    // This combination ensures that:
    // 1. We normally keep few threads (1) around.
    // 2. We queue only after launching a significantly larger, but still bounded, set of threads.
    // 3. We keep the total number of threads bounded, but still allow an unbounded set of tasks
    //    to be queued.
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int BACKUP_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;

    @Synthetic static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "ParallelThread #" + count.incrementAndGet());
        }
    };

    // Used only for rejected executions.
    private static final Singleton<Void, Executor> sBackupExecutorSingleton =
            new Singleton<Void, Executor>() {
                @NonNull
                @Override
                protected Executor onCreate(Void... voids) {
                    return new ThreadPoolExecutor(
                            0, BACKUP_POOL_SIZE,
                            KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            sThreadFactory);
                }
            };

    private static final RejectedExecutionHandler sRunOnSerialPolicy =
            (r, e) -> {
                Log.w(TAG, "Exceeded ParallelThreadExecutor pool size");
                // As a last ditch fallback, run it on an executor with an unbounded queue.
                // Create this executor lazily, hopefully almost never.
                sBackupExecutorSingleton.get().execute(r);
            };

    private static final Singleton<Void, ParallelThreadExecutor> sParallelThreadExecutorSingleton =
            new Singleton<Void, ParallelThreadExecutor>() {
                @SuppressLint("SyntheticAccessor")
                @NonNull
                @Override
                protected ParallelThreadExecutor onCreate(Void... voids) {
                    return new ParallelThreadExecutor();
                }
            };

    public static ParallelThreadExecutor getSingleton() {
        return sParallelThreadExecutorSingleton.get();
    }

    private ParallelThreadExecutor() {
        super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                sThreadFactory,
                sRunOnSerialPolicy);
    }
}
