package com.lenovo.newdevice.tangocar.map;

import android.graphics.Point;

import java.util.Iterator;
import java.util.Queue;

/**
 * Created by liujk2 on 2016/12/27.
 */

public class MapPath extends GridMap {

    private Queue<Point> mExpactedPath;

    public void clear() {
        super.clear();
        mExpactedPath = null;
    }

    synchronized public void addRunnedGrid(Point gridPoint) {
        if (gridPoint == null) {
            return;
        }
        GridInfo gridInfo = getGridInfo(gridPoint.x, gridPoint.y, true);
        gridInfo.setType(GridInfo.TYPE_RUNNED_PATH);
    }

    synchronized public void setPathAs(MapPath path) {
        if (path == null || path.mGridCount == 0) {
            return;
        }
        mGridData.clear();
        Iterator iterator = path.mGridData.keySet().iterator();
        while(iterator.hasNext()) {
            Point point = (Point) iterator.next();
            GridInfo gridInfo = path.mGridData.get(point);
            putGridInfo(point, gridInfo);
        }
    }

    synchronized private void addExpectedGrid(Point gridPoint) {
        GridInfo gridInfo = getGridInfo(gridPoint.x, gridPoint.y, true);
        gridInfo.setType(GridInfo.TYPE_EXPECTED_PATH);
    }

    synchronized public void updateExpectedPath(Queue<Point> path) {
        if (path.size() == 0) {
            return;
        }
        mGridData.clear();
        mExpactedPath = path;
        for (Point point : path) {
            addExpectedGrid(point);
        }
    }

    synchronized public Queue<Point> getExpectedPath() {
        return mExpactedPath;
    }
}
