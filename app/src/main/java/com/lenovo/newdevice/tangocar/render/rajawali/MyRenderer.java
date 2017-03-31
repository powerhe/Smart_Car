package com.lenovo.newdevice.tangocar.render.rajawali;

import android.content.Context;

import org.rajawali3d.renderer.RajawaliRenderer; // for rajawali 1.0.325
//import org.rajawali3d.renderer.Renderer; // for rajawali 1.1.777

/**
 * Created by liujk2 on 2016/12/28.
 */

public abstract class MyRenderer extends RajawaliRenderer {
    public MyRenderer(Context context) {
        super(context);
    }
}
