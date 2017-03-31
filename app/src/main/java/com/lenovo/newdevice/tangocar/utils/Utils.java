package com.lenovo.newdevice.tangocar.utils;

import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.Point;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.lenovo.newdevice.tangocar.map.GridMap;
import com.lenovo.newdevice.tangocar.path.AbortSignal;
import com.lenovo.newdevice.tangocar.path.PathFinder;
import com.lenovo.newdevice.tangocar.path.finder.provider.FinderSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;

/**
 * Created by liujk2 on 2016/12/24.
 */

public class Utils {
    public static final double UPDATE_INTERVAL_MS = 100.0;
    public static final int SECS_TO_MILLISECS = 1000;
    public static final long DEFAULT_MILLISECS_PER_WHILE = 100;
    private static final String LOG_NAME = "log.txt";
    public static int DIR_TYPE_PGM = 0;
    public static int DIR_TYPE_MAP = 1;
    private static Utils sInstance = null;
    private static ContextWrapper sContext = null;
    private File mPgmDir = null;
    private File mMapDir = null;
    private FileOutputStream mLogOut;
    private boolean mLogAppend;

    private Utils() {
        mLogOut = null;
        mLogAppend = true;
    }

    public static void init(ContextWrapper context) {
        if (sInstance == null) {
            sInstance = new Utils();
        }
        sContext = context;
    }

    public static void stop() {
        if (sInstance.mLogOut != null) {
            sInstance.closeOutputStream(sInstance.mLogOut);
            sInstance.mLogOut = null;
        }
    }

    public static Utils getInstance() {
        return sInstance;
    }

    public static void outLog(String logStr) {
        sInstance.outLog(logStr, sInstance.mLogAppend);
    }

    public static String getLog() {
        return sInstance.getLogInner();
    }

    public String getLogInner() {
        String log = "";
        if (mLogOut != null) {
            try {
                mLogOut.flush();
            } catch (IOException e) {
                Log.e(TAG, "flush log error:", e);
            }
        }
        FileInputStream fIn = openFileToRead(getFilesDir(), LOG_NAME);
        if (fIn == null) {
            return log;
        }
        try {
            int length = fIn.available();
            byte[] logBuffer = new byte[length];
            int off = 0;
            int reserve = length;
            int readLen = 0;
            while(true) {
                int len = fIn.read(logBuffer, off, reserve);
                readLen += len;
                if (readLen >= length) {
                    break;
                }
                off += len;
                reserve -= len;
            }
            log = new String(logBuffer);
        } catch (IOException e) {
            Log.e(TAG, "read log error:", e);
        }
        return log;
    }

    public static void outputMapToPgmFile(String fileName, GridMap map) {
        FileOutputStream fOut = sInstance.openFileToWrite(DIR_TYPE_PGM, fileName, false);
        try {
            map.writeToOutputStreamAsPgm(fOut);
        } catch (Exception e) {
            Log.e(TAG, "outputWorldMap error:", e);
        } finally {
            sInstance.closeOutputStream(fOut);
        }
    }

    // for thread
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            Log.e(TAG, "sleep error:", e);
        }
    }

    // for compute path
    public static void computeTargetPath(final GridMap map, final Point currentPoint,
                                         final Point targetPoint,
                                         final PathFinder.FinderListener finderListener,
                                         final AbortSignal signal) {
        PathFinder.Builder builder = PathFinder.builder(sContext)
                .from(currentPoint)
                .to(targetPoint)
                .carSize(FinderSettings.from(sContext).carSize())
                .map(map)
                .nativeAlgo(FinderSettings.from(sContext).useNativeAlgo())
                .abortSignal(signal);
        if (finderListener != null) {
            builder.listener(finderListener);
        }
        builder.build().findAsync();
    }

    private static Date sBuildDate = null;

    public static boolean isBuildUserOrUserDebug() {
        return (Build.TYPE.indexOf("user") >= 0);
    }

    public static Date getBuildDate() {
        if (sBuildDate == null) {
            String buildDataUtcStr = System.getProperty("ro.build.date.utc", "0");
            try {
                long buildDataUtc = Long.valueOf(buildDataUtcStr);
                sBuildDate = new Date(buildDataUtc);
            } catch (Exception e) {
                Log.e(TAG, "get build date error:", e);
                sBuildDate = null;
            }
        }
        return sBuildDate;
    }

    public File getFilesDir() {
        File baseDir = Environment.getExternalStorageDirectory();
        File pathMapDir = new File(baseDir, "/tango_car/");
        if (!pathMapDir.exists()) {
            pathMapDir.mkdirs();
        }
        Log.v(TAG, Utils.class.getName() + ".getFilesDir >>> " + pathMapDir);
        return pathMapDir;
    }

    synchronized private File getPgmDir() {
        if (mPgmDir == null) {
            mPgmDir = new File(getFilesDir(), "pgm");
        }
        if (!mPgmDir.exists()) {
            mPgmDir.mkdirs();
        }
        return mPgmDir;
    }

    synchronized private File getMapDir() {
        if (mMapDir == null) {
            mMapDir = new File(getFilesDir(), "map");
        }
        if (!mMapDir.exists()) {
            mMapDir.mkdirs();
        }
        return mMapDir;
    }

    public FileInputStream openFileToRead(File baseDir, String fileName) {
        FileInputStream fIn = null;
        File f = new File(baseDir, fileName);
        try {
            fIn = new FileInputStream(f);
        } catch (Exception e) {
            Log.e(TAG, "Open file error:", e);
        }
        return fIn;
    }

    public FileInputStream openFileToRead(int dirType, String fileName) {
        File dir = getDirByType(dirType);
        if (dir == null) {
            throw new RuntimeException("dir type error");
        }
        return openFileToRead(dir, fileName);
    }

    public FileOutputStream openFileToWrite(File baseDir, String fileName, boolean append) {
        FileOutputStream fout = null;
        File f = new File(baseDir, fileName);
        try {
            fout = new FileOutputStream(f, append);
        } catch (Exception e) {
            Log.e(TAG, "Open file error:", e);
        }
        return fout;
    }

    public FileOutputStream openFileToWrite(int dirType, String fileName, boolean append) {
        File dir = getDirByType(dirType);
        if (dir == null) {
            throw new RuntimeException("dir type error");
        }
        return openFileToWrite(dir, fileName, append);
    }

    public void closeOutputStream(FileOutputStream fout) {
        if (fout != null) {
            try {
                fout.close();
            } catch (Exception e) {
                Log.e(TAG, "close OutputStream error:", e);
            }
        }
    }

    public void closeInputStream(FileInputStream fin) {
        if (fin != null) {
            try {
                fin.close();
            } catch (Exception e) {
                Log.e(TAG, "close InputStream error:", e);
            }
        }
    }

    public void closeLogOut() {
        closeOutputStream(mLogOut);
        mLogOut = null;
    }

    public void outLog(String logStr, boolean append) {
        Log.d(TAG, String.valueOf(logStr));
        if (mLogOut == null) {
            mLogOut = openFileToWrite(getFilesDir(), LOG_NAME, append);
        }
        if (mLogOut != null && logStr != null) {
            try {
                mLogOut.write(logStr.getBytes());
                mLogOut.write('\n');
            } catch (Exception e) {
                Log.e(TAG, "Write file error:", e);
            }
        }
    }

    public void deleteAllPgmFiles() {
        File file = getPgmDir();

        File[] childFiles = file.listFiles();
        for (int i = 0; i < childFiles.length; i++) {
            childFiles[i].delete();
        }
    }

    public File getDirByType(int dirType) {
        File dir = null;
        if (dirType == DIR_TYPE_PGM) {
            dir = getPgmDir();
        } else if (dirType == DIR_TYPE_MAP) {
            dir = getMapDir();
        }
        return dir;
    }

    public static void showToast(final Activity activity, final CharSequence text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToast(final Activity activity, final int resId) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String sHostApName = null;
    public static String getDeviceApName() {
        if (sHostApName == null) {
            sHostApName = AppConfig.getConfig().getString(AppConfig.CFG_AP_NAME, null);
            if (sHostApName == null) {
                sHostApName = Build.SERIAL;
                AppConfig.getConfig().putString(AppConfig.CFG_AP_NAME, sHostApName);
            }
        }
        return sHostApName;
    }
}
