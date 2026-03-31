package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.Detector.ExceptionDetailEncoder;
import com.jthx.x.cms.watchdog.pojo.ExceptionDetail;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(
        value = "/api/webSocket/{user_id}",
        configurator = SpringEndpointConfigurator.class,
        encoders = ExceptionDetailEncoder.class)
@Service
public class WebSocketPushService {
    private static Set<Session> sessions = new HashSet<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("user_id") String userId) {
        if (sessions.contains(session) && session.isOpen()) {
            return;
        }

        sessions.add(session);
        System.out.println(this.hashCode() + " websocket连接已建立:" + session.hashCode() + "  总个数：" + sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
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

    public static void sendMsgToAll(ExceptionDetail message) {
        for (Session session : sessions) {
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendObject(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
