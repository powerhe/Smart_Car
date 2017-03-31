package com.lenovo.newdevice.tangocar.control;

import com.lenovo.newdevice.tangocar.control.engine.BaseEngine;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarData;
import com.lenovo.newdevice.tangocar.map.FloatPoint3D;

/**
 * Created by liujk2 on 2017/1/10.
 */

public class ElectronicFence {
    private float left;
    private float right;
    private float front;
    private float back;

    private CarPose mCarPose;
    private DistanceInfo mDistanceInfo;

    public ElectronicFence(CarData carData) {
        mCarPose = carData.getPose();
        setup(-1.2f, 1.2f, 1.2f, -1.2f);
        init();
    }

    private void init() {
        mDistanceInfo = new DistanceInfo();
    }

    synchronized public void setup(float left, float right, float front, float back) {
        this.left = left;
        this.right = right;
        this.front = front;
        this.back = back;
    }

    // 1. degree to judge direction
    //   0 ~ 90 -> front and right
    //   90 ~ 180 -> front and left
    //   -180 ~ -90 -> back and left
    //   -90 ~ 0 -> back and right
    // 2. judge distance of direction, and get turn direction
    //   0 ~ 90: front -> right, right -> left
    //   90 ~ 180: front -> left, left -> right
    //   -180 ~ -90: back -> right, left -> left
    //   -90 ~ 0 -> back -> left, right -> right
    synchronized public DistanceInfo getDistanceInfo() {
        float currentDegree = mCarPose.getDeviceYawDegree();
        FloatPoint3D currentPose = mCarPose.getDevicePose();
        BaseEngine.Direction firstDirection = BaseEngine.Direction.NONE;
        BaseEngine.Direction secondDirection = BaseEngine.Direction.NONE;
        float firstDistance = Float.MAX_VALUE;
        float secondDistance = Float.MAX_VALUE;
        BaseEngine.TurnDirection firstSuggestTurn = BaseEngine.TurnDirection.NONE;
        BaseEngine.TurnDirection secondSuggestTurn = BaseEngine.TurnDirection.NONE;
        if (currentDegree >= 0 && currentDegree < 90) {
            firstDirection = BaseEngine.Direction.FRONT;
            secondDirection = BaseEngine.Direction.RIGHT;
            firstSuggestTurn = BaseEngine.TurnDirection.TURN_RIGHT;
            secondSuggestTurn = BaseEngine.TurnDirection.TURN_LEFT;
        } else if (currentDegree >= 90 && currentDegree <= 180) {
            firstDirection = BaseEngine.Direction.FRONT;
            secondDirection = BaseEngine.Direction.LEFT;
            firstSuggestTurn = BaseEngine.TurnDirection.TURN_LEFT;
            secondSuggestTurn = BaseEngine.TurnDirection.TURN_RIGHT;
        } else if (currentDegree >= -180 && currentDegree < -90) {
            firstDirection = BaseEngine.Direction.BACK;
            secondDirection = BaseEngine.Direction.LEFT;
            firstSuggestTurn = BaseEngine.TurnDirection.TURN_RIGHT;
            secondSuggestTurn = BaseEngine.TurnDirection.TURN_LEFT;
        } else if (currentDegree >= -90 && currentDegree < 0) {
            firstDirection = BaseEngine.Direction.BACK;
            secondDirection = BaseEngine.Direction.RIGHT;
            firstSuggestTurn = BaseEngine.TurnDirection.TURN_LEFT;
            secondSuggestTurn = BaseEngine.TurnDirection.TURN_RIGHT;
        }
        if (firstDirection == BaseEngine.Direction.FRONT) {
            firstDistance = front - currentPose.y;
        } else if (firstDirection == BaseEngine.Direction.BACK) {
            firstDistance = currentPose.y - back;
        }
        if (secondDirection == BaseEngine.Direction.LEFT) {
            secondDistance = currentPose.x - left;
        } else if (secondDirection == BaseEngine.Direction.RIGHT) {
            secondDistance = right - currentPose.x;
        }

        if (firstDistance <= secondDistance) {
            mDistanceInfo.minDistance = firstDistance;
            mDistanceInfo.nearestDirection = firstDirection;
            mDistanceInfo.suggestTurnDirection = firstSuggestTurn;
        } else {
            mDistanceInfo.minDistance = secondDistance;
            mDistanceInfo.nearestDirection = secondDirection;
            mDistanceInfo.suggestTurnDirection = secondSuggestTurn;
        }

        return mDistanceInfo;
    }

    public static class DistanceInfo {
        public BaseEngine.Direction nearestDirection;
        public float minDistance;
        public BaseEngine.TurnDirection suggestTurnDirection;
    }
}
