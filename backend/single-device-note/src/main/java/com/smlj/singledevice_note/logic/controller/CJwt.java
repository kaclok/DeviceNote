package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/x")
@Tag(name = "CJwt", description = "jwt相关操作")
public class CJwt {
    /*
     * ⚠️ 这里原有 /x/getAccessToken 与 /x/getRefreshToken 两个方法，把请求体里的 Map 原样签名成 token 返回。
     * 两个问题叠加就是完整的认证绕过，且全仓库无任何调用方（前端只用 /x/refreshAccessToken），故删除：
     *   1. RegistryOf 只把 TokenInterceptor 挂在 /cghtz/**，/x/** 不经过任何认证
     *      —— excludePathPatterns 里列的 "/x/getAccessToken" 是失效配置，看着像管住了，其实没管；
     *   2. 签发内容由调用方决定：匿名 POST {"account":"<任意账号>"} 就能拿到该账号的合法 AT。
     * 将来若确实需要"服务端签发 token"，只能基于服务端已确认的身份（内网签名 / 服务间凭证），
     * 绝不能再让请求体决定签什么。
     */

    /**
     * AT 过期后凭 RT 无感刷新。
     * <p>
     * 刻意不复制 RT 的 payload：RT 里只有 account（见 {@link JwtUtil#ACCOUNT_CLAIM}），
     * 用户信息由 TokenInterceptor 每请求按 account 现查 t_user（CurUserService）。
     * 原实现把 RT 的 payload 原样重签成 AT，等于让"登录那一刻的用户快照"一路续命到 RT 过期
     * （最长 4h）—— admin 改了权限/数据范围后，用户必须重新登录才生效。
     */
    @PostMapping("/refreshAccessToken")
    public Result<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String rt = request.getHeader(JwtUtil.RT_HEADER);
        if (rt == null || rt.isBlank()) {
            // refresh token缺失，视为已过期，前端应跳转登录
            return Result.fail(ResultCode.RC10001);
        }

        var jwt = JwtUtil.getJWTByToken(rt);
        if (!JwtUtil.verifyOnly(jwt) || JwtUtil.isExpired(jwt)) {
            // refresh token签名无效或已过期，前端应跳转登录
            return Result.fail(ResultCode.RC10001);
        }

        // RT有效：只取出 account 重新签发 AT，其余字段一律丢弃（这正是"快照不再续命"的关键）
        var claims = JwtUtil.parseToken(jwt);
        Object account = claims == null ? null : claims.get(JwtUtil.ACCOUNT_CLAIM);
        if (account == null || !StringUtils.hasText(account.toString())) {
            // 改造前签发的旧 RT：payload 里存的是整个 user 对象、没有 account 字段。
            // 不能猜一个字段出来继续用，直接判失效，让用户重新登录换一张新 RT。
            return Result.fail(ResultCode.RC10001);
        }
        JwtUtil.setAccessTokenHeader(response, Map.<String, Object>of(JwtUtil.ACCOUNT_CLAIM, account.toString()));
        return Result.success();
    }
}
