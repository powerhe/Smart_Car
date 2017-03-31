package com.lenovo.newdevice.tangocar.path.finder.neighbor;

import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicScheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Neighbor selector that returns adjacent tiles excluding diagonal moves: N, S, E, W
 */
public class NeighborFourDirections extends NeighborSelector {
    @Override
    public List<WeightedPoint> getNeighbors(BaseMap map, WeightedPoint cursor, HeuristicScheme distanceCalculator) {
        List<WeightedPoint> neighbors = new ArrayList<>();

        // If the neighbor is not out of bounds and that the neighbor is traversable, then add it to the list
        if (map.isTraversable(cursor.getX() - 1, cursor.getY())) {
            neighbors.add(new WeightedPoint(cursor.getX() - 1, cursor.getY())); // North
        }
        if (map.isTraversable(cursor.getX(), cursor.getY() + 1)) {
            neighbors.add(new WeightedPoint(cursor.getX(), cursor.getY() + 1)); // East
        }
        if (map.isTraversable(cursor.getX() + 1, cursor.getY())) {
            neighbors.add(new WeightedPoint(cursor.getX() + 1, cursor.getY())); // South
        }
        if (map.isTraversable(cursor.getX(), cursor.getY() - 1)) {
            neighbors.add(new WeightedPoint(cursor.getX(), cursor.getY() - 1)); // West
        }

        return neighbors;
    }

    @Override
    public String getLabel() {
        return "4-directional";
    }

    @Override
    public String getExplanation() {
        return "Neighbors are selected from the traversable tiles to the north, east, south, and west only. This works best with the Manhattan heuristic";
    }
}
