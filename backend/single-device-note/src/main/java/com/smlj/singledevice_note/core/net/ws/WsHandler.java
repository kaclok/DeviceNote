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
    public void afterConnectionEstablished(WebSocketSession session) {
        // 从请求URI中获取查询参数
        // var account = (String) session.getAttributes().get("account");
        WsService.add(session.getId(), session);

        var str = "新连接建立: " + session.getId();
        log.info(str);
        /*session.sendMessage(new TextMessage(str));*/
    }

    // 接收客户端发送的文本消息，并返回相同的内容
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 获得客户端传来的消息
        String payload = message.getPayload();
        var str = "server 接收到消息: " + payload;
        log.info(str);
        /*session.sendMessage(new TextMessage(str));*/
    }

    // https://blog.csdn.net/w1014074794/article/details/113845879
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("异常处理");
        WsService.removeAndClose(session.getId());
    }

    // 连接关闭时执行清理操作
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("关闭ws连接");
        WsService.removeAndClose(session.getId());
    }
}