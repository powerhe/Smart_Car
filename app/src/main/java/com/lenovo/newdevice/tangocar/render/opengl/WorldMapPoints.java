package com.lenovo.newdevice.tangocar.render.opengl;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.map.MapPath;
import com.lenovo.newdevice.tangocar.map.MapScope;
import com.lenovo.newdevice.tangocar.map.TraversorByGrid;
import com.lenovo.newdevice.tangocar.map.WorldMapGrid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class WorldMapPoints {
    private WorldMapGrid mMap;
    private MapPath mPath;

    private ArrayList<Point> mBorderPoints;
    private ArrayList<Point> mInnerPoints;
    private ArrayList<Point> mRunnedPoints;
    private ArrayList<Point> mExpectedPoints;

    private FloatBuffer mCacheVertices = null;
    private int mCachePointsCount = 0;
    private float mPixelsPerPoint = GLMapRender.DEFAULT_PIXELS_PER_POINT;

    public WorldMapPoints(int widthPoint, int heightPoint) {
        mBorderPoints = new ArrayList<Point>();
        mInnerPoints = new ArrayList<Point>();
        mRunnedPoints = new ArrayList<Point>();
        mExpectedPoints = new ArrayList<Point>();
        prepareVerticesCache(widthPoint * heightPoint / 2);
    }

    public void setMap(WorldMapGrid map, MapPath path) {
        mMap = map;
        mPath = path;
    }

    private void prepareVerticesCache(int cachePointsCount) {
        if (cachePointsCount > mCachePointsCount) {
            mCachePointsCount = cachePointsCount;
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mCachePointsCount * 2 * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mCacheVertices = byteBuffer.asFloatBuffer();
        }
    }

    public void setPixelsPerPoint(float pixels) {
        mPixelsPerPoint = pixels;
    }

    public void drawPointsInArrayList(GL10 gl10, ArrayList<Point> points, float[] color) {
        int pointsCount = points.size();
        if (pointsCount == 0) {
            return;
        }
        prepareVerticesCache(pointsCount);
        mCacheVertices.clear();
        for (Point point : points) {
            mCacheVertices.put((float)point.x);
            mCacheVertices.put((float)point.y);
        }
        mCacheVertices.flip();

        gl10.glPointSize(mPixelsPerPoint);
        gl10.glColor4f(color[0], color[1], color[2], 1.0f);

        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glVertexPointer(2, GL10.GL_FLOAT, 0, mCacheVertices);
        gl10.glDrawArrays(GL10.GL_POINTS, 0, pointsCount);
    }

    public void draw(GL10 gl10, MapScope mapScope) {
        mBorderPoints.clear();
        mInnerPoints.clear();
        mRunnedPoints.clear();
        mExpectedPoints.clear();

        if (mMap != null) {
            mMap.traverseGridByScope(mWorldMapTraveser, mapScope, false);
        }
        if (mPath != null) {
            mPath.traverseGridByScope(mWorldMapTraveser, mapScope, false);
        }

        drawPointsInArrayList(gl10, mBorderPoints, GLMapRender.COLOR_BORDER);
        drawPointsInArrayList(gl10, mInnerPoints, GLMapRender.COLOR_INNER);
        drawPointsInArrayList(gl10, mRunnedPoints, GLMapRender.COLOR_RUNNED_PATH);
        drawPointsInArrayList(gl10, mExpectedPoints, GLMapRender.COLOR_EXPECTED_PATH);
    }

    private WorldMapTraversor mWorldMapTraveser = new WorldMapTraversor();
    class WorldMapTraversor implements TraversorByGrid {
        private Point mCenterPos;

        @Override
        public void traverseBegin() {
        }

        @Override
        public void traverseGrid(Point pointIdx, GridInfo gridInfo) {
            int type = GridInfo.TYPE_UNKNOWN;
            if (gridInfo != null) {
                type = gridInfo.getType();
            }
            putPointByType(pointIdx, type);
        }

        @Override
        public void traverseEnd() {
        }

        private void putPointByType(Point point, int type) {
            switch (type) {
                case GridInfo.TYPE_BORDER:
                    mBorderPoints.add(point);
                    break;
                case GridInfo.TYPE_INNER:
                    mInnerPoints.add(point);
                    break;
                case GridInfo.TYPE_RUNNED_PATH:
                    mRunnedPoints.add(point);
                    break;
                case GridInfo.TYPE_EXPECTED_PATH:
                    mExpectedPoints.add(point);
                    break;
                default:
                    break;
            }
        }
    };

}
