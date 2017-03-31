package com.lenovo.newdevice.tangocar.connection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.lenovo.newdevice.tangocar.utils.HandlerThread;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/15.
 */

public class Connection extends Service {
    protected InputStream mInputStream;
    protected OutputStream mOutputStream;
    protected ConnectionBinder mBinder;
    protected MessageReceiver mReceiver;
    protected ConnectionCallback mCallback;
    private boolean mConnected;
    private boolean mNeedReceiveThread;
    private ReceiveThread mReceiveThread;
    private Object mReceiveLock;
    protected Context mContext;

    protected HandlerThread mHandlerThread;
    protected Handler mHandler;

    public Connection() {
        super();
        mBinder = new ConnectionBinder();
        mReceiveLock = new Object();
        mNeedReceiveThread = false;
    }

    protected void onCreateHandler() {
        mHandler = new Handler();
    }

    private void startHandlerThread() {
        mHandlerThread = new HandlerThread("HandlerThread-" + getClass().getSimpleName()){
            protected void createHandler() {
                onCreateHandler();
            }
        };
        mHandlerThread.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Light);
        startHandlerThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandlerThread.quit();
    }

    protected class ReceiveThread extends Thread {
        boolean running;

        ReceiveThread() {
            running = true;
            setName("ReceiveThread");
        }

        public void run() {
            while (running) {
                try {
                    if (mInputStream == null) {
                        Utils.sleep(100);
                        continue;
                    }
                    if (mReceiver != null) {
                        mReceiver.onReceiveData(mInputStream);
                    } else {
                        Utils.sleep(100);
                    }
                } catch (Exception e) {
                    if (mReceiver != null) {
                        mReceiver.onError("exception: " + e);
                    }
                    Log.w(TAG, "something wrong: ", e);
                    Utils.sleep(100);
                }
            }
        }

        public void cancel() {
            running = false;
        }
    }

    public class ConnectionBinder extends Binder {
        public Connection getService() {
            return Connection.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Connection onBind(" + intent + "), mBinder is " + mBinder);
        return mBinder;
    }

    public void setMessageReceiver(MessageReceiver receiver) {
        mReceiver = receiver;
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        mCallback = callback;
    }

    public boolean sendMessage(byte[] msg) {
        if (!mConnected) {
            return false;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.write(msg);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
        return false;
    }

    // should be called in onCreate method
    protected void setNeedReceiveThread() {
        mNeedReceiveThread = true;
    }

    public boolean isConnected() {
        return mConnected;
    }

    protected void connected() {
        if (!mConnected) {
            mConnected = true;
            if (mNeedReceiveThread) {
                startReceive();
            }
            if (mCallback != null) {
                mCallback.onConnected();
            }
        }
    }

    protected void disconnected() {
        if (mConnected) {
            mConnected = false;
            if (mNeedReceiveThread) {
                stopReceive();
            }
            if (mCallback != null) {
                mCallback.onDisconnected();
            }
        }
    }

    protected void invalid() {
        if (!mConnected) {
            if (mCallback != null) {
                mCallback.onInvalid();
            }
        }
    }

    private void startReceive() {
        synchronized (mReceiveLock) {
            if (mReceiveThread == null) {
                mReceiveThread = new ReceiveThread();
                mReceiveThread.start();
            }
        }
    }

    private void stopReceive() {
        synchronized (mReceiveLock) {
            if (mReceiveThread != null) {
                mReceiveThread.cancel();
                mReceiveThread = null;
            }
        }
    }
}
