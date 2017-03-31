package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.util.Log;

import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.path.AbortSignal;
import com.lenovo.newdevice.tangocar.path.PathFinder;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Queue;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/12/30.
 */

public class NormalEngine extends BaseEngine {
    private static final long WAIT_TIME = 1000;

    enum RunState {
        RUN_INIT,
        RUN_GOT_TARGET,
        RUN_RECOMPUTE,
        RUN_STOP_TOO_CLOSE,
        RUN_COMPUTING_PATH,
        RUN_COMPUTE_OVER,
        RUN_GOING_BY_PATH,
        RUN_ARRIVED_TARGET,
        RUN_CANNOT_MOVE,
        RUN_STOP;
    };
    private RunState mRunState;
    private PathFinder.FailureCause mFailureCause;

    private Point mCurrentTarget;
    private long mWaitEndTime;

    private AbortSignal mAbortSignal;

    public NormalEngine(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);

        mAbortSignal = new AbortSignal();
        mRunState = RunState.RUN_INIT;
    }

    private Point getNextTarget() {
        if (mMap == null) {
            return null;
        }
        return mMap.getOneUnknownGrid(mCarPose.getCurrentGridIndex());
    }

    @Override
    protected Point getCurrentTarget() {
        return mCurrentTarget;
    }

    @Override
    protected String getEngineState() {
        String runState = "RunState is " + mRunState;
        if (mRunState == RunState.RUN_CANNOT_MOVE) {
            runState += "\ncause: \'" + mFailureCause + "\'";
        }
        return runState;
    }

    private void computePathToTarget() {
        Utils.computeTargetPath(mMap,
                mCarPose.getCurrentGridIndex(),
                mCurrentTarget,
                new PathFinder.FinderListener.Stub() {
            @Override
            public void onStart() {
                /*VoiceReporter.from(mActivity.getApplicationContext()).report("开始规划");
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        mProgressDialog.show();
                    }
                });*/
            }

            @Override
            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                Log.d(TAG, "receive:" + pathPoints.size());
                if (mTangoCar != null) {
                    mTangoCar.setExpectedPath(pathPoints);
                }
                mRunState = RunState.RUN_COMPUTE_OVER;
                Utils.outLog("NormalEngine " + "compute success and found a path");

                /*mProgressDialog.dismiss();
                VoiceReporter.from(mActivity.getApplicationContext()).report("路径规划成功");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTargetBtnState = MainActivity.TargetBtnState.COMPUTE_OVER;
                        mTargetBtn.setText(R.string.btn_str_go);
                        mTargetBtn.setEnabled(true);
                    }
                });*/
            }

            @Override
            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                Utils.outLog("NormalEngine " + "compute failed(" + cause + ")");
                boolean isIn = mMap.removeRecentUnknownGrid();
                mFailureCause = cause;
                Utils.outLog("NormalEngine " + "last target is " + ( isIn ? "in" : "out"));
                if (isIn) {
                    mRunState = RunState.RUN_INIT;
                } else {
                    mRunState = RunState.RUN_CANNOT_MOVE;
                }
                /*mProgressDialog.dismiss();
                VoiceReporter.from(getApplicationContext()).report("路径规划失败");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTargetBtnState = MainActivity.TargetBtnState.COMPUTE_OVER;
                        mTargetBtn.setEnabled(true);
                    }
                });*/
            }

            @Override
            public void onCancelled() {

            }
        }, mAbortSignal);
    }

    @Override
    protected void doInit() {
        super.doInit();
        mRunState = RunState.RUN_INIT;
    }

    private RunState mLastLogRunState = RunState.RUN_STOP;
    @Override
    protected void doAction() {
        if (!mLastLogRunState.equals(mRunState)) {
            Utils.outLog("NormalEngine " + "mRunState is " + mRunState);
            mLastLogRunState = mRunState;
        }
        if (mRunState == RunState.RUN_INIT || mRunState == RunState.RUN_ARRIVED_TARGET) {
            mCurrentTarget = getNextTarget();
            mRunState = RunState.RUN_GOT_TARGET;
            Utils.outLog("NormalEngine " + "mCurrentTarget is " + mCurrentTarget);
        } else if (mRunState == RunState.RUN_STOP_TOO_CLOSE) {
            if (System.currentTimeMillis() > mWaitEndTime) {
                mRunState = RunState.RUN_RECOMPUTE;
            }
        } else if (mRunState == RunState.RUN_GOT_TARGET || mRunState == RunState.RUN_RECOMPUTE) {
            Utils.outLog("NormalEngine " + "begin to compute path, mCurrentTarget is " + mCurrentTarget);
            Utils.outLog("NormalEngine " + "current grid is " + mCarPose.getCurrentGridIndex());
            computePathToTarget();
            mRunState = RunState.RUN_COMPUTING_PATH;
        } else if (mRunState == RunState.RUN_COMPUTE_OVER) {
            mRunState = RunState.RUN_GOING_BY_PATH;
            Utils.outLog("NormalEngine " + "begin to run by path");
            mAdvanceControl.runByPath(mCarData.getExpectedPath(), mRunCallback);
            Utils.outLog("NormalEngine " + "after call run by path");
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        mAbortSignal.abort();
        mRunState = RunState.RUN_STOP;
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
            if (shouldTurn()) {
                Utils.outLog("NormalEngine " + "distance is too close");
                mStopType = StopType.STOP_DISTANCE;
                return true;
            }
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
                    mWaitEndTime = System.currentTimeMillis() + WAIT_TIME;
                }
            }
        }
    }
}
