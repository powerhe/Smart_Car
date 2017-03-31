package com.lenovo.newdevice.tangocar.remote;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lenovo.newdevice.tangocar.connection.Connection;
import com.lenovo.newdevice.tangocar.connection.net.SocketConnectionCallback;
import com.lenovo.newdevice.tangocar.connection.net.SocketConnectionThread;
import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.WaitThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/9.
 */

public class RCConnectionService extends Connection {
    private static final boolean DEBUG = false;
    protected static final String ACTION_RCSERVICE_CLIENT = "com.tangocar.connection.net.ACTION_RCSERVICE_CLIENT";
    protected static final String ACTION_RCSERVICE_SERVER = "com.tangocar.connection.net.ACTION_RCSERVICE_SERVER";
    protected static final String ACTION_RCSERVICE_STOP = "com.tangocar.connection.net.ACTION_RCSERVICE_STOP";
    protected ServiceThread mServiceThread;
    protected ApControl mApControl;
    private static Boolean sLastIsRemoter = null;
    private MessageHandler mMessageHandler;
    private RCConnectionCallback mConnectionCallback;
    private boolean mLinkConnected;

    @Override
    public void onCreate() {
        super.onCreate();
        mApControl = ApControl.getInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mServiceThread != null && mServiceThread.isRunning()) {
            mServiceThread.cancel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, this.getClass().getSimpleName() + " Service onStartCommand " + intent);
        if (judgeNeedStop(intent)) {
            stopSelfService();
            return START_NOT_STICKY;
        }
        if (mServiceThread == null || !mServiceThread.isRunning()) {
            createServiceThread();
            mServiceThread.start();
        }
        return START_NOT_STICKY;
    }

    public void setRCConnectionCallback(RCConnectionCallback connectionCallback) {
        mConnectionCallback = connectionCallback;
    }

    protected void createServiceThread() {
    }

    private boolean judgeNeedStop(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_RCSERVICE_STOP.equals(action)) {
                return true;
            }
        }
        return false;
    }

    public void stopSelfService() {
        if (mServiceThread != null && mServiceThread.isRunning()) {
            mServiceThread.cancel();
        }
        stopSelf();
    }

    public void reConnect() {
        if (mServiceThread != null && mServiceThread.isRunning()) {
            mServiceThread.reConnect();
        }
    }

    protected boolean isLinkConnected() {
        return mLinkConnected;
    }

    @Override
    protected void onCreateHandler() {
        mMessageHandler = new MessageHandler();
    }

    public static Class getServiceClass(boolean isRemoter) {
        Class serviceClass = RemoteControlHost.class;
        if (isRemoter) {
            serviceClass = RemoteController.class;
        }
        return serviceClass;
    }

    public static void stopLastService(ContextWrapper contextWrapper) {
        if (sLastIsRemoter == null) {
            return;
        }
        Class serverClass = getServiceClass(sLastIsRemoter);
        Intent serviceIntent = new Intent(contextWrapper, serverClass);
        serviceIntent.setAction(ACTION_RCSERVICE_STOP);
        contextWrapper.startService(serviceIntent);
    }

    public static void start(ContextWrapper contextWrapper, boolean isRemoter) {
        if (sLastIsRemoter != null) {
            if (sLastIsRemoter != isRemoter) {
                stopLastService(contextWrapper);
            }
        }
        sLastIsRemoter = isRemoter;
        Class serviceClass = getServiceClass(isRemoter);
        String action = ACTION_RCSERVICE_SERVER;
        if (isRemoter) {
            action = ACTION_RCSERVICE_CLIENT;
        }
        Intent serviceIntent = new Intent(contextWrapper, serviceClass);
        serviceIntent.setAction(action);
        contextWrapper.startService(serviceIntent);
    }

    enum CState{INIT, CONNECT_FAILED, SETUP_WIFI, SETUP_CONNECTION, WAIT_CONNECTION_OK, WAIT_DISCONNECTED};
    protected class ServiceThread extends WaitThread {
        private boolean mConnected;
        private boolean mIsRunning;
        private boolean mHasShakeHand;
        SocketConnectionThread mHeartBeatConnectionThread;
        SocketConnectionThread mMessageConnectionThread;
        HeartBeatCallback mHeartBeatCallback;
        MessageCallback mMessageCallback;
        private Object mReConnectedLock;
        private Object mConnectedLock;
        private Object mDisconnectedLock;
        private CState mState = CState.INIT;

        public ServiceThread() {
            super();
            mConnected = false;
            mIsRunning = false;
            mHasShakeHand = false;
            mHeartBeatConnectionThread = null;
            mMessageConnectionThread = null;
            mHeartBeatCallback = new HeartBeatCallback();
            mMessageCallback = new MessageCallback();
            mReConnectedLock = new Object();
            mConnectedLock = new Object();
            mDisconnectedLock = new Object();
        }

        protected boolean setupWifiConnection(WifiConnectCallback callback) {
            callback.connectSuccess();
            return false;
        }

        protected void createConnectionThreads() {
        }

        private void stopConnectionThreads() {
            if (mHeartBeatConnectionThread != null) {
                mHeartBeatConnectionThread.setCallback(null);
                mHeartBeatConnectionThread.cancel();
                mHeartBeatConnectionThread = null;
            }
            if (mMessageConnectionThread != null) {
                mMessageConnectionThread.setCallback(null);
                mMessageConnectionThread.cancel();
                mMessageConnectionThread = null;
            }
        }

        private void setupConnectionThreads() {
            stopConnectionThreads();
            createConnectionThreads();
            mHeartBeatConnectionThread.setCallback(mHeartBeatCallback);
            mMessageConnectionThread.setCallback(mMessageCallback);
        }

        public void run() {
            mIsRunning = true;
            while (mIsRunning) {
                boolean canceled = false;
                if (mState == CState.INIT) {
                    mState = CState.SETUP_WIFI;
                } else if (mState == CState.CONNECT_FAILED) {
                    // wait for re connect
                    Log.i(TAG, "connection " + getName() + " wait for re connected");
                    canceled = waitForReConnect();
                    mState = CState.SETUP_WIFI;
                } else if (mState == CState.SETUP_WIFI) {
                    Log.i(TAG, "connection " + getName() + " setupWifiConnection()");
                    canceled = setupWifiConnection(new WifiConnectCallback() {
                        @Override
                        public void connectSuccess() {
                            Log.i(TAG, "connection " + getName() + " connect success");
                            mState = CState.SETUP_CONNECTION;
                        }
                        @Override
                        public void connectFailed() {
                            Log.w(TAG, "connection " + getName() + " connectFailed()", new Exception());
                            serverInvalid();
                        }
                    });
                } else if (mState == CState.SETUP_CONNECTION) {
                    Log.i(TAG, "connection " + getName() + " setupConnectionThreads()");
                    setupConnectionThreads();
                    Log.i(TAG, "connection " + getName() + " start heartbeat");
                    mHeartBeatConnectionThread.start();
                    mState = CState.WAIT_CONNECTION_OK;
                } else if (mState == CState.WAIT_CONNECTION_OK) {
                    Log.i(TAG, "connection " + getName() + " wait for connected");
                    canceled = wait(mConnectedLock);
                    if (mState == CState.WAIT_CONNECTION_OK) {
                        mState = CState.WAIT_DISCONNECTED;
                    }
                } else if (mState == CState.WAIT_DISCONNECTED) {
                    Log.i(TAG, "connection " + getName() + " connected, wait for disconnected");
                    canceled = wait(mDisconnectedLock);
                    Log.i(TAG, "connection " + getName() + " disconnected");
                    mState = CState.CONNECT_FAILED;
                }
                if (canceled) {
                    break;
                }
            }
            doOver();
        }

        public boolean isRunning() {
            return mIsRunning;
        }

        public void cancel() {
            if (mIsRunning) {
                mIsRunning = false;
            }
            onCancel();
        }

        private void doOver() {
            switchConnected(false);
            stopConnectionThreads();
        }

        protected boolean waitForReConnect() {
            return wait(mReConnectedLock);
        }

        public void reConnect() {
            Log.i(TAG, "connection " + getName() + " reConnect(), mState is " + mState);
            if (mState == CState.CONNECT_FAILED) {
                notifyAll(mReConnectedLock);
            }
        }

        private void serverInvalid() {
            Log.i(TAG, "connection " + getName() + " serverInvalid(), mState is " + mState);
            CState oldState = mState;
            mState = CState.CONNECT_FAILED;
            if (oldState == CState.WAIT_CONNECTION_OK) {
                notifyAll(mConnectedLock);
            }
            RCConnectionService.this.invalid();
        }

        private void switchConnected(boolean connected) {
            Log.i(TAG, "connection " + getName() + " switchConnected(" + connected + "), mState is " + mState);
            if (mConnected != connected) {
                mConnected = connected;
                if (mConnected) {
                    notifyAll(mConnectedLock);
                    connected();
                } else {
                    notifyAll(mDisconnectedLock);
                    disconnected();
                }
            }
        }

        protected RCHeartBeat createRCHeartBeat(InputStream inputStream, OutputStream outputStream) {
            return null;
        }

        private boolean hasShakeHand() {
            return mHasShakeHand;
        }

        // called by subclass
        protected void shakeHandOK() {
            mHasShakeHand = true;
            switchConnected(true);
        }

        // override by subclass
        protected void beginToShakeHand() {
        }

        // override by subclass
        protected void onShakeHand() {
        }

        public class HeartBeatCallback implements SocketConnectionCallback {
            private RCHeartBeat mRCHearBeat;

            @Override
            public void onConnected(InputStream inputStream, OutputStream outputStream) {
                if (mMessageConnectionThread != null) {
                    mMessageConnectionThread.start();
                }
                mRCHearBeat = createRCHeartBeat(inputStream, outputStream);
            }

            @Override
            public void onLoop() {
                if (!mRCHearBeat.oneTime()) {
                    if (mHeartBeatConnectionThread != null) {
                        mHeartBeatConnectionThread.cancel();
                    }
                }
            }

            @Override
            public void onDisconnected() {
                if (mMessageConnectionThread != null) {
                    mMessageConnectionThread.cancel();
                }
            }

            @Override
            public void onServerInvalid() {
                serverInvalid();
            }
        }

        public class MessageCallback implements SocketConnectionCallback {
            @Override
            public void onConnected(InputStream inputStream, OutputStream outputStream) {
                mInputStream = inputStream;
                mOutputStream = outputStream;
                mLinkConnected = true;
                mHasShakeHand = false;
                beginToShakeHand();
            }

            @Override
            public void onLoop() {
                if (!hasShakeHand()) {
                    onShakeHand();
                } else {
                    receiveAndProcessRCMessage();
                }
            }

            @Override
            public void onDisconnected() {
                mLinkConnected = false;
                mInputStream = null;
                mOutputStream = null;
                if (mHeartBeatConnectionThread != null) {
                    mHeartBeatConnectionThread.cancel();
                }
                switchConnected(false);
            }

            @Override
            public void onServerInvalid() {
                serverInvalid();
            }
        }
    }

    private void sendRCMessageToSocket(RCMessage message) throws IOException {
        if (message.isNotData()) Log.i(TAG, "sendRCMessageToSocket("+message+"), isLinkConnected is " + isLinkConnected());
        if (message == null || !isLinkConnected()) {
            return;
        }
        message.send(mOutputStream);
    }

    private RCMessage receiveRCMessageFromSocket() throws IOException {
        if (DEBUG) Log.i(TAG, "receiveRCMessageFromSocket(), isLinkConnected is " + isLinkConnected());
        if (!isLinkConnected()) {
            return null;
        }
        RCMessage message = RCMessage.readFromInputStream(mInputStream);
        if (message.isNotData()) Log.i(TAG, "receiveRCMessageFromSocket():" + message);
        return message;
    }

    public void sendObject(byte type, DataSerializable obj) {
        if (!isConnected()) {
            return;
        }
        RCMessage message = RCMessage.getMessageFromObject(type, obj);
        sendRCMessage(message);
    }

    public void sendControlRCMessage(byte type) {
        if (!isConnected()) {
            return;
        }
        sendControlRCMessageInner(type);
    }

    public void sendRCMessage(RCMessage message) {
        if (!isConnected()) {
            return;
        }
        sendRCMessageInner(message);
    }

    private void sendControlRCMessageInner(byte type) {
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.SEND_CTL_MSG, type, 0));
    }

    protected void sendRCMessageInner(RCMessage message) {
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.SEND_MSG, message));
    }

    private void receiveAndProcessRCMessage() {
        if (DEBUG) Log.i(TAG, "receiveAndProcessRCMessage()");
        RCMessage message = null;
        try {
            message = receiveRCMessageFromSocket();
        } catch (IOException e) {
            Log.e(TAG, "receive rc message error:", e);
        }
        if (DEBUG) Log.i(TAG, "message is " + message + ", isConnected:" + isConnected());
        if (message == null) {
            return;
        } else {
            if (mConnectionCallback != null) {
                mConnectionCallback.onReceivedRCMessage(message);
            }
        }
    }

    protected RCMessage receiveRCMessage() {
        RCMessage message = null;
        try {
            message = receiveRCMessageFromSocket();
        } catch (IOException e) {
            Log.e(TAG, "receive rc message error:", e);
        }
        return message;
    }

    class MessageHandler extends Handler {
        public static final int SEND_MSG = 100;
        public static final int SEND_CTL_MSG = 101;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SEND_MSG) {
                realSendRCMessage((RCMessage)msg.obj);
            } else if (msg.what == SEND_CTL_MSG) {
                byte type = (byte)(msg.arg1 & 0xff);
                RCMessage message = RCMessage.getControlMessage(type);
                realSendRCMessage(message);
            }
        }

        private void realSendRCMessage(RCMessage message) {
            if (DEBUG) Log.i(TAG, "realSendRCMessage("+message+"), isLinkConnected is " + isLinkConnected());
            if (!isLinkConnected() || message == null) {
                return;
            }
            try {
                sendRCMessageToSocket(message);
            } catch (IOException e) {
                Log.e(TAG, "send rc message error:", e);
            }
        }
    }

    interface WifiConnectCallback {
        public void connectSuccess();
        public void connectFailed();
    }
}
