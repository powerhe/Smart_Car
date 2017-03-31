package com.lenovo.newdevice.tangocar.path.finder;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.path.finder.common.Cancelable;
import com.lenovo.newdevice.tangocar.path.finder.data.PointInfo;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicEnum;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEnum;

import java.util.Queue;

/**
 * Created by Nick@NewStand.org on 2017/3/19 16:50
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

public interface Finder extends Cancelable {

    long TIME_OUT_NOT_SET = -1;
    int MAX_STEP_DEFAULT = Integer.MAX_VALUE;
    int CANCEL_SIGNAL = PointInfo.EXPIRE_CANCELED.ordinal();

    FinderStatus solve();

    Point getStart();

    Point getGoal();

    void setShuffle(boolean shuffle);

    void setDijkstra(boolean dijkstra);

    FinderStatus getStatus();

    Queue<Point> getPath();

    void setNeighborSelector(NeighborEnum selector);

    void setHeuristic(HeuristicEnum scheme);

    void setTimeout(long time);

    void setMaxStep(int max);
}
