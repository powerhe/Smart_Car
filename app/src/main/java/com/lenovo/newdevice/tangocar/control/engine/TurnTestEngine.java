package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;

import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.GlobalData;

/**
 * Created by liujk2 on 2017/3/2.
 */

public class TurnTestEngine extends BaseEngine {
    private static final String RUNNING = "running";
    private static final String STOP = "stop";
    private String mRunState = RUNNING;

    private float[] mDegreeArray = {
            0, 30, 90, 150, 180, 210, 270, 300, 330,
            //-30, -90, -150,-180, -210, -270
    };
    private int mDegreeIndex = 0;
    private int mTestIndex = 0;
    private int mSleepCount = 0;
    private boolean mNoOver = false;

    public TurnTestEngine(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);
    }

    @Override
    protected String getEngineState() {
        return "RunState is " + mRunState;
    }

    @Override
    protected void doInit() {
        super.doInit();
    }

    @Override
    protected void doAction() {
        if (mNoOver
                || (mCarControl.isTurnOver()
                && mDegreeIndex < mDegreeArray.length)) {
            if (mSleepCount == 0) {
                float degree = mDegreeArray[mDegreeIndex];
                if (degree > 1 || degree < -1) {
                    mNoOver = false;
                    if (mTestIndex == 0) {
                        mCarControl.turnLeft(degree);
                        mTestIndex++;
                    } else if (mTestIndex == 1) {
                        mCarControl.turnRight(degree);
                        mTestIndex++;
                    } else if (mTestIndex == 2) {
                        mCarControl.turnToDegree(degree);
                        mTestIndex++;
                    } else if (mTestIndex == 3) {
                        mCarControl.turnToDegree(0);
                        mTestIndex = 0;
                        mDegreeIndex++;
                    }
                    mSleepCount = 30;
                } else {
                    mNoOver = true;
                    if (mTestIndex == 0) {
                        mCarControl.turnLeft();
                        mTestIndex++;
                    } else if (mTestIndex == 1) {
                        mCarControl.turnRight();
                        mTestIndex = 0;
                        mDegreeIndex++;
                    }
                    mSleepCount = 200;
                }
            }
            mSleepCount --;
        }
        if (mDegreeIndex >= mDegreeArray.length) {
            cancel();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        mRunState = STOP;
    }

}
