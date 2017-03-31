package com.lenovo.newdevice.tangocar.render.rajawali;

import android.opengl.GLES10;
import android.opengl.GLES20;

import org.rajawali3d.Object3D;

import java.nio.FloatBuffer;

/**
 * Created by liujk2 on 2016/12/29.
 */

/**
 * A Point primitive for Rajawali.
 * Intended to be contributed and PR'ed to Rajawali.
 */
public class Points extends Object3D {
    private static final int BYTES_PER_FLOAT = 4;

    private int mMaxNumberOfVertices;
    // Float values per point to expect in points FloatBuffer. XYZ format = 3, XYZC format = 4.
    protected int mFloatsPerPoint = 3;
    // Float values per color = 4 (RGBA).
    protected int mFloatsPerColor = 4;

    public Points(int numberOfPoints, int floatsPerPoint, boolean isCreateColors) {
        super();
        mMaxNumberOfVertices = numberOfPoints;
        mFloatsPerPoint = floatsPerPoint;
        init(true, isCreateColors);
    }

    // Initialize the buffers for Points primitive.
    // Since only vertex, index and color buffers are used,
    // we only initialize them using setData call.
    protected void init(boolean createVBOs, boolean createColors) {
        float[] vertices = new float[mMaxNumberOfVertices * mFloatsPerPoint];
        int[] indices = new int[mMaxNumberOfVertices];
        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
        }
        float[] colors = null;
        if (createColors) {
            colors = new float[mMaxNumberOfVertices * mFloatsPerColor];
        }
        mGeometry.getVertexBufferInfo().stride = mFloatsPerPoint * BYTES_PER_FLOAT;
        setData(vertices, null, null, colors, indices, createVBOs);
    }

    /**
     * Update the geometry of the points based on the provided points float buffer.
     */
    public void updatePoints(int pointCount, float[] pointCloudBuffer) {
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(pointCloudBuffer);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0,
                pointCount * mFloatsPerPoint);
    }

    /**
     * Update the geometry of the points based on the provided points float buffer and corresponding
     * colors based on the provided float array.
     */
    public void updatePoints(int pointCount, float[] points, float[] colors) {
        if (pointCount > mMaxNumberOfVertices) {
            throw new RuntimeException(
                    String.format("pointClount = %d exceeds maximum number of points = %d",
                            pointCount, mMaxNumberOfVertices));
        }
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(points);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0,
                pointCount * mFloatsPerPoint);
        mGeometry.setColors(colors);
        mGeometry.changeBufferData(mGeometry.getColorBufferInfo(), mGeometry.getColors(), 0,
                pointCount * mFloatsPerColor);
    }

    @Override
    public void preRender() {
        super.preRender();
        setDrawingMode(GLES20.GL_POINTS);
        GLES10.glPointSize(5.0f);
    }
}
