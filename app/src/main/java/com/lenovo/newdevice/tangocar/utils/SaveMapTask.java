package com.lenovo.newdevice.tangocar.utils;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;
import com.lenovo.newdevice.tangocar.map.GridMap;

import static com.lenovo.newdevice.tangocar.utils.AppConfig.CFG_LAST_MAP_NAME;

/**
 * Created by liujk2 on 2017/1/3.
 */

/**
 * Saves the ADF on a background thread and shows a progress dialog while
 * saving.
 */
public class SaveMapTask extends AsyncTask<Void, Integer, String> {
    /**
     * Listener for the result of the async ADF saving task.
     */
    public interface SaveMapListener {
        void onSaveMapFailed(String mapName);
        void onSaveMapSuccess(String mapName, String adfUuid);
    }

    Activity mActivity;
    SaveMapListener mCallbackListener;
    SaveMapDialog mProgressDialog;
    Tango mTango;
    GridMap mMap;
    String mMapName;

    public SaveMapTask(Activity activity, SaveMapListener callbackListener, Tango tango, GridMap map, String mapName) {
        mActivity = activity;
        mCallbackListener = callbackListener;
        mTango = tango;
        mMap = map;
        mMapName = mapName;
        mProgressDialog = new SaveMapDialog(activity);
    }

    /**
     * Sets up the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    /**
     * Performs long-running save in the background.
     */
    @Override
    protected String doInBackground(Void... params) {
        String adfUuid = null;
        try {

            Utils.outputMapToPgmFile(mMapName + ".pgm", mMap);
            SerializableUtils.writeObjectToFile(Utils.DIR_TYPE_MAP, mMapName + ".bin", mMap);

            // Save the ADF.
            adfUuid = mTango.saveAreaDescription();

            // Read the ADF Metadata, set the desired name, and save it back.
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mMapName.getBytes());
            mTango.saveAreaDescriptionMetadata(adfUuid, metadata);

            AppConfig.getConfig().putString(CFG_LAST_MAP_NAME, mMapName);

            mActivity.finish();
        } catch (TangoErrorException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        } catch (TangoInvalidException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        }
        return adfUuid;
    }

    /**
     * Responds to progress updates events by updating the UI.
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    /**
     * Dismisses the progress dialog and call the activity.
     */
    @Override
    protected void onPostExecute(String adfUuid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mCallbackListener != null) {
            if (adfUuid == null) {
                mCallbackListener.onSaveMapFailed(mMapName);
            } else {
                mCallbackListener.onSaveMapSuccess(mMapName, adfUuid);
            }
        }
    }
}
