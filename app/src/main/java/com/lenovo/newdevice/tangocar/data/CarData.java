package com.lenovo.newdevice.tangocar.data;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.data.CarConfigValue;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarSpeed;
import com.lenovo.newdevice.tangocar.data.CarStatus;
import com.lenovo.newdevice.tangocar.data.FrontDistanceInfo;
import com.lenovo.newdevice.tangocar.map.MapPath;

import java.util.Queue;

/**
 * Created by liujk2 on 2017/2/28.
 */
public class CarData {
    int mIndex;
    boolean mValid;
    MapPath mPath;
    FrontDistanceInfo mDistance;
    private final Object mDistanceLock = new Object();
    CarPose mPose;
    private final Object mPoseLock = new Object();
    CarStatus mStatus;
    private final Object mStatusLock = new Object();
    CarConfigValue mConfig;
    private final Object mConfigLock = new Object();
    CarSpeed mSpeed;
    private final Object mSpeedLock = new Object();

    public CarData() {
        mPath = new MapPath();
        mPose = new CarPose();
        mStatus = new CarStatus();
        mConfig = new CarConfigValue();
        mSpeed = new CarSpeed();
    }

    public boolean valid() {
        return mValid;
    }

    public MapPath getPath() {
        return mPath;
    }

    public void clearPath() {
        mPath.clear();
    }

    public void setPath(MapPath path) {
        mPath.setPathAs(path);
    }

    public void setConfig(CarConfigValue config) {
        synchronized (mConfigLock) {
            mConfig = config;
        }
    }

    public void updateConfig(CarConfigValue config) {
        synchronized (mConfigLock) {
            mConfig.update(config);
        }
    }

    public CarConfigValue getConfig() {
        synchronized (mConfigLock) {
            return mConfig;
        }
    }

    public void setDistance(FrontDistanceInfo distance) {
        synchronized (mDistanceLock) {
            mDistance = distance;
        }
    }

    public FrontDistanceInfo getDistance() {
        synchronized (mDistanceLock) {
            return mDistance;
        }
    }

    public void updatePath() {
        synchronized (mPoseLock) {
            mPath.addRunnedGrid(mPose.getCurrentGridIndex());
        }
    }

    public void updateExpectedPath(Queue<Point> path) {
        mPath.updateExpectedPath(path);
    }

    public Queue<Point> getExpectedPath() {
        return mPath.getExpectedPath();
    }

    public void updatePose(CarPose carPose) {
        synchronized (mPoseLock) {
            mPose.update(carPose);
            updatePath();
        }
        updateSpeed(carPose);
    }

    private void updateSpeed(CarPose carPose) {
        synchronized (mSpeedLock) {
            mSpeed.update(carPose);
        }
    }

    public CarSpeed getSpeed() {
        synchronized (mSpeedLock) {
            return mSpeed;
        }
    }

    public CarPose getPose() {
        synchronized (mPoseLock) {
            return mPose;
        }
    }

    public void setStatus(CarStatus carStatus) {
        synchronized (mPoseLock) {
            mStatus = carStatus;
        }
    }

    public CarStatus getStatus() {
        synchronized (mStatusLock) {
            return mStatus;
        }
    }
}
