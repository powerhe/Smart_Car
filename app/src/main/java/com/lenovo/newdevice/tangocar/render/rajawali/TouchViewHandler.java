package com.lenovo.newdevice.tangocar.render.rajawali;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.lenovo.newdevice.tangocar.data.GlobalData;

import org.rajawali3d.cameras.Camera;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import static org.rajawali3d.util.RajLog.TAG;

/**
 * Created by liujk2 on 2016/12/28.
 */

public class TouchViewHandler {
    // Touch interaction tuning constants.
    private static final int TOUCH_THIRD_PITCH_LIMIT = 60;
    private static final int TOUCH_THIRD_PITCH_DEFAULT = 45;
    private static final int TOUCH_THIRD_YAW_DEFAULT = -45;
    private static final int TOUCH_FOV_MAX = 120;
    private static final int TOUCH_THIRD_DISTANCE = 10;

    // Virtual reality view parameters.
    private static final int THIRD_PERSON_FOV = 65;

    private enum ViewMode {
        THIRD_PERSON, FOLLOW_MODE
    }

    private ViewMode viewMode = ViewMode.FOLLOW_MODE;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private float thirdPersonPitch = TOUCH_THIRD_PITCH_DEFAULT;
    private float thirdPersonYaw = TOUCH_THIRD_YAW_DEFAULT;

    private int mTouchedPointsCount = 0;
    private Vector3 mCameraDisplayPose = new Vector3(0, 0, 0);
    private Vector3 mCameraRealPose = new Vector3(0, 0, 0);
    private Vector3 mDeltaPose = new Vector3(0, 0, 0);

    private GlobalData mGlobalData;

    private Camera camera;

    public TouchViewHandler(Context context, Camera camera) {
        gestureDetector = new GestureDetector(context, new DragListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.camera = camera;
        this.camera.setFieldOfView(THIRD_PERSON_FOV);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void updateCamera(Vector3 position, Quaternion orientation) {
        mCameraRealPose.x = position.x;
        mCameraRealPose.y = position.y;
        mCameraRealPose.z = position.z;
        updateCameraDisplayPose(position);
        if (viewMode == ViewMode.THIRD_PERSON) {
            camera.setPosition(mCameraDisplayPose.x, mCameraDisplayPose.y, mCameraDisplayPose.z);
            camera.setRotZ(thirdPersonPitch);
            camera.rotate(Vector3.Axis.Y, thirdPersonYaw);
            camera.moveForward(TOUCH_THIRD_DISTANCE);
        } else if (viewMode == ViewMode.FOLLOW_MODE) {
            camera.setPosition(mCameraDisplayPose.x, mCameraDisplayPose.y, mCameraDisplayPose.z);
            camera.setRotZ(thirdPersonPitch);
            camera.rotate(Vector3.Axis.Y, 0 - mGlobalData.getCarPose(0).getCameraYawDegree());
            camera.moveForward(TOUCH_THIRD_DISTANCE);
        }
    }

    private void updateCameraDisplayPose(Vector3 position) {
        mCameraDisplayPose.x = mCameraRealPose.x + mDeltaPose.x;
        mCameraDisplayPose.y = mCameraRealPose.y + mDeltaPose.y;
        mCameraDisplayPose.z = mCameraRealPose.z + mDeltaPose.z;
    }

    public void onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mTouchedPointsCount = 1;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            mTouchedPointsCount = 2;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            mTouchedPointsCount--;
        } else if (action == MotionEvent.ACTION_UP) {
            mTouchedPointsCount = 0;
        }
        Log.i(TAG, "touch points count is "+ mTouchedPointsCount);
        gestureDetector.onTouchEvent(motionEvent);
        scaleGestureDetector.onTouchEvent(motionEvent);
    }

    public void switchMode() {
        if (viewMode == ViewMode.THIRD_PERSON) {
            setFollowModeView();
        } else if (viewMode == ViewMode.FOLLOW_MODE) {
            setThirdPersonView();
        }
    }

    public void setFollowModeView() {
        mDeltaPose.x = 0;
        mDeltaPose.y = 0;
        mDeltaPose.z = 0;
        viewMode = ViewMode.FOLLOW_MODE;
        camera.setFieldOfView(THIRD_PERSON_FOV);
        thirdPersonYaw = 0;
        thirdPersonPitch = TOUCH_THIRD_PITCH_DEFAULT;
    }

    public void setThirdPersonView() {
        viewMode = ViewMode.THIRD_PERSON;
        thirdPersonYaw = TOUCH_THIRD_YAW_DEFAULT;
        thirdPersonPitch = TOUCH_THIRD_PITCH_DEFAULT;
        camera.setFieldOfView(THIRD_PERSON_FOV);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        float scale = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale = detector.getScaleFactor();
            scale = Math.max(0.1f, Math.min(scale, 5f));

            camera.setFieldOfView(
                    Math.min(camera.getFieldOfView() / scale, TOUCH_FOV_MAX));

            return true;
        }
    }

    private class DragListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (viewMode == ViewMode.THIRD_PERSON) {
                if (mTouchedPointsCount == 1) {
                    thirdPersonPitch -= distanceY / 10;
                    thirdPersonPitch =
                            Math.min(thirdPersonPitch, TOUCH_THIRD_PITCH_LIMIT);
                    thirdPersonPitch =
                            Math.max(thirdPersonPitch, -TOUCH_THIRD_PITCH_LIMIT);
                    thirdPersonYaw -= distanceX / 10;
                    thirdPersonYaw %= 360;
                } else {
                    double factor = camera.getFieldOfView() / 45;
                    mDeltaPose.x += distanceX / 100 * factor;
                    mDeltaPose.z += distanceY / 100 * factor;
                }
            }

            return true;
        }
    }

    public void setGlobalData(GlobalData globalData) {
        mGlobalData = globalData;
    }
}
