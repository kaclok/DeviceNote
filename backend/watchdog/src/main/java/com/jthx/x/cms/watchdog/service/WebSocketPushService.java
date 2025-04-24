package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.Detector.ExceptionDetector;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = "/api/webSocket/{user_id}", configurator = SpringEndpointConfigurator.class)
@Service
public class WebSocketPushService {
    private static Set<Session> sessions = new HashSet<>();

    @Autowired
    private ExceptionDetector detector; // 由Spring注入

    @OnOpen
    public void onOpen(Session session, @PathParam("user_id") String userId) {
        if (sessions.contains(session) && session.isOpen()) {
            return;
        }

        System.out.println("websocket连接已建立");
        if (sessions.isEmpty()) {
            if (detector != null) {
                detector.startMonitoring();
            }
        }
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);

        if (sessions.isEmpty()) {
            if (detector != null) {
                detector.stopMonitoring();
            }
        }

        System.out.println("websocket连接以关闭");
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            System.out.println(message);
            session.getBasicRemote().sendText("以下异常工况请及时处理");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendExceptionMessage(String message) {
        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}