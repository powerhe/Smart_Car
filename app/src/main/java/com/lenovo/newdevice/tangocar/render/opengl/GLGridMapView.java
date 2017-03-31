package com.lenovo.newdevice.tangocar.render.opengl;

import android.content.Context;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.main.ControlView;
import com.lenovo.newdevice.tangocar.data.GlobalData;
import com.lenovo.newdevice.tangocar.map.FloatPoint;
import com.lenovo.newdevice.tangocar.utils.MotionEventAssistant;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class GLGridMapView extends GLSurfaceView {
    private GLMapRender mGLMapRender;
    private Context mContext;
    private ScaleGestureDetector scaleGestureDetector;

    private TextView mTouchInfoView;
    private ControlView mControlView;
    private MotionEventAssistant mMotionEventAssistant;

    public GLGridMapView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public GLGridMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mMotionEventAssistant = new GestureProcess(mContext);
        scaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleListener());
        mGLMapRender = new GLMapRender();
        setRenderer(mGLMapRender);
    }

    public void setTouchInfoView(TextView touchInfoView) {
        mTouchInfoView = touchInfoView;
    }

    public void setControlView(ControlView controlView) {
        mControlView = controlView;
        mControlView.setMapView(this);
    }

    public void setDisplayTarget(boolean display) {
        mGLMapRender.setDisplayTarget(display);
    }

    public boolean setTargetPoint(FloatPoint targetPoint) {
        return mGLMapRender.setTargetPoint(targetPoint);
    }

    public boolean getTargetPoint(FloatPoint targetPoint) {
        return mGLMapRender.getTargetPoint(targetPoint);
    }

    public void setGlobalData(GlobalData globalData) {
        mGLMapRender.setGlobalData(globalData);
    }

    public Rect getMapScope() {
        return mGLMapRender.getMapScope();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        final int action = motionEvent.getActionMasked();
        if (action == MotionEvent.ACTION_UP) {
            Log.i(TAG, "GLGridMapView.onTouchEvent ACTION_UP (" + motionEvent.getX() + ", " + motionEvent.getY() + ")");
        }
        return super.onTouchEvent(motionEvent);
    }

    public boolean procTouchEvent(MotionEvent motionEvent) {
        mMotionEventAssistant.onTouchEvent(motionEvent);
        scaleGestureDetector.onTouchEvent(motionEvent);
        return false;
    }

    public boolean procGenericMotionEvent(MotionEvent event) {
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                // process the scroll wheel movement
                case MotionEvent.ACTION_SCROLL:
                    // get scroll direction
                    if( event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f){
                        // scroll down
                        mGLMapRender.setViewScale(0.9f);
                    } else {
                        // scroll up
                        mGLMapRender.setViewScale(1.1f);
                    }
                    return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        float scale = 1f;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale = detector.getScaleFactor();
            mGLMapRender.setViewScale(scale);
            return true;
        }
    }

    private int[] getLocationInWindow() {
        int[] location = new int[2];
        getLocationInWindow(location);
        return location;
    }

    private FloatPoint eventPoseToMapPose(FloatPoint eventPose) {
        FloatPoint point = new FloatPoint();
        Log.i(TAG, "eventPoseToMapPose() screen " + eventPose);
        int[] location = getLocationInWindow();
        Log.i(TAG, "eventPoseToMapPose() view location(" + location[0] + ", " + location[1] + ")");

        point.x = eventPose.x - location[0];
        point.y = eventPose.y - location[1];
        point = mGLMapRender.getMapPointFromViewPoint(point);
        Log.i(TAG, "eventPoseToMapPose() map" + point);
        return point;
    }

    private class GestureProcess extends MotionEventAssistant {
        public GestureProcess(Context context) {
            super(MotionEventAssistant.TYPE_ONE_TAP, context);
        }
        @Override
        protected void onOneTap(MotionEvent motionEvent) {
            FloatPoint point = new FloatPoint(motionEvent.getX(), motionEvent.getY());
            point = eventPoseToMapPose(point);
            mTouchInfoView.setText("Tap pose " + point);
            if (mControlView.needSelectTarget()) {
                mControlView.setTarget(point);
                mGLMapRender.setTargetPoint(point);
            }
        }
    }
}
