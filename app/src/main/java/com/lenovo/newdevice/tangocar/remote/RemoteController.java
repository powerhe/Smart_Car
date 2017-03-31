package com.lenovo.newdevice.tangocar.remote;

import android.util.Log;

import com.lenovo.newdevice.tangocar.connection.net.ServerSocketConnectionThread;
import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/26.
 */

public class RemoteController extends RCConnectionService {
    public static final int DEF_PORT_HEARTBEAT = 18086;
    public static final int DEF_PORT_MESSAGE = 18087;

    private boolean mSendHandShakeReply = false;
    private ControllerThread mControllerThread;

    @Override
    public void stopSelfService() {
        super.stopSelfService();
        mApControl.stopAp();
    }

    @Override
    protected void createServiceThread() {
        mControllerThread = new ControllerThread();
        mServiceThread = mControllerThread;
    }

    class ControllerThread extends ServiceThread {
        private final Object mWifiApEnabledLock;
        private boolean mSetupApResult;

        public ControllerThread() {
            super();
            setName("RCController");
            mWifiApEnabledLock = new Object();
            mSetupApResult = false;
        }

        protected RCHeartBeat createRCHeartBeat(InputStream inputStream, OutputStream outputStream) {
            return new RCHeartBeat(true, inputStream, outputStream);
        }

        protected boolean waitForReConnect() {
            return false;
        }

        private void wifiApEnabled(boolean success) {
            notifyAll(mWifiApEnabledLock);
            mSetupApResult = success;
        }

        protected boolean setupWifiConnection(WifiConnectCallback callback) {
            mSetupApResult = false;
            int startRes = mApControl.startTangoCarAp(Utils.getDeviceApName(), new ApControl.WifiApCallback() {
                @Override
                public void onEnabled(boolean success) {
                    wifiApEnabled(success);
                }
            });
            if (!mSetupApResult) {
                if (startRes == 1) {
                    mSetupApResult = true;
                } else if (startRes == 0) {
                    if (wait(mWifiApEnabledLock)) {
                        Log.w(TAG, "wait mWifiApEnabledLock, canceled!", new Exception());
                        return true;
                    }
                }
            }
            if (mSetupApResult) {
                callback.connectSuccess();
            } else {
                callback.connectFailed();
            }
            return false;
        }

        protected void createConnectionThreads() {
            mHeartBeatConnectionThread = new ServerSocketConnectionThread(DEF_PORT_HEARTBEAT, 1000);
            mHeartBeatConnectionThread.setName("RCHeartBeatController");
            mMessageConnectionThread = new ServerSocketConnectionThread(DEF_PORT_MESSAGE, 0);
            mMessageConnectionThread.setName("RCMessageController");
        }

        @Override
        protected void beginToShakeHand() {
            mSendHandShakeReply = false;
        }

        @Override
        protected void onShakeHand() {
            RCMessage message = receiveRCMessage();
            if (message != null) {
                if (!mSendHandShakeReply) {
                    if (message.isType(RCMessage.TYPE_HANDSHAKE)) {
                        RCMessage reply = message.getReply();
                        sendRCMessageInner(reply);
                        mSendHandShakeReply = true;
                    }
                } else {
                    if (message.isType(RCMessage.TYPE_HANDSHAKE_OK)) {
                        shakeHandOK();
                    }
                }
            }
        }
    }
}
