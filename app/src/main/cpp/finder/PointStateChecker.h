//
// Created by guohao4 on 2017/3/17.
//

#ifndef TANGO_CAR_POINTSTATECHECKER_H
#define TANGO_CAR_POINTSTATECHECKER_H

#include <functional>
#include "WeightedPoint.h"

typedef std::function<bool(const WeightedPoint &)> PointStateChecker;

#endif //TANGO_CAR_POINTSTATECHECKER_H
