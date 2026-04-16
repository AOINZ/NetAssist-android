package com.example.netassist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService executorService;
    private TcpServerCallback callback;
    private Thread acceptThread;
    private List<Socket> clientSockets;
    private List<ClientHandler> clientHandlers;

    public interface TcpServerCallback {
        void onServerStarted();
        void onServerStopped();
        void onClientConnected(String clientInfo);
        void onClientDisconnected(String clientInfo);
        void onServerReceived(String data, String clientInfo);
        void onServerError(String error);
    }

    private static class ClientHandler {
        Socket socket;
        String clientInfo;
        InputStream inputStream;
        OutputStream outputStream;
        
        ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
    }

    public TcpServer(TcpServerCallback callback) {
        this.callback = callback;
        this.executorService = Executors.newCachedThreadPool();
        this.clientSockets = new ArrayList<>();
        this.clientHandlers = new ArrayList<>();
    }

    public void start(final int port) {
        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                
                if (callback != null) {
                    callback.onServerStarted();
                }
                
                // 接受客户端连接
                acceptThread = new Thread(() -> {
                    while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            ClientHandler handler = new ClientHandler(clientSocket);
                            handler.inputStream = clientSocket.getInputStream();
                            handler.outputStream = clientSocket.getOutputStream();
                            
                            synchronized (clientHandlers) {
                                clientHandlers.add(handler);
                            }
                            
                            if (callback != null) {
                                callback.onClientConnected(handler.clientInfo);
                            }
                            
                            // 处理客户端数据
                            handleClient(handler);
                            
                        } catch (IOException e) {
                            if (isRunning) {
                                if (callback != null) {
                                    callback.onServerError("接受连接失败: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
                acceptThread.start();
                
            } catch (Exception e) {
                if (callback != null) {
                    callback.onServerError("启动服务器失败: " + e.getMessage());
                }
            }
        });
    }

    private void handleClient(ClientHandler handler) {
        new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRunning && handler.socket != null && !handler.socket.isClosed()) {
                try {
                    int len = handler.inputStream.read(buffer);
                    if (len > 0) {
                        String received = new String(buffer, 0, len, "UTF-8");
                        if (callback != null) {
                            callback.onServerReceived(received, handler.clientInfo);
                        }
                    } else if (len == -1) {
                        // 客户端断开连接
                        break;
                    }
                } catch (IOException e) {
                    break;
                }
            }
            
            // 客户端断开
            synchronized (clientHandlers) {
                clientHandlers.remove(handler);
            }
            
            try {
                if (handler.socket != null && !handler.socket.isClosed()) {
                    handler.socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            if (callback != null) {
                callback.onClientDisconnected(handler.clientInfo);
            }
        }).start();
    }

    public void sendToAll(final String data) {
        if (!isRunning) {
            if (callback != null) {
                callback.onServerError("服务器未运行");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                byte[] sendData = data.getBytes("UTF-8");
                synchronized (clientHandlers) {
                    for (ClientHandler handler : clientHandlers) {
                        try {
                            handler.outputStream.write(sendData);
                            handler.outputStream.flush();
                        } catch (IOException e) {
                            // 发送失败，客户端可能已断开
                        }
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onServerError("发送失败: " + e.getMessage());
                }
            }
        });
    }

    public void sendBytesToAll(final byte[] bytes) {
        if (!isRunning) {
            if (callback != null) {
                callback.onServerError("服务器未运行");
            }
            return;
        }
        
        executorService.execute(() -> {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    try {
                        handler.outputStream.write(bytes);
                        handler.outputStream.flush();
                    } catch (IOException e) {
                        // 发送失败
                    }
                }
            }
        });
    }

    public void stop() {
        isRunning = false;
        
        // 关闭所有客户端连接
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                try {
                    if (handler.socket != null && !handler.socket.isClosed()) {
                        handler.socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clientHandlers.clear();
        }
        
        // 关闭服务器Socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverSocket = null;
        
        if (acceptThread != null) {
            acceptThread.interrupt();
            acceptThread = null;
        }
        
        if (callback != null) {
            callback.onServerStopped();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getClientCount() {
        synchronized (clientHandlers) {
            return clientHandlers.size();
        }
    }
}
