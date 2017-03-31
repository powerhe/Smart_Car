package com.lenovo.newdevice.tangocar.map;

/**
 * Created by liujk2 on 2016/12/19.
 */

import android.graphics.Point;
import android.util.Log;

import java.nio.FloatBuffer;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

public class PointCloudGrid extends GridMap {
    private static float sFarZ = 4.0F;
    private static float sNearZ = 0.4F;
    private static float sNearAvaZ = 0.25F;
    private static double sMaxAngleTan = 0.5773502692;

    private static float sTopY = 0.1F;
    private static float sBottomY = -0.25F;

    private static int[] sMaxXGridCountArray;
    private static Point[][] sXArrayByAngle;

    private static int sFarZGridCount;
    private static int sNearZGridCount;
    private static int sAnglesCount = 30 + 1 + 30;

    private static boolean sAddPoint;

    private static double[] sTan0_30 = {
            0,
            0.017455064928217585, //tan1
            0.03492076949174773, //tan2
            0.052407779283041196, //tan3
            0.06992681194351041, //tan4
            0.08748866352592401, //tan5
            0.10510423526567646, //tan6
            0.1227845609029046, //tan7
            0.14054083470239145, //tan8
            0.15838444032453627, //tan9
            0.17632698070846497, //tan10
            0.19438030913771848, //tan11
            0.2125565616700221, //tan12
            0.2308681911255631, //tan13
            0.24932800284318068, //tan14
            0.2679491924311227, //tan15
            0.2867453857588079, //tan16
            0.30573068145866033, //tan17
            0.3249196962329063, //tan18
            0.34432761328966527, //tan19
            0.36397023426620234, //tan20
            0.3838640350354158, //tan21
            0.4040262258351568, //tan22
            0.4244748162096047, //tan23
            0.4452286853085361, //tan24
            0.4663076581549986, //tan25
            0.4877325885658614, //tan26
            0.5095254494944288, //tan27
            0.5317094316614788, //tan28
            0.554309051452769, //tan29
            0.5773502691896257, //tan30
    };

    private int mRealPointCount;
    public double mTimestamp;
    private int[] mMinZOfAngle;

    static {
        sAddPoint = true;
        sMaxAngleTan = sTan0_30[28];
        setGridWidth(GridInfo.sGridWidth);
        initBaseData();
    }

    public static boolean getAddPoint() {
        return sAddPoint;
    }

    public PointCloudGrid(double timestamp) {
        super();
        mRealPointCount = 0;
        mTimestamp = timestamp;
        mMinZOfAngle = new int[sAnglesCount];
    }

    public static void setGridWidth(float gridWidth) {
        if (sGridWidth != gridWidth) {
            GridMap.setGridWidth(gridWidth);
            initBaseData();
        }
    }

    private static void initBaseData() {
        sFarZGridCount = (int) (sFarZ / sGridWidth);
        sNearZGridCount = (int) (sNearZ / sGridWidth);
        initGridCountArray();
        initXArrayByAngle();
    }

    private static void initGridCountArray() {
        sMaxXGridCountArray = new int[sFarZGridCount];
        int maxXGridCount;
        for (int zIdx = sNearZGridCount; zIdx < sFarZGridCount; zIdx ++) {
            maxXGridCount = (int)(zIdx * sMaxAngleTan);
            sMaxXGridCountArray[zIdx] = maxXGridCount;
            //Log.i(TAG, "Max X of Line("+zIdx+") is "+maxXGridCount);
        }
    }

    private static void initXArrayByAngle() {
        sXArrayByAngle = new Point[sAnglesCount][sFarZGridCount];
        for (int angle = 0; angle < sAnglesCount; angle ++) {
            int angleA = angle - 30;
            double tanA = tan(angleA);
            double tanB = tan(angleA + 1);
            for (int zIdx = sNearZGridCount; zIdx < sFarZGridCount; zIdx ++) {
                float z = zIdx * sGridWidth + sGridWidth / 2;
                float xA = (float ) (z * tanA);
                float xB = (float ) (z * tanB);
                int xIdxA = (int) (xA / sGridWidth);
                int xIdxB = (int) (xB / sGridWidth);
                sXArrayByAngle[angle][zIdx] = new Point(xIdxA, xIdxB);
            }
        }
    }

    public static double tan(int angle) {
        if (angle < -30 || angle > 30) {
            return 0;
        }
        if (angle < 0) {
            return (0 - sTan0_30[0 - angle]);
        } else {
            return sTan0_30[angle];
        }
    }

    public static int arcTan(float tan) {
        if (tan == 0) {
            return 0;
        }
        int val = 90;
        boolean negative = false;
        if (tan < 0) {
            negative = true;
            tan = 0 - tan;
        }
        if (tan <= (0 - sTan0_30[30]) || tan > sTan0_30[30]) {
            return 90;
        }
        for (int i = 1; i <= 30; i ++) {
            if (tan <= sTan0_30[i] && tan > sTan0_30[i - 1]) {
                val = i;
            }
        }
        if (negative) {
            val = 0 - val;
        }
        return val;
    }

    private void updateMaxZOfAngle(float x, float z, Point index) {
        float tanA = x/z;
        int angle = arcTan(tanA) + 30;
        if (angle >= sAnglesCount) {
            return;
        }
        int minZ = mMinZOfAngle[angle];
        if (minZ == 0 || index.y < minZ) {
            mMinZOfAngle[angle] = index.y;
        }
    }

    private void initTriangleMap() {
        int maxXGridCount;
        for (int zIdx = sNearZGridCount; zIdx < sFarZGridCount; zIdx ++) {
            maxXGridCount = sMaxXGridCountArray[zIdx];
            for (int xIdx = -maxXGridCount; xIdx < maxXGridCount; xIdx ++) {
                adjustScope(xIdx, zIdx);
                GridInfo gridInfo = new GridInfo(xIdx, zIdx);
                gridInfo.setType(GridInfo.TYPE_UNKNOWN);
                mGridData.put(new Point(xIdx, zIdx), gridInfo);
            }
        }
    }

    private void removeUnknownGrid(int xIdx, int zIdx) {
        GridInfo gridInfo = getGridInfo(xIdx, zIdx, false);
        if (gridInfo != null) {
            if (gridInfo.getType() == GridInfo.TYPE_UNKNOWN) {
                removeGridInfo(xIdx, zIdx);
            }
        }
    }

    private void procReservedGrid(int xIdx, int zIdx) {
        GridInfo gridInfo = getGridInfo(xIdx, zIdx, false);
        if (gridInfo != null) {
            int type = gridInfo.getType();
            if (type == GridInfo.TYPE_UNKNOWN) {
                gridInfo.setType(GridInfo.TYPE_INNER);
                gridInfo.generatePointCloudCenterPoint();
            } else if (type == GridInfo.TYPE_BORDER) {
                //Utils.getInstance().outLog("Grid["+xIdx+"]["+zIdx+"]: c("+gridInfo.getMaxConfidence()+"), C("+gridInfo.getPointCount()+")");
                if ((gridInfo.getMaxConfidence() < 0.3 && gridInfo.getPointCount() < 5)
                        || (gridInfo.getMaxConfidence() < 0.5 && gridInfo.getPointCount() < 3)) {
                    removeGridInfo(xIdx, zIdx);
                }
            }
        }
    }

    private void computeInner() {
        int maxXGridCount;
        for (int angle = 0; angle < sAnglesCount; angle ++) {
            int minZ = mMinZOfAngle[angle];
            if (minZ != 0) {
                for (int zIdx = minZ; zIdx < sFarZGridCount; zIdx++) {
                    Point xIdxs = sXArrayByAngle[angle][zIdx];
                    if (xIdxs != null) {
                        int xIdxA = xIdxs.x;
                        int xIdxB = xIdxs.y;
                        if (xIdxA != xIdxB) {
                            for (int xIdx = xIdxA; xIdx <= xIdxB; xIdx++) {
                                removeUnknownGrid(xIdx, zIdx);
                            }
                        } else {
                            removeUnknownGrid(xIdxA, zIdx);
                        }
                    }
                }
            }
        }
        for (int zIdx = sNearZGridCount; zIdx < sFarZGridCount; zIdx ++) {
            maxXGridCount = sMaxXGridCountArray[zIdx];
            for (int xIdx = -maxXGridCount; xIdx < maxXGridCount; xIdx ++) {
                procReservedGrid(xIdx, zIdx);
            }
        }
    }

    public void traverseGridByOrder(TraversorByGrid traversor) {
        if (traversor == null) return;
        traversor.traverseBegin();
        int maxXGridCount;
        for (int zIdx = sNearZGridCount; zIdx < sFarZGridCount; zIdx ++) {
            maxXGridCount = sMaxXGridCountArray[zIdx];
            for (int xIdx = -maxXGridCount; xIdx < maxXGridCount; xIdx++) {
                Point pointIndex = new Point(xIdx, zIdx);
                GridInfo gridInfo = mGridData.get(pointIndex);
                if (gridInfo == null) {
                    continue;
                }
                traversor.traverseGrid(pointIndex, gridInfo);
            }
        }
        traversor.traverseEnd();
    }

    public static void setVerticalScope(float top, float bottom) {
        sTopY = top;
        sBottomY = bottom;
    }

    public static PointCloudGrid getGridFromPointCloud(double timestamp, FloatBuffer pointCloudBuffer, int numPoints) {
        if (numPoints == 0) {
            return null;
        }
        float[] points = new float[numPoints * 4];
        pointCloudBuffer.rewind();
        pointCloudBuffer.get(points);
        pointCloudBuffer.rewind();

        int numFloats = 4 * numPoints;
        float totalZ = 0.0F;
        float averageZ = 0.0F;
        for (int i = 0; i < numFloats; i = i + 4) {
            totalZ = totalZ + points[i + 2];
        }
        averageZ = totalZ / numPoints;
        //Log.i(TAG, "Average Z of point cloud is " + averageZ + ".");
        if (averageZ < sNearAvaZ) {
            Log.i(TAG, "Too close to object, average Z is " + averageZ + ".");
            return null;
        }
        PointCloudGrid pointCloudGrid = new PointCloudGrid(timestamp);
        pointCloudGrid.initTriangleMap();

        for (int i = 0; i < numFloats; i = i + 4) {
            float x = points[i];
            float y = points[i + 1];
            float z = points[i + 2];
            float c = points[i + 3];
            if (//c < 0.5 ||
                    y < sBottomY || y > sTopY) {
                continue;
            }
            Point index = GridMap.getGridIndex(x, z);
            pointCloudGrid.updateMaxZOfAngle(x, z, index);
            GridInfo gridInfo = pointCloudGrid.getGridInfo(index, false);
            if (gridInfo != null) {
                if (gridInfo.getType() == GridInfo.TYPE_UNKNOWN) {
                    gridInfo.setType(GridInfo.TYPE_BORDER);
                    gridInfo.mTimestamp = timestamp;
                    if (!sAddPoint) {
                        gridInfo.generatePointCloudCenterPoint();
                    } else {
                        gridInfo.setHasRealPoint(true);
                    }
                }
                gridInfo.addConfidence(c);
                if (sAddPoint) {
                    gridInfo.addPoint3D(x, y, z);
                    pointCloudGrid.mRealPointCount++;
                } else {
                    gridInfo.addPointCount();
                }
            } else {
                //Log.i(TAG, "Can't get grid info for ("+x+","+y+","+z+").");
            }
        }

        //Utils.getInstance().outLog("compute inner for PointCloudGrid-" + timestamp);
        pointCloudGrid.computeInner();
        //Utils.getInstance().outLog("compute inner over");
        //Utils.getInstance().outLog("X:"+pointCloudGrid.mRectScope.left+","+pointCloudGrid.mRectScope.right+"; Y:"+pointCloudGrid.mRectScope.top+","+pointCloudGrid.mRectScope.bottom);
        //Utils.getInstance().outputMapToPgmFile("PointCloudGrid-" + timestamp + ".pgm", pointCloudGrid);

        return pointCloudGrid;
    }

}
