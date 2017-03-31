//
// Created by guohao4 on 2017/3/18.
//

#include <chrono>
#include "BenchMarkTest.h"
#include "../finder/PathFinder.h"

void BenchMark::testLargeMap() {

    PathFinder *finder = new PathFinder;

    finder->setNeighborSelector(new NeighborSelectorFourDirection);
    finder->setHeuristic(new HeuristicManhanttan);
    finder->setChecker([&](const WeightedPoint &pos) -> bool {
        return true;
    });


    finder->setDijkstra(false);
    finder->setShuffle(false);
    finder->setMaxStep(1024 * 1024);

    finder->setStart(new WeightedPoint(0, 0));
    finder->setGoal(new WeightedPoint(10000, -10000));

    auto start_time = std::chrono::system_clock::now();

    finder->resolve();

    auto end_time = std::chrono::system_clock::now();

    LOGD("Got paths %d, within steps%d ", int(finder->getPath().size()), finder->getStepCount());

    LOGD("Takes %d mills",
         (int) std::chrono::duration_cast<std::chrono::milliseconds>(
                 end_time - start_time).count());
}

