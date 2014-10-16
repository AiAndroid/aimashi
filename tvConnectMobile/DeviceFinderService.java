package com.aimashi.tv.store;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by lhd on 10/15/14.
 */
public class DeviceFinderService extends Service {
    private static final String TAG = "udp wifi";
    DeviceFinderManager DFM;
    @Override
    public void onCreate() {
        super.onCreate();

        DFM = new DeviceFinderManager(this);
        DFM.start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction()!= null &&  intent.getAction().equals("com.aimashi.tv.ACTION_BROADCASE")){
            DFM.alarmComing();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(DFM != null) {
            DFM.onStop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
