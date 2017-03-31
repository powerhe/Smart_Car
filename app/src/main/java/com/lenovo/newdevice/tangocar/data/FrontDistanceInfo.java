package com.lenovo.newdevice.tangocar.data;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.MathUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/2/22.
 */
public class FrontDistanceInfo implements DataSerializable {
    public boolean valid = false;
    public float minDistance;
    public float aveDistance;

    public int nearestGridX = 0;
    public int nearestGridY = 0;
    public int validPointCount = 0;
    public int validGridCount = 0;

    public void reset() {
        valid = false;
        minDistance = 0;
        aveDistance = 0;

        nearestGridX = 0;
        nearestGridY = 0;
        validPointCount = 0;
        validGridCount = 0;
    }

    public FrontDistanceInfo clone() {
        FrontDistanceInfo distanceInfo = new FrontDistanceInfo();

        distanceInfo.valid = valid;
        distanceInfo.minDistance = minDistance;
        distanceInfo.aveDistance = aveDistance;
        distanceInfo.nearestGridX = nearestGridX;
        distanceInfo.nearestGridY = nearestGridY;
        distanceInfo.validPointCount = validPointCount;
        distanceInfo.validGridCount = validGridCount;

        return distanceInfo;
    }

    public String toString() {
        return "Min Grid Z: " + MathUtils.FORMAT_THREE_DECIMAL.format(minDistance)
                + "\nAve Grid Z: " + MathUtils.FORMAT_THREE_DECIMAL.format(aveDistance)
                + "\nMinZ index(" + nearestGridX + "," + nearestGridY + ")"
                + "\npoint count :" + validPointCount + ", grid count :" + validGridCount;
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeBoolean(valid);
        out.writeFloat(minDistance);
        out.writeFloat(aveDistance);
        out.writeInt(nearestGridX);
        out.writeInt(nearestGridY);
        out.writeInt(validPointCount);
        out.writeInt(validGridCount);
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        valid = in.readBoolean();
        minDistance = in.readFloat();
        aveDistance = in.readFloat();
        nearestGridX = in.readInt();
        nearestGridY = in.readInt();
        validPointCount = in.readInt();
        validGridCount = in.readInt();
    }
}
