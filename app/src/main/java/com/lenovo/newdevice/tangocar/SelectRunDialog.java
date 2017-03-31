package com.lenovo.newdevice.tangocar;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by liujk2 on 2017/3/9.
 */

public class SelectRunDialog extends AlertDialog implements View.OnClickListener {
    private Button mHostBtn;
    private Button mControllerBtn;
    private Button mTestBtn;
    private SelectCallback mCallback;
    private boolean mSupportTango;
    public SelectRunDialog(Context context, boolean supportTango, SelectCallback callback) {
        super(context);
        mCallback = callback;
        mSupportTango = supportTango;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_select_run);
        setCancelable(false);

        mHostBtn = (Button) findViewById(R.id.btn_host_mode);
        mControllerBtn = (Button) findViewById(R.id.btn_controller_mode);
        mTestBtn = (Button) findViewById(R.id.btn_test_view);

        mHostBtn.setOnClickListener(this);
        mControllerBtn.setOnClickListener(this);
        mTestBtn.setOnClickListener(this);

        if (!mSupportTango) {
            mHostBtn.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mHostBtn) {
            mCallback.onSelect(SelectCallback.OPTION_HOST);
            dismiss();
        } else if (view == mControllerBtn) {
            mCallback.onSelect(SelectCallback.OPTION_CONTROLLER);
            dismiss();
        } else if (view == mTestBtn) {
            mCallback.onSelect(SelectCallback.OPTION_TEST);
            dismiss();
        }
    }

    public interface SelectCallback {
        public static final int OPTION_HOST = 1;
        public static final int OPTION_CONTROLLER = 2;
        public static final int OPTION_TEST = 3;

        public void onSelect(int option);
    }
}
