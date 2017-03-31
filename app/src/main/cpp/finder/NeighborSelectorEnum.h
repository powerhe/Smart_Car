//
// Created by guohao4 on 2017/3/19.
//

#ifndef TANGO_CAR_NEIGHBORSELECTORENUM_H
#define TANGO_CAR_NEIGHBORSELECTORENUM_H

#include "NeighborSelector.h"

enum NeighborSelectorEnum {
    NeighborFourDirections,
    NeighborEightDirections,
    NeighborJumpPoint
};

NeighborSelector *buildSelector(int selectorEnum) {
    switch (selectorEnum) {
        case NeighborFourDirections:
            return new NeighborSelectorFourDirection();
        case NeighborEightDirections:
            return new NeighborSelectorEightDirection();
        case NeighborJumpPoint:
            return new NeighborSelectorJumpPoint();
        default:
            return nullptr;
    }
};


#endif //TANGO_CAR_NEIGHBORSELECTORENUM_H
