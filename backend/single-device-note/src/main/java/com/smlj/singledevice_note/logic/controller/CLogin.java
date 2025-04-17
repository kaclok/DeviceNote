package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TUserDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CLogin", description = "登录")
public class CLogin {
    private final TUserDao userDao;

    @PostMapping(value = "/login")
    public Result<?> login(String account, String password, @RequestParam(required = false) String captcha) {
        String conds = "account = \'" + account + "\' and password = \'" + password + "\' ";
        var ls = userDao.doSelectSimple("t_user", "*", conds, null);
        if (ls.isEmpty()) {
            return Result.fail(ResultCode.RC10301);
        }

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("account", ls.get(0));
        var token = JwtUtil.getToken(claims, JwtUtil.EXPIRE);
        claims.put("token", token);
        return Result.success(claims);
    }
}
