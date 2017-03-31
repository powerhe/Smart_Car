package com.lenovo.newdevice.tangocar.utils;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/3/16.
 */

public class MotionEventAssistant {
    public static final int TYPE_ONE_TAP = 1;
    public static final int TYPE_DOUBLE_TAP = 2;
    public static final int TYPE_TRIPLE_TAP = 3;

    private final Context mContext;
    private final int mTapTimeSlop;
    private final int mMultiTapTimeSlop;
    private final int mTapDistanceSlop;
    private final int mMultiTapDistanceSlop;

    private int mTapType = TYPE_DOUBLE_TAP;

    private MotionEvent mLastDownEvent;
    private MotionEvent mLastTapUpEvent;
    private int mTapCount;

    public MotionEventAssistant(int type, Context context) {
        mTapType = type;
        mContext = context;
        mTapTimeSlop = ViewConfiguration.getJumpTapTimeout();
        mMultiTapTimeSlop = ViewConfiguration.getDoubleTapTimeout();
        mTapDistanceSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMultiTapDistanceSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
    }

    protected void onOneTap(MotionEvent event) {
    }

    protected void onDoubleTap(MotionEvent event) {
    }

    protected void onTripleTap(MotionEvent event) {
    }

    public void onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if (mLastDownEvent != null && GestureUtils.isTimedOut(mLastDownEvent, event,
                        mMultiTapTimeSlop)) {
                    clearTapDetectionState();
                }
                mLastDownEvent = MotionEvent.obtain(event);
            } break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                clearTapDetectionState();
            } break;
            case MotionEvent.ACTION_MOVE: {
                if (mLastDownEvent != null) {
                    final double distance = GestureUtils.computeDistance(mLastDownEvent,
                            event, 0);
                    if (Math.abs(distance) > mTapDistanceSlop) {
                        clearTapDetectionState();
                    }
                }
            } break;
            case MotionEvent.ACTION_UP: {
                if (mLastDownEvent == null) {
                    return;
                }
                if (GestureUtils.isTap(mLastDownEvent, event, mTapTimeSlop,
                        mTapDistanceSlop, 0)) {
                    if (mTapType == TYPE_ONE_TAP) {
                        Log.d(TAG, "tap on screen once time");
                        onOneTap(event);
                        clearTapDetectionState();
                        return;
                    }
                } else {
                    clearTapDetectionState();
                    return;
                }
                if (mLastTapUpEvent != null && !GestureUtils.isMultiTap(mLastTapUpEvent,
                        event, mMultiTapTimeSlop, mMultiTapDistanceSlop, 0)) {
                    clearTapDetectionState();
                    return;
                }
                mTapCount++;
                if (mTapType == TYPE_DOUBLE_TAP && mTapCount == 2) {
                    Log.d(TAG, "tap on screen twice times");
                    onDoubleTap(event);
                    clearTapDetectionState();
                    return;
                }
                if (mTapType == TYPE_TRIPLE_TAP && mTapCount == 3) {
                    Log.d(TAG,"tap on screen triple times");
                    onTripleTap(event);
                    clearTapDetectionState();
                    return;
                }
                mLastTapUpEvent = MotionEvent.obtain(event);
            } break;
            case MotionEvent.ACTION_CANCEL: {
                if (mLastDownEvent == null) {
                    return;
                }
                if (!GestureUtils.isTap(mLastDownEvent, event, mTapTimeSlop,
                        mTapDistanceSlop, 0)) {
                    clearTapDetectionState();
                    return;
                }
                mTapCount ++;
                mLastTapUpEvent = MotionEvent.obtain(event);
            } break;
            case MotionEvent.ACTION_POINTER_UP: {
                    /* do nothing */
            } break;
        }
    }

    private void clearTapDetectionState() {
        mTapCount = 0;
        clearLastTapUpEvent();
        clearLastDownEvent();
    }

    private void clearLastTapUpEvent() {
        if (mLastTapUpEvent != null) {
            mLastTapUpEvent.recycle();
            mLastTapUpEvent = null;
        }
    }

    private void clearLastDownEvent() {
        if (mLastDownEvent != null) {
            mLastDownEvent.recycle();
            mLastDownEvent = null;
        }
    }
}
