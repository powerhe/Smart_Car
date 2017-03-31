package com.lenovo.newdevice.tangocar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.lenovo.newdevice.tangocar.utils.InfoDialog;

/**
 * Created by liujk2 on 2017/2/10.
 */

public abstract class BasicEntryActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.TAG;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1000;
    private static final int REQUEST_CODE_WRITE_SETTINGS_PERMISSION = 1001;
    private static final int REQUEST_CODE_ACCESS_COARSE_PERMISSION = 1002;
    private static final int REQUEST_CODE_ENABLE_LOCATION = 1003;

    protected boolean mSupportTango = false;
    private boolean mJustJudgeSupportTango = false;

    private boolean mHostPermissionChecking = false;
    private boolean mControllerPermissionChecking = false;
    private PermissionCheckCallback mCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected boolean isSupportTango() {
        mJustJudgeSupportTango = true;
        mSupportTango = false;
        grantADFPermission();
        return mSupportTango;
    }

    public boolean isController() {
        return false;
    }

    protected void checkAllPermissions(PermissionCheckCallback callback) {
        if (mCallback != null) {
            Log.w(TAG, "W: call check permission before last permission check over!");
        }
        mCallback = callback;
        checkAllPermissions();
    }

    private void checkAllPermissions() {
        if (isController()) {
            checkAllPermissionsForController();
        } else {
            checkAllPermissionsForHost();
        }
    }

    private void checkAllPermissionsForHost() {
        if (mHostPermissionChecking) {
            return;
        }
        mHostPermissionChecking = true;
        mJustJudgeSupportTango = false;
        grantADFPermission();
    }

    private void checkAllPermissionsForController() {
        if (mHostPermissionChecking) {
            return;
        }
        if (mControllerPermissionChecking) {
            return;
        }
        mControllerPermissionChecking = true;
        requestPermissionForWriteSettings();
    }

    protected void grantADFPermission() {
        try {
            startActivityForResult(
                    Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                    Tango.TANGO_INTENT_ACTIVITYCODE);
            mSupportTango = true;
        } catch (ActivityNotFoundException e) {
        }
    }

    private boolean checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                noEnoughPermissions();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean isLocationEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                != Settings.Secure.LOCATION_MODE_OFF;
    }

    protected void checkLocationEnabled() {
        if (isLocationEnabled()) {
            locationEnabledOK();
        } else {
            InfoDialog.alert(this, getString(R.string.alert_enable_location), new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_LOCATION);
                }
            });
        }
    }

    protected void locationEnabledOK() {
        requestPermissionForWriteSettings();
    }

    protected void checkCoarseLocationPermissions() {
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE_ACCESS_COARSE_PERMISSION)) {
            coarseLocationPermissionOK();
        }
    }

    protected void coarseLocationPermissionOK() {
        //requestPermissionForWriteSettings();
        checkLocationEnabled();
    }

    protected void checkStoragePermissions() {
        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_STORAGE_PERMISSION)) {
            storagePermissionOK();
        }
    }

    private void storagePermissionOK() {
        checkCoarseLocationPermissions();
    }

    private void clearFlags() {
        mControllerPermissionChecking = false;
        mHostPermissionChecking = false;
    }

    protected void allPermissionsOK() {
        clearFlags();
        toastPermissionOK();
        if (mCallback != null) {
            mCallback.onPermissionOK();
            mCallback = null;
        }
    }

    private void toastPermissionOK() {
        Toast.makeText(this, "Permissions is OK!", Toast.LENGTH_SHORT).show();
    }

    protected void noEnoughPermissions() {
        clearFlags();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_not_enough_permissions)
                .setNeutralButton(R.string.btn_str_request_again, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkAllPermissions();
                    }
                })
                .setNegativeButton(R.string.btn_str_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCallback = null;
                    }
                });
        builder.create().show();
    }

    protected void requestPermissionForWriteSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.System.canWrite(this)) {
            InfoDialog.alert(this, getString(R.string.alert_enable_write_settings), new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS_PERMISSION);
                }
            });
        } else {
            allPermissionsOK();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            if (!mJustJudgeSupportTango) {
                if (resultCode == -1) {
                    checkStoragePermissions();
                } else {
                    if (mSupportTango) {
                        noEnoughPermissions();
                    }
                }
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && Settings.System.canWrite(this)) {
                allPermissionsOK();
            } else {
                noEnoughPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_ENABLE_LOCATION) {
            if (isLocationEnabled()) {
                locationEnabledOK();
            } else {
                noEnoughPermissions();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermissionOK();
            } else {
                noEnoughPermissions();
            }
        }
        if (requestCode == REQUEST_CODE_ACCESS_COARSE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                coarseLocationPermissionOK();
            } else {
                noEnoughPermissions();
            }
        }
    }

    public interface PermissionCheckCallback {
        public void onPermissionOK();
    }
}
