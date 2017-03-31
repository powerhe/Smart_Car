package com.lenovo.newdevice.tangocar.data;

import android.graphics.Point;
import android.graphics.Rect;

import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.control.engine.BaseEngine;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/2/22.
 */

public class CarStatus implements DataSerializable {
    private int mEngineType;
    private String mEngineState;
    private Point mEngineTarget;
    private CarControl.Status mCarControlStatus;
    private boolean mUsbConnected;
    private String mUsbReceiveInfo;
    private boolean mTangoConnected;
    private boolean mRunning;

    public CarStatus() {
        mEngineType = BaseEngine.ENGINE_TYPE_NONE;
        mEngineState = null;
        mEngineTarget = null;
    }

    public void setEngineState(String engineState) {
        mEngineState = engineState;
    }

    public String getEngineState() {
        return mEngineState;
    }

    public void setEngineTarget(Point targetPoint) {
        mEngineTarget = targetPoint;
    }

    public Point getEngineTarget() {
        return mEngineTarget;
    }

    public void setCarControlStatus(CarControl.Status status) {
        mCarControlStatus = status;
    }

    public String getControlCommand() {
        CarControl.Status status = mCarControlStatus;
        if (status != null) {
            return status.command;
        }
        return null;
    }

    public String getControlDebugInfo() {
        CarControl.Status status = mCarControlStatus;
        if (status != null) {
            return status.debugInfo;
        }
        return null;
    }

    public float getControlTargetDegree() {
        CarControl.Status status = mCarControlStatus;
        if (status != null) {
            return status.lastTargetDegree;
        }
        return 0;
    }

    public void setTangoConnected(boolean connected) {
        mTangoConnected = connected;
    }

    public boolean isTangoConnected() {
        return mTangoConnected;
    }

    public void setUsbConnected(boolean connected) {
        mUsbConnected = connected;
    }

    public boolean isUsbConnected() {
        return mUsbConnected;
    }

    public void setUsbReceiveInfo(String usbReceiveInfo) {
        mUsbReceiveInfo = usbReceiveInfo;
    }

    public String getUsbReceiveInfo() {
        return mUsbReceiveInfo;
    }

    public void setRunning(boolean running) {
        mRunning = running;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeInt(mEngineType);
        SerializableUtils.writeStringToData(mEngineState, out);
        boolean hasTarget = mEngineTarget != null;
        out.writeBoolean(hasTarget);
        if (hasTarget) {
            out.writeInt(mEngineTarget.x);
            out.writeInt(mEngineTarget.y);
        }
        SerializableUtils.writeObjectToData(mCarControlStatus, out);
        out.writeBoolean(mUsbConnected);
        SerializableUtils.writeStringToData(mUsbReceiveInfo, out);
        out.writeBoolean(mTangoConnected);
        out.writeBoolean(mRunning);
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        mEngineType = in.readInt();
        mEngineState = SerializableUtils.readStringFromData(in);
        boolean hasTarget = in.readBoolean();
        if (hasTarget) {
            if (mEngineTarget == null) {
                mEngineTarget = new Point();
            }
            mEngineTarget.x = in.readInt();
            mEngineTarget.y = in.readInt();
        }
        mCarControlStatus = (CarControl.Status) SerializableUtils.readObjectFromData(in);
        mUsbConnected = in.readBoolean();
        mUsbReceiveInfo = SerializableUtils.readStringFromData(in);
        mTangoConnected = in.readBoolean();
        mRunning = in.readBoolean();
    }
}
