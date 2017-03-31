package com.lenovo.newdevice.tangocar.render.opengl;

import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class GridLines {
    private static final int GRID_SIZE = 20;
    private Rect mScope = new Rect(0, 0, 0, 0);
    private FloatBuffer mVertices;
    private int mVerticesLength;
    private float mTopLineY;
    private float mLeftLineX;
    private int mHorizontalLineCount;
    private int mVerticalLinecount;

    public GridLines(Rect scope) {
        updateScope(scope);
    }

    private void createVertices() {
        mVerticesLength = (mHorizontalLineCount + mVerticalLinecount) * 4;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mVerticesLength * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        mVertices = byteBuffer.asFloatBuffer();
    }

    synchronized private void fillLineData() {
        float[] line = new float[4];

        mVertices.rewind();
        float curY = mTopLineY;
        line[0] = mScope.left;
        line[2] = mScope.right;
        for (int i = 0; i < mHorizontalLineCount; i ++) {
            line[1] = line[3] = curY;
            mVertices.put(line);
            curY += GRID_SIZE;
        }

        float curX = mLeftLineX;
        line[1] = mScope.top;
        line[3] = mScope.bottom;
        for (int i = 0; i < mVerticalLinecount; i ++) {
            line[0] = line[2] = curX;
            mVertices.put(line);
            curX += GRID_SIZE;
        }
        mVertices.flip();
    }

    public void updateScope(Rect scope) {
        if (!mScope.equals(scope)) {
            mTopLineY = (scope.top / GRID_SIZE) * GRID_SIZE;
            mLeftLineX = (scope.left / GRID_SIZE) * GRID_SIZE;
            int horizontalLineCount = (scope.bottom - scope.top) / GRID_SIZE + 1;
            int verticalLinecount = (scope.right - scope.left) / GRID_SIZE + 1;

            if (horizontalLineCount != mHorizontalLineCount || verticalLinecount != mVerticalLinecount) {
                mHorizontalLineCount = horizontalLineCount;
                mVerticalLinecount = verticalLinecount;
                createVertices();
            }

            mScope.left = scope.left;
            mScope.right = scope.right;
            mScope.top = scope.top;
            mScope.bottom = scope.bottom;
            fillLineData();
        }
    }

    synchronized public void draw(GL10 gl10, float[] color, float alpha) {
        gl10.glLineWidth(1.0f);
        gl10.glColor4f(color[0], color[1], color[2], alpha);
        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertices);
        gl10.glDrawArrays(GL10.GL_LINES, 0, mVerticesLength / 2);
    }
}
