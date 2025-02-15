package com.smlj.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// https://mp.weixin.qq.com/s/QYIOUNK5FZVu_jdiWGnMZg
public class SignUtil {
    public static String sign(String secretKey, String content) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean verify(String secretKey, String content, String signed) {
        String calculated = sign(secretKey, content);
        if (calculated != null) {
            return calculated.equals(signed);
        }
        return false;
    }
}
