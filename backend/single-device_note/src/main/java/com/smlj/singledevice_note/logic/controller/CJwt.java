package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/x")
@Tag(name = "CJwt", description = "jwt相关操作")
public class CJwt {
    @JwtIgnore
    @GetMapping("/getToken")
    public Triple<Date, Date, String> getToken(boolean recordInRedis, Map<String, Object> claims) {
        return JwtUtil.getToken(claims, JwtUtil.EXPIRE);
    }
}
