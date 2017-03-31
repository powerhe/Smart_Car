package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;
import android.graphics.Point;

import com.lenovo.newdevice.tangocar.data.CarConfigValue;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarStatus;
import com.lenovo.newdevice.tangocar.data.FrontDistanceInfo;
import com.lenovo.newdevice.tangocar.data.CarData;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.main.PointCloudProcess;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.control.ElectronicFence;
import com.lenovo.newdevice.tangocar.main.TangoCar;
import com.lenovo.newdevice.tangocar.map.WorldMapGrid;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.ProcessThread;
import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.VoiceReporter;

/**
 * Created by liujk2 on 2017/1/5.
 */

public class BaseEngine extends ProcessThread {
    public static final int ENGINE_TYPE_NONE = 0;
    public static final int ENGINE_TYPE_SIMPLE = 1;
    public static final int ENGINE_TYPE_TARGET = 2;
    public static final int ENGINE_TYPE_NORMAL = 3;
    public static final int ENGINE_TYPE_TURN_TEST = 4;
    public static final int ENGINE_TYPE_FORWARD_TEST = 5;

    private static final long ONE_WHILE_MAX_SLEEP = 100;

    protected Activity mActivity;
    protected WorldMapGrid mMap;
    protected CarData mCarData;
    protected CarPose mCarPose;
    protected CarStatus mCarStatus;

    protected AdvanceControl mAdvanceControl;
    protected CarControl mCarControl;

    protected TangoCar mTangoCar;

    protected ElectronicFence mElectronicFence;
    protected boolean mHasElectFence;

    public enum Direction {
        NONE, FRONT, BACK, LEFT, RIGHT
    }

    public enum TurnDirection {
        NONE, TURN_LEFT, TURN_RIGHT
    }
    protected TurnDirection mTurnDirection;

    private float mMinDistance;
    private float mMinAveDistance;
    private boolean mHasStop;
    private boolean mIsPaused;
    protected long mDelayStartTime;
    private UpdateStatusThread mUpdateStatusThread;

    public BaseEngine(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(ONE_WHILE_MAX_SLEEP);
        mActivity = activity;
        mMap = globalData.getMap();
        mCarData = globalData.getCarData(0);
        mCarPose = mCarData.getPose();
        mCarStatus = mCarData.getStatus();
        mAdvanceControl = advanceControl;
        mCarControl = carControl;
        mHasStop = false;
        setName(this.getClass().getSimpleName());
        mDelayStartTime = 0;
    }

    private void setTangoCar(TangoCar tangoCar) {
        mTangoCar = tangoCar;
    }

    protected void setControlData(DataSerializable controlData) {
    }

    public boolean start(DataSerializable controlData) {
        start();
        return true;
    }

    public void setStartDelay(long time) {
        mDelayStartTime = time;
    }

    synchronized public void applyConfig(CarConfigValue configValue) {
        setMinDistance(configValue.mMinDistance, configValue.mMinAveDistance);
        if (configValue.mEnableFence) {
            enableElectronicFence(true);
            setupElectronicFence(0, configValue.mFenceRight, configValue.mFenceFront, 0);
        }
        if (configValue.mStartDelay) {
            setStartDelay(configValue.mDelayStart);
        }
    }

    synchronized public void enableElectronicFence(boolean enable) {
        if (enable) {
            mHasElectFence = true;
            if (mElectronicFence == null) {
                mElectronicFence = new ElectronicFence(mCarData);
            }
        } else {
            mHasElectFence = false;
            mElectronicFence = null;
        }
    }

    synchronized public void setupElectronicFence(float left, float right, float front, float back) {
        if (!mHasElectFence) {
            return;
        }
        if (mElectronicFence == null) {
            mElectronicFence = new ElectronicFence(mCarData);
        }
        mElectronicFence.setup(left, right, front, back);
    }

    synchronized public void setMinDistance(float distance, float aveDistance) {
        mMinDistance = distance;
        mMinAveDistance = aveDistance;
    }

    protected boolean shouldTurn() {
        FrontDistanceInfo frontDistanceInfo = PointCloudProcess.getDistanceInfo();
        if (frontDistanceInfo.valid) {
            if (frontDistanceInfo.minDistance < mMinDistance
                    ||frontDistanceInfo.aveDistance < mMinAveDistance) {
                // should turn
                if (frontDistanceInfo.nearestGridX < 0) {
                    mTurnDirection = TurnDirection.TURN_LEFT;
                } else {
                    mTurnDirection = TurnDirection.TURN_RIGHT;
                }
                return true;
            }
        }
        if (mHasElectFence && mElectronicFence != null) {
            ElectronicFence.DistanceInfo distanceInfo = mElectronicFence.getDistanceInfo();
            if (distanceInfo.minDistance < mMinDistance) {
                mTurnDirection = distanceInfo.suggestTurnDirection;
                soundByResId(R.string.sound_str_electronic_fence);
                return true;
            }
        }
        return false;
    }

    protected void soundByResId(int resId) {
        VoiceReporter.from(mActivity.getApplicationContext()).report(mActivity.getString(resId));
    }

    protected String getEngineState() {
        return null;
    }

    protected Point getCurrentTarget() {
        return null;
    }

    @Override
    protected boolean runCondition() {
        if (mDelayStartTime > 0) {
            Utils.sleep(mDelayStartTime);
            mDelayStartTime = 0;
        }
        if (!mCarStatus.isTangoConnected()) {
            return false;
        }
        if (mCarPose.getCurrentGridIndex() == null) {
            return false;
        }
        if (mHasStop) {
            return false;
        }
        if (mIsPaused) {
            return false;
        }
        return true;
    }

    @Override
    protected void doInit() {
        mUpdateStatusThread = new UpdateStatusThread();
        mUpdateStatusThread.start();
    }

    @Override
    protected void doOver() {
        mCarControl.stop();
        mUpdateStatusThread.cancel();
    }

    public void pauseCar() {
        mIsPaused = true;
        mCarControl.pause();
    }

    public void resumeCar() {
        mCarControl.resume();
        mIsPaused = false;
    }

    @Override
    public void cancel() {
        super.cancel();
        mHasStop = true;
        mCarControl.stop();
    }

    public static BaseEngine createEngine(int engineType, Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl, TangoCar tangoCar) {
        BaseEngine baseEngine = null;
        switch (engineType) {
            case ENGINE_TYPE_SIMPLE:
                baseEngine = new SimpleEngine(activity, globalData, carControl, advanceControl);
                break;
            case ENGINE_TYPE_TARGET:
                baseEngine = new TargetEngine(activity, globalData, carControl, advanceControl);
                break;
            case ENGINE_TYPE_NORMAL:
                baseEngine = new NormalEngine(activity, globalData, carControl, advanceControl);
                break;
            case ENGINE_TYPE_TURN_TEST:
                baseEngine = new TurnTestEngine(activity, globalData, carControl, advanceControl);
                break;
            case ENGINE_TYPE_FORWARD_TEST:
                baseEngine = new ForwardTest(activity, globalData, carControl, advanceControl);
                break;
            case ENGINE_TYPE_NONE:
            default:
                throw new IllegalArgumentException("error engine type " + engineType);
        }
        baseEngine.setTangoCar(tangoCar);
        return baseEngine;
    }

    private class UpdateStatusThread extends Thread {
        boolean isRunning = true;
        public void run() {
            while(isRunning) {
                mCarStatus.setEngineState(getEngineState());
                mCarStatus.setEngineTarget(getCurrentTarget());
                Utils.sleep(ONE_WHILE_MAX_SLEEP);
            }
            mCarStatus.setEngineState(null);
            mCarStatus.setEngineTarget(null);
        }
        public void cancel() {
            isRunning = false;
        }
    }
}
