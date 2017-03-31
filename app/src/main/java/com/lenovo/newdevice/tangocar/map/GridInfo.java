package com.lenovo.newdevice.tangocar.map;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by liujk2 on 2016/12/19.
 */

public class GridInfo implements DataSerializable {
    public static float sGridWidth = 0.05F;

    public static final int TYPE_UNKNOWN = 1;
    public static final int TYPE_BORDER = 2;
    public static final int TYPE_INNER = 3;
    public static final int TYPE_RUNNED_PATH = 4;
    public static final int TYPE_EXPECTED_PATH = 5;

    private boolean mHasRealPoint;
    public double mTimestamp;
    public int mType;
    private int mRealPointCount;
    private int mPointCount;
    private float mMaxConfidence;
    private int mXIdx = 0;
    private int mYIdx = 0;
    private HashSet<FloatPoint3D> mPointData = new HashSet<FloatPoint3D>();
    public FloatPoint3D mCenterPoint = null;

    public GridInfo() {
    }

    public GridInfo(int x, int y) {
        mHasRealPoint = false;
        mRealPointCount = 0;
        mPointCount = 0;
        mXIdx = x;
        mYIdx = y;
        mType = TYPE_UNKNOWN;
        mMaxConfidence = 0;
    }

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void generatePointCloudCenterPoint() {
        if (mCenterPoint == null) {
            float x = (mXIdx * sGridWidth) + (sGridWidth / 2);
            float z = (mYIdx * sGridWidth) + (sGridWidth / 2);
            mCenterPoint = new FloatPoint3D(x, 0.0F, z);
        }
    }

    public void generateCenterPoint() {
        if (mCenterPoint == null) {
            float x = (mXIdx * sGridWidth) + (sGridWidth / 2);
            float y = (mYIdx * sGridWidth) + (sGridWidth / 2);
            mCenterPoint = new FloatPoint3D(x, y, 0.0F);
        }
    }

    public FloatPoint3D getCenterPoint() {
        return mCenterPoint;
    }

    public void addPointCount() {
        mPointCount ++;
    }

    public void addPoint3D(float x, float y, float z) {
        mPointData.add(new FloatPoint3D(x, y, z));
        mRealPointCount++;
    }

    public void addPoint3D(FloatPoint3D p) {
        mPointData.add(p);
        mRealPointCount++;
    }

    public void addConfidence(float confidence) {
        if (confidence > mMaxConfidence) {
            mMaxConfidence = confidence;
        }
    }

    public float getMaxConfidence() {
        return mMaxConfidence;
    }

    public void setHasRealPoint(boolean hasRealPoint) {
        mHasRealPoint = hasRealPoint;
    }

    public int getPointCount() {
        if (mHasRealPoint) {
            return mRealPointCount;
        } else {
            return mPointCount;
        }
    }

    public void clear() {
        mType = TYPE_UNKNOWN;
        mPointData.clear();
        mRealPointCount = 0;
        mMaxConfidence = 0;
        mPointCount = 0;
    }

    public FloatPoint3D[] getPoints() {
        FloatPoint3D[] points = new FloatPoint3D[mRealPointCount];
        int i = 0;
        for(FloatPoint3D point : mPointData) {
            points[i++] = point;
        }
        return points;
    }

    @Override
    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeInt(mXIdx);
        out.writeInt(mYIdx);
        out.writeInt(mType);
        out.writeDouble(mTimestamp);
        out.writeInt(mPointCount);
        out.writeBoolean(mHasRealPoint);
        out.writeInt(mRealPointCount);
        out.writeFloat(mMaxConfidence);
        SerializableUtils.writeObjectToData(mCenterPoint, out);
        /*for(FloatPoint3D point : mPointData) {
            Utils.writeObjectToData(point, out);
        }*/
    }

    @Override
    public void readFromDataInputStream(DataInputStream in) throws IOException {
        mXIdx = in.readInt();
        mYIdx = in.readInt();
        mType = in.readInt();
        mTimestamp = in.readDouble();
        mPointCount = in.readInt();
        mHasRealPoint = in.readBoolean();
        mRealPointCount = in.readInt();
        mMaxConfidence = in.readFloat();
        mCenterPoint = (FloatPoint3D) SerializableUtils.readObjectFromData(in);
        /*for (int i = 0; i < mRealPointCount; i ++) {
            FloatPoint3D point = (FloatPoint3D) Utils.readObjectFromData(in);
            mPointData.add(point);
        }*/
    }

    @Override
    public String toString() {
        return "GridInfo{" +
                "mXIdx=" + mXIdx +
                ", mYIdx=" + mYIdx +
                ", mType=" + mType +
                '}';
    }
}