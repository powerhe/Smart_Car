package com.lenovo.newdevice.tangocar.render.rajawali;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.lenovo.newdevice.tangocar.data.CarPose;
import com.lenovo.newdevice.tangocar.data.GlobalData;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

/**
 * Created by liujk2 on 2016/12/27.
 */

public class MapRenderer extends MyRenderer {
    private static final int MIN_DISTANCE_TO_BORDER = 4 * 20;
    private static final float CAMERA_POSE_Y = 0;

    private static final float CAMERA_NEAR = 0.01f;
    private static final float CAMERA_FAR = 200f;

    public static int MAP_SIZE = 30;
    private static int WORLD_MAX_POINT = MAP_SIZE * MAP_SIZE * 20 * 20;
    private static int SIZE_BY_GRID = MAP_SIZE * 20;

    private TouchViewHandler mTouchViewHandler;

    private ObjectGrid mObjectGrid;
    private ObjectVisibleRangeLine mVisibleRangeLine;
    private ObjectWorldMap mWorldMap;

    private Object mScopeLock = new Object();
    private Rect mCurrScope = new Rect(0, 0, 0, 0);
    private Point mCenterPose = new Point(0,0);
    private Vector3 mDeltaPose = new Vector3(0, 0, 0);
    private int mHarfSizeByGrid;

    private GlobalData mGlobalData;

    public static void initSize(int size) {
        MAP_SIZE = size;
        WORLD_MAX_POINT = MAP_SIZE * MAP_SIZE * 20 * 20;
        SIZE_BY_GRID = MAP_SIZE * 20;
    }

    public MapRenderer(Context context) {
        super(context);
        mHarfSizeByGrid = SIZE_BY_GRID / 2;
        mCurrScope = new Rect(0 - mHarfSizeByGrid, 0 - mHarfSizeByGrid, mHarfSizeByGrid, mHarfSizeByGrid);
        mTouchViewHandler = new TouchViewHandler(mContext, getCurrentCamera());
    }

    @Override
    protected void initScene() {
        mObjectGrid = new ObjectGrid(MAP_SIZE * 2, 1, 1, 0xFFCCCCCC);
        getCurrentScene().addChild(mObjectGrid);

        mVisibleRangeLine = new ObjectVisibleRangeLine(1, Color.BLUE);
        getCurrentScene().addChild(mVisibleRangeLine);

        mWorldMap = new ObjectWorldMap(WORLD_MAX_POINT, 3);
        getCurrentScene().addChild(mWorldMap);

        getCurrentScene().setBackgroundColor(Color.GRAY);
        getCurrentCamera().setNearPlane(CAMERA_NEAR);
        getCurrentCamera().setFarPlane(CAMERA_FAR);
        getCurrentCamera().setFieldOfView(37.5);
    }

    @Override
    public void onOffsetsChanged(float v, float v1, float v2, float v3, int i, int i1) {
    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        mTouchViewHandler.onTouchEvent(motionEvent);
    }

    private void updateCameraPose() {
        CarPose carPose = mGlobalData.getCarPose(0);
        if (!carPose.hasCameraPose()) {
            return;
        }
        float[] rotation = carPose.getCameraRotationAsFloats();
        float[] translation = carPose.getCameraTranslationAsFloats();
        updateTranslation(translation);
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        mVisibleRangeLine.setRotY(0 - mGlobalData.getCarPose(0).getCameraYawDegree());
        mVisibleRangeLine.setPosition(translation[0], CAMERA_POSE_Y, translation[2]);
        mTouchViewHandler.updateCamera(new Vector3(translation[0], CAMERA_POSE_Y, translation[2]),
                quaternion);
    }

    private void updateTranslation(float[] translation) {
        translation[0] -= ((float)(mCenterPose.x) * 0.05);
        translation[2] += ((float)(mCenterPose.y) * 0.05);
    }

    public void switchMode() {
        mTouchViewHandler.switchMode();
    }

    private void updateScope(Point currGrid) {
        synchronized (mScopeLock) {
            int delta;
            delta = currGrid.x - (mCurrScope.left - MIN_DISTANCE_TO_BORDER);
            if (delta < 0) {
                mCenterPose.x += delta;
            } else {
                delta = currGrid.x - (mCurrScope.right - MIN_DISTANCE_TO_BORDER);
                if (delta > 0) {
                    mCenterPose.x += delta;
                }
            }
            delta = currGrid.y - (mCurrScope.top + MIN_DISTANCE_TO_BORDER);
            if (delta < 0) {
                mCenterPose.y += delta;
            } else {
                delta = currGrid.y - (mCurrScope.bottom - MIN_DISTANCE_TO_BORDER);
                if (delta > 0) {
                    mCenterPose.y += delta;
                }
            }
            mCurrScope.left = mCenterPose.x - mHarfSizeByGrid;
            mCurrScope.right = mCenterPose.x + mHarfSizeByGrid;
            mCurrScope.top = mCenterPose.y - mHarfSizeByGrid;
            mCurrScope.bottom = mCenterPose.y + mHarfSizeByGrid;
        }
    }

    public Rect getCurrentScope() {
        synchronized (mScopeLock) {
            return mCurrScope;
        }
    }

    public void setGlobalData(GlobalData globalData) {
        mGlobalData = globalData;
        mTouchViewHandler.setGlobalData(globalData);
    }

    public void updateMap() {
        if (mGlobalData == null) {
            return;
        }
        Point currentPos = mGlobalData.getCarPose(0).getCurrentGridIndex();
        if (currentPos != null) {
            updateScope(currentPos);
        }
        updateCameraPose();
        mWorldMap.updateMap(mCenterPose, mCurrScope, mGlobalData);
    }
}
