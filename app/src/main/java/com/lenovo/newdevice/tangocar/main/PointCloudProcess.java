package com.lenovo.newdevice.tangocar.main;

import android.util.Log;

import com.lenovo.newdevice.tangocar.MainActivity;
import com.lenovo.newdevice.tangocar.data.CarConfigValue;
import com.lenovo.newdevice.tangocar.data.FrontDistanceInfo;
import com.lenovo.newdevice.tangocar.utils.FloatRect;

import java.nio.FloatBuffer;

import static java.lang.Float.POSITIVE_INFINITY;

/**
 * Created by liujk2 on 2016/11/21.
 */

public class PointCloudProcess {
    private static final String TAG = MainActivity.TAG;
    private static final float DISTANCE_INFINITE = POSITIVE_INFINITY;

    private boolean initialized = false;

    private FloatRect mBorder;
    private float mGridWidth;
    private int mGridLineWidth;
    private int mGridLineCount;
    private int[][] mGridPointCount;
    private float[][] mGridMaxConfidence;
    private float[][] mGridTotalZ;
    private float[][] mGridAverageZ;

    private static FrontDistanceInfo sDistanceInfo;
    private static Object sDistanceLock;

    static {
        sDistanceInfo = new FrontDistanceInfo();
        sDistanceLock = new Object();
    }

    public PointCloudProcess() {
        mGridWidth = 0.05F;
        mBorder = new FloatRect();
        mGridLineWidth = 0;
        mGridLineCount = 0;
    }

    synchronized public void applyConfig(CarConfigValue configValue) {
        mBorder = new FloatRect(0 - configValue.mBorderRight,
                0 - configValue.mBorderBottom,
                configValue.mBorderLeft,
                configValue.mBorderRight);
        mGridWidth = configValue.mGridWidth;
        setupGridInfo(false);
    }

    synchronized public void setBorder(float left, float top, float right, float bottom) {
        mBorder = new FloatRect(left, top, right,bottom);
        setupGridInfo(false);
    }

    synchronized public void setGridWidth(float width) {
        mGridWidth = width;
        setupGridInfo(false);
    }

    synchronized private void resetGridInfo() {
        for (int i = 0; i < mGridLineWidth; i++) {
            for (int j = 0; j < mGridLineCount; j++) {
                mGridPointCount[i][j] = 0;
                mGridMaxConfidence[i][j] = 0.0f;
                mGridTotalZ[i][j] = 0.0f;
                mGridAverageZ[i][j] = POSITIVE_INFINITY;
            }
        }
    }

    synchronized private void setupGridInfo(boolean init) {
        int gridLineWidth = (int)(mBorder.mWidth/mGridWidth) + 1;
        int gridLineCount = (int)(mBorder.mHeight/mGridWidth) + 1;
        if (gridLineCount < 0 || gridLineCount < 0) {
            Log.e(TAG, "setupGridInfo error:", new RuntimeException("(gridLineWidth,gridLineCount) is(" + gridLineWidth + "," + gridLineCount + ")"));
            return;
        }
        if (init || (gridLineWidth != mGridLineWidth || gridLineCount != mGridLineCount)) {
            initialized = true;
            mGridLineWidth = gridLineWidth;
            mGridLineCount = gridLineCount;
            mGridPointCount = new int[gridLineWidth][gridLineCount];
            mGridMaxConfidence = new float[gridLineWidth][gridLineCount];
            mGridTotalZ = new float[gridLineWidth][gridLineCount];
            mGridAverageZ = new float[gridLineWidth][gridLineCount];
            resetGridInfo();
        }
    }

    synchronized public void update(FloatBuffer pointCloudBuffer, int numPoints) {
        if (numPoints == 0) {
            return;
        }
        if (!initialized) {
            setupGridInfo(true);
        }
        resetGridInfo();

        float[] points = new float[numPoints * 4];
        pointCloudBuffer.rewind();
        pointCloudBuffer.get(points);
        pointCloudBuffer.rewind();

        int validPointCount = 0;
        int numFloats = 4 * numPoints;
        for (int i = 0; i < numFloats; i = i + 4) {
            float x = points[i];
            float y = points[i + 1];
            float z = points[i + 2];
            float c = points[i + 3];
            if (//c > 0.5 &&
                    x < mBorder.mRight && x >= mBorder.mLeft
                            && y < mBorder.mBottom && y >= mBorder.mTop) {
                int gridX = (int)((x - mBorder.mLeft) / mGridWidth);
                int gridY = (int)((y - mBorder.mTop) / mGridWidth);
                //Log.i(TAG, "got point (" + x + ", " + y + "," + z + "), gridXY is (" + gridX + "," + gridY + ")");
                if (c > mGridMaxConfidence[gridX][gridY]) {
                    mGridMaxConfidence[gridX][gridY] = c;
                }
                mGridPointCount[gridX][gridY] ++;
                mGridTotalZ[gridX][gridY] += z;
                //Log.i(TAG, "Grid[" + gridX + "][" + gridY + "] TotalZ is " + mGridTotalZ[gridX][gridY] + ",Count = " + mGridPointCount[gridX][gridY]);
                validPointCount ++;
            }
        }
        float minAverageZ = POSITIVE_INFINITY;
        float totalAverageZ = POSITIVE_INFINITY;
        float totalZ = 0;
        int validGridCount = 0;
        int nearestXIndex = 0;
        int nearestYIndex = 0;
        for (int i = 0; i < mGridLineWidth; i++) {
            for (int j = 0; j < mGridLineCount; j++) {
                int count = mGridPointCount[i][j];
                float maxConfidence = mGridMaxConfidence[i][j];
                if ((count < 3 && maxConfidence < 0.5f)
                    || (count < 5 && maxConfidence < 0.3f)) {
                    continue;
                }
                if (count > 0) {
                    validGridCount ++;
                    mGridAverageZ[i][j] = mGridTotalZ[i][j] / mGridPointCount[i][j];
                    //Log.i(TAG, "Grid[" + i + "][" + j + "] TotalZ is " + mGridTotalZ[i][j] + ",Count = " + mGridPointCount[i][j] + ",AverageZ = " + mGridAverageZ[i][j]);
                    totalZ += mGridAverageZ[i][j];
                    if (mGridAverageZ[i][j] < minAverageZ) {
                        minAverageZ = mGridAverageZ[i][j];
                        nearestXIndex = i;
                        nearestYIndex = j;
                    }
                    mGridTotalZ[i][j] = 0.0f;
                    mGridAverageZ[i][j] = POSITIVE_INFINITY;
                }
            }
        }
        totalAverageZ = totalZ / validGridCount;
        synchronized (sDistanceLock) {
            sDistanceInfo.reset();
            sDistanceInfo.validPointCount = validPointCount;
            sDistanceInfo.validGridCount = validGridCount;
            sDistanceInfo.minDistance = minAverageZ;
            sDistanceInfo.aveDistance = totalAverageZ;
            sDistanceInfo.nearestGridX = nearestXIndex - mGridLineWidth/2;
            sDistanceInfo.nearestGridY = nearestYIndex - mGridLineCount/2;
            sDistanceInfo.valid = (validPointCount >= 200);
        }
    }

    public static FrontDistanceInfo getDistanceInfo() {
        synchronized (sDistanceLock) {
            return sDistanceInfo.clone();
        }
    }

}
