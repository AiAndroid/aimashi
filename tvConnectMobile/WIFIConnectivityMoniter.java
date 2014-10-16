package com.aimashi.tv.store;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by liuhuadong on 10/16/14.
 */
public class WIFIConnectivityMoniter extends BroadcastReceiver {
    final static String TAG = "udp receiver";
    DeviceFinderManager DFM;
    public WIFIConnectivityMoniter(){
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null)
            return;

        if(DFM == null ){
            DFM = new DeviceFinderManager(context);
        }

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            Log.d(TAG, "wifiState" + wifiState);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    DFM.stop();
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    DFM.stop();
                    break;
            }
        }

        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                Log.e(TAG, "isConnected" + isConnected);
                if (isConnected) {
                    //start listen
                    DFM.start();
                } else {
                    DFM.stop();
                }
            }
        }

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            Log.i(TAG, "网络状态改变:" + wifi.isConnected() + " 3g:" + gprs.isConnected());
            NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(TAG, "info.getTypeName()" + info.getTypeName());
                Log.d(TAG, "getSubtypeName()" + info.getSubtypeName());
                Log.d(TAG, "getState()" + info.getState());
                Log.d(TAG, "getDetailedState()" + info.getDetailedState().name());
                Log.d(TAG, "getDetailedState()" + info.getExtraInfo());
                Log.d(TAG, "getType()" + info.getType());

                if (NetworkInfo.State.CONNECTED == info.getState()) {
                    DFM.start();
                } else if (NetworkInfo.State.DISCONNECTING == info.getState() || NetworkInfo.State.DISCONNECTED == info.getState()) {
                    DFM.stop();
                }
            }
        }

        if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
            DFM.resume();
        }else if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            DFM.pause();
        }
    }
}
