package com.lepu.ecntest;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DecimalFormat;

/**
 * 连接线程
 * @author chenyongfeng
 */
public class ConnectThread extends Thread {

    private String TAG = "ConnectThread";
    private final Socket socket;
    private Handler handler;
    private InputStream inputStream;
    private OutputStream outputStream;
    Context context;

    public ConnectThread(Context context, Socket socket, Handler handler) {
        setName("ConnectThread");
        Log.d(TAG, "ConnectThread");
        this.socket = socket;
        this.handler = handler;
        this.context = context;
    }

    @Override
    public void run() {
        if (socket == null) {
            return;
        }
        handler.sendEmptyMessage(WifiActivity.DEVICE_CONNECTED);
        try {
            // 获取数据流
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                // 读取数据
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    final byte[] data = new byte[bytes];
                    System.arraycopy(buffer, 0, data, 0, bytes);

                    Message message = Message.obtain();
                    message.what = WifiActivity.GET_MSG;
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG", new String(data));
                    message.setData(bundle);
                    handler.sendMessage(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 格式化文件大小
     *
     * @param length
     * @return
     */
    private String getFormatFileSize(long length) {
        DecimalFormat df = new DecimalFormat("#0.0");
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
        } catch (Exception e) {

            return false;
        }
        return true;
    }

    /**
     * 发送数据
     */
    public void sendData(String msg) {
        Log.d(TAG, "发送数据:" + (outputStream == null));
        if (outputStream != null) {
            try {
                outputStream.write(msg.getBytes());
                Log.d(TAG, "发送消息：" + msg);
                Message message = Message.obtain();
                message.what = WifiActivity.SEND_MSG_SUCCSEE;
                Bundle bundle = new Bundle();
                bundle.putString("MSG", msg);
                message.setData(bundle);
                handler.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = WifiActivity.SEND_MSG_ERROR;
                Bundle bundle = new Bundle();
                bundle.putString("MSG", msg);
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }

}


