package com.lenovo.newdevice.tangocar.utils;

import android.graphics.Point;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by liujk2 on 2017/2/24.
 */

public class MathUtils {

    public static final DecimalFormat FORMAT_THREE_DECIMAL = new DecimalFormat("0.000");
    private static final Random sRandom = new Random();
    private static final float DEG_TO_RAD = 3.1415926f / 180.0f;
    private static final float RAD_TO_DEG = 180.0f / 3.1415926f;

    private MathUtils() {
    }

    public static float abs(float v) {
        return v > 0 ? v : -v;
    }

    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    public static float log(float a) {
        return (float) Math.log(a);
    }

    public static float exp(float a) {
        return (float) Math.exp(a);
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static float max(int a, int b) {
        return a > b ? a : b;
    }

    public static float max(float a, float b, float c) {
        return a > b ? (a > c ? a : c) : (b > c ? b : c);
    }

    public static float max(int a, int b, int c) {
        return a > b ? (a > c ? a : c) : (b > c ? b : c);
    }

    public static float min(float a, float b) {
        return a < b ? a : b;
    }

    public static float min(int a, int b) {
        return a < b ? a : b;
    }

    public static float min(float a, float b, float c) {
        return a < b ? (a < c ? a : c) : (b < c ? b : c);
    }

    public static float min(int a, int b, int c) {
        return a < b ? (a < c ? a : c) : (b < c ? b : c);
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        return (float) Math.hypot(x, y);
    }

    public static float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        final float z = (z2 - z1);
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static float mag(float a, float b) {
        return (float) Math.hypot(a, b);
    }

    public static float mag(float a, float b, float c) {
        return (float) Math.sqrt(a * a + b * b + c * c);
    }

    public static float sq(float v) {
        return v * v;
    }

    public static float dot(float v1x, float v1y, float v2x, float v2y) {
        return v1x * v2x + v1y * v2y;
    }

    public static float cross(float v1x, float v1y, float v2x, float v2y) {
        return v1x * v2y - v1y * v2x;
    }

    public static float radians(float degrees) {
        return degrees * DEG_TO_RAD;
    }

    public static float degrees(float radians) {
        return radians * RAD_TO_DEG;
    }

    public static float acos(float value) {
        return (float) Math.acos(value);
    }

    public static float asin(float value) {
        return (float) Math.asin(value);
    }

    public static float atan(float value) {
        return (float) Math.atan(value);
    }

    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    public static float tan(float angle) {
        return (float) Math.tan(angle);
    }

    public static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    public static float norm(float start, float stop, float value) {
        return (value - start) / (stop - start);
    }

    public static float map(float minStart, float minStop, float maxStart, float maxStop, float value) {
        return maxStart + (maxStart - maxStop) * ((value - minStart) / (minStop - minStart));
    }

    public static int random(int howbig) {
        return (int) (sRandom.nextFloat() * howbig);
    }

    public static int random(int howsmall, int howbig) {
        if (howsmall >= howbig) return howsmall;
        return (int) (sRandom.nextFloat() * (howbig - howsmall) + howsmall);
    }

    public static float random(float howbig) {
        return sRandom.nextFloat() * howbig;
    }

    public static float random(float howsmall, float howbig) {
        if (howsmall >= howbig) return howsmall;
        return sRandom.nextFloat() * (howbig - howsmall) + howsmall;
    }

    public static void randomSeed(long seed) {
        sRandom.setSeed(seed);
    }

    public static boolean sFormat180 = true;

    public static float formatDegree(float degree) {
        if (sFormat180) {
            return formatDegree180(degree);
        } else {
            return formatDegree360(degree);
        }
    }

    public static float formatDegree(boolean format180, float degree) {
        if (format180) {
            return formatDegree180(degree);
        } else {
            return formatDegree360(degree);
        }
    }

    /* convert all degree to 0 - 359 */
    public static float formatDegree360(float degree) {
        degree = degree % 360;
        if (degree < 0) {
            degree = degree + 360;
        }
        return degree;
    }

    /* convert all degree to -179 - 180 */
    public static float formatDegree180(float degree) {
        degree = degree % 360;
        if (degree > 180) {
            degree = degree - 360;
        }
        if (degree <= -180) {
            degree = degree + 360;
        }
        return degree;
    }

    public static float getYawDegreeFromOrientation(boolean format180, OrientationMath orientationMath, float deltaDegree) {
        float degree = (float) Math.toDegrees(orientationMath.getYaw()) + deltaDegree;
        if (!format180) {
            return MathUtils.formatDegree360(degree);
        } else {
            return MathUtils.formatDegree180(degree);
        }
    }

    public static float calculateOrientation(Point srcPoint, Point dstPoint)
    {
        return (float) Math.toDegrees(Math.atan2(dstPoint.y-srcPoint.y, dstPoint.x-srcPoint.x));
    }

    public static float calculateDegreeDiff(float dir1, float dir2)
    {
        return MathUtils.formatDegree180(dir1 - dir2);
    }
}