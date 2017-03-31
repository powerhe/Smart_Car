package com.lenovo.newdevice.tangocar.control.engine;

import android.app.Activity;

import com.lenovo.newdevice.tangocar.control.AdvanceControl;
import com.lenovo.newdevice.tangocar.control.CarControl;
import com.lenovo.newdevice.tangocar.data.GlobalData;

/**
 * Created by liujk2 on 2017/3/2.
 */

public class EngineExample extends BaseEngine {
    private static final String RUNNING = "running";
    private static final String STOP = "stop";
    private String mRunState = RUNNING;

    public EngineExample(Activity activity, GlobalData globalData, CarControl carControl, AdvanceControl advanceControl) {
        super(activity, globalData, carControl, advanceControl);
    }

    @Override
    protected String getEngineState() {
        return "RunState is " + mRunState;
    }

    @Override
    protected void doInit() {
        super.doInit();
    }

    @Override
    protected void doAction() {

    }

    @Override
    public void cancel() {
        super.cancel();
        mRunState = STOP;
    }

}
