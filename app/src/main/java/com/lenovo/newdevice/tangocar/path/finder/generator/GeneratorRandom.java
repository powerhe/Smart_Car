package com.lenovo.newdevice.tangocar.path.finder.generator;

import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;

import java.util.Random;


/**
 * Generate a 2D Boolean map with random point obstacles
 */
public class GeneratorRandom extends MapGenerator {
    @Override
    public void addObstacles(Random rnd, boolean[][] map, WeightedPoint start, WeightedPoint goal) {
        final int pointCount = Math.max(1, (map.length * map[0].length) / (2 + rnd.nextInt(4)));
        int r, c;
        for (int i = 0; i < pointCount; i++) {
            r = rnd.nextInt(Math.max(1, map.length));
            c = rnd.nextInt(Math.max(1, map[0].length));
            if ((start.getY() != c || start.getX() != r) && (goal.getY() != c || goal.getX() != r)) {
                map[r][c] = true;
            }
        }
    }

    @Override
    public String getLabel() {
        return "Random Points";
    }

}
