//
// Created by guohao4 on 2017/3/17.
//

#include "WeightedPoint.h"

WeightedPoint::WeightedPoint(int inX, int inY) : fromCost(0), toCost(0), prev(NULL) {
    x = inX;
    y = inY;
}

int WeightedPoint::getX() const {
    return x;
}

int WeightedPoint::getY() const {
    return y;
}

int WeightedPoint::getFromCost() const {
    return fromCost;
}

int WeightedPoint::getToCost() const {
    return toCost;
}

void WeightedPoint::setFromCost(int c) {
    fromCost = c;
}

void WeightedPoint::setToCost(int c) {
    toCost = c;
}

WeightedPoint *WeightedPoint::getParent() {
    return prev;
}

void WeightedPoint::setPrev(WeightedPoint *p) {
    prev = p;
}

int WeightedPoint::getCost() const {
    return fromCost + toCost;
}

std::string WeightedPoint::toString() {
    char container[128] = {0};
    sprintf(container, "%s%d%s%d%s%d%s, ", "Point{", x, ", ", y, ", @", getCost(), "}");
    return std::string(container);
}







