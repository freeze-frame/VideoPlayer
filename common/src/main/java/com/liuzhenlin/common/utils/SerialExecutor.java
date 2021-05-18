/*
 * Created on 2020-11-25 6:45:21 PM.
 */

package com.liuzhenlin.common.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * An {@link Executor} that executes tasks one at a time in serial order.
 */
public final class SerialExecutor implements Executor {

    private final LinkedList<Runnable> mTasks = new LinkedList<>();
    private Runnable mActive;

    public synchronized void execute(@NonNull final Runnable r) {
        mTasks.offer(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            Executors.THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }

    public synchronized boolean remove(@Nullable Runnable r) {
        if (r == null) {
            if (mTasks.size() > 0) {
                mTasks.clear();
                return true;
            }
        } else {
            return mTasks.remove(r);
        }
        return false;
    }

    public synchronized boolean isIdle() {
        return mActive == null;
    }
}