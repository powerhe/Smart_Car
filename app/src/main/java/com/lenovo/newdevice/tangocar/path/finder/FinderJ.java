package com.lenovo.newdevice.tangocar.path.finder;

import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.lenovo.newdevice.tangocar.path.finder.common.Cancelable;
import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicEnum;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicScheme;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEnum;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborSelector;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class FinderJ implements Finder {


    /**
     * The state of the algorithm: running, completed with a result, completed with no result
     */
    private FinderStatus status;

    /**
     * The map: rows x columns, false = traversable, true = wall
     */
    private BaseMap map;

    /**
     * Tail of path being built by running this algorithm
     */
    private WeightedPoint tail;

    /**
     * Cursor point
     */
    private WeightedPoint cursor;

    /**
     * Points that have yet to be visited stored as a min-heap based on the point cost (Note that this "set" is actually
     * a heap, so a manual test for whether the point being added should be used prior to calling push().) The heap is
     * used for the open set to quickly return the point with the minimum cost.
     */
    private Heap<WeightedPoint> openSet;

    /**
     * Points that have already been visited
     */
    private Set<WeightedPoint> closedSet;

    /**
     * The means for determining distances
     */
    private HeuristicScheme heuristic;

    /**
     * The means for determining valid neighbors
     */
    private NeighborSelector neighborSelector;

    /**
     * Whether to assign any h value (to cost) with the heuristic. If true, will find the optimal path.
     */
    private boolean dijkstra;

    /**
     * Whether to shuffle the order of nodes with the same cost to avoid the bias of the default neighbor order
     */
    private boolean shuffle;

    /**
     * Whether the initial step of the algorithm has been taken,
     * used to set the tail node and push the start onto the open set
     */
    private boolean initialStep;

    private boolean abort;

    private long timeout;

    private int maxStep = MAX_STEP_DEFAULT;

    private int stepCount;

    /**
     * Construct the PathFinder
     *
     * @param map the map
     */
    public FinderJ(BaseMap map) {
        reset(map);
    }

    /**
     * Get the Map
     *
     * @return the map
     */
    public BaseMap getMap() {
        return map;
    }

    /**
     * Get the Start point
     *
     * @return the start
     */
    public WeightedPoint getStart() {
        return map.getStart();
    }

    /**
     * Get the Goal point
     *
     * @return the goal
     */
    public WeightedPoint getGoal() {
        return map.getGoal();
    }

    /**
     * Get the Cursor point
     *
     * @return the cursor
     */
    public WeightedPoint getCursor() {
        return cursor;
    }

    /**
     * Get the seed that generated the current map
     */
    public int getSeed() {
        return map.getSeed();
    }

    /**
     * Get the open set which contains points that have yet to be visited stored as a min-heap based on the point cost
     * (Note that this "set" is actually a heap, so a manual test for whether the point being added should be used prior
     * to calling push().) The heap is used for the open set to quickly return the point with the minimum cost.
     *
     * @return the openSet
     */
    public Heap<WeightedPoint> getOpenSet() {
        return openSet;
    }

    /**
     * Get the Closed Set
     *
     * @return the closedSet
     */
    public Set<WeightedPoint> getClosedSet() {
        return closedSet;
    }

    @Override
    public String toString() {
        return "FinderJ{" +
                "heuristic=" + heuristic +
                ", neighborSelector=" + neighborSelector +
                ", dijkstra=" + dijkstra +
                ", shuffle=" + shuffle +
                ", timeout=" + timeout +
                ", maxStep=" + maxStep +
                '}';
    }

    /**
     * Set the Heuristic
     */
    public void setHeuristic(HeuristicScheme heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Set the Neighbor Selector
     */
    public void setNeighborSelector(NeighborSelector neighborSelector) {
        this.neighborSelector = neighborSelector;
    }

    /**
     * Sets whether to assign any h value (to cost) with the heuristic. If true, will find the optimal path.
     */
    public void setDijkstra(boolean dijkstra) {
        this.dijkstra = dijkstra;
    }

    /**
     * Set whether to shuffle the order of nodes with the same cost to avoid the bias of the default neighbor order
     */
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    /**
     * Reset the PathFinder using the original map
     */
    public void reset() {
        reset(this.map, this.heuristic, this.neighborSelector);
    }

    /**
     * Reset the PathFinder with a redefined map, start, and goal, but leaving the selected heuristic and neighbor selector
     */
    public void reset(BaseMap map) {
        reset(map, this.heuristic, this.neighborSelector);
    }

    /**
     * Reset the PathFinder
     *
     * @param map the map
     */
    public void reset(BaseMap map, HeuristicScheme heuristic, NeighborSelector neighborSelector) {
        this.map = map;

        this.cursor = null;

        this.heuristic = heuristic;
        this.neighborSelector = neighborSelector;

        this.initialStep = true;

        this.status = FinderStatus.RUNNING;

        this.openSet = new Heap<>();
        this.closedSet = new HashSet<>();
    }

    /**
     * Run through the next step in the algorithm. Each step represents investigating another possible point on the path
     *
     * @return the status after running the step
     */
    public FinderStatus step() {
        if (stepCount >= maxStep) {
            status = FinderStatus.COMPLETED_NOT_FOUND;
            return status;
        }
        status = stepInternal();
        stepCount++;
        return status;
    }

    /**
     * Run through the next step in the algorithm. Each step represents investigating another possible point on the path
     *
     * @return the status after running the step
     */
    private FinderStatus stepInternal() {
        if (initialStep) {
            this.tail = map.getStart();
            this.openSet.push(map.getStart());
            initialStep = false;
        }

        if (status != FinderStatus.RUNNING)
            return status;

        cursor = openSet.pop(); // Pull the cursor off the open set min-heap
        if (cursor == null) {
            // The open set was empty, so although we have not reached the goal, there are no more points to investigate
            return FinderStatus.COMPLETED_NOT_FOUND;
        }

        while (closedSet.contains(cursor) || !map.isTraversable(cursor)) {
            // The cursor is in the closed set (meaning it was already investigated) or the cursor point is non traversable on the map
            cursor = openSet.pop();
            if (cursor == null) {
                return FinderStatus.COMPLETED_NOT_FOUND;
            }
        }

        // The goal has been reached, the path is complete
        if (cursor.equals(map.getGoal())) {
            tail = cursor; // Set the member tail to be used in the reconstruction done in getPath()
            return FinderStatus.COMPLETED_FOUND;
        }

        // Add the cursor point to the closed set
        closedSet.add(cursor);

        // Get the list of neighboring points
        List<WeightedPoint> neighbors = neighborSelector.getNeighbors(map, cursor, heuristic);

        // Link the neighbors to the cursor (for backtracking the path when the goal is reached) and calculate their weight
        for (WeightedPoint wp : neighbors) {
            if (map.isTraversable(wp.getX(), wp.getY()) && !closedSet.contains(wp)) {
                wp.setFromCost(cursor.getFromCost() + heuristic.distance(cursor, wp));

                if (dijkstra) {
                    wp.setToCost(0);
                } else {
                    wp.setToCost(heuristic.distance(wp, map.getGoal()));
                }
                wp.setPrev(cursor);
            }
        }

        if (shuffle) {
            // Shuffle the neighbors to randomize the order of testing nodes with the same cost value
            Collections.shuffle(neighbors);
        }
        Collections.sort(neighbors);

        // Put the neighbors on the open set
        for (WeightedPoint wp : neighbors) {
            if (!openSet.contains(wp))
                openSet.push(wp);
        }

        return FinderStatus.RUNNING;
    }

    /**
     * Continue to call step until the algorithm is complete
     */
    public FinderStatus solve() {

        Utils.outLog("Solving with params:\n" + toString());

        TimeoutTracker tracker = null;

        if (timeout > 0) {
            tracker = new TimeoutTracker(timeout, new Runnable() {
                @Override
                public void run() {
                    if (status == FinderStatus.RUNNING) status = FinderStatus.TIMEOUT;
                }
            });
            tracker.start();
        }


        while (status == FinderStatus.RUNNING) {
            if (abort) {
                status = FinderStatus.CANCELED;
            }
            step();
        }

        if (tracker != null) tracker.cancel();

        return status;
    }

    /**
     * Get the path finder algorithm status
     */
    public FinderStatus getStatus() {
        return status;
    }

    /**
     * Constructs and returns a List version of the tail linked list generated by the algorithm
     */
    public Queue<Point> getPath() {
        return getPath(this.tail);
    }

    @Override
    public void setNeighborSelector(NeighborEnum selector) {
        this.neighborSelector = selector.create();
    }

    @Override
    public void setHeuristic(HeuristicEnum scheme) {
        this.heuristic = scheme.create();
    }

    @Override
    public void setTimeout(long time) {
        this.timeout = time;
    }

    @Override
    public void setMaxStep(int max) {
        this.maxStep = max;
    }

    /**
     * Constructs and returns a List version of the linked list generated by the algorithm from the specified cursor point
     */
    public Queue<Point> getPath(WeightedPoint cursor) {
        LinkedList<Point> path = new LinkedList<>();
        while (cursor != null) {
            path.addFirst(cursor);
            cursor = cursor.getPrev();
        }
        return path;
    }

    private void setAbort(boolean abort) {
        this.abort = abort;
    }

    @Override
    public void cancel() {
        setAbort(true);
    }

    public int countSteps() {
        return stepCount;
    }

    private class TimeoutTracker implements Cancelable {

        private static final int MSG = 1;
        /**
         * Millis since epoch when alarm should stop.
         */
        private final long mMillisInFuture;
        /**
         * The interval in millis that the user receives callbacks
         */
        private final long mCountdownInterval;
        private Runnable mTimeoutRunnable;
        private long mStopTimeInFuture;
        /**
         * boolean representing if the timer was cancelled
         */
        private boolean mCancelled = false;
        // handles counting down
        private Handler mHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {

                synchronized (TimeoutTracker.this) {
                    if (mCancelled) {
                        return;
                    }

                    final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();

                    if (millisLeft <= 0) {
                        onFinish();
                    } else if (millisLeft < mCountdownInterval) {
                        // no tick, just delay until done
                        sendMessageDelayed(obtainMessage(MSG), millisLeft);
                    } else {
                        long lastTickStart = SystemClock.elapsedRealtime();

                        // take into account user's onTick taking time to execute
                        long delay = lastTickStart + mCountdownInterval - SystemClock.elapsedRealtime();

                        // special case: user's onTick took more than interval to
                        // complete, skip to next interval
                        while (delay < 0) delay += mCountdownInterval;

                        sendMessageDelayed(obtainMessage(MSG), delay);
                    }
                }
            }
        };

        /**
         * @param millisInFuture The number of millis in the future from the call
         */
        TimeoutTracker(long millisInFuture, Runnable timeoutRunnable) {
            this.mMillisInFuture = millisInFuture;
            this.mCountdownInterval = 3 * 1000;
            this.mTimeoutRunnable = timeoutRunnable;
        }

        /**
         * Cancel the countdown.
         */
        public synchronized final void cancel() {
            mCancelled = true;
            mHandler.removeMessages(MSG);
        }

        /**
         * Start the countdown.
         */
        public synchronized final TimeoutTracker start() {
            mCancelled = false;
            if (mMillisInFuture <= 0) {
                onFinish();
                return this;
            }
            mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
            mHandler.sendMessage(mHandler.obtainMessage(MSG));
            return this;
        }

        void onFinish() {
            mTimeoutRunnable.run();
        }
    }
}
