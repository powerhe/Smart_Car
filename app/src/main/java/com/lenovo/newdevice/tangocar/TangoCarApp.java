package com.lenovo.newdevice.tangocar;

import android.app.Application;

import com.lenovo.newdevice.tangocar.connection.net.wifi.ApControl;
import com.lenovo.newdevice.tangocar.utils.AppConfig;
import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.VoiceReporter;

public class TangoCarApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        AppConfig.init(this);
        ApControl.init(this);
        // Call this to prepare the reporter.
        VoiceReporter.from(this).report(null);
    }

    @Override
    public void onTerminate() {
        ApControl.destroy();
    }
}
