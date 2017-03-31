//
// Created by guohao4 on 2017/3/20.
//

#ifndef TANGO_CAR_TIMEOUTTRACKER_H
#define TANGO_CAR_TIMEOUTTRACKER_H

#include <functional>

class TimeoutTracker {

    typedef std::function<void()> TimeoutListener;

private:
    TimeoutListener listener;
    long time;

public:
    TimeoutTracker(long timeMills, const std::function<void()> &TimeoutListener)
            : time(timeMills), listener(TimeoutListener) { }


    virtual ~TimeoutTracker() { listener = nullptr; }

public:
    void start();


protected:
    bool check();
};


#endif //TANGO_CAR_TIMEOUTTRACKER_H
