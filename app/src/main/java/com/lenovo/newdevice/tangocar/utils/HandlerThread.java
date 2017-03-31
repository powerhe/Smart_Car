package com.lenovo.newdevice.tangocar.utils;

import android.os.Looper;

import com.lenovo.newdevice.tangocar.control.CarControl;

/**
 * Created by liujk2 on 2016/12/27.
 */

public class HandlerThread extends WaitThread {
    private Looper mLooper = null;
    private final Object mLooperLock = new Object();

    public HandlerThread(String name) {
        super();
        setName(name);
    }

    protected void createHandler() {
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mLooperLock) {
            mLooper = Looper.myLooper();
        }
        createHandler();
        Looper.loop();
    }

    public void quit() {
        synchronized (mLooperLock) {
            if (mLooper != null) {
                mLooper.quit();
            }
            onCancel();
        }
    }
}
