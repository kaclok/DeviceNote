package com.smlj.common.service;

import com.smlj.common.o.to.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// https://juejin.cn/post/7221125237500330039
// 注意：userId != clientId,因为一个user可能在两个client同时登录
@Slf4j
public class SseService {
    private static final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public static SseEmitter connect(String clientId) {
        try {
            if (sseEmitters.containsKey(clientId)) {
                return sseEmitters.get(clientId);
            }

            // 设置超时时间，0表示不过期。默认30秒
            SseEmitter sseEmitter = new SseEmitter(30L);
            // 注册回调
            sseEmitter.onTimeout(() -> {
                log.error("clientId：[{}]连接超时", clientId);
                sseEmitters.remove(clientId);
            });
            sseEmitter.onCompletion(() -> {
                log.error("clientId：[{}]连接关闭", clientId);
                sseEmitters.remove(clientId);
            });
            sseEmitter.onError((throwable) -> {
                log.error("连接异常，正准备关闭，clientId：{}", clientId);
                sseEmitters.remove(clientId);
            });


            sseEmitters.put(clientId, sseEmitter);
            return sseEmitter;
        } catch (Exception e) {
            log.error("创建新的sse连接异常，当前用户：{}", clientId);
        }
        return null;
    }

    public static <T> void send(String clientId, Result<T> message) {
        if (sseEmitters.containsKey(clientId)) {
            try {
                sseEmitters.get(clientId).send(message);
            } catch (IOException e) {
                log.error("clientId: [{}]推送异常:{}", clientId, e.getMessage());
                sseEmitters.remove(clientId);
            }
        }
    }

    public static <T> void send(String clientId, T message) {
        if (sseEmitters.containsKey(clientId)) {
            try {
                sseEmitters.get(clientId).send(message);
            } catch (IOException e) {
                log.error("clientId: [{}]推送异常:{}", clientId, e.getMessage());
                sseEmitters.remove(clientId);
            }
        }
    }

    public static void close(String clientId) {
        if (sseEmitters.containsKey(clientId)) {
            sseEmitters.get(clientId).complete();
        }
    }
}
