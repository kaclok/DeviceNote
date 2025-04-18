package com.smlj.singledevice_note.core.net.ws;

import com.smlj.singledevice_note.core.utils.UrlUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

// 握手拦截器
public class WsInterceptor extends HttpSessionHandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 从请求URI中获取查询参数
        String query = request.getURI().getQuery(); // "userId=029567"
        var map = UrlUtil.getQueryMap(query); // 直接提取值

        // 将数据存入WebSocket会话的attributes
        attributes.putAll(map);
        return true;
    }
}
