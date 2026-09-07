package com.smlj.singledevice_note.core.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 签名工具：HMAC-SHA256 + hex 输出（与前端 CryptoJS.HmacSHA256(...).toString() 对齐）
 */
public class SignUtil {

    /** 与前端 SignParamUtil.js 的 SECRET_KEY 保持一致 */
    public static final String SECRET_KEY = "your-secret-key";

    public static String sign(String content) {
        return sign(SECRET_KEY, content);
    }

    public static String sign(String secretKey, String content) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] raw = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            // 转 hex 字符串，与前端 CryptoJS.HmacSHA256(...).toString() 一致
            return bytesToHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean verify(String content, String signed) {
        return verify(SECRET_KEY, content, signed);
    }

    public static boolean verify(String secretKey, String content, String signed) {
        String calculated = sign(secretKey, content);
        if (calculated != null && signed != null) {
            return calculated.equals(signed);
        }
        return false;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
