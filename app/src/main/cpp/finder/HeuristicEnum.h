//
// Created by guohao4 on 2017/3/18.
//

#ifndef TANGO_CAR_HEURISTICENUM_H
#define TANGO_CAR_HEURISTICENUM_H

#include "Heuristic.h"

enum HeuristicEnum {
    Squared,
    Chebyshev,
    Diagonal,
    Euclidean,
    Manhattan,
};

Heuristic *buildHeuristic(int heuristicEnum) {
    switch (heuristicEnum) {
        case Manhattan:
            return new HeuristicManhanttan();
        case Chebyshev:
            return new HeuristicChebyshev();
        case Diagonal:
            return new HeuristicDiagonal();
        case Euclidean:
            return new HeuristicEuclidean();
        case Squared:
            return new HeuristicSquared();
        default:
            return nullptr;
    }
}

#endif //TANGO_CAR_HEURISTICENUM_H
