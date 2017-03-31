package com.lenovo.newdevice.tangocar;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);
        addFragment(R.id.container, SettingsFragment.getInstance(), null, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
    }

    protected boolean addFragment(final int containerId, Fragment f, Bundle bundle, boolean animate) {

        if (isDestroyed() || f == null) {
            return false;
        }

        if (bundle != null) {
            f.setArguments(bundle);
        }

        if (!animate) {
            getFragmentManager().beginTransaction()
                    .replace(containerId, f).commit();
        } else {
            getFragmentManager().beginTransaction()
                    .replace(containerId, f).commit();
        }
        return true;
    }

    public static class SettingsFragment extends PreferenceFragment {
        public static SettingsFragment getInstance() {
            return new SettingsFragment();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
        }
    }
}
