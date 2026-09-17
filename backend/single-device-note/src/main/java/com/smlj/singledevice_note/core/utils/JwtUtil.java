package com.smlj.singledevice_note.core.utils;

import cn.hutool.core.convert.NumberWithFormat;
import cn.hutool.jwt.JWT;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    public static final String AT_HEADER = "at";
    public static final String AT_ISSUE_HEADER = "at_issue_at";
    public static final String AT_EXPIRE_HEADER = "at_expire_at";

    public static final String RT_HEADER = "rt";
    public static final String RT_ISSUE_HEADER = "rt_issue_at";
    public static final String RT_EXPIRE_HEADER = "rt_expire_at";

    public static final String KEY = "smlj";
    public static final String AUTHORIZE = "Authorize";
    public static final byte[] KEY_BYTES = KEY.getBytes();

    /**
     * JWT payload 里唯一的业务字段：登录账号。
     * <p>
     * 刻意只放身份标识 —— username / role / perms / data_scope / open_status 全部由
     * TokenInterceptor 按 account 现查 t_user（见 CurUserService）：
     * admin 改了某人的姓名、角色、权限或数据范围后，这些字段若跟着 JWT 走，就得等用户重登才生效；
     * 而 CJwt.refreshAccessToken 是"复制 RT payload 重签 AT"，旧快照甚至能被续命到 RT 过期（最长 4h）。
     * <p>
     * 另一个副作用是安全：hutool 的 JWT.addPayloads 走自家 BeanSerializer，**不认 Jackson 的 @JsonIgnore**
     * （已用 5.8.16 实测），原来 claims.put("user", user) 会把明文 pwd 一起写进 payload（base64 可解）。
     * 只放 account 之后这条泄露路径自然消失。
     */
    public static final String ACCOUNT_CLAIM = "account";
    // 动态延长token过期时间
    // https://mp.weixin.qq.com/s/juSk00SEKhYKb2IkG1NhSQ
    // https://mp.weixin.qq.com/s/fnmGRvE8JFPR5ZG6RfWbIg
    // https://mp.weixin.qq.com/s/bQnoeS1ZwROoaPGIR8oCKg
    public static final long RRFRESH_EXPIRE = 4 * 3600 * 1000;
    public static final long ACCESS_EXPIRE = 1 * 120 * 1000;

    //接收业务数据,生成token并返回
    public static Triple<Date, Date, String> getToken(Map<String, Object> claims, long expire) {
        var issuedAt = new Date();
        var expireAt = new Date(issuedAt.getTime() + expire);
        String token = JWT.create().addHeaders(null).addPayloads(claims).setKey(KEY_BYTES).setIssuedAt(issuedAt).setExpiresAt(expireAt).sign();
        return Triple.of(issuedAt, expireAt, token);
    }

    public static JWT getJWTByToken(String token) {
        JWT jwt = null;
        try {
            jwt = cn.hutool.jwt.JWTUtil.parseToken(token);
        } catch (Exception ignored) {
        }
        return jwt;
    }

    // 接收token,验证token,并返回业务数据
    public static Map<String, Object> parseToken(String token) {
        var jwt = getJWTByToken(token);
        return parseToken(jwt);
    }

    public static Map<String, Object> parseToken(JWT jwt) {
        if (jwt != null) {
            return jwt.getPayload().getClaimsJson();
        }
        return null;
    }

    public static boolean verify(String token) {
        var jwt = getJWTByToken(token);
        return verifyOnly(jwt) && !isExpired(jwt);
    }

    public static boolean verifyOnly(JWT jwt) {
        if (jwt != null) {
            return jwt.setKey(KEY_BYTES).verify();
        }
        return false;
    }

    public static boolean isExpired(String token) {
        var jwt = getJWTByToken(token);
        return isExpired(jwt);
    }

    public static boolean isExpired(JWT jwt) {
        var exp = (NumberWithFormat) jwt.getPayload("exp");
        Date expireAt = new Date();
        expireAt.setTime(exp.intValue() * 1000L);
        return expireAt.before(new Date());
    }

    public static void setResponseHeader(HttpServletResponse response, Map<String, Object> claims) {
        setAccessTokenHeader(response, claims);
        setRefreshTokenHeader(response, claims);
    }

    public static void setAccessTokenHeader(HttpServletResponse response, Map<String, Object> claims) {
        var at = JwtUtil.getToken(claims, JwtUtil.ACCESS_EXPIRE);
        // 将 Token 放入响应头（不改变返回体结构）
        setAccessTokenHeader(response, at);
    }

    public static void setRefreshTokenHeader(HttpServletResponse response, Map<String, Object> claims) {
        var rt = JwtUtil.getToken(claims, JwtUtil.RRFRESH_EXPIRE);
        // 将 Token 放入响应头（不改变返回体结构）
        setRefreshTokenHeader(response, rt);
    }

    public static void setAccessTokenHeader(HttpServletResponse response, Triple<Date, Date, String> at) {
        if (at == null) {
            return;
        }
        response.setHeader(JwtUtil.AT_HEADER, at.getRight());
        response.setHeader(JwtUtil.AT_ISSUE_HEADER, String.valueOf(at.getLeft().getTime()));
        response.setHeader(JwtUtil.AT_EXPIRE_HEADER, String.valueOf(at.getMiddle().getTime()));
    }

    public static void setRefreshTokenHeader(HttpServletResponse response, Triple<Date, Date, String> rt) {
        if (rt == null) {
            return;
        }
        response.setHeader(JwtUtil.RT_HEADER, rt.getRight());
        response.setHeader(JwtUtil.RT_ISSUE_HEADER, String.valueOf(rt.getLeft().getTime()));
        response.setHeader(JwtUtil.RT_EXPIRE_HEADER, String.valueOf(rt.getMiddle().getTime()));
    }

    public static void setAccessTokenHeader(ServerHttpResponse response, Triple<Date, Date, String> at) {
        if (at == null) {
            return;
        }
        response.getHeaders().set(JwtUtil.AT_HEADER, at.getRight());
        response.getHeaders().set(JwtUtil.AT_ISSUE_HEADER, String.valueOf(at.getLeft().getTime()));
        response.getHeaders().set(JwtUtil.AT_EXPIRE_HEADER, String.valueOf(at.getMiddle().getTime()));
    }

    public static void setRefreshTokenHeader(ServerHttpResponse response, Triple<Date, Date, String> rt) {
        if (rt == null) {
            return;
        }
        response.getHeaders().set(JwtUtil.RT_HEADER, rt.getRight());
        response.getHeaders().set(JwtUtil.RT_ISSUE_HEADER, String.valueOf(rt.getLeft().getTime()));
        response.getHeaders().set(JwtUtil.RT_EXPIRE_HEADER, String.valueOf(rt.getMiddle().getTime()));
    }
}
