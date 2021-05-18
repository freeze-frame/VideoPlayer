/*
 * Copyright (C) 2008 The Android Open Source Project
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
 * limitations under the License.
 */
package com.liuzhenlin.common.utils;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import com.liuzhenlin.common.Consts;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Various different executors used in Videos
 */
public class Executors {
    private Executors() {
    }

    /**
     * An {@link Executor} to be used with async task with no limit on the queue size.
     */
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = ParallelThreadExecutor.getSingleton();

    /**
     * An {@link Executor} that executes tasks one at a time in serial order.
     * This serialization is global to a particular process.
     */
    public static final SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

    /**
     * Returns the executor for running tasks on the main thread.
     */
    public static final LooperExecutor MAIN_EXECUTOR =
            new LooperExecutor(Consts.getMainThreadHandler());

//    /**
//     * A background executor for using time sensitive actions where user is waiting for response.
//     */
//    public static final LooperExecutor UI_HELPER_EXECUTOR =
//            new LooperExecutor(createAndStartNewForegroundLooper("UiThreadHelper"));

    /**
     * Utility method to get a started handler thread statically
     */
    public static Looper createAndStartNewLooper(@NonNull String name) {
        return createAndStartNewLooper(name, Process.THREAD_PRIORITY_DEFAULT);
    }

    /**
     * Similar to {@link #createAndStartNewLooper(String)}, but starts the thread with
     * foreground priority.
     * Think before using.
     */
    public static Looper createAndStartNewForegroundLooper(@NonNull String name) {
        return createAndStartNewLooper(name, Process.THREAD_PRIORITY_FOREGROUND);
    }

    /**
     * Utility method to get a started handler thread statically with the provided priority
     */
    public static Looper createAndStartNewLooper(@NonNull String name, int priority) {
        HandlerThread thread = new HandlerThread(name, priority);
        thread.start();
        return thread.getLooper();
    }
}
