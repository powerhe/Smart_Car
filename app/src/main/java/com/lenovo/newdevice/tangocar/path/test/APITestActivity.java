package com.lenovo.newdevice.tangocar.path.test;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.lenovo.newdevice.tangocar.path.AbortSignal;
import com.lenovo.newdevice.tangocar.path.PathFinder;
import com.lenovo.newdevice.tangocar.path.finder.data.WeightedPoint;
import com.lenovo.newdevice.tangocar.path.finder.data.WorldMap;

import java.util.Queue;
import java.util.Random;

public class APITestActivity extends AppCompatActivity {


    private ProgressDialog mProgressDialog, mProgressDialog2;

    private AbortSignal mAbortSignal = new AbortSignal();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getClass().getSimpleName());

        mProgressDialog = new ProgressDialog(APITestActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAbortSignal.abort();
            }
        });

        mProgressDialog2 = new ProgressDialog(APITestActivity.this);
        mProgressDialog2.setIndeterminate(true);
        mProgressDialog2.setCancelable(false);

        mProgressDialog.setMessage("正在规划路径");
        mProgressDialog2.setMessage("正在处理一些事情");

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setGravity(LinearLayout.VERTICAL);

        Button cancelBtn = new Button(this);
        cancelBtn.setText("cancel test");

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                WeightedPoint start = new WeightedPoint(1, 2);
                WeightedPoint end = new WeightedPoint(1000, 1001);
                HeadlessMap_1200 map = new HeadlessMap_1200(start, end);


                PathFinder.builder(getApplicationContext())
                        .from(map.getStart())
                        .to(map.getGoal())
                        .map(map)
                        .nativeAlgo(true)
                        .abortSignal(mAbortSignal)
                        .listener(new PathFinder.FinderListener.Stub() {
                            @Override
                            public void onStart() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                        mProgressDialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .build().findAsync();

            }
        });

        Button largeBtn = new Button(this);
        largeBtn.setText("2000xy-c test");

        largeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                WeightedPoint start = new WeightedPoint(1, 2);
                WeightedPoint end = new WeightedPoint(1880, 1999);
                HeadlessMap_2000 map = new HeadlessMap_2000(start, end);

                PathFinder.builder(getApplicationContext())
                        .from(map.getStart())
                        .to(map.getGoal())
                        .map(map)
                        .nativeAlgo(true)
                        .abortSignal(mAbortSignal)
                        .listener(new PathFinder.FinderListener.Stub() {
                            @Override
                            public void onStart() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                        mProgressDialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .build().findAsync();

            }
        });

        Button largeBtnJ = new Button(this);
        largeBtnJ.setText("2000xy-java test");

        largeBtnJ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                WeightedPoint start = new WeightedPoint(1, 2);
                WeightedPoint end = new WeightedPoint(1880, 1999);
                HeadlessMap_2000 map = new HeadlessMap_2000(start, end);

                PathFinder.builder(getApplicationContext())
                        .from(map.getStart())
                        .to(map.getGoal())
                        .map(map)
                        .nativeAlgo(false)
                        .abortSignal(mAbortSignal)
                        .listener(new PathFinder.FinderListener.Stub() {
                            @Override
                            public void onStart() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                        mProgressDialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .build().findAsync();

            }
        });

        Button timeoutBtnJ = new Button(this);
        timeoutBtnJ.setText("setTimeout-3s-Java test");

        timeoutBtnJ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                WeightedPoint start = new WeightedPoint(1, 2);
                WeightedPoint end = new WeightedPoint(999, 1000);
                HeadlessMap_1200 map = new HeadlessMap_1200(start, end);

                PathFinder.builder(getApplicationContext())
                        .from(map.getStart())
                        .to(map.getGoal())
                        .map(map)
                        .nativeAlgo(false)
                        .abortSignal(mAbortSignal)
                        .timeout(3 * 1000)
                        .listener(new PathFinder.FinderListener.Stub() {
                            @Override
                            public void onStart() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                        mProgressDialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onTimeout() {
                                super.onTimeout();
                                onCancelled();
                            }

                            @Override
                            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .build().findAsync();

            }
        });

        Button timeoutBtnC = new Button(this);
        timeoutBtnC.setText("setTimeout-3s-C test");

        timeoutBtnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                WeightedPoint start = new WeightedPoint(1, 2);
                WeightedPoint end = new WeightedPoint(999, 1000);
                HeadlessMap_1200 map = new HeadlessMap_1200(start, end);

                PathFinder.builder(getApplicationContext())
                        .from(map.getStart())
                        .to(map.getGoal())
                        .map(map)
                        .nativeAlgo(true)
                        .abortSignal(mAbortSignal)
                        .timeout(3 * 1000)
                        .listener(new PathFinder.FinderListener.Stub() {
                            @Override
                            public void onStart() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                        mProgressDialog.show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }


                            @Override
                            public void onTimeout() {
                                super.onTimeout();
                                onCancelled();
                            }

                            @Override
                            public void onPathFound(@NonNull Queue<Point> pathPoints) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }

                            @Override
                            public void onPathNotFound(@NonNull PathFinder.FailureCause cause) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                            }
                        })
                        .build().findAsync();

            }
        });


        linearLayout.addView(cancelBtn);
        linearLayout.addView(largeBtn);
        linearLayout.addView(largeBtnJ);
        linearLayout.addView(timeoutBtnJ);
        linearLayout.addView(timeoutBtnC);
        setContentView(linearLayout);
    }

    private static class HeadlessMap_1200 extends WorldMap {

        public HeadlessMap_1200(WeightedPoint start, WeightedPoint goal) {
            super(null, start, goal);
        }

        @Override
        public int getWidth() {
            return 1200;
        }

        @Override
        public int getHeight() {
            return 1200;
        }

        @Override
        public boolean isTraversable(int x, int y) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
            return new Random().nextBoolean();
        }

        @Override
        public boolean isTraversable(WeightedPoint wp) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
            return new Random().nextBoolean();
        }

        @Override
        public boolean isStartTraversable() {
            return true;
        }

        @Override
        public boolean isGoalTraversable() {
            return true;
        }
    }

    private static class HeadlessMap_2000 extends WorldMap {

        public HeadlessMap_2000(WeightedPoint start, WeightedPoint goal) {
            super(null, start, goal);
        }

        @Override
        public int getWidth() {
            return 2000;
        }

        @Override
        public int getHeight() {
            return 2000;
        }

        @Override
        public boolean isTraversable(int x, int y) {
            return (x + y) % 3 == 0;
        }

        @Override
        public boolean isTraversable(WeightedPoint wp) {
            return new Random().nextBoolean();
        }

        @Override
        public boolean isStartTraversable() {
            return true;
        }

        @Override
        public boolean isGoalTraversable() {
            return true;
        }
    }
}
