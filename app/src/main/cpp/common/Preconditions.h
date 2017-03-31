//
// Created by guohao4 on 2017/3/18.
//

#ifndef TANGO_CAR_PRECONDITIONS_H
#define TANGO_CAR_PRECONDITIONS_H


#include <stdexcept>

template<class T>
static T checkNoneNull(T &t, std::string msg) {
    if (t == nullptr) {
        LOGE("Throwing:%s", msg.c_str());
        throw std::invalid_argument(msg);
    }
    return t;
}

template<class T>
static T checkNoneNull(T &t) {
    if (t == nullptr) throw std::invalid_argument("T is null");
    return t;
}

#endif //TANGO_CAR_PRECONDITIONS_H
