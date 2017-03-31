package com.lenovo.newdevice.tangocar.render.opengl;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liujk2 on 2017/1/7.
 */

public class GlColor {
    private static final float COLOR_DEPTH_00 = ((float)0x00)/((float)0xFF);
    private static final float COLOR_DEPTH_44 = ((float)0x44)/((float)0xFF);
    private static final float COLOR_DEPTH_88 = ((float)0x88)/((float)0xFF);
    private static final float COLOR_DEPTH_CC = ((float)0xCC)/((float)0xFF);
    private static final float COLOR_DEPTH_FF = ((float)0xFF)/((float)0xFF);

    public static final float[] GL_COLOR_BLACK       = {COLOR_DEPTH_00, COLOR_DEPTH_00, COLOR_DEPTH_00}; //0x000000
    public static final float[] GL_COLOR_DKGRAY      = {COLOR_DEPTH_44, COLOR_DEPTH_44, COLOR_DEPTH_44}; //0x444444
    public static final float[] GL_COLOR_GRAY        = {COLOR_DEPTH_88, COLOR_DEPTH_88, COLOR_DEPTH_88}; //0x888888
    public static final float[] GL_COLOR_LTGRAY      = {COLOR_DEPTH_CC, COLOR_DEPTH_CC, COLOR_DEPTH_CC}; //0xCCCCCC
    public static final float[] GL_COLOR_WHITE       = {COLOR_DEPTH_FF, COLOR_DEPTH_FF, COLOR_DEPTH_FF}; //0xFFFFFF
    public static final float[] GL_COLOR_RED         = {COLOR_DEPTH_FF, COLOR_DEPTH_00, COLOR_DEPTH_00}; //0xFF0000
    public static final float[] GL_COLOR_GREEN       = {COLOR_DEPTH_00, COLOR_DEPTH_FF, COLOR_DEPTH_00}; //0x00FF00
    public static final float[] GL_COLOR_BLUE        = {COLOR_DEPTH_00, COLOR_DEPTH_00, COLOR_DEPTH_FF}; //0x0000FF
    public static final float[] GL_COLOR_YELLOW      = {COLOR_DEPTH_FF, COLOR_DEPTH_FF, COLOR_DEPTH_00}; //0xFFFF00
    public static final float[] GL_COLOR_CYAN        = {COLOR_DEPTH_00, COLOR_DEPTH_FF, COLOR_DEPTH_FF}; //0x00FFFF
    public static final float[] GL_COLOR_MAGENTA     = {COLOR_DEPTH_FF, COLOR_DEPTH_00, COLOR_DEPTH_FF}; //0xFF00FF

    public static void glClear(GL10 gl10, float[] color, float alpha) {
        gl10.glClearColor(color[0], color[1], color[2], alpha);
        gl10.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);
    }
}
