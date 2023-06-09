package com.lepu.ecntest;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class WifiApManager {
    private static final String TAG = "WifiApManager";
    private static final String AP_CONNECTED = "0x2";
    private static final String AP_DEVICE_TYPE = "wlan0";
    public static final int WIFI_NONE_TYPE = 0;
    public static final int WIFI_WEP_TYPE = 1;
    public static final int WIFI_WPA_TYPE = 2;
    public static final int WIFI_PSK_TYPE = 3;


    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfiguration;

    public WifiApManager(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        Log.d(TAG, "WifiApManager create");
    }

    /**
     * 创建热点
     *
     * @param ssid   热点名称
     * @param passwd 热点密码
     * @param type   热点类型
     */
    public void startWifiAp(String ssid, String passwd, int type) {

        Log.d(TAG, "startWifiAp ssid == " + ssid + ", passwd == " + passwd + ", type == " + type);
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            Log.d(TAG, "startWifiAp mWifiManager.isWifiEnabled() == " + mWifiManager.isWifiEnabled());
        }

        try {
            WifiConfiguration netConfig = new WifiConfiguration();

            netConfig.SSID = ssid;
            netConfig.preSharedKey = passwd;
            netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

            switch (type) {
                case WIFI_NONE_TYPE:
                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    netConfig.preSharedKey = null;
                    break;
                case WIFI_WEP_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_WPA_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_PSK_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                default:
                    break;
            }
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(mWifiManager, netConfig, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查是否开启Wifi热点
     *
     * @return
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            Log.d(TAG, "isWifiApEnabled method.invoke(mWifiManager) == " + method.invoke(mWifiManager));
            return (boolean) method.invoke(mWifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "isWifiApEnabled false");
        return false;
    }

    /**
     * 关闭热点
     */
    public void closeWifiAp() {
        if (isWifiApEnabled()) {
            Log.d(TAG, "closeWifiAp");
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(mWifiManager, config, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打开热点
     */
    public void openWifiAp() {

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }

        if (!isWifiApEnabled()) {
            Log.d(TAG, "openWifiAp");
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(mWifiManager, config, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public WifiConfiguration getWifiApInfo() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            mWifiConfiguration = (WifiConfiguration) method.invoke(mWifiManager);
            Log.d(TAG,"getWifiApInfo mWifiConfiguration.SSID == " + mWifiConfiguration.SSID + ", mWifiConfiguration.preSharedKey == " + mWifiConfiguration.preSharedKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mWifiConfiguration;
    }


    /**
     * 开热点手机获得其他连接手机IP的方法
     *
     * @return 其他手机IP 数组列表
     */
    public ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIp = new ArrayList();
        BufferedReader br = null;
        FileReader fileReader = null;
        boolean flags = true;
        try {
            String line;
            fileReader = new FileReader("/proc/net/arp");
            br = new BufferedReader(fileReader);

            mWifiManager.getDhcpInfo().toString();

            while ((line = br.readLine()) != null) {
                Log.d(TAG, "getConnectedIP line == " + line);
                if (!flags) {
                    final String[] splitted = line.split(" + ");
                    Log.d(TAG, "getConnectedIP splitted.length == " + splitted.length);
                    for (int i=0; i<splitted.length; i++) {
                        Log.d(TAG, "getConnectedIP splitted["+ i +"] == " + splitted[i]);
                    }
                    if (splitted.length >= 6) {
                        if (splitted[2].equals(AP_CONNECTED) && splitted[5].equals(AP_DEVICE_TYPE)) {
                            connectedIp.add(splitted[0] + " " + splitted[3]);
                        }
                    }
                }
                flags = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileReader) {
                    fileReader.close();
                }

                if (null != br) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return connectedIp;
    }

    public String getClientDeviceName() {
        BufferedReader br = null;
        FileReader fileReader = null;

        try {
            fileReader = new FileReader("/data/misc/dhcp/dnsmasq.leases");
            br = new BufferedReader(fileReader);
            String line = "";
            while ((line = br.readLine()) != null) {
                Log.d(TAG,"getClientDeviceName line == " + line);
                if (line.indexOf("") != 1) {
                    String[] fields = line.split(" ");
                    //校验数据是不是破损
                    if (fields.length > 4) {
                        //返回第4个栏位

                        return fields[3];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileReader) {
                    fileReader.close();
                }

                if (null != br) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

}