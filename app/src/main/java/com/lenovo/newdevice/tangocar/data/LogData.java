package com.lenovo.newdevice.tangocar.data;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/3/27.
 */

public class LogData implements DataSerializable {
    private String log;
    public void addLine(String line) {

    }
    public String getLog() {
        return log;
    }
    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        SerializableUtils.writeStringToData(log, out);
    }
    public void readFromDataInputStream(DataInputStream in) throws IOException {
        log = SerializableUtils.readStringFromData(in);
    }
}
