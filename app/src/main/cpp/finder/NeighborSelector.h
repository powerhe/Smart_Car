//
// Created by guohao4 on 2017/3/17.
//

#ifndef TANGO_CAR_NEIGHBORSELECTOR_H
#define TANGO_CAR_NEIGHBORSELECTOR_H

#include <vector>
#include <string>
#include "WeightedPoint.h"
#include <math.h>

class NeighborSelector {
public:

    virtual std::vector<WeightedPoint *> getNeighbors(WeightedPoint *target) = 0;

    virtual std::string getLabel() = 0;
};

class NeighborSelectorFourDirection : public NeighborSelector {

public:

    virtual std::vector<WeightedPoint *> getNeighbors(WeightedPoint *target) override {
        std::vector<WeightedPoint *> neighbors;

        // North
        WeightedPoint *n = new WeightedPoint(target->getX() - 1, target->getY());
        neighbors.push_back(n);

        // East
        WeightedPoint *e = new WeightedPoint(target->getX(), target->getY() + 1);
        neighbors.push_back(e);

        // South
        WeightedPoint *s = new WeightedPoint(target->getX() + 1, target->getY());
        neighbors.push_back(s);

        // West
        WeightedPoint *w = new WeightedPoint(target->getX(), target->getY() - 1);
        neighbors.push_back(w);

        return neighbors;
    }

    virtual std::string getLabel() override {
        return "NeighborSelector-4 Direction";
    }


};

class NeighborSelectorEightDirection : public NeighborSelector {

public:

    virtual std::vector<WeightedPoint *> getNeighbors(WeightedPoint *target) override {
        std::vector<WeightedPoint *> neighbors;

        // North
        WeightedPoint *n = new WeightedPoint(target->getX() - 1, target->getY());
        neighbors.push_back(n);

        // East
        WeightedPoint *e = new WeightedPoint(target->getX(), target->getY() + 1);
        neighbors.push_back(e);

        // South
        WeightedPoint *s = new WeightedPoint(target->getX() + 1, target->getY());
        neighbors.push_back(s);

        // West
        WeightedPoint *w = new WeightedPoint(target->getX(), target->getY() - 1);
        neighbors.push_back(w);

        // Northwest
        WeightedPoint *nw = new WeightedPoint(target->getX() - 1, target->getY() - 1);
        neighbors.push_back(nw);

        // Southeast
        WeightedPoint *se = new WeightedPoint(target->getX() + 1, target->getY() + 1);
        neighbors.push_back(se);

        // Southwest
        WeightedPoint *sw = new WeightedPoint(target->getX() + 1, target->getY() - 1);
        neighbors.push_back(sw);

        // Northeast
        WeightedPoint *ne = new WeightedPoint(target->getX() - 1, target->getY() + 1);
        neighbors.push_back(ne);

        return neighbors;
    }

    virtual std::string getLabel() override {
        return "NeighborSelector-8 Direction";
    }


};

class NeighborSelectorJumpPoint : public NeighborSelector {

public:

    virtual std::vector<WeightedPoint *> getNeighbors(WeightedPoint *target) override {
        std::vector<WeightedPoint *> empty;
        return empty;
    }

    virtual std::string getLabel() override {
        return "NeighborSelector-JumpPoint";
    }


};

#endif //TANGO_CAR_NEIGHBORSELECTOR_H

