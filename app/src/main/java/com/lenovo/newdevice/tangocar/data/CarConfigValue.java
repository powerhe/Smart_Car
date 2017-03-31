package com.lenovo.newdevice.tangocar.data;

import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.utils.AppConfig;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/2/22.
 */
public class CarConfigValue implements DataSerializable {
    public static final long DEFAULT_START_DELAY = 5000;

    public static final CarConfigValue DEFAULT;

    static {
        CarConfigValue defaultVal = new CarConfigValue();
        defaultVal.mBorderLeft = 0.15f;
        defaultVal.mBorderRight = 0.25f;
        defaultVal.mBorderTop = 0.15f;
        defaultVal.mBorderBottom = 0.18f;
        defaultVal.mBorderFront = 0.15f;
        defaultVal.mBorderBack =0.1f;
        defaultVal.mGridWidth = 0.05f;
        defaultVal.mMinDistance = 0.5f;
        defaultVal.mMinAveDistance = 0.65f;
        defaultVal.mSpeedRate = 30;
        defaultVal.mStartDelay = false;
        defaultVal.mDelayStart = 0;
        defaultVal.mEnableFence = false;
        defaultVal.mFenceFront = 2f;
        defaultVal.mFenceRight = 2f;
        defaultVal.mRemoteControl = false;
        DEFAULT = defaultVal;
    }

    private AppConfig mAppConfig;

    public float mBorderLeft;
    public float mBorderRight;
    public float mBorderTop;
    public float mBorderBottom;
    public float mBorderFront;
    public float mBorderBack;
    public float mGridWidth = 0.05F;
    public float mMinDistance;
    public float mMinAveDistance;
    public int mSpeedRate;
    public boolean mStartDelay;
    public long mDelayStart;
    public boolean mEnableFence;
    public float mFenceFront;
    public float mFenceRight;
    public boolean mRemoteControl;

    public CarConfigValue() {
        mAppConfig = AppConfig.getConfig();
    }

    public void update(CarConfigValue configValue) {
        if (configValue == this) {
            return;
        }
        mBorderLeft = configValue.mBorderLeft;
        mBorderRight = configValue.mBorderRight;
        mBorderTop = configValue.mBorderTop;
        mBorderBottom = configValue.mBorderBottom;
        mBorderFront = configValue.mBorderFront;
        mBorderBack = configValue.mBorderBack;
        mGridWidth = configValue.mGridWidth;
        mMinDistance = configValue.mMinDistance;
        mMinAveDistance = configValue.mMinAveDistance;
        mSpeedRate = configValue.mSpeedRate;
        mStartDelay = configValue.mStartDelay;
        mEnableFence = configValue.mEnableFence;
        mFenceFront = configValue.mFenceFront;
        mFenceRight = configValue.mFenceRight;
        mDelayStart = configValue.mDelayStart;
        mRemoteControl = configValue.mRemoteControl;
    }

    public void loadFromPreferences() {
        CarConfigValue defaultVal = DEFAULT;
        mBorderLeft = loadFloat(AppConfig.CFG_BORDER_LEFT, defaultVal.mBorderLeft);
        mBorderRight = loadFloat(AppConfig.CFG_BORDER_RIGHT, defaultVal.mBorderRight);
        mBorderTop = loadFloat(AppConfig.CFG_BORDER_TOP, defaultVal.mBorderTop);
        mBorderBottom = loadFloat(AppConfig.CFG_BORDER_BOTTOM, defaultVal.mBorderBottom);
        mBorderFront = loadFloat(AppConfig.CFG_BORDER_FRONT, defaultVal.mBorderFront);
        mBorderBack = loadFloat(AppConfig.CFG_BORDER_BACK, defaultVal.mBorderBack);
        mGridWidth = loadFloat(AppConfig.CFG_GRID_WIDTH, defaultVal.mGridWidth);
        mMinDistance = loadFloat(AppConfig.CFG_MIN_DISTANCE, defaultVal.mMinDistance);
        mMinAveDistance = loadFloat(AppConfig.CFG_MIN_AVE_DISTANCE, defaultVal.mMinAveDistance);

        int valInt = loadInteger(AppConfig.CFG_SPEED_RATE, defaultVal.mSpeedRate);
        if (valInt <= CarControl.SPEED_RATE_MAX && valInt >= CarControl.SPEED_RATE_MIN) {
            mSpeedRate = valInt;
        } else {
            mSpeedRate = defaultVal.mSpeedRate;
        }

        mStartDelay = loadBoolean(AppConfig.CFG_START_DELAY, defaultVal.mStartDelay);
        if (mStartDelay) {
            mDelayStart = CarConfigValue.DEFAULT_START_DELAY;
        } else {
            mDelayStart = 0;
        }

        mEnableFence = loadBoolean(AppConfig.CFG_FENCE_ENABLED, defaultVal.mEnableFence);
        mFenceFront = loadFloat(AppConfig.CFG_FENCE_FRONT, defaultVal.mFenceFront);
        mFenceRight = loadFloat(AppConfig.CFG_FENCE_RIGHT, defaultVal.mFenceRight);

        mRemoteControl = loadBoolean(AppConfig.CFG_REMOTE_CONTROL, defaultVal.mRemoteControl);
    }

    public void saveToPreferences() {
        saveFloat(AppConfig.CFG_BORDER_LEFT, mBorderLeft);
        saveFloat(AppConfig.CFG_BORDER_RIGHT, mBorderRight);
        saveFloat(AppConfig.CFG_BORDER_TOP, mBorderTop);
        saveFloat(AppConfig.CFG_BORDER_BOTTOM, mBorderBottom);
        saveFloat(AppConfig.CFG_BORDER_FRONT, mBorderFront);
        saveFloat(AppConfig.CFG_BORDER_BACK, mBorderBack);
        saveFloat(AppConfig.CFG_GRID_WIDTH, mGridWidth);
        saveFloat(AppConfig.CFG_MIN_DISTANCE, mMinDistance);
        saveFloat(AppConfig.CFG_MIN_AVE_DISTANCE, mMinAveDistance);

        saveInteger(AppConfig.CFG_SPEED_RATE, mSpeedRate);

        saveBoolean(AppConfig.CFG_START_DELAY, mStartDelay);

        saveBoolean(AppConfig.CFG_FENCE_ENABLED, mEnableFence);
        saveFloat(AppConfig.CFG_FENCE_FRONT, mFenceFront);
        saveFloat(AppConfig.CFG_FENCE_RIGHT, mFenceRight);

        saveBoolean(AppConfig.CFG_REMOTE_CONTROL, mRemoteControl);
    }

    private float loadFloat(String key, float def) {
        float val = mAppConfig.getFloat(key, def);
        return val;
    }

    private void saveFloat(String key, float val) {
        mAppConfig.putFloat(key, val);
    }

    private int loadInteger(String key, int def) {
        int val = mAppConfig.getInteger(key, def);
        return val;
    }

    private void saveInteger(String key, int val) {
        mAppConfig.putInteger(key, val);
    }

    private boolean loadBoolean(String key, boolean def) {
        boolean val = mAppConfig.getBoolean(key, def);
        return val;
    }

    private void saveBoolean(String key, boolean val) {
        mAppConfig.putBoolean(key, val);
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeFloat(mBorderLeft);
        out.writeFloat(mBorderRight);
        out.writeFloat(mBorderTop);
        out.writeFloat(mBorderBottom);
        out.writeFloat(mBorderFront);
        out.writeFloat(mBorderBack);
        out.writeFloat(mGridWidth);
        out.writeFloat(mMinDistance);
        out.writeFloat(mMinAveDistance);
        out.writeInt(mSpeedRate);
        out.writeBoolean(mStartDelay);
        out.writeBoolean(mEnableFence);
        out.writeFloat(mFenceFront);
        out.writeFloat(mFenceRight);
        out.writeLong(mDelayStart);
        out.writeBoolean(mRemoteControl);
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        mBorderLeft = in.readFloat();
        mBorderRight = in.readFloat();
        mBorderTop = in.readFloat();
        mBorderBottom = in.readFloat();
        mBorderFront = in.readFloat();
        mBorderBack = in.readFloat();
        mGridWidth = in.readFloat();
        mMinDistance = in.readFloat();
        mMinAveDistance = in.readFloat();
        mSpeedRate = in.readInt();
        mStartDelay = in.readBoolean();
        mEnableFence = in.readBoolean();
        mFenceFront = in.readFloat();
        mFenceRight = in.readFloat();
        mDelayStart = in.readLong();
        mRemoteControl = in.readBoolean();
    }
}
