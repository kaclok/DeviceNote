package com.smlj.singledevice_note.core.net.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
@Slf4j
public class WsService {
    public void sendMsg(WebSocketSession session, String text) throws IOException {
        session.sendMessage(new TextMessage(text));
    }

    public void broadcastMsg(String text) throws IOException {
        for (WebSocketSession session: WsSessionManager.wss.values()) {
            session.sendMessage(new TextMessage(text));
        }
    }
}
