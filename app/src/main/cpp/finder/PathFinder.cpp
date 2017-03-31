//
// Created by guohao4 on 2017/3/17.
//

#include <android/log.h>
#include <algorithm>
#include "PathFinder.h"
#include "../common/Preconditions.h"
#include "TimeoutTracker.h"

#define DEBUG_VERBOSE_NOOP
#define DEBUG_DUMP_PARAMS
#define PARAMS_STRICT_CHECK

PathFinder::PathFinder() : stepCount(0),
                           maxStep(1024 * 1024),
                           timeout(-1),
                           status(IDLE),
                           checker(nullptr),
                           start(nullptr),
                           goal(nullptr),
                           cursor(nullptr),
                           canceled(false) {
}

void PathFinder::setStart(WeightedPoint *inStart) {
    start = inStart;
}

WeightedPoint *PathFinder::getStart() {
    return start;
}

void PathFinder::setGoal(WeightedPoint *in) {
    goal = in;
}

WeightedPoint *PathFinder::getGoal() {
    return goal;
}

void PathFinder::setHeuristic(Heuristic *in) {
    heuristic = in;
}

Heuristic *PathFinder::getHeuristic() {
    return heuristic;
}

NeighborSelector *PathFinder::getNeighborSelector() {
    return neighborSelector;
}

void PathFinder::setNeighborSelector(NeighborSelector *in) {
    neighborSelector = in;
}

FinderStatus PathFinder::getStatus() {
    return status;
}

void PathFinder::setShuffle(bool in) {
    shuffle = in;
}

void PathFinder::setDijkstra(bool in) {
    dijkstra = in;
}

void PathFinder::setChecker(PointStateChecker in) {
    checker = in;
}

PointStateChecker PathFinder::getPointChecker() {
    return checker;
}

FinderStatus PathFinder::resolve() {
    prepare();

    LOGD("Resolving...");

    // Timer
    if (timeout > 0) {
        TimeoutTracker *timeoutTracker = new TimeoutTracker(timeout, [&]() -> void {
            LOGD("Time out...");
            if (status == RUNNING) {
                status = TIMEOUT;
            }
        });
        timeoutTracker->start();
    }


    // Loop start.
    while (status == RUNNING) {

        if (isCanceled()) {
            status = CANCELED;
            LOGW("Canceled, dud...");
            return status;
        }

        // Hit the limited!!!
        if (stepCount >= maxStep) {
            status = COMPLETED_NOT_FOUND;
            return status;
        }

        status = step();
        stepCount++;
    }

    return status;
}

void PathFinder::prepare() {

    initialStep = true;

#ifdef PARAMS_STRICT_CHECK
    // Invalidate params
    LOGD("Validating finder params");
    checkNoneNull(checker, "The checker should not ne null");
    checkNoneNull(start, "Start is null");
    checkNoneNull(goal, "Goal is null");
    checkNoneNull(neighborSelector, "NeighborSelector is null");
    checkNoneNull(heuristic, "Heuristic is null");
#endif

#ifdef DEBUG_DUMP_PARAMS
    LOGD("Start is %s", start->toString().c_str());
    LOGD("Goal is %s", goal->toString().c_str());
    LOGD("NeighborSelector is %s", neighborSelector->getLabel().c_str());
    LOGD("Heuristic is %s", heuristic->getLabel().c_str());
    LOGD("Shuffle is %s", shuffle ? "TRUE" : "FALSE");
    LOGD("Dijkstra is %s", dijkstra ? "TRUE" : "FALSE");
    LOGD("Timeout is %d", (int) timeout);
#endif

    status = RUNNING;
}

FinderStatus PathFinder::step() {

    if (initialStep) {
        tail = start;
        //openSet.push_back(start);
        heap.push(start);
        initialStep = false;
    }

    if (status != RUNNING) {
        return status;
    }

    if (heap.empty()) {
        return COMPLETED_NOT_FOUND;;
    }

    cursor = heap.peek();
    heap.pop();

    if (cursor == nullptr) {
        // The open set was empty, so although we have not reached the goal, there are no more points to investigate
        return COMPLETED_NOT_FOUND;
    }

#ifdef DEBUG_VERBOSE
    LOGD("-Checking %s", cursor->toString().c_str());
#endif

    // Loop
    while (closedSet.find(std::pair<int, int>(cursor->getX(), cursor->getY())) != closedSet.end() ||
           !checker(*cursor)) {

#ifdef DEBUG_VERBOSE
        LOGD("Un-Travelable or Closed %s", cursor->toString().c_str());
#endif

        // The cursor is in the closed set (meaning it was already investigated) or the cursor point is non traversable on the map
        if (heap.empty()) {
            return COMPLETED_NOT_FOUND;
        }

        cursor = heap.peek();
        heap.pop();

        if (cursor == nullptr) {
            return COMPLETED_NOT_FOUND;
        }
    }

    // The goal has been reached, the path is complete
    if (arrived()) {
        tail = cursor;
        return COMPLETED_FOUND;
    }

    // Add the cursor point to the closed set
    closedSet.insert(std::pair<int, int>(cursor->getX(), cursor->getY()));
#ifdef DEBUG_VERBOSE
    LOGD("Closing %s", cursor->toString().c_str());
#endif


    // Get the list of neighboring points
    std::vector<WeightedPoint *> neighbors = neighborSelector->getNeighbors(cursor);

    std::vector<WeightedPoint *> checkedNeighbors;

    for (WeightedPoint *p : neighbors) {

        bool travelable = checker(*p);
        if (!travelable) {
#ifdef DEBUG_VERBOSE
            LOGD("Skipping no-tra %s", p->toString().c_str());
#endif
            continue;
        }

        bool closed = closedSet.find(std::pair<int, int>(p->getX(), p->getY())) != closedSet.end();
        if (closed) {
#ifdef DEBUG_VERBOSE
            LOGD("Skipping closed %s", p->toString().c_str());
#endif
            continue;
        }

        p->setFromCost(cursor->getFromCost() + heuristic->distance(cursor, p));

        if (dijkstra) {
            p->setToCost(0);
        } else {
            p->setToCost(heuristic->distance(p, goal));
        }

        p->setPrev(cursor);

        checkedNeighbors.push_back(p);
    }

    // Now using checked neighbors.

    if (shuffle) {
        random_shuffle(checkedNeighbors.begin(), checkedNeighbors.end());
    }

    // Sort the neighbors
    sort(checkedNeighbors.begin(), checkedNeighbors.end(), WeightedPoint::reverstCompare);

    // Put the neighbors on the open set
    for (WeightedPoint *p : checkedNeighbors) {
        if (openSet.find(p) == openSet.end()) { // First we should check if is already in open!
            heap.push(p);
            openSet.insert(p);
        }

#ifdef DEBUG_VERBOSE
        LOGD("Pushing %s", p->toString().c_str());
#endif
    }

    neighbors.clear();
    checkedNeighbors.clear();

    return RUNNING;
}

void PathFinder::clean() {
    LOGD("Cleaning...");
    start = 0;
    goal = 0;
    closedSet.clear();
    openSet.clear();
    heap.clear();
}

PathFinder::~PathFinder() {
    clean();
}

bool PathFinder::arrived() {
    return cursor->getX() == goal->getX() && cursor->getY() == goal->getY();
}

bool PathFinder::isCanceled() {
    return canceled;
}

std::vector<WeightedPoint *> PathFinder::getPath() {

    std::vector<WeightedPoint *> out;

    while (cursor != NULL) {
        out.push_back(cursor);
        cursor = cursor->getParent();
    }

    reverse(out.begin(), out.end());

    return out;
}

void PathFinder::setMaxStep(int max) {
    maxStep = max;
}

int PathFinder::getStepCount() const {
    return stepCount;
}

void PathFinder::cancel() {
    canceled = true;
}

void PathFinder::setTimeout(long timeMills) {
    timeout = timeMills;
}


















































