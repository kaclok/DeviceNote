package com.smlj.nfcpatrol.core.network.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.smlj.nfcpatrol.core.network.NetStatus;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.client.SocketOptionBuilder;
import lombok.Data;

@Data
public class SocketIOProvider {
    private static final String _TAG = "Socket.IO";

    private Socket _socket;
    private volatile NetStatus _state = NetStatus.DISCONNECTED;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String url;
    private String token;

    private SocketIOProvider() {
    }

    private static final class _InstanceHolder {
        private static final SocketIOProvider _instance = new SocketIOProvider();
    }

    public static SocketIOProvider Instance() {
        return _InstanceHolder._instance;
    }

    // ================= 初始化 & 连接 =================

    public SocketIOProvider init(String url, String token) {
        this.url = url;
        this.token = token;
        return this;
    }

    public void connect() {
        if (_state == NetStatus.CONNECTING) {
            Log.w(_TAG, "正在连接中，请稍后");
            return;
        }

        // 如果已连接，先断开再重连
        if (_state == NetStatus.CONNECTED) {
            Log.i(_TAG, "已连接，重新连接...");
            return;
        }

        try {
            IO.Options options = SocketOptionBuilder.builder()
                    .setReconnection(true)
                    .setReconnectionAttempts(Integer.MAX_VALUE)
                    .setReconnectionDelay(1000)
                    .setReconnectionDelayMax(10000)
                    .setTimeout(20000)
                    .build();

            // ===== 鉴权（常见）=====
            options.auth = new java.util.HashMap<>();
            options.auth.put("token", token);

            _socket = IO.socket(url, options);
            // 防止多次调用connect导致事件多次注册
            handlerListeners(_socket, false);
            handlerListeners(_socket, true);

            _state = NetStatus.CONNECTING;
            _socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void _onConnected(Object[] args) {
        Log.d(_TAG, "CONNECTED");

        _state = NetStatus.CONNECTED;
    }

    private void _onDisconnected(Object[] args) {
        Log.d(_TAG, "DISCONNECTED");

        _state = NetStatus.DISCONNECTED;
    }

    private void _onConnectError(Object[] args) {
        Log.d(_TAG, "CONNECT_ERROR");

        _state = NetStatus.DISCONNECTED;
    }

    private void _onReconnected(Object[] args) {
        Log.d(_TAG, "RECONNECTED");

        _state = NetStatus.RECONNECTED;
    }

    private void _onMessage(Object[] args) {
        String msg = args[0].toString();
        Log.d(_TAG, "RECEIVE: " + msg);
    }

    private void _onPong(Object[] args) {
        Log.d(_TAG, "PONG");
    }

    private void handlerListeners(Socket soc, boolean toRegister) {
        if (toRegister) {
            soc.on(Socket.EVENT_CONNECT, this::_onConnected);
            soc.on(Socket.EVENT_DISCONNECT, this::_onDisconnected);
            soc.on(Socket.EVENT_CONNECT_ERROR, this::_onConnectError);
            soc.on(Manager.EVENT_RECONNECT, this::_onReconnected);
            // ===== 业务消息 =====
            soc.on("message", this::_onMessage);
            // ===== 心跳（可选，Socket.IO 内建 ping/pong）=====
            soc.on("pong", this::_onPong);
        } else {
            soc.off(Socket.EVENT_CONNECT, this::_onConnected);
            soc.off(Socket.EVENT_DISCONNECT, this::_onDisconnected);
            soc.off(Socket.EVENT_CONNECT_ERROR, this::_onConnectError);
            soc.off(Manager.EVENT_RECONNECT, this::_onReconnected);
            soc.off("message", this::_onMessage);
            soc.off("pong", this::_onPong);
        }
    }

    // ================= 发送消息 =================

    public void sendMessage(String msg) {
        if (isConnected()) {
            _socket.emit("message", msg);
        }
    }

    // ================= 主动断开 =================

    public void disConnect() {
        if (_socket != null) {
            _socket.disconnect();
            _socket.off();
            _socket.close();
            _state = NetStatus.DISCONNECTED;
        }
        _socket = null;
    }

    // ================= 状态获取 =================

    public boolean isConnected() {
        return _state == NetStatus.CONNECTED;
    }
}
