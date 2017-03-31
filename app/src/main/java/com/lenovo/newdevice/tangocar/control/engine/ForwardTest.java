package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;

import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.map.FloatPoint;

/**
 * Created by liujk2 on 2017/3/28.
 */

public class ForwardTest extends BaseEngine {
    enum State {INIT, FORWARD, TURN_BACK, STOP};
    private State mRunState = State.INIT;
    private int mSleepCount = 0;
    private int mTestIndex = 0;
    private CarPose mLastPose;
    private double mDistance;
    private double mForwardDistance = 5.0;

    public ForwardTest(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);
        mLastPose = new CarPose();
    }

    @Override
    protected String getEngineState() {
        return "RunState is " + mRunState + ", mDistance is " + mDistance;
    }

    @Override
    protected void doInit() {
        super.doInit();
    }

    @Override
    protected void doAction() {
        if (mRunState == State.INIT) {
            mLastPose.update(mCarPose);
            mCarControl.forwardPwd();
            mRunState = State.FORWARD;
        } else if (mRunState == State.FORWARD) {
            mDistance = mCarPose.distanceFrom(mLastPose);
            if (mDistance >= mForwardDistance) {
                mCarControl.turnLeft(180);
                mRunState = State.TURN_BACK;
            }
        } else if (mRunState == State.TURN_BACK) {
            if (mCarControl.isTurnOver()) {
                mRunState = State.INIT;
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        mRunState = State.STOP;
    }
}
