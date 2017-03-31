package com.lenovo.newdevice.tangocar.connection.net;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by liujk2 on 2017/2/9.
 */

public class ClientSocketConnectionThread extends SocketConnectionThread {
    private String mServerIP;

    public ClientSocketConnectionThread(String serverIP, int port, long looptime) {
        super(port, looptime);
        mServerIP = serverIP;
    }

    @Override
    protected Socket getLinkSocket() throws IOException {
        return new Socket(mServerIP, mPort);
    }

    @Override
    protected boolean isClosed() {
        return (mLinkSocket == null || mLinkSocket.isClosed());
    }

    @Override
    protected boolean doClosed() {
        return closeLinkSocket();
    }

}
