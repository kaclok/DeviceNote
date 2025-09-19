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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/encrypt")
public class CEncrypt {
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/encryptJson")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> encryptJson(@RequestParam(name = "json") String json,
                                 @RequestParam(name = "key") String key,
                                 @RequestParam(name = "iv") String iv) {
        String r = encrypt(json, key.getBytes(), iv.getBytes());
        if (r == null) {
            return Result.fail(ResultCode.RC400);
        }
        return Result.success(r);
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
