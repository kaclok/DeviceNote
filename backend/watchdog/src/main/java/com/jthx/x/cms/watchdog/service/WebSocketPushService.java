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

    private static volatile boolean isDetecting = false;

    @Autowired
    private ExceptionDetector detector; // 由Spring注入

    @OnOpen
    public void onOpen(Session session, @PathParam("user_id") String userId) {
        if (sessions.contains(session) && session.isOpen()) {
            return;
        }

        sessions.add(session);
        System.out.println(this.hashCode() + " websocket连接已建立:" + session.hashCode() + "  总个数：" + sessions.size() + " isDetecting:" + isDetecting);
        /*if (sessions.isEmpty()) {
            detector.startMonitoring();
        }*/

        synchronized (WebSocketPushService.class) {
            if (!isDetecting) {
                isDetecting = true;
                detector.startMonitoring();
            }
        }

        // 发送之前的过期的异常消息
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);

        /*if (sessions.isEmpty()) {
            detector.stopMonitoring();
        }*/

        System.out.println("websocket连接已关闭:" + session.hashCode());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        try {
            System.out.println(message);
            session.getBasicRemote().sendText("接收来自前端的消息");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendExceptionMessageToAll(String message) {
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
