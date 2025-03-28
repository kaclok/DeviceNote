package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.SseService;
import com.smlj.singledevice_note.core.o.to.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/sse")
public class CSse {
    private final SseService sseService;

    @GetMapping(path = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        return sseService.connect();
    }

    @GetMapping("/testPublish")
    public String testPublish(@RequestParam String message) {
        Result<String> result = Result.success(message);
        sseService.publish(result);
        return "推送触发成功: " + message;
    }
}
