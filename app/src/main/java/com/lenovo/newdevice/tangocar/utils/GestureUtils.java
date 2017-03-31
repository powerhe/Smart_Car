package com.lenovo.newdevice.tangocar.utils;

import android.view.MotionEvent;

/**
 * Created by liujk2 on 2017/3/16.
 */

public final class GestureUtils {

    private GestureUtils() {
        /* cannot be instantiated */
    }

    public static boolean isTap(MotionEvent down, MotionEvent up, int tapTimeSlop,
                                int tapDistanceSlop, int actionIndex) {
        return eventsWithinTimeAndDistanceSlop(down, up, tapTimeSlop, tapDistanceSlop, actionIndex);
    }

    public static boolean isMultiTap(MotionEvent firstUp, MotionEvent secondUp,
                                     int multiTapTimeSlop, int multiTapDistanceSlop, int actionIndex) {
        return eventsWithinTimeAndDistanceSlop(firstUp, secondUp, multiTapTimeSlop,
                multiTapDistanceSlop, actionIndex);
    }

    private static boolean eventsWithinTimeAndDistanceSlop(MotionEvent first, MotionEvent second,
                                                           int timeout, int distance, int actionIndex) {
        if (isTimedOut(first, second, timeout)) {
            return false;
        }
        final double deltaMove = computeDistance(first, second, actionIndex);
        if (deltaMove >= distance) {
            return false;
        }
        return true;
    }

    public static double computeDistance(MotionEvent first, MotionEvent second, int pointerIndex) {
        return MathUtils.dist(first.getX(pointerIndex), first.getY(pointerIndex),
                second.getX(pointerIndex), second.getY(pointerIndex));
    }

    public static boolean isTimedOut(MotionEvent firstUp, MotionEvent secondUp, int timeout) {
        final long deltaTime = secondUp.getEventTime() - firstUp.getEventTime();
        return (deltaTime >= timeout);
    }
}


