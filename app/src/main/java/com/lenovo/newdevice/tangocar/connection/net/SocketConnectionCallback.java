package com.lenovo.newdevice.tangocar.connection.net;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by liujk2 on 2017/2/9.
 */

public interface SocketConnectionCallback {
    public void onConnected(InputStream inputStream, OutputStream outputStream);
    public void onLoop();
    public void onDisconnected();
    public void onServerInvalid();
}
