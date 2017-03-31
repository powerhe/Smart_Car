package com.lenovo.newdevice.tangocar.connection.net;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/9.
 */

public class ServerSocketConnectionThread extends SocketConnectionThread {
    private ServerSocket mServerSocket = null;
    private boolean mAcceptBlocked = false;

    public ServerSocketConnectionThread(int port, long looptime) {
        super(port, looptime);
    }

    private void breakAccept() {
        if (mAcceptBlocked) {
            // make accept return
            try {
                Socket socket = new Socket("localhost", mPort);
                Log.i(TAG, "localhost socket for port("+mPort+") is " + socket);
                socket.close();
            } catch (IOException e) {
                // ignore
                Log.e(TAG, "something wrong:", e);
            }
            mAcceptBlocked = false;
        }
    }

    private boolean closeServerSocket() {
        boolean res = true;
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close server socket error", e);
                res = false;
            }
            mServerSocket = null;
        }
        return res;
    }

    @Override
    protected Socket getLinkSocket() throws IOException {
        if (mServerSocket == null) {
            mServerSocket = new ServerSocket(mPort);
        }
        mAcceptBlocked = true;
        Socket socket = mServerSocket.accept();
        mAcceptBlocked = false;
        return socket;
    }

    @Override
    protected boolean isClosed() {
        if (mServerSocket != null) {
            return mServerSocket.isClosed() || (mLinkSocket != null && mLinkSocket.isClosed());
        }
        return false;
    }

    @Override
    public void cancel() {
        super.cancel();
        breakAccept();
    }

    @Override
    protected boolean doClosed() {
        Log.i(TAG, getName() + " doClosed", new Exception());
        boolean clientCloseRes = closeLinkSocket();
        boolean serverCloseRes = closeServerSocket();
        if (!clientCloseRes || !serverCloseRes) {
            return false;
        }
        return true;
    }
}
