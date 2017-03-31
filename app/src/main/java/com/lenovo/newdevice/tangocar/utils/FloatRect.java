package com.lenovo.newdevice.tangocar.utils;

/**
 * Created by liujk2 on 2016/11/21.
 */

public class FloatRect {
    public float mLeft;
    public float mTop;
    public float mRight;
    public float mBottom;

    public float mWidth;
    public float mHeight;

    public FloatRect(){
        mLeft = -0.20F;
        mTop = -0.20F;
        mRight = 0.20F;
        mBottom = 0.20F;
        calculateWidthHeight();
    }

    public FloatRect(float left, float top, float right, float bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        calculateWidthHeight();
    }

    private void calculateWidthHeight() {
        mWidth = mRight - mLeft;
        mHeight = mBottom - mTop;
    }
}
