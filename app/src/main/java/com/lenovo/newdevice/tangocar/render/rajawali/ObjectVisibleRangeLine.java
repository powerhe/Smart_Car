package com.lenovo.newdevice.tangocar.render.rajawali;

import android.graphics.Color;
import android.opengl.GLES20;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;

import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

/**
 * Created by liujk2 on 2016/12/29.
 */

public class ObjectVisibleRangeLine extends Line3D {
    private static final float FRUSTUM_WIDTH = 0.5773502692f * 8f;
    private static final float FRUSTUM_DEPTH = 4f;
    private static final float CAR_FRONT = 0.2f;
    private static final float CAR_BACK = 0.2f;
    private static final float CAR_LEFT = 0.2f;
    private static final float CAR_RIGHT = 0.2f;

    public ObjectVisibleRangeLine(float thickness, int color) {
        super(makePoints(), thickness, color);
        Material material = new Material();
        material.useVertexColors(true);
        setMaterial(material);
    }

    private static Stack<Vector3> makePoints() {
        Vector3 o = new Vector3(0, 0, 0);
        Vector3 a = new Vector3(-FRUSTUM_WIDTH / 2f, 0, -FRUSTUM_DEPTH);
        Vector3 b = new Vector3(FRUSTUM_WIDTH / 2f, 0, -FRUSTUM_DEPTH);
        Vector3 fl = new Vector3(-CAR_FRONT, 0, -CAR_LEFT);
        Vector3 fr = new Vector3(-CAR_FRONT, 0, CAR_RIGHT);
        Vector3 bl = new Vector3(CAR_BACK, 0, -CAR_LEFT);
        Vector3 br = new Vector3(CAR_BACK, 0, CAR_RIGHT);

        Stack<Vector3> points = new Stack<Vector3>();
        Collections.addAll(points, o, a, o, b, fl, fr, fl, bl, fr, br, bl, br);

        return points;
    }

    @Override
    protected void init(boolean createVBOs) {
        super.init(createVBOs);
        setDrawingMode(GLES20.GL_LINES);
    }
}
