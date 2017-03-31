package com.lenovo.newdevice.tangocar.main;

import android.graphics.Point;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.MainActivity;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.control.engine.BaseEngine;
import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.render.opengl.GLGridMapView;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.Utils;

/**
 * Created by liujk2 on 2017/3/14.
 */

public class ControlView implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private TangoCar mTangoCar;
    private MainActivity mActivity;

    private Button mStartBtn;

    private GLGridMapView mMapView;

    private View mEngineView;
    private RadioGroup mEngineGroup;
    private RadioButton mEngineSimple;
    private RadioButton mEngineTarget;
    private RadioButton mEngineNormal;
    private RadioButton mEngineTurnTest;
    private RadioButton mForwardTurnTest;

    private View mTargetView;
    private EditText mTargetXEdit;
    private EditText mTargetYEdit;

    private static final int VIEW_NONE = 0;
    private static final int VIEW_STOP = 1;
    private static final int VIEW_SELECT = 2;
    private static final int VIEW_START = 3;
    private int mViewMode = VIEW_STOP;

    private int mEngineType = BaseEngine.ENGINE_TYPE_SIMPLE;
    private FloatPoint mTargetPoint = new FloatPoint(10, 10);
    final private Object mTargetLock = new Object();

    public ControlView(MainActivity activity) {
        mActivity = activity;

        mStartBtn = (Button) mActivity.findViewById(R.id.start_button);
        mStartBtn.setOnClickListener(this);

        mEngineView = mActivity.findViewById(R.id.engine_select_layout);
        mEngineGroup = (RadioGroup) mActivity.findViewById(R.id.engine_group);
        mEngineSimple = (RadioButton) mActivity.findViewById(R.id.engine_simple);
        mEngineTarget = (RadioButton) mActivity.findViewById(R.id.engine_target);
        mEngineNormal = (RadioButton) mActivity.findViewById(R.id.engine_normal);
        mEngineTurnTest = (RadioButton) mActivity.findViewById(R.id.engine_turn_test);
        mForwardTurnTest = (RadioButton) mActivity.findViewById(R.id.engine_forward_test);
        mEngineGroup.setOnCheckedChangeListener(this);

        mTargetView = mActivity.findViewById(R.id.target_point_input_layout);
        mTargetXEdit = (EditText) mActivity.findViewById(R.id.target_point_x);
        mTargetYEdit = (EditText) mActivity.findViewById(R.id.target_point_y);
    }

    public void onResume() {
        updateByMode();
    }

    public void setTangoCar(TangoCar tangoCar) {
        mTangoCar = tangoCar;
    }

    public void setStartOrStop(boolean start) {
        if (start) {
            mViewMode = VIEW_START;
        } else {
            mViewMode = VIEW_STOP;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateByMode();
            }
        });
    }

    private void updateByMode() {
        switch (mViewMode) {
            case VIEW_STOP:
                mEngineView.setVisibility(View.GONE);
                mStartBtn.setText(R.string.btn_str_start);
                break;
            case VIEW_SELECT:
                mEngineView.setVisibility(View.VISIBLE);
                mStartBtn.setText(R.string.btn_str_make_sure);
                break;
            case VIEW_START:
                mEngineView.setVisibility(View.GONE);
                mStartBtn.setText(R.string.btn_str_stop);
                break;
        }
        updateTargetView();
    }

    private void updateTargetView() {
        boolean displayTarget = false;
        if (mEngineType == BaseEngine.ENGINE_TYPE_TARGET) {
            mTargetView.setVisibility(View.VISIBLE);
            setTargetToView();
            if (mMapView != null) {
                setTargetPointOnMapView();
                if (mViewMode != VIEW_STOP) {
                    displayTarget = true;
                }
            }
        } else {
            mTargetView.setVisibility(View.GONE);
        }
        if (mMapView != null) {
            mMapView.setDisplayTarget(displayTarget);
        }
    }

    private void doAction(int newMode) {
        if (newMode == VIEW_START) {
            boolean startOK = false;
            if (mTangoCar != null) {
                DataSerializable controlData = null;
                if (mEngineType == BaseEngine.ENGINE_TYPE_TARGET) {
                    getTargetFromView();
                    controlData = mTargetPoint;
                }
                startOK = mTangoCar.startEngine(mEngineType, controlData);
            }
            if (startOK) {
                mViewMode = newMode;
            } else {
                Utils.showToast(mActivity, R.string.toast_engine_start_failed);
            }
        } else {
            if (newMode == VIEW_STOP) {
                if (mTangoCar != null) {
                    mTangoCar.stopEngine();
                }
            }
            mViewMode = newMode;
        }
    }

    private void getTargetFromView() {
        synchronized (mTargetLock) {
            mTargetPoint.x = getFloatFromTextView(mTargetXEdit);
            mTargetPoint.y = getFloatFromTextView(mTargetYEdit);
        }
    }

    private void setTargetToView() {
        setTargetPointOnMapView();
        setTargetToEditView();
    }

    private void setTargetToEditView() {
        synchronized (mTargetLock) {
            setFloatToTextView(mTargetXEdit, mTargetPoint.x);
            setFloatToTextView(mTargetYEdit, mTargetPoint.y);
        }
    }

    public void setTarget(FloatPoint point) {
        synchronized (mTargetLock) {
            mTargetPoint.x = point.x;
            mTargetPoint.y = point.y;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTargetView();
                }
            });
        }
    }

    public boolean needSelectTarget() {
        return (mEngineType == BaseEngine.ENGINE_TYPE_TARGET
                && mViewMode == VIEW_SELECT);
    }

    public void setMapView(GLGridMapView mapView) {
        mMapView = mapView;
    }

    private void setTargetPointOnMapView() {
        if (mEngineType == BaseEngine.ENGINE_TYPE_TARGET) {
            if (mMapView != null) {
                boolean setOK = mMapView.setTargetPoint(mTargetPoint);
                if (!setOK) {
                    mMapView.getTargetPoint(mTargetPoint);
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mStartBtn) {
            int newMode = VIEW_NONE;
            if (mViewMode == VIEW_STOP) {
                newMode = VIEW_SELECT;
            } else if (mViewMode == VIEW_SELECT) {
                newMode = VIEW_START;
            } else if (mViewMode == VIEW_START) {
                newMode = VIEW_STOP;
            }
            doAction(newMode);
            updateByMode();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == mEngineSimple.getId()) {
            mEngineType = BaseEngine.ENGINE_TYPE_SIMPLE;
        } else if (checkedId == mEngineTarget.getId()) {
            mEngineType = BaseEngine.ENGINE_TYPE_TARGET;
        } else if (checkedId == mEngineNormal.getId()) {
            mEngineType = BaseEngine.ENGINE_TYPE_NORMAL;
        } else if (checkedId == mEngineTurnTest.getId()) {
            mEngineType = BaseEngine.ENGINE_TYPE_TURN_TEST;
        } else if (checkedId == mForwardTurnTest.getId()) {
            mEngineType = BaseEngine.ENGINE_TYPE_FORWARD_TEST;
        }
        updateTargetView();
    }

    private float setFloatToTextView(TextView textView, float val) {
        textView.setText(String.valueOf(val));
        return val;
    }

    private float getFloatFromTextView(TextView textView) {
        return Float.valueOf(textView.getText().toString());
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int keyAction = event.getAction();
        if (mTangoCar != null) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (keyAction == KeyEvent.ACTION_UP) {
                    if (mTangoCar.isRunning()) {
                        mTangoCar.stopEngine();
                    } else {
                        mTangoCar.startEngine(BaseEngine.ENGINE_TYPE_SIMPLE, null);
                    }
                }
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (keyAction == KeyEvent.ACTION_UP) {
                    mTangoCar.switchRemoteControl();
                }
                return true;
            }
        }
        return false;
    }
}
