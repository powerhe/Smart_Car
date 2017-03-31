package com.lenovo.newdevice.tangocar.connection.net.wifi;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.lenovo.newdevice.tangocar.R;

import java.util.List;

/**
 * Created by liujk2 on 2017/2/21.
 */

public class ControllerSearchActivity extends AppCompatActivity {
    public static final int RESULT_FOUND_SSID = 100;
    public static final int RESULT_NO_SSID = 101;
    public static final String EXTRA_SSID = "ssid";
    private TextView mListTittle;
    private ListView mCarListView;
    private ListViewAdapter mListViewAdapter;

    private List<String> mCarList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller_search);

        mListTittle = (TextView) findViewById(R.id.car_list_title);
        mCarListView = (ListView) findViewById(R.id.car_list);
        mCarList = null;
        mListViewAdapter = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        doSearch();
    }

    private void doSearch() {
        setTextViewContent(mListTittle, R.string.list_title_searching);
        new ControllerSearchThread(this).start();
    }

    private void onSearchResult(int carCount, List<String> carList) {
        if (carCount <= 0) {
            setTextViewContent(mListTittle, R.string.list_title_no_controller);
            setResultAndFinish(null);
            return;
        } else if (carCount > 0) {
            setTextViewContent(mListTittle, R.string.list_title_has_controller);
            if (carCount == 1) {
                setResultAndFinish(carList.get(0));
                return;
            }
        }

        if (!carList.equals(mCarList)) {
            mCarList = carList;
            if (mListViewAdapter == null) {
                mListViewAdapter = new ListViewAdapter(this, mCarList);
                setupListViewItemOnclickListener();
            } else {
                mListViewAdapter.update(mCarList);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCarListView.setVisibility(View.VISIBLE);
                    mCarListView.setAdapter(mListViewAdapter);
                }
            });
        }
    }

    private void setupListViewItemOnclickListener() {
        mCarListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                String ssid = mCarList.get(position);
                setResultAndFinish(ssid);
            }
        });
    }

    private void setResultAndFinish(String ssid) {
        if (ssid == null) {
            setResult(RESULT_NO_SSID);
        } else {
            Intent resultIntent = new Intent(getIntent());
            resultIntent.putExtra(EXTRA_SSID, ssid);
            setResult(RESULT_FOUND_SSID, resultIntent);
        }
        finish();
    }

    private void setTextViewContent(final TextView textView, final int resId) {
        if (textView == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(resId);
            }
        });
    }

    public static void start(Activity activity, boolean forResult, int requestCode) {
        Intent intent = new Intent(activity, ControllerSearchActivity.class);
        if (forResult) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            activity.startActivity(intent);
        }
    }

    class ControllerSearchThread extends SearchThread {
        ControllerSearchThread(Activity activity) {
            super(activity);
        }

        public void onSearchResult(int count, List<String> list) {
            ControllerSearchActivity.this.onSearchResult(count, list);
        }
    }

    class ListViewAdapter extends BaseAdapter {
        View[] mItemViews;
        int mListCount;
        Activity mActivity;
        final Object mLock;

        public ListViewAdapter(Activity activity, List<String> mListString) {
            mActivity = activity;
            mLock = new Object();
            update(mListString);
        }

        private void update(List<String> mListString) {
            synchronized (mLock) {
                mListCount = mListString.size();
                mItemViews = new View[mListCount];
                for (int i = 0; i < mListCount; i++) {
                    TextView view = new TextView(mActivity);
                    view.setText(mListString.get(i));
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
                    mItemViews[i] = view;
                }
            }
        }

        @Override
        public int getCount() {
            synchronized (mLock) {
                return mListCount;
            }
        }

        @Override
        public View getItem(int position) {
            synchronized (mLock) {
                return mItemViews[position];
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                return mItemViews[position];
            return convertView;
        }
    }
}
