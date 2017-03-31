package com.lenovo.newdevice.tangocar.path.test;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.SettingsActivity;
import com.lenovo.newdevice.tangocar.path.AbortSignal;
import com.lenovo.newdevice.tangocar.path.PathFinder;
import com.lenovo.newdevice.tangocar.path.finder.data.TileMap;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.generator.GeneratorEnum;
import com.lenovo.newdevice.tangocar.path.finder.generator.GeneratorRandom;
import com.lenovo.newdevice.tangocar.path.finder.generator.MapGenerator;
import com.lenovo.newdevice.tangocar.path.finder.generator.MapManager;
import com.lenovo.newdevice.tangocar.path.finder.provider.FinderSettings;
import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.VoiceReporter;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CountDownLatch;


public class PathFinderTestActivity extends AppCompatActivity {

    public GridMapView mGridMapViewView;
    private WeightedPoint mStartPoint, mEndPoint;
    private Queue<Point> mPaths;
    private ProgressDialog mProgressDialog;

    private TileMap mMap;

    private CountDownLatch mLatch;

    private boolean mSetCarSize, mUseJni;

    private MapGenerator mGenerator;

    private AbortSignal mAbortSignal = new AbortSignal();

    private void autoTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    if (isDestroyed()) break;
                    randomTest();
                    waitForComplete();
                }
            }
        }).start();
    }

    private void randomTest() {
        mLatch = new CountDownLatch(1);
        generateMap();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setupUI();
            }
        });
        findPath();
        cleanUp();
    }

    private void waitForComplete() {
        while (true) {
            try {
                mLatch.await();
                Thread.sleep(3000);
                break;
            } catch (InterruptedException ignored) {

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        generateMap();
        setupUI();
    }

    private void initUI() {
        setContentView(R.layout.activity_gridmap_present);
        setTitle(getClass().getSimpleName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mProgressDialog = new ProgressDialog(PathFinderTestActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAbortSignal.abort();
            }
        });
        mProgressDialog.setMessage("正在规划路径");

        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(PathFinderTestActivity.this,
                R.array.map_generator_type,
                android.R.layout.simple_spinner_dropdown_item);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        actionBar.setListNavigationCallbacks(mSpinnerAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {

                mGenerator = GeneratorEnum.valueOf(getResources()
                        .getStringArray(R.array.map_generator_type)[itemPosition]).create();
                cleanUp();
                generateMap();
                setupUI();
                return true;
            }
        });

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void setupUI() {

        ViewGroup parent = (ViewGroup) findViewById(R.id.parent);

        parent.removeAllViews();

        mGridMapViewView = (GridMapView) LayoutInflater.from(this).inflate(R.layout.test_map_view, null, false);
        mGridMapViewView.setMaxSelected(0);

        parent.addView(mGridMapViewView);

        mGridMapViewView.setNodeChecker(new GridMapView.NodeChecker() {

            @Override
            public boolean isValidNode(int row, int column) {
                return true;
            }

            @Override
            public boolean isBarNode(int row, int column) {
                return !mMap.isTraversable(row, column);
            }

            @Override
            public boolean isPathNode(int x, int y) {
                return mPaths != null && (mPaths.contains(new Point(x, y))
                        || mPaths.contains(new WeightedPoint(x, y)));
            }

            @Override
            public void checked(int row, int column) {

            }

            @Override
            public void unCheck(int row, int column) {

            }

            @Override
            public String[] checkedSeatTxt(int row, int column) {
                if (mStartPoint != null && mStartPoint.equals(new WeightedPoint(row, column))) {
                    return new String[]{"始"};
                }
                if (mEndPoint != null && mEndPoint.equals(new WeightedPoint(row, column))) {
                    return new String[]{"终"};
                }
                return null;
            }

        });
        mGridMapViewView.setData(mMap.getWidth(), mMap.getHeight());
        mGridMapViewView.postInvalidate();
    }


    private void generateMap() {
        MapManager mapManager = MapManager.getInstance();
        ensureGenerator();
        mapManager.setGenerator(mGenerator);
        this.mMap = mapManager.generate(new Random().nextInt());
    }

    private void ensureGenerator() {
        if (mGenerator == null) mGenerator = new GeneratorRandom();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_presenter, menu);
        return true;
    }

    private void findPath() {

        mStartPoint = mMap.getStart();
        mEndPoint = mMap.getGoal();


        final long[] startTimeMills = new long[1];

        PathFinder.builder(this)
                .from(new Point(mStartPoint.getX(), mStartPoint.getY()))
                .to(new Point(mEndPoint.getX(), mEndPoint.getY()))
                .carSize(!mSetCarSize ? PathFinder.CAR_SIZE_IGNORED : FinderSettings.from(getApplicationContext()).carSize())
                .map(mMap)
                .abortSignal(mAbortSignal)
                .timeout(5 * 1000)
                .nativeAlgo(mUseJni)
                .listener(new PathFinder.FinderListener.Stub() {
                    @Override
                    public void onStart() {
                        startTimeMills[0] = System.currentTimeMillis();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                                mProgressDialog.show();
                            }
                        });
                    }

                    @Override
                    public void onPathFound(@NonNull Queue<Point> pathPoints) {
                        final long timeTaken = System.currentTimeMillis() - startTimeMills[0];
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showTimeTaken(timeTaken);
                            }
                        });
                        mPaths = pathPoints;
                        mGridMapViewView.postInvalidate();

                        Utils.outLog("onPathFound, path size: " + pathPoints.size());

                        for (Point p : pathPoints) {
                            Log.d("EndlessDebug", p.toString());
                        }

                        mProgressDialog.dismiss();

                        VoiceReporter.from(getApplicationContext()).report("成功");
                        if (mLatch != null && mLatch.getCount() > 0) {
                            mLatch.countDown();
                        }
                    }

                    @Override
                    public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                        mProgressDialog.dismiss();
                        VoiceReporter.from(getApplicationContext()).report("未找到路径");
                        if (mLatch != null && mLatch.getCount() > 0) {
                            mLatch.countDown();
                        }
                        mGridMapViewView.postInvalidate();

                        Utils.outLog("onPathNotFound:" + cause.toString());
                    }

                    @Override
                    public void onCancelled() {
                        super.onCancelled();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressDialog.dismiss();
                            }
                        });
                        VoiceReporter.from(getApplicationContext()).report("已经取消");
                        if (mLatch != null && mLatch.getCount() > 0) {
                            mLatch.countDown();
                        }
                        mGridMapViewView.postInvalidate();
                    }
                })
                .build()
                .findAsync();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_navigate) {
            findPath();
        }

        if (item.getItemId() == R.id.action_reset) {
            cleanUp();
            generateMap();
            setupUI();
        }

        if (item.getItemId() == R.id.action_playground) {
            autoTest();
        }

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        if (item.getItemId() == R.id.action_api) {
            startActivity(new Intent(this, APITestActivity.class));
        }

        if (item.getItemId() == R.id.action_car_size) {
            mSetCarSize = !item.isChecked();
            item.setChecked(!item.isChecked());
        }

        if (item.getItemId() == R.id.action_jni) {
            mUseJni = !item.isChecked();
            item.setChecked(!item.isChecked());
        }

        if (item.getItemId() == android.R.id.home) finish();

        return true;
    }

    void cleanUp() {
        if (mPaths != null) mPaths.clear();
        mGridMapViewView.clearSelection();
        mGridMapViewView.postInvalidate();
        mStartPoint = null;
        mEndPoint = null;
    }

    void showTimeTaken(long timeMills) {
        Snackbar.make(mGridMapViewView, "总时间：" + timeMills + "毫秒", Snackbar.LENGTH_SHORT)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Null hook.
                    }
                }).show();
    }
}
