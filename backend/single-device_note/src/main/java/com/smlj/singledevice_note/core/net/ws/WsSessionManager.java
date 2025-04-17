package com.smlj.singledevice_note.core.net.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class WsSessionManager {
    public static ConcurrentHashMap<String, WebSocketSession> wss = new ConcurrentHashMap<String, WebSocketSession>();

    public static void add(String key, WebSocketSession session) {
        if (key == null || session == null) {
            return;
        }
        wss.put(key, session);
    }

    public static WebSocketSession remove(String key) {
        return wss.remove(key);
    }

    public static void removeAndClose(String key) {
        WebSocketSession session = remove(key);
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                // todo: 关闭出现异常处理
                e.printStackTrace();
            }
        }
    }

    public static WebSocketSession get(String key) {
        return wss.get(key);
    }

    public static void clear() throws IOException {
        for (WebSocketSession session : wss.values()) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        wss.clear();
    }
}
