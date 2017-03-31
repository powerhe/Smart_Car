package com.lenovo.newdevice.tangocar.data;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.utils.DataSerializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by liujk2 on 2017/3/15.
 */

public class PointsQueue implements DataSerializable {
    Queue<Point> mPoints = null;
    public PointsQueue() {
    }

    public PointsQueue(Queue<Point> points) {
        mPoints = points;
    }

    public Queue<Point> getQueue() {
        return mPoints;
    }

    public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        int size = mPoints.size();
        out.writeInt(size);
        for (Point point : mPoints) {
            out.writeInt(point.x);
            out.writeInt(point.y);
        }
    }

    public void readFromDataInputStream(DataInputStream in) throws IOException {
        if (mPoints == null) {
            mPoints = new LinkedList<Point>();
        }
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            int x = in.readInt();
            int y = in.readInt();
            Point point = new Point(x, y);
            mPoints.add(point);
        }
    }
}
