package com.lenovo.newdevice.tangocar.data;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/2/23.
 */

public class CarControlValue implements DataSerializable {

    public static final int CONTROL_CMD_START = 1;
    public static final int CONTROL_CMD_STOP = 2;
    public static final int CONTROL_CMD_PAUSE = 3;
    public static final int CONTROL_CMD_RESUME = 4;

    public int mEngineType;
    public int mControlCmd;
    private DataSerializable mControlData;

    public CarControlValue() {
    }

    public CarControlValue(int engineType, int controlCmd) {
        mEngineType = engineType;
        mControlCmd = controlCmd;
    }

    public DataSerializable getControlData() {
        return mControlData;
    }

    public void setControlData(DataSerializable controlData) {
        mControlData = controlData;
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeInt(mEngineType);
        out.writeInt(mControlCmd);
        SerializableUtils.writeObjectToData(mControlData, out);
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        mEngineType = in.readInt();
        mControlCmd = in.readInt();
        mControlData = SerializableUtils.readObjectFromData(in);
    }
}
