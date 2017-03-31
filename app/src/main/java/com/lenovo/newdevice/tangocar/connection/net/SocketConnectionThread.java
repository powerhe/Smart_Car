package com.lenovo.newdevice.tangocar.connection.net;

import android.util.Log;

import com.lenovo.newdevice.tangocar.utils.ProcessThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/9.
 */

public abstract class SocketConnectionThread extends ProcessThread {
    private static final int MAX_CONNECT_RETRY_COUNT = 6;
    protected int mPort;
    protected Socket mLinkSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private SocketConnectionCallback mCallback;
    private boolean mInit;
    private boolean mConnected;
    private long mLoopTime;
    private int mRetryCount;

    public SocketConnectionThread(int port, long loopTime) {
        super(loopTime);
        mPort = port;
        mLinkSocket = null;
        mCallback = null;
        mConnected = false;
        mInit = false;
        mLoopTime = loopTime;
        mRetryCount = 0;
    }

    public void setCallback(SocketConnectionCallback callback) {
        mCallback = callback;
    }

    @Override
    protected boolean runCondition() {
        return !mInit || mConnected;
    }

    protected boolean closeLinkSocket() {
        boolean res = true;
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "close link socket input stream error", e);
                res = false;
            }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "close link socket output stream error", e);
                res = false;
            }
            mOutputStream = null;
        }
        if (mLinkSocket != null) {
            try {
                mLinkSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close link socket error", e);
                res = false;
            }
            mLinkSocket = null;
        }
        return res;
    }

    private boolean connect() {
        mLinkSocket = null;
        mInputStream = null;
        mOutputStream = null;

        try {
            mLinkSocket = getLinkSocket();
        } catch (IOException e) {
            Log.e(TAG, "get link socket error", e);
        }
        Log.e(TAG, " got link socket " + mLinkSocket);

        if (mLinkSocket != null) {
            try {
                mInputStream = mLinkSocket.getInputStream();
                mOutputStream = mLinkSocket.getOutputStream();
            } catch (IOException e) {
                if (mInputStream == null) {
                    Log.e(TAG, "get socket input stream error", e);
                } else if (mOutputStream == null) {
                    Log.e(TAG, "get socket output stream error", e);
                }
                closeLinkSocket();
                return false;
            }
            Log.e(TAG, " get InputStream and OutputStream ok ");

            if (mCallback != null) {
                mCallback.onConnected(mInputStream, mOutputStream);
            }
            return true;
        }
        return false;
    }

    private void disconnect() {
        if (mConnected) {
            mConnected = false;
            if (mCallback != null) {
                mCallback.onDisconnected();
            }
        }
    }

    @Override
    protected void doInit() {
        mConnected = connect();
        if (mConnected) {
            mInit = true;
        }
    }

    @Override
    protected void doAction() {
        if (!mConnected) {
            setLoopTime(1000);
            boolean connected = connect();
            mRetryCount ++;
            if (connected) {
                mInit = true;
                mConnected = true;
                setLoopTime(mLoopTime);
            } else {
                if (mRetryCount >= MAX_CONNECT_RETRY_COUNT) {
                    if (mCallback != null) {
                        mCallback.onServerInvalid();
                        cancel();
                    }
                }
            }
        } else {
            mCallback.onLoop();
            if (isClosed()) {
                disconnect();
                cancel();
            }
        }
    }

    @Override
    protected void doOver() {
        doClosed();
        disconnect();
    }

    @Override
    public void cancel() {
        //Log.i(TAG, getName() + " cancel", new Exception());
        super.cancel();
        doClosed();
    }

    abstract protected Socket getLinkSocket() throws IOException;
    abstract protected boolean isClosed();
    abstract protected boolean doClosed();
}
