package com.example.netassist;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP客户端类
 * 负责与服务器的连接、数据发送和接收
 */
public class TcpClient {
    private static final String TAG = "TcpClient";
    private static final int CONNECT_TIMEOUT = 5000; // 连接超时5秒
    private static final int READ_BUFFER_SIZE = 1024; // 读取缓冲区大小

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ExecutorService executorService;
    private Handler mainHandler;
    private boolean isConnected = false;
    private boolean isReceiving = false;

    // 回调接口
    public interface TcpCallback {
        void onConnected();
        void onDisconnected();
        void onReceiveData(String data);
        void onError(String error);
    }

    private TcpCallback callback;

    public TcpClient(TcpCallback callback) {
        this.callback = callback;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 连接到服务器
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public void connect(final String host, final int port) {
        executorService.execute(() -> {
            try {
                // 创建Socket并设置超时
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
                socket.setSoTimeout(0); // 设置读取超时为无限（阻塞模式）

                // 获取输入输出流
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                isConnected = true;
                isReceiving = true;

                // 通知主线程连接成功
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onConnected();
                    }
                });

                // 开始接收数据
                startReceiving();

            } catch (SocketTimeoutException e) {
                notifyError("连接超时");
            } catch (IOException e) {
                notifyError("连接失败: " + e.getMessage());
            } catch (Exception e) {
                notifyError("连接异常: " + e.getMessage());
            }
        });
    }

    /**
     * 开始接收数据
     */
    private void startReceiving() {
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int bytesRead;

        while (isReceiving && isConnected && socket != null && !socket.isClosed()) {
            try {
                bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    // 将接收到的数据转换为字符串
                    String receivedData = new String(buffer, 0, bytesRead);

                    // 通知主线程接收到数据
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onReceiveData(receivedData);
                        }
                    });
                } else if (bytesRead == -1) {
                    // 服务器关闭了连接
                    disconnect();
                    break;
                }
            } catch (IOException e) {
                if (isReceiving) {
                    Log.e(TAG, "接收数据异常: " + e.getMessage());
                    disconnect();
                }
                break;
            }
        }
    }

    /**
     * 发送数据
     * @param data 要发送的数据
     */
    public void send(final String data) {
        if (!isConnected || outputStream == null) {
            notifyError("未连接到服务器");
            return;
        }

        executorService.execute(() -> {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
                Log.d(TAG, "发送数据: " + data);
            } catch (IOException e) {
                Log.e(TAG, "发送数据失败: " + e.getMessage());
                notifyError("发送数据失败: " + e.getMessage());
                disconnect();
            }
        });
    }

    /**
     * 发送字节数组
     * @param bytes 要发送的字节数组
     */
    public void sendBytes(final byte[] bytes) {
        if (!isConnected || outputStream == null) {
            notifyError("未连接到服务器");
            return;
        }

        executorService.execute(() -> {
            try {
                outputStream.write(bytes);
                outputStream.flush();
                Log.d(TAG, "发送字节数据: " + bytes.length + " bytes");
            } catch (IOException e) {
                Log.e(TAG, "发送数据失败: " + e.getMessage());
                notifyError("发送数据失败: " + e.getMessage());
                disconnect();
            }
        });
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isReceiving = false;
        isConnected = false;

        executorService.execute(() -> {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    socket = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭连接异常: " + e.getMessage());
            }

            // 通知主线程断开连接
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onDisconnected();
                }
            });
        });
    }

    /**
     * 通知错误
     * @param error 错误信息
     */
    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
        });
    }

    /**
     * 是否已连接
     * @return 连接状态
     */
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    /**
     * 关闭客户端并释放资源
     */
    public void close() {
        disconnect();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
