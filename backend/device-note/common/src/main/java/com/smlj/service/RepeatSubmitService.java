package com.smlj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Spring Boot结合Redis实现接口防止重复提交
// https://mp.weixin.qq.com/s/Q40PjpJgtBJjnhmmteMxFg
@Service
@RequiredArgsConstructor
public class RepeatSubmitService {
    /*private final StringRedisTemplate redisTemplate;

    private static final String REPEAT_SUBMIT_KEY_PREFIX = "repeat_submit:";

    *//**
     * 设置防重提交标识
     * @param userId 用户ID
     * @param requestId 请求ID
     * @param expireTime 过期时间（秒）
     *//*
    public void setRepeatSubmitKey(String userId, String requestId, long expireTime) {
        String key = REPEAT_SUBMIT_KEY_PREFIX + userId + ":" + requestId;
        redisTemplate.opsForValue().set(key, "1", expireTime, TimeUnit.SECONDS);
    }

    *//**
     * 检查是否存在防重提交标识
     * @param userId 用户ID
     * @param requestId 请求ID
     * @return 存在返回true，不存在返回false
     *//*
    public boolean hasRepeatSubmitKey(String userId, String requestId) {
        String key = REPEAT_SUBMIT_KEY_PREFIX + userId + ":" + requestId;
        return redisTemplate.hasKey(key);
    }*/
}
