package com.lenovo.newdevice.tangocar.path.finder.data;

public class OptTileMap extends TileMap {

    private int mCarSize;

    public OptTileMap(boolean[][] map, WeightedPoint start, WeightedPoint goal, int seed, int carSize) {
        super(map, start, goal, seed);
        this.mCarSize = carSize;
    }

    public OptTileMap(int[][] map, WeightedPoint start, WeightedPoint goal, int seed, int carSize) {
        super(map, start, goal, seed);
        this.mCarSize = carSize;
    }

    public OptTileMap(TileMap map, int carSize) {
        this(map.map, map.getStart(), map.getGoal(), map.getSeed(), carSize);
    }

    @Override
    public boolean isTraversable(int x, int y) {
        boolean isTraversable = super.isTraversable(x, y);

        int offset = (mCarSize - 1) / 2;

        if (offset == 0) return isTraversable;

        if (isTraversable) {
            isTraversable = super.isTraversable(x + offset, y + offset);
        }

        if (isTraversable) {
            isTraversable = super.isTraversable(x - offset, y - offset);
        }

        if (isTraversable) {
            isTraversable = super.isTraversable(x + offset, y - offset);
        }

        if (isTraversable) {
            isTraversable = super.isTraversable(x - offset, y + offset);
        }

        return isTraversable;
    }

}
