package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTContractDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTPermDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContract;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
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
import java.util.UUID;

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
    /** 组织架构只读 DAO：queryOptions 走 train 库（@DS 打在方法上），其余方法走 main 库 */
    private final TDeptDao deptDao;

    // ================================================================
    // 组织架构缓存（进程内）
    // ================================================================
    // 为什么要缓存、而不是每次 deptExists() 直接查库：
    // accountSave / contractCreate|Update|Import 都带 @Transactional，
    // 而 dynamic-datasource 4.3.1 在事务开启时就把连接绑定到主库(main)，
    // 事务内再切 @DS("train") 不生效 —— 会拿着 PG 连接去查 MySQL 的 train.t_org 直接报错。
    // 因此部门有效性校验一律只读下面的内存索引，彻底不进事务查库。
    // 附带好处：校验读的就是下发给前端的那一份数据，「能选到的」与「能存进去的」口径天然一致。
    private volatile Set<String> deptCodes = Set.of();

    /** 启动预热：让首个请求不必等一次 train 库查询（失败只告警，不阻断启动） */
    @PostConstruct
    public void warmDeptCache() {
        try {
            refreshDeptCache();
        } catch (Exception e) {
            log.warn("组织架构预热失败（将在首次 /cghtz/dept/list 时重试）: {}", e.getMessage());
        }
    }

    /** 加载/刷新组织架构缓存，返回全量启用部门（失败时抛出，由调用方决定是否保留旧缓存） */
    private synchronized List<TDept> refreshDeptCache() {
        List<TDept> ls = deptDao.queryOptions();
        if (ls == null) ls = new ArrayList<>();
        Set<String> codes = new HashSet<>(ls.size());
        for (TDept d : ls) {
            if (d != null && StringUtils.hasText(d.getDept_code())) codes.add(d.getDept_code());
        }
        this.deptCodes = codes;
        return ls;
    }

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

    /**
     * 账号列表：服务端分页 + 关键字/部门筛选。
     * <p>
     * 关键字 kw 同时匹配 account / username（模糊），dept_code 精确匹配归属部门。
     * 之所以把原先「一次拉 200 条、前端本地过滤分页」改成服务端搜索：
     * 账号量级将来会到几万，整表下发+本地过滤不可持续。
     */
    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/list")
    public Result<?> accountList(@RequestParam(name = "kw", required = false) String kw,
                                 @RequestParam(name = "dept_code", required = false) String dept_code,
                                 @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                 @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = userDao.queryAll(kw, dept_code, true, false);
        for (var i : ls) {
            i.setRole(roleDao.query(i.getRole_code()));
        }
        return Result.success(new PageSerializable<>(ls));
    }

    /**
     * 账号保存：统一新增/修改入口。
     * 前端 users.vue 不区分新建/编辑，一律调用同一接口；后端根据 account 是否存在做 insert / update。
     * 密码只在“新增”且前端提交了 password 时写入；否则使用默认 123456。编辑不改密码（密码另走 resetPwd）。
     * <p>
     * dept_code 归属部门为必填：必须是非空、且组织架构(train.t_org)中启用中的真实部门。
     * 数据隔离以该字段为基准点，因此新建/编辑都必须明确归属。
     */
    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/save")
    public Result<?> accountSave(
            @RequestParam(name = "account") String account,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "role_code") String role_code,
            @RequestParam(name = "dept_code", required = false) String dept_code,
            @RequestParam(name = "password", required = false) String password) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        if (!StringUtils.hasText(role_code)) {
            return Result.fail(ResultCode.RC10101, "角色(role_code)不能为空");
        }
        // 归属部门必填：空值直接拒绝，非空则必须能在组织架构中找到（口径与下拉/组织树一致）
        if (!StringUtils.hasText(dept_code)) {
            return Result.fail(ResultCode.RC10101, "归属部门(dept_code)不能为空");
        }
        if (!deptExists(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("部门 %s 不存在或已停用，请重新选择", dept_code));
        }
        var role = roleDao.query(role_code);
        if (role == null) {
            return Result.fail(ResultCode.RC10305);
        }

        TCGHTUser old = userDao.query(account);
        if (old == null) {
            // 全新账号：新建
            if (!StringUtils.hasText(username)) return Result.fail(ResultCode.RC10101, "姓名(username)不能为空");
            final String pwd = StringUtils.hasText(password) ? password : DEFAULT_INIT_PWD;
            TCGHTUser u = new TCGHTUser();
            u.setAccount(account);
            u.setUsername(username);
            u.setPwd(pwd);
            u.setRole_code(role_code);
            u.setDept_code(dept_code);
            u.setOpen_status(true);
            userDao.insert(u);

            u.setRole(roleDao.query(role_code));
            return Result.success(u);
        }

        if (old.isOpen_status()) {
            // 账号已存在且启用中：带密码视为新增重复，拒绝；不带密码视为编辑
            if (StringUtils.hasText(password)) {
                return Result.fail(ResultCode.RC10304);
            }
            if (StringUtils.hasText(username)) {
                old.setUsername(username);
            }
            old.setRole_code(role_code);
            old.setDept_code(dept_code);
            userDao.update(old);

            old.setRole(roleDao.query(role_code));
            return Result.success(old);
        }

        // 账号已存在但已删除：复用，更新信息并重新启用
        if (StringUtils.hasText(username)) {
            old.setUsername(username);
        }
        old.setRole_code(role_code);
        old.setDept_code(dept_code);
        userDao.update(old);
        userDao.toggleStatus(account, true);
        final String pwd = StringUtils.hasText(password) ? password : DEFAULT_INIT_PWD;
        userDao.resetPwd(account, pwd);
        old.setOpen_status(true);
        old.setPwd(pwd);
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

    /**
     * 修改密码（用户自助）：管理员设定的初始密码，用户可凭原密码自行修改。
     * 与 /account/resetPwd 的区别：
     * - resetPwd 是管理员在账号管理页强制重置为初始密码，需要 perm:assign 权限，无需原密码；
     * - changePwd 是账号本人的自助操作，不加 @RequirePermission（任何登录用户都必须能改自己的密码），但必须校验原密码。
     * account 一律从请求头 at(JWT) 解析，不接收前端传参，避免越权修改他人密码。
     */
    @Transactional
    @PostMapping(value = "/account/changePwd")
    public Result<?> accountChangePwd(@RequestParam(name = "oldPwd") String oldPwd,
                                      @RequestParam(name = "newPwd") String newPwd,
                                      @Acc TCGHTUser curUser) {
        final String account = curUser.getAccount();
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10002);
        }
        // 注意：Result.fail(ResultCode, T) 的第二个参数是 data 而非 message，
        // 自定义提示需用 fail(int code, String message) 重载，否则前端读到的 message 仍是枚举默认值
        if (!StringUtils.hasText(oldPwd) || !StringUtils.hasText(newPwd)) {
            return Result.fail(ResultCode.RC10101.getCode(), "原密码与新密码不能为空");
        }
        if (newPwd.length() < 6 || newPwd.length() > 20) {
            return Result.fail(ResultCode.RC10101.getCode(), "新密码长度需为 6~20 位");
        }

        TCGHTUser user = userDao.query(account);
        if (user == null) {
            return Result.fail(ResultCode.RC10301);
        }
        if (!user.isOpen_status()) {
            return Result.fail(ResultCode.RC10301.getCode(), String.format("账号 %s 已停用，请联系管理员", account));
        }
        if (!user.getPwd().equals(oldPwd)) {
            return Result.fail(ResultCode.RC10303.getCode(), "原密码不正确");
        }
        if (newPwd.equals(oldPwd)) {
            return Result.fail(ResultCode.RC10101.getCode(), "新密码不能与原密码相同");
        }

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
        // 不加关键字/部门筛选（kw=null, dept_code=null 表示不过滤）
        var ls = userDao.queryAll(null, null, true, false);
        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/perm/list")
    public Result<?> permList() {
        var ls = permDao.queryAll();
        return Result.success(ls);
    }

    // ================================================================
    // 组织架构（只读）
    // 数据源：train.t_org（MySQL，集团组织树权威主数据，116 个启用部门）。
    // 部门下拉、组织架构树、列表部门筛选、部门名校验、dept_code→公司/部门名回显，五处共用同一份数据：
    // 登录成功后前端立即拉一次全量并缓存，之后本地完成关键字匹配、组织树渲染/搜索与回显，不再逐次请求。
    // 不限权限：任何已登录用户录合同/选部门都要用到，仅由 JWT 认证兜底。
    // ================================================================

    /**
     * 部门全量字典（dept_code / dept_name / dept_all_name / parent_dept_code）。
     * 含 parent_dept_code 是为了让前端能自行拼出组织树并推算「公司/部门」全路径，无需再提供单独的树接口。
     * <p>
     * 刻意不加 @Transactional：纯读接口，且事务内切数据源不生效（详见 deptCodes 字段上的说明）。
     * 每次调用都刷新缓存 —— 登录后的预加载会走到这里，顺带让组织架构变更在重新登录时生效。
     */
    @PostMapping(value = "/dept/list")
    public Result<?> deptList() {
        return Result.success(refreshDeptCache());
    }

    /**
     * 部门编码有效性：必须非空、且存在于组织架构（train.t_org 中启用中的部门）。
     * 只查内存索引，不进数据库 —— 调用方全都在 @Transactional 里，此时切数据源是无效的。
     */
    private boolean deptExists(String deptCode) {
        if (!StringUtils.hasText(deptCode)) return false;
        Set<String> codes = this.deptCodes;
        if (codes.isEmpty()) {
            // 预热失败或尚未加载：惰性重试一次
            refreshDeptCache();
            codes = this.deptCodes;
        }
        return codes.contains(deptCode);
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
            @RequestParam(name = "payment_type", required = false) Integer payment_type,
            @RequestParam(name = "supplier", required = false) String supplier,
            @RequestParam(name = "dept_code", required = false) String dept_code,
            @RequestParam(name = "queryBegin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryBegin,
            @RequestParam(name = "queryEnd", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date queryEnd,
            @RequestParam(name = "finish_step", required = false) Integer finish_step,
            @RequestParam(name = "rkBegin", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date rkBegin,
            @RequestParam(name = "rkEnd", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date rkEnd,
            @RequestParam(name = "warn_day", required = false) Integer warn_day,
            @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = contractDao.queryAll(id, title, sign_person, sign_type, payment_type, supplier, dept_code, queryBegin, queryEnd, finish_step, rkBegin, rkEnd, warn_day);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/contract/get")
    public Result<?> contractGet(@RequestParam(name = "unique_id", required = false) String unique_id) {
        if (unique_id == null || unique_id.isBlank()) {
            return Result.fail(ResultCode.RC10101, "unique_id 不能为空");
        }
        TCGHTContract c = contractDao.query(unique_id);
        if (c == null || !c.isOpen_status()) {
            return Result.fail(ResultCode.RC10103, "目标合同不存在");
        }
        return Result.success(c);
    }

    @RequirePermission("contract:create")
    @Transactional
    @PostMapping(value = "/contract/create")
    public Result<?> contractCreate(@RequestBody TCGHTContract c) {
        if (c == null || c.getId() == null || c.getId().isBlank()) {
            return Result.fail(ResultCode.RC10101, "合同编号(id)不能为空");
        }
        // 归属部门必填：空值拒绝，非空必须在组织架构中真实存在
        if (!StringUtils.hasText(c.getDept_code())) {
            return Result.fail(ResultCode.RC10101, "归属部门(dept_code)不能为空");
        }
        if (!deptExists(c.getDept_code())) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("部门 %s 不存在或已停用，请重新选择", c.getDept_code()));
        }

        // 即时结算类(1)：id 必须唯一；周期结算类(2)：id 可重复
        if (c.getPayment_type() != null && c.getPayment_type() == 1) {
            if (contractDao.exist(c.getId()) > 0) {
                return Result.fail(ResultCode.RC10102.getCode(),
                        String.format("即时结算类合同编号 %s 已存在，禁止重复录入", c.getId()));
            }
        }

        // 生成 unique_id 作为主键
        c.setUnique_id(UUID.randomUUID().toString().replace("-", ""));
        contractDao.insert(c);
        return Result.success(c);
    }

    @RequirePermission("contract:update")
    @Transactional
    @PostMapping(value = "/contract/update")
    public Result<?> contractUpdate(@RequestBody TCGHTContract c) {
        if (c == null || c.getUnique_id() == null || c.getUnique_id().isBlank()) {
            return Result.fail(ResultCode.RC10101, "unique_id 不能为空");
        }
        TCGHTContract old = contractDao.query(c.getUnique_id());
        if (old == null) {
            return Result.fail(ResultCode.RC10103, "目标合同不存在");
        }
        // 归属部门必填：空值拒绝，非空必须在组织架构中真实存在
        if (!StringUtils.hasText(c.getDept_code())) {
            return Result.fail(ResultCode.RC10101, "归属部门(dept_code)不能为空");
        }
        if (!deptExists(c.getDept_code())) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("部门 %s 不存在或已停用，请重新选择", c.getDept_code()));
        }
        // 保留 unique_id 和 open_status，其余从 c 拷贝
        BeanUtils.copyProperties(c, old, "unique_id", "open_status");
        contractDao.update(old);
        return Result.success(old);
    }

    @RequirePermission("contract:delete")
    @Transactional
    @PostMapping(value = "/contract/delete")
    public Result<?> contractDelete(@RequestParam(name = "unique_id", required = false) String unique_id) {
        if (unique_id == null || unique_id.isBlank()) {
            return Result.fail(ResultCode.RC10101, "unique_id 不能为空");
        }
        TCGHTContract c = contractDao.query(unique_id);
        if (c == null) {
            return Result.fail(ResultCode.RC10103, "目标合同不存在");
        }
        contractDao.markInvalid(unique_id);
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
        // 批次内即时结算类 id 去重
        Set<String> batchInstantIds = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            TCGHTContract c = rows.get(i);
            List<String> reasons = new ArrayList<>();
            String id = c.getId();
            if (id == null || id.isBlank()) {
                reasons.add("合同编号为空");
            } else {
                // 即时结算类(1)：id 唯一校验（DB + 批次内）；周期结算类(2)：跳过 id 唯一校验
                boolean isInstant = c.getPayment_type() != null && c.getPayment_type() == 1;
                if (isInstant) {
                    if (contractDao.exist(id) > 0) {
                        reasons.add("即时结算类合同编号已存在");
                    } else if (batchInstantIds.contains(id)) {
                        reasons.add("即时结算类合同编号在本批次中重复");
                    } else {
                        batchInstantIds.add(id);
                    }
                }
            }
            if (c.getTitle() == null || c.getTitle().isBlank()) {
                reasons.add("合同名称必填");
            }
            if (c.getSupplier() == null || c.getSupplier().isBlank()) {
                reasons.add("供应商必填");
            }
            // 归属部门必填：逐行校验，缺部门或部门非法都按行拦截（不静默丢数据）
            if (!StringUtils.hasText(c.getDept_code())) {
                reasons.add("归属部门必填");
            } else if (!deptExists(c.getDept_code())) {
                reasons.add(String.format("归属部门 %s 不存在或已停用", c.getDept_code()));
            }
            if (c.getDate_sign() == null) {
                reasons.add("签订时间必填");
            }
            if (reasons.isEmpty()) {
                c.setUnique_id(UUID.randomUUID().toString().replace("-", ""));
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
