package com.smlj.controller;

import com.smlj.annotation.JwtIgnore;
import com.smlj.utils.JwtUtil;
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
@RequestMapping("/train/jwt")
@Tag(name = "CJwt", description = "jwt相关操作")
public class CJwt {
    public Triple<Date, Date, String> jwt = null;
    /*private final RedisTemplate<String, Object> redis;

    public CJwt(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }*/

    @JwtIgnore
    @GetMapping("/getToken")
    public String getToken(boolean recordInRedis, Map<String, Object> claims) {
        jwt = JwtUtil.getToken(claims, JwtUtil.EXPIRE);
        return jwt.getRight();
    }
}
