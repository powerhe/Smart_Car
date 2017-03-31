package com.lenovo.newdevice.tangocar.render.rajawali;

import android.graphics.Point;
import android.graphics.Rect;

import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.map.GridMap;
import com.lenovo.newdevice.tangocar.map.MapPath;
import com.lenovo.newdevice.tangocar.map.MapScope;
import com.lenovo.newdevice.tangocar.map.TraversorByGrid;

import org.rajawali3d.materials.Material;

/**
 * Created by liujk2 on 2016/12/29.
 */

public class ObjectWorldMap extends Points {
    private static final float[] ARRAY_COLOR_BLACK = {0F, 0F, 0F, 1F};  //0xFF000000
    private static final float[] ARRAY_COLOR_BORDER = {0F, 1F, 0F, 1F}; //0xFF00FF00
    private static final float[] ARRAY_COLOR_INNER = {68F/255F, 68F/255F, 68F/255F, 1F}; //0xFF444444
    private static final float[] ARRAY_COLOR_UNKNOWN = {136F/255F, 136F/255F, 136F/255F, 1F}; //0xFF888888
    private static final float[] ARRAY_COLOR_CURRENT = {1F, 0F, 0F, 1F};  //0xFFFF0000
    private static final float[] ARRAY_COLOR_RUNNED_PATH = {0F, 0F, 1F, 1F}; //0xFF0000FF
    private static final float[] ARRAY_COLOR_EXPECTED_PATH = {0F, 1F, 1F, 1F}; //0xFF00FFFF

    private float[] mColorArray;
    private float[] mPointArray;

    private int mPointCount;
    private int mPointIdex;
    private int mColorIdex;

    public ObjectWorldMap(int maxPoints, int floatsPerPoint) {
        super(maxPoints, floatsPerPoint, true);
        mColorArray = new float[maxPoints * 4];
        mPointArray = new float[maxPoints * floatsPerPoint];
        Material m = new Material();
        m.useVertexColors(true);
        setMaterial(m);
    }

    private float[] gridTypeToColor(int type) {
        switch (type) {
            case GridInfo.TYPE_UNKNOWN: {
                return ARRAY_COLOR_UNKNOWN;
            }
            case GridInfo.TYPE_BORDER: {
                return ARRAY_COLOR_BORDER;
            }
            case GridInfo.TYPE_INNER: {
                return ARRAY_COLOR_INNER;
            }
            case GridInfo.TYPE_RUNNED_PATH: {
                return ARRAY_COLOR_RUNNED_PATH;
            }
            case GridInfo.TYPE_EXPECTED_PATH: {
                return ARRAY_COLOR_EXPECTED_PATH;
            }
        }
        return ARRAY_COLOR_BLACK;
    }

    private FillPointAndColorTraversor mFillPointAndColorTraveser = new FillPointAndColorTraversor();
    class FillPointAndColorTraversor implements TraversorByGrid {
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
            setColor(pointIdx.x, pointIdx.y, gridTypeToColor(type));
        }

        @Override
        public void traverseEnd() {
        }

        public void setCenterPose(Point centerPos) {
            mCenterPos = centerPos;
        }

        private void setColor(int x, int y, float[] c) {
            float pointX = (x - mCenterPos.x) * 0.05f;
            float pointY = (mCenterPos.y - y) * 0.05f;
            mPointArray[mPointIdex++] = pointX;
            mPointArray[mPointIdex++] = 0;
            mPointArray[mPointIdex++] = pointY;
            System.arraycopy(c, 0, mColorArray, mColorIdex, 4);
            mColorIdex += 4;
            mPointCount ++;
        }
    };

    public void updateMap(Point centerPose, Rect currScope, GlobalData globalData) {
        mPointCount = 0;
        mPointIdex = 0;
        mColorIdex = 0;
        mFillPointAndColorTraveser.setCenterPose(centerPose);
        MapScope mapScope = new MapScope(currScope);
        if (globalData != null) {
            GridMap map = globalData.getMap();
            MapPath path = globalData.getPath(0);
            if (path != null) {
                path.traverseGridByScope(mFillPointAndColorTraveser, mapScope, false);
            }
            if (map != null) {
                map.traverseGridByScope(mFillPointAndColorTraveser, mapScope, false);
            }
        }
        updatePoints(mPointCount, mPointArray, mColorArray);
    }
}
