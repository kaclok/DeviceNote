package com.smlj.singledevice_note.core;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

@Service
public class SseService {
    // 保存所有连接的客户端
    private final List<SseEmitter> emitters = new ArrayList<>();
    private static final long timeout = 60_000L;

    public SseEmitter connect() {
        SseEmitter emitter = new SseEmitter(timeout); // 超时时间60秒
        emitters.add(emitter);
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
        });
        return emitter;
    }

    public boolean close(SseEmitter emitter) {
        return emitters.remove(emitter);
    }

    public <T> void publish(T msg) {
        List<SseEmitter> failedEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .data(msg)
                        .id(String.valueOf(System.currentTimeMillis())));
            } catch (Exception e) {
                failedEmitters.add(emitter);
            }
        });
        emitters.removeAll(failedEmitters);
    }
}
