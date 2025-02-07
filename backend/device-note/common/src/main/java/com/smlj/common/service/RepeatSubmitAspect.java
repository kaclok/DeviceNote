package com.smlj.common.service;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// Spring Boot结合Redis实现接口防止重复提交
// https://mp.weixin.qq.com/s/Q40PjpJgtBJjnhmmteMxFg
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {
    /*private final RepeatSubmitService redisService;

    @Before("@annotation(RepeatSubmit)")
    public void beforeMethod(JoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        // 获取当前用户ID和请求ID（这里假设通过ThreadLocal获取，实际项目中可能通过Session、JWT等方式获取）
        String userId = null;
        var user = CurUserService.get();
        if (user != null) {
            userId = user.getUid();
        }
        String requestId = UUID.randomUUID().toString(); // 使用UUID作为请求ID

        // 检查Redis中是否存在防重提交标识
        if (redisService.hasRepeatSubmitKey(userId, requestId)) {
            throw new RepeatSubmitException(userId, requestId);
        }

        // 设置防重提交标识（设置过期时间，如60秒）
        redisService.setRepeatSubmitKey(userId, requestId, repeatSubmit.expireTime());
    }*/
}
