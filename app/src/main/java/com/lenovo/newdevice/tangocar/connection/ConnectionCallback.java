package com.lenovo.newdevice.tangocar.connection;

/**
 * Created by liujk2 on 2017/1/15.
 */

public interface ConnectionCallback {
    public void onConnected();
    public void onDisconnected();
    public void onInvalid();
}
