package com.lenovo.newdevice.tangocar.render.opengl;

import android.graphics.Point;
import android.util.Log;

import com.lenovo.newdevice.tangocar.map.FloatPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/3/16.
 */

public class MyGLUtils {
    private static FloatBuffer sDrawPointBuffer;
    static {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 2 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        sDrawPointBuffer = byteBuffer.asFloatBuffer();
    }

    public static void drawPoint(GL10 gl10, FloatPoint point, float size, float[] color) {

        gl10.glPointSize(size);
        gl10.glEnable(GL10.GL_POINT_SMOOTH);
        gl10.glHint(GL10.GL_POINT_SMOOTH, GL10.GL_NICEST);
        gl10.glColor4f(color[0], color[1], color[2], 1.0f);

        sDrawPointBuffer.clear();
        sDrawPointBuffer.put(point.x);
        sDrawPointBuffer.put(point.y);
        sDrawPointBuffer.flip();

        gl10.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl10.glVertexPointer(2, GL10.GL_FLOAT, 0, sDrawPointBuffer);
        gl10.glDrawArrays(GL10.GL_POINTS, 0, 1);
        gl10.glDisable(GL10.GL_POINT_SMOOTH);
    }
}
