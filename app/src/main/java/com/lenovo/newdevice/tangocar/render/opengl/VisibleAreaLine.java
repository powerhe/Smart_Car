package com.lenovo.newdevice.tangocar.render.opengl;

import android.graphics.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class VisibleAreaLine {
    private static final float FRUSTUM_WIDTH = 0.5773502692f * 8f * 20f;
    private static final float FRUSTUM_DEPTH = 4f * 20f;
    private static final float CAR_FRONT = 0.2f * 20f;
    private static final float CAR_BACK = 0.2f * 20f;
    private static final float CAR_LEFT = 0.2f * 20f;
    private static final float CAR_RIGHT = 0.2f * 20f;

    private float mRotateDegree;

    private Point mCurrentPose;

    private FloatBuffer mVertices;
    private int mVerticesLength;

    public VisibleAreaLine() {
        mVerticesLength = 6 * 4;
        mRotateDegree = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mVerticesLength * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        mVertices = byteBuffer.asFloatBuffer();
        mCurrentPose = new Point(0, 0);
        fillLineData();
    }

    synchronized public void updatePose(Point pose) {
        if (pose != null && !pose.equals(mCurrentPose)) {
            mCurrentPose.x = pose.x;
            mCurrentPose.y = pose.y;
        }
    }

    public void updateDegree(float degree) {
        mRotateDegree = degree;
    }

    synchronized private void fillLineData() {
        int x = 0;
        int y = 0;
        float[] o  = {x, y};
        float[] a  = {x - FRUSTUM_WIDTH / 2f, y + FRUSTUM_DEPTH};
        float[] b  = {x + FRUSTUM_WIDTH / 2f, y + FRUSTUM_DEPTH};
        float[] fl = {x - CAR_LEFT, y + CAR_FRONT};
        float[] fr = {x + CAR_RIGHT, y + CAR_FRONT};
        float[] bl = {x - CAR_LEFT, y - CAR_BACK};
        float[] br = {x + CAR_RIGHT, y - CAR_BACK};

        mVertices.rewind();
        mVertices.put(o);
        mVertices.put(a);
        mVertices.put(o);
        mVertices.put(b);
        mVertices.put(fl);
        mVertices.put(fr);
        mVertices.put(fr);
        mVertices.put(br);
        mVertices.put(br);
        mVertices.put(bl);
        mVertices.put(bl);
        mVertices.put(fl);
        mVertices.flip();
    }

    synchronized public void draw(GL10 gl10, float[] color, float alpha) {
        gl10.glTranslatef(mCurrentPose.x, mCurrentPose.y, 0);
        gl10.glRotatef(mRotateDegree, 0, 0, 1);
        gl10.glLineWidth(1.0f);
        gl10.glColor4f(color[0], color[1], color[2], alpha);
        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertices);
        gl10.glDrawArrays(GL10.GL_LINES, 0, mVerticesLength / 2);
        gl10.glRotatef(0 - mRotateDegree, 0, 0, 1);
        gl10.glTranslatef(0 - mCurrentPose.x, 0 - mCurrentPose.y, 0);
    }
}
