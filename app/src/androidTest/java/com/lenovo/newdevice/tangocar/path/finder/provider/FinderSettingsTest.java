package com.lenovo.newdevice.tangocar.path.finder.provider;

import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FinderSettingsTest {

    @Test
    public void testObserver() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        FinderSettings.from(InstrumentationRegistry.getTargetContext())
                .addObserver(new Observer() {
                    @Override
                    public void update(Observable o, Object arg) {
                        Assert.assertTrue(arg.equals("key_debug"));
                        latch.countDown();
                    }
                });

        boolean old = FinderSettings.from(InstrumentationRegistry.getTargetContext()).debug();

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getTargetContext())
                .edit().putBoolean("key_debug", !old).apply();

        boolean ok = latch.await(10 * 1000, TimeUnit.MILLISECONDS);
        Assert.assertTrue(ok);
    }
}