package com.lenovo.newdevice.tangocar.path.finder;

import android.graphics.Point;
import android.support.annotation.VisibleForTesting;

import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.PointInfo;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicEnum;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEnum;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.LinkedList;
import java.util.Queue;

public class FinderC implements Finder {

    private HeuristicEnum mHeuristic;

    private NeighborEnum mNeighbor;

    private boolean dijkstra;
    private boolean shuffle;

    private int maxStep = Integer.MAX_VALUE;
    private long timeout = TIME_OUT_NOT_SET;

    private BaseMap map;

    private Queue<Point> mPath = new LinkedList<>();
    private FinderStatus mFinderStatus = FinderStatus.IDLE;

    private boolean mIsCanceled;

    public FinderC(BaseMap map) {
        this.map = map;
    }

    @Override
    public FinderStatus solve() {

        nativeFind(getStart(), getGoal(),
                mHeuristic,
                mNeighbor,
                dijkstra,
                shuffle,
                maxStep,
                timeout,
                new CCallback() {
                    @Override
                    public int getNodeType(int x, int y) {
                        if (mIsCanceled) {
                            return CANCEL_SIGNAL;
                        }
                        boolean travelable = map.isTraversable(x, y);
                        return travelable ? PointInfo.TRAVELABLE.ordinal()
                                : PointInfo.NOT_TRAVELABLE.ordinal();
                    }

                    @Override
                    public void onStart() {
                        Utils.outLog("FindNative is started");
                    }

                    @Override
                    public void onComplete(int status) {
                        mFinderStatus = FinderStatus.fromInt(status);
                    }
                }, new PathSink() {
                    @Override
                    public void receive(int x, int y) {
                        mPath.add(new Point(x, y));
                    }
                });

        return mFinderStatus;
    }

    public void setMap(BaseMap map) {
        this.map = map;
    }

    @Override
    public Point getStart() {
        return map.getStart();
    }

    @Override
    public Point getGoal() {
        return map.getGoal();
    }

    @Override
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    @Override
    public void setDijkstra(boolean dijkstra) {
        this.dijkstra = dijkstra;
    }

    @Override
    public FinderStatus getStatus() {
        return mFinderStatus;
    }

    @Override
    public Queue<Point> getPath() {
        return mPath;
    }

    @Override
    public void setNeighborSelector(NeighborEnum selector) {
        this.mNeighbor = selector;
    }

    public void setHeuristic(HeuristicEnum heuristic) {
        this.mHeuristic = heuristic;
    }

    @Override
    public void setTimeout(long time) {
        this.timeout = time;
    }

    @Override
    public void setMaxStep(int max) {
        this.maxStep = max;
    }

    @Override
    public void cancel() {
        this.mIsCanceled = true;
    }

    //
    // Native api
    //
    static {
        System.loadLibrary("finder");
    }

    private static native void nativeFind(int startX, int startY,
                                          int goalX, int goalY,
                                          int heuristic, int neighborSelector,
                                          boolean dijkstra, boolean shuffle,
                                          int maxStep, long timeout,
                                          CCallback callback, PathSink sink);

    @VisibleForTesting
    private static void nativeFind(Point start, Point goal,
                                   HeuristicEnum heuristic,
                                   NeighborEnum neighbor,
                                   boolean dijkstra, boolean shuffle,
                                   int maxStep, long timeout,
                                   CCallback callback, PathSink sink) {
        int startX = start.x;
        int startY = start.y;
        int goalX = goal.x;
        int goalY = goal.y;

        nativeFind(startX, startY, goalX, goalY, heuristic.ordinal(), neighbor.ordinal(), dijkstra, shuffle, maxStep, timeout, callback, sink);
    }

}
