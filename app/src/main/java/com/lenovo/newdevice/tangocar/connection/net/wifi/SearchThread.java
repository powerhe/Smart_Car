package com.lenovo.newdevice.tangocar.connection.net.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;

import com.lenovo.newdevice.tangocar.R;

import java.util.List;

/**
 * Created by liujk2 on 2017/2/21.
 */

public abstract class SearchThread extends Thread {
    private ProgressDialog mDialog;
    private Activity mActivity;

    public SearchThread(Activity activity) {
        super();
        mActivity = activity;
        mDialog = new ProgressDialog(mActivity);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setTitle(R.string.list_title_searching);
    }

    protected void showDialog(final Dialog dialog) {
        if (dialog == null) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
            }
        });
    }

    @Override
    public void run() {
        List<String> mCarList = null;
        int carCount = 0;
        int searchCount = 0;
        showDialog(mDialog);
        while (carCount <= 0) {
            mCarList = ApControl.getInstance().getTangoCarApSSIDList();
            carCount = mCarList.size();
            searchCount ++;
            if (searchCount >= 3) {
                break;
            }
        }
        mDialog.cancel();
        onSearchResult(carCount, mCarList);
    }

    public abstract void onSearchResult(int count, List<String> list);
}
