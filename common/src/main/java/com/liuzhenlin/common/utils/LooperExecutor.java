/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.liuzhenlin.common.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Extension of {@link AbstractExecutorService} which executed on a provided looper.
 */
public class LooperExecutor extends AbstractExecutorService {

    private final Handler mHandler;

    public LooperExecutor(@NonNull Looper looper) {
        mHandler = new Handler(looper);
    }

    public LooperExecutor(@NonNull Handler handler) {
        mHandler = handler;
    }

    @NonNull
    public final Handler getHandler() {
        return mHandler;
    }

    @Override
    public final void execute(@NonNull Runnable runnable) {
        if (mHandler.getLooper() == Looper.myLooper()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    /**
     * Same as execute, but never runs the action inline.
     */
    public final void post(@NonNull Runnable runnable) {
        mHandler.post(runnable);
    }

    /**
     * Remove any pending posts of Runnable that exists in the run queue.
     * To remove all, pass {@code null}.
     */
    public final void remove(@Nullable Runnable runnable) {
        if (runnable == null) {
            mHandler.removeCallbacksAndMessages(null);
        } else {
            mHandler.removeCallbacks(runnable);
        }
    }

    /**
     * Not supported and throws an exception when used.
     */
    @Override
    @Deprecated
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported and throws an exception when used.
     */
    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    /**
     * Not supported and throws an exception when used.
     */
    @Override
    @Deprecated
    public boolean awaitTermination(long l, TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the thread for this executor
     */
    @NonNull
    public final Thread getThread() {
        return mHandler.getLooper().getThread();
    }

    /**
     * Returns the looper for this executor
     */
    @NonNull
    public final Looper getLooper() {
        return mHandler.getLooper();
    }

    /**
     * Set the priority of a thread, based on Linux priorities.
     *
     * @param priority Linux priority level, from -20 for highest scheduling priority
     *                 to 19 for lowest scheduling priority.
     * @see Process#setThreadPriority(int, int)
     */
    public final void setThreadPriority(int priority) {
        Process.setThreadPriority(((HandlerThread) getThread()).getThreadId(), priority);
    }
}
