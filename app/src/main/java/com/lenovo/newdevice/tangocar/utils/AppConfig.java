package com.lenovo.newdevice.tangocar.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by liujk2 on 2016/11/21.
 */

public class AppConfig {
    private final static String SHARED_PREFERENCE = "save_config";

    public static final String CFG_BORDER_LEFT = "border_left";
    public static final String CFG_BORDER_RIGHT = "border_right";
    public static final String CFG_BORDER_TOP = "border_top";
    public static final String CFG_BORDER_BOTTOM = "border_bottom";
    public static final String CFG_BORDER_FRONT = "border_front";
    public static final String CFG_BORDER_BACK = "border_back";
    public static final String CFG_GRID_WIDTH = "grid_width";
    public static final String CFG_MIN_DISTANCE = "min_distance";
    public static final String CFG_MIN_AVE_DISTANCE = "min_ave_distance";
    public static final String CFG_SPEED_RATE = "speed_rate";
    public static final String CFG_START_DELAY = "start_delay";
    public static final String CFG_FENCE_ENABLED = "fence_enabled";
    public static final String CFG_FENCE_FRONT = "fence_front";
    public static final String CFG_FENCE_RIGHT = "fence_right";
    public static final String CFG_FORWARD_SPEED_MSG = "forward_speed_msg";
    public static final String CFG_TURN_SPEED_MSG = "turn_speed_msg";
    public static final String CFG_LAST_MAP_NAME = "last_map_name";
    public static final String CFG_AP_NAME = "ap_name";
    public static final String CFG_CONTROLLER_NAME = "controller_name";
    public static final String CFG_REMOTE_CONTROL = "remote_control";

    private static AppConfig sConfig = null;
    private static Context sContext;

    private SharedPreferences mPreferences;

    public static void init(Context context) {
        sContext = context;
        if (sConfig == null) {
            sConfig = new AppConfig();
        }
        sConfig.reset();
    }

    public static AppConfig getConfig() {
        return sConfig;
    }

    private void reset() {
        mPreferences = sContext.getSharedPreferences(SHARED_PREFERENCE, 0);
    }

    public float getFloat(String key, float def) {
        return mPreferences.getFloat(key, def);
    }

    public void putFloat(String key, float val) {
        SharedPreferences.Editor ed = mPreferences.edit();
        ed.putFloat(key, val);
        ed.commit();
    }

    public boolean getBoolean(String key, boolean def) {
        return mPreferences.getBoolean(key, def);
    }

    public void putBoolean(String key, boolean val) {
        SharedPreferences.Editor ed = mPreferences.edit();
        ed.putBoolean(key, val);
        ed.commit();
    }

    public int getInteger(String key, int def) {
        return mPreferences.getInt(key, def);
    }

    public void putInteger(String key, int val) {
        SharedPreferences.Editor ed = mPreferences.edit();
        ed.putInt(key, val);
        ed.commit();
    }

    public String getString(String key, String def) {
        return mPreferences.getString(key, def);
    }

    public void putString(String key, String val) {
        SharedPreferences.Editor ed = mPreferences.edit();
        ed.putString(key, val);
        ed.commit();
    }

}
