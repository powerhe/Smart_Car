package com.lenovo.newdevice.tangocar.remote;

import android.util.Log;

import com.lenovo.newdevice.tangocar.connection.net.ClientSocketConnectionThread;
import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;
/**
 * Created by liujk2 on 2017/1/26.
 */

public class RemoteControlHost extends RCConnectionService {
    private boolean mReceivedHandShakeReply = false;
    private HostThread mHostThread;

    @Override
    protected void createServiceThread() {
        mHostThread = new HostThread();
        mServiceThread = mHostThread;
    }

    public void setSSID(String ssid) {
        mHostThread.setSSID(ssid);
    }

    class HostThread extends ServiceThread {
        private final Object mWaitSSIDLock;
        private final Object mWifiConnectedLock;
        private String mSSID = null;
        private String mServerIp = null;
        private boolean mWifiConnectOK;

        public HostThread() {
            super();
            setName("RCHost");
            mWaitSSIDLock = new Object();
            mWifiConnectedLock = new Object();
        }

        protected RCHeartBeat createRCHeartBeat(InputStream inputStream, OutputStream outputStream) {
            return new RCHeartBeat(false, inputStream, outputStream);
        }

        private void wifiConnected(boolean connected) {
            notifyAll(mWifiConnectedLock);
            mWifiConnectOK = connected;
        }

        protected boolean setupWifiConnection(WifiConnectCallback callback) {
            if (waitForSetSSID()) {
                return true;
            }
            mWifiConnectOK = false;
            int startRes = mApControl.connectToTangoCarAp(mSSID, new ApControl.WifiCallback() {
                @Override
                public void onConnected() {
                    wifiConnected(true);
                }
                @Override
                public void onDisconnected() {
                    Log.w(TAG, "wifi connected failed", new Exception());
                    wifiConnected(false);
                }
            });
            if (!mWifiConnectOK) {
                if (startRes == 1) {
                    mWifiConnectOK = true;
                } else if (startRes == 0) {
                    if (wait(mWifiConnectedLock)) {
                        Log.w(TAG, "wait mWifiConnectedLock, canceled!", new Exception());
                        return true;
                    }
                }
            }
            if (mWifiConnectOK) {
                callback.connectSuccess();
                mServerIp = mApControl.getDhcpGatewayIP();
            } else {
                callback.connectFailed();
            }
            return false;
        }

        protected void createConnectionThreads() {
            mHeartBeatConnectionThread = new ClientSocketConnectionThread(mServerIp, RemoteController.DEF_PORT_HEARTBEAT, 1000);
            mHeartBeatConnectionThread.setName("RCHeartBeatHost");
            mMessageConnectionThread = new ClientSocketConnectionThread(mServerIp, RemoteController.DEF_PORT_MESSAGE, 0);
            mMessageConnectionThread.setName("RCMessageHost");
        }

        public void setSSID(String ssid) {
            synchronized (mWaitSSIDLock) {
                mSSID = ssid;
                if (mSSID != null) {
                    notifyAll(mWaitSSIDLock);
                }
            }
        }

        private boolean waitForSetSSID() {
            if (mSSID == null) {
                return wait(mWaitSSIDLock);
            } else {
                return false;
            }
        }

        @Override
        protected void beginToShakeHand() {
            mReceivedHandShakeReply = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    RCMessage message = receiveRCMessage();
                    if (message != null) {
                        if (message.isReplyForType(RCMessage.TYPE_HANDSHAKE)) {
                            mReceivedHandShakeReply = true;
                            message = RCMessage.getMessageForType(RCMessage.TYPE_HANDSHAKE_OK);
                            sendRCMessageInner(message);
                            shakeHandOK();
                        }
                    }
                }
            }).start();
        }

        @Override
        protected void onShakeHand() {
            if (!mReceivedHandShakeReply) {
                RCMessage message = RCMessage.getMessageForType(RCMessage.TYPE_HANDSHAKE);
                sendRCMessageInner(message);
                Utils.sleep(500);
            }
        }
    }
}
