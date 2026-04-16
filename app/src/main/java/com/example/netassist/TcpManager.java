package com.example.netassist;

public class TcpManager {
    private static TcpManager instance;
    private TcpClient tcpClient;
    private boolean isConnected = false;

    private TcpManager() {
    }

    public static TcpManager getInstance() {
        if (instance == null) {
            instance = new TcpManager();
        }
        return instance;
    }

    public void setTcpClient(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void send(String data) {
        if (tcpClient != null && isConnected) {
            tcpClient.send(data);
        }
    }
}
