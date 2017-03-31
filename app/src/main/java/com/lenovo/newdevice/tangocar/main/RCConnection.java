package com.lenovo.newdevice.tangocar.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.IBinder;
import android.util.Log;

import com.lenovo.newdevice.tangocar.MainActivity;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.connection.Connection;
import com.lenovo.newdevice.tangocar.connection.ConnectionCallback;
import com.lenovo.newdevice.tangocar.control.engine.BaseEngine;
import com.lenovo.newdevice.tangocar.data.CarConfigValue;
import com.lenovo.newdevice.tangocar.data.CarControlValue;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarStatus;
import com.lenovo.newdevice.tangocar.data.FrontDistanceInfo;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.data.PointsQueue;
import com.lenovo.newdevice.tangocar.data.RemoteStatus;
import com.lenovo.newdevice.tangocar.map.MapPath;
import com.lenovo.newdevice.tangocar.map.WorldMapGrid;
import com.lenovo.newdevice.tangocar.remote.RCConnectionCallback;
import com.lenovo.newdevice.tangocar.remote.RCConnectionService;
import com.lenovo.newdevice.tangocar.remote.RCMessage;
import com.lenovo.newdevice.tangocar.remote.RemoteControlHost;
import com.lenovo.newdevice.tangocar.remote.RemoteController;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.InfoDialog;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Queue;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/14.
 */

public class RCConnection {
    private boolean mConnected;
    private Boolean mLastBoundIsRemoter;
    private Boolean mIsRemoter;
    private boolean mServiceBound;
    private boolean mServiceBinding;
    private String mControllerName = null;
    private boolean mMasterCar;

    private ServiceConnection mServiceConnection;
    private ServiceConnection mRemoterServiceConnection = new RCServiceConnection();
    private ServiceConnection mHostServiceConnection = new RCServiceConnection();

    private RCConnectionService mRCService;
    private RemoteControlHost mRCHost;
    private RemoteController mRCController;
    private final Object mRCHostLock = new Object();

    private GlobalData mGlobalData;
    private RemoteStatus mRemoteStatus;

    private MainActivity mActivity;
    private TangoCar mTangoCar;

    public RCConnection(MainActivity activity, TangoCar tangoCar, GlobalData globalData) {
        mActivity = activity;
        mTangoCar = tangoCar;
        mGlobalData = globalData;
        mRemoteStatus = globalData.getRemoteStatus();
        mLastBoundIsRemoter = null;
        mServiceBound = false;
        mServiceBinding = false;
        mMasterCar = false;
    }

    public void onCreate() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onDestroy() {
        unbindConnection();
    }

    private boolean isRemoter() {
        if (mIsRemoter == null) {
            mIsRemoter = mActivity.isController();
        }
        return mIsRemoter;
    }

    public String getControllerName() {
        return mControllerName;
    }

    public void setupControllerName(String name) {
        if (!isRemoter()) {
            mControllerName = name;
            if (mControllerName != null) {
                if (mRCHost != null) {
                    mRCHost.setSSID(mControllerName);
                    mRCHost.reConnect();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mRCHostLock) {
                                try {
                                    mRCHostLock.wait();
                                    mRCHost.setSSID(mControllerName);
                                } catch (Exception e) {
                                    Log.e(TAG, "wait for RCHost error:", e);
                                }
                            }
                        }
                    }).run();
                }
            }
        }
    }

    private void showControllerDisconnectedDialog() {
        Utils.showToast(mActivity, mActivity.getString(
                R.string.toast_controller_disconnected, mControllerName));
        if (mTangoCar != null) {
            mTangoCar.searchController(true);
        }
    }

    private void serverDisconnected() {
        mActivity.runOnIdleThread(new Runnable() {
            @Override
            public void run() {
                if (!isRemoter()) {
                    showControllerDisconnectedDialog();
                }
            }
        });
    }

    private void showControllerInvalidDialog() {
        Utils.showToast(mActivity, mActivity.getString(
                R.string.toast_controller_invalid, mControllerName));
        if (mTangoCar != null) {
            mTangoCar.searchController(true);
        }
    }

    private void serverInvalid() {
        mActivity.runOnIdleThread(new Runnable() {
            @Override
            public void run() {
                Runnable finishActivityAction = new Runnable() {
                    @Override
                    public void run() {
                        mActivity.finish();
                    }
                };

                if (isRemoter()) {
                    InfoDialog.alert(mActivity, mActivity.getString(
                            R.string.alert_cant_as_controller),
                            finishActivityAction);
                } else {
                    showControllerInvalidDialog();
                }
            }
        });
    }

    class RCServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Connection.ConnectionBinder connectionBinder = (Connection.ConnectionBinder) iBinder;
            mServiceBinding = false;
            mServiceBound = true;
            setupConnection(connectionBinder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mRCService = null;
            mRCHost = null;
            mRCController = null;
        }
    }

    public void bindConnection(boolean isRemoter) {
        Log.i(TAG, "RCConnection bindConnection(" + isRemoter + "), mServiceBound is " + mServiceBound + ", mServiceBinding is " + mServiceBinding);
        if (mServiceBound || mServiceBinding) {
            if (mLastBoundIsRemoter != null && mLastBoundIsRemoter != isRemoter) {
                unbindConnection();
            } else {
                return;
            }
        }
        mLastBoundIsRemoter = isRemoter;

        mServiceBinding = true;

        Intent intent = new Intent(mActivity, RCConnectionService.getServiceClass(isRemoter));

        Log.i(TAG, "RCConnection call mActivity.bindService");
        if (isRemoter) {
            mServiceConnection = mRemoterServiceConnection;
        } else {
            mServiceConnection = mHostServiceConnection;
        }
        mActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindConnection() {
        Log.i(TAG, "RCConnection unbindConnection(), mServiceBound is " + mServiceBound + ", mServiceBinding is " + mServiceBinding);
        if (!mServiceBound && !mServiceBinding) {
            return;
        }
        Log.i(TAG, "RCConnection call mActivity.unbindService.");
        mActivity.unbindService(mServiceConnection);
        mServiceBound = false;
        mServiceBinding = false;
        if (!mLastBoundIsRemoter) {
            synchronized (mRCHostLock) {
                mRCHostLock.notifyAll();
            }
        }
    }

    private void setConnectionStatus(final boolean connected) {
        Log.i(TAG, "setConnectionStatus("+connected+")", new Exception());
        if (connected && !mConnected) {
            connectedOK();
        }
        mRemoteStatus.connected = mConnected = connected;
    }

    private void setupConnection(Connection connection) {
        Log.i(TAG, "RCConnection setupConnection, connection is " + connection);
        if (connection == null) {
            mRCService = null;
        } else if (connection instanceof RCConnectionService) {
            boolean isRemoter = isRemoter();

            mRCService = (RCConnectionService) connection;

            if (mRCService instanceof RemoteControlHost) {
                if (isRemoter) {
                    // should not run here
                    return;
                }
                synchronized (mRCHostLock) {
                    mRCHost = (RemoteControlHost) mRCService;
                    if (mControllerName == null) {
                        mRCHostLock.notifyAll();
                    } else {
                        mRCHost.setSSID(mControllerName);
                    }
                }
            }

            if (mRCService instanceof RemoteController) {
                if (!isRemoter) {
                    // should not run here
                    return;
                }
                mRCController = (RemoteController) mRCService;
            }

            if (mRCService.isConnected()) {
                setConnectionStatus(true);
            } else {
                setConnectionStatus(false);
            }

            mRCService.setConnectionCallback(new ConnectionCallback() {
                @Override
                public void onConnected() {
                    setConnectionStatus(true);
                }

                @Override
                public void onDisconnected() {
                    setConnectionStatus(false);
                    serverDisconnected();
                }

                @Override
                public void onInvalid() {
                    serverInvalid();
                }
            });

            mRCService.setRCConnectionCallback(new RCConnectionCallback() {
                @Override
                public void onReceivedRCMessage(RCMessage message) {
                    onRCMessageReceived(message);
                }
            });
        }
    }

    private void connectedOK() {
        if (isRemoter()) {
            connectedOneCar();
        } else {
            connectedToController();
        }
    }

    private void connectedToController() {
        if (mTangoCar != null) {
            mTangoCar.saveControllerNameToPreferences(mControllerName);
        }
    }

    private void connectedOneCar() {
        if (isRemoter()) {
            boolean clientIsMaster = true;
            if (clientIsMaster) {
                sendControlRCMessage(RCMessage.TYPE_CTL_SET_MASTER);
                // get all info from car
                sendControlRCMessage(RCMessage.TYPE_CTL_GET_ALL);
            } else {
                sendControlRCMessage(RCMessage.TYPE_CTL_SET_SLAVE);
                sendControlRCMessage(RCMessage.TYPE_CTL_CLEAR_MAP);
                // send map and adf
                sendMap(mGlobalData.getMap());
            }
        }
    }

    private void sendCarAllInfo() {
        if (!isRemoter()) {
            if (mMasterCar) {
                sendControlRCMessage(RCMessage.TYPE_CTL_CLEAR_MAP);
            }
            sendMap(mGlobalData.getMap());
            sendPath(mGlobalData.getPath(0));
            // send car config
            sendCarConfig(mGlobalData.getCarConfig(0));
        }
    }

    private void applyCarControl(CarControlValue controlValue) {
        int cmd = controlValue.mControlCmd;
        switch (cmd) {
            case CarControlValue.CONTROL_CMD_START:
                startHostCar(controlValue);
                break;
            case CarControlValue.CONTROL_CMD_STOP:
                stopHostCar();
                break;
        }
    }

    // for host
    private void startHostCar(CarControlValue controlValue) {
        if (!mTangoCar.isRunning()) {
            boolean startOK = mTangoCar.startEngine(controlValue.mEngineType, controlValue.getControlData());
            if (startOK) {
                mTangoCar.setControlViewTarget(controlValue.mEngineType, controlValue.getControlData());
            }
        }
    }

    // for host
    private void stopHostCar() {
        if (mTangoCar.isRunning()) {
            mTangoCar.stopEngine();
        }
    }

    // for controller
    public void startClientEngine(int engineType, DataSerializable controlData) {
        CarControlValue controlValue = new CarControlValue(
                engineType, CarControlValue.CONTROL_CMD_START);
        if (controlData != null) {
            controlValue.setControlData(controlData);
        }
        sendCarControl(controlValue);
    }

    // for controller
    public void stopClientEngine() {
        CarControlValue controlValue = new CarControlValue(
                BaseEngine.ENGINE_TYPE_NONE,
                CarControlValue.CONTROL_CMD_STOP);
        sendCarControl(controlValue);
    }

    private void onHostReceiveRCMessage(RCMessage message) {
        switch (message.getType()) {
            case RCMessage.TYPE_CTL_GET_ALL:
                sendCarAllInfo();
                break;
            case RCMessage.TYPE_CTL_SET_MASTER:
                mMasterCar = true;
                break;
            case RCMessage.TYPE_CTL_SET_SLAVE:
                mMasterCar = false;
                break;
            case RCMessage.TYPE_DATA_CAR_CONTROL:
                CarControlValue controlValue = (CarControlValue)RCMessage.getObjectFromMessage(message);
                applyCarControl(controlValue);
                break;
            case RCMessage.TYPE_CTL_GET_LOG:
                String log = Utils.getLog();
                RCMessage logMessage = RCMessage.getMessageFromString(RCMessage.TYPE_DATA_LOG, log);
                sendRCMessage(logMessage);
                break;
        }
    }

    private void onControllerReceiveRCMessage(RCMessage message) {
        switch (message.getType()) {
            case RCMessage.TYPE_DATA_CAR_POSE:
                CarPose carPose = (CarPose) RCMessage.getObjectFromMessage(message);
                mGlobalData.updateCarPose(0, carPose);
                break;
            case RCMessage.TYPE_DATA_CAR_DISTANCE:
                FrontDistanceInfo distanceInfo = (FrontDistanceInfo) RCMessage.getObjectFromMessage(message);
                mGlobalData.setCarDistance(0, distanceInfo);
                break;
            case RCMessage.TYPE_DATA_CAR_STATUS:
                CarStatus carStatus = (CarStatus) RCMessage.getObjectFromMessage(message);
                mGlobalData.setCarStatus(0, carStatus);
                break;
            case RCMessage.TYPE_DATA_CAR_EXPECTED_PATH:
                PointsQueue pointsQueue = (PointsQueue) RCMessage.getObjectFromMessage(message);
                mGlobalData.updateCarExpectedPath(0, pointsQueue.getQueue());
                break;
            case RCMessage.TYPE_DATA_LOG:
                String log = RCMessage.getStringFromMessage(message);
                mTangoCar.applyLog(log);
                break;
        }
    }

    private boolean processCommonRCMessage(RCMessage message) {
        boolean processed = true;
        switch (message.getType()) {
            case RCMessage.TYPE_CTL_CLEAR_MAP:
                mGlobalData.clearMap();
                break;
            case RCMessage.TYPE_CTL_CLEAR_PATH:
                mGlobalData.clearCarPath(0);
                break;
            case RCMessage.TYPE_DATA_MAP:
                WorldMapGrid map = (WorldMapGrid)RCMessage.getObjectFromMessage(message);
                mGlobalData.updateMap(map);
                break;
            case RCMessage.TYPE_DATA_CAR_PATH:
                MapPath path = (MapPath)RCMessage.getObjectFromMessage(message);
                mGlobalData.setCarPathAs(0, path);
                break;
            case RCMessage.TYPE_DATA_CAR_CONFIG:
                CarConfigValue configValue = (CarConfigValue)RCMessage.getObjectFromMessage(message);
                // call TangoCar apply and config config
                mTangoCar.applyConfigFromRemote(0, configValue);
                break;
            default:
                processed = false;
                break;
        }
        return processed;
    }

    private void onRCMessageReceived(RCMessage message) {
        if (message == null) {
            return;
        }
        if (processCommonRCMessage(message)) {
            return;
        }
        if (isRemoter()) {
            onControllerReceiveRCMessage(message);
        } else {
            onHostReceiveRCMessage(message);
        }
    }

    public void sendCarConfig(CarConfigValue configValue) {
        sendObject(RCMessage.TYPE_DATA_CAR_CONFIG, configValue);
    }

    public void sendCarControl(CarControlValue controlValue) {
        sendObject(RCMessage.TYPE_DATA_CAR_CONTROL, controlValue);
    }

    public void sendCarStatus(CarStatus carStatus) {
        sendObject(RCMessage.TYPE_DATA_CAR_STATUS, carStatus);
    }

    public void sendCarPose(CarPose carPose) {
        sendObject(RCMessage.TYPE_DATA_CAR_POSE, carPose);
    }

    public void sendDistanceInfo(FrontDistanceInfo distanceInfo) {
        sendObject(RCMessage.TYPE_DATA_CAR_DISTANCE, distanceInfo);
    }

    public void sendMap(WorldMapGrid map) {
        sendObject(RCMessage.TYPE_DATA_MAP, map);
    }

    public void sendPath(MapPath path) {
        sendObject(RCMessage.TYPE_DATA_CAR_PATH, path);
    }

    public void sendExpectedPath(Queue<Point> pathPoints) {
        sendObject(RCMessage.TYPE_DATA_CAR_EXPECTED_PATH, new PointsQueue(pathPoints));
    }

    public void sendObject(byte type, DataSerializable obj) {
        if (mRCService != null) {
            mRCService.sendObject(type, obj);
        }
    }

    public void sendControlRCMessage(byte type) {
        if (mRCService != null) {
            mRCService.sendControlRCMessage(type);
        }
    }

    public void sendRCMessage(RCMessage message) {
        if (mRCService != null) {
            mRCService.sendRCMessage(message);
        }
    }
}
