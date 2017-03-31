package com.lenovo.newdevice.tangocar.map;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.lang.Float.POSITIVE_INFINITY;

/**
 * Created by liujk2 on 2016/11/22.
 */

public class FloatPoint3D implements DataSerializable {
    public static final FloatPoint3D ORIGIN_POINT = new FloatPoint3D(0, 0, 0);
    public float x;
    public float y;
    public float z;

    public FloatPoint3D() {
        x = POSITIVE_INFINITY;
        y = POSITIVE_INFINITY;
        z = POSITIVE_INFINITY;
    }

    public FloatPoint3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FloatPoint3D) {
            FloatPoint3D p = (FloatPoint3D)o;
            if (this.x == p.x && this.y == p.y && this.z == p.z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
    }

    @Override
    public void readFromDataInputStream(DataInputStream in) throws IOException {
        x = in.readFloat();
        y = in.readFloat();
        z = in.readFloat();
    }
}
