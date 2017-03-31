package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.lang.Float.POSITIVE_INFINITY;

/**
 * Created by liujk2 on 2016/11/22.
 */

public class FloatPoint implements DataSerializable {
    public float x;
    public float y;

    public FloatPoint() {
        x = POSITIVE_INFINITY;
        y = POSITIVE_INFINITY;
    }

    public FloatPoint(Point point) {
        x = point.x;
        y = point.y;
    }

    public FloatPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Point toPoint() {
        return new Point((int)x, (int)y);
    }

    public void update(FloatPoint point) {
        this.x = point.x;
        this.y = point.y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        x = in.readFloat();
        y = in.readFloat();
    }
}
