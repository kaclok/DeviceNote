package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/x")
@Tag(name = "CJwt", description = "jwt相关操作")
public class CJwt {
    @PostMapping("/getAccessToken")
    public Triple<Date, Date, String> getAccessToken(Map<String, Object> claims) {
        return JwtUtil.getToken(claims, JwtUtil.ACCESS_EXPIRE);
    }

    @PostMapping("/getRefreshToken")
    public Triple<Date, Date, String> getRefreshToken(Map<String, Object> claims) {
        return JwtUtil.getToken(claims, JwtUtil.RRFRESH_EXPIRE);
    }

    @PostMapping("/refreshAccessToken")
    public Result<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        String rt = request.getHeader(JwtUtil.RT_HEADER);
        var claims = JwtUtil.parseToken(rt);
        JwtUtil.setAccessTokenHeader(response, claims);
        return Result.success();
    }
}
