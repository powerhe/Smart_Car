package com.lenovo.newdevice.tangocar.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.lenovo.newdevice.tangocar.R;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2017/1/3.
 */

/**
 * Displays progress bar and text information while saving an map.
 */
public class SaveMapDialog extends AlertDialog {
    private ProgressBar mProgressBar;

    public SaveMapDialog(Context context) {
        super(context);
    }

    public void setProgress(int progress) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(progress);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_map_dialog);
        setCancelable(false);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (mProgressBar == null) {
            Log.e(TAG, "Unable to find view progress_bar.");
        }
    }
}
