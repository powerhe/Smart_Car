package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;

/**
 * Created by liujk2 on 2016/12/20.
 */

public interface TraversorByGrid {
    public void traverseBegin();
    public void traverseGrid(Point pointIdx, GridInfo gridInfo);
    public void traverseEnd();
}
