//
// Created by guohao4 on 2017/3/17.
//

#ifndef TANGO_CAR_PATHFINDER_H
#define TANGO_CAR_PATHFINDER_H

#include "WeightedPoint.h"
#include "FinderStatus.h"
#include "Heuristic.h"
#include "NeighborSelector.h"
#include "PointStateChecker.h"
#include "PointInfo.h"
#include "Heap.h"

#include <set>
#include <map>

class PathFinder {

public:
    PathFinder();

    ~PathFinder();

private:
    WeightedPoint *start, *goal;

    WeightedPoint *tail, *cursor;

    Heuristic *heuristic;

    NeighborSelector *neighborSelector;

    PointStateChecker checker;

    FinderStatus status;

    std::set<std::pair<int, int>> closedSet;

    std::set<WeightedPoint *> openSet;

    Heap<WeightedPoint> heap;

    /**
     * Whether to assign any h value (to cost) with the heuristic. If true, will find the optimal path.
     */
    bool dijkstra;

    /**
     * Whether to shuffle the order of nodes with the same cost to avoid the bias of the default neighbor order
     */
    bool shuffle;

    /**
     * Whether the initial step of the algorithm has been taken,
     * used to set the tail node and push the start onto the open set
     */
    bool initialStep;

    int maxStep;

    int stepCount;

    bool canceled;

    long timeout;

private:
    bool arrived();

    bool isCanceled();

protected:
    void prepare();

    void clean();


public:
    void setStart(WeightedPoint *start);

    WeightedPoint *getStart();

    void setGoal(WeightedPoint *in);

    WeightedPoint *getGoal();

    void setHeuristic(Heuristic *in);

    Heuristic *getHeuristic();

    void setNeighborSelector(NeighborSelector *in);

    NeighborSelector *getNeighborSelector();

    void setChecker(PointStateChecker in);

    PointStateChecker getPointChecker();

    FinderStatus getStatus();

    void setShuffle(bool in);

    void setDijkstra(bool in);

    FinderStatus step();

    FinderStatus resolve();

    std::vector<WeightedPoint *> getPath();

    void setMaxStep(int max);

    int getStepCount() const;

    void cancel();

    void setTimeout(long timeMills);
};


#endif //TANGO_CAR_PATHFINDER_H
