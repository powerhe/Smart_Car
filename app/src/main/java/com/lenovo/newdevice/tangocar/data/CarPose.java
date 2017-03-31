package com.lenovo.newdevice.tangocar.data;

import android.graphics.Point;

import com.lenovo.newdevice.tangocar.map.FloatPoint3D;
import com.lenovo.newdevice.tangocar.map.GridInfo;
import com.lenovo.newdevice.tangocar.utils.DataSerializable;
import com.lenovo.newdevice.tangocar.utils.MathUtils;
import com.lenovo.newdevice.tangocar.utils.OrientationMath;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by liujk2 on 2017/2/22.
 * this util class is used most for controller display
 */
public class CarPose implements DataSerializable {
    public double timestamp;
    private final Object deviceLock = new Object();
    private final Object cameraLock = new Object();
    private boolean mHasDevicePose = false;
    private boolean mHasCameraPose = false;
    private double[] deviceTranslation = new double[3];
    private double[] deviceRotation = new double[4];
    private double[] cameraTranslation = new double[3];
    private double[] cameraRotation = new double[4];
    private FloatPoint3D mDevicePose = FloatPoint3D.ORIGIN_POINT;

    public boolean hasCameraPose() {
        return mHasCameraPose;
    }

    public float[] getCameraRotationAsFloats() {
        synchronized (cameraLock) {
            if (mHasCameraPose) {
                float[] rotation = new float[4];
                rotation[0] = (float) cameraRotation[0];
                rotation[1] = (float) cameraRotation[1];
                rotation[2] = (float) cameraRotation[2];
                rotation[3] = (float) cameraRotation[3];
                return rotation;
            }
            return null;
        }
    }

    public float[] getCameraTranslationAsFloats() {
        synchronized (cameraLock) {
            if (mHasCameraPose) {
                float[] translation = new float[3];
                translation[0] = (float) cameraTranslation[0];
                translation[1] = (float) cameraTranslation[1];
                translation[2] = (float) cameraTranslation[2];
                return translation;
            }
            return null;
        }
    }

    public void setDeviceTranslationAndRotation(double[] translation, double[] rotation) {
        synchronized (deviceLock) {
            mHasDevicePose = true;
            mDevicePose.x = (float) translation[0];
            mDevicePose.y = (float) translation[1];
            mDevicePose.z = (float) translation[2];
            setTranslationAndRotationTo(translation, rotation,
                    deviceTranslation, deviceRotation);
        }
    }

    public void setCameraTranslationAndRotation(double[] translation, double[] rotation) {
        synchronized (cameraLock) {
            mHasCameraPose = true;
            setTranslationAndRotationTo(translation, rotation,
                    cameraTranslation, cameraRotation);
        }
    }

    public boolean hasDevicePose() {
        return mHasDevicePose;
    }

    public FloatPoint3D getDevicePose() {
        return mDevicePose;
    }

    public double[] getDeviceTranslation() {
        return deviceTranslation;
    }

    public void update(CarPose carPose) {
        if (carPose == this || carPose == null) {
            return;
        }
        this.timestamp = carPose.timestamp;
        setDeviceTranslationAndRotation(carPose.deviceTranslation, carPose.deviceRotation);
        setCameraTranslationAndRotation(carPose.cameraTranslation, carPose.cameraRotation);
    }

    private void setTranslationAndRotationTo(double[] translationSrc, double[] rotationSrc,
                                             double[] translationDest, double[] rotationDest) {
        if (translationSrc != null && translationDest != null) {
            System.arraycopy(translationSrc, 0, translationDest, 0, 3);
        }
        if (rotationSrc != null && rotationDest != null) {
            System.arraycopy(rotationSrc, 0, rotationDest, 0, 4);
        }
    }

    public Point getCurrentGridIndex() {
        synchronized (deviceLock) {
            if (mHasDevicePose) {
                return new Point((int) (deviceTranslation[0] / GridInfo.sGridWidth),
                        (int) (deviceTranslation[1] / GridInfo.sGridWidth));
            }
        }
        return null;
    }

    public float getCameraYawDegree() {
        return getCameraYawDegree(MathUtils.sFormat180, 0);
    }

    public float getDeviceYawDegree() {
        return getCameraYawDegree(MathUtils.sFormat180, 90);
    }

    public float getDeviceYawDegree(boolean format180) {
        return getCameraYawDegree(format180, 90);
    }

    private float getCameraYawDegree(boolean format180, float deltaDegree) {
        OrientationMath orientationMath = null;
        synchronized (cameraLock) {
            if (mHasCameraPose) {
                orientationMath = new OrientationMath(cameraRotation);
            }
        }
        if (orientationMath != null) {
            return MathUtils.getYawDegreeFromOrientation(format180, orientationMath, deltaDegree);
        } else {
            return MathUtils.formatDegree(format180, deltaDegree);
        }
    }

    public double distanceFrom(CarPose pose) {
        if (pose == null || pose == this) {
            return 0;
        }
        synchronized (deviceLock) {
            double[] thisPose = deviceTranslation;
            double[] lastPose = pose.deviceTranslation;
            return Math.sqrt((thisPose[0] - lastPose[0]) * (thisPose[0] - lastPose[0])
                    + (thisPose[1] - lastPose[1]) * (thisPose[1] - lastPose[1]));
        }
    }

    private void writeTranslationAndRotation(DataOutputStream out,
                                            double[] translation, double[] rotation) throws IOException {
        out.writeDouble(translation[0]);
        out.writeDouble(translation[1]);
        out.writeDouble(translation[2]);
        out.writeDouble(rotation[0]);
        out.writeDouble(rotation[1]);
        out.writeDouble(rotation[2]);
        out.writeDouble(rotation[3]);
    }

    private void readTranslationAndRotation(DataInputStream in,
                                           double[] translation, double[] rotation) throws IOException {
        translation[0] = in.readDouble();
        translation[1] = in.readDouble();
        translation[2] = in.readDouble();
        rotation[0] = in.readDouble();
        rotation[1] = in.readDouble();
        rotation[2] = in.readDouble();
        rotation[3] = in.readDouble();
    }

    synchronized public void writeToDataOutputStream(DataOutputStream out) throws IOException {
        out.writeDouble(timestamp);
        writeTranslationAndRotation(out, deviceTranslation, deviceRotation);
        writeTranslationAndRotation(out, cameraTranslation, cameraRotation);
    }

    synchronized public void readFromDataInputStream(DataInputStream in) throws IOException {
        timestamp = in.readDouble();
        synchronized (deviceLock) {
            readTranslationAndRotation(in, deviceTranslation, deviceRotation);
            mHasDevicePose = true;
        }
        synchronized (cameraLock) {
            readTranslationAndRotation(in, cameraTranslation, cameraRotation);
            mHasCameraPose = true;
        }
    }
}
