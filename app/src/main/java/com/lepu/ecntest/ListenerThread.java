package com.lepu.ecntest;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 监听线程
 * @author chenyongfeng
 */
public class ListenerThread extends Thread {

    private String TAG = "ListenerThread";
    private ServerSocket serverSocket = null;
    private Handler handler;
    private Socket socket;

    public ListenerThread(int port, Handler handler) {
        setName("ListenerThread");
        this.handler = handler;
        try {
            // 监听本机的12345端口
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                Log.d(TAG, "阻塞");
                // 阻塞，等待设备连接
                if (serverSocket != null) {
                    socket = serverSocket.accept();
                }
                Message message = Message.obtain();
                message.what = WifiActivity.DEVICE_CONNECTING;
                handler.sendMessage(message);
            } catch (Exception e) {
                Log.d(TAG, "error:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }
}

