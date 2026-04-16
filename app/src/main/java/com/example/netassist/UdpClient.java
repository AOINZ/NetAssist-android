package com.example.netassist;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean isConnected = false;
    private ExecutorService executorService;
    private UdpCallback callback;
    private Thread receiveThread;

    public interface UdpCallback {
        void onUdpConnected();
        void onUdpDisconnected();
        void onUdpReceived(String data);
        void onUdpError(String error);
    }

    public UdpClient(UdpCallback callback) {
        this.callback = callback;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void connect(final String host, final int port) {
        executorService.execute(() -> {
            try {
                serverAddress = InetAddress.getByName(host);
                serverPort = port;
                socket = new DatagramSocket();
                isConnected = true;
                
                // 启动接收线程
                startReceiving();
                
                if (callback != null) {
                    callback.onUdpConnected();
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onUdpError("连接失败: " + e.getMessage());
                }
            }
        });
    }

    private void startReceiving() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (callback != null) {
                        callback.onUdpReceived(received);
                    }
                } catch (IOException e) {
                    if (isConnected) {
                        if (callback != null) {
                            callback.onUdpError("接收数据失败: " + e.getMessage());
                        }
                    }
                    break;
                }
            }
        });
        receiveThread.start();
    }

    public void send(final String data) {
        if (!isConnected || socket == null) {
            if (callback != null) {
                callback.onUdpError("未连接到服务器");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                byte[] sendData = data.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(
                    sendData, 
                    sendData.length, 
                    serverAddress, 
                    serverPort
                );
                socket.send(packet);
            } catch (Exception e) {
                if (callback != null) {
                    callback.onUdpError("发送失败: " + e.getMessage());
                }
            }
        });
    }

    public void sendBytes(final byte[] bytes) {
        if (!isConnected || socket == null) {
            if (callback != null) {
                callback.onUdpError("未连接到服务器");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                DatagramPacket packet = new DatagramPacket(
                    bytes, 
                    bytes.length, 
                    serverAddress, 
                    serverPort
                );
                socket.send(packet);
            } catch (Exception e) {
                if (callback != null) {
                    callback.onUdpError("发送失败: " + e.getMessage());
                }
            }
        });
    }

    public void disconnect() {
        isConnected = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = null;
        
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        
        if (callback != null) {
            callback.onUdpDisconnected();
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
