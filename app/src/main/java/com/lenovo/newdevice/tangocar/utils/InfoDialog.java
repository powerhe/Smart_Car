package com.lenovo.newdevice.tangocar.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.R;

/**
 * Created by liujk2 on 2017/2/7.
 */

public class InfoDialog extends AlertDialog {
    private String mAlertInfo;
    private Button mDimissBtn;
    private TextView mInfoView;
    private Runnable mDoneAction;
    public InfoDialog(Context context, String alertInfo) {
        super(context);
        mAlertInfo = alertInfo;
        mDoneAction = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_dialog);
        setCancelable(false);
        mInfoView = (TextView) findViewById(R.id.alert_info);
        mInfoView.setText(mAlertInfo);
        mDimissBtn = (Button) findViewById(R.id.dismiss_button);
        mDimissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickOKButton(view);
            }
        });
    }

    public InfoDialog setDoneAction(Runnable runnable) {
        mDoneAction = runnable;
        return this;
    }

    private void onClickOKButton(View v) {
        dismiss();
        if (mDoneAction != null) {
            mDoneAction.run();
        }
    }

    public static void alert(ContextWrapper contextWrapper, String alertInfo) {
        new InfoDialog(contextWrapper, alertInfo).show();
    }

    public static void alert(ContextWrapper contextWrapper, String alertInfo, Runnable doneAction) {
        new InfoDialog(contextWrapper, alertInfo).setDoneAction(doneAction).show();
    }
}
