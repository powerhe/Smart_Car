package com.lenovo.newdevice.tangocar.path.finder.generator;

import com.lenovo.newdevice.tangocar.path.finder.data.TileMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;

import java.util.Random;

/**
 * Randomly generates a tilemap
 */
public class MapManager {
    /**
     * The single permitted instance of this class
     */
    private static final MapManager onlyOne = new MapManager();
    // Minimum and maximum tile map dimensions
    private static final int MIN_HEIGHT = 50;
    private static final int MAX_HEIGHT = 100;
    private static final int MIN_WIDTH = MIN_HEIGHT;
    private static final int MAX_WIDTH = MAX_HEIGHT;
    /**
     * The stored seed for map generation, initialized to some random value and incremented when the find button is pressed
     */
    private int mapSeed = new Random().nextInt(10000);
    /**
     * The random number generator used to produce the varience in the tile maps
     */
    private Random rnd;

    /**
     * Holds the implementation of the map generator scheme that is currently being used
     */
    private MapGenerator generator = new MapGenerator();

    /**
     * Private default constructor
     */
    private MapManager() {
        this.rnd = new Random(mapSeed);
    }

    /**
     * Get the one instance of the MapManager class
     *
     * @return onlyOne
     */
    public static MapManager getInstance() {
        return onlyOne;
    }

    /**
     * Set the map generator
     *
     * @param generator
     */
    public void setGenerator(MapGenerator generator) {
        this.generator = generator;
    }

    /**
     * Generate a random TileMap
     *
     * @param increment whether to increment the random seed
     * @return new TileMap
     */
    public TileMap generate(boolean increment) {
        if (increment) {
            mapSeed++;
        }

        return generate(mapSeed);
    }

    /**
     * Generate a random TileMap
     *
     * @param seed The random number generator seed to use
     * @return new TileMap
     */
    public TileMap generate(int seed) {
        rnd = new Random(seed);
        this.mapSeed = seed;

        boolean[][] map = new boolean[rnd.nextInt(MAX_HEIGHT - MIN_HEIGHT) + MIN_HEIGHT][rnd.nextInt(MAX_WIDTH - MIN_WIDTH) + MIN_WIDTH];

        WeightedPoint start = generator.generatePoint(rnd, map);
        WeightedPoint goal = generator.generatePoint(rnd, map);

        // Generate random blocks on the map
        generator.addObstacles(rnd, map, start, goal);

        // Put a border on all maps
        generator.addBorder(map);

        return new TileMap(map, start, goal, seed);
    }
}
