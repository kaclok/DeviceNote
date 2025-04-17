package com.smlj.singledevice_note.core.net.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class WsHandler extends TextWebSocketHandler {
    // 客户端连接建立时调用，可发送欢迎消息
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WsSessionManager.add(session.getId(), session);

        var str = "新连接建立: " + session.getId();
        log.info(str);
        session.sendMessage(new TextMessage(str));
    }

    // 接收客户端发送的文本消息，并返回相同的内容
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 获得客户端传来的消息
        String payload = message.getPayload();
        var str = "server 接收到消息: " + payload;
        log.info(str);
        session.sendMessage(new TextMessage(str));
    }

    // https://blog.csdn.net/w1014074794/article/details/113845879
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("异常处理");
        WsSessionManager.removeAndClose(session.getId());
    }

    // 连接关闭时执行清理操作
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("关闭ws连接");
        WsSessionManager.removeAndClose(session.getId());
    }
}