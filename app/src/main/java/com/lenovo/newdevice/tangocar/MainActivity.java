package com.lenovo.newdevice.tangocar;

import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.connection.usb.ArduinoUsbService;
import com.lenovo.newdevice.tangocar.data.RemoteStatus;
import com.lenovo.newdevice.tangocar.main.ControlView;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.main.TangoCar;
import com.lenovo.newdevice.tangocar.path.test.PathFinderTestActivity;
import com.lenovo.newdevice.tangocar.render.opengl.GLGridMapView;
import com.lenovo.newdevice.tangocar.render.rajawali.MapRenderer;
import com.lenovo.newdevice.tangocar.render.rajawali.MySurfaceView;
import com.lenovo.newdevice.tangocar.utils.HandlerThread;
import com.lenovo.newdevice.tangocar.utils.MotionEventAssistant;
import com.lenovo.newdevice.tangocar.utils.Utils;

import org.rajawali3d.scene.ASceneFrameCallback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends BasicEntryActivity {
    public static final String TAG = "TangoCar";

    public static final String ACTION_MAIN = "com.lenovo.newdevice.tangocar.action.MAIN";

    public static final int MODE_NONE = 0;
    public static final int MODE_HOST = 1;
    public static final int MODE_CONTROLLER = 2;

    // for wake lock
    private PowerManager mPowerManager;
    private WakeLock mWakeLock;

    private int mMode = MODE_NONE;

    private GlobalData mGlobalData;
    private TangoCar mTangoCar;
    private boolean mNeedResumeTangoCar;

    private HandlerThread mIdleHandlerThread;
    private Handler mIdleHandler = null;
    private boolean mActivityStop = false;

    // for config
    private boolean mConfigMode;
    private View mConfigView;
    private Button mConfigBtn;

    // for status
    private TextView mMapScope;
    private TextView mCarStatus;
    private TextView mRemoteStatus;

    // for control
    private Button mFinishBtn;
    private ControlView mControlView;

    // for render
    private Button mModeBtn;
    private MySurfaceView mRajawaliMapView;
    private MapRenderer mRenderer;
    private GLGridMapView mGLMapView;
    enum DisplayMode {OPENGL, RAJAWALI};
    private DisplayMode mDisplayMode;

    private void judgeMode(final boolean onCreate, final Intent intent) {
        boolean supportTango = isSupportTango();
        boolean isStartByArduino = false;
        boolean needSelect = false;
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(intent.getAction())) {
            // arduino usb device connected
            isStartByArduino = true;
        }
        int judgeMode = MODE_NONE;
        if (supportTango) {
            if (isStartByArduino) {
                judgeMode = MODE_HOST;
            } else {
                if (mMode == MODE_NONE) {
                    needSelect = true;
                } else {
                    judgeMode = mMode;
                }
            }
        } else {
            needSelect = true;
            Utils.showToast(this, R.string.toast_not_support_tango);
        }
        Log.d(TAG, "mJudgeMode is " + mMode + ", needSelect is " + needSelect);
        if (needSelect) {
            SelectRunDialog dialog = new SelectRunDialog(this, supportTango,
                    new SelectRunDialog.SelectCallback() {
                        @Override
                        public void onSelect(int option) {
                            int mode = MODE_NONE;
                            if (option == SelectRunDialog.SelectCallback.OPTION_HOST) {
                                mode = MODE_HOST;
                            } else if (option == SelectRunDialog.SelectCallback.OPTION_CONTROLLER) {
                                mode = MODE_CONTROLLER;
                            } else if (option == SelectRunDialog.SelectCallback.OPTION_TEST) {
                                Intent testIntent = new Intent();
                                testIntent.setClass(MainActivity.this, PathFinderTestActivity.class);
                                startActivity(testIntent);
                                MainActivity.this.finish();
                            }
                            if (mode != MODE_NONE) {
                                initialize(onCreate, mode, intent);
                            }
                        }
                    });
            dialog.show();
        } else {
            initialize(onCreate, judgeMode, intent);
        }
    }

    private void initialize(final boolean onCreate, final int mode, final Intent intent) {
        if (mode != mMode) {
            mMode = mode;
            if (mMode == MODE_CONTROLLER) {
                setTitle(R.string.remoter_app_name);
            } else {
                setTitle(R.string.app_name);
            }
        }
        checkAllPermissions(new PermissionCheckCallback() {
            @Override
            public void onPermissionOK() {
                initAfterPermissionOK(onCreate, intent);
            }
        });
    }

    private void initAfterPermissionOK(boolean onCreate, Intent intent) {
        if (mMode == MODE_HOST) {
            ArduinoUsbService.start(this, intent);
        }
        if (onCreate) {
            mTangoCar = new TangoCar(this, mControlView);
            mTangoCar.onCreate();
            mTangoCar.switchRunMode(convertMode(mMode));

            if (mRenderer != null) {
                mRenderer.setGlobalData(mGlobalData);
            }
            if (mGLMapView != null) {
                mGLMapView.setControlView(mControlView);
                mGLMapView.setGlobalData(mGlobalData);
            }
            if (mNeedResumeTangoCar) {
                mTangoCar.onResume();
                mNeedResumeTangoCar = false;
            }
        } else {
            mTangoCar.switchRunMode(convertMode(mMode));
        }
    }

    @Override
    public boolean isController() {
        return mMode == MODE_CONTROLLER;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "MainActivity "+this+" call onNewIntent(" + intent + ")");
        judgeMode(false, intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity "+this+" call onCreate(" + savedInstanceState + ")");
        setContentView(R.layout.layout_main);
        Intent intent = getIntent();
        Log.d(TAG, "onCreate() intent is " + intent);
        judgeMode(true, intent);

        mControlView = new ControlView(this);
        ApControl.getInstance().setup(this);

        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Tang Car");

        //for config
        mConfigMode = false;
        mConfigView = findViewById(R.id.config_layout);
        mConfigBtn = (Button) findViewById(R.id.config_button);

        // for status
        mMapScope = (TextView) findViewById(R.id.map_scope);
        mCarStatus = (TextView) findViewById(R.id.car_status);
        mRemoteStatus = (TextView)  findViewById(R.id.remote_status);

        // for control
        mFinishBtn = (Button) findViewById(R.id.finish_button);

        // for render
        mModeBtn = (Button) findViewById(R.id.mode_button);
        mDisplayMode = DisplayMode.OPENGL;
        setupDisplayContent();

        startIdleHandlerThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity "+this+" call onResume()");

        if (mTangoCar == null) {
            mNeedResumeTangoCar = true;
        } else {
            mTangoCar.onResume();
        }

        mControlView.onResume();
        if (mConfigMode) {
            mConfigView.setVisibility(View.VISIBLE);
        } else {
            mConfigView.setVisibility(View.GONE);
        }

        if (mDisplayMode == DisplayMode.OPENGL) {
            if (mGLMapView != null) {
                mGLMapView.onResume();
            }
        }

        startDisplayState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity "+this+" call onPause()");

        if (mNeedResumeTangoCar) {
            mNeedResumeTangoCar = false;
        }
        if (mTangoCar != null) {
            mTangoCar.onPause();
        }

        if (mDisplayMode == DisplayMode.OPENGL) {
            if (mGLMapView != null) {
                mGLMapView.onPause();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity "+this+" call onStart()");
        mWakeLock.acquire();
        mActivityStop = false;

        if (mTangoCar != null) {
            mTangoCar.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity "+this+" call onStop()");
        mWakeLock.release();

        if (mTangoCar != null) {
            mTangoCar.onStop();
        }

        Utils.stop();
        mActivityStop = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity "+this+" call onDestroy()");

        ApControl.getInstance().reset();

        mIdleHandlerThread.quit();
        if (mTangoCar != null) {
            mTangoCar.onDestroy();
        }
    }

    private MotionEventAssistant mDoubleTapProc;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDoubleTapProc == null) {
            mDoubleTapProc = new MotionEventAssistant(MotionEventAssistant.TYPE_DOUBLE_TAP, this) {
                @Override
                protected void onDoubleTap(MotionEvent event) {
                    Log.i(TAG, "MainActivity onDoubleTap()");
                    mTangoCar.displayLog();
                }
            };
        }
        if (mDisplayMode == DisplayMode.RAJAWALI) {
            mRenderer.onTouchEvent(event);
            return true;
        } else if (mDisplayMode == DisplayMode.OPENGL) {
            mGLMapView.procTouchEvent(event);
            mDoubleTapProc.onTouchEvent(event);
            return true;
        }
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mDisplayMode == DisplayMode.OPENGL) {
            return mGLMapView.procGenericMotionEvent(event);
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFullScreenView(View view, int index) {
        RelativeLayout main_layout = (RelativeLayout) findViewById(R.id.layout_main);
        ViewGroup.LayoutParams params =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        main_layout.addView(view, index, params);
    }

    private void setupDisplayContent() {
        if (mRajawaliMapView == null) {
            mRajawaliMapView = (MySurfaceView) findViewById(R.id.rajawali_map_view);
            mRenderer = new MapRenderer(this);
            setupRenderer();
        }

        if (mGLMapView == null) {
            mGLMapView = (GLGridMapView) findViewById(R.id.gl_map_view);
            mGLMapView.setTouchInfoView((TextView) findViewById(R.id.touch_info));
        }

        if (mDisplayMode == DisplayMode.RAJAWALI) {
            mRajawaliMapView.setVisibility(View.VISIBLE);
            mModeBtn.setVisibility(View.VISIBLE);
        } else {
            mRajawaliMapView.setVisibility(View.GONE);
            mModeBtn.setVisibility(View.GONE);
        }
        if (mDisplayMode == DisplayMode.OPENGL) {
            mGLMapView.setVisibility(View.VISIBLE);
        } else {
            mGLMapView.setVisibility(View.GONE);
        }
    }

    private void switchDisplayMode() {
        if (mDisplayMode == DisplayMode.OPENGL) {
            mDisplayMode = DisplayMode.RAJAWALI;
        } else if(mDisplayMode == DisplayMode.RAJAWALI) {
            mDisplayMode = DisplayMode.OPENGL;
        }
        setupDisplayContent();
    }

    public void runOnIdleThread(Runnable r) {
        if (mIdleHandler != null) {
            mIdleHandler.post(r);
        }
    }

    private void startIdleHandlerThread() {
        mIdleHandlerThread = new HandlerThread("IdleHandlerThread"){
            protected void createHandler() {
                mIdleHandler = new Handler();
            }
        };
        mIdleHandlerThread.start();
    }

    public Rect getDisplayScope() {
        Rect mapScope = null;
        if (mDisplayMode == MainActivity.DisplayMode.RAJAWALI) {
            mapScope = (mRenderer == null ? null : mRenderer.getCurrentScope());
        } else if (mDisplayMode == MainActivity.DisplayMode.OPENGL) {
            mapScope = (mGLMapView == null ? null : mGLMapView.getMapScope());
        }
        return mapScope;
    }

    private void startDisplayState() {
        runOnIdleThread(new Runnable() {
            @Override
            public void run() {
                final Rect mapScope = getDisplayScope();
                final String mapScopeStr;
                final int remoteStatusResId;
                final String statusInfo;
                if (mGlobalData != null) {
                    final RemoteStatus remoteStatus = mGlobalData.getRemoteStatus();
                    remoteStatusResId = remoteStatus.enabled ?
                            (remoteStatus.connected ? R.string.connected : R.string.disconnected) :
                            R.string.disabled;
                    if (isController() && !remoteStatus.connected) {
                        statusInfo = "No info";
                    } else {
                        statusInfo = mGlobalData.getCarStatusInfo(0);
                    }
                } else {
                    remoteStatusResId = R.string.na;
                    statusInfo = "No info";
                }

                if (mapScope != null) {
                    mapScopeStr = "(" + mapScope.left + "," + mapScope.top + "," + mapScope.right + "," + mapScope.bottom + ")";
                } else {
                    mapScopeStr = null;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mapScope != null) {
                            mMapScope.setText(mapScopeStr);
                        }
                        mCarStatus.setText(statusInfo);
                        mRemoteStatus.setText(remoteStatusResId);
                    }
                });

                if (!mActivityStop) {
                    mIdleHandler.postDelayed(this, 100);
                }
            }
        });
    }

    public void setGlobalData(GlobalData globalData) {
        mGlobalData = globalData;
    }

    public void setupRenderer() {
        mRajawaliMapView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                // NOTE: This will be executed on each cycle before rendering, called from the
                if (mDisplayMode != DisplayMode.RAJAWALI) {
                    return;
                }
                if (mGlobalData == null) {
                    return;
                }
                if (!mGlobalData.getCarPose(0).hasCameraPose()) {
                    return;
                }
                mRenderer.updateMap();
            }

            @Override
            public boolean callPreFrame() {
                return true;
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });
        mRajawaliMapView.setSurfaceRenderer(mRenderer);
    }

    public void onClickConfigButton(View v) {
        if (!v.equals(mConfigBtn)) {
            return;
        }
        if (mConfigMode) {
            mConfigMode = false;
            mConfigView.setVisibility(View.GONE);
            mConfigBtn.setText(R.string.btn_str_config);
            if (mTangoCar != null) {
                mTangoCar.saveAndApplyConfig();
                if (mTangoCar.isPaused()) {
                    mTangoCar.resumeCar();
                }
            }
        } else {
            mConfigMode = true;
            mConfigView.setVisibility(View.VISIBLE);
            mConfigBtn.setText(R.string.btn_str_ok);
            if (mTangoCar != null) {
                if (!mTangoCar.isPaused()) {
                    mTangoCar.pauseCar();
                }
            }
        }
    }

    public void onClickCleanButton(View v) {
        mTangoCar.clearMapAndPath();
    }

    public void onClickDisplayButton(View v) {
        switchDisplayMode();
    }

    public void onClickModeButton(View v) {
        if (!v.equals(mModeBtn)) {
            return;
        }
        if (mRenderer != null) {
            mRenderer.switchMode();
        }
    }

    public void onClickFinishButton(View v) {
        if (!v.equals(mFinishBtn)) {
            return;
        }
        mTangoCar.finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mControlView.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private TangoCar.RunMode convertMode(int mode) {
        switch (mode) {
            case MODE_HOST:
                return TangoCar.RunMode.STUDY;
            case MODE_CONTROLLER:
                return TangoCar.RunMode.CONTROLLER;
            case MODE_NONE:
            default:
                return TangoCar.RunMode.NONE;
        }
    }
}