package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/x")
@Tag(name = "CAliQRCode", description = "支付宝扫码登录相关")
public class CAliQRCode {
    @JwtIgnore
    @GetMapping("/api/alipay/callback")
    public void callback(@RequestParam("auth_code") String authCode,
                         @RequestParam(value = "state", required = false) String state,
                         @RequestParam(value = "app_id", required = false) String appId,
                         @RequestParam(value = "scope", required = false) String scope,
                         HttpServletResponse response) throws IOException {
        log.error("authCode:{} state:{} appId{}:", authCode, state, appId);
    }
}
