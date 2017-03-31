package com.lenovo.newdevice.tangocar.path.finder.heuristic;

import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;

public class HeuristicDiagonal extends HeuristicScheme {

    public static final float DIAGONAL_SCALE = (float) Math.sqrt(2.0);

    @Override
    public float distance(WeightedPoint one, WeightedPoint two) {
        int dx = Math.abs(one.getX() - two.getX());
        int dy = Math.abs(one.getY() - two.getY());

        return (dx < dy) ?
                DIAGONAL_SCALE * dx + (dy - dx) :
                DIAGONAL_SCALE * dy + (dx - dy);
    }

    @Override
    public String getLabel() {
        return "Diagonal";
    }

    @Override
    public String getExplanation() {
        return "Diagonal distance is a perfect pair with the 8-directional " +
                "neighbor selection scheme. It performs a perfect measure in " +
                "distance when movement is restricted entirely to horizontal, " +
                "vertical, and diagonal motion.";
    }
}
