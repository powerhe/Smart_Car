package com.lenovo.newdevice.tangocar.utils;

/**
 * Created by liujk2 on 2016/12/26.
 */

public class ProcessThread extends Thread {
    private boolean mRunning;
    private long mSleepTimePerWhile;
    public ProcessThread(long sleepTimePerWhile) {
        mSleepTimePerWhile = sleepTimePerWhile;
        mRunning = true;
    }

    protected boolean runCondition() {
        return true;
    }

    protected void doInit() {
    }

    protected void doAction() {
    }

    protected void doOver() {
    }

    protected void setLoopTime(long loopTime) {
        mSleepTimePerWhile = loopTime;
    }

    public void cancel() {
        mRunning = false;
    }

    public void waitForTime(long waitTime) {
        synchronized (this) {
            try {
                this.wait(waitTime);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void run() {
        while (!runCondition()) {
            waitForTime(mSleepTimePerWhile);
        }
        doInit();
        while (mRunning) {
            long sleepTime = mSleepTimePerWhile;
            long currentTime = System.currentTimeMillis();
            if (runCondition()) {
                doAction();
            }
            sleepTime -= System.currentTimeMillis() - currentTime;
            if (sleepTime > 0) {
                waitForTime(sleepTime);
            }
        }
        doOver();
    }
}
