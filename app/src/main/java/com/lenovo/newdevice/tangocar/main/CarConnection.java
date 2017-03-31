package com.lenovo.newdevice.tangocar.main;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.connection.Connection;
import com.lenovo.newdevice.tangocar.connection.ConnectionCallback;
import com.lenovo.newdevice.tangocar.connection.MessageReceiver;
import com.lenovo.newdevice.tangocar.connection.usb.ArduinoUsbService;
import com.lenovo.newdevice.tangocar.data.CarStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/11/21.
 */

public class CarConnection {
    private boolean mConnected;

    private ArduinoUsbService mConnection;

    private Activity mActivity;
    private CarStatus mCarStatus;

    public CarConnection(Activity activity, CarStatus carStatus) {
        mConnected = false;
        mActivity = activity;
        mCarStatus = carStatus;
    }

    public void onCreate() {
        bindConnection();
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void onDestroy() {
        unbindConnection();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Connection.ConnectionBinder connectionBinder = (Connection.ConnectionBinder) iBinder;
            setupConnection(connectionBinder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConnection = null;
        }
    };

    private void bindConnection() {
        Intent intent = new Intent(mActivity, ArduinoUsbService.class);

        Log.i(TAG, "CarConnection call mActivity.bindService");
        mActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindConnection() {
        Log.i(TAG, "CarConnection call mActivity.unbindService");
        mActivity.unbindService(mServiceConnection);
    }

    private void setupConnection(Connection connection) {
        Log.i(TAG, "CarConnection setupConnection, connection is " + connection);
        if (connection == null) {
            mConnection = null;
        } else if (connection instanceof ArduinoUsbService) {
            mConnection = (ArduinoUsbService) connection;
            if (mConnection.isConnected()) {
                setConnectionStatus(true);
            } else {
                setConnectionStatus(false);
            }
            mConnection.setConnectionCallback(new ConnectionCallback() {
                @Override
                public void onConnected() {
                    setConnectionStatus(true);
                }

                @Override
                public void onDisconnected() {
                    setConnectionStatus(false);
                }

                @Override
                public void onInvalid() {
                }
            });
            mConnection.setMessageReceiver(new MessageReceiver() {
                byte[] buffer = new byte[1024];
                @Override
                public void onReceiveData(InputStream inputStream) {
                    try {
                        int bytes = inputStream.read(buffer);
                        if (bytes > 3) { // The message is 4 bytes long
                            long timer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();
                            mCarStatus.setUsbReceiveInfo(Long.toString(timer));
                        }
                    } catch (IOException ignore) {
                    }
                }
                @Override
                public void onError(String errorInfo) {
                    mCarStatus.setUsbReceiveInfo(errorInfo);
                }
            });
        }
    }

    private void setConnectionStatus(final boolean connected) {
        mConnected = connected;
        mCarStatus.setUsbConnected(connected);
        if (!connected) {
            mCarStatus.setUsbReceiveInfo(mActivity.getString(R.string.na));
        }
    }

    public void sendMessage(byte[] msg) {
        if (!mConnected) {
            return;
        }
        if (mConnection != null) {
            mConnection.sendMessage(msg);
        }
    }
}
