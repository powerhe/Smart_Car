package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.atap.tangoservice.TangoPoseData;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.projecttango.tangosupport.TangoSupport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by liujk2 on 2016/11/21.
 */

public class WorldMapGrid extends GridMap {
    private static boolean sAddPoint;
    static {
        sAddPoint = false;
    }

    private static float sTopY = 0.1F;
    private static float sBottomY = -0.25F;

    // this border used for compute known grid, here we use big grid
    private static final int BIG_GRID_SIZE = 10;
    private Rect mInnerBorder;
    private HashSet<Point> mInnerUnknown;
    private Point mRecentUnknownGrid;
    private Object mUnknownGridLock;

    private Rect mMaxScope;

    public WorldMapGrid() {
        super();
        mInnerBorder = new Rect(0, 0, 0, 0);
        mMaxScope = new Rect(0, 0, 0, 0);
        mInnerUnknown = new HashSet<Point>();
        mUnknownGridLock = new Object();
    }

    @Override
    public void clear() {
        super.clear();
        synchronized (mScopeLock) {
            mInnerBorder = new Rect(0, 0, 0, 0);
            mMaxScope = new Rect(0, 0, 0, 0);
        }
        synchronized (mUnknownGridLock) {
            mInnerUnknown.clear();
        }
    }

    public static void setVerticalScope(float top, float bottom) {
        sTopY = top;
        sBottomY = bottom;
    }

    @Override
    public Rect getScope() {
        synchronized (mScopeLock) {
            return mMaxScope;
        }
    }

    @Override
    protected void onCreateGridInfo(Point index) {
        Point bigGridIndex = new Point(index.x / BIG_GRID_SIZE, index.y / BIG_GRID_SIZE);
        boolean grow = false;
        Rect newBorder = null;

        synchronized (mScopeLock) {
            newBorder = new Rect(mInnerBorder);
        }

        if (bigGridIndex.x - 1 < mInnerBorder.left) {
            newBorder.left = bigGridIndex.x - 1;
            grow = true;
        }
        if (bigGridIndex.x + 1 > mInnerBorder.right) {
            newBorder.right = bigGridIndex.x + 1;
            grow = true;
        }
        if (bigGridIndex.y - 1 < mInnerBorder.top) {
            newBorder.top = bigGridIndex.y - 1;
            grow = true;
        }
        if (bigGridIndex.y + 1 > mInnerBorder.bottom) {
            newBorder.bottom = bigGridIndex.y + 1;
            grow = true;
        }

        if (grow) {
            synchronized (mScopeLock) {
                mInnerBorder = newBorder;
                mMaxScope.left = mInnerBorder.left * BIG_GRID_SIZE;
                mMaxScope.right = mInnerBorder.right * BIG_GRID_SIZE;
                mMaxScope.top = mInnerBorder.top * BIG_GRID_SIZE;
                mMaxScope.bottom = mInnerBorder.bottom * BIG_GRID_SIZE;
            }
        }

        synchronized (mUnknownGridLock) {
            if (grow) {
                grow(newBorder);
            }
            mInnerUnknown.remove(bigGridIndex);
        }
    }

    private void grow(Rect newBorder) {
        synchronized (mUnknownGridLock) {
            for (int i = newBorder.left; i <= newBorder.right; i++) {
                for (int j = newBorder.top; j <= newBorder.bottom; j++) {
                    if (!mInnerUnknown.add(new Point(i, j))) {
                    }
                }
            }
        }
    }

     public boolean removeRecentUnknownGrid() {
        synchronized (mUnknownGridLock) {
            if (mRecentUnknownGrid != null) {
                mInnerUnknown.remove(mRecentUnknownGrid);
                return true;
            } else {
                return false;
            }
        }
    }

    public Point getOneUnknownGrid(Point currentPose) {
        Point unknownGrid = null;
        synchronized (mUnknownGridLock) {
            Point bigGridIndex = null;
            Iterator it = mInnerUnknown.iterator();
            if (it.hasNext()) {
                bigGridIndex = (Point) it.next();
                mRecentUnknownGrid = bigGridIndex;
            } else {
                mRecentUnknownGrid = null;
                if (currentPose != null) {
                    bigGridIndex = new Point(currentPose.x / BIG_GRID_SIZE, (currentPose.y / BIG_GRID_SIZE) + 1);
                }
            }
            if (bigGridIndex != null) {
                unknownGrid = new Point(bigGridIndex.x * BIG_GRID_SIZE, bigGridIndex.y * BIG_GRID_SIZE);
            }
        }
        return unknownGrid;
    }

    synchronized private boolean procPointInfo(GridInfo gridInfo, int type, double timestamp) {
        boolean addPoint = true;
        int oldType = gridInfo.getType();
        if (oldType == GridInfo.TYPE_UNKNOWN) {
            gridInfo.setType(type);
        } else {
            if (type == GridInfo.TYPE_BORDER) {
                if (oldType == GridInfo.TYPE_INNER) {
                    gridInfo.clear();
                }
                gridInfo.setType(type);
            }
            if (type == GridInfo.TYPE_INNER) {
                if (oldType == GridInfo.TYPE_BORDER) {
                    addPoint = false;
                }
            }
        }
        gridInfo.mTimestamp = timestamp;
        return addPoint;
    }

    synchronized public void procPointInfoNotAddPoint(float[] point, int type, double timestamp) {
        GridInfo gridInfo = getGridInfo(point[0], point[1], true);
        boolean addPoint = procPointInfo(gridInfo, type, timestamp);
        //gridInfo.generateCenterPoint();
        if (addPoint) {
            gridInfo.addPointCount();
        }
    }

    synchronized public void procPointInfoWithAddPoint(float[] point, int type, double timestamp) {
        GridInfo gridInfo = getGridInfo(point[0], point[1], true);
        boolean addPoint = procPointInfo(gridInfo, type, timestamp);
        gridInfo.setHasRealPoint(true);
        if (addPoint) {
            gridInfo.addPoint3D(point[0], point[1], point[2]);
        }
    }

    class DeltaMapGenerator implements TraversorByPoint3D {
        private float[] matrixTransform;
        float[] mPoint;
        private float mZScopeMax;
        private float mZScopeMin;
        protected WorldMapGrid mMap;
        public DeltaMapGenerator(TangoSupport.TangoMatrixTransformData transform, float currentZ) {
            mPoint = new float[3];
            matrixTransform = transform.matrix;
            mZScopeMax = currentZ + sTopY;
            mZScopeMin = currentZ + sBottomY + 0.08F;
            mMap = new WorldMapGrid();
        }

        @Override
        public void traverseBegin() {
        }

        @Override
        public void traversePoint(FloatPoint3D point, int type, double timestamp) {
            mPoint[0] = point.x;
            mPoint[1] = point.y;
            mPoint[2] = point.z;
            float[] newPoint = TangoSupport.transformPoint(matrixTransform, mPoint);
            float z = newPoint[2];
            if (z < mZScopeMax && z > mZScopeMin) {
                procPoint(newPoint, type, timestamp);
            }
        }

        @Override
        public void traverseEnd() {
            //throw new RuntimeException("test over");
        }

        protected void procPoint(float[] point, int type, double timestamp) {
            mMap.procPointInfoWithAddPoint(point, type, timestamp);
        }

        protected WorldMapGrid getMap() {
            return mMap;
        }
    }

    class DeltaMapGeneratorNotAddPoint extends DeltaMapGenerator {
        public DeltaMapGeneratorNotAddPoint(TangoSupport.TangoMatrixTransformData transform, float currentZ) {
            super(transform, currentZ);
        }

        @Override
        protected void procPoint(float[] point, int type, double timestamp) {
            mMap.procPointInfoNotAddPoint(point, type, timestamp);
        }
    }

    synchronized public WorldMapGrid getDeltaMap(PointCloudGrid pointCloudGrid, TangoSupport.TangoMatrixTransformData transform, float currentZ) {
        if (pointCloudGrid == null) {
            return null;
        }
        DeltaMapGenerator generator;
        if (sAddPoint) {
            generator = new DeltaMapGenerator(transform, currentZ);
        } else {
            generator = new DeltaMapGeneratorNotAddPoint(transform, currentZ);
        }
        if (PointCloudGrid.getAddPoint()) {
            pointCloudGrid.traversePoints(generator);
        } else {
            pointCloudGrid.traverseCenterPoints(generator);
        }
        return generator.getMap();
    }

    synchronized public void update(WorldMapGrid worldMapGrid) {
        if (worldMapGrid == null) {
            return;
        }

        worldMapGrid.traverseGrid(new TraversorByGrid() {
            @Override
            public void traverseBegin() {

            }

            @Override
            public void traverseGrid(Point pointIdx, GridInfo gridInfo) {
                putGridInfo(pointIdx, gridInfo);
            }

            @Override
            public void traverseEnd() {

            }
        });
    }

    synchronized public void update(PointCloudGrid pointCloudGrid, TangoSupport.TangoMatrixTransformData transform, float currentZ) {
        if (pointCloudGrid == null) {
            return;
        }
        WorldMapGrid deltaMap = getDeltaMap(pointCloudGrid, transform, currentZ);
        update(deltaMap);
    }

    synchronized public int computeCommand(TangoPoseData poseData) {
        return CarControl.CMD_KEEP;
    }

    @Override
    synchronized public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        super.writeToDataOutputStream(out);
        out.writeInt(mInnerBorder.left);
        out.writeInt(mInnerBorder.right);
        out.writeInt(mInnerBorder.top);
        out.writeInt(mInnerBorder.bottom);
        int unknownCount = mInnerUnknown.size();
        out.writeInt(unknownCount);
        Iterator<Point> iterator = mInnerUnknown.iterator();
        for (int i = 0; i < unknownCount; i ++) {
            if (iterator.hasNext()) {
                Point point = iterator.next();
                out.writeInt(point.x);
                out.writeInt(point.y);
            }
        }
    }

    @Override
    synchronized public void readFromDataInputStream(DataInputStream in) throws IOException {
        super.readFromDataInputStream(in);
        mInnerBorder.left = in.readInt();
        mInnerBorder.right = in.readInt();
        mInnerBorder.top = in.readInt();
        mInnerBorder.bottom = in.readInt();
        int unknownCount = in.readInt();
        for (int i = 0; i < unknownCount; i ++) {
            int x = in.readInt();
            int y = in.readInt();
            mInnerUnknown.add(new Point(x, y));
        }
    }
}
