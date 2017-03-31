package com.lenovo.newdevice.tangocar.connection.net.wifi;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.lenovo.newdevice.tangocar.utils.Utils;
import com.lenovo.newdevice.tangocar.utils.WifiExtManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.lenovo.newdevice.tangocar.MainActivity.TAG;
/**
 * Created by liujk2 on 2017/2/6.
 */

public class ApControl {
    protected static final String AP_NAME_PREFIX = "TANGO_CAR_";
    protected static final String AP_PRE_SHARE_KEY = "TangoCar852753";

    protected WifiManager mWifiManager;
    protected WifiExtManager mWifiExtManager;
    protected HashMap<String, ScanResult> mTangoCarApMap;
    private final Object mApListLock;
    private WifiReceiver mWifiReceiver;
    private WifiApCallback mWifiApCallback;

    private PendingConnection mPendingConnection;

    private ContextWrapper mContext;
    private boolean mReady;
    private boolean mWifiConnected;
    private WifiInfo mConnectedWifi;

    private static ApControl sInstance;
    private static Application sApp;
    public static ApControl getInstance() {
        return sInstance;
    }

    public static void init(Application app) {
        boolean resetManager = false;
        if (sApp != null && sApp != app) {
            resetManager = true;
        }
        sApp = app;
        if (sInstance == null) {
            sInstance = new ApControl();
        } else if (resetManager) {
            sInstance.createManager();
        }
    }

    private static void resetManager() {
        sApp = null;
        if (sInstance != null) {
            sInstance.mWifiManager = null;
            sInstance.mWifiExtManager = null;
        }
    }

    public static void destroy() {
        if (sInstance != null) {
            sInstance.reset();
        }
        resetManager();
    }

    private void createManager() {
        if (sApp == null) {
            throw new IllegalArgumentException("app is null");
        }
        mWifiManager = (WifiManager) sApp.getSystemService(Context.WIFI_SERVICE);
        mWifiExtManager = new WifiExtManager(mWifiManager);
    }

    private ApControl() {
        createManager();
        mTangoCarApMap = new HashMap<String, ScanResult>();
        mApListLock = new Object();
        mWifiReceiver = new WifiReceiver();
        mPendingConnection = new PendingConnection();
    }

    public void setup(ContextWrapper context) {
        if (mContext != null) {
            if (mContext == context) {
                return;
            } else {
                reset();
            }
        }
        mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiExtManager.WIFI_AP_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiReceiver, intentFilter);
        mWifiConnected = mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
        mConnectedWifi = mWifiManager.getConnectionInfo();
        mReady = true;
    }

    public void reset() {
        mReady = false;
        if (mContext != null) {
            mContext.unregisterReceiver(mWifiReceiver);
            mContext = null;
        }
        mWifiConnected = false;
        mConnectedWifi = null;
    }

    public boolean ready() {
        return mReady;
    }

    private void checkReady() {
        if (!ready()) {
            throw new RuntimeException("not ready!");
        }
    }

    private WifiConfiguration generateApConfig(String apSSID, String setPassword) {
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = apSSID;
        apConfig.preSharedKey = setPassword;
        apConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        apConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        return apConfig;
    }

    private WifiConfiguration generateWifiConfig(String apSSID, ScanResult scanResult, String setPassword) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + apSSID + "\"";
        wifiConfig.BSSID = scanResult.BSSID;

        boolean wapi_psk = scanResult.capabilities.contains("WAPI-PSK");
        boolean wapi_cert = scanResult.capabilities.contains("WAPI-CERT");
        boolean wep = scanResult.capabilities.contains("WEP");
        boolean psk = scanResult.capabilities.contains("PSK");
        boolean eap = scanResult.capabilities.contains("EAP");
        boolean open = false;

        Log.i(TAG, "scan ap type{eap:"+eap+",psk:"+psk+",wep:"+wep+",wapi_cert:"+wapi_cert+",wapi_psk:"+wapi_psk+"}");

        if (!eap && !psk && !wep && !wapi_cert && !wapi_psk) {
            open = true;
        }

        if (open) {
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            if (psk) {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

                if (setPassword.matches("[0-9A-Fa-f]{64}")) {
                    wifiConfig.preSharedKey = setPassword;
                } else {
                    wifiConfig.preSharedKey = '"' + setPassword + '"';
                }
            } else if (wep) {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

                int length = setPassword.length();
                if ((length == 10 || length == 26 || length == 58) && setPassword.matches("[0-9A-Fa-f]*")) {
                    wifiConfig.wepKeys[0] = setPassword;
                } else {
                    wifiConfig.wepKeys[0] = '"' + setPassword + '"';
                }
            }
        }
        return wifiConfig;
    }

    public void stopAp() {
        int apState = mWifiExtManager.getWifiApState();
        Log.i(TAG, "wifi ap state is " + apState);
        if (apState == WifiExtManager.WIFI_AP_STATE_ENABLED
                || apState == WifiExtManager.WIFI_AP_STATE_ENABLING) {
            mWifiExtManager.setWifiApEnabled(null, false);
        }
    }

    public int startTangoCarAp(String ssid, WifiApCallback wifiApCallback) {
        checkReady();
        if (ssid == null) {
            throw new IllegalArgumentException("ssid is null");
        }
        String apSSID = AP_NAME_PREFIX + ssid;
        int apState = mWifiExtManager.getWifiApState();
        Log.i(TAG, "wifi ap state is " + apState);
        if (apState == WifiExtManager.WIFI_AP_STATE_ENABLED
                || apState == WifiExtManager.WIFI_AP_STATE_ENABLING) {
            WifiConfiguration apConfig = mWifiExtManager.getWifiApConfiguration();
            if (apConfig != null && apConfig.SSID.equals(apSSID)) {
                return 1;
            } else {
                mWifiExtManager.setWifiApEnabled(null, false);
            }
        }

        if (mWifiApCallback != null) {
            throw new IllegalArgumentException("can not call startTangoCarAp before start ap over!");
        }

        mWifiApCallback = wifiApCallback;
        WifiConfiguration apConfig = generateApConfig(apSSID, AP_PRE_SHARE_KEY);
        mWifiExtManager.setWifiApEnabled(apConfig, true);
        return 0;
    }

    private void createThreadToWaitWifiConnected() {
        Thread waitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int retryCount = 0;
                while (ready()) {
                    Utils.sleep(3000);
                    synchronized (mPendingConnection.lock) {
                        if (retryCount > 3) {
                            if (mPendingConnection.valid) {
                                mPendingConnection.valid = false;
                                mPendingConnection.wifiCallback.onDisconnected();
                            }
                            break;
                        }
                        if (mPendingConnection.valid && ready()) {
                            retryCount ++;
                            connectToTangoCarAp(mPendingConnection.ssid, mPendingConnection.wifiCallback);
                        } else {
                            break;
                        }
                    }
                }
            }
        });
        waitThread.setName("WaitWifiConnected");
        waitThread.start();
    }

    public List<String> getTangoCarApSSIDList() {
        List<String> apList = new ArrayList<String>();
        if (!mReady) {
            return apList;
        }
        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        Log.i(TAG, "begin to scan!");
        mWifiManager.startScan();
        Log.i(TAG, "wait for scan result");
        List<ScanResult> wifiList = mWifiReceiver.getWifiList();
        Log.i(TAG, "after get scan result!");
        mTangoCarApMap.clear();
        for (ScanResult scanResult : wifiList) {
            String apSSID = scanResult.SSID;
            if (apSSID.startsWith(AP_NAME_PREFIX)) {
                Log.i(TAG, "found : " + apSSID);
                String ssid = apSSID.substring(AP_NAME_PREFIX.length());
                apList.add(ssid);
                mTangoCarApMap.put(ssid, scanResult);
            }
        }
        return apList;
    }

    public int connectToTangoCarAp(String ssid, WifiCallback wifiCallback) {
        checkReady();
        Log.i(TAG, "connectToTangoCarAp(" + ssid + ")");
        String apSSID = AP_NAME_PREFIX + ssid;
        ScanResult scanResult = mTangoCarApMap.get(ssid);
        if (scanResult == null) {
            Log.e(TAG, "Invalid SSID is " + ssid);
            return -1;
        }
        Log.i(TAG, "apSSID is {" + apSSID + "}");

        int netId = -1;
        WifiConfiguration wifiConfig = null;
        List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifiConfigurations) {
            if (apSSID.equals(config.SSID) && scanResult.BSSID.equals(config.BSSID)) {
                if (config.networkId != -1) {
                    wifiConfig = config;
                    netId = config.networkId;
                    break;
                } else {
                    Log.w(TAG, "skeep invalid netId");
                }
            }
        }
        Log.i(TAG, "found wifi config, netId is "+ netId +", {" + wifiConfig + "}");
        boolean saveConfig = false;
        if (wifiConfig == null) {
            wifiConfig = generateWifiConfig(apSSID, scanResult, AP_PRE_SHARE_KEY);
            netId = mWifiManager.addNetwork(wifiConfig);
            saveConfig = true;
        }
        if (wifiConfig == null) {
            Log.w(TAG, "wifi config generate failed!");
            return -1;
        }
        if (netId == -1) {
            Log.w(TAG, "invalid netId");
            return -1;
        }

        if (wifiConfigConnected(wifiConfig)) {
            return 1;
        }

        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            mWifiManager.disconnect();
        }

        synchronized (mPendingConnection.lock) {
            if (mPendingConnection.valid) {
                Log.w(TAG, "call connectToTangoCarAp before connect to ap over!");
                if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED
                        && mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) {
                    if (mWifiManager.enableNetwork(netId, true)) {
                        mWifiManager.reconnect();
                    }
                }
                return -1;
            }
        }

        boolean connectRes = false;
        Log.i(TAG, "wifi config, netId is "+ netId +", {" + wifiConfig + "}");
        connectRes = mWifiManager.enableNetwork(netId, true);
        Log.i(TAG, "after enableNetwork(" + netId + ") return " + connectRes);

        synchronized (mPendingConnection.lock) {
            mPendingConnection.ssid = ssid;
            mPendingConnection.wifiCallback = wifiCallback;
            mPendingConnection.wifiConfig = wifiConfig;
            if (!mPendingConnection.valid) {
                mPendingConnection.valid = true;
                createThreadToWaitWifiConnected();
            }
        }

        if (connectRes) {
            if (saveConfig) {
                Log.i(TAG, "call saveConfiguration()");
                mWifiManager.saveConfiguration();
                Log.i(TAG, "after saveConfiguration()");
            }
            connectRes = mWifiManager.reconnect();
            Log.i(TAG, "after reconnect() return " + connectRes);
        }

        if (!connectRes) {
            return -1;
        }

        return 0;
    }

    private boolean wifiConfigConnected(WifiConfiguration wifiConfig) {
        if (wifiConfig == null) {
            return false;
        }
        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return false;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getSSID().equals(wifiConfig.SSID)
                && wifiInfo.getBSSID().equals(wifiConfig.BSSID)) {
            return true;
        }
        return false;
    }

    private String intToIpString(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }

    public String getDhcpGatewayIP() {
        Log.i(TAG, "getDhcpGatewayIP()");
        DhcpInfo dhcpInfo = null;
        while (dhcpInfo == null) {
            dhcpInfo = mWifiManager.getDhcpInfo();
            Utils.sleep(100);
        }
        Log.i(TAG, "getDhcpGatewayIP, end wait, dhcpInfo is " + dhcpInfo);
        String gateWay = intToIpString(dhcpInfo.gateway);
        Log.i(TAG, "gateWay IP is " + gateWay);
        return gateWay;
    }

    public static interface WifiApCallback {
        public void onEnabled(boolean success);
    }

    public static interface WifiCallback {
        public void onConnected();
        public void onDisconnected();
    }

    class WifiReceiver extends BroadcastReceiver {
        List<ScanResult> mScanList;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "WifiReceiver.onReceive " + intent);
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                Log.i(TAG, "EXTRA_RESULTS_UPDATED is " + success);
                List<ScanResult> scanList;
                scanList = mWifiManager.getScanResults();
                synchronized (mApListLock) {
                    mScanList = scanList;
                    try {
                        mApListLock.notify();
                    } catch (Exception e) {
                    }
                }
            } else if (WifiExtManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "mWifiApCallback is " + mWifiApCallback);
                int wifiApState = intent.getIntExtra(WifiExtManager.EXTRA_WIFI_AP_STATE,
                        WifiExtManager.WIFI_AP_STATE_FAILED);
                Log.i(TAG, "wifiApState is " + wifiApState);
                if (wifiApState == WifiExtManager.WIFI_AP_STATE_FAILED) {
                    Log.i(TAG, "wifiApState changed to WIFI_AP_STATE_FAILED");
                    mWifiExtManager.setWifiApEnabled(null, false);
                    if (mWifiApCallback != null) {
                        mWifiApCallback.onEnabled(false);
                        mWifiApCallback = null;
                    }
                } else if (wifiApState == WifiExtManager.WIFI_AP_STATE_ENABLED) {
                    Log.i(TAG, "wifiApState changed to WIFI_AP_STATE_ENABLED");
                    if (mWifiApCallback != null) {
                        mWifiApCallback.onEnabled(true);
                        mWifiApCallback = null;
                    }
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                Log.i(TAG, "mPendingConnection is " + (mPendingConnection.valid ? "valid" : "invalid"));
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();
                Log.i(TAG, "networkInfo is " + networkInfo + ", detailedState is " + detailedState);
                if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
                    mWifiConnected = true;
                    mConnectedWifi = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    Log.i(TAG, "wifi connected to wifiInfo " + mConnectedWifi);
                    synchronized (mPendingConnection.lock) {
                        if (mPendingConnection.valid) {
                            if (mPendingConnection.wifiConfig != null) {
                                if (mConnectedWifi.getSSID().equals(mPendingConnection.wifiConfig.SSID)
                                        && mConnectedWifi.getBSSID().equals(mPendingConnection.wifiConfig.BSSID)) {
                                    Log.i(TAG, "mWifiCallback.onConnected()");
                                    mPendingConnection.valid = false;
                                    mPendingConnection.wifiCallback.onConnected();
                                }
                            }
                        }
                    }
                } else {
                    if (detailedState != NetworkInfo.DetailedState.CONNECTING) {
                        mWifiConnected = false;
                    }
                    synchronized (mPendingConnection.lock) {
                        if (mPendingConnection.valid) {
                            if (detailedState == NetworkInfo.DetailedState.FAILED) {
                                Log.i(TAG, "connect failed, mWifiCallback.onDisconnected()");
                                mPendingConnection.valid = false;
                                mPendingConnection.wifiCallback.onDisconnected();
                            }
                        }
                    }
                }
            }
        }

        public List<ScanResult> getWifiList() {
            synchronized (mApListLock) {
                try {
                    mApListLock.wait();
                } catch (Exception e) {
                }
                return mScanList;
            }
        }
    }

    class PendingConnection {
        public final Object lock = new Object();
        public boolean valid;
        public String ssid;
        public WifiCallback wifiCallback;
        public int netId;
        public WifiConfiguration wifiConfig;
    }
}
