package com.smlj.encrypt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlj.encrypt.core.Result;
import com.smlj.encrypt.core.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/encrypt")
public class CEncrypt {
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/encryptJson")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> encryptJson(@RequestParam(name = "data") String data,
                                 @RequestParam(name = "zypdata") String zypdata,
                                 @RequestParam(name = "key") String key,
                                 @RequestParam(name = "iv") String iv) {
        String r_data = encrypt(data, key.getBytes(), iv.getBytes());
        String r_zypdata = encrypt(zypdata, key.getBytes(), iv.getBytes());
        if (r_data == null) {
            return Result.fail(data + "加密失败");
        }
        if (r_zypdata == null) {
            return Result.fail(zypdata + "加密失败");
        }

        Map<String, String> map = new HashMap<>();
        map.put("data", r_data);
        map.put("zypdata", r_zypdata);

        return Result.success(map);
    }

    public static String encrypt(String plainText, byte[] key, byte[] iv) {
        String sr = null;
        try {
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            GCMBlockCipher cipher = new GCMBlockCipher(new
                    AESFastEngine());
            AEADParameters parameters =
                    new AEADParameters(new KeyParameter(key),
                            128, iv, null);
            cipher.init(true, parameters);
            byte[] encryptedBytes = new
                    byte[cipher.getOutputSize(plainBytes.length)];
            int retLen = cipher.processBytes(plainBytes, 0, plainBytes.length, encryptedBytes, 0);
            cipher.doFinal(encryptedBytes, retLen);
            sr = Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            sr = null;
        }
        return sr;
    }
}
