package com.lenovo.newdevice.tangocar.remote;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;
/**
 * Created by liujk2 on 2017/2/8.
 */

public class RCHeartBeat {
    private boolean mIsController;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    public RCHeartBeat(boolean isController, InputStream inputStream, OutputStream outputStream) {
        mIsController = isController;
        mInputStream = inputStream;
        mOutputStream = outputStream;
    }

    private boolean hostOneTime() {
        boolean writeReadOK = false;
        RCMessage heartBeatMessage = RCMessage.getHeartBeatMessage();
        try {
            heartBeatMessage.send(mOutputStream);
            writeReadOK = true;
        } catch (IOException e) {
            Log.e(TAG, "hostOneTime send error:", e);
        }
        if (writeReadOK) {
            writeReadOK = false;
            try {
                RCMessage replyMessage = RCMessage.readFromInputStream(mInputStream);
                if (replyMessage.isReplyFor(heartBeatMessage)) {
                    writeReadOK = true;
                } else {
                    Log.e(TAG, "message is not reply for last heart beat");
                }
            } catch (IOException e) {
                Log.e(TAG, "hostOneTime read error:", e);
            }
        }
        return writeReadOK;
    }

    private boolean controllerOneTime() {
        boolean readWriteOK = false;
        RCMessage heartBeatMessage = null;
        try {
            heartBeatMessage = RCMessage.readFromInputStream(mInputStream);
            if (heartBeatMessage.isHeartBeat()) {
                readWriteOK = true;
            } else {
                Log.e(TAG, "message is not heart beat");
            }
        } catch (IOException e) {
            Log.e(TAG, "controllerOneTime read error:", e);
        }
        if (readWriteOK) {
            readWriteOK = false;
            try {
                RCMessage replyMessage = heartBeatMessage.getReply();
                replyMessage.send(mOutputStream);
                readWriteOK = true;
            } catch (IOException e) {
                Log.e(TAG, "controllerOneTime send error:", e);
            }
        }

        return readWriteOK;
    }

    public boolean oneTime() {
        if (mIsController) {
            return controllerOneTime();
        } else {
            return hostOneTime();
        }
    }
}
