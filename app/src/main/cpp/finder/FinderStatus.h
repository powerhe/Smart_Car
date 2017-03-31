//
// Created by guohao4 on 2017/3/17.
//

#ifndef TANGO_CAR_FINDERSTATUS_H
#define TANGO_CAR_FINDERSTATUS_H

enum FinderStatus {
    RUNNING,
    CANCELED,
    TIMEOUT,
    COMPLETED_FOUND,
    COMPLETED_NOT_FOUND,
    IDLE
};

#endif //TANGO_CAR_FINDERSTATUS_H
