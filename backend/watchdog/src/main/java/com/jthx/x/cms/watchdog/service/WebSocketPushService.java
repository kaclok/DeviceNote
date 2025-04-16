package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.Detector.ExceptionDetector;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = "/api/webSocket/{user_id}")
@Component
public class WebSocketPushService {

    private static Logger logger = LoggerFactory.getLogger(WebSocketPushService.class);

    private static Set<Session> sessions = new HashSet<>();

    private String userId;

    private static Boolean detecting = false;

    @OnOpen
    public void onOpen(Session session, @PathParam("user_id") String userId) {
        this.userId = userId;
        if (sessions.contains(session) && session.isOpen()) {
            return;
        }

        sessions.add(session);
        System.out.println("websocket连接已建立");
        if (!detecting) {
            detecting = true;
            startDetectException();
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
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

    private void startDetectException() {
        ExceptionDetector detector = new ExceptionDetector();
        detector.startMonitoring();
    }

}