package com.lenovo.newdevice.tangocar.path.finder.data;

import com.lenovo.newdevice.tangocar.map.GridMap;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborSelector;
import com.lenovo.newdevice.tangocar.path.finder.provider.FinderSettings;
import com.lenovo.newdevice.tangocar.utils.Utils;

public class OptWorldMap extends WorldMap {

    private boolean mDebug;

    private int mCarSize;

    private NeighborSelector mNeighborSelector;

    public OptWorldMap(GridMap gridMap, WeightedPoint start, WeightedPoint goal, int carSize) {
        super(gridMap, start, goal);
        this.mCarSize = carSize;
        this.mDebug = FinderSettings.from(null).debug();

        this.mNeighborSelector = FinderSettings.from(null).neighborSelector();

        logIfDebug(toString());
    }

    public OptWorldMap(WorldMap worldMap, int carSize) {
        this(worldMap.gridMap, worldMap.getStart(), worldMap.getGoal(), carSize);
    }

    @Override
    public boolean isTraversable(int x, int y) {

        boolean isTraversable = super.isTraversable(x, y);

        logIfDebug("ENTER {%s, %s}, isTraversable %s", x, y, isTraversable);

        int offset = (mCarSize - 1) / 2;

        if (offset == 0) return isTraversable;

        logIfDebug("Offset %s", offset);

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x + i, y);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x + i, y, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x - i, y);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x - i, y, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x, y - i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x, y - i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x, y + i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x, y + i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x + i, y + i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x + i, y + i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x - i, y - i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x - i, y - i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x + i, y - i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x + i, y - i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        if (isTraversable) {
            for (int i = 1; i <= offset; i++) {
                isTraversable = super.isTraversable(x - i, y + i);
                logIfDebug("NEIGHBOR {%s, %s}, isTraversable %s", x - i, y + i, isTraversable);
                if (!isTraversable) return false;
            }
        }

        logIfDebug("LATER_EXIT isTraversable %s", isTraversable);

        return isTraversable;
    }

    @Override
    public String toString() {
        return "OptWorldMap{" +
                "mDebug=" + mDebug +
                ", mCarSize=" + mCarSize +
                ", mNeighborSelector=" + mNeighborSelector +
                '}';
    }

    private void logIfDebug(String log, Object... args) {
        if (mDebug)
            Utils.outLog(getClass().getSimpleName() + "-" + String.format(log, args));
    }
}
