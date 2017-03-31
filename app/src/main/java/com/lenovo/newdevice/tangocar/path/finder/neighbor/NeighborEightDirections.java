package com.lenovo.newdevice.tangocar.path.finder.neighbor;

import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicScheme;

import java.util.List;

/**
 * Neighbor selector that returns all eight adjacent tiles: N, S, E, W, NE, SE, NW, SW
 */
public class NeighborEightDirections extends NeighborSelector {
    @Override
    public List<WeightedPoint> getNeighbors(BaseMap map, WeightedPoint cursor, HeuristicScheme distanceCalculator) {
        List<WeightedPoint> neighbors = (new NeighborFourDirections()).getNeighbors(map, cursor, distanceCalculator);

        // If the neighbor is not out of bounds and that the neighbor is traversable, then add it to the list
        if (map.isTraversable(cursor.getX() - 1, cursor.getY() - 1)) {
            neighbors.add(new WeightedPoint(cursor.getX() - 1, cursor.getY() - 1)); // Northwest
        }
        if (map.isTraversable(cursor.getX() + 1, cursor.getY() + 1)) {
            neighbors.add(new WeightedPoint(cursor.getX() + 1, cursor.getY() + 1)); // Southeast
        }
        if (map.isTraversable(cursor.getX() + 1, cursor.getY() - 1)) {
            neighbors.add(new WeightedPoint(cursor.getX() + 1, cursor.getY() - 1)); // Southwest
        }
        if (map.isTraversable(cursor.getX() - 1, cursor.getY() + 1)) {
            neighbors.add(new WeightedPoint(cursor.getX() - 1, cursor.getY() + 1)); // Northeast
        }

        return neighbors;
    }

    @Override
    public String getLabel() {
        return "8-Directional";
    }

    @Override
    public String getExplanation() {
        return "Neighbors are selected from diagonally adjacent tiles in " +
                "addition to the north, east, south, and west directions. " +
                "This works best with the Euclidean heuristic. Combining " +
                "this neighbor selection scheme with the Manhattan " +
                "heuristic can yield strange results because a diagonal " +
                "path over a single tile is counted as a distance of 2.";

    }
}
