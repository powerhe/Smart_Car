package com.lenovo.newdevice.tangocar.control;

import android.app.Activity;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarData;
import com.lenovo.newdevice.tangocar.data.CarSpeed;
import com.lenovo.newdevice.tangocar.utils.AppConfig;
import com.lenovo.newdevice.tangocar.main.CarConnection;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.utils.BytesUtils;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.HandlerThread;
import com.lenovo.newdevice.tangocar.utils.MathUtils;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;
import com.lenovo.newdevice.tangocar.utils.SimpleQueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/12/24.
 */

public class CarControl {
    public static final int CMD_KEEP = 0;
    public static final int CMD_TURN_LEFT = 1;
    public static final int CMD_TURN_RIGHT = 2;
    public static final int CMD_FORWARD = 3;
    public static final int CMD_BACK = 4;
    public static final int CMD_STOP = 5;
    public static final int CMD_COUNT = 6;
    public static final int CMD_TURN = 6;
    public static final int CMD_FORWARD_PWD = 7;
    public static final int CMD_FORWARD_TARGET = 8;

    private static final int MSG_FORWARD = 100;
    private static final int MSG_STOP = 101;
    private static final int MSG_TURN = 102;
    private static final int MSG_TURN_LEFT = 103;
    private static final int MSG_TURN_RIGHT = 104;
    private static final int MSG_BACK = 105;
    private static final int MSG_FORWARD_PWD = 106;
    private static final int MSG_FORWARD_TARGET = 107;
    private static final int MSG_LOOP_SEND_CMD = 120;

    private static final int RUN_STATE_UNKNOWN = 0;
    private static final int RUN_STATE_FORWARD = 1;
    private static final int RUN_STATE_TURN = 2;
    private static final int RUN_STATE_TURN_LEFT = 3;
    private static final int RUN_STATE_TURN_RIGHT = 4;
    private static final int RUN_STATE_STOP = 5;
    private static final int RUN_STATE_FORWARD_TARGET = 6;
    private static final int RUN_STATE_FORWARD_PWD = 7;
    private static final int RUN_STATE_BACK = 8;

    private static final byte MSG_CMD_TURN_LEFT_50MS = 14;
    private static final byte MSG_CMD_TURN_RIGHT_50MS = 16;
    private static final byte MSG_CMD_TURN_LEFT = 4;
    private static final byte MSG_CMD_TURN_RIGHT = 6;
    private static final byte MSG_CMD_FORWARD = 2;
    private static final byte MSG_CMD_FORWARD_PWD = 12;
    private static final byte MSG_CMD_BACK = 8;
    private static final byte MSG_CMD_STOP = 5;

    public final static int SPEED_RATE_MAX = 100;
    public final static int SPEED_RATE_MIN = 1;

    private static final int MSG_SPEED_MIN = 30;
    private static final int MSG_SPEED_MAX = 250;

    private static final int MSG_MOVE_SPEED_MIN = 50;
    private static final int MSG_MOVE_SPEED_MAX = 250;

    private static final int MSG_TURN_SPEED_ZERO = 30;
    private static final int MSG_TURN_SPEED_MIN = 60;
    private static final int MSG_TURN_SPEED_MAX = 150;

    private static final float MOVE_SPEED_DEF = 0.20f;
    private static final float MOVE_SPEED_MIN = 0.05f;
    private static final float MOVE_SPEED_MAX = 0.55f;
    private static final float TURN_SPEED_DEF = 45;
    private static final float TURN_SPEED_MIN = 30;
    private static final float TURN_SPEED_MAX = 160;
    private static final float TURN_SPEED_ZERO = 10;

    private static final float RATE_OF_MOVE_SPEED_TO_MSG = (MSG_MOVE_SPEED_MAX - MSG_MOVE_SPEED_MIN) / (MOVE_SPEED_MAX - MOVE_SPEED_MIN) * 0.6f;
    private static final float RATE_OF_TURN_SPEED_TO_MSG = (MSG_TURN_SPEED_MAX - MSG_TURN_SPEED_MIN) / (TURN_SPEED_MAX - TURN_SPEED_MIN) * 0.6f;

    private static float TARGET_MOVE_SPEED;
    private static float TARGET_TURN_SPEED;

    private static byte MOVE_SPEED_INIT_MSG;
    private static byte TURN_SPEED_INIT_MSG;

    private byte mCurrentMoveSpeedMessage = 0;
    private byte mCurrentTurnSpeedMessage = 0;

    private static final byte[] sMsgStop = {MSG_CMD_STOP, 0};

    private Activity mActivity = null;
    private CarConnection mCarConnection = null;
    private Status mStatus;
    private CarPose mCarPose;
    private CarSpeed mCarSpeed;

    private boolean mIsStop;
    private boolean mPaused;
    private int mRunState;
    private int mLastRunState;
    private boolean mNoTargetDegree;
    private int mLastTurnCmd;
    private boolean mTurnOver;
    private float mLastTargetDegree;
    private float mMoveSpeed;
    private float mTurnSpeed;
    private Point mLastDestPos;

    private Object mTurnOverLock;

    private HandlerThread mControlThread;
    private ControlHandler mControlHandler;

    private static float valueToMsg(float msgVal) {
        int msg = Math.round(msgVal);
        if (msgVal > MSG_SPEED_MAX) {
            msg = MSG_SPEED_MAX;
        } else if (msgVal < MSG_SPEED_MIN) {
            msg = MSG_SPEED_MIN;
        }
        return msg;
    }

    private static byte valueToMoveMsg(float msgVal) {
        int msg = Math.round(msgVal);
        if (msgVal > MSG_MOVE_SPEED_MAX) {
            msg = MSG_MOVE_SPEED_MAX;
        } else if (msgVal < MSG_MOVE_SPEED_MIN) {
            msg = MSG_MOVE_SPEED_MIN;
        }
        return (byte) msg;
    }

    private static byte valueToTurnMsg(float msgVal, boolean zero) {
        int msg = Math.round(msgVal);
        if (msgVal > MSG_TURN_SPEED_MAX) {
            msg = MSG_TURN_SPEED_MAX;
        } else if (msgVal < MSG_TURN_SPEED_MIN) {
            if (zero && msgVal < MSG_TURN_SPEED_ZERO) {
                msg = MSG_TURN_SPEED_ZERO;
            } else {
                msg = MSG_TURN_SPEED_MIN;
            }
        }
        return (byte) msg;
    }

    private static float speedRateToMoveSpeed(float rate) {
        return MOVE_SPEED_MIN + ((rate - SPEED_RATE_MIN) * (MOVE_SPEED_MAX - MOVE_SPEED_MIN) / (SPEED_RATE_MAX - SPEED_RATE_MIN));
    }

    private static float speedRateToTurnSpeed(float rate) {
        return TURN_SPEED_MIN + ((rate - SPEED_RATE_MIN) * (TURN_SPEED_MAX - TURN_SPEED_MIN) / (SPEED_RATE_MAX - SPEED_RATE_MIN));
    }

    private static byte updateMoveMsg(byte initSpeed, float delta) {
        return valueToMoveMsg((((int)initSpeed) & 0xff) + delta);
    }

    private static byte updateTurnMsg(byte initSpeed, float delta, boolean zero) {
        return valueToTurnMsg((((int)initSpeed) & 0xff) + delta, zero);
    }

    private static float updateMsg(float initSpeed, float delta) {
        return valueToMsg((((int)initSpeed) & 0xff) + delta);
    }

    private static byte moveSpeedToMsg(float speed) {
        float msgVal = MSG_MOVE_SPEED_MIN + ((speed - MOVE_SPEED_MIN) * RATE_OF_MOVE_SPEED_TO_MSG);
        return valueToMoveMsg(msgVal);
    }

    private static byte turnSpeedToMsg(float speed) {
        float msgVal = MSG_TURN_SPEED_MIN + ((speed - TURN_SPEED_MIN) * RATE_OF_TURN_SPEED_TO_MSG);
        return valueToTurnMsg(msgVal, false);
    }

    public static void setSpeedRate(int rate) {
        if (rate < SPEED_RATE_MIN || rate > SPEED_RATE_MAX) {
            return;
        }

        TARGET_MOVE_SPEED = speedRateToMoveSpeed(rate);
        TARGET_TURN_SPEED = speedRateToTurnSpeed(rate);
        MOVE_SPEED_INIT_MSG = moveSpeedToMsg(TARGET_MOVE_SPEED);
        TURN_SPEED_INIT_MSG = turnSpeedToMsg(TARGET_TURN_SPEED);
    }

    public void terminate() {
        mControlThread.quit();
        stop();
    }

    public void setCommandText(final String str) {
        mStatus.command = str;
        Log.i(TAG, "CarControl " + str);
    }

    private void loadConfig() {
        mMoveSpeed = AppConfig.getConfig().getFloat(AppConfig.CFG_FORWARD_SPEED_MSG, MOVE_SPEED_DEF);
        mTurnSpeed = AppConfig.getConfig().getFloat(AppConfig.CFG_TURN_SPEED_MSG, TURN_SPEED_DEF);
    }

    private void saveConfig() {
        AppConfig.getConfig().putFloat(AppConfig.CFG_FORWARD_SPEED_MSG, mMoveSpeed);
        AppConfig.getConfig().putFloat(AppConfig.CFG_TURN_SPEED_MSG, mTurnSpeed);
    }

    public CarControl(Activity activity, CarConnection carConnection, CarData carData) {
        reset();

        mActivity = activity;
        mCarConnection = carConnection;

        mStatus = new Status();
        carData.getStatus().setCarControlStatus(mStatus);
        mCarPose = carData.getPose();
        mCarSpeed = carData.getSpeed();

        mTurnOverLock = new Object();

        loadConfig();

        mControlThread = new HandlerThread("CarControl"){
            protected void createHandler() {
                mControlHandler = new ControlHandler();
            }
        };
        mControlThread.start();
    }

    synchronized public void reset() {
        mRunState = RUN_STATE_UNKNOWN;
        mLastRunState = RUN_STATE_UNKNOWN;
        mTurnOver = true;
        mPaused = false;
        mNoTargetDegree = false;
    }

    /* here is simple control */

    private static byte[] cmd2Msg(int cmd) {
        byte[] msg = new byte[2];
        switch (cmd) {
            case CarControl.CMD_TURN_LEFT:
                msg[0] = MSG_CMD_TURN_LEFT;
                msg[1] = TURN_SPEED_INIT_MSG;
                break;
            case CarControl.CMD_TURN_RIGHT:
                msg[0] = MSG_CMD_TURN_RIGHT;
                msg[1] = TURN_SPEED_INIT_MSG;
                break;
            case CarControl.CMD_FORWARD:
                msg[0] = MSG_CMD_FORWARD;
                msg[1] = MOVE_SPEED_INIT_MSG;
                break;
            case CarControl.CMD_BACK:
                msg[0] = MSG_CMD_BACK;
                msg[1] = MOVE_SPEED_INIT_MSG;
                break;
            case CarControl.CMD_STOP:
                msg[0] = MSG_CMD_STOP;
                msg[1] = 0;
                break;
            default:
                msg[0] = MSG_CMD_STOP;
                msg[1] = 0;
                break;
        }
        return msg;
    }

    private int mLastCmd = CarControl.CMD_KEEP;
    private int mLastRunCmd = CarControl.CMD_FORWARD;

    public void sendCommand(int cmd) {
        if (cmd < CarControl.CMD_KEEP || cmd >= CarControl.CMD_COUNT) {
            return;
        }

        int realCmd = cmd;
        if (cmd == CarControl.CMD_KEEP) {
            realCmd = mLastRunCmd;
        } else if (cmd == CarControl.CMD_TURN_LEFT || cmd == CarControl.CMD_TURN_RIGHT) {
            if (mLastCmd == CarControl.CMD_TURN_LEFT || mLastCmd == CarControl.CMD_TURN_RIGHT) {
                realCmd = mLastCmd;
            }
        }
        /*Log.i(TAG, "cmd is '" + getStringFromCmd(cmd) + "'"
                + ", mLastCmd is '" + getStringFromCmd(mLastCmd) + "'"
                + ", mLastRunCmd is '" + getStringFromCmd(mLastRunCmd) + "'"
                + ", realCmd is '" + getStringFromCmd(realCmd) + "'");*/

        if (true) {
            if (realCmd != mLastCmd) {
                sendMessage(realCmd);
                setCommandText(getStringFromCmd(realCmd));
            }
        } else {
            if (realCmd == CarControl.CMD_FORWARD) {
                forward();
            } else if (realCmd == CarControl.CMD_TURN_LEFT) {
                turnLeft();
            } else if (realCmd == CarControl.CMD_TURN_RIGHT) {
                turnRight();
            } else if (realCmd == CarControl.CMD_STOP) {
                stop();
            }
        }
        if (realCmd >= CarControl.CMD_FORWARD && realCmd < CarControl.CMD_COUNT){
            mLastRunCmd = realCmd;
        }
        mLastCmd = realCmd;
    }

    public void sendMessage(int cmd) {
        if (cmd <= CarControl.CMD_KEEP || cmd >= CarControl.CMD_COUNT) {
            return;
        }
        byte[] msg = cmd2Msg(cmd);
        mCarConnection.sendMessage(msg);
    }

    public static int cmdToStringSourceID(int cmd) {
        switch (cmd) {
            case CarControl.CMD_KEEP:
                return R.string.cmd_keep;
            case CarControl.CMD_TURN_LEFT:
                return R.string.cmd_turn_left;
            case CarControl.CMD_TURN_RIGHT:
                return R.string.cmd_turn_right;
            case CarControl.CMD_FORWARD:
                return R.string.cmd_forward;
            case CarControl.CMD_BACK:
                return R.string.cmd_back;
            case CarControl.CMD_STOP:
                return R.string.cmd_stop;
            case CarControl.CMD_TURN:
                return R.string.cmd_turn;
            default:
                return R.string.cmd_keep;
        }
    }

    public void sendCommandMsg(byte[] msg) {
        if (mIsStop || mPaused) {
            return;
        }
        mCarConnection.sendMessage(msg);
    }

    /* advance control */

    public boolean isTurnOver() {
        synchronized (mTurnOverLock) {
            return mTurnOver;
        }
    }

    public void waitForTurnOver() {
        synchronized (mTurnOverLock) {
            try {
                if (!mTurnOver) {
                    Log.i(TAG, "wait for turn over!");
                    mTurnOverLock.wait();
                    Log.i(TAG, "after wait turn over!");
                }
            } catch (Exception e) {
                Log.e(TAG, "wait error!", e);
            }
        }
    }

    private void turnOver(boolean updateStatus) {
        Log.i(TAG, "call turnOver("+updateStatus+")");
        if (updateStatus) {
            mStatus.debugInfo = "Turn Over";
        }
        synchronized (mTurnOverLock) {
            mTurnOver = true;
            mTurnOverLock.notifyAll();
        }
    }

    private String getStringFromCmd(int cmd) {
        return mActivity.getString(cmdToStringSourceID(cmd));
    }

    private void doStop() {
        mStatus.debugInfo = "CarControl sendMsg " + BytesUtils.bytesToString(sMsgStop);
        sendCommandMsg(sMsgStop);
    }

    private void doTurn(int cmd) {
        doTurn(cmd, 0, true);
    }

    private void doTurn(int cmd, float degree) {
        doTurn(cmd, degree, false);
    }

    private void doTurn(int cmd, float degree, boolean noDegree) {
        synchronized (mTurnOverLock) {
            mTurnOver = false;
        }
        synchronized (this) {
            mLastRunState = mRunState;
            mRunState = RUN_STATE_TURN;
            mLastTargetDegree = degree;
            mNoTargetDegree = noDegree;
            if (noDegree) {
                mLastTurnCmd = cmd;
            } else {
                mLastTurnCmd = CMD_TURN;
            }

            mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_TURN, new TurnArgs(cmd, degree, noDegree)));
        }
    }

    public void turnToDegree(float degree) {
        doTurn(CarControl.CMD_TURN, degree);
    }

    public void turnLeft(float degree) {
        doTurn(CarControl.CMD_TURN_LEFT, degree);
    }

    public void turnRight(float degree) {
        doTurn(CarControl.CMD_TURN_RIGHT, degree);
    }

    public void turnLeft() {
        doTurn(CarControl.CMD_TURN_LEFT);
    }

    public void turnRight() {
        doTurn(CarControl.CMD_TURN_RIGHT);
    }

    public void simpleTurnLeft() {
        mRunState = RUN_STATE_TURN_LEFT;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_TURN_LEFT));
    }

    public void simpleTurnRight() {
        mRunState = RUN_STATE_TURN_RIGHT;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_TURN_RIGHT));
    }

    synchronized public void pause() {
        Log.i(TAG, "CarControl pause() ");
        if (mPaused) {
            return ;
        }
        mPaused = true;
        doStop();
    }

    synchronized public void resume() {
        Log.i(TAG, "CarControl resume() ");
        if (!mPaused) {
            return ;
        }
        mPaused = false;
        if (mRunState == RUN_STATE_TURN) {
            doTurn(mLastTurnCmd, mLastTargetDegree, mNoTargetDegree);
        }
        if (mRunState == RUN_STATE_FORWARD) {
            forward();
        }
        if (mRunState == RUN_STATE_FORWARD_PWD) {
            forwardPwd();
        }
        if (mRunState == RUN_STATE_FORWARD_TARGET) {
            forwardTarget(mLastDestPos);
        }
    }

    synchronized public void stop() {
        Log.i(TAG, "CarControl stop() ");
        mRunState = RUN_STATE_STOP;
        setCommandText(getStringFromCmd(CarControl.CMD_STOP));
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_STOP));
        turnOver(false);
    }

    synchronized public void back() {
        Log.i(TAG, "CarControl back() ");
        mRunState = RUN_STATE_BACK;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_BACK));
    }

    synchronized public void forward() {
        Log.i(TAG, "CarControl forward() ");
        mRunState = RUN_STATE_FORWARD;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_FORWARD));
    }

    synchronized public void forwardPwd() {
        Log.i(TAG, "CarControl forwardPwd() ");
        mRunState = RUN_STATE_FORWARD_PWD;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_FORWARD_PWD));
    }

    synchronized public void forwardTarget(final Point destPos) {
        Log.i(TAG, "CarControl forwardTarget(" + destPos + ") ");
        mRunState = RUN_STATE_FORWARD_TARGET;
        mLastDestPos = destPos;
        mControlHandler.sendMessage(mControlHandler.obtainMessage(MSG_FORWARD_TARGET, destPos));
    }

    class RunControl {
        protected boolean mIsTurn;
        protected int mRunCmd;
        protected byte[] mCmdMsg2;
        protected byte[] mCmdMsg3;
        protected String mDebugInfo;

        public RunControl(int runCmd) {
            mRunCmd = runCmd;
            mCmdMsg2 = new byte[2];
            mCmdMsg3 = new byte[3];
            mDebugInfo = "";
        }

        protected void updateCommandText(int runCmd) {
            setCommandText(getStringFromCmd(runCmd));
        }

        protected String debugInfo() {
            return mDebugInfo;
        }

        protected boolean isTurn() {
            return mIsTurn;
        }

        protected boolean isTurnOver() {
            return false;
        }

        protected byte[] getCmdMsg() {
            return mCmdMsg2;
        }

        protected void updateCmdMsg() {
        }

        protected void updateSpeed() {
        }
    }

    class BackControl extends RunControl {
        public BackControl() {
            super(CMD_BACK);
            mCmdMsg2[0] = MSG_CMD_BACK;
            mCmdMsg2[1] = MOVE_SPEED_INIT_MSG;
            updateCommandText(CMD_BACK);
        }

        @Override
        protected void updateSpeed() {
            mCmdMsg2[1] = MOVE_SPEED_INIT_MSG;
        }
    }

    class ForwardControl extends RunControl {
        public ForwardControl() {
            super(CMD_FORWARD);
            mCmdMsg2[0] = MSG_CMD_FORWARD;
            mCmdMsg2[1] = MOVE_SPEED_INIT_MSG;
            updateCommandText(CMD_FORWARD);
        }

        @Override
        protected void updateSpeed() {
            mCmdMsg2[1] = MOVE_SPEED_INIT_MSG;
        }
    }

    class SimpleTurnControl extends RunControl {
        public SimpleTurnControl(int cmd) {
            super(cmd);
            mCmdMsg2[0] = cmdToMsg(cmd);
            mCmdMsg2[1] = TURN_SPEED_INIT_MSG;
            updateCommandText(cmd);
        }

        @Override
        protected void updateSpeed() {
            mCmdMsg2[1] = TURN_SPEED_INIT_MSG;
        }
    }

    private static byte cmdToMsg(int cmd) {
        switch (cmd) {
            case CMD_FORWARD_PWD:
            case CMD_FORWARD_TARGET:
                return MSG_CMD_FORWARD_PWD;
            case CMD_TURN_LEFT:
                return MSG_CMD_TURN_LEFT;
            case CMD_TURN_RIGHT:
                return MSG_CMD_TURN_RIGHT;
            default:
                return MSG_CMD_STOP;
        }
    }

    class MoveControl extends RunControl {
        float targetDirection;
        float targetMoveSpeed;
        AdvancePID mMoveSpeedPID;
        PID mDirectionPID;

        public MoveControl(int runCmd) {
            super(runCmd);
            targetMoveSpeed = TARGET_MOVE_SPEED;
            targetDirection = mCarPose.getDeviceYawDegree();
            mMoveSpeedPID = new AdvancePID(targetMoveSpeed, RATE_OF_MOVE_SPEED_TO_MSG / 2, 0f, 1) {
                @Override
                protected float defaultOut() {
                    return moveSpeedToMsg(expect);
                }
                @Override
                protected float plusOut(float base, float delta) {
                    return updateMsg(base, delta);
                }
            };
            mMoveSpeedPID.setLag(3);
            mMoveSpeedPID.setScope(MOVE_SPEED_MAX, MOVE_SPEED_MIN);
            mDirectionPID = new PID(2, 0, 1);
            if (mCurrentMoveSpeedMessage == 0) {
                mCurrentMoveSpeedMessage = MOVE_SPEED_INIT_MSG;
            }
            mCmdMsg3[0] = cmdToMsg(runCmd);
            mCmdMsg3[1] = mCurrentMoveSpeedMessage;
            mCmdMsg3[2] = mCurrentMoveSpeedMessage;
        }

        @Override
        protected byte[] getCmdMsg() {
            return mCmdMsg3;
        }

        protected void updateCmdSpeedMsg() {
            mMoveSpeedPID.setExpect(targetMoveSpeed);
            float currSpeed = (float)mCarSpeed.getInstantMoveSpeed();

            byte newMsg = (byte) mMoveSpeedPID.compute(currSpeed);

            mDebugInfo += "\nMove Speed:" + mMoveSpeedPID.getDebugInfo();

            mCurrentMoveSpeedMessage = mCmdMsg3[1] = newMsg;
        }

        protected void updateCmdDirectionMsg() {
            float currDirection = mCarPose.getDeviceYawDegree(true);
            float orientationDelta = targetDirection - currDirection;
            orientationDelta = MathUtils.formatDegree180(orientationDelta);
            float pidDelta = mDirectionPID.update(orientationDelta);
            byte newMsg = updateMoveMsg(mCmdMsg3[1], pidDelta);
            mDebugInfo += "\ndirection: " + "target(" + targetDirection + ")"
                    + "current(" + currDirection + ")"
                    + ", left msg(" + mCmdMsg3[1] + ")"
                    + ", delta(" + orientationDelta + ")"
                    + ", pid delta(" + pidDelta + ")"
                    + ", right msg(" + newMsg + ")";
            mCmdMsg3[2] = newMsg;
        }

        @Override
        protected void updateCmdMsg() {
            mDebugInfo = "";
            updateCmdSpeedMsg();
            updateCmdDirectionMsg();
        }

        @Override
        protected void updateSpeed() {
            targetMoveSpeed = TARGET_MOVE_SPEED;
        }
    }

    class ForwardPwdControl extends MoveControl {
        public ForwardPwdControl() {
            super(CMD_FORWARD_PWD);
            updateCommandText(CMD_FORWARD_PWD);
        }
    }

    class ForwardTargetControl extends MoveControl {
        private Point mDestPos;
        public ForwardTargetControl(Point destPose) {
            super(CMD_FORWARD_TARGET);
            mDestPos = new Point(destPose);
            updateCommandText(CMD_FORWARD_TARGET);
        }

        public void updateDestPose(Point destPose) {
            mDestPos.x = destPose.x;
            mDestPos.y = destPose.y;
        }

        @Override
        protected void updateCommandText(int runCmd) {
            String cmdStr = getStringFromCmd(CarControl.CMD_FORWARD) + " by dest pose (" + mDestPos.x + ", " + mDestPos.y + ")";
            setCommandText(cmdStr);
        }

        @Override
        protected void updateCmdDirectionMsg() {
            float currDirection = mCarPose.getDeviceYawDegree(true);
            Point currPos = mCarPose.getCurrentGridIndex();
            float expectedDirection = MathUtils.calculateOrientation(currPos, mDestPos);
            float orientationDelta = expectedDirection - currDirection;
            orientationDelta = MathUtils.formatDegree180(orientationDelta);
            float pidDelta = mDirectionPID.update(orientationDelta);
            byte newMsg = updateMoveMsg(mCmdMsg3[1], pidDelta);
            mDebugInfo += "\ndirection: " + "target(" + expectedDirection + ")"
                    + "current(" + currDirection + ")"
                    + ", left msg(" + mCmdMsg3[1] + ")"
                    + ", delta(" + orientationDelta + ")"
                    + ", pid delta(" + pidDelta + ")"
                    + ", right msg(" + newMsg + ")";
            mCmdMsg3[2] = newMsg;
        }
    }

    class TurnControl extends RunControl {
        AdvancePID mTurnSpeedPID;
        boolean positive;
        //boolean mUniformSpeed;
        float mTargetTurnSpeed;
        float mDefTargetTurnSpeed;

        private boolean mNoDegree = false;
        private boolean mTurnOver = false;
        private float mDegree = 0;
        private float mTargetDegree = 0;

        public TurnControl(int runCmd) {
            this(runCmd, true, 0);
        }

        public TurnControl(int runCmd, boolean noDegree, float degree) {
            super(runCmd);
            mIsTurn = true;
            mNoDegree = noDegree;
            mDegree = degree;
            updateCommandText(runCmd);
            if (mNoDegree) {
                init(runCmd);
            } else {
                mTargetDegree = MathUtils.formatDegree180(degree);
                float currentDegree = mCarPose.getDeviceYawDegree();
                float deltaDegree = MathUtils.formatDegree180(mTargetDegree - currentDegree);
                if (deltaDegree >= -2 && deltaDegree <= 2) {
                    mTurnOver = true;
                } else if (deltaDegree > 2) {
                    init(CMD_TURN_LEFT);
                } else if (deltaDegree < -2) {
                    init(CMD_TURN_RIGHT);
                }
            }
        }

        @Override
        protected void updateCommandText(int runCmd) {
            String cmdStr = getStringFromCmd(runCmd) + (mNoDegree ? "" : mDegree + mActivity.getString(R.string.str_degree));
            setCommandText(cmdStr);
        }

        protected void init(int runCmd) {
            mTargetTurnSpeed = TARGET_TURN_SPEED;
            mDefTargetTurnSpeed = TARGET_TURN_SPEED;
            if (CMD_TURN_LEFT == runCmd) {
                positive = true;
            }
            if (CMD_TURN_RIGHT == runCmd) {
                positive = false;
            }
            mTurnSpeedPID = new AdvancePID(TARGET_TURN_SPEED, RATE_OF_TURN_SPEED_TO_MSG / 2, 0f, 0.1f) {
                @Override
                protected float defaultOut() {
                    float defVal = turnSpeedToMsg(expect);
                    Log.i(TAG, "mTurnSpeedPID.defaultOut(), expect speed (" + expect + "), defVal(" + defVal + ")");
                    return defVal;
                }
                @Override
                protected float plusOut(float base, float delta) {
                    return updateMsg(base, delta);
                }
            };
            mTurnSpeedPID.setLag(3);
            mTurnSpeedPID.setScope(TURN_SPEED_MAX, TURN_SPEED_MIN);
            if (mCurrentTurnSpeedMessage == 0) {
                mCurrentTurnSpeedMessage = TURN_SPEED_INIT_MSG;
            }
            mCmdMsg2[0] = cmdToMsg(runCmd);
            mCmdMsg2[1] = mCurrentTurnSpeedMessage;
        }

        @Override
        protected void updateCmdMsg() {
            mDebugInfo = "";
            float deltaDegree = 0;
            boolean zero = false;
            if (!mNoDegree) {
                float currentDegree = mCarPose.getDeviceYawDegree();
                deltaDegree = MathUtils.formatDegree180(mTargetDegree - currentDegree);
                if (deltaDegree > 0) {
                    mCmdMsg2[0] = MSG_CMD_TURN_LEFT;
                    positive = true;
                } else {
                    mCmdMsg2[0] = MSG_CMD_TURN_RIGHT;
                    positive = false;
                }
                deltaDegree = Math.abs(deltaDegree);
                if (deltaDegree < 50) {
                    zero = true;
                    mTurnSpeedPID.setScope(TURN_SPEED_MAX, TURN_SPEED_ZERO);
                    mTargetTurnSpeed = TURN_SPEED_ZERO + (TURN_SPEED_MIN - TURN_SPEED_ZERO) * deltaDegree / 40;
                } else if (deltaDegree < 90) {
                    mTargetTurnSpeed = TURN_SPEED_MIN + (TARGET_TURN_SPEED - TURN_SPEED_MIN) * deltaDegree / 40;
                } else {
                    mTargetTurnSpeed = mDefTargetTurnSpeed;
                }
            } else {
                mTargetTurnSpeed = mDefTargetTurnSpeed;
            }

            mTurnSpeedPID.setExpect(mTargetTurnSpeed);
            float currentSpeed = (float) mCarSpeed.getInstantTurnSpeed();
            currentSpeed = positive ? currentSpeed : 0 - currentSpeed;

            byte newMsg = (byte) mTurnSpeedPID.compute(currentSpeed);

            mDebugInfo += "\nTurn Speed:" + mTurnSpeedPID.getDebugInfo();

            mCmdMsg2[1] = newMsg;
            if (mTargetTurnSpeed == TARGET_TURN_SPEED) {
                mCurrentTurnSpeedMessage = newMsg;
            }

            if (!mNoDegree) {
                if (deltaDegree <= 2 && currentSpeed < 2) {
                    mTurnOver = true;
                }
            }
        }

        @Override
        protected boolean isTurnOver() {
            return mTurnOver;
        }

        @Override
        protected void updateSpeed() {
            mDefTargetTurnSpeed = TARGET_TURN_SPEED;
        }
    }

    class LeftTurnControl extends TurnControl {
        public LeftTurnControl() {
            super(CMD_TURN_LEFT);
        }
    }

    class RightTurnControl extends TurnControl {
        public RightTurnControl() {
            super(CMD_TURN_RIGHT);
        }
    }

    class DegreeTurnControl extends TurnControl {
        public DegreeTurnControl(float degree) {
            super(CMD_TURN, false, degree);
        }
    }

    public Status getStatus() {
        return mStatus;
    }

    class ControlHandler extends Handler {
        private static final long SEND_CMD_DELAY = 100;
        private RunControl mRunControl = null;
        private boolean mRunning = false;

        private TurnArgs getTurnArgs(Message msg) {
            if (msg != null && msg.obj != null
                    && msg.obj instanceof TurnArgs) {
                TurnArgs turnArgs = (TurnArgs) msg.obj;
                return turnArgs;
            }
            return null;
        }

        private void stop() {
            removeMessages(MSG_LOOP_SEND_CMD);
            mRunControl = null;
            doStop();
            mIsStop = true;
            mRunning = false;
        }

        private void loopSendCmd() {
            if (mRunControl != null) {
                if (mRunControl.isTurn() && mRunControl.isTurnOver()) {
                    Log.i(TAG, "loopSendCmd(), now is turn and has turn over");
                    turnOver(true);
                    stop();
                    return;
                }
                mRunControl.updateSpeed();
                mRunControl.updateCmdMsg();
                byte[] cmdMsg = mRunControl.getCmdMsg();
                mStatus.debugInfo = "sendMsg " + BytesUtils.bytesToString(cmdMsg) + mRunControl.debugInfo();
                Log.i(TAG, "CarControl " + mStatus.debugInfo);
                sendCommandMsg(cmdMsg);
            }
            sendMessageDelayed(this.obtainMessage(MSG_LOOP_SEND_CMD), SEND_CMD_DELAY);
        }

        private void createTurnControl(TurnArgs turnArgs) {
            if (turnArgs == null) {
                Log.e(TAG, "argument turn args is null");
            }

            int cmd = turnArgs.mCmd;
            if (turnArgs.mNoDegree) {
                if (cmd != CMD_TURN_LEFT
                        && cmd != CMD_TURN_RIGHT) {
                    Log.e(TAG, "turn args no degree, but cmd is [" + cmd + "]");
                    return;
                }
                mRunControl = new TurnControl(turnArgs.mCmd);
                return;
            }
            float degree = turnArgs.mDegree;
            float targetDegree;
            if (cmd == CMD_TURN) {
                targetDegree = degree;
            } else if (cmd == CMD_TURN_LEFT
                    || cmd == CMD_TURN_RIGHT) {
                if (cmd == CMD_TURN_LEFT) {
                    targetDegree = mCarPose.getDeviceYawDegree() + degree;
                } else {
                    targetDegree = mCarPose.getDeviceYawDegree() - degree;
                }
                targetDegree = MathUtils.formatDegree(targetDegree);
            } else {
                Log.e(TAG, "turn args error, cmd [" + cmd + "] is not valid!");
                return;
            }
            mStatus.lastTargetDegree = targetDegree;
            mRunControl = new DegreeTurnControl(targetDegree);
        }

        private void checkAndBeginLoopSendCmd() {
            Log.i(TAG, "checkAndBeginLoopSendCmd() mRunning is " + mRunning);
            if (!mRunning) {
                Log.i(TAG, "checkAndBeginLoopSendCmd() start loop send command ");
                sendMessage(this.obtainMessage(MSG_LOOP_SEND_CMD));
                mRunning = true;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_LOOP_SEND_CMD) {
                loopSendCmd();
            } else {
                mIsStop = false;
                if (msg.what == MSG_FORWARD) {
                    mRunControl = new ForwardControl();
                } else if (msg.what == MSG_FORWARD_PWD) {
                    mRunControl = new ForwardPwdControl();
                } else if (msg.what == MSG_FORWARD_TARGET) {
                    if (msg.obj != null && msg.obj instanceof Point) {
                        Point destPose = (Point) msg.obj;
                        if (mRunControl instanceof ForwardTargetControl) {
                            ForwardTargetControl forwardTargetControl = (ForwardTargetControl)mRunControl;
                            forwardTargetControl.updateDestPose(destPose);
                        } else {
                            mRunControl = new ForwardTargetControl(destPose);
                        }
                    }
                } else if (msg.what == MSG_BACK) {
                    mRunControl = new BackControl();
                } else if (msg.what == MSG_TURN) {
                    createTurnControl(getTurnArgs(msg));
                } else if (msg.what == MSG_TURN_LEFT) {
                    mRunControl = new SimpleTurnControl(CMD_TURN_LEFT);
                } else if (msg.what == MSG_TURN_RIGHT) {
                    mRunControl = new SimpleTurnControl(CMD_TURN_LEFT);
                } else if (msg.what == MSG_STOP) {
                    stop();
                    return;
                } else {
                    return;
                }
                checkAndBeginLoopSendCmd();
            }
        }
    }

    class TurnArgs {
        public int mCmd;
        public float mDegree;
        public boolean mNoDegree;
        public TurnArgs(int cmd, float degree, boolean noDegree) {
            mCmd = cmd;
            mDegree = degree;
            mNoDegree = noDegree;
        }
    }

    public static class Status implements DataSerializable {
        public float lastTargetDegree;
        public String debugInfo;
        public String command;

        public void writeToDataOutputStream(DataOutputStream out) throws IOException {
            out.writeFloat(lastTargetDegree);
            SerializableUtils.writeStringToData(debugInfo, out);
            SerializableUtils.writeStringToData(command, out);
        }

        public void readFromDataInputStream(DataInputStream in) throws IOException {
            lastTargetDegree = in.readFloat();
            debugInfo = SerializableUtils.readStringFromData(in);
            command = SerializableUtils.readStringFromData(in);
        }
    }

    private class AdvancePID extends PID {
        private SimpleQueue<Float> mOutQueue = new SimpleQueue<Float>(Float.class, 6);
        protected float expect;
        private int lag = 0;
        private String debugInfo = null;
        public AdvancePID(float expect, float KP, float KI, float KD) {
            super(KP, KI, KD);
            this.expect = expect;
        }

        public void setLag(int lag) {
            this.lag = lag;
        }

        public void setExpect(float expect) {
            this.expect = expect;
        }

        public float compute(float current) {
            float outVal = 0;
            debugInfo = "";
            if (needPid(current)) {
                float delta = computeDelta(current);
                float pidDelta = update(delta);
                float old = mOutQueue.get(0 - 1 - lag);
                outVal = plusOut(old, pidDelta);
                debugInfo += "expect(" + expect + ")"
                        + ",current(" + current + ")"
                        + ",delta(" + delta + ")"
                        + ",pid delta(" + pidDelta + ")"
                        + ",old out(" + old + ")"
                        + ",out(" + outVal + ")";
            } else {
                outVal = defaultOut();
                debugInfo += "expect(" + expect + ")"
                        + ",current(" + current + ")"
                        + ",use default(" + outVal + ")";
            }
            mOutQueue.put(outVal);
            return outVal;
        }

        public String getDebugInfo() {
            return debugInfo;
        }

        private float pidMin;
        private float pidMax;
        public void setScope(float max, float min) {
            pidMin = min;
            pidMax = max;
        }

        protected float computeDelta(float current) {
            return expect - current;
        }

        protected float plusOut(float base, float delta) {
            return base + delta;
        }

        protected float defaultOut() {
            return 0;
        }

        protected boolean needPid(float current) {
            if (current <= pidMax && current >= pidMin) {
                if (mOutQueue.isFull()) {
                    return true;
                }
            }
            return false;
        }

    }

    private class PID {
        private float KP = 2;
        private float KI = 0;
        private float KD = 1;

        public PID(float KP, float KI, float KD) {
            this.KP = KP;
            this.KI = KI;
            this.KD = KD;
        }

        private float last_delta = 0;
        private float sum = 0;
        public float update(float delta) {
            float r;
            sum += delta;
            r = KP * delta + KI * sum + KD * (delta - last_delta);
            last_delta = delta;
            return r;
        }
    }
}
