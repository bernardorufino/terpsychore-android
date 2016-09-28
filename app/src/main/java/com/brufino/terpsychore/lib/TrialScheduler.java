package com.brufino.terpsychore.lib;

import android.os.Handler;

import java.util.concurrent.TimeUnit;

/**
 * Class to be used when there is the possibility of trying to execute some code multiple times when we only
 * want to execute it the last.
 * TODO: Pretty sure we don't need sync, but double check
 * TODO: Write doc, assume try execute is all from the same thread (needed?)
 */
public abstract class TrialScheduler {

    private static final long MARGIN_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private boolean mOutstanding = false;
    private final Handler mHandler;
    private final long mIntervalInNanos;
    private volatile long mLastScheduledExecution;

    public TrialScheduler(Handler handler, long interval, TimeUnit timeUnit) {
        mHandler = handler;
        mIntervalInNanos =  timeUnit.toNanos(interval);

    }

    public void tryExecute() {
        mLastScheduledExecution = System.nanoTime();
        if (!mOutstanding) {
            mOutstanding = true;
            mHandler.postDelayed(mTryExecuteRunnable, delayInMs(mIntervalInNanos));
        }
    }

    private Runnable mTryExecuteRunnable = new Runnable() {
        @Override
        public void run() {
            long interval = System.nanoTime() - mLastScheduledExecution;
            if (interval < mIntervalInNanos) {
                mHandler.postDelayed(mTryExecuteRunnable, delayInMs(mIntervalInNanos - interval + MARGIN_IN_NANOS));
            } else {
                mOutstanding = false;
                doExecute();
            }
        }
    };

    protected abstract void doExecute();

    public boolean isOutstanding() {
        return mOutstanding;
    }

    private long delayInMs(long delayInNanos) {
        return delayInNanos / 1000_000;
    }
}
