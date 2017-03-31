package com.lenovo.newdevice.tangocar.path.finder.heuristic;

import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;

/**
 * Euclidean distance heuristic
 */
public class HeuristicEuclidean extends HeuristicScheme {
    @Override
    public float distance(WeightedPoint one, WeightedPoint two) {
        return (float) Math.sqrt((one.getY() - two.getY()) * (one.getY() - two.getY()) + (one.getX() - two.getX()) * (one.getX() - two.getX()));
    }

    @Override
    public String getLabel() {
        return "Euclidean";
    }

    @Override
    public String getExplanation() {
        return "This is normal distance calculated by the Pythagorean formula:" +
                "d = sqrt( (ax - bx)2 + (ay - by)2)";
    }
}
