package com.smlj.singledevice_note.core.utils;

import cn.hutool.core.convert.NumberWithFormat;
import cn.hutool.jwt.JWT;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Date;
import java.util.Map;

public class JwtUtil {
    public static final String JWT_HEADER = "Token";
    public static final String KEY = "smlj";
    public static final String AUTHORIZE = "Authorize";
    public static final byte[] KEY_BYTES = KEY.getBytes();
    // 动态延长token过期时间
    // https://mp.weixin.qq.com/s/juSk00SEKhYKb2IkG1NhSQ
    // https://mp.weixin.qq.com/s/fnmGRvE8JFPR5ZG6RfWbIg
    // https://mp.weixin.qq.com/s/bQnoeS1ZwROoaPGIR8oCKg
    public static final long EXPIRE = 4 * 3600 * 1000;

    //接收业务数据,生成token并返回
    public static Triple<Date, Date, String> getToken(Map<String, Object> claims, long expire) {
        var issuedAt = new Date();
        var expireAt = new Date(issuedAt.getTime() + expire);
        String token = JWT.create().addHeaders(null).addPayloads(claims).setKey(KEY_BYTES).setIssuedAt(issuedAt).setExpiresAt(expireAt).sign();
        return Triple.of(issuedAt, issuedAt, token);
    }

    public static JWT getJWTByToken(String token) {
        JWT jwt = null;
        try {
            jwt = cn.hutool.jwt.JWTUtil.parseToken(token);
        } catch (Exception e) {
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
}