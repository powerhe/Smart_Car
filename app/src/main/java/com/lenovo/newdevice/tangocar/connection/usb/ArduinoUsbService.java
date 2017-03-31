package com.lenovo.newdevice.tangocar.connection.usb;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.lenovo.newdevice.tangocar.connection.Connection;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/15.
 */

public class ArduinoUsbService extends Connection {
    public static final String ACTION_ARDUINO_INIT = "com.tangocar.connection.usb.ARDUINO_INIT";
    public static final String ACTION_ARDUINO_PERMISSION = "com.tangocar.connection.usb.ARDUINO_PERMISSION";
    public static final String ACTION_ARDUINO_ATTACHED = "com.tangocar.connection.usb.ARDUINO_ATTACHED";
    public static final String ACTION_ARDUINO_DETACHED = "com.tangocar.connection.usb.ARDUINO_DETACHED";

    private static final String ARDUINO_MODEL = "ArduinoBlinkLED";
    private static final String ARDUINO_MANUFACTURER = "TKJElectronics";

    private static final int MSG_OPEN_ACCESSORY = 100;

    private UsbAccessory mAccessory;
    private Dialog mNoPermissionAlert;
    private UsbManager mUsbManager;
    private ParcelFileDescriptor mFileDescriptor;

    private UsbDetachedReceiver mDetachedReceiver;

    private static PendingIntent sPermissionIntent = null;
    private static UsbManager sUsbManager = null;

    @Override
    public void onCreate() {
        super.onCreate();
        setNeedReceiveThread();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setTitle("No permission")
                .setMessage("Permission denied for usb accessory")
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //mActivity.finish();
                    }
                });
        mNoPermissionAlert = builder.create();
        registerDetachedReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDetachedReceiver();
        closeAccessory();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        UsbAccessory accessory = null;
        boolean init = false;
        boolean detached = false;
        boolean attached = false;

        Log.i(TAG, "ArduinoUsbService onStartCommand action is " + action);
        if (action == null) {
            return START_NOT_STICKY;
        }

        accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        Log.i(TAG, "ArduinoUsbService onStartCommand accessory is " + accessory);

        if (ACTION_ARDUINO_INIT.equals(action)) {
            init = true;
        }
        if (ACTION_ARDUINO_ATTACHED.equals(action)) {
            attached = true;
        }
        if (ACTION_ARDUINO_DETACHED.equals(action)) {
            detached = true;
        }

        if (ACTION_ARDUINO_PERMISSION.equals(action)) {
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                attached = true;
            } else {
                Log.d(TAG, "ArduinoUsbService Permission denied for accessory " + accessory);
                mNoPermissionAlert.dismiss();
                mNoPermissionAlert.show();
                return START_NOT_STICKY;
            }
        }

        if (accessory == null) {
            return START_NOT_STICKY;
        }

        if ((init && !isConnected()) || attached) {
            openAccessoryCheckUsb(accessory);
        }
        if (detached) {
            if (accessory.equals(mAccessory)) {
                closeAccessory();
            }
        }
        if (!detached) {
            return START_STICKY;
        }

        return START_NOT_STICKY;
    }

    protected void onCreateHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_OPEN_ACCESSORY) {
                    UsbAccessory accessory = (UsbAccessory) msg.obj;
                    openAccessory(accessory);
                }
            }
        };
    }

    private void openAccessoryCheckUsb(UsbAccessory accessory) {
        if (usbIsGood()) {
            openAccessory(accessory);
        } else {
            mHandler.removeMessages(MSG_OPEN_ACCESSORY);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_OPEN_ACCESSORY, accessory), 8000);
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        Log.i(TAG, "ArduinoUsbService openAccessory(" + accessory + ")");
        try {
            mFileDescriptor = mUsbManager.openAccessory(accessory);
        } catch (Exception e) {
            Log.e(TAG, "open accessory error:", e);
        }
        Log.i(TAG, "ArduinoUsbService mFileDescriptor is " + mFileDescriptor);
        if (mFileDescriptor != null) {
            Log.d(TAG, "Accessory opened ok");
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            connected();
        } else {
            Log.d(TAG, "Accessory open failed");
        }
    }

    private void closeAccessory() {
        disconnected();
        // Close all streams
        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (Exception ignored) {
        } finally {
            mInputStream = null;
        }
        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (Exception ignored) {
        } finally {
            mOutputStream = null;
        }
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException ignored) {
        } finally {
            mFileDescriptor = null;
        }
        mAccessory = null;
    }

    private void registerDetachedReceiver() {
        if (mDetachedReceiver == null) {
            mDetachedReceiver = new UsbDetachedReceiver();
        }
        registerReceiver(mDetachedReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED));
    }

    private void unregisterDetachedReceiver() {
        if (mDetachedReceiver != null) {
            unregisterReceiver(mDetachedReceiver);
        }
    }

    private class UsbDetachedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Intent serviceIntent = new Intent(context, ArduinoUsbService.class);
            UsbAccessory accessory = null;

            Log.i(TAG, "UsbDetachedReceiver onReceive {" + intent + "}");
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            }
            Log.i(TAG, "UsbDetachedReceiver accessory is " + accessory);
            if (accessory == null) {
                return;
            }
            if (ArduinoUsbService.checkArduinoUsb(accessory)) {
                if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                    serviceIntent.setAction(ArduinoUsbService.ACTION_ARDUINO_DETACHED);
                }
                serviceIntent.putExtra(UsbManager.EXTRA_ACCESSORY, accessory);
                context.startService(serviceIntent);
            }
        }
    }

    public static void start(ContextWrapper contextWrapper, Intent intent) {
        Intent serviceIntent = new Intent(contextWrapper, ArduinoUsbService.class);
        UsbAccessory accessory = null;
        String action = ArduinoUsbService.ACTION_ARDUINO_INIT;
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(intent.getAction())) {
            action = ArduinoUsbService.ACTION_ARDUINO_ATTACHED;
            accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        } else {
            accessory = ArduinoUsbService.getArduinoUsb(contextWrapper);
        }
        serviceIntent.setAction(action);
        if (accessory != null) {
            serviceIntent.putExtra(UsbManager.EXTRA_ACCESSORY, accessory);
            if (ArduinoUsbService.checkAndRequestUsbPermission(contextWrapper, accessory)) {
                contextWrapper.startService(serviceIntent);
            }
        } else {
            Log.e(TAG, "Usb accessory is null when start with intent " + intent);
        }
    }

    private static UsbManager getUsbManager(ContextWrapper contextWrapper) {
        if (sUsbManager == null) {
            sUsbManager = (UsbManager) contextWrapper.getSystemService(Context.USB_SERVICE);
        }
        return sUsbManager;
    }

    private static UsbAccessory getArduinoUsb(ContextWrapper contextWrapper) {
        UsbAccessory[] accessories = getUsbManager(contextWrapper).getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null && checkArduinoUsb(accessory)) {
            return accessory;
        } else {
            return null;
        }
    }

    private static boolean checkArduinoUsb(UsbAccessory accessory) {
        if (ARDUINO_MODEL.equals(accessory.getModel())
                && ARDUINO_MANUFACTURER.equals(accessory.getManufacturer())) {
            return true;
        }
        Log.i(TAG, "not arduino device: {model:" + accessory.getModel() + ", manufacturer:" + accessory.getManufacturer() + "}");
        return false;
    }

    private static boolean checkAndRequestUsbPermission(ContextWrapper contextWrapper, UsbAccessory accessory) {
        UsbManager usbManager = getUsbManager(contextWrapper);
        if (sPermissionIntent == null) {
            Intent serviceIntent = new Intent(contextWrapper, ArduinoUsbService.class);
            serviceIntent.setAction(ACTION_ARDUINO_PERMISSION);
            sPermissionIntent = PendingIntent.getService(contextWrapper, 0, serviceIntent, 0);
        }
        if (usbManager.hasPermission(accessory)) {
            Log.i(TAG, "ArduinoUsbService has permission to access accessory:{" + accessory + "}");
            return true;
        } else {
            Log.i(TAG, "ArduinoUsbService request permission to access accessory:{" + accessory + "}");
            usbManager.requestPermission(accessory, sPermissionIntent);
            return false;
        }
    }

    private static boolean usbIsGood() {
        if (Utils.isBuildUserOrUserDebug()) {
            Date buildDate = Utils.getBuildDate();
            if (buildDate == null) {
                return false;
            }
            Date goodBuildDate = new Date(117, 2, 1); // 2017-02-01
            if (buildDate.compareTo(goodBuildDate) >= 0) {
                return true;
            }
            Date liujk2BuildDate = new Date(1484646135L); // Tue Jan 17 17:42:15 CST 2017
            if (buildDate.compareTo(liujk2BuildDate) == 0
                    && Build.USER.equals("liujk2")) {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }
}
