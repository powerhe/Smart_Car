package com.lenovo.newdevice.tangocar.path.finder.neighbor;

import com.lenovo.newdevice.tangocar.path.finder.data.BaseMap;
import com.lenovo.newdevice.tangocar.path.finder.data.TileMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicScheme;

import java.util.List;

/**
 * Selects the group of neighboring points to be considered in the next step of the algorithm
 */
public abstract class NeighborSelector {
    /**
     * Get all the neighbor selectors that extend this base abstract class
     *
     * @return neighborSelectors
     */
    public static NeighborSelector[] getAllNeighborSelectors() {
        return new NeighborSelector[]{new NeighborFourDirections(), new NeighborEightDirections(), new NeighborJumpPoint()};
    }

    /**
     * Returns a list of neighboring points to be considered in the next step of the algorithm
     *
     * @param map
     * @param cursor
     * @param distanceCalculator
     * @return neighbors
     */
    public abstract List<WeightedPoint> getNeighbors(BaseMap map, WeightedPoint cursor, HeuristicScheme distanceCalculator);

    /**
     * Get the label
     *
     * @return label
     */
    public abstract String getLabel();

    /**
     * Get an HTML formatted String explaining the neighbor selector
     *
     * @return explanation
     */
    public abstract String getExplanation();

    public String toString() {
        return getLabel();
    }
}
