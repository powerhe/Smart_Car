package com.lenovo.newdevice.tangocar.map;

/**
 * Created by liujk2 on 2016/12/20.
 */

public interface TraversorByPoint3D {
    public void traverseBegin();
    public void traversePoint(FloatPoint3D point, int type, double timestamp);
    public void traverseEnd();
}
