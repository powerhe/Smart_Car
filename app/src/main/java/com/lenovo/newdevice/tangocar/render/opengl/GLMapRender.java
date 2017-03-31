package com.lenovo.newdevice.tangocar.render.opengl;

import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.utils.FloatRect;
import com.lenovo.newdevice.tangocar.map.MapScope;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class GLMapRender implements GLSurfaceView.Renderer {
    private static final boolean DEBUG = false;
    public static final float[] COLOR_BACK_GROUND   = GlColor.GL_COLOR_LTGRAY;
    public static final float[] COLOR_GRID          = GlColor.GL_COLOR_GRAY;
    public static final float[] COLOR_BORDER        = GlColor.GL_COLOR_GREEN;
    public static final float[] COLOR_INNER         = GlColor.GL_COLOR_WHITE;
    public static final float[] COLOR_TARGET        = GlColor.GL_COLOR_RED;
    public static final float[] COLOR_RUNNED_PATH   = GlColor.GL_COLOR_BLUE;
    public static final float[] COLOR_EXPECTED_PATH = GlColor.GL_COLOR_CYAN;

    public static final float DEFAULT_PIXELS_PER_POINT = 5.0f;
    private static final float DEFAULT_VIEW_SCALE = 1.0f;
    public static final float MIN_VIEW_SCALE = 0.2f;
    public static final float MAX_VIEW_SCALE = 4.0f;

    private static final Point ORIGIN = new Point(0, 0);

    private GridLines mGridLines;
    private VisibleAreaLine mVisibleAreaLine;
    private WorldMapPoints mWorldMap;

    private boolean mDisplayTarget;
    private FloatPoint mTargetPoint;

    private GlobalData mGlobalData;

    private float mPixelsPerPoint = DEFAULT_PIXELS_PER_POINT;
    private float mNewViewScale = DEFAULT_VIEW_SCALE;
    private float mViewScale = DEFAULT_VIEW_SCALE;

    private FloatRect mCurrentScop = new FloatRect(0, 0, 0, 0);
    private MapScope mMapScope;
    private Point mCenterPose = new Point(0, 0);
    private Object mScopeLock = new Object();

    private int mWidthPixel = 0;
    private int mHeightPixel = 0;

    public GLMapRender() {
        mGridLines = null;
        mVisibleAreaLine = new VisibleAreaLine();
        mDisplayTarget = false;
        mTargetPoint = new FloatPoint(10, 10);
    }

    public void setGlobalData(GlobalData globalData) {
        mGlobalData = globalData;
        if (mWorldMap != null) {
            mWorldMap.setMap(mGlobalData.getMap(), mGlobalData.getPath(0));
        }
    }

    public void setViewScale(float scale) {
        float newScale = mNewViewScale * scale;
        mNewViewScale = Math.max(GLMapRender.MIN_VIEW_SCALE, Math.min(newScale, GLMapRender.MAX_VIEW_SCALE));
    }

    public Rect getMapScope() {
        if (mMapScope != null) {
            return mMapScope.mScopeRect;
        }
        return null;
    }

    private void drawBackGround(GL10 gl10) {
        GlColor.glClear(gl10, COLOR_BACK_GROUND, 1.0f);
    }

    private void drawGrids(GL10 gl10) {
        if (mGridLines != null) {
            mGridLines.draw(gl10, COLOR_GRID, 1.0f);
        }
    }

    private void drawVisibleAreaLines(GL10 gl10) {
        if (mVisibleAreaLine != null) {
            mVisibleAreaLine.draw(gl10, GlColor.GL_COLOR_BLACK, 1.0f);
        }
    }

    private void drawWorldMap(GL10 gl10) {
        if (mWorldMap != null) {
            mWorldMap.draw(gl10, mMapScope);
        }
    }

    private void drawTarget(GL10 gl10) {
        if (mDisplayTarget) {
            MyGLUtils.drawPoint(gl10, mTargetPoint, mPixelsPerPoint * 5, GLMapRender.COLOR_TARGET);
        }
    }

    private boolean updateViewScaleData(float scale) {
        synchronized (mScopeLock) {
            boolean update = false;
            if (DEBUG) Log.i(TAG, "GLMapRender updateSizeData() mViewScale is " + mViewScale);
            if (DEBUG) Log.i(TAG, "GLMapRender updateSizeData() mNewViewScale is " + mNewViewScale);
            if (scale != mViewScale) {
                mViewScale = scale;
                mPixelsPerPoint = mViewScale * DEFAULT_PIXELS_PER_POINT;
                if (DEBUG) Log.i(TAG, "GLMapRender updateSizeData() mPixelsPerPoint is " + mPixelsPerPoint);
                if (mWorldMap != null) {
                    mWorldMap.setPixelsPerPoint(mPixelsPerPoint);
                }
                update = true;
            }
            return update;
        }
    }

    private boolean updateSizeData(int widthPixel, int heightPixel) {
        synchronized (mScopeLock) {
            boolean update = false;
            if (DEBUG) Log.i(TAG, "GLMapRender updateSizeData() (widthPixel, heightPixel) is (" + widthPixel + ", " + heightPixel + ")");
            if (DEBUG) Log.i(TAG, "GLMapRender updateSizeData() (mWidthPixel, mHeightPixel) is (" + mWidthPixel + ", " + mHeightPixel + ")");
            if (widthPixel != mWidthPixel || heightPixel != mHeightPixel) {
                mWidthPixel = widthPixel;
                mHeightPixel = heightPixel;
                update = true;
            }
            return update;
        }
    }

    private void updateScope(GL10 gl10, Point centerPose, boolean force) {
        boolean update = false;
        if (centerPose != null) {
            update = updateCenterData(centerPose);
        }
        update |= updateViewScaleData(mNewViewScale);
        if (update || force) {
            updateScope(gl10);
        }
    }

    private boolean updateCenterData(Point centerPose) {
        if (centerPose == null) {
            return false;
        }
        synchronized (mScopeLock) {
            boolean update = false;
            if (!mCenterPose.equals(centerPose)) {
                mCenterPose.x = centerPose.x;
                mCenterPose.y = centerPose.y;
                update = true;
            }
            return update;
        }
    }

    public boolean setTargetPoint(FloatPoint targetPoint) {
        mTargetPoint.update(targetPoint);
        return true;
    }

    public boolean getTargetPoint(FloatPoint targetPoint) {
        if (mTargetPoint != null) {
            targetPoint.update(mTargetPoint);
            return true;
        }
        return false;
    }

    public void setDisplayTarget(boolean display) {
        mDisplayTarget = display;
    }

    public FloatPoint getMapPointFromViewPoint(FloatPoint viewPose) {
        FloatPoint mapPose = new FloatPoint();
        float pixelDeltaX = viewPose.x - ((float) mWidthPixel / 2);
        float pixelDeltaY = ((float) mHeightPixel / 2) - viewPose.y;
        mapPose.x = Math.round(((float) mCenterPose.x) + pixelDeltaX / mPixelsPerPoint);
        mapPose.y = Math.round(((float) mCenterPose.y) + pixelDeltaY / mPixelsPerPoint);
        return mapPose;
    }

    private void updateScope(GL10 gl10) {
        synchronized (mScopeLock) {
            float widthPoint = ((float)mWidthPixel / mPixelsPerPoint);
            float heightPoint = ((float)mHeightPixel / mPixelsPerPoint);
            if (DEBUG) Log.i(TAG, "GLMapRender updateScope() (widthPoint, heightPoint) is (" + widthPoint + ", " + heightPoint + ")");

            mCurrentScop.mLeft = ((float) mCenterPose.x) - (widthPoint / 2);
            mCurrentScop.mRight = mCurrentScop.mLeft + widthPoint;
            mCurrentScop.mTop = ((float) mCenterPose.y) - (heightPoint / 2);
            mCurrentScop.mBottom = mCurrentScop.mTop + heightPoint;

            mMapScope = new MapScope(mCenterPose, (int)widthPoint, (int)heightPoint);

            if (mGridLines == null) {
                mGridLines = new GridLines(mMapScope.mScopeRect);
            } else {
                mGridLines.updateScope(mMapScope.mScopeRect);
            }

            if (mGlobalData != null && mWorldMap == null) {
                mWorldMap = new WorldMapPoints((int)widthPoint, (int)heightPoint);
                mWorldMap.setPixelsPerPoint(mPixelsPerPoint);
                mWorldMap.setMap(mGlobalData.getMap(), mGlobalData.getPath(0));
            }

            gl10.glViewport(0, 0, mWidthPixel, mHeightPixel);
            gl10.glMatrixMode(GL10.GL_PROJECTION);
            gl10.glLoadIdentity();
            gl10.glOrthof(mCurrentScop.mLeft, mCurrentScop.mRight, mCurrentScop.mTop, mCurrentScop.mBottom, -1, 1);
            if (DEBUG) Log.i(TAG, "GLMapRender glOrthof2 (" + mCurrentScop.mLeft + "," + mCurrentScop.mRight + "," + mCurrentScop.mTop + "," + mCurrentScop.mBottom + ")");
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        drawBackGround(gl10);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int w, int h) {
        if (DEBUG) Log.i(TAG, "GLMapRender onSurfaceChanged (" + w + ", " + h + ")");
        boolean update = updateSizeData(w, h);
        if (update) {
            updateScope(gl10);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (mGlobalData != null) {
            updateScope(gl10, mGlobalData.getCarPose(0).getCurrentGridIndex(), false);
            if (mVisibleAreaLine != null) {
                mVisibleAreaLine.updatePose(mCenterPose);
                mVisibleAreaLine.updateDegree(mGlobalData.getCarPose(0).getCameraYawDegree());
            }
        } else {
            updateScope(gl10, null, true);
        }

        gl10.glClear(GL10.GL_COLOR_BUFFER_BIT);
        drawGrids(gl10);
        drawWorldMap(gl10);
        drawTarget(gl10);
        drawVisibleAreaLines(gl10);
    }
}
