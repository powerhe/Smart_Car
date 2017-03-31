package com.lenovo.newdevice.tangocar.control;

import android.graphics.Point;
import android.util.Log;

import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.CarData;
import com.lenovo.newdevice.tangocar.utils.Utils;

import java.util.Queue;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/12/29.
 */

public class AdvanceControl {
    private CarControl mCarControl;
    private CarPose mCarPose;

    private final byte POS_TOO_FAR      = 0;
    private final byte POS_TOP_LEFT     = 1;
    private final byte POS_TOP          = 2;
    private final byte POS_TOP_RIGHT    = 3;
    private final byte POS_LEFT         = 4;
    private final byte POS_CENTER       = 5;
    private final byte POS_RIGHT        = 6;
    private final byte POS_BOTTOM_LEFT  = 7;
    private final byte POS_BOTTOM       = 8;
    private final byte POS_BOTTOM_RIGHT = 9;
    private final float[] RelativePos2Orientation = {0,
            135,  90,  45,
            180,   0,   0,
            -135, -90, -45};

    public AdvanceControl(CarControl carControl, CarData carData) {
        mCarControl = carControl;
        mCarPose = carData.getPose();
    }

    private void runByPathOver(RunByPathCallback callBack, boolean stop) {
        mCarControl.stop();
        callBack.runOver(stop);
    }

    public void runByPath(Queue<Point> pathPoints, RunByPathCallback callBack) {
        Point currentPose = mCarPose.getCurrentGridIndex();
        Point lastPoint = pathPoints.poll();
        /* too far from the start point, will go to the first point
        if (getRelativePos(currentPose, lastPoint) == POS_TOO_FAR) {
            // too far from the start point, return
            callBack.runOver();
            return;
        }
        */
        byte lastDirection = POS_TOO_FAR;
        while (lastPoint != null) {
            Point point = pathPoints.poll();
            byte currDirection = POS_TOO_FAR;
            if (point != null) {
                currDirection = getRelativePos(lastPoint, point);
            }
            if (currDirection != lastDirection || point == null) {
                Point tmpDest = lastPoint;
                Log.i(TAG, "Tmp Dest set to:" + tmpDest.toString());
                currentPose = mCarPose.getCurrentGridIndex();
                if (getRelativePos(currentPose, tmpDest) != POS_TOO_FAR) {
                    mCarControl.stop();
                    lastDirection = currDirection;
                    lastPoint = point;
                    continue;
                }
                if (callBack.needStop()) {
                    runByPathOver(callBack, true);
                    return;
                }
                mCarControl.turnToDegree(calculateOrientation(currentPose, tmpDest));
                mCarControl.waitForTurnOver();
                while (getRelativePos(currentPose, tmpDest) == POS_TOO_FAR) {
                    if (Math.abs(currentPose.x-tmpDest.x) <= 2.1f && Math.abs(currentPose.y-tmpDest.y) <= 2.1f) {
                        mCarControl.stop();
                        break;
                    }
                    mCarControl.forwardTarget(tmpDest);
                    Utils.sleep(10);
                    if (callBack.needStop()) {
                        runByPathOver(callBack, true);
                        return;
                    }
                    currentPose = mCarPose.getCurrentGridIndex();
                }
                lastDirection = currDirection;
            }
            lastPoint = point;
        }

        runByPathOver(callBack, false);
    }

    /* return the orientation looks from srcPoint to dstPoint in Degrees*/
    private float calculateOrientation(Point srcPoint, Point dstPoint)
    {
        return (float) Math.toDegrees(Math.atan2(dstPoint.y-srcPoint.y, dstPoint.x-srcPoint.x));
    }

    /* return the relative pos info from pos1 to pos2 */
    private byte getRelativePos(Point pos1, Point pos2)
    {
        if (pos1.equals(pos2)) {
            return POS_CENTER;
        }
        if (pos2.x == pos1.x){
            if (pos2.y == pos1.y + 1)
                return POS_TOP;
            else if (pos2.y == pos1.y - 1)
                return POS_BOTTOM;
            else
                return POS_TOO_FAR;
        } else if (pos2.x == pos1.x - 1 ){
            if (pos2.y == pos1.y + 1)
                return POS_TOP_LEFT;
            else if (pos2.y == pos1.y - 1)
                return POS_BOTTOM_LEFT;
            else if (pos2.y == pos1.y)
                return POS_LEFT;
            else
                return POS_TOO_FAR;
        } else if (pos2.x == pos1.x + 1 ){
            if (pos2.y == pos1.y + 1)
                return POS_TOP_RIGHT;
            else if (pos2.y == pos1.y - 1)
                return POS_BOTTOM_RIGHT;
            else if (pos2.y == pos1.y)
                return POS_RIGHT;
            else
                return POS_TOO_FAR;
        } else {
            return POS_TOO_FAR;
        }
    }

    public interface RunByPathCallback {
        public boolean needStop();
        public void runOver(boolean stop);
    }
}
