package com.lepu.ecntest;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

public class WifiActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 有设备正在连接热点
     */
    public static final int DEVICE_CONNECTING = 1;
    /**
     * 有设备连上热点
     */
    public static final int DEVICE_CONNECTED = 2;
    /**
     * 发送消息成功
     */
    public static final int SEND_MSG_SUCCSEE = 3;
    /**
     * 发送消息失败
     */
    public static final int SEND_MSG_ERROR = 4;
    /**
     * 获取新消息
     */
    public static final int GET_MSG = 6;
    private TextView text_state;
    private WifiManager wifiManager;
    /**
     * 连接线程
     */
    private ConnectThread connectThread;
    /**
     * 监听线程
     */
    private ListenerThread listenerThread;
    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "ECN_TEST";
    /**
     * 端口号
     */
    private static final int PORT = 54321;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    connectThread = new ConnectThread(WifiActivity.this, listenerThread.getSocket(), handler);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    text_state.setText("设备连接成功");
                    break;
                case SEND_MSG_SUCCSEE:
                    text_state.setText("发送消息成功:" + msg.getData().getString("MSG"));
                    break;
                case SEND_MSG_ERROR:
                    text_state.setText("发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    text_state.setText("收到消息:" + msg.getData().getString("MSG"));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        findViewById(R.id.create_wifi).setOnClickListener(this);
        findViewById(R.id.close_wifi).setOnClickListener(this);
        findViewById(R.id.send).setOnClickListener(this);
        text_state = (TextView) findViewById(R.id.receive);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 先开启监听线程，在开启连接
        listenerThread = new ListenerThread(PORT, handler);
        listenerThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 开启连接线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("ip", "getWifiApIpAddress()" + getWifiApIpAddress());
                    // 本地路由开启通信
                    String ip = getWifiApIpAddress();
                    if (ip == null) {
                        ip = "192.168.43.1";
                    }
                    Socket socket = new Socket(ip, PORT);
                    connectThread = new ConnectThread(WifiActivity.this, socket, handler);
                    connectThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText("创建通信失败");
                        }
                    });

                }
            }
        }).start();


    }

    /**
     * 创建Wifi热点
     */
    private void createWifiHotspot() {
        if (wifiManager.isWifiEnabled()) {
            // 如果wifi处于打开状态，则关闭wifi,
            wifiManager.setWifiEnabled(false);
        }
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_HOTSPOT_SSID;
        config.preSharedKey = "123456789";
        config.hiddenSSID = false;
        // 开放系统认证
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        // 通过反射调用设置热点

        //192.168.43.59
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, config, true);
            if (enable) {
                text_state.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");

                // 开启连接线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("ip", "getWifiApIpAddress()" + getWifiApIpAddress());
                            String ip = getWifiApIpAddress();
                            if (ip == null) {
                                // 一般Android手机默认路由是
                                ip = "192.168.43.1";
                            }
                            // 本地路由开启通信
                            Socket socket = new Socket(ip, PORT);
                            connectThread = new ConnectThread(WifiActivity.this, socket, handler);
                            connectThread.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    text_state.setText("创建通信失败");
                                }
                            });

                        }
                    }
                }).start();
                Thread.sleep(1000);
            } else {
                text_state.setText("创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            text_state.setText("创建热点失败");
        }
    }

    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d("Main", inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("Main", ex.toString());
        }
        return null;
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManager, config, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        text_state.setText("热点已关闭");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.create_wifi:
                createWifiHotspot();
                break;
            case R.id.close_wifi:
                closeWifiHotspot();
                break;
            case R.id.send:
                connectThread.sendData("000000");
                break;
        }
    }
}
