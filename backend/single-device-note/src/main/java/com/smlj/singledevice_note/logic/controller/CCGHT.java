package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTContractDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cghtz")
@Tag(name = "CCGHT", description = "采购合同")
public class CCGHT {
    private final CJwt cJwt;
    private final TCGHTUserDao userDao;
    private final TCGHTRoleDao roleDao;
    private final TCGHTContractDao contractDao;

    @Transactional
    @PostMapping(value = "/account/login")
    public Result<?> accountLogin(@RequestParam(name = "account") String account,
                                  @RequestParam(name = "pwd") String pwd,
                                  HttpServletResponse response) {
        var user = userDao.query(account);
        if (user == null) {
            return Result.fail(ResultCode.RC10301);
        }
        if (!user.getPwd().equals(pwd)) {
            return Result.fail(ResultCode.RC10303);
        }

        TCGHTRole role = roleDao.query(user.getRole_code());
        user.setRole(role);

        // 登录成功，创建JWT令牌
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("user", user);

        JwtUtil.setResponseHeader(response, claims);

        return Result.success(claims);
    }

    @Transactional
    @PostMapping(value = "/account/logout")
    public Result<?> accountLogout(@RequestParam(name = "account") String account) {
        var user = userDao.query(account);
        if (user == null) {
            return Result.fail(ResultCode.RC10301);
        }

        return Result.success(user);
    }

    @Transactional
    @PostMapping(value = "/account/list")
    public Result<?> accountList(@RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                 @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = userDao.queryAll(true);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/account/save")
    public Result<?> accountSave() {
        return null;
    }

    @Transactional
    @PostMapping(value = "/account/resetPwd")
    public Result<?> accountResetPwd() {
        return null;
    }

    @Transactional
    @PostMapping(value = "/account/toggle")
    public Result<?> accountToggle() {
        return null;
    }

    @Transactional
    @PostMapping(value = "/role/list")
    public Result<?> roleList() {
        var ls = roleDao.queryAll();
        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/signer/list")
    public Result<?> signerList() {
        var ls = userDao.queryAll(false);
        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/perm/list")
    public Result<?> permList() {
        return null;
    }

    @Transactional
    @PostMapping(value = "/contract/list")
    public Result<?> contractList(@RequestParam(name = "id", required = false) String id,
                                  @RequestParam(name = "title", required = false) String title,
                                  @RequestParam(name = "signer", required = false) String signer,
                                  @RequestParam(name = "signe_type", required = false) String signe_type,
                                  @RequestParam(name = "queryBegin", required = false) String queryBegin,
                                  @RequestParam(name = "queryEnd", required = false) String queryEnd,
                                  @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {

        return null;
    }

    @Transactional
    @PostMapping(value = "/contract/edit")
    public Result<?> contractEdit(@RequestParam(name = "id", required = false) String id) {
        return null;
    }

    @Transactional
    @PostMapping(value = "/contract/delete")
    public Result<?> contractDelete(@RequestParam(name = "id", required = false) String id) {
        return null;
    }

    @Transactional
    @PostMapping(value = "/contract/RK")
    public Result<?> contractRK(@RequestParam(name = "id", required = false) String id) {
        return null;
    }
}
