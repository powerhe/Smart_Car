package com.lenovo.newdevice.tangocar.utils;

/**
 * Created by liujk2 on 2016/12/24.
 */

public class OrientationMath {
    private static final double NORMALIZATION_TOLERANCE = 0.00001;

    private double w, x, y, z;

    public OrientationMath(double orientation[]) {
        w = orientation[3];
        x = orientation[0];
        y = orientation[1];
        z = orientation[2];

        normalize();
    }
    /**
     * Calculates the square of the Euclidean length of this {@link Quaternion}.
     *
     * @return double The square of the Euclidean length.
     */
    public double length2() {
        return w * w + x * x + y * y + z * z;
    }

    /**
     * Get the pole of the gimbal lock, if any.
     *
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
     * @see <a href="https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java">
     * https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java</a>
     */
    private int getGimbalPole() {
        final double t = y * x + z * w;
        return t > 0.499 ? 1 : (t < -0.499 ? -1 : 0);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }

    /**
     * Multiplies each component of this {@link Quaternion} by the input
     * value.
     *
     * @param scalar double The value to multiply by.
     * @return A reference to this {@link Quaternion} to facilitate chaining.
     */
    private void multiply(double scalar) {
        w *= scalar;
        x *= scalar;
        y *= scalar;
        z *= scalar;
    }

    /**
     * Normalizes this {@link Quaternion} to unit length.
     *
     * @return double The scaling factor used to normalize this {@link Quaternion}.
     */
    private double normalize() {
        double len = length2();
        if (len != 0 && (Math.abs(len - 1.0) > NORMALIZATION_TOLERANCE)) {
            double factor = 1.0 / Math.sqrt(len);
            multiply(factor);
        }
        return len;
    }

    /**
     * Gets the roll angle from this {@link Quaternion}. This is defined as the rotation about the Z axis.
     *
     * @return double The roll angle in radians.
     * @see <a href="https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java">
     *     https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java</a>
     */
    public double getRoll() {
        final int pole = getGimbalPole();
        return pole == 0 ? Math.atan2(2.0 * (w * z + y * x), 1.0 - 2.0 * (x * x + z * z)) : pole * 2.0 * Math.atan2(y, w);
    }

    /**
     * Gets the pitch angle from this {@link Quaternion}. This is defined as the rotation about the X axis.
     *
     * @return double The pitch angle in radians.
     * @see <a href="https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java">
     *     https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java</a>
     */
    public double getPitch() {
        final int pole = getGimbalPole();
        return pole == 0 ? Math.asin(clamp(2.0 * (w * x - z * y), -1.0, 1.0)) : pole * Math.PI * 0.5;
    }

    /**
     * Gets the yaw angle from this {@link Quaternion}. This is defined as the rotation about the Y axis.
     *
     * @return double The yaw angle in radians.
     * @see <a href="https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java">
     *     https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/Quaternion.java</a>
     */
    public double getYaw() {
        return getGimbalPole() == 0 ? Math.atan2(2.0 * (y * w + x * z), 1.0 - 2.0 * (y * y + x * x)) : 0.0;
    }

}

