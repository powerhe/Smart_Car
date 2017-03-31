package com.lenovo.newdevice.tangocar.connection;

import android.os.IBinder;

import java.io.InputStream;

/**
 * Created by liujk2 on 2017/1/15.
 */

public interface MessageReceiver {
    public void onReceiveData(InputStream inputStream);
    public void onError(String errorInfo);
}
