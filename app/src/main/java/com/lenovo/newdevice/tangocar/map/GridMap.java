package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.SerializableUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/12/19.
 */

public class GridMap implements DataSerializable {
    protected static float sGridWidth;

    protected Rect mRectScope;
    protected Object mScopeLock;
    protected HashMap<Long, GridInfo> mFastGridData;

    protected HashMap<Point, GridInfo> mGridData;
    protected int mGridCount;

    static {
        setGridWidth(GridInfo.sGridWidth);
    }

    public GridMap() {
        mRectScope = new Rect(0, 0, 0, 0);
        mScopeLock = new Object();
        mFastGridData = new HashMap<Long, GridInfo>();
        mGridData = new HashMap<Point, GridInfo>();
        mGridCount = 0;
    }

    public void clear() {
        synchronized (mScopeLock) {
            mRectScope = new Rect(0, 0, 0, 0);
        }
        synchronized (this) {
            mGridData.clear();
            mGridCount = 0;
        }
    }

    public static void setGridWidth(float gridWidth) {
        sGridWidth = gridWidth;
    }

    public static long getIndex(int x, int y) {
        return ((long)x) << 32 + y;
    }

    public static Point indexToPoint(long index) {
        return new Point((int)((index & 0xFFFFFFFF00000000L) >> 32), (int)(index & 0xFFFFFFFFL));
    }

    protected void adjustScope(int x, int y) {
        synchronized (mScopeLock) {
            if (x < mRectScope.left) {
                mRectScope.left = x;
            }
            if (x > mRectScope.right) {
                mRectScope.right = x;
            }
            if (y < mRectScope.top) {
                mRectScope.top = y;
            }
            if (y > mRectScope.bottom) {
                mRectScope.bottom = y;
            }
        }
    }

    public Rect getScope() {
        synchronized (mScopeLock) {
            return mRectScope;
        }
    }

    public GridInfo getGrid(int x, int y) {
        return mFastGridData.get(getIndex(x,y));
    }

    public GridInfo getGridNeedCreate(int x, int y) {
        GridInfo gridInfo = getGrid(x, y);
        if (gridInfo == null) {
            adjustScope(x, y);
            gridInfo = new GridInfo(x, y);
            mFastGridData.put(getIndex(x,y), gridInfo);
            mGridCount ++;
        }
        return gridInfo;
    }

    public static Point getGridIndex(float x, float y) {
        int xIdx = (int)(x/ sGridWidth);
        int yIdx = (int)(y/ sGridWidth);
        return new Point(xIdx, yIdx);
    }

    public static Point getGridIndex(int x, int y) {
        return new Point(x, y);
    }

    public GridInfo getGridInfo(float x, float y, boolean needCreate) {
        GridInfo gridInfo = getGridInfo(getGridIndex(x, y), needCreate);
        /*if (!needCreate && gridInfo == null) {
            Log.i(TAG, "no grid for index("+xIdx+","+yIdx+")");
        }*/
        return gridInfo;
    }

    public GridInfo getGridInfo(int x, int y, boolean needCreate) {
        return getGridInfo(getGridIndex(x, y), needCreate);
    }

    public void putGridInfo(int x, int y, GridInfo newGridInfo) {
        Point index = getGridIndex(x, y);
        putGridInfo(index, newGridInfo);
    }

    public void putGridInfo(Point index, GridInfo newGridInfo) {
        GridInfo gridInfo = mGridData.get(index);
        if (gridInfo == null) {
            mGridCount ++;
            onCreateGridInfo(index);
            adjustScope(index.x, index.y);
        }
        mGridData.put(index, newGridInfo);
    }

    protected void onCreateGridInfo(Point index) {
    }

    synchronized protected GridInfo createGridInfo(Point index) {
        GridInfo gridInfo = mGridData.get(index);
        if (gridInfo == null) {
            mGridCount++;
            onCreateGridInfo(index);
            adjustScope(index.x, index.y);
        }
        gridInfo = new GridInfo(index.x, index.y);
        mGridData.put(index, gridInfo);
        return gridInfo;
    }

    synchronized public GridInfo getGridInfo(Point index, boolean needCreate) {
        GridInfo gridInfo = mGridData.get(index);
        if (gridInfo == null && needCreate) {
            gridInfo = createGridInfo(index);
        }
        return gridInfo;
    }

    synchronized protected void removeGridInfo(int x, int y) {
        Point pointIndex = new Point(x, y);
        GridInfo gridInfo = mGridData.remove(pointIndex);
        if (gridInfo != null) {
            mGridCount--;
        }
    }

    synchronized public void traverseGrid(TraversorByGrid traversor) {
        if (traversor == null) return;
        traversor.traverseBegin();
        Iterator iterator = mGridData.keySet().iterator();
        while(iterator.hasNext()) {
            Point point = (Point) iterator.next();
            traversor.traverseGrid(point, mGridData.get(point));
        }
        traversor.traverseEnd();
    }

    synchronized public void traverseGridByOrder(TraversorByGrid traversor) {
        traverseGridByOrder(traversor, false);
    }

    synchronized public void traverseGridWithNull(TraversorByGrid traversor) {
        traverseGridByOrder(traversor, true);
    }

    synchronized protected void traverseGridByOrder(TraversorByGrid traversor, boolean withNull) {
        if (traversor == null) return;
        traversor.traverseBegin();
        for (int yIdx = mRectScope.top; yIdx <= mRectScope.bottom; yIdx ++) {
            for (int xIdx = mRectScope.left; xIdx <= mRectScope.right; xIdx ++) {
                Point pointIndex = new Point(xIdx, yIdx);
                GridInfo gridInfo = mGridData.get(pointIndex);
                if (withNull || gridInfo != null) {
                    traversor.traverseGrid(pointIndex, gridInfo);
                }
            }
        }
        traversor.traverseEnd();
    }

    synchronized public void traverseGridByScope(TraversorByGrid traversor, MapScope scope, boolean withNull) {
        if (traversor == null) return;
        if (scope.isRect) {
            Rect rect = scope.mScopeRect;
            boolean traverseAllScope = false;
            boolean allGridInScope = false;
            if (withNull) {
                traverseAllScope = true;
            } else {
                if (mRectScope.left >= rect.left
                        && mRectScope.top >= rect.top
                        && mRectScope.right <= rect.right
                        && mRectScope.bottom <= rect.bottom) {
                    allGridInScope = true;
                } else {
                    if (mGridCount >= (rect.right - rect.left) * (rect.bottom - rect.right)) {
                        traverseAllScope = true;
                    }
                }
            }
            if (traverseAllScope) {
                traversor.traverseBegin();
                int minXIndex = rect.left;
                int minYIndex = rect.top;
                int maxXIndex = rect.right - 1;
                int maxYIndex = rect.bottom - 1;
                for (int yIdx = minYIndex; yIdx <= maxYIndex; yIdx++) {
                    for (int xIdx = minXIndex; xIdx <= maxXIndex; xIdx++) {
                        Point pointIndex = new Point(xIdx, yIdx);
                        GridInfo gridInfo = mGridData.get(pointIndex);
                        if (withNull || gridInfo != null) {
                            traversor.traverseGrid(pointIndex, gridInfo);
                        }
                    }
                }
                traversor.traverseEnd();
            } else {
                if (allGridInScope) {
                    traverseGrid(traversor);
                } else {
                    traversor.traverseBegin();
                    Iterator iterator = mGridData.keySet().iterator();
                    while(iterator.hasNext()) {
                        Point point = (Point) iterator.next();
                        if (point.x >= rect.left
                                && point.x < rect.right
                                && point.y >= rect.top
                                && point.y < rect.bottom) {
                            traversor.traverseGrid(point, mGridData.get(point));
                        }
                    }
                    traversor.traverseEnd();
                }
            }
        }
    }

    synchronized public void traverseCenterPoints(TraversorByPoint3D traversor) {
        if (traversor == null) return;
        CenterPointTraversorFromGrid gridTraveser = new CenterPointTraversorFromGrid(traversor);
        traverseGrid(gridTraveser);
    }

    synchronized public void traverseCenterPointsByOrder(TraversorByPoint3D traversor) {
        if (traversor == null) return;
        CenterPointTraversorFromGrid gridTraveser = new CenterPointTraversorFromGrid(traversor);
        traverseGridByOrder(gridTraveser);
    }

    synchronized public void traversePoints(TraversorByPoint3D traversor) {
        if (traversor == null) return;
        PointTraversorFromGrid gridTraveser = new PointTraversorFromGrid(traversor);
        traverseGrid(gridTraveser);
    }

    synchronized public void traversePointsByOrder(TraversorByPoint3D traversor) {
        if (traversor == null) return;
        PointTraversorFromGrid gridTraveser = new PointTraversorFromGrid(traversor);
        traverseGridByOrder(gridTraveser);
    }

    synchronized public GridInfo[] getGrids() {
        GridInfo[] gridInfos = new GridInfo[mGridCount];
        int idx = 0;
        for (Point point : mGridData.keySet()) {
            gridInfos[idx ++] = mGridData.get(point);
        }
        return gridInfos;
    }

    class RealPointTraversorFromGrid extends PointTraversorFromGrid {

        public RealPointTraversorFromGrid(TraversorByPoint3D traversor) {
            super(traversor);
        }

        @Override
        public void traverseGrid(Point pointIdx, GridInfo gridInfo) {
            int type = gridInfo.getType();
            double timestamp = gridInfo.mTimestamp;

            FloatPoint3D[] points = gridInfo.getPoints();
            for (FloatPoint3D point: points) {
                mPointTraversor.traversePoint(point, type, timestamp);
            }
        }
    }

    class CenterPointTraversorFromGrid extends PointTraversorFromGrid {

        public CenterPointTraversorFromGrid(TraversorByPoint3D traversor) {
            super(traversor);
        }

        @Override
        public void traverseGrid(Point pointIdx, GridInfo gridInfo) {
            int type = gridInfo.getType();
            double timestamp = gridInfo.mTimestamp;

            FloatPoint3D centerPoint = gridInfo.getCenterPoint();
            if (centerPoint != null) {
                mPointTraversor.traversePoint(centerPoint, type, timestamp);
            }
        }
    }

    class PointTraversorFromGrid implements TraversorByGrid {
        protected TraversorByPoint3D mPointTraversor;
        public PointTraversorFromGrid(TraversorByPoint3D traversor) {
            mPointTraversor = traversor;
        }
        @Override
        public void traverseBegin() {
            mPointTraversor.traverseBegin();
        }

        @Override
        public void traverseGrid(Point pointIdx, GridInfo gridInfo) {
            int type = gridInfo.getType();
            double timestamp = gridInfo.mTimestamp;
            FloatPoint3D[] points = gridInfo.getPoints();
            for (FloatPoint3D point: points) {
                mPointTraversor.traversePoint(point, type, timestamp);
            }
            FloatPoint3D centerPoint = gridInfo.getCenterPoint();
            if (centerPoint != null) {
                mPointTraversor.traversePoint(centerPoint, type, timestamp);
            }
        }

        @Override
        public void traverseEnd() {
            mPointTraversor.traverseEnd();
        }
    }

    public String toPgmString() {
        StringBuffer stringBuffer = new StringBuffer();

        int width = mRectScope.right - mRectScope.left + 1;
        int height = mRectScope.bottom - mRectScope.top + 1;

        stringBuffer.append("P2\n");
        stringBuffer.append("" + width + " " + height + " 255\n");

        for (int y = mRectScope.bottom; y >= mRectScope.top; y--) {
            for (int x = mRectScope.left; x <= mRectScope.right; x++) {
                if (x > mRectScope.left) stringBuffer.append(" ");
                int type = GridInfo.TYPE_UNKNOWN;
                GridInfo gridInfo = getGridInfo(x, y, false);
                if (gridInfo != null) {
                    type = gridInfo.getType();
                }
                if (type == GridInfo.TYPE_UNKNOWN) {
                    stringBuffer.append("127");
                } else if (type == GridInfo.TYPE_INNER) {
                    stringBuffer.append("255");
                } else if (type == GridInfo.TYPE_BORDER) {
                    stringBuffer.append("000");
                }
            }
            stringBuffer.append("\n");
        }

        return stringBuffer.toString();
    }

    synchronized public void writeToOutputStreamAsPgm(OutputStream out) {
        int width = mRectScope.right - mRectScope.left + 1;
        int height = mRectScope.bottom - mRectScope.top + 1;

        byte bytesSpace = ' ';
        byte bytesEnter = '\n';
        byte[] bytes000 = {'0','0','0'};
        byte[] bytes127 = {'1','2','7'};
        byte[] bytes255 = {'2','5','5'};

        try {
            String str = "P2\n";
            out.write(str.getBytes());
            str = "" + width + " " + height + " 255\n";
            out.write(str.getBytes());

            for (int y = mRectScope.bottom; y >= mRectScope.top; y--) {
                for (int x = mRectScope.left; x <= mRectScope.right; x++) {
                    if (x > mRectScope.left) {
                        out.write(bytesSpace);
                    }
                    int type = GridInfo.TYPE_UNKNOWN;
                    GridInfo gridInfo = getGridInfo(x, y, false);
                    if (gridInfo != null) {
                        type = gridInfo.getType();
                    }
                    if (type == GridInfo.TYPE_UNKNOWN) {
                        out.write(bytes127);
                    } else if (type == GridInfo.TYPE_INNER) {
                        out.write(bytes255);
                    } else if (type == GridInfo.TYPE_BORDER) {
                        out.write(bytes000);
                    }
                }
                out.write(bytesEnter);
            }
            out.flush();
        } catch (Exception e) {
            Log.i(TAG, "writePgmStringToOutputStream error: ", e);
        }
    }

    @Override
    synchronized public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeInt(mGridData.size());
        out.writeInt(mRectScope.left);
        out.writeInt(mRectScope.top);
        out.writeInt(mRectScope.right);
        out.writeInt(mRectScope.bottom);
        Iterator iterator = mGridData.keySet().iterator();
        while(iterator.hasNext()) {
            Point pointIndex = (Point) iterator.next();
            GridInfo gridInfo = mGridData.get(pointIndex);
            if (gridInfo != null) {
                out.writeInt(pointIndex.x);
                out.writeInt(pointIndex.y);
                SerializableUtils.writeObjectToData(gridInfo, out);
            }
        }
    }

    @Override
    synchronized public void readFromDataInputStream(DataInputStream in) throws IOException {
        mGridCount = in.readInt();
        mRectScope.left = in.readInt();
        mRectScope.top = in.readInt();
        mRectScope.right = in.readInt();
        mRectScope.bottom = in.readInt();
        for (int i = 0; i < mGridCount; i ++) {
            int xIdx = in.readInt();
            int yIdx = in.readInt();
            Point pointIndex = new Point(xIdx, yIdx);
            GridInfo gridInfo = (GridInfo) SerializableUtils.readObjectFromData(in);
            mGridData.put(pointIndex, gridInfo);
        }
    }
}
