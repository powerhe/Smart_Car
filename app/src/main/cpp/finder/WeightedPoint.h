//
// Created by guohao4 on 2017/3/17.
//

#include <string>
#include <android/log.h>
#include "../common/Logger.h"

#ifndef TANGO_CAR_WEIGHTEDPOINT_H
#define TANGO_CAR_WEIGHTEDPOINT_H

class WeightedPoint {

public:
    WeightedPoint(int x, int y);

public:

    int getX() const;

    int getY() const;

    int getFromCost() const;

    int getToCost() const;

    void setFromCost(int cost);

    void setToCost(int cost);

    int getCost() const;

    WeightedPoint *getParent();

    void setPrev(WeightedPoint *p);

    std::string toString();

    static bool reverstCompare(const WeightedPoint *w1, const WeightedPoint *w2) {
        return w1->getCost() > w2->getCost();
    }

    bool operator==(const WeightedPoint &that) const {
        return x == that.x && y == that.y;
    }

    bool operator>(const WeightedPoint &that) const {
        return getCost() > that.getCost();
    }

    bool operator<(const WeightedPoint &that) const {
        return getCost() < that.getCost();
    }

    bool operator()(const WeightedPoint &that) const {
        return getCost() < that.getCost();
    }

private:

    int x;
    int y;

/**
 * The cost from a starting point to have reached this point (g)
 */
    int fromCost;

/**
 * The estimated cost to go from this point to a goal (h)
 */
    int toCost;

/**
 * A link to the previous point in a linked list of points tracing the path from this point back to the start
 */
    WeightedPoint *prev;

};


#endif //TANGO_CAR_WEIGHTEDPOINT_H
