package com.aimashi.tv.store;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.aimashi.tv.app.data.download.XMDownloadManager;
import com.aimashi.tv.app.utils.Constants;
import com.aimashi.tv.app.utils.WLUIUtils;
import com.aimashi.tv.store.utils.Utils;
import com.aimashi.tv.store.video.PlayerActivity;
import com.google.gson.Gson;
import com.tv.ui.metro.model.VideoItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;

/**
 * Created by liuhuadong on 10/15/14.
 */
public class UDPCommandProtocol {

    private Context mContext;
    private PackageManager pm;

    private UDPCommandProtocol(Context context){
        mContext = context.getApplicationContext();
        pm = mContext.getPackageManager();
    }
    private static UDPCommandProtocol _instance;
    public static  UDPCommandProtocol getInstance(Context context){
        if(_instance == null){
            _instance = new UDPCommandProtocol(context);
        }

        return _instance;
    }

    static HashMap<String, UDPComnnad> serviceHost = new HashMap<String, UDPComnnad>();

    public static boolean hasTVConnected(Context context){
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && serviceHost.size() > 0){
            return  true;
            /*
            long now = System.currentTimeMillis();

            for(UDPComnnad comnad: serviceHost.values()){
                if(now - comnad.sendDate  < 60*1000){
                    return  true;
                }
            }
            */
        }

        return false;
    }

    public static HashMap<String, UDPComnnad> getConnectedHost(){
        return serviceHost;
    }

    private static final String TAG = "udp connect";
    public static final int PORT = 59110;
    public static final int SERVER_PORT = 59111;


    public static final  int SEND    = 10;
    public static final  int RECEIVE = 11;
    public static final  int SERVER_SEND = 12;

    private String mLogText = "";
    private Handler timestamp = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SEND:
                    Log.d(TAG, "send data ="+(String)msg.obj);
                    break;
                case RECEIVE:
                    Log.d(TAG, "receive data ="+(String)msg.obj);
                    break;
                case SERVER_SEND:
                    Log.d(TAG, "server send back data ="+(String)msg.obj);
                    break;

            }
        }
    };


    ArrayList<Socket> clients = new ArrayList<Socket>();
    private Listen    mListener;
    private TCPListen serverListener;
    public void bootup() {
        final  UDPComnnad uc = new UDPComnnad();
        uc.ns             = "device";
        uc.command        = "startup";
        uc.id             = "0";
        uc.params         = Build.PRODUCT;
        uc.server_mode    = (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        uc.sendDate       = System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress addr = InetAddress.getByName("255.255.255.255");
                    broadcast(addr, toJSON(uc).getBytes());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "send").start();
    }

    ArrayList<UDPComnnad> commands = new ArrayList<UDPComnnad>();
    static int CommandID = 0;
    public static class  UDPComnnad implements Serializable {
        private static final long serialVersionUID = 2L;
        public String ns;      //app, video
        public String command; //install, uninstall, play
        public String id;      //appid, video id
        public String params;  //parameters (package name, video url)
        public String feedback;//OK
        public String feedback_msg;//

        public boolean server_mode;
        public int     commandID;
        public int     state;
        public long    sendDate;
        public String  device;

        public UDPComnnad(){
            commandID = CommandID++;
            sendDate  = System.currentTimeMillis();
        }
    }


    private static Gson mGson;
    private static String toJSON(UDPComnnad comnnad){
        if(mGson == null){
            mGson = new Gson();
        }

        return mGson.toJson(comnnad);
    }

    private static UDPComnnad fromJson(String json){
        if(mGson == null){
            mGson = new Gson();
        }
        return mGson.fromJson(json, UDPComnnad.class);
    }

    public void  startListen(){
        if(mListener != null){
            mListener.stopListener();
            mListener.cancel(true);
            mListener = null;
        }

        if(mListener == null) {
            Log.d(TAG, "startListen create listen");

            mListener = new Listen();
            mListener.execute();
        }

        synchronized (TCPListen.class) {
            if (serverListener == null && mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                serverListener = new TCPListen();
                serverListener.execute();
            }
            for(Socket item: clients){
                try{
                    item.close();
                }catch (Exception ne){}
            }

            clients.clear();
        }
    }

    public void stopListen(){
        if(mListener != null){
            Log.d(TAG , "call stop listen");
            mListener.stopListener();
            boolean stoped = mListener.cancel(true);
            Log.d(TAG , "stopped="+stoped);
            mListener = null;
        }

        synchronized (TCPListen.class) {
            if (serverListener != null){
                serverListener.stopListener();
                serverListener.cancel(true);
                serverListener = null;
            }

            for(Socket item: clients){
                try{
                    item.close();
                }catch (Exception ne){}
            }

            clients.clear();
        }

        serviceHost.clear();
    }

    public void broadcast(final UDPComnnad command){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(serviceHost.size() > 0) {
                    byte []buffer = new byte[1024];
                    Set<String> sets = serviceHost.keySet();
                    Iterator<String> its = sets.iterator();
                    while (its.hasNext()) {
                        String ip = its.next();
                        try {
                            Socket tvSocket = new Socket(ip, SERVER_PORT);
                            tvSocket.setSoTimeout(5*1024);
                            OutputStream osp = tvSocket.getOutputStream();
                            osp.write(toJSON(command).getBytes());

                            InputStream is = tvSocket.getInputStream();
                            int len = is.read(buffer);
                            isReplyHeader(buffer, len);
                            is.close();
                            osp.flush();
                            osp.close();
                            tvSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else {//go broadcast
                    try {
                        InetAddress addr = InetAddress.getByName("255.255.255.255");
                        broadcast(addr, toJSON(command).getBytes());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "send").start();
    }

    private void executeCommand(InetAddress ip, UDPComnnad data)throws Exception{
        //let app call command
        try {
            if (data.ns.equals("app") || data.ns.equals("game")) {
                if (data.command.equals("install")) {
                    String id = data.id;
                    String param = data.params;
                    try {
                        ApplicationInfo info = pm.getApplicationInfo(param, 0);
                        if (info != null) {
                            WLUIUtils.openGame(mContext, param, "from_remote_phone");

                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        XMDownloadManager.getInstance().appendDownloadTask(id);
                    }
                }
            } else if (data.ns.equals("video")) {
                Intent intent = new Intent();
                intent.setAction(Constants.MITV_VIDEO_INTENT_ACTION);
                intent.setData(Uri.parse(data.params));
                try {
                    mContext.startActivity(intent);
                } catch (Exception e) {
                    Intent videoIntent = new Intent(mContext, PlayerActivity.class);
                    videoIntent.putExtra("video_url", data.params);
                    videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(videoIntent);
                }
            } else if (data.ns.equals("service_notify")) {

            } else {
                Toast.makeText(mContext, data.command + " " + data.params, Toast.LENGTH_LONG).show();
            }

            data.feedback = "OK";
            data.device = Build.MANUFACTURER;
            data.server_mode = (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
            data.sendDate = System.currentTimeMillis();

            DatagramSocket dgSocket = new DatagramSocket();
            byte[] bdate = toJSON(data).getBytes();
            DatagramPacket dgPacket = new DatagramPacket(bdate, bdate.length, ip, PORT);
            dgSocket.send(dgPacket);
            Message msg = timestamp.obtainMessage(SEND);
            msg.obj = "send replay at:" + System.currentTimeMillis() + " to:" + ip.getHostAddress();
            msg.sendToTarget();
            dgSocket.close();
        }catch (Exception ne){ne.printStackTrace();}
    }

    private void broadcast(InetAddress ip, byte[] data)throws Exception{
        DatagramSocket dgSocket=new DatagramSocket();
        dgSocket.setBroadcast(true);
        dgSocket.setReuseAddress(true);
        DatagramPacket dgPacket=new DatagramPacket(data,data.length, ip, PORT);
        dgSocket.send(dgPacket);
        Message msg = timestamp.obtainMessage(SEND);
        msg.obj = "broadcast at:" + System.currentTimeMillis() + " to:" + ip.getHostAddress() +  " data="+new String(data) + "\n";
        msg.sendToTarget();
        dgSocket.close();
    }

    private class Listen extends AsyncTask<Void, Void, Void> {
        DatagramSocket dgSocket;

        public void stopListener(){
            try{
                if(dgSocket != null){
                    dgSocket.close();
                }
            }catch (Exception ne){}
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "begin doInBackground");
                InetAddress local = getLocalIp();
                dgSocket = new DatagramSocket(PORT);
                dgSocket.setReuseAddress(true);
                dgSocket.setSoTimeout(10 * 1000);
                byte[] by = new byte[1024];
                while (true && !isCancelled()){
                    DatagramPacket packet = new DatagramPacket(by, by.length);
                    try {
                        Log.d(TAG, "before receive");
                        dgSocket.receive(packet);
                    }catch (SocketTimeoutException e){
                        Log.d(TAG, "timeout");
                        if(this.isCancelled() == true) {
                            Log.d(TAG, "exit");
                            break;
                        }
                        continue;
                    }

                    byte[] data = packet.getData();
                    Log.d(TAG, "receive data="+new String(data));

                    boolean broadcast = isBroadcastHeader(data, packet.getLength(), packet);
                    boolean reply     = isReplyHeader(data, packet.getLength());
                    if(!broadcast && !reply) {
                        continue;
                    }

                    long receiveTime = System.currentTimeMillis();
                    InetAddress remote = packet.getAddress();

                    Message msg = timestamp.obtainMessage(RECEIVE);
                    msg.obj = "receive " + (broadcast?"broadcast":"reply") + " at:" + receiveTime + " from:" + remote.getHostAddress() + new String (data);
                    msg.sendToTarget();

                    if(remote != null && !remote.equals(local) && broadcast) {
                        executeCommand(remote, fromJson(new String(data, 0, packet.getLength())));
                    }
                }
                dgSocket.close();

                Log.d(TAG, "exit listen udp ");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    if(dgSocket != null) {
                        dgSocket.close();
                        dgSocket.disconnect();
                        dgSocket = null;
                    }
                }catch (Exception e){}
            }

            mListener = null;
            Log.d(TAG, "exit listen udp 2");
            return null;
        }
    };


    private class TCPListen extends AsyncTask<Void, Void, Void> {
        final static String TAG = "tcp";
        ServerSocket serverSocket;

        public void stopListener(){
            try{
                if(serverSocket != null){
                    serverSocket.close();
                }
            }catch (Exception ne){}
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "begin server socket doInBackground");
                serverSocket = new ServerSocket(SERVER_PORT, 10, getLocalIp());
                serverSocket.setReuseAddress(true);
                serverSocket.setSoTimeout(10 * 1000);
                while (true && isCancelled() == false) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();

                        Log.d(TAG, "receive client connection="+socket.getLocalAddress().getHostAddress());
                        clients.add(socket);
                        ClientThread ct = new ClientThread();
                        ct.peer = socket;
                        ct.start();

                    }catch (Exception e){}
                }

                serverSocket.close();
                Log.d(TAG, "exit server listen udp ");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if(serverSocket != null) {
                        serverSocket.close();
                        serverSocket = null;
                    }
                }catch (Exception e){}
            }

            mListener = null;
            Log.d(TAG, "exit server listen udp 2");
            return null;
        }
    };

    public class  ClientThread extends Thread{
        final static String TAG = "tcp socket thread";
        public Socket peer;
        byte []buffer = new byte[1024];
        public void run(){
            if(peer != null){
                try {
                    InputStream is = peer.getInputStream();
                    int len = is.read(buffer);

                    String bdata = new String(buffer, 0, len);
                    Log.d(TAG, "receive client data="+bdata);
                    final UDPComnnad data = fromJson(bdata);
                    //let app call command
                    try {
                        if (data.ns.equals("app") || data.ns.equals("game")) {
                            if (data.command.equals("install")) {
                                String id = data.id;
                                String param = data.params;
                                try {
                                    ApplicationInfo info = pm.getApplicationInfo(param, 0);
                                    if (info != null) {
                                        WLUIUtils.openGame(mContext, param, "from_remote_phone");
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    XMDownloadManager.getInstance().appendDownloadTask(id);
                                }
                            }
                        } else if (data.ns.equals("video")) {
                            Intent intent = new Intent();
                            intent.setAction(Constants.MITV_VIDEO_INTENT_ACTION);
                            intent.setData(Uri.parse(data.params));
                            try {
                                mContext.startActivity(intent);
                            } catch (Exception e) {
                                Intent videoIntent = new Intent(mContext, PlayerActivity.class);
                                videoIntent.putExtra("video_url", data.params);
                                videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(videoIntent);
                            }
                        } else if (data.ns.equals("service_notify")) {

                        }
                    }catch (Exception ne){}

                    data.feedback = "OK";
                    data.device   = Build.MANUFACTURER;
                    data.server_mode = (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
                    data.sendDate    = System.currentTimeMillis();

                    byte[] bdate = toJSON(data).getBytes();

                    peer.getOutputStream().write(bdate);

                    Message msg = timestamp.obtainMessage(SERVER_SEND);
                    msg.obj = "server send replay at:" + System.currentTimeMillis() + " to:" + peer.getInetAddress().getHostAddress();
                    msg.sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        peer.close();
                    }catch (Exception ne){}
                }

                clients.remove(peer);
            }
        }
    }

    private boolean isBroadcastHeader(byte[] a, int len, final DatagramPacket packet){
        if(a==null) return false;
        final UDPComnnad udp = fromJson(new String(a, 0, len));

        if(udp != null){
            if(udp.command.equals("startup")){
                if(udp.server_mode == false){
                    Log.d(TAG, "the command is from mobile device");
                }
                else {//from tv
                    serviceHost.put(packet.getAddress().getHostAddress(), udp);
                    return true;
                }
            }else {
                //command from mobile
                if (udp.feedback == null || udp.feedback.length() == 0) {
                    Log.d(TAG, "receive broadcast command =" + udp.command);
                    if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        return true;
                    }
                    //TODO
                    //no need care for mobile command
                } else {
                    if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        timestamp.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, String.format("%1$s电视在 %2$s 已经接受您的命令", udp.device, Utils.longToDate(udp.sendDate)), Toast.LENGTH_LONG).show();
                            }
                        });

                        Log.d(TAG, "电视已经接受您的命令, IP=" + packet.getAddress().getHostAddress());
                    }
                }
            }
        }

        return false;
    }

    private boolean isReplyHeader(byte[] a, int len){
        if(a==null) return false;
        UDPComnnad udp = fromJson(new String(a, 0, len));
        if(udp != null && udp.feedback != null && udp.feedback.equals("OK")){
            Log.d(TAG, "replay command="+udp.command );
            //
            //for TV
            if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && udp.server_mode == false) {
                Log.d(TAG, "Peer received msg ="+udp.command);

                timestamp.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, "Peer received msg", Toast.LENGTH_LONG).show();
                    }
                });

                return true;
            }else if(mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && udp.server_mode == true){

                timestamp.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, "命令已经接收", Toast.LENGTH_LONG).show();
                    }
                });
                Log.d(TAG, "命令已经接收");

                return true;
            }
        }
        return false;
    }


    private InetAddress getLocalIp() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                System.out.println(netInterface.getName());
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address && !ip.getHostAddress().startsWith("127.0.0.1") && !ip.getHostAddress().startsWith("10.0")) {
                        Log.d(TAG, "local IP = " + ip.getHostAddress());
                        return ip;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
