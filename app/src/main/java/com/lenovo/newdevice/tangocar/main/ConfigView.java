package com.lenovo.newdevice.tangocar.main;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.MainActivity;
import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.CarConfigValue;

/**
 * Created by liujk2 on 2017/1/18.
 */

public class ConfigView {
    private MainActivity mActivity;

    private EditText mBorderLeft;
    private EditText mBorderRight;
    private EditText mBorderTop;
    private EditText mBorderBottom;
    private EditText mBorderFront;
    private EditText mBorderBack;
    private EditText mGridWidth;
    private EditText mMinDistance;
    private EditText mMinAveDistance;
    private TextView mSpeedVal;
    private SeekBar mSpeedSeekBar;
    private Switch mSwitchStartDelay;
    private Switch mSwitchElectFence;
    private View mSetValueOfElectFence;
    private EditText mElectFenceFront;
    private EditText mElectFenceRight;
    private Switch mSwitchRemoteControl;

    public ConfigView(MainActivity activity) {
        mActivity = activity;
    }

    public void onCreate() {
        mBorderLeft = (EditText) mActivity.findViewById(R.id.border_left);
        mBorderRight = (EditText) mActivity.findViewById(R.id.border_right);
        mBorderTop = (EditText) mActivity.findViewById(R.id.border_top);
        mBorderBottom = (EditText) mActivity.findViewById(R.id.border_bottom);
        mBorderFront = (EditText) mActivity.findViewById(R.id.border_front);
        mBorderBack = (EditText) mActivity.findViewById(R.id.border_back);
        mGridWidth = (EditText) mActivity.findViewById(R.id.grid_width);
        mMinDistance = (EditText) mActivity.findViewById(R.id.min_distance);
        mMinAveDistance = (EditText) mActivity.findViewById(R.id.min_ave_distance);
        mSpeedVal = (TextView) mActivity.findViewById(R.id.set_speed_val);
        setupSpeedSeekBar();
        mSwitchStartDelay = (Switch) mActivity.findViewById(R.id.switch_start_delay);
        setupSwitchElectFence();
        setupSwitchRemoteControl();
    }

    private void setupSpeedSeekBar() {
        mSpeedSeekBar = (SeekBar) mActivity.findViewById(R.id.speed_seek_bar);
        mSpeedSeekBar.setMax(CarControl.SPEED_RATE_MAX - CarControl.SPEED_RATE_MIN);
        mSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mSpeedVal.setText(String.valueOf(CarControl.SPEED_RATE_MIN + mSpeedSeekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initSpeedSeekBar(CarConfigValue configValue) {
        mSpeedSeekBar.setProgress(configValue.mSpeedRate - CarControl.SPEED_RATE_MIN);
    }

    private void setupSwitchElectFence() {
        mSetValueOfElectFence = mActivity.findViewById(R.id.set_val_of_elect_fence);
        mSetValueOfElectFence.setVisibility(View.INVISIBLE);
        mSwitchElectFence = (Switch) mActivity.findViewById(R.id.switch_elect_fence);
        mSwitchElectFence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    mSetValueOfElectFence.setVisibility(View.VISIBLE);
                } else {
                    mSetValueOfElectFence.setVisibility(View.INVISIBLE);
                }
            }
        });
        mElectFenceFront = (EditText) mActivity.findViewById(R.id.elect_fence_front);
        mElectFenceRight = (EditText) mActivity.findViewById(R.id.elect_fence_right);
    }

    private void setupSwitchRemoteControl() {
        View setValueOfRemoteControl = mActivity.findViewById(R.id.set_val_of_remote_control);
        mSwitchRemoteControl = (Switch) mActivity.findViewById(R.id.switch_remote_control);
        setValueOfRemoteControl.setVisibility(View.VISIBLE);
    }

    public void applyConfigToView(CarConfigValue configValue) {
        setFloatToEditText(mBorderLeft, configValue.mBorderLeft);
        setFloatToEditText(mBorderRight, configValue.mBorderRight);
        setFloatToEditText(mBorderTop, configValue.mBorderTop);
        setFloatToEditText(mBorderBottom, configValue.mBorderBottom);
        setFloatToEditText(mBorderFront, configValue.mBorderFront);
        setFloatToEditText(mBorderBack, configValue.mBorderBack);
        setFloatToEditText(mGridWidth, configValue.mGridWidth);
        setFloatToEditText(mMinDistance, configValue.mMinDistance);
        setFloatToEditText(mMinAveDistance, configValue.mMinAveDistance);

        int valInt = configValue.mSpeedRate;
        if (valInt <= CarControl.SPEED_RATE_MAX && valInt >= CarControl.SPEED_RATE_MIN) {
            setIntToTextView(mSpeedVal, valInt);
            initSpeedSeekBar(configValue);
        }

        boolean valBoolean = configValue.mStartDelay;
        mSwitchStartDelay.setChecked(valBoolean);

        valBoolean = configValue.mEnableFence;
        mSwitchElectFence.setChecked(valBoolean);
        setFloatToEditText(mElectFenceFront, configValue.mFenceFront);
        setFloatToEditText(mElectFenceRight, configValue.mFenceRight);

        valBoolean = configValue.mRemoteControl;
        mSwitchRemoteControl.setChecked(valBoolean);
    }

    public void getConfigFromView(CarConfigValue configValue) {
        configValue.mBorderLeft = getFloatFromEditText(mBorderLeft);
        configValue.mBorderRight = getFloatFromEditText(mBorderRight);
        configValue.mBorderTop = getFloatFromEditText(mBorderTop);
        configValue.mBorderBottom = getFloatFromEditText(mBorderBottom);
        configValue.mBorderFront = getFloatFromEditText(mBorderFront);
        configValue.mBorderBack = getFloatFromEditText(mBorderBack);
        configValue.mGridWidth = getFloatFromEditText(mGridWidth);
        configValue.mMinDistance = getFloatFromEditText(mMinDistance);
        configValue.mMinAveDistance = getFloatFromEditText(mMinAveDistance);

        configValue.mSpeedRate = getIntFromTextView(mSpeedVal);

        configValue.mStartDelay = mSwitchStartDelay.isChecked();
        if (configValue.mStartDelay) {
            configValue.mDelayStart = CarConfigValue.DEFAULT_START_DELAY;
        } else {
            configValue.mDelayStart = 0;
        }

        configValue.mEnableFence = mSwitchElectFence.isChecked();
        configValue.mFenceFront = getFloatFromEditText(mElectFenceFront);
        configValue.mFenceRight = getFloatFromEditText(mElectFenceRight);

        configValue.mRemoteControl = mSwitchRemoteControl.isChecked();
    }

    private float setFloatToEditText(EditText editText, float val) {
        editText.setText(String.valueOf(val));
        return val;
    }

    private float getFloatFromEditText(EditText editText) {
        return Float.valueOf(editText.getText().toString());
    }

    private int setIntToTextView(TextView textView, int val) {
        textView.setText(String.valueOf(val));
        return val;
    }

    private int getIntFromTextView(TextView textView) {
        return Integer.valueOf(textView.getText().toString());
    }

}
