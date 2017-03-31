//
// Created by guohao4 on 2017/3/18.
//

#include "HeapTest.h"
#include "../common/Logger.h"
#include "../finder/WeightedPoint.h"
#include "../finder/Heap.h"


void HeapTest::start() {

    // WP
    WeightedPoint *p2 = new WeightedPoint(1, 1);
    p2->setFromCost(1);
    p2->setToCost(1);

    WeightedPoint *p3 = new WeightedPoint(2, 2);
    p3->setFromCost(2);
    p3->setToCost(2);

    WeightedPoint *p4 = new WeightedPoint(2, 2);
    p4->setFromCost(9);
    p4->setToCost(10);

    Heap<WeightedPoint> heapWP;
    WeightedPoint *p1 = new WeightedPoint(0, 0);
    p1->setFromCost(0);
    p1->setToCost(0);

    heapWP.push(p1);
    heapWP.push(p3);
    heapWP.push(p2);
    heapWP.push(p4);

    int size = heapWP.size();
    LOGD("size %d", size);

    while (!heapWP.empty()) {
        LOGD("min %s", heapWP.peek()->toString().c_str());
        heapWP.pop();
    }

    LOGD("--------");
}

