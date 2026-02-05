package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CLogin", description = "登录")
public class CLogin {
    private final TDeptUserDao deptUserDao;
    private final TDeptDao deptDao;

    @Transactional
    @PostMapping(value = "/loginWithAccount")
    public Result<?> loginWithAccount(String account, String password) {
        var userList = deptUserDao.queryByAccount(account);
        if (userList == null || userList.isEmpty()) {
            return Result.fail(ResultCode.RC10301);
        }

        if (userList.size() == 1) {
            return loginWithUser(userList.get(0), password);
        }

        // 用户列表展示给前端
        HashMap<String, Object> r = new HashMap<>();
        r.put("result", 1);
        r.put("userList", userList);
        return Result.success(r);
    }

    @Transactional
    private Result<?> loginWithUser(TDeptUser user, String password) {
        if (user == null) {
            return Result.fail(ResultCode.RC10301);
        }

        String deptCode = user.getDept_code();
        var org = deptDao.queryById(deptCode);
        if (org == null) {
            return Result.fail(ResultCode.RC10302);
        }

        HashMap<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("account", user.getAccount());
        claims.put("name", user.getName());

        claims.put("dept_code", deptCode);
        claims.put("dept_name", org.getDept_name());
        claims.put("dept_all_name", org.getDept_all_name());

        String token = JwtUtil.getToken(claims, JwtUtil.EXPIRE).getRight();
        claims.put(JwtUtil.TOKEN, token);

        HashMap<String, Object> r = new HashMap<>();
        r.put("result", 0);
        r.put("id", user.getId());
        r.put("account", user.getAccount());
        r.put("claims", claims);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/loginWithId")
    public Result<?> loginWithId(String id, String password) {
        var user = deptUserDao.queryById(id);
        return loginWithUser(user, password);
    }
}
