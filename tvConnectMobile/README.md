#怎么在统一个局域网连接android设备和你家里的智能电视？

##背景
做了一个视频类的app， 可以同时在手机和电视端同时run， 想在手机端点击播放时，在电视直接播放出来。
<a href="https://github.com/AiAndroid/stream/raw/master/tv/game/androidTV.apk">apk 地址</a>。

##方案 1
用UDP 实现连接发现和通讯。每个设备都发送广播，命令在广播中传送， 
缺点：电视在播放流媒体时， 广播有时候收不着。

##方案 2
用UDP + TCP 实现连接发现和通讯

TV 端发送广播， 同一个局域网的设备都会收到这个广播消息。在消息中声明自己是TV。
收到广播的手机， 会得到提供服务的IP地址。

TV端同时监听一个TCP端口， 
手机端，收到电视的广播，记录IP地址，发送命令时， 直接建立Socket。


###AndroidManifest.xml添加：
<receiver android:name=".store.WIFIConnectivityMoniter" >
            <intent-filter>
                <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
                <action android:name="android.net.wifi.WIFI_STATE_CHANGED" />
                <action android:name="android.net.wifi.STATE_CHANGE" />
                <action android:name="android.intent.action.SCREEN_ON"/>
                <action android:name="android.intent.action.SCREEN_OFF"/>
            </intent-filter>
        </receiver>
        
###移动端调用命令：
UDPCommandProtocol.UDPComnnad uc = new UDPCommandProtocol.UDPComnnad();
        uc.command = "play";
        uc.ns      = "video";
        uc.id      = item.id;
        uc.params  = videoUrl;
        UDPCommandProtocol.getInstance(getBaseContext()).broadcast(uc);
                    


###WIFIConnectivityMoniter.java
解决WIFI状态的监控问题

###DeviceFinderService.java， DeviceFinderManager.java
给电视服务端使用，它需要不断的发布自己的能力，告诉连接进局域网的移动设备， 
粗暴的设计为每10秒广播一次。用Alarm实现。


优化空间，重用连接的Socket。
代码在一天之内写的，有点乱，谅解

##献给 爱码士 = 热爱编写代码的男/女士
欢迎联系我们 <a href="mailto:liuhuadong7804@gmail.com">发送邮件</a>
</br>

微信订阅号：爱码士
</br>

记住我们的LOGO, A M S
