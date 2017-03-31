package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by liujk2 on 2016/12/27.
 */

public class MapScope {
    public boolean isRect;
    public Point mCenterPos;
    public Rect mScopeRect;
    public MapScope(Point centerPos, int width, int height) {
        isRect = true;
        mCenterPos = centerPos;
        mScopeRect = new Rect();
        mScopeRect.left = centerPos.x - width / 2;
        mScopeRect.top = centerPos.y - height / 2;
        mScopeRect.right = mScopeRect.left + width;
        mScopeRect.bottom = mScopeRect.top + height;
    }

    public MapScope(Rect rect) {
        isRect = true;
        mScopeRect = rect;
    }
}
