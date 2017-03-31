package com.lenovo.newdevice.tangocar.utils;

import android.util.Log;

import java.util.HashMap;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/23.
 */
public class WaitThread extends Thread {
    private HashMap<Object, Integer> mLockMap;
    private boolean mCanceled;

    public WaitThread(Runnable r) {
        super(r);
        init();
    }

    public WaitThread() {
        super();
        init();
    }

    private void init() {
        mLockMap = new HashMap<Object, Integer>();
        mCanceled = false;
    }

    private void plusLock(Object lock) {
        Integer lockCount = mLockMap.get(lock);
        if (lockCount == null) {
            lockCount = 0;
        }
        lockCount++;
        mLockMap.put(lock, lockCount);
    }

    private void subLock(Object lock) {
        Integer lockCount = mLockMap.get(lock);
        if (lockCount != null) {
            lockCount--;
            if (lockCount == 0) {
                mLockMap.remove(lock);
            } else {
                mLockMap.put(lock, lockCount);
            }
        }
    }

    private void removeLock(Object lock) {
        mLockMap.remove(lock);
    }

    /**
     * @param lock
     * @return if is canceled
     */
    public boolean wait(Object lock) {
        synchronized (lock) {
            try {
                plusLock(lock);
                lock.wait();
            } catch (Exception e) {
            }
        }
        return mCanceled;
    }

    public void notify(Object lock) {
        synchronized (lock) {
            lock.notify();
            subLock(lock);
        }
    }

    public void notifyAll(Object lock) {
        synchronized (lock) {
            lock.notifyAll();
            removeLock(lock);
        }
    }

    public void onCancel() {
        Log.i(TAG, "onCancel", new Exception());
        mCanceled = true;
        for (Object lock : mLockMap.keySet()) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        mLockMap.clear();
    }
}
