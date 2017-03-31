package com.lenovo.newdevice.tangocar.path.finder.data;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.map.GridMap;

public class WorldMap implements BaseMap {

    // We own a world map!!!
    private static final int MAX_MAP_SIZE = Integer.MAX_VALUE;

    protected GridMap gridMap;

    /**
     * The starting point
     */
    private WeightedPoint start;
    /**
     * The goal point4
     */
    private WeightedPoint goal;


    public WorldMap(GridMap gridMap, WeightedPoint start, WeightedPoint goal) {
        this.gridMap = gridMap;
        this.start = start;
        this.goal = goal;
    }

    @Override
    public WeightedPoint getStart() {
        return start;
    }

    @Override
    public WeightedPoint getGoal() {
        return goal;
    }

    @Override
    public int getSeed() {
        return 0;
    }

    @Override
    public boolean isTraversable(WeightedPoint wp) {
        return isTraversable(wp.getX(), wp.getY());
    }

    @Override
    public boolean isTraversable(final int x, final int y) {
        if (Math.abs(x) > getWidth()) return false;
        if (Math.abs(y) > getHeight()) return false;
        GridInfo info = gridMap.getGridInfo(new Point(x, y), false);
        return info == null || info.getType() != GridInfo.TYPE_BORDER;
    }

    @Override
    public void setTraversable(int row, int col, boolean traversable) {
        GridInfo info = gridMap.getGridInfo(col, row, traversable);
        info.setType(GridInfo.TYPE_INNER);
    }

    @Override
    public int getWidth() {
        return gridMap.getScope().width();
    }

    @Override
    public int getHeight() {
        return gridMap.getScope().height();
    }

    @Override
    public String getMapStats() {
        return toString();
    }

    @Override
    public boolean isStartTraversable() {
        return isTraversable(start);
    }

    @Override
    public boolean isGoalTraversable() {
        return isTraversable(goal);
    }

    @Override
    public String toString() {
        return "WorldMap{" +
                "gridMap=" + gridMap +
                ", start=" + start +
                ", goal=" + goal +
                ", row=" + getWidth() +
                ", col=" + getHeight() +
                '}';
    }
}
