package com.lenovo.newdevice.tangocar.path.finder.data;

public interface BaseMap {
    /**
     * Get the starting point for this tile map
     *
     * @return start
     */
    public WeightedPoint getStart();

    /**
     * Get the goal point for this tile map
     *
     * @return goal
     */
    public WeightedPoint getGoal();

    /**
     * Get the seed used on the random number generator to build this map
     *
     * @return seed
     */
    public int getSeed();

    /**
     * Whether the point is a traversable point on this tile map
     *
     * @param wp the point to check
     * @return traversable
     */
    public boolean isTraversable(WeightedPoint wp);

    /**
     * Whether the move is a valid on this tile map
     */
    public boolean isTraversable(int x, int y);

    /**
     * Set whether the move is a valid on this tile map
     */
    public void setTraversable(int x, int y, boolean traversable);

    /**
     * Get the number of rows
     *
     * @return number of rows
     */
    public int getWidth();

    /**
     * Get the number of columns
     *
     * @return number of columns
     */
    public int getHeight();

    /**
     * Get the map statistics to be displayed on the status bar
     *
     * @return map statistics
     */
    public String getMapStats();

    public boolean isStartTraversable();

    public boolean isGoalTraversable();
}
