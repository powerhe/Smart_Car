package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.Log;

import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.path.AbortSignal;
import com.lenovo.newdevice.tangocar.path.PathFinder;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Queue;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/3/13.
 */

public class TargetEngine extends BaseEngine {
    enum RunState {
        RUN_INIT,
        RUN_COMPUTING_PATH,
        RUN_COMPUTE_OVER,
        RUN_COMPUTE_FAILED,
        RUN_COMPUTE_CANCELLED,
        RUN_COMPUTE_TIME_OUT,
        RUN_GOING_BY_PATH,
        RUN_ARRIVED_TARGET,
        RUN_STOP_TOO_CLOSE,
        RUN_STOP,
    };
    private RunState mRunState;
    private PathFinder.FailureCause mFailureCause;

    private FloatPoint mTargetPoint;
    private AbortSignal mAbortSignal;

    public TargetEngine(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);

        mAbortSignal = new AbortSignal();
    }

    protected void setControlData(DataSerializable controlData) {
        if (controlData != null && controlData instanceof FloatPoint) {
            mTargetPoint = (FloatPoint)controlData;
        }
    }

    public boolean start(DataSerializable controlData) {
        if (controlData == null
                || ! (controlData instanceof FloatPoint)) {
            return false;
        }
        setControlData(controlData);
        start();
        return true;
    }

    @Override
    protected String getEngineState() {
        String runState = "RunState is " + mRunState;
        if (mRunState == RunState.RUN_COMPUTE_FAILED) {
            runState += "\ncause: \'" + mFailureCause + "\'";
        }
        return runState;
    }

    @Override
    protected Point getCurrentTarget() {
        return mTargetPoint.toPoint();
    }

    @Override
    protected void doInit() {
        super.doInit();
        mRunState = RunState.RUN_INIT;
    }

    @Override
    protected void doAction() {
        if (mRunState == RunState.RUN_INIT) {
            mRunState = RunState.RUN_COMPUTING_PATH;
            computePathToTarget();
        } else if (mRunState == RunState.RUN_COMPUTE_OVER) {
            mRunState = RunState.RUN_GOING_BY_PATH;
            Utils.outLog("TargetEngine " + "begin to run by path");
            mAdvanceControl.runByPath(mCarData.getExpectedPath(), mRunCallback);
            Utils.outLog("TargetEngine " + "after call run by path");
        } else if (mRunState == RunState.RUN_COMPUTE_CANCELLED
                || mRunState == RunState.RUN_COMPUTE_FAILED
                || mRunState == RunState.RUN_COMPUTE_TIME_OUT) {
            //
        } else if (mRunState == RunState.RUN_STOP_TOO_CLOSE) {
            //
            mCarControl.stop();
        } else if (mRunState == RunState.RUN_ARRIVED_TARGET) {
            mCarControl.stop();
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        mRunState = RunState.RUN_STOP;
    }

    private void computePathToTarget() {
        Utils.computeTargetPath(mMap,
                mCarPose.getCurrentGridIndex(),
                mTargetPoint.toPoint(),
                new PathFinder.FinderListener.Stub() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onPathFound(@NonNull Queue<Point> pathPoints) {
                        Log.d(TAG, "receive:" + pathPoints.size());
                        mRunState = RunState.RUN_COMPUTE_OVER;
                        if (mTangoCar != null) {
                            mTangoCar.clearCarPath();
                            mTangoCar.setExpectedPath(pathPoints);
                        }
                        Utils.outLog("TargetEngine " + "compute success and found a path");
                    }

                    @Override
                    public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                        Utils.outLog("TargetEngine " + "compute failed(" + cause + ")");
                        mFailureCause = cause;
                        mRunState = RunState.RUN_COMPUTE_FAILED;
                    }

                    @Override
                    public void onCancelled() {
                        mRunState = RunState.RUN_COMPUTE_CANCELLED;
                    }

                    @Override
                    public void onTimeout() {
                        mRunState = RunState.RUN_COMPUTE_TIME_OUT;
                    }

                }, mAbortSignal);
    }

    private RunCallback mRunCallback = new RunCallback();
    enum StopType {STOP_NONE, STOP_STOP, STOP_DISTANCE};
    class RunCallback implements AdvanceControl.RunByPathCallback {
        private StopType mStopType;
        @Override
        public boolean needStop() {
            if (mRunState == RunState.RUN_STOP) {
                mStopType = StopType.STOP_STOP;
                return true;
            }
            /*if (shouldTurn()) {
                Utils.outLog("TargetEngine " + "distance is too close");
                mStopType = StopType.STOP_DISTANCE;
                return true;
            }*/
            mStopType = StopType.STOP_NONE;
            return false;
        }

        @Override
        public void runOver(boolean stop) {
            if (!stop) {
                mRunState = RunState.RUN_ARRIVED_TARGET;
            } else {
                if (mStopType == StopType.STOP_STOP) {
                    mCarControl.stop();
                } else if (mStopType == StopType.STOP_DISTANCE) {
                    mRunState = RunState.RUN_STOP_TOO_CLOSE;
                }
            }
        }
    }
}
