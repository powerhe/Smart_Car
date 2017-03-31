package com.lenovo.newdevice.tangocar.path.finder.data;

import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.map.GridMap;

public class TileMap implements BaseMap {

    private static final int MAX_MAP_SIZE = 250;

    /**
     * Whether the start and goal points are swapped
     */
    private static boolean endPointSwap = false;
    /**
     * The seed used on the random number generator to build this map
     */
    private final int seed;
    /**
     * The grid of tiles where TRUE is non-traversable and FALSE is traversable
     */
    protected boolean[][] map;
    /**
     * The starting point
     */
    private WeightedPoint start;
    /**
     * The goal point
     */
    private WeightedPoint goal;

    /**
     * Construct a TileMap with the specified boolean map of TRUE non-traversable tiles
     */
    public TileMap(boolean[][] map, WeightedPoint start, WeightedPoint goal, int seed) {
        this.map = map;
        this.start = start;
        this.goal = goal;
        this.seed = seed;
    }

    /**
     * Construct a TileMap with the specified int map where 0 is a traversable tile, all other numbers are non-traversable tiles
     */
    public TileMap(int[][] map, WeightedPoint start, WeightedPoint goal, int seed) {
        this(convertIntMap(map), start, goal, seed);
    }

    /**
     * Construct a TileMap with the specified grid map where 0 is a traversable tile, all other numbers are non-traversable tiles
     */
    public TileMap(GridMap map, WeightedPoint start, WeightedPoint goal, int seed) {
        this(convertIntMap(map), start, goal, seed);
    }

    /**
     * Construct a TileMap with the specified int map where all non-zero numbers are non-traversable tiles
     */
    public static boolean[][] convertIntMap(int[][] imap) {
        boolean[][] map = new boolean[imap.length][imap[0].length];
        for (int r = 0; r < imap.length; r++) {
            for (int c = 0; c < imap[r].length; c++) {
                map[r][c] = imap[r][c] != 0;
            }
        }
        return map;
    }

    /**
     * FIXME Check the row?
     * Construct a TileMap with the specified grid map where all non-zero numbers are non-traversable tiles
     */
    public static boolean[][] convertIntMap(GridMap gridMap) {
        boolean[][] map = new boolean[MAX_MAP_SIZE][MAX_MAP_SIZE];
        for (int r = 0; r < MAX_MAP_SIZE; r++) {
            for (int c = 0; c < MAX_MAP_SIZE; c++) {
                GridInfo info = gridMap.getGridInfo(c, r, false);
                map[r][c] = info != null && info.getType() == GridInfo.TYPE_BORDER;
            }
        }
        return map;
    }

    /**
     * Set whether the end points should be swapped
     *
     * @param endPointSwap
     */
    public static void setEndPointSwap(boolean endPointSwap) {
        TileMap.endPointSwap = endPointSwap;
    }

    /**
     * Get the starting point for this tile map
     *
     * @return start
     */
    public WeightedPoint getStart() {
        return TileMap.endPointSwap ? goal : start;
    }

    /**
     * Get the goal point for this tile map
     *
     * @return goal
     */
    public WeightedPoint getGoal() {
        return TileMap.endPointSwap ? start : goal;
    }

    /**
     * Get the seed used on the random number generator to build this map
     *
     * @return seed
     */
    public final int getSeed() {
        return seed;
    }

    /**
     * Whether the point is a traversable point on this tile map
     *
     * @param wp the point to check
     * @return traversable
     */
    public boolean isTraversable(WeightedPoint wp) {
        return isTraversable(wp.getX(), wp.getY());
    }

    /**
     * Whether the move is a valid on this tile map
     */
    public boolean isTraversable(int row, int col) {
        return !(row < 0 || col < 0) && !(row >= MAX_MAP_SIZE || col >= MAX_MAP_SIZE) && !map[row][col];
    }

    /**
     * Set whether the move is a valid on this tile map
     */
    public void setTraversable(int row, int col, boolean traversable) {
        map[row][col] = !traversable;
    }

    /**
     * Get the number of rows
     *
     * @return number of rows
     */
    public int getWidth() {
        return map.length;
    }

    /**
     * Get the number of columns
     *
     * @return number of columns
     */
    public int getHeight() {
        return map[0].length;
    }

    /**
     * Get the map statistics to be displayed on the status bar
     *
     * @return map statistics
     */
    public String getMapStats() {
        return "Seed:" + seed + "   w:" + getHeight() + " h:" + getWidth() + "   Start:" + start.toString() + ", Goal:" + goal.toString();
    }

    @Override
    public boolean isStartTraversable() {
        return true;
    }

    @Override
    public boolean isGoalTraversable() {
        return true;
    }
}
