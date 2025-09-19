package com.smlj.encrypt.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static byte[] key = "eMoCB4tYXI3bK4W3CH3c1g==".getBytes();
    private static byte[] iv = "S2xI6A5Kj4p+wn7pSg7Ygw==".getBytes();

    @PostMapping(value = "/encryptJson")
    // formType = 0 表示所有  1是作业票  2是操作票
    public String encryptJson(@RequestParam(name = "json") String json) throws JsonProcessingException {
        /*var t = new TestXX(212);
        json = objectMapper.writeValueAsString(t);*/
        return encrypt(json);
    }

    public static String encrypt(String plainText) {
        String sr;
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
            throw new RuntimeException(ex.getMessage());
        }
        return sr;
    }
}
