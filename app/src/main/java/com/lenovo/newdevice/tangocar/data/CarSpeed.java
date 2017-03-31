package com.lenovo.newdevice.tangocar.data;

import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.utils.MathUtils;
import com.lenovo.newdevice.tangocar.utils.SimpleQueue;

import java.lang.reflect.Array;

/**
 * Created by liujk2 on 2017/3/17.
 */

public class CarSpeed {
    private boolean mValid = false;

    private SpeedVal mInstantSpeed = new SpeedVal();
    private SpeedVal mSpeed = new SpeedVal();

    SimpleQueue<CarPose> mPoseQueue = new SimpleQueue<CarPose>(CarPose.class, 7);
    SimpleQueue<SpeedVal> mSpeedQueue = new SimpleQueue<SpeedVal>(SpeedVal.class, 7);

    public boolean valid() {
        return mValid;
    }

    private boolean computeSpeed(SpeedVal speed, CarPose newPose, CarPose oldPose) {
        if (newPose == null || oldPose == null) {
            return false;
        }
        if (newPose.timestamp == oldPose.timestamp) {
            return false;
        }
        double timeDelta = newPose.timestamp - oldPose.timestamp;
        if (newPose.hasCameraPose()
                && oldPose.hasCameraPose()) {
            float degreeDelta = newPose.getCameraYawDegree() - oldPose.getCameraYawDegree();
            degreeDelta = MathUtils.formatDegree180(degreeDelta);
            speed.turn = degreeDelta / timeDelta;
        }
        timeDelta = newPose.timestamp - oldPose.timestamp;
        if (newPose.hasDevicePose()
                && oldPose.hasDevicePose()) {
            double distance = newPose.distanceFrom(oldPose);
            speed.move = distance / timeDelta;
        }
        return true;
    }

    private void updateInner(CarPose currPose) {
        if (currPose == null) {
            return;
        }
        CarPose oldPose = mPoseQueue.get(-1);
        if (oldPose == null) {
            return;
        }
        SpeedVal speed = new SpeedVal();
        computeSpeed(speed, currPose, oldPose);
        mSpeedQueue.put(speed);
        mInstantSpeed.update(speed);
        if (!mPoseQueue.isFull()) {
            return;
        }
        mValid = true;
        oldPose = mPoseQueue.poll();
        computeSpeed(mSpeed, currPose, oldPose);
    }

    synchronized public void update(CarPose currPose) {
        updateInner(currPose);
        mPoseQueue.put(currPose);
    }

    public double getMoveSpeed() {
        return mSpeed.move;
    }

    public double getTurnSpeed() {
        return mSpeed.turn;
    }

    public double getInstantMoveSpeed() {
        return mInstantSpeed.move;
    }

    public double getInstantTurnSpeed() {
        return mInstantSpeed.turn;
    }

    public double getPastMoveSpeed(int i) {
        if (i < 0) {
            return 0;
        }
        SpeedVal speed = mSpeedQueue.get(0 - i - 1);
        if (speed == null) {
            return 0;
        }
        return speed.move;
    }

    public double getPastTurnSpeed(int i) {
        if (i < 0) {
            return 0;
        }
        SpeedVal speed = mSpeedQueue.get(0 - i - 1);
        if (speed == null) {
            return 0;
        }
        return speed.turn;
    }

    class SpeedVal {
        public double move = 0;
        public double turn = 0;
        public void update(SpeedVal speed) {
            move = speed.move;
            turn = speed.turn;
        }
    }
}
