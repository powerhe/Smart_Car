package com.lenovo.newdevice.tangocar.main;

import android.graphics.Point;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.lenovo.newdevice.tangocar.MainActivity;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.control.engine.BaseEngine;
import com.lenovo.newdevice.tangocar.data.CarConfigValue;
import com.lenovo.newdevice.tangocar.data.CarData;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarStatus;
import com.lenovo.newdevice.tangocar.data.FrontDistanceInfo;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.data.RemoteStatus;
import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.remote.RCConnectionService;
import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.map.PointCloudGrid;
import com.lenovo.newdevice.tangocar.map.WorldMapGrid;
import com.lenovo.newdevice.tangocar.remote.RCMessage;
import com.lenovo.newdevice.tangocar.utils.AppConfig;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.InfoDialog;
import com.lenovo.newdevice.tangocar.utils.ProcessThread;
import com.lenovo.newdevice.tangocar.utils.SaveMapTask;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;
import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.VoiceReporter;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;
/**
 * Created by liujk2 on 2017/1/23.
 */

public class TangoCar {
    private static final boolean DEBUG = false;
    private static final int REQUEST_SEARCHING_ACTIVITY = 100;

    private RunMode mRunMode;
    public enum RunMode {NONE, STUDY, MISSION, CONTROLLER};
    private boolean mIsRunning = false;
    private boolean mIsPaused = false;
    private boolean mProcessSetup = false;

    private MainActivity mActivity;
    private ControlView mControlView;
    private TextView mLogView;

    private Tango mTango;
    private TangoUx mTangoUx;
    private TangoPointCloudManager mPointCloudManager;

    private SaveMapTask mSaveMapTask;

    private SearchControllerThread mSearchThread;

    private ProcessThread mProcessThread;
    private ProcessThread mStudyThread;
    private ProcessThread mDevelopThread;

    private boolean mNeedAreaLearning;
    private boolean mLoadADF;
    private String mAdfUUID;

    private int mWorldCoordinateFrame = TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE;

    private boolean mNeedSetupControllerName;
    private boolean mTangoDestroyed;

    private GlobalData mGlobalData;
    private CarStatus mCarStatus;
    private CarControl mCarControl;
    private AdvanceControl mAdvanceControl;

    private RCConnection mRCConnection;
    private CarConnection mCarConnection;
    private PointCloudProcess mPointCloudProcess;
    private WorldMapGrid mMap;
    private BaseEngine mEngine;

    private boolean mReceivedCarConfig;

    private boolean mUseNewControl;

    ConfigView mConfigView;
    CarConfigValue mConfigValue;
    private boolean mLastRemoteControl;

    public TangoCar(MainActivity activity, ControlView controlView) {
        mRunMode = RunMode.NONE;
        mActivity = activity;
        mControlView = controlView;
        mControlView.setTangoCar(this);
        mNeedSetupControllerName = false;
        mTangoDestroyed = true;
    }

    public void onCreate() {
        mConfigView = new ConfigView(mActivity);

        mGlobalData = new GlobalData();
        mMap = mGlobalData.getMap();
        mLogView = (TextView) mActivity.findViewById(R.id.log_info);

        if (mActivity.isController()) {
            RCConnectionService.start(mActivity, true);
            mGlobalData.getRemoteStatus().type = RemoteStatus.ClientType.CONTROLLER;
            mConfigValue = new CarConfigValue();
        } else {
            mGlobalData.getRemoteStatus().type = RemoteStatus.ClientType.HOST;
            CarData carData = mGlobalData.getCarData(0);
            mCarStatus = carData.getStatus();
            mConfigValue = carData.getConfig();

            mCarConnection = new CarConnection(mActivity, mCarStatus);
            mCarControl = new CarControl(mActivity, mCarConnection, carData);
            mAdvanceControl = new AdvanceControl(mCarControl, carData);

            mTangoUx = setupTangoUxAndLayout();
            mPointCloudManager = new TangoPointCloudManager();

            mPointCloudProcess = new PointCloudProcess();

            mCarConnection.onCreate();
        }

        mRCConnection = new RCConnection(mActivity, this, mGlobalData);
        mRCConnection.onCreate();

        mActivity.setGlobalData(mGlobalData);

        mUseNewControl = false;

        mConfigView.onCreate();

        mConfigValue.update(CarConfigValue.DEFAULT);
    }

    public void onStart() {
    }

    public void onResume() {
        if (mActivity.isController()) {
            mGlobalData.getRemoteStatus().enabled = true;
            mRCConnection.bindConnection(true);
        } else {
            setupTango();
            mActivity.runOnIdleThread(new Runnable() {
                @Override
                public void run() {
                    PointCloudGrid.setGridWidth(GridInfo.sGridWidth);
                }
            });

            mCarConnection.onResume();
            resumeCar();

            mConfigValue.loadFromPreferences();
            mConfigView.applyConfigToView(mConfigValue);
            applyConfig();
        }
        mRCConnection.onResume();
    }

    public void onPause() {
        mRCConnection.onPause();
        if (mActivity.isController()) {
        } else {
            destroyTango();
            mCarConnection.onPause();
            pauseCar();
        }
    }

    public void onStop() {
        if (!mActivity.isController()) {
            stopEngine();
        }
    }

    public void onDestroy() {
        if (mActivity.isController()) {
        } else {
            stopAllThread();
            mCarConnection.onDestroy();
            mCarControl.terminate();
            if (mSearchThread != null) {
                mSearchThread.cancel();
                mSearchThread = null;
            }
        }
        mRCConnection.onDestroy();
        RCConnectionService.stopLastService(mActivity);
    }

    private void alertTangoOutOfDate() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InfoDialog.alert(mActivity, mActivity.getString(R.string.alert_tango_out_of_date), new Runnable() {
                    @Override
                    public void run() {
                        mActivity.finish();
                    }
                });
            }
        });
    }

    private void setupTango() {
        if (!mTangoDestroyed) {
            return;
        }
        if (mRunMode == RunMode.NONE) {
            return;
        }
        mTangoDestroyed = false;
        mTangoUx.start(new TangoUx.StartParams());
        mTango = new Tango(mActivity, new Runnable() {
            @Override
            public void run() {
                synchronized (TangoCar.this) {
                    try {
                        TangoSupport.initialize();
                        TangoConfig config = setupTangoConfig(mTango);
                        mTango.connect(config);
                        startupTango();
                        mCarStatus.setTangoConnected(true);
                        setupAfterTangoConnected();
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "Tango init error: ", e);
                        if (e instanceof TangoOutOfDateException) {
                            alertTangoOutOfDate();
                        }
                    }
                }
            }
        });
    }

    private void destroyTango() {
        if (mTangoDestroyed) {
            return;
        }
        if (mRunMode == RunMode.NONE) {
            return;
        }
        mTangoDestroyed = true;
        synchronized (this) {
            try {
                mTangoUx.stop();
                mTango.disconnect();
                mCarStatus.setTangoConnected(false);
            } catch (TangoOutOfDateException e) {
                if (mTangoUx != null) {
                    mTangoUx.showTangoOutOfDate();
                }
                Log.e(TAG, mActivity.getString(R.string.exception_out_of_date), e);
            } catch (TangoErrorException e) {
                Log.e(TAG, mActivity.getString(R.string.exception_tango_error), e);
            } catch (TangoInvalidException e) {
                Log.e(TAG, mActivity.getString(R.string.exception_tango_invalid), e);
            }
        }
    }

    private void restartTango() {
        destroyTango();
        setupTango();
    }

    public void saveControllerNameToPreferences(String name) {
        AppConfig.getConfig().putString(AppConfig.CFG_CONTROLLER_NAME, name);
    }

    public String loadControllerNameToPreferences() {
        return AppConfig.getConfig().getString(AppConfig.CFG_CONTROLLER_NAME, null);
    }

    public void displayLog() {
        if (mActivity.isController()) {
            mRCConnection.sendControlRCMessage(RCMessage.TYPE_CTL_GET_LOG);
        } else {
            String log = Utils.getLog();
            applyLog(log);
        }
    }

    public void applyLog(final String log) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogView.setText(log);
            }
        });
    }

    // for search controller use thread begin

    public void searchController(boolean force) {
        if (mNeedSetupControllerName) {
            String controllerName = mRCConnection.getControllerName();
            if (controllerName == null || force) {
                String loadControllerName = loadControllerNameToPreferences();
                mSearchThread = new SearchControllerThread(3000, loadControllerName);
                mSearchThread.start();
            }
        }
    }

    private void setupControllerName(String name) {
        if (name != null) {
            Utils.showToast(mActivity, mActivity.getString(R.string.toast_connect_controller, name));
            mRCConnection.setupControllerName(name);
        } else {
            Utils.showToast(mActivity, R.string.toast_no_controller);
            mRCConnection.setupControllerName(null);
        }
    }

    class SearchControllerThread extends Thread {
        private long mSearchDelay;
        private boolean mContinue;
        private String mDefName;
        SearchControllerThread(long delayMillis, String defName) {
            mSearchDelay = delayMillis;
            mContinue = true;
            mDefName = defName;
        }

        @Override
        public void run() {
            int count = 0;
            while (mContinue) {
                List<String> list
                        = ApControl.getInstance().getTangoCarApSSIDList();
                count = list.size();
                if (count > 0) {
                    String name = null;
                    if (mDefName == null) {
                        name = list.get(0);
                    } else {
                        for (String n : list) {
                            if (n.equals(mDefName)) {
                                name = mDefName;
                                break;
                            }
                        }
                        if (name == null) {
                            name = list.get(0);
                        }
                    }
                    setupControllerName(name);
                    break;
                } else {
                    if (mConfigValue.mRemoteControl) {
                        setupControllerName(null);
                    } else {
                        break;
                    }
                    Utils.sleep(mSearchDelay);
                }
            }
        }

        public void cancel() {
            mContinue = false;
        }
    }
    // for search controller use thread end

    private void processRemoteControl() {
        if (mConfigValue.mRemoteControl != mLastRemoteControl) {
            mGlobalData.getRemoteStatus().enabled = mConfigValue.mRemoteControl;
            mLastRemoteControl = mConfigValue.mRemoteControl;
            if (mLastRemoteControl) {
                // start service
                RCConnectionService.start(mActivity, false);
                mRCConnection.bindConnection(false);
                mNeedSetupControllerName = true;
                searchController(false);
            } else {
                // stop service
                RCConnectionService.stopLastService(mActivity);
                mRCConnection.unbindConnection();
                mNeedSetupControllerName = false;
            }
        }
    }

    private void applyConfig() {
        if (mActivity.isController()) {
        } else {
            processRemoteControl();
            // left is positive, up is positive
            mPointCloudProcess.applyConfig(mConfigValue);
            if (mEngine != null) {
                mEngine.applyConfig(mConfigValue);
            }
            PointCloudGrid.setVerticalScope(mConfigValue.mBorderTop, 0 - mConfigValue.mBorderBottom);
            WorldMapGrid.setVerticalScope(mConfigValue.mBorderTop, 0 - mConfigValue.mBorderBottom);
            CarControl.setSpeedRate(mConfigValue.mSpeedRate);
        }
    }

    public void saveAndApplyConfig() {
        mConfigView.getConfigFromView(mConfigValue);
        if (mActivity.isController()) {
        } else {
            mConfigValue.saveToPreferences();
            applyConfig();
        }
        if (!mActivity.isController() || mReceivedCarConfig) {
            mRCConnection.sendCarConfig(mConfigValue);
        }
    }

    public void switchRemoteControl() {
        setRemoteControl(!mConfigValue.mRemoteControl);
    }

    public void setRemoteControl(boolean enable) {
        if (mConfigValue.mRemoteControl == enable) {
            return;
        }
        if (enable) {
            VoiceReporter.from(mActivity.getApplicationContext()).report(mActivity.getString(R.string.sound_str_remote_control_enable));
        } else {
            VoiceReporter.from(mActivity.getApplicationContext()).report(mActivity.getString(R.string.sound_str_remote_control_disable));
        }
        mConfigValue.mRemoteControl = enable;
        updateConfig(mConfigValue, false);
        applyConfig();
    }

    private void updateConfig(final CarConfigValue configValue, boolean fromRemote) {
        mConfigValue.update(configValue);
        if (fromRemote) {
            mReceivedCarConfig = true;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConfigView.applyConfigToView(configValue);
            }
        });
    }

    public void applyConfigFromRemote(int carIndex, CarConfigValue configValue) {
        if (configValue == null) {
            return;
        }
        updateConfig(configValue, true);
        if (mActivity.isController()) {
            // receive car config
            mGlobalData.updateCarConfig(carIndex, configValue);
        } else {
            // receive controller config
            applyConfig();
        }
    }

    class PointCloutProcessThread extends ProcessThread {
        private double mPointCloudPreviousTimeStamp;
        private double mPointCloudTimeToNextUpdate = Utils.UPDATE_INTERVAL_MS;

        public PointCloutProcessThread() {
            super(Utils.DEFAULT_MILLISECS_PER_WHILE);
        }

        @Override
        protected boolean runCondition() {
            return mCarStatus.isTangoConnected();
        }

        protected void processPointCloud(TangoPointCloudData pointCloud) {
        }

        @Override
        protected void doAction() {
            TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
            if (pointCloud != null) {
                final double currentTimeStamp = pointCloud.timestamp;
                final double pointCloudFrameDelta =
                        (currentTimeStamp - mPointCloudPreviousTimeStamp) * Utils.SECS_TO_MILLISECS;
                mPointCloudPreviousTimeStamp = currentTimeStamp;

                mPointCloudTimeToNextUpdate -= pointCloudFrameDelta;

                if (mPointCloudTimeToNextUpdate < 0.0) {
                    try {
                        processPointCloud(pointCloud);
                    } catch (Exception e) {
                        Log.i(TAG, "error:", e);
                    }
                }
                mPointCloudTimeToNextUpdate = Utils.UPDATE_INTERVAL_MS;
            }
        }
    }

    private void stopAllThread() {
        if (!mActivity.isController()) {
            mProcessSetup = false;
            if (mProcessThread != null) {
                mProcessThread.cancel();
            }
            if (mStudyThread != null) {
                mStudyThread.cancel();
            }
            if (mDevelopThread != null) {
                mDevelopThread.cancel();
            }
        }
    }

    private void setupAfterTangoConnected() {
        if (!mProcessSetup) {
            mProcessSetup = true;
            startProcessThread();
        }
    }

    private void startProcessThread() {
        if (mActivity.isController()) {
        } else {
            mProcessThread = new PointCloutProcessThread() {
                @Override
                protected void processPointCloud(TangoPointCloudData pointCloud) {
                    mPointCloudProcess.update(pointCloud.points, pointCloud.numPoints);

                    FrontDistanceInfo distanceInfo = PointCloudProcess.getDistanceInfo();
                    mGlobalData.setCarDistance(0, distanceInfo);
                    if (mConfigValue.mRemoteControl) {
                        // send distance info
                        mRCConnection.sendDistanceInfo(distanceInfo);
                    }
                }
            };
            mProcessThread.setName("ProcessThread");
            mProcessThread.start();

            mStudyThread = new PointCloutProcessThread() {
                @Override
                protected boolean runCondition() {
                    return super.runCondition() && mIsRunning;
                }

                @Override
                protected void processPointCloud(TangoPointCloudData pointCloud) {
                    TangoPoseData poseData;
                    int numPoints;

                    try {
                        poseData = TangoSupport.getPoseAtTime(pointCloud.timestamp,
                                mWorldCoordinateFrame,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,
                                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                Surface.ROTATION_0);
                        TangoSupport.TangoMatrixTransformData transform =
                                TangoSupport.getMatrixTransformAtTime(pointCloud.timestamp,
                                        mWorldCoordinateFrame,
                                        TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                        TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                                        Surface.ROTATION_0);
                        if (DEBUG) Log.i(TAG, "transform.statusCode is " + transform.statusCode);
                        if (poseData.statusCode == TangoPoseData.POSE_VALID
                                && transform.statusCode == TangoPoseData.POSE_VALID) {
                            numPoints = pointCloud.numPoints;
                            if (numPoints > 1) {
                                if (DEBUG) Log.i(TAG, "before call PointCloudGrid.getGridFromPointCloud ");
                                PointCloudGrid pointCloudGrid = PointCloudGrid.getGridFromPointCloud(pointCloud.timestamp, pointCloud.points, pointCloud.numPoints);
                                if (DEBUG) Log.i(TAG, "before call mMap.getDeltaMap ");
                                WorldMapGrid deltaMap = mMap.getDeltaMap(pointCloudGrid, transform, (float) poseData.translation[2]);
                                if (mConfigValue.mRemoteControl) {
                                    // send map info
                                    mRCConnection.sendMap(deltaMap);
                                }
                                if (DEBUG) Log.i(TAG, "before call mGlobalData.updateMap ");
                                mGlobalData.updateMap(deltaMap);
                                if (DEBUG) Log.i(TAG, "return from mGlobalData.updateMap ");
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "process point cloud throw exception ", e);
                    }
                }
            };
            mStudyThread.setName("StudyThread");
            mStudyThread.start();
        }
        {
            mDevelopThread = new ProcessThread(Utils.DEFAULT_MILLISECS_PER_WHILE) {
                private boolean mLastRunning = mIsRunning;

                @Override
                protected void doAction() {
                    mRCConnection.sendCarStatus(mCarStatus);
                    boolean isRunning = mIsRunning;
                    if (mLastRunning == isRunning) {
                        return;
                    }
                    if (!isRunning) {
                        Utils.getInstance().closeLogOut();
                        //Utils.outputMapToPgmFile("wroldmap.pgm", mMap);
                        //SerializableUtils.writeObjectToFile(Utils.DIR_TYPE_MAP, "wroldmap.bin", mMap);

                    } else {
                        Utils.getInstance().deleteAllPgmFiles();
                        Utils.getInstance().outLog(null, false);
                    }
                    mLastRunning = isRunning;
                }
            };
            mDevelopThread.setName("DevelopThread");
            mDevelopThread.start();
        }
    }

    public WorldMapGrid getMap() {
        return mMap;
    }

    public void clearCarPath() {
        mGlobalData.clearCarPath(0);
        if (mActivity.isController() || mConfigValue.mRemoteControl) {
            mRCConnection.sendControlRCMessage(RCMessage.TYPE_CTL_CLEAR_PATH);
        }
    }

    public void clearMapAndPath() {
        mGlobalData.clearMap();
        if (mActivity.isController() || mConfigValue.mRemoteControl) {
            mRCConnection.sendControlRCMessage(RCMessage.TYPE_CTL_CLEAR_MAP);
        }
    }

    private TangoUx setupTangoUxAndLayout() {
        TangoUxLayout uxLayout = (TangoUxLayout) mActivity.findViewById(R.id.layout_tangoux);
        TangoUx tangoUx = new TangoUx(mActivity);
        tangoUx.setLayout(uxLayout);
        tangoUx.setUxExceptionEventListener(mUxExceptionListener);
        return tangoUx;
    }

    private UxExceptionEventListener mUxExceptionListener = new UxExceptionEventListener() {

        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(TAG, "Device lying on surface ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(TAG, "Very few depth points in mPoint cloud ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_INCOMPATIBLE_VM) {
                Log.i(TAG, "Device not running on ART");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_OVER_EXPOSED) {
                Log.i(TAG, "Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_TANGO_SERVICE_NOT_RESPONDING) {
                Log.i(TAG, "TangoService is not responding ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_UNDER_EXPOSED) {
                Log.i(TAG, "Camera Under Exposed ");
            }
        }
    };

    private TangoConfig setupTangoConfig(Tango tango) {
        // Use the default configuration plus add depth sensing.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        if (mNeedAreaLearning) {
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }
        if (mLoadADF) {
            String adfUUID = null;
            ArrayList<String> fullUuidList = tango.listAreaDescriptions();
            if (fullUuidList.size() > 0) {
                if (mAdfUUID != null) {
                    for (int i = 0; i < fullUuidList.size(); i++) {
                        if (mAdfUUID.equals(fullUuidList.get(i))) {
                            adfUUID = mAdfUUID;
                            break;
                        }
                    }
                } else {
                    adfUUID = fullUuidList.get(fullUuidList.size() - 1);
                    mAdfUUID = adfUUID;
                }
            }
            if (adfUUID != null) {
                Log.i(TAG, "load adf file \'" + adfUUID + "\'");
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, adfUUID);
            }
        }
        config.putBoolean(TangoConfig.KEY_BOOLEAN_AUTORECOVERY, true);
        return config;
    }

    private void startupTango() {
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Passing in the pose data to UX library produce exceptions.
                if (mTangoUx != null) {
                    mTangoUx.updatePoseStatus(pose.statusCode);
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData pointCloud) {
                if (mTangoUx != null) {
                    mTangoUx.updateXyzCount(pointCloud.numPoints);
                }
                mPointCloudManager.updatePointCloud(pointCloud);
                boolean poseValid = true;
                CarPose carPose = new CarPose();
                carPose.timestamp = pointCloud.timestamp;
                /* update pose date of device */
                try {
                    TangoPoseData pose = TangoSupport.getPoseAtTime(pointCloud.timestamp,
                            mWorldCoordinateFrame,
                            TangoPoseData.COORDINATE_FRAME_DEVICE,
                            TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                            Surface.ROTATION_0);
                    if (pose.statusCode == TangoPoseData.POSE_VALID) {
                        carPose.setDeviceTranslationAndRotation(pose.translation, pose.rotation);
                    }
                } catch (TangoErrorException e) {
                    poseValid = false;
                    Log.e(TAG, "Could not get valid pose from device");
                }
                /* update pose date of Color camera */
                try {
                    TangoPoseData pose = TangoSupport.getPoseAtTime(pointCloud.timestamp,
                            mWorldCoordinateFrame,
                            TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                            TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                            Surface.ROTATION_0);
                    if (pose.statusCode == TangoPoseData.POSE_VALID) {
                        carPose.setCameraTranslationAndRotation(pose.translation, pose.rotation);
                    }
                } catch (TangoErrorException e) {
                    poseValid = false;
                    Log.e(TAG, "Could not get valid pose from COLOR camera");
                }
                if (poseValid) {
                    mGlobalData.updateCarPose(0, carPose);
                    if (mConfigValue.mRemoteControl) {
                        mRCConnection.sendCarPose(carPose);
                    }
                }
            }

            @Override
            public void onTangoEvent(TangoEvent event) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(event);
                }
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }

    public void setControlViewTarget(int engineType, DataSerializable controlData) {
        if (engineType == BaseEngine.ENGINE_TYPE_TARGET) {
            if (controlData != null && controlData instanceof FloatPoint) {
                mControlView.setTarget((FloatPoint)controlData);
            }
        }
    }

    public boolean startEngine(int engineType, DataSerializable controlData) {
        if (mIsRunning) {
            Log.w(TAG, "Tango car is already running!");
            return false;
        }
        boolean startOK = false;
        mIsRunning = true;

        if (mActivity.isController()) {
            mRCConnection.startClientEngine(engineType, controlData);
            startOK = true;
        } else {
            mCarStatus.setRunning(true);
            if (mEngine != null) {
                mEngine.cancel();
            }
            mEngine = BaseEngine.createEngine(engineType, mActivity, mGlobalData, mCarControl, mAdvanceControl, this);
            mEngine.applyConfig(mConfigValue);
            startOK = mEngine.start(controlData);
            VoiceReporter.from(mActivity.getApplicationContext()).report(mActivity.getString(R.string.btn_str_start));
        }

        if (startOK) {
            mControlView.setStartOrStop(true);
        } else {
            mIsRunning = false;
        }
        return startOK;
    }

    public void stopEngine() {
        mIsRunning = false;
        if (mActivity.isController()) {
            mRCConnection.stopClientEngine();
        } else {
            mCarStatus.setRunning(false);
            if (mEngine != null) {
                mEngine.cancel();
                mEngine = null;
            }
            VoiceReporter.from(mActivity.getApplicationContext()).report(mActivity.getString(R.string.btn_str_stop));
        }

        mControlView.setStartOrStop(false);
    }

    public void setExpectedPath(Queue<Point> pathPoints) {
        if (!mActivity.isController()) {
            mGlobalData.updateCarExpectedPath(0, pathPoints);
            mRCConnection.sendExpectedPath(pathPoints);
        }
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void pauseCar() {
        if (mIsPaused) {
            return;
        }
        mIsPaused = true;
        if (!mActivity.isController()) {
            if (mEngine != null) {
                mEngine.pauseCar();
            }
        }
    }

    public void resumeCar() {
        if (!mIsPaused) {
            return;
        }
        mIsPaused = false;
        if (!mActivity.isController()) {
            if (mEngine != null) {
                mEngine.resumeCar();
            }
        }
    }

    public boolean isPaused() {
        return mIsPaused;
    }

    public void switchRunMode(RunMode runMode) {
        if (RunMode.NONE.equals(runMode)) {
            throw new IllegalArgumentException("run mode is " + runMode);
        }
        if (!mActivity.isController()) {
            if (mRunMode != runMode) {
                if (RunMode.STUDY.equals(mRunMode)) {
                    mNeedAreaLearning = true;
                    mLoadADF = false;
                    mAdfUUID = null;
                    mWorldCoordinateFrame = TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE;
                } else if (RunMode.MISSION.equals(mRunMode)) {
                    mNeedAreaLearning = false;
                    mLoadADF = true;
                    mAdfUUID = null;
                    mWorldCoordinateFrame = TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION;
                    loadMap();
                }
                mRunMode = runMode;
                if (mRunMode == RunMode.STUDY || mRunMode == RunMode.MISSION) {
                    restartTango();
                }
            }
        }
    }

    private SaveMapTask.SaveMapListener mSaveMapListener = new SaveMapTask.SaveMapListener() {
        /**
         * Handles failed save from mSaveAdfTask.
         */
        @Override
        public void onSaveMapFailed(String mapName) {
            String toastMessage = String.format(
                    mActivity.getString(R.string.save_map_failed_toast_format),
                    mapName);
            Toast.makeText(mActivity, toastMessage, Toast.LENGTH_LONG).show();
            mSaveMapTask = null;
        }

        /**
         * Handles successful save from mSaveAdfTask.
         */
        @Override
        public void onSaveMapSuccess(String mapName, String adfUuid) {
            String toastMessage = String.format(
                    mActivity.getString(R.string.save_map_success_toast_format),
                    mapName, adfUuid);
            Toast.makeText(mActivity, toastMessage, Toast.LENGTH_LONG).show();
            mSaveMapTask = null;
        }

    };

    public void finish() {
        if (!mActivity.isController()) {
            if (RunMode.STUDY.equals(mRunMode)) {
                mSaveMapTask = new SaveMapTask(mActivity, mSaveMapListener, mTango, mMap, "worldmap2");
                mSaveMapTask.execute();
            }
        }
    }

    private boolean loadMap() {
        String mapName = AppConfig.getConfig().getString(AppConfig.CFG_LAST_MAP_NAME, null);
        String toastMessage = null;
        if (mapName == null) {
            toastMessage = mActivity.getString(R.string.load_map_no_map_toast_format);
        }
        if (mapName != null) {
            WorldMapGrid map = (WorldMapGrid) SerializableUtils.readObjectFromFile(Utils.DIR_TYPE_MAP, mapName + ".bin");
            if (map == null) {
                toastMessage = String.format(
                        mActivity.getString(R.string.load_map_failed_toast_format),
                        mapName);
            } else {
                mMap.clear();
                mMap.update(map);
                return true;
            }
        }
        if (toastMessage != null) {
            Toast.makeText(mActivity, toastMessage, Toast.LENGTH_LONG).show();
        }
        return false;
    }
}
