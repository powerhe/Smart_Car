package com.lenovo.newdevice.tangocar.utils;

import android.util.Log;

import java.lang.reflect.Field;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/2/3.
 */

public class BeanUtils {
    public static Object getFieldValue(Object obj, String name) {
        Class cls = obj.getClass();
        try {
            Field fd = cls.getField(name);
            return fd.get(obj);
        } catch (Exception e) {
            Log.e(TAG, "Get field error:" + e);
        }
        return null;
    }

    public static void setFieldValue(Object obj, String name, Object field) {
        Class cls = obj.getClass();
        try {
            Field fd = cls.getField(name);
            fd.set(obj, field);
        } catch (Exception e) {
            Log.e(TAG, "Set field error:" + e);
        }
    }
}
