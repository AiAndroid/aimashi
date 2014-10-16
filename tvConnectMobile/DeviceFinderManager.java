package com.aimashi.tv.store;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by liuhuadong78@gmail.com on 10/16/14.
 */
public class DeviceFinderManager {
    final static String TAG = "DeviceFinderManager";

    Context     mContext;
    WifiManager mWifiMng;
    public DeviceFinderManager(Context context){
        mContext = context;
        mWifiMng = (WifiManager) mContext.getSystemService(mContext.WIFI_SERVICE);
    }

    public void start() {
        if(mWifiMng.getWifiState() == WifiManager.WIFI_STATE_ENABLED){
            UDPCommandProtocol.getInstance(mContext).startListen();
            UDPCommandProtocol.getInstance(mContext).bootup();

            rescheduleDeviceBroadcase(10*A_SECOND);
        }else {
            Log.d(TAG, "wifi is not connected");
        }
    }

    final int A_MINUTE = 60*1000;
    final int A_SECOND = 1000;
    public void alarmComing() {
        Log.d(TAG, "alarmComing");
        mHandler.obtainMessage(CAPABILITY_BROADCAST).sendToTarget();

        if (resumealarming) {
            final long nexttime = System.currentTimeMillis() + 10 * A_SECOND;
            rescheduleDeviceBroadcase(nexttime);
        }
    }


    public void rescheduleDeviceBroadcase(long newTime){
        Log.d(TAG, "rescheduleDeviceBroadcase");
        Intent i = new Intent(mContext, DeviceFinderService.class);
        i.setAction("com.aimashi.tv.ACTION_BROADCASE");
        PendingIntent pendingIntent = PendingIntent.getService(mContext.getApplicationContext(),  0, i, PendingIntent.FLAG_CANCEL_CURRENT);


        AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC, newTime, pendingIntent);
    }

    final int CAPABILITY_BROADCAST = 10;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CAPABILITY_BROADCAST:
                    if(mWifiMng.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                        Log.d(TAG, "bootup");
                        UDPCommandProtocol.getInstance(mContext).bootup();
                    }
                    break;
            }
        }
    };

    public void onStop() {
        stop();
    }

    public void stop() {
        UDPCommandProtocol.getInstance(mContext).stopListen();
    }

    static boolean resumealarming = true;
    public void resume() {
        resumealarming = true;

        //continue
        final long nexttime = System.currentTimeMillis() + 10 * A_SECOND;
        rescheduleDeviceBroadcase(nexttime);
    }

    public void pause() {
        resumealarming = false;
    }
}
