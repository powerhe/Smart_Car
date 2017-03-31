//
// Created by guohao4 on 2017/3/17.
//

#include <string>
#include "WeightedPoint.h"

#include <android/log.h>
#include <cstdlib>
#include <cmath>
#include "../common/Logger.h"

#ifndef TANGO_CAR_HEURISTIC_H
#define TANGO_CAR_HEURISTIC_H

class Heuristic {

public:

    // Get the distance between the specified points for the extended heuristic scheme
    virtual int distance(WeightedPoint *one, WeightedPoint *two) = 0;

    virtual std::string getLabel() = 0;

};

class HeuristicManhanttan : public Heuristic {

public:
    virtual int distance(WeightedPoint *one, WeightedPoint *two) override {
        return abs(two->getY() - one->getY()) + abs(two->getX() - one->getX());
    }

    std::string getLabel() {
        return "Manhantta";
    }
};

class HeuristicChebyshev : public Heuristic {

public:
    virtual int distance(WeightedPoint *one, WeightedPoint *two) override {
        int dx = abs(one->getX() - two->getX());
        int dy = abs(one->getY() - two->getY());
        return (int) fmax(dx, dy);
    }

    virtual std::string getLabel() override {
        return "Chebyshev";
    }
};

class HeuristicDiagonal : public Heuristic {

public:
    virtual int distance(WeightedPoint *one, WeightedPoint *two) override {

        int dx = abs(one->getX() - two->getX());
        int dy = abs(one->getY() - two->getY());

        return (int) ((dx < dy) ? sqrt(2.0) * dx + (dy - dx) : sqrt(2.0) * dy + (dx - dy));
    }

    virtual std::string getLabel() override {
        return "Diagonal";
    }
};

class HeuristicEuclidean : public Heuristic {

public:
    virtual int distance(WeightedPoint *one, WeightedPoint *two) override {
        return (int) sqrt((one->getY() - two->getY()) * (one->getY() - two->getY())
                          + (one->getX() - two->getX()) * (one->getX() - two->getX()));
    }

    virtual std::string getLabel() override {
        return "Euclidean";
    }
};

class HeuristicSquared : public Heuristic {

public:
    virtual int distance(WeightedPoint *one, WeightedPoint *two) override {
        float dx = one->getY() - two->getY();
        float dy = one->getX() - two->getX();

        return (int) (dx * dx + dy * dy);
    }

    virtual std::string getLabel() override {
        return "Squared";
    }
};


#endif //TANGO_CAR_HEURISTIC_H