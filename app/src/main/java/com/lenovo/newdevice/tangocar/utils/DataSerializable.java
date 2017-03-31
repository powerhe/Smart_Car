package com.lenovo.newdevice.tangocar.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2016/12/26.
 */

public interface DataSerializable {
    public void writeToDataOutputStream(DataOutputStream out) throws IOException;
    public void readFromDataInputStream(DataInputStream in) throws IOException;
}
