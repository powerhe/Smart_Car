package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;
import android.graphics.Point;

import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.utils.VoiceReporter;

/**
 * Created by liujk2 on 2017/1/5.
 */

public class SimpleEngine extends BaseEngine {
    private static final long FREEZE_TIME_OUT = 1000;
    private static final long MOVE_MIN_TIME = 300;
    private static final long TURN_MIN_TIME = 300;
    private static final long RESUME_TIME_OUT = 1500;
    private static final long RESUME_OK_TIME_OUT = 3000;
    private static final long ONE_FREEZE_TIME_OUT = 3500;
    private static final long SOUND_TIME_OUT = 3000;

    enum RunState {
        RUN_INIT,
        RUN_FORWARD,
        RUN_TURN_LEFT,
        RUN_TURN_RIGHT,
        RUN_BACK,
        RUN_STOP,
        RUN_FREEZE_MOVE,
        RUN_FREEZE_TURN,
        RUN_RESUME_FROM_FREEZE_MOVE,
        RUN_RESUME_FROM_FREEZE_TURN,
    };
    private RunState mRunState;
    private RunState mResumeState;
    private RunState mLastCommandState;

    private Point mLastPose;
    private long mLastPoseTime;
    private long mTimeWaitTo;
    private long mTimeResumeFirstTo;
    private long mTimeResumeSecondTo;

    private long mTimeToNextFreeze;
    private boolean mJustResumeFromFreeze;

    private int mFreezeCountWhenResume;
    private boolean mFreezeWhenResume;

    private int mTimesOfFreeze;

    private float mLastDegree;
    private long mLastDegreeTime;

    private long mCoundSoundTime;

    private String[] mFreezeSoundStrArray;
    private int mLengthOfFSSA;

    public SimpleEngine(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);
        mCoundSoundTime = 0;
        mFreezeSoundStrArray = activity.getResources().getStringArray(R.array.sound_str_freeze);
        mLengthOfFSSA = mFreezeSoundStrArray.length;
    }

    @Override
    protected String getEngineState() {
        return "RunState is " + mRunState;
    }

    private boolean isFreezeMove() {
        Point currentPose = mCarPose.getCurrentGridIndex();
        long poseTime = System.currentTimeMillis();
        if (poseTime - mLastPoseTime >= MOVE_MIN_TIME) {
            if (currentPose.equals(mLastPose)) {
                return true;
            }
            mLastPose = currentPose;
            mLastPoseTime = poseTime;
        }
        return false;
    }

    private boolean isFreezeTurn() {
        float currentDegree = mCarPose.getDeviceYawDegree();
        long degreeTime = System.currentTimeMillis();
        if (degreeTime - mLastDegreeTime >= TURN_MIN_TIME) {
            float deltaDegree =  Math.abs(currentDegree - mLastDegree);
            if (deltaDegree <= 1) {
                return true;
            }
            mLastDegree = currentDegree;
            mLastDegreeTime = degreeTime;
        }
        return false;
    }

    private void soundFreeze() {
        long currentTimeMillis = System.currentTimeMillis();
        int strIdx = (int)(currentTimeMillis % mLengthOfFSSA);
        if (currentTimeMillis > mCoundSoundTime) {
            VoiceReporter.from(mActivity.getApplicationContext()).report(mFreezeSoundStrArray[strIdx]);
            mCoundSoundTime = currentTimeMillis + SOUND_TIME_OUT;
        }
    }

    protected void soundByResId(int resId) {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > mCoundSoundTime) {
            super.soundByResId(resId);
            mCoundSoundTime = currentTimeMillis + SOUND_TIME_OUT;
        }
    }

    private void resetResumeTime(long currentTimeMillis) {
        mTimeResumeFirstTo = currentTimeMillis + RESUME_TIME_OUT;
        mTimeResumeSecondTo = currentTimeMillis + RESUME_OK_TIME_OUT;
        mTimeToNextFreeze = currentTimeMillis + ONE_FREEZE_TIME_OUT;
    }

    private RunState computeCommandState(boolean freezeInResume, int freezeCount) {
        RunState commandState = RunState.RUN_INIT;
        if (freezeInResume) {
            if (mLastCommandState == RunState.RUN_FORWARD) {
                commandState = RunState.RUN_BACK;
            } else if (mLastCommandState == RunState.RUN_BACK) {
                commandState = RunState.RUN_TURN_LEFT;
            } else if (mLastCommandState == RunState.RUN_TURN_LEFT) {
                commandState = RunState.RUN_TURN_RIGHT;
            } else if (mLastCommandState == RunState.RUN_TURN_RIGHT) {
                commandState = RunState.RUN_FORWARD;
            }
        } else {
            commandState = RunState.RUN_BACK;
        }

        if (commandState == RunState.RUN_FORWARD
                || commandState == RunState.RUN_BACK) {
            if ((freezeCount & 0x1) == 1) {
                mResumeState = RunState.RUN_TURN_LEFT;
            } else {
                mResumeState = RunState.RUN_TURN_RIGHT;
            }
        }
        if (commandState == RunState.RUN_TURN_LEFT
                || commandState == RunState.RUN_TURN_RIGHT) {
            if ((freezeCount & 0x1) == 1) {
                mResumeState = RunState.RUN_FORWARD;
            } else {
                mResumeState = RunState.RUN_BACK;
            }
        }
        return commandState;
    }

    @Override
    protected void doInit() {
        super.doInit();
        //mCarControl.forward();
        mCarControl.forwardPwd();
        //mCarControl.sendCommand(CarControl.CMD_FORWARD);
        mLastPose = mCarPose.getCurrentGridIndex();
        mLastPoseTime = System.currentTimeMillis();
        mLastDegree = mCarPose.getDeviceYawDegree();
        mLastDegreeTime = System.currentTimeMillis();
        mRunState = RunState.RUN_INIT;
        mLastCommandState = RunState.RUN_INIT;
        mTimesOfFreeze = 0;
    }

    @Override
    protected void doAction() {
        RunState newState = RunState.RUN_INIT;
        RunState commandState = RunState.RUN_INIT;

        boolean freezeMove = false;
        boolean freezeTurn = false;
        long currentTimeMillis = System.currentTimeMillis();
        if (mLastCommandState == RunState.RUN_FORWARD || mLastCommandState == RunState.RUN_BACK
                || mRunState == RunState.RUN_FREEZE_MOVE) {
            freezeMove = isFreezeMove();
            if (freezeMove) {
                if (mRunState == RunState.RUN_FORWARD || mRunState == RunState.RUN_BACK) {
                    mTimeWaitTo = currentTimeMillis + FREEZE_TIME_OUT;
                    newState = RunState.RUN_FREEZE_MOVE;
                }
                if (mRunState == RunState.RUN_FREEZE_MOVE) {
                    if (currentTimeMillis > mTimeWaitTo) {
                        newState = RunState.RUN_RESUME_FROM_FREEZE_MOVE;
                    }
                }
            }
        }
        if (mLastCommandState == RunState.RUN_TURN_LEFT || mLastCommandState == RunState.RUN_TURN_RIGHT
                || mRunState == RunState.RUN_FREEZE_TURN) {
            freezeTurn = isFreezeTurn();
            if (freezeTurn) {
                if (mRunState == RunState.RUN_TURN_LEFT || mRunState == RunState.RUN_TURN_RIGHT) {
                    mTimeWaitTo = currentTimeMillis + FREEZE_TIME_OUT;
                    newState = RunState.RUN_FREEZE_TURN;
                }
                if (mRunState == RunState.RUN_FREEZE_TURN) {
                    if (currentTimeMillis > mTimeWaitTo) {
                        newState = RunState.RUN_RESUME_FROM_FREEZE_TURN;
                    }
                }
            }
        }

        if (newState == RunState.RUN_RESUME_FROM_FREEZE_MOVE
                || newState == RunState.RUN_RESUME_FROM_FREEZE_TURN) {
            resetResumeTime(currentTimeMillis);
            mFreezeCountWhenResume = 0;
            mTimesOfFreeze ++;
        }

        boolean shouldTurn = false;
        if (!freezeMove && !freezeTurn
                && mRunState != RunState.RUN_RESUME_FROM_FREEZE_MOVE
                && mRunState != RunState.RUN_RESUME_FROM_FREEZE_TURN) {
            shouldTurn = shouldTurn();
            if (!shouldTurn) {
                newState = RunState.RUN_FORWARD;
            }
        }

        if (shouldTurn) {
            if (mRunState != RunState.RUN_TURN_LEFT && mRunState != RunState.RUN_TURN_RIGHT && mRunState != RunState.RUN_BACK) {
                newState = (mTurnDirection == TurnDirection.TURN_LEFT) ? RunState.RUN_TURN_LEFT : RunState.RUN_TURN_RIGHT;
            }
        }

        if (mJustResumeFromFreeze) {
            if (freezeMove || freezeTurn) {
                mJustResumeFromFreeze = false;
            } else if (currentTimeMillis  > mTimeToNextFreeze) {
                mJustResumeFromFreeze = false;
                mTimesOfFreeze = 0;
            }
        }

        if (mTimesOfFreeze > 10 || mFreezeCountWhenResume > 15) {
            soundByResId(R.string.sound_str_help_me);
        }

        if (mRunState == RunState.RUN_RESUME_FROM_FREEZE_MOVE
                || mRunState == RunState.RUN_RESUME_FROM_FREEZE_TURN) {
            if (freezeMove || freezeTurn) {
                if (mFreezeWhenResume) {
                    if (currentTimeMillis > mTimeWaitTo) {
                        commandState = computeCommandState(true, mFreezeCountWhenResume++);
                        resetResumeTime(currentTimeMillis);
                        mFreezeWhenResume = false;
                    }
                } else {
                    mFreezeWhenResume = true;
                    mTimeWaitTo = currentTimeMillis + FREEZE_TIME_OUT;
                }
            } else {
                if (mFreezeWhenResume) {
                    mFreezeWhenResume = false;
                }
            }
            if (currentTimeMillis  > mTimeResumeSecondTo) {
                newState = mResumeState;
                mJustResumeFromFreeze = true;
                mResumeState = RunState.RUN_INIT;
            } else if (currentTimeMillis  > mTimeResumeFirstTo) {
                if (mLastCommandState != mResumeState) {
                    commandState = mResumeState;
                }
            }
        }

        if (newState != RunState.RUN_INIT && newState != mRunState) {
            commandState = newState;
            if (newState == RunState.RUN_RESUME_FROM_FREEZE_MOVE
                    || newState == RunState.RUN_RESUME_FROM_FREEZE_TURN) {
                soundFreeze();
                commandState = computeCommandState(false, mTimesOfFreeze);
            } else if (newState == RunState.RUN_TURN_LEFT
                    || newState == RunState.RUN_TURN_RIGHT) {
                //soundByResId(R.string.sound_str_will_turn);
            }
            mRunState = newState;
        }

        if (commandState == RunState.RUN_INIT) {
            return;
        }
        if (commandState != mLastCommandState) {
            if (commandState == RunState.RUN_FORWARD
                    || commandState == RunState.RUN_BACK
                    || commandState == RunState.RUN_TURN_LEFT
                    || commandState == RunState.RUN_TURN_RIGHT) {
                mLastCommandState = commandState;
            }
            if (commandState == RunState.RUN_FORWARD) {
                //mCarControl.forward();
                mCarControl.forwardPwd();
                //mCarControl.sendCommand(CarControl.CMD_FORWARD);
            } else if (commandState == RunState.RUN_TURN_LEFT) {
                //mCarControl.simpleTurnLeft();
                mCarControl.turnLeft();
                //mCarControl.sendCommand(CarControl.CMD_TURN_LEFT);
            } else if (commandState == RunState.RUN_TURN_RIGHT) {
                //mCarControl.simpleTurnRight();
                mCarControl.turnRight();
                //mCarControl.sendCommand(CarControl.CMD_TURN_RIGHT);
            } else if (commandState == RunState.RUN_BACK) {
                mCarControl.back();
                //mCarControl.sendCommand(CarControl.CMD_BACK);
            } else if (commandState == RunState.RUN_STOP) {
                mCarControl.stop();
                //mCarControl.sendCommand(CarControl.CMD_STOP);
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        mRunState = RunState.RUN_STOP;
    }

}
