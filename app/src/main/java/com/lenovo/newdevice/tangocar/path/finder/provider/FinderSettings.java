package com.lenovo.newdevice.tangocar.path.finder.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.lenovo.newdevice.tangocar.R;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicEnum;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicScheme;
import com.lenovo.newdevice.tangocar.path.finder.heuristic.HeuristicSquared;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEightDirections;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborEnum;
import com.lenovo.newdevice.tangocar.path.finder.neighbor.NeighborSelector;

import java.util.Observable;

public class FinderSettings extends Observable {

    private static final String KEY_SHUFFLE = "key_shuffle";
    private static String KEY_DIJKSTRA = "key_dijkstra";

    private static final String KEY_NEIGHBOR_SELECTOR = "key_neighbor_selector";
    private static final String KEY_HEURISTIC_SCHEMES = "key_heuristic_schemes";

    private static final String KEY_VALIDATE_START = "key_validate_start";
    private static final String KEY_VALIDATE_GOAL = "key_validate_goal";

    private static final String KEY_CAR_SIZE = "key_car_size";
    private static final String KEY_USE_NATIVE = "key_use_native";

    private static final String KEY_DEBUG = "key_debug";

    private static FinderSettings onlyMe;

    private SharedPreferences mPref;
    private Resources mRes;

    private FinderSettings(Context context) {
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
        mPref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                setChanged();
                notifyObservers(key);
            }
        });
        mRes = context.getResources();
    }

    public synchronized static FinderSettings from(Context context) {
        if (onlyMe == null) onlyMe = new FinderSettings(context);
        return onlyMe;
    }

    public boolean shuffle() {
        return mPref.getBoolean(KEY_SHUFFLE, false);
    }

    public boolean dijkstra() {
        return mPref.getBoolean(KEY_DIJKSTRA, false);
    }

    public boolean useNativeAlgo() {
        return mPref.getBoolean(KEY_USE_NATIVE, false);
    }

    public
    @NonNull
    NeighborSelector neighborSelector() {
        String neighborStr = mPref.getString(KEY_NEIGHBOR_SELECTOR, NeighborEightDirections.class.getSimpleName());
        NeighborEnum neighborEnum = NeighborEnum.valueOf(neighborStr);
        return neighborEnum.create();
    }

    public
    @NonNull
    NeighborEnum neighborEnum() {
        String neighborStr = mPref.getString(KEY_NEIGHBOR_SELECTOR, NeighborEightDirections.class.getSimpleName());
        return NeighborEnum.valueOf(neighborStr);
    }

    public
    @NonNull
    HeuristicScheme heuristicScheme() {
        String scheme = mPref.getString(KEY_HEURISTIC_SCHEMES, HeuristicSquared.class.getSimpleName());
        HeuristicEnum heuristicEnum = HeuristicEnum.valueOf(scheme);
        return heuristicEnum.create();
    }

    public
    @NonNull
    HeuristicEnum heuristicEnum() {
        String scheme = mPref.getString(KEY_HEURISTIC_SCHEMES, HeuristicSquared.class.getSimpleName());
        HeuristicEnum heuristicEnum = HeuristicEnum.valueOf(scheme);
        return heuristicEnum;
    }

    public int carSize() {
        String sizeStr = mPref.getString(KEY_CAR_SIZE, String.valueOf(mRes.getInteger(R.integer.default_car_size)));
        return Integer.parseInt(sizeStr);
    }

    public boolean validateStart() {
        return mPref.getBoolean(KEY_VALIDATE_START, mRes.getBoolean(R.bool.default_validate_start));
    }

    public boolean validateGoal() {
        return mPref.getBoolean(KEY_VALIDATE_GOAL, mRes.getBoolean(R.bool.default_validate_goal));
    }

    public boolean debug() {
        return mPref.getBoolean(KEY_DEBUG, mRes.getBoolean(R.bool.default_debug));
    }
}
