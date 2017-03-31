//
// Created by guohao4 on 2017/3/17.
//

#include <android/log.h>
#include <algorithm>
#include "FinderApiTest.h"
#include "../finder/PathFinder.h"
#include "BenchMarkTest.h"
#include "../common/Preconditions.h"

void FinderTest::start() {

    WeightedPoint* pNoNull = new WeightedPoint(1, 1);

    checkNoneNull(pNoNull, "P is null");

    LOGI("---------------TEST START----------------");

    BenchMark benchMark;
    benchMark.testLargeMap();


    LOGI("---------------TEST START----------------");

    PathFinder finder;

    WeightedPoint *start = new WeightedPoint(9, 8);
    finder.setStart(start);

    int startX = finder.getStart()->getX();
    int startY = finder.getStart()->getY();

    LOGD("startX %d - Y %d", startX, startY);

    LOGD("Status: %d", finder.getStatus());

    Heuristic *heuristic = new HeuristicManhanttan();

    finder.setHeuristic(heuristic);

    WeightedPoint *testStart = new WeightedPoint(1, 1);
    LOGD("%s", testStart->toString().c_str());
    int distance = finder.getHeuristic()->distance(testStart, testStart);

    LOGD("distance: %d", distance);
    LOGD("heuristic %s", finder.getHeuristic()->getLabel().c_str());


    NeighborSelector *fourDirection = new NeighborSelectorFourDirection();
    finder.setNeighborSelector(fourDirection);

    WeightedPoint *neighborAt0 = finder.getNeighborSelector()->getNeighbors(testStart).at(0);
    LOGD("neighborAt0 X %d - Y %d", neighborAt0->getX(), neighborAt0->getY());
    LOGD("neighbor %s", finder.getNeighborSelector()->getLabel().c_str());

    finder.setChecker([&](const WeightedPoint &pos) -> bool {
        return pos.getX() < 20 && pos.getY() < 20;
    });

    WeightedPoint testCheckPoint(8, 8);
    PointStateChecker checker = finder.getPointChecker();
    bool passed = checker(testCheckPoint);
    LOGD("Checker, passed: %s", passed ? "TRUE" : "FALSE");


    LOGI("---------------++++++----------------");


    WeightedPoint *s = new WeightedPoint(9, 10);
    WeightedPoint *e = new WeightedPoint(-10, -21);

    finder.setStart(s);
    finder.setGoal(e);

    finder.setDijkstra(false);
    finder.setShuffle(false);

    finder.setMaxStep(100);

    FinderStatus status = finder.resolve();
    LOGD("Status: %d with steps: %d", status, finder.getStepCount());

    if (status == COMPLETED_FOUND) {
        std::vector<WeightedPoint *> paths = finder.getPath();
        int size = paths.size();
        LOGD("Size %d", size);

        for_each(paths.begin(), paths.end(),
                 [](WeightedPoint *s) { LOGD("---%d, $=%d", s->getX(), s->getY()); });
    }


    LOGI("---------------TEST END----------------");


}

