package com.lenovo.newdevice.tangocar.path;

import android.content.Context;
import android.graphics.Point;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.lenovo.newdevice.tangocar.map.GridMap;
import com.lenovo.newdevice.tangocar.path.finder.Finder;
import com.lenovo.newdevice.tangocar.path.finder.FinderC;
import com.lenovo.newdevice.tangocar.path.finder.FinderJ;
import com.lenovo.newdevice.tangocar.path.finder.FinderStatus;
import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.OptTileMap;
import com.lenovo.newdevice.tangocar.path.finder.data.OptWorldMap;
import com.lenovo.newdevice.tangocar.path.finder.data.TileMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.data.WorldMap;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicEnum;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEnum;
import com.lenovo.newdevice.tangocar.path.finder.provider.FinderSettings;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Observable;
import java.util.Observer;
import java.util.Queue;


/**
 * Proxy class to make it easy to call Java or Jni Finders.
 * use {@link Builder} to build and 'Instance' and call {@link #findAsync()}
 */
public class PathFinder {

    public static final int CAR_SIZE_IGNORED = 0;

    private BaseMap map;

    private FinderListener finderListener;

    private long timeout;

    private boolean nativeAlgo;

    private FinderSettings settings;

    private AbortSignal signal;

    private PathFinder(Context context,
                       BaseMap map,
                       FinderListener finderListener,
                       AbortSignal signal,
                       long timeout,
                       boolean nativeAlgo) {
        this.map = map;
        this.finderListener = finderListener;
        this.timeout = timeout;
        this.settings = FinderSettings.from(context);
        this.signal = signal;
        this.nativeAlgo = nativeAlgo;
    }

    /**
     * @return Instance of {@link Builder}.
     */
    public static Builder builder(Context context) {
        return new Builder(context);
    }

    /**
     * Called to find path in background.
     */
    public void findAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
                getCaller().call();
            }
        }).start();
    }

    private Caller getCaller() {
        return nativeAlgo ? new CallerImplNative() : new CallerImplJava();
    }

    public enum FailureCause {
        INVALID_START,
        INVALID_GOAL,
        GENERIC_FAILURE
    }

    public interface FinderListener {
        void onStart();

        void onCancelled();

        void onTimeout();

        void onPathFound(@NonNull Queue<Point> pathPoints);

        void onPathNotFound(@NonNull FailureCause cause);

        class Stub implements FinderListener {
            @Override
            public void onStart() {

            }

            @Override
            public void onCancelled() {

            }

            @Override
            public void onTimeout() {

            }

            @Override
            public void onPathFound(@NonNull Queue<Point> pathPoints) {

            }

            @Override
            public void onPathNotFound(@NonNull FailureCause cause) {

            }
        }
    }

    private interface Caller {
        @WorkerThread
        void call();
    }

    public static class Builder {

        private WeightedPoint from, to;

        private BaseMap map;

        private FinderListener finderListener;

        private AbortSignal signal;

        private int carSize = CAR_SIZE_IGNORED;

        private long timeout;

        private boolean nativeAlgo;

        private Context context;

        private Builder(Context context) {
            this.context = context;
        }

        public Builder from(Point from) {
            this.from = new WeightedPoint(from.x, from.y);
            return this;
        }

        public Builder to(Point to) {
            this.to = new WeightedPoint(to.x, to.y);
            return this;
        }

        public Builder map(@NonNull BaseMap baseMap) {
            if (from == null || to == null) {
                throw new IllegalArgumentException("Set from & to first.");
            }

            this.map = baseMap;

            return this;
        }

        public Builder map(@NonNull GridMap gridMap) {
            if (from == null || to == null) {
                throw new IllegalArgumentException("Set from & to first.");
            }

            if (carSize == CAR_SIZE_IGNORED) {
                this.map = new WorldMap(gridMap, from, to);
            } else {
                this.map = new OptWorldMap(gridMap, from, to, carSize);
            }

            return this;
        }

        public Builder map(@NonNull TileMap tileMap) {
            if (carSize != CAR_SIZE_IGNORED) {
                this.map = new OptTileMap(tileMap, carSize);
            } else {
                this.map = tileMap;
            }
            return this;
        }

        public Builder listener(@NonNull FinderListener finderListener) {
            this.finderListener = finderListener;
            return this;
        }

        public Builder carSize(int size) {
            if (size < CAR_SIZE_IGNORED) throw new IllegalArgumentException("Should be positive");
            this.carSize = size;
            return this;
        }

        public Builder timeout(long timeoutMills) {
            if (timeoutMills < 0) throw new IllegalArgumentException("Should be positive");
            this.timeout = timeoutMills;
            return this;
        }

        public Builder nativeAlgo(boolean nativeAlgo) {
            this.nativeAlgo = nativeAlgo;
            return this;
        }

        public Builder abortSignal(@NonNull AbortSignal signal) {
            this.signal = signal;
            return this;
        }

        public PathFinder build() {
            if (finderListener == null) {
                throw new IllegalArgumentException("Set a receiver first.");
            }
            // Wrap it to opt world map.
            if (this.carSize != CAR_SIZE_IGNORED && !(this.map instanceof OptWorldMap) && this.map instanceof WorldMap) {
                WorldMap worldMap = (WorldMap) map;
                this.map = new OptWorldMap(worldMap, carSize);
            }
            // Wrap it to opt tile map.
            if (this.carSize != CAR_SIZE_IGNORED && !(this.map instanceof OptTileMap) && this.map instanceof TileMap) {
                TileMap tileMap = (TileMap) map;
                this.map = new OptTileMap(tileMap, carSize);
            }
            return new PathFinder(context, map, finderListener, signal, timeout, nativeAlgo);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "from=" + from +
                    ", to=" + to +
                    ", carSize=" + carSize +
                    ", setTimeout=" + timeout +
                    ", nativeAlgo=" + nativeAlgo +
                    ", map clz=" + map.getClass() +
                    '}';
        }
    }

    private abstract class BaseCaller implements Caller {

        boolean checkInput() {

            if (settings.validateStart() && !map.isStartTraversable()) {
                finderListener.onPathNotFound(FailureCause.INVALID_START);
                return false;
            }

            if (settings.validateGoal() && !map.isGoalTraversable()) {
                finderListener.onPathNotFound(FailureCause.INVALID_GOAL);
                return false;
            }

            return true;
        }

        @Override
        public void call() {
            if (!checkInput()) return;

            finderListener.onStart();

            final Finder finder = onCreateFinder();

            NeighborEnum selector = settings.neighborEnum();

            HeuristicEnum scheme = settings.heuristicEnum();

            finder.setNeighborSelector(selector);
            finder.setHeuristic(scheme);

            boolean shuffle = settings.shuffle();
            finder.setShuffle(shuffle);

            boolean dijkstra = settings.dijkstra();
            finder.setDijkstra(dijkstra);

            if (signal != null) signal.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    finder.cancel();
                }
            });

            if (timeout > 0) {
                finder.setTimeout(timeout);
            }

            FinderStatus status = finder.solve();

            Utils.outLog("Finder status:" + status);

            // Notify listener.
            switch (status) {
                case CANCELED:
                    finderListener.onCancelled();
                    break;
                case COMPLETED_FOUND:
                    finderListener.onPathFound(finder.getPath());
                    break;
                case COMPLETED_NOT_FOUND:
                    finderListener.onPathNotFound(FailureCause.GENERIC_FAILURE);
                    break;
                case TIMEOUT:
                    finderListener.onTimeout();
                    break;
            }

            signal.deleteObservers();
        }


        abstract Finder onCreateFinder();
    }

    private class CallerImplJava extends BaseCaller {

        @Override
        Finder onCreateFinder() {
            return new FinderJ(map);
        }
    }

    private class CallerImplNative extends BaseCaller {
        @Override
        Finder onCreateFinder() {
            return new FinderC(map);
        }
    }
}
