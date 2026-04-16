package com.example.netassist;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.view.MotionEvent;

public class MainFragment extends Fragment implements TcpClient.TcpCallback, UdpClient.UdpCallback, TcpServer.TcpServerCallback {

    private Spinner spinnerProtocol;
    private EditText etHost;
    private EditText etPort;
    private Button btnConnect;
    private ImageButton btnIpSelector;
    private RadioButton rbAscii;
    private RadioButton rbHex;
    private CheckBox cbAutoScroll;
    private CheckBox cbShowLog;
    private Button btnClear;
    private TextView tvLog;
    private ScrollView svLog;
    private EditText etSend;
    private Button btnSend;
    private HorizontalScrollView hsvQuickCommands;
    private LinearLayout llQuickButtonsContainer;

    private TcpClient tcpClient;
    private UdpClient udpClient;
    private TcpServer tcpServer;
    private boolean isConnected = false;
    private StringBuilder logBuilder;
    private java.text.SimpleDateFormat dateFormat;
    private int currentProtocol = 0; // 0: TCP Client, 1: TCP Server, 2: UDP

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        initViews(view);
        initData();
        initListeners();
        return view;
    }

    private void initViews(View view) {
        spinnerProtocol = view.findViewById(R.id.spinner_protocol);
        etHost = view.findViewById(R.id.et_host);
        etPort = view.findViewById(R.id.et_port);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnIpSelector = view.findViewById(R.id.btn_ip_selector);
        rbAscii = view.findViewById(R.id.rb_ascii);
        rbHex = view.findViewById(R.id.rb_hex);
        cbAutoScroll = view.findViewById(R.id.cb_auto_scroll);
        cbShowLog = view.findViewById(R.id.cb_show_log);
        btnClear = view.findViewById(R.id.btn_clear);
        tvLog = view.findViewById(R.id.tv_log);
        svLog = view.findViewById(R.id.sv_log);
        etSend = view.findViewById(R.id.et_send);
        btnSend = view.findViewById(R.id.btn_send);
        hsvQuickCommands = view.findViewById(R.id.hsv_quick_commands);
        llQuickButtonsContainer = view.findViewById(R.id.ll_quick_buttons_container);

        tvLog.setMovementMethod(new ScrollingMovementMethod());
        
        // 禁止快捷指令区域的触摸事件传递到ViewPager2
        hsvQuickCommands.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        
        // 禁止日志区域的触摸事件传递到ViewPager2
        svLog.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private void initData() {
        logBuilder = new StringBuilder();
        dateFormat = new java.text.SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS]", java.util.Locale.getDefault());

        String[] protocols = {"TCP Client", "TCP Server", "UDP"};
        ArrayAdapter<String> protocolAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, protocols);
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(protocolAdapter);
        
        spinnerProtocol.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentProtocol = position;
                // 切换协议时断开连接
                if (isConnected) {
                    disconnect();
                }
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        tcpClient = new TcpClient(this);
        udpClient = new UdpClient(this);
        tcpServer = new TcpServer(this);
        TcpManager.getInstance().setTcpClient(tcpClient);
    }

    private void initListeners() {
        btnConnect.setOnClickListener(v -> {
            if (isConnected) {
                disconnect();
            } else {
                connect();
            }
        });

        btnClear.setOnClickListener(v -> {
            logBuilder.setLength(0);
            tvLog.setText("");
        });

        btnSend.setOnClickListener(v -> {
            String data = etSend.getText().toString().trim();
            if (!data.isEmpty()) {
                if (isConnected) {
                    sendData(data);
                    appendLog("SEND", data);
                    etSend.setText("");
                } else {
                    Toast.makeText(requireContext(), "未连接到服务器", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "请输入要发送的数据", Toast.LENGTH_SHORT).show();
            }
        });

        btnIpSelector.setOnClickListener(v -> showIpSelectorDialog());
    }

    private void showIpSelectorDialog() {
        List<String> ipList = new ArrayList<>();
        ipList.add("127.0.0.1");
        ipList.add("0.0.0.0");
        
        // 获取本机IP地址
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        ipList.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] ipArray = ipList.toArray(new String[0]);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("选择IP地址")
                .setItems(ipArray, (dialog, which) -> {
                    etHost.setText(ipArray[which]);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void connect() {
        String host = etHost.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();

        if (currentProtocol == 0 || currentProtocol == 2) {
            // TCP Client 或 UDP 需要输入服务器地址
            if (host.isEmpty()) {
                Toast.makeText(requireContext(), "请输入服务器地址", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (portStr.isEmpty()) {
            Toast.makeText(requireContext(), "请输入端口号", Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "端口号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConnect.setEnabled(false);
        btnConnect.setText("连接中...");
        
        switch (currentProtocol) {
            case 0: // TCP Client
                tcpClient.connect(host, port);
                break;
            case 1: // TCP Server
                tcpServer.start(port);
                break;
            case 2: // UDP
                udpClient.connect(host, port);
                break;
        }
    }

    private void disconnect() {
        switch (currentProtocol) {
            case 0: // TCP Client
                tcpClient.disconnect();
                break;
            case 1: // TCP Server
                tcpServer.stop();
                break;
            case 2: // UDP
                udpClient.disconnect();
                break;
        }
    }

    private void sendData(String data) {
        if (!isConnected) {
            Toast.makeText(requireContext(), "未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 如果是HEX格式，需要将字符串转换为字节数组发送
        if (rbHex.isChecked()) {
            try {
                byte[] bytes = hexStringToByteArray(data);
                switch (currentProtocol) {
                    case 0: // TCP Client
                        tcpClient.sendBytes(bytes);
                        break;
                    case 1: // TCP Server
                        tcpServer.sendBytesToAll(bytes);
                        break;
                    case 2: // UDP
                        udpClient.sendBytes(bytes);
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "HEX格式错误", Toast.LENGTH_SHORT).show();
            }
        } else {
            switch (currentProtocol) {
                case 0: // TCP Client
                    tcpClient.send(data);
                    break;
                case 1: // TCP Server
                    tcpServer.sendToAll(data);
                    break;
                case 2: // UDP
                    udpClient.send(data);
                    break;
            }
        }
    }

    private byte[] hexStringToByteArray(String s) {
        // 移除空格和常见分隔符
        s = s.replaceAll("\\s+", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void appendLog(String type, String data) {
        if (!cbShowLog.isChecked()) {
            return;
        }

        String timestamp = dateFormat.format(new java.util.Date());
        String format = rbAscii.isChecked() ? "ASCII" : "HEX";
        
        // 根据格式显示数据
        String displayData;
        if (rbHex.isChecked()) {
            // 转换为HEX格式显示
            displayData = bytesToHexString(data.getBytes());
        } else {
            displayData = data;
        }
        
        String logEntry = String.format("%s %s %s> %s\n", timestamp, type, format, displayData);

        logBuilder.append(logEntry);
        tvLog.setText(logBuilder.toString());

        if (cbAutoScroll.isChecked()) {
            svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    public void onConnected() {
        isConnected = true;
        TcpManager.getInstance().setConnected(true);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("断开");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.error));
                Toast.makeText(requireContext(), "连接成功", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onDisconnected() {
        isConnected = false;
        TcpManager.getInstance().setConnected(false);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("连接");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.success));
                Toast.makeText(requireContext(), "已断开连接", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onReceiveData(String data) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> appendLog("RECV", data));
        }
    }

    @Override
    public void onError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                btnConnect.setEnabled(true);
                btnConnect.setText("连接");
            });
        }
    }

    // UDP Client 回调
    @Override
    public void onUdpConnected() {
        isConnected = true;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("断开");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.error));
                Toast.makeText(requireContext(), "UDP已连接", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onUdpDisconnected() {
        isConnected = false;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("连接");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.success));
                Toast.makeText(requireContext(), "UDP已断开", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onUdpReceived(String data) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> appendLog("RECV", data));
        }
    }

    @Override
    public void onUdpError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                btnConnect.setEnabled(true);
                btnConnect.setText("连接");
            });
        }
    }

    // TCP Server 回调
    @Override
    public void onServerStarted() {
        isConnected = true;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("停止");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.error));
                Toast.makeText(requireContext(), "服务器已启动", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onServerStopped() {
        isConnected = false;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnConnect.setEnabled(true);
                btnConnect.setText("启动");
                btnConnect.setBackgroundColor(getResources().getColor(R.color.success));
                Toast.makeText(requireContext(), "服务器已停止", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onClientConnected(String clientInfo) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                appendLog("CONNECT", clientInfo);
                Toast.makeText(requireContext(), "客户端连接: " + clientInfo, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onClientDisconnected(String clientInfo) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                appendLog("DISCONNECT", clientInfo);
                Toast.makeText(requireContext(), "客户端断开: " + clientInfo, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onServerReceived(String data, String clientInfo) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> appendLog("RECV [" + clientInfo + "]", data));
        }
    }

    @Override
    public void onServerError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                btnConnect.setEnabled(true);
                btnConnect.setText("启动");
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateQuickCommandsSidebar();
    }

    private void updateQuickCommandsSidebar() {
        if (getActivity() != null) {
            List<QuickCommand> commands = ((MainActivity) getActivity()).getQuickCommands();
            
            llQuickButtonsContainer.removeAllViews();
            
            if (commands.isEmpty()) {
                hsvQuickCommands.setVisibility(View.GONE);
            } else {
                hsvQuickCommands.setVisibility(View.VISIBLE);
                
                for (QuickCommand command : commands) {
                    Button btn = new Button(requireContext());
                    btn.setText(command.getName());
                    btn.setTextSize(12);
                    btn.setTextColor(getResources().getColor(R.color.text_primary));
                    btn.setBackgroundResource(R.drawable.bg_quick_button);
                    
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(4, 0, 4, 0);
                    btn.setLayoutParams(params);
                    btn.setPadding(12, 6, 12, 6);
                    btn.setAllCaps(false);
                    
                    btn.setOnClickListener(v -> {
                        if (TcpManager.getInstance().isConnected()) {
                            TcpManager.getInstance().send(command.getData());
                            appendLog("SEND", command.getData());
                        } else {
                            Toast.makeText(requireContext(), "未连接到服务器", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    llQuickButtonsContainer.addView(btn);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tcpClient != null) {
            tcpClient.close();
        }
        if (udpClient != null) {
            udpClient.disconnect();
        }
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }
}
