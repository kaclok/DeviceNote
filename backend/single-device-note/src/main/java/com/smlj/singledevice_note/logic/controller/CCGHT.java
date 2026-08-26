package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTContractDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTPermDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContract;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cghtz")
@Tag(name = "CCGHT", description = "采购合同")
public class CCGHT {
    private static final String DEFAULT_INIT_PWD = "123456";

    private final CJwt cJwt;
    private final TCGHTUserDao userDao;
    private final TCGHTRoleDao roleDao;
    private final TCGHTPermDao permDao;
    private final TCGHTContractDao contractDao;

    @Transactional
    @PostMapping(value = "/account/login")
    public Result<?> accountLogin(@RequestParam(name = "account") String account,
                                  @RequestParam(name = "pwd") String pwd,
                                  HttpServletResponse response) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(pwd)) {
            return Result.fail(ResultCode.RC10101);
        }
        var user = userDao.query(account);
        if (user == null) {
            return Result.fail(ResultCode.RC10301);
        }
        if (!user.isOpen_status()) {
            return Result.fail(ResultCode.RC10301, String.format("账号 %s 已停用，请联系管理员", account));
        }
        if (!user.getPwd().equals(pwd)) {
            return Result.fail(ResultCode.RC10303);
        }

        TCGHTRole role = roleDao.query(user.getRole_code());
        if (role == null) return Result.fail(ResultCode.RC10305);
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

    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/list")
    public Result<?> accountList(@RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                 @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = userDao.queryAll(true, false);
        for (var i : ls) {
            i.setRole(roleDao.query(i.getRole_code()));
        }
        return Result.success(new PageSerializable<>(ls));
    }

    /**
     * 账号保存：统一新增/修改入口。
     * 前端 users.vue 不区分新建/编辑，一律调用同一接口；后端根据 account 是否存在做 insert / update。
     * 密码只在“新增”且前端提交了 password 时写入；否则使用默认 123456。编辑不改密码（密码另走 resetPwd）。
     */
    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/save")
    public Result<?> accountSave(
            @RequestParam(name = "account") String account,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "role_code") String role_code,
            @RequestParam(name = "password") String password) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        if (!StringUtils.hasText(role_code)) {
            return Result.fail(ResultCode.RC10101, "角色(role_code)不能为空");
        }
        var role = roleDao.query(role_code);
        if (role == null) {
            return Result.fail(ResultCode.RC10305);
        }

        boolean exists = userDao.exist(account) > 0;
        if (!exists) {
            // 新建：username 必填（姓名）
            if (!StringUtils.hasText(username)) return Result.fail(ResultCode.RC10101, "姓名(username)不能为空");
            final String pwd = StringUtils.hasText(password) ? password : DEFAULT_INIT_PWD;
            TCGHTUser u = new TCGHTUser();
            u.setAccount(account);
            u.setUsername(username);
            u.setPwd(pwd);
            u.setRole_code(role_code);
            u.setOpen_status(true);
            userDao.insert(u);

            u.setRole(roleDao.query(role_code));
            return Result.success(u);
        }
        // 编辑：username 不允许置空
        TCGHTUser old = userDao.query(account);
        if (old == null) {
            return Result.fail(ResultCode.RC10103, "目标账号不存在");
        }
        if (StringUtils.hasText(username)) {
            old.setUsername(username);
        }
        old.setRole_code(role_code);
        userDao.update(old);

        old.setRole(roleDao.query(role_code));
        return Result.success(old);
    }

    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/resetPwd")
    public Result<?> accountResetPwd(@RequestParam(name = "account") String account,
                                     @RequestParam(name = "pwd", required = false) String pwd) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        if (userDao.exist(account) <= 0) {
            return Result.fail(ResultCode.RC10301);
        }
        final String newPwd = StringUtils.hasText(pwd) ? pwd : DEFAULT_INIT_PWD;
        userDao.resetPwd(account, newPwd);
        return Result.success();
    }

    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/toggle")
    public Result<?> accountToggle(@RequestParam(name = "account") String account) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        TCGHTUser u = userDao.query(account);
        if (u == null) {
            return Result.fail(ResultCode.RC10301);
        }
        boolean next = !u.isOpen_status();
        // 业务约束：不允许停用当前登录账号自身（当无法获取当前登录人时，此约束由前端 v-notSelf 先行拦截）
        // CJwt.resolveCurrentAccount 若可用，可在此做二次兜底；暂无 CJwt 解析方法则返回 OK，让前端 UI 继续生效
        userDao.toggleStatus(account, next);
        return Result.success();
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
        // signer 下拉用：只返回启用的非管理员，且仅需 account/username（合同 sign_person 关联）
        var ls = userDao.queryAll(true, false);
        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/perm/list")
    public Result<?> permList() {
        var ls = permDao.queryAll();
        return Result.success(ls);
    }

    // ================================================================
    // 合同台账 CRUD（TCGHTContract 24 个业务字段 + open_status 逻辑删除）
    // 查询参数名与前端 query 参数一致；date_sign 日期范围用 queryBegin/queryEnd，date_rk 挂账日期范围用 rkBegin/rkEnd
    // ================================================================

    @Transactional
    @PostMapping(value = "/contract/list")
    public Result<?> contractList(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "sign_person", required = false) String sign_person,
            @RequestParam(name = "sign_type", required = false) Integer sign_type,
            @RequestParam(name = "supplier", required = false) String supplier,
            @RequestParam(name = "queryBegin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryBegin,
            @RequestParam(name = "queryEnd", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryEnd,
            @RequestParam(name = "finish_step", required = false) Integer finish_step,
            @RequestParam(name = "has_rk", required = false) Boolean has_rk,
            @RequestParam(name = "rkBegin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date rkBegin,
            @RequestParam(name = "rkEnd", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date rkEnd,
            @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = contractDao.queryAll(id, title, sign_person, sign_type, supplier, queryBegin, queryEnd, finish_step, has_rk, rkBegin, rkEnd);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/contract/get")
    public Result<?> contractGet(@RequestParam(name = "id", required = false) String id) {
        if (id == null || id.isBlank()) return Result.fail(ResultCode.RC10101, "合同编号(id)不能为空");
        TCGHTContract c = contractDao.query(id);
        if (c == null) return Result.fail(ResultCode.RC10103, "目标合同不存在");
        return Result.success(c);
    }

    @RequirePermission("contract:create")
    @Transactional
    @PostMapping(value = "/contract/create")
    public Result<?> contractCreate(@RequestBody TCGHTContract c) {
        if (c == null || c.getId() == null || c.getId().isBlank()) {
            return Result.fail(ResultCode.RC10101, "合同编号(id)不能为空");
        }
        if (contractDao.exist(c.getId()) > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("duplicate", true);
            return new Result<>(ResultCode.RC10102.getCode(), String.format("合同编号 %s 已存在，禁止重复录入", c.getId()));
        }
        contractDao.insert(c);
        return Result.success(contractDao.query(c.getId()));
    }

    @RequirePermission("contract:update")
    @Transactional
    @PostMapping(value = "/contract/update")
    public Result<?> contractUpdate(@RequestBody TCGHTContract c) {
        if (c == null || c.getId() == null || c.getId().isBlank()) {
            return Result.fail(ResultCode.RC10101, "合同编号(id)不能为空");
        }
        TCGHTContract old = contractDao.query(c.getId());
        if (old == null) return Result.fail(ResultCode.RC10103, "目标合同不存在");
        BeanUtils.copyProperties(c, old, "id", "open_status");
        contractDao.update(old);
        return Result.success(contractDao.query(c.getId()));
    }

    @RequirePermission("contract:delete")
    @Transactional
    @PostMapping(value = "/contract/delete")
    public Result<?> contractDelete(@RequestParam(name = "id", required = false) String id) {
        if (id == null || id.isBlank()) return Result.fail(ResultCode.RC10101, "合同编号(id)不能为空");
        if (contractDao.exist(id) <= 0) return Result.fail(ResultCode.RC10103, "目标合同不存在");
        contractDao.markInvalid(id);
        return Result.success();
    }

    @RequirePermission("contract:import")
    @Transactional
    @PostMapping(value = "/contract/import")
    public Result<?> contractImport(@RequestBody List<TCGHTContract> rows) {
        if (rows == null || rows.isEmpty()) {
            return Result.fail(ResultCode.RC10101, "导入数据为空");
        }
        int okCnt = 0;
        List<Map<String, Object>> failRows = new ArrayList<>();
        Set<String> batchIds = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            TCGHTContract c = rows.get(i);
            List<String> reasons = new ArrayList<>();
            String id = c.getId();
            if (id == null || id.isBlank()) reasons.add("合同编号为空");
            else if (contractDao.exist(id) > 0) reasons.add("合同编号已存在");
            else if (batchIds.contains(id)) reasons.add("合同编号在本批次中重复");
            else batchIds.add(id);
            if (c.getTitle() == null || c.getTitle().isBlank()) reasons.add("合同名称必填");
            if (c.getSupplier() == null || c.getSupplier().isBlank()) reasons.add("供应商必填");
            if (c.getDate_sign() == null) reasons.add("签订时间必填");
            if (reasons.isEmpty()) {
                contractDao.insert(c);
                okCnt++;
            } else {
                Map<String, Object> fail = new HashMap<>();
                fail.put("row", i + 2);
                fail.put("id", id == null ? "-" : id);
                fail.put("title", c.getTitle() == null ? "" : c.getTitle());
                fail.put("reason", String.join("；", reasons));
                failRows.add(fail);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", okCnt);
        data.put("fail", failRows.size());
        data.put("failRows", failRows);
        return Result.success(data);
    }
}
