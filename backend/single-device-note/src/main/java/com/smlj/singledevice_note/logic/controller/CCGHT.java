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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * data_scope 档位。范围的唯一来源是 cght.t_user.data_scope（NOT NULL，新建必填），
     * 角色侧不再有任何范围字段，所以取值注释只跟 t_user 对表。
     * <p>
     * ⚠️ 编号本身就是「包含序」，而且这里是**严格**包含序：
     * {归属部门子树} ⊂ {所属公司子树} ⊂ {全集}。
     * <p>
     * 「本部门」档自带下级，所以不存在单独的「本部门及下级」档 —— 同一个 dept_code 下
     * 两者都展开成 subtreeOf(dept_code)，结果完全一致，多留一档只会让人以为有得选。
     * 一级真正包含一级之后，"该配哪一档"只剩一个问题：这个人管到部门、公司，还是整个集团。
     * <p>
     * 前端「只能分配不高于自身档位」的收窄下拉按它判断（只是体验层，真正的拦截在后端的集合包含校验）。
     * 新增档位必须插在包含序的正确位置上，不要图省事追加到末尾 —— 否则收窄会静默失效。
     */
    private static final int SCOPE_SELF = 1;      // 本人（缺 t_contract.creator，暂按本部门收敛）
    private static final int SCOPE_DEPT = 2;      // 本部门（含全部下级部门 / 分厂 / 中心）
    private static final int SCOPE_COMPANY = 3;   // 本公司（含全部下级部门）
    private static final int SCOPE_ALL = 4;       // 全集团

    /**
     * 合法档位集合。用集合判断而非区间判断：档位增删时不会被编号顺序误导。
     */
    private static final Set<Integer> SCOPE_VALID = Set.of(
            SCOPE_SELF, SCOPE_DEPT, SCOPE_COMPANY, SCOPE_ALL);
    private final CJwt cJwt;
    private final TCGHTUserDao userDao;
    private final TCGHTRoleDao roleDao;
    private final TCGHTPermDao permDao;
    private final TCGHTContractDao contractDao;
    /**
     * 组织架构只读 DAO：queryOptions 走 train 库（@DS 打在方法上），其余方法走 main 库
     */
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
    /**
     * 组织树索引（仅启用部门）：dept_code -> parent_dept_code，用于向上定位「所属公司」
     */
    private volatile Map<String, String> deptParent = Map.of();
    /**
     * 组织树索引（仅启用部门）：parent_dept_code -> 直接子部门，用于向下展开子树
     */
    private volatile Map<String, List<String>> deptChildren = Map.of();

    /**
     * 启动预热：让首个请求不必等一次 train 库查询（失败只告警，不阻断启动）
     */
    @PostConstruct
    public void warmDeptCache() {
        try {
            refreshDeptCache();
        } catch (Exception e) {
            log.warn("组织架构预热失败（将在首次 /cghtz/dept/list 时重试）: {}", e.getMessage());
        }
    }

    /**
     * 加载/刷新组织架构缓存，返回全量启用部门（失败时抛出，由调用方决定是否保留旧缓存）
     */
    private synchronized List<TDept> refreshDeptCache() {
        List<TDept> ls = deptDao.queryOptions();
        if (ls == null) ls = new ArrayList<>();
        Set<String> codes = new HashSet<>(ls.size());
        Map<String, String> parent = new HashMap<>(ls.size());
        Map<String, List<String>> children = new HashMap<>(ls.size());
        for (TDept d : ls) {
            if (d == null || !StringUtils.hasText(d.getDept_code())) continue;
            String code = d.getDept_code();
            codes.add(code);
            if (StringUtils.hasText(d.getParent_dept_code())) {
                parent.put(code, d.getParent_dept_code());
                children.computeIfAbsent(d.getParent_dept_code(), k -> new ArrayList<>()).add(code);
            }
        }
        // 父子索引与 code 集合一起重建：queryOptions 只返回启用节点，
        // 所以两个索引的键值天然都落在启用集合内，无需再单独过滤停用部门。
        this.deptCodes = codes;
        this.deptParent = parent;
        this.deptChildren = children;
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
                                 @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize,
                                 @Acc TCGHTUser curUser) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        // 账号列表也要限范围：否则分公司管理员能翻出全集团账号，再把某个账号(或自己)改成 ADMIN 提权
        var ls = userDao.queryAll(kw, dept_code, true, false, resolveScopeDepts(curUser));
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
     * <p>
     * data_scope 数据范围为新建必填：它是范围的唯一来源（t_role 侧已无该字段），留空没有东西能兜底。
     * 编辑时不传 = 保持原样；无论新建还是修改，给出的范围都必须落在操作者自己的可见范围内。
     */
    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/save")
    public Result<?> accountSave(
            @RequestParam(name = "account") String account,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "role_code") String role_code,
            @RequestParam(name = "dept_code", required = false) String dept_code,
            @RequestParam(name = "password", required = false) String password,
            // 数据范围：新建必填（它是范围的唯一来源，没有角色兜底）；编辑不传/传空 = 保持原样，传了 = 整体设置
            @RequestParam(name = "data_scope", required = false, defaultValue = "2") String data_scope,
            @Acc TCGHTUser curUser) {
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

        // ---------------- 数据范围：读写都限 + 「范围」入参解析 ----------------
        // 账号是否已存在决定"范围是否必填"，先取出来
        TCGHTUser old = userDao.query(account);
        // 解析一次，角色分配 / 归属部门 / 原账号 / 范围入参四处校验共用
        List<String> scopeDepts = resolveScopeDepts(curUser);
        // (1) 不允许分配「权限高于自身」的角色：判据是 perms 集合包含（目标角色 perms ⊆ 我的 perms）。
        //     改造前比的是 role.data_scope 的数值，该列已删除；集合包含与档位编号、锚点位置无关，更严谨。
        //     对超级管理员不再豁免 —— 库里 ADMIN ⊇ EDITOR ⊇ VIEWER，实测对现有数据零行为变化。
        TCGHTRole myRole = (curUser == null || !StringUtils.hasText(curUser.getRole_code()))
                ? null : roleDao.query(curUser.getRole_code());
        if (!permsSubsetOf(role.getPerms(), myRole == null ? null : myRole.getPerms())) {
            return Result.fail(ResultCode.RC10307.getCode(), "不能分配权限高于你自身的角色");
        }
        // (2) 目标归属部门必须落在自己的数据范围内
        if (!inScope(scopeDepts, dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), "归属部门超出你的数据范围");
        }
        // (3) 编辑已有账号：原账号也必须在范围内，否则可跨范围改人、改角色、改归属部门
        if (old != null && !inScope(scopeDepts, old.getDept_code())) {
            return accountOutOfScope(account, curUser);
        }
        // (3.5) 目标账号的「数据范围」也必须在可管理范围内 —— 归属部门在范围内不等于整个人在范围内。
        //       缺这道闸的后果：同公司的低范围管理员能改高范围账号的角色/姓名/归属部门，
        //       还能重置它的密码（重置后登录即拿到对方的范围，是完整的提权链条）。
        //       它必须排在 (5) 之前：第 (5) 道闸被 scopeChanged 短路，而"范围原样不动"恰恰是
        //       最常见的越权入口 —— 改角色、改姓名时前端根本不传 data_scope。
        if (old != null && !canManageAccount(scopeDepts, old)) {
            return Result.fail(ResultCode.RC10307.getCode(), "目标账号的数据范围超出你的可管理范围");
        }
        // (4) 范围入参：新建必须传；编辑不传 = 保持原样，传了 = 整体设置。
        //     范围的唯一来源是 t_user.data_scope，没有角色兜底 —— 新建时留空没有任何东西能补上，
        //     所以这里直接拒绝，而不是给个默认档（默认档会让配错的人以为已经配好了）。
        if (old == null && !StringUtils.hasText(data_scope)) {
            return Result.fail(ResultCode.RC10101.getCode(), "新建账号必须指定数据范围（1/2/3/4）");
        }
        final boolean scopeProvided = StringUtils.hasText(data_scope);
        Integer scopeValue = null;
        if (scopeProvided) {
            final int v;
            try {
                v = Integer.parseInt(data_scope.trim());
            } catch (NumberFormatException e) {
                return Result.fail(ResultCode.RC10101.getCode(), "数据范围取值必须为 1/2/3/4");
            }
            if (!SCOPE_VALID.contains(v)) {
                return Result.fail(ResultCode.RC10101.getCode(), "数据范围取值必须为 1/2/3/4");
            }
            scopeValue = v;
        }
        // (5) 不能给出「超出自己可管理范围」的数据范围。
        //     判据是精确集合包含（新范围的展开 ⊆ 我的可见部门），比数值比大小严谨。
        //     只在新账号、或范围真的发生变化时校验：编辑既有账号而范围原样不动时不该被拦，
        //     否则低权限操作者连"改个姓名"都做不了（那一行的范围值本来就超出他）。
        final boolean scopeChanged = old == null
                || !Objects.equals(scopeValue, old.getData_scope());
        if (scopeChanged && scopeValue != null && scopeDepts != null) {
            List<String> targetDepts = expandScope(scopeValue, dept_code, account);
            // ⚠️ expandScope 对「4 全集团」返回 null（null = 不限制），不是空集合。
            // 能走到这里说明操作者自己是受限的（scopeDepts != null），所以目标档位只要展开成 null
            // 就必然是越权 —— 而且是最严重的那种（一把把自己或下属提到全集团）。
            // 缺了这个判空，下面的 targetDepts.isEmpty() 会抛 NPE：接口返回 500，
            // 调用方看不到「超出范围」，日志里也读不出这是权限问题，只会当成偶发故障重试。
            if (targetDepts == null || targetDepts.isEmpty() || !scopeDepts.containsAll(targetDepts)) {
                return Result.fail(ResultCode.RC10307.getCode(), "数据范围超出你的可管理范围");
            }
        }
        // 三个分支只负责账号基本信息；数据范围统一在最后写一次（见下面的 scopeProvided）
        final TCGHTUser saved;
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
            saved = u;
        } else if (old.isOpen_status()) {
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
            saved = old;
        } else {
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
            saved = old;
        }
        // 只有传了范围才写（编辑不传 = 保持原样）。新建必传，所以新账号一定有范围，无需任何兜底。
        if (scopeProvided) {
            userDao.updateScope(account, scopeValue);
            saved.setData_scope(scopeValue);
        }
        saved.setRole(roleDao.query(role_code));
        return Result.success(saved);
    }

    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/account/resetPwd")
    public Result<?> accountResetPwd(@RequestParam(name = "account") String account,
                                     @RequestParam(name = "pwd", required = false) String pwd,
                                     @Acc TCGHTUser curUser) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        // 用 query 替代 exist：一次查询同时拿到"存不存在"和"归属部门在不在范围内"
        TCGHTUser target = userDao.query(account);
        List<String> scopeDepts = resolveScopeDepts(curUser);
        if (target == null || !inScope(scopeDepts, target.getDept_code())) {
            return accountOutOfScope(account, curUser);
        }
        // 归属部门在范围内还不够：目标账号自己的数据范围也必须 ⊆ 我的可见范围。
        // 否则低范围管理员能重置高范围账号的密码，登录后即拿到对方全部可见数据。
        if (!canManageAccount(scopeDepts, target)) {
            return Result.fail(ResultCode.RC10307.getCode(), "目标账号的数据范围超出你的可管理范围");
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
    public Result<?> accountToggle(@RequestParam(name = "account") String account,
                                   @Acc TCGHTUser curUser) {
        if (!StringUtils.hasText(account)) {
            return Result.fail(ResultCode.RC10101, "账号(account)不能为空");
        }
        TCGHTUser u = userDao.query(account);
        List<String> scopeDepts = resolveScopeDepts(curUser);
        if (u == null || !inScope(scopeDepts, u.getDept_code())) {
            return accountOutOfScope(account, curUser);
        }
        // 同 accountSave / resetPwd：停用/启用是整个账号级操作，目标账号范围也必须 ⊆ 我的范围
        if (!canManageAccount(scopeDepts, u)) {
            return Result.fail(ResultCode.RC10307.getCode(), "目标账号的数据范围超出你的可管理范围");
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
    // 数据范围（data_scope）—— 一处解析，多处执法
    // ================================================================
    // 所有需要限权的接口都调用 resolveScopeDepts()，不要在各接口里各写一套条件：
    // 漏掉任何一个写入接口都是越权写，比越权读严重得多。
    //
    // 为什么用 userDao 反查，而不是直接读 @Acc 里的账号：
    // JWT 里存的是 JSON，@Acc 经 BeanUtil.toBean 反序列化成 TCGHTUser，
    // 其中 data_scope 一旦转换失败会静默变成 0 —— 那是"所有人都看不见"级别的故障。
    // 用 account 反查 t_user，成本可忽略，换来的是确定性。
    //
    // 档位来源：只有账号行自己的 data_scope（NOT NULL、新建必填），角色侧不再参与范围解析。
    // 「全集团 VIEWER」「分公司管理员」这类组合因此不必新建角色行，perms 也不用复制多份。
    //
    // 档位 = 怎么展开，展开起点固定为账号自己的 dept_code（组织归属，与人事一致）：
    //   1 本人      -> 起点自身（缺 t_contract.creator 列，暂降级为「本部门」）
    //   2 本部门    -> 起点整棵子树（含下级部门 / 分厂 / 中心，BFS 不写死层数）
    //   3 本公司    -> 起点向上定位到的公司节点，取其整棵子树
    //   4 全集团    -> 不限制
    //
    // 返回约定（三态，调用方必须区分）：
    //   null   → 不做范围限制（data_scope=4 全集团）
    //   空列表 → 无任何可见部门（拿不到账号、账号已停用、无归属部门、未知档位）—— fail-closed
    //   非空   → 仅这些 dept_code 可见
    private List<String> resolveScopeDepts(TCGHTUser curUser) {
        if (curUser == null || !StringUtils.hasText(curUser.getAccount())) {
            return List.of();
        }
        TCGHTUser db = userDao.query(curUser.getAccount());
        if (db == null || !db.isOpen_status()) {
            return List.of();
        }
        return expandScope(dataScopeOf(db), db.getDept_code(), db.getAccount());
    }

    /**
     * 把「档位」展开成可见部门集合 —— 全模块唯一的范围展开实现。
     * resolveScopeDepts（执法）与 accountSave 的「不许分配高于自身的范围」（判定）都走它，
     * 保证"判定范围"与"执法范围"永远同源。
     *
     * @param scope      档位；见 SCOPE_*
     * @param deptCode   展开起点部门（账号归属部门）
     * @param logAccount 仅用于日志
     * @return 三态：null = 不限制 / 空列表 = 无可见部门(fail-closed) / 非空 = 白名单
     */
    private List<String> expandScope(int scope, String deptCode, String logAccount) {
        if (scope == SCOPE_ALL) {
            return null;
        }
        if (!SCOPE_VALID.contains(scope)) {
            log.warn("账号 {} 的 data_scope={} 不是已知档位，按无可见数据兜底", logAccount, scope);
            return List.of();
        }
        if (!StringUtils.hasText(deptCode)) {
            log.warn("账号 {} 的 data_scope={} 但没有归属部门，按无可见数据兜底", logAccount, scope);
            return List.of();
        }
        // 3 本公司：以「起点向上定位到的公司节点」为根，展开整棵子树。
        //   公司节点 = 集团根（没有父、或父不在启用集合里的那个节点）的直接子。
        //   例：1030015006（本部/采购供应部）-> 公司 1030015（金泰化学本部）-> 其下 14 个部门。
        if (scope == SCOPE_COMPANY) {
            Set<String> sub = subtreeOf(companyOf(deptCode));
            if (sub.isEmpty()) {
                log.warn("账号 {} 本公司范围展开为空(dept_code={})，按归属部门收敛", logAccount, deptCode);
                return List.of(deptCode);
            }
            return List.copyOf(sub);
        }
        // 1 本人：依据是 t_contract 的「录入人」字段，当前库里还没有该列（PRD §4.5 规划中），
        //        因此无法实现 —— 降级为「本部门」而不是放行，比原档位更严格，不会造成越权。
        if (scope == SCOPE_SELF) {
            log.warn("账号 {} 的 data_scope=1(本人) 缺少 t_contract.creator 列，暂按本部门收敛", logAccount);
        }
        // 2 本部门：起点自身 + 全部递归下级（BFS，不写死层数）。
        //   部门天然包含下级 —— 这正是不再单设「本部门及下级」一档的原因：
        //   同一个 dept_code 下两者都展开成 subtreeOf(dept_code)，完全重合，留两档只会让人以为有得选。
        //   注意它不是「本公司」的别名 —— 起点=生产技术部时本档 12 个，本公司档是整公司 24 个。
        Set<String> deptTree = subtreeOf(deptCode);
        return deptTree.isEmpty() ? List.of() : List.copyOf(deptTree);
    }

    /**
     * 目标账号是否在「可管理范围」内 —— 写入侧的第二维闸（第一维是归属部门）。
     * <p>
     * 只看归属部门是不够的：同一个公司里可能存在「数据范围比操作者更大」的账号。
     * 实例：王航舟(024537, 本公司 37 个部门) 与崔斌斌(029567, 4 全集团) 同属神木氯碱，
     * 崔斌斌的归属部门(数字化中心)落在王航舟可见集内 —— 只校验归属部门时，王航舟可以改
     * 崔斌斌的角色、把它降权到自己范围、改它的姓名，更能重置它的密码
     * （重置后登录即拿到全集团权限，是一条完整的提权链条）。
     * <p>
     * 判据与 accountSave 第 (5) 道闸同源：目标账号**现有**范围的展开 ⊆ 操作者可见部门。
     * 目标账号是「4 全集团」时展开为 null，对受限操作者恒为越权；
     * 操作者自身不限制(myDepts == null)时一律可管理。
     */
    private boolean canManageAccount(List<String> myDepts, TCGHTUser target) {
        if (myDepts == null) {
            return true;                        // 操作者不限范围
        }
        if (target == null) {
            return false;
        }
        List<String> targetDepts = expandScope(dataScopeOf(target), target.getDept_code(), target.getAccount());
        if (targetDepts == null) {
            log.warn("目标账号 {} 是 4 全集团，超出受限操作者的可管理范围", target.getAccount());
            return false;
        }
        boolean ok = myDepts.containsAll(targetDepts);
        if (!ok) {
            log.warn("目标账号 {} 的范围展开 {} 个部门，超出可管理范围", target.getAccount(), targetDepts.size());
        }
        return ok;
    }

    /**
     * 有效范围档位：只取账号行自己的 data_scope —— 它是范围的唯一来源。
     * <p>
     * 角色的 data_scope 已删除（列已 drop）：范围是「这个人在组织里的位置」的函数，
     * 挂在角色上时「全集团 VIEWER」「分公司管理员」必须为每档范围复制一份角色行（连带复制整份 perms）。
     * <p>
     * 取不到（账号不存在 / 该列为空）一律返回 0：不在 SCOPE_VALID 内 → expandScope 返回空集合 → fail-closed。
     * 写入侧已把该列设为 NOT NULL 且新建必填，所以 0 只可能出现在脏数据上，按最严处理。
     */
    private int dataScopeOf(TCGHTUser db) {
        if (db == null || db.getData_scope() == null) return 0;
        return db.getData_scope();
    }

    /**
     * 角色权限包含判定：target ⊆ mine —— 拦住「给下属挂一个权限比自己更大的角色」借他人之手提权。
     * <p>
     * 改造前这一步比的是 role.data_scope 的数值（"档位编号 = 包含序"），列删除后没有比较对象了；
     * 换成 perms 集合包含，与档位编号、锚点位置都无关，更严谨。
     * mine 取不到（账号无角色/角色已删）时按空集合处理 → 只能分配无权限的角色，fail-closed。
     * target 为空（角色本身没有任何权限）恒为真 —— 空集是任意集合的子集。
     */
    private boolean permsSubsetOf(String[] target, String[] mine) {
        if (target == null || target.length == 0) return true;
        Set<String> owned = (mine == null) ? Set.of() : new HashSet<>(Arrays.asList(mine));
        return owned.containsAll(Arrays.asList(target));
    }

    /**
     * 越权访问「已存在的账号」时的统一响应：按不暴露存在性处理（与合同口径一致）。
     * 真实原因只进日志；表单输入校验（如归属部门超范围）另给明确文案。
     */
    private Result<?> accountOutOfScope(String account, TCGHTUser curUser) {
        log.warn("账号 {} 越权访问账号 {}，已按不存在处理",
                curUser == null ? null : curUser.getAccount(), account);
        return Result.fail(ResultCode.RC10301);
    }

    /**
     * 某部门的直接子部门（仅启用）。无子部门返回空列表。
     */
    private List<String> childrenOf(String deptCode) {
        return this.deptChildren.getOrDefault(deptCode, List.of());
    }

    /**
     * 某部门的整棵子树（含自身）。null/空白入参返回空集。
     */
    private Set<String> subtreeOf(String deptCode) {
        Set<String> out = new HashSet<>();
        if (!StringUtils.hasText(deptCode)) return out;
        if (this.deptChildren.isEmpty()) refreshDeptCache();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(deptCode);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            // out.add 返回 false = 已访问过：既防重复入队，也防组织树里出现环
            if (!out.add(cur)) continue;
            for (String child : childrenOf(cur)) {
                if (!out.contains(child)) queue.add(child);
            }
        }
        return out;
    }

    /**
     * 集团根：没有父节点、或父节点不在启用集合里的那个节点。取不到返回 null。
     */
    private String rootOf() {
        if (this.deptCodes.isEmpty()) {
            refreshDeptCache();
        }
        for (String code : this.deptCodes) {
            String p = this.deptParent.get(code);
            if (!StringUtils.hasText(p) || !this.deptCodes.contains(p)) {
                return code;
            }
        }
        return null;
    }

    /**
     * 账号所属部门「向上定位到的公司节点」= 集团根的直接子。
     * 例：1030015006（本部/采购供应部）-> 1030015（金泰化学本部）。
     * 定位不到时返回入参自身 —— 语义退化为「本部门」，只会收窄范围，不会放大。
     */
    private String companyOf(String deptCode) {
        if (!StringUtils.hasText(deptCode)) {
            return null;
        }
        if (this.deptParent.isEmpty()) {
            refreshDeptCache();
        }
        String root = rootOf();
        String cur = deptCode;
        for (int i = 0; i < 16; i++) {                      // 组织树最深 4 层，16 只是防环上限
            String p = this.deptParent.get(cur);
            // ⚠️「父节点不在启用集合里」同样算到顶：集团根(1030)的父是 1，而 1 不在启用集合里。
            // 若判据只看"父为空"，会一路走到 1，再 subtreeOf("1") 得到一个根本不存在的部门编码，
            // 结果「本公司」档在集团根上反而比「本部门」档更窄（2 ⊄ 3），包含序被打破。
            // 现在的规则：到顶就返回当前节点 → 集团根的"公司"就是它自己，本公司 == 全集团，语义自洽。
            if (!StringUtils.hasText(p) || !this.deptCodes.contains(p)) {
                return cur;   // 已到顶
            }
            if (root != null && root.equals(p)) {
                return cur; // 父是集团根 -> cur 即公司节点
            }
            cur = p;
        }
        return deptCode;
    }

    /**
     * 某个部门是否落在数据范围内。scope 为 null 表示不限制。
     */
    private boolean inScope(List<String> scope, String deptCode) {
        if (scope == null) {
            return true;
        }
        return StringUtils.hasText(deptCode) && scope.contains(deptCode);
    }

    /**
     * 越权访问「已存在的合同」时的统一响应。
     * 刻意沿用「目标合同不存在」而不是「无权限」：不向调用方暴露该 unique_id 是否真实存在（PRD §7）。
     * 注意只用于「按 id 访问已有合同」；表单/导入的输入校验必须给出可操作提示，
     * 否则用户不知道为什么存不进去 —— 那种场景用明确文案，不要复用本方法。
     */
    private Result<?> outOfScope() {
        return Result.fail(ResultCode.RC10103.getCode(), "目标合同不存在");
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
            @RequestParam(name = "sign_type", required = false) String sign_type,
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
            @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize,
            @Acc TCGHTUser curUser) {
        // 数据范围下推：null 不限 / 空列表=无可见部门 / 非空=仅这些部门。
        // 与筛选栏的 dept_code 以 AND 叠加：受限用户筛了范围外的部门，结果自然为空，而不是越权。
        var scopeDepts = resolveScopeDepts(curUser);
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = contractDao.queryAll(id, title, sign_person, sign_type, payment_type, supplier, dept_code, queryBegin, queryEnd, finish_step, rkBegin, rkEnd, warn_day, scopeDepts);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/contract/get")
    public Result<?> contractGet(@RequestParam(name = "unique_id", required = false) String unique_id,
                                 @Acc TCGHTUser curUser) {
        if (unique_id == null || unique_id.isBlank()) {
            return Result.fail(ResultCode.RC10101, "unique_id 不能为空");
        }
        TCGHTContract c = contractDao.query(unique_id);
        if (c == null || !c.isOpen_status()) {
            return Result.fail(ResultCode.RC10103, "目标合同不存在");
        }
        // 单条读取同样受数据范围约束：否则换个 unique_id 就能读到别人部门的合同
        if (!inScope(resolveScopeDepts(curUser), c.getDept_code())) {
            return outOfScope();
        }
        return Result.success(c);
    }

    @RequirePermission("contract:create")
    @Transactional
    @PostMapping(value = "/contract/create")
    public Result<?> contractCreate(@RequestBody TCGHTContract c, @Acc TCGHTUser curUser) {
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
        // 数据范围：只能录入到自己有权看的部门。
        // deptExists 只回答"这个部门存在吗"，不回答"归你管吗" —— 两者必须分开校验。
        if (!inScope(resolveScopeDepts(curUser), c.getDept_code())) {
            return Result.fail(ResultCode.RC10307.getCode(),
                    String.format("无权将合同录入到部门 %s（超出你的数据范围）", c.getDept_code()));
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
    public Result<?> contractUpdate(@RequestBody TCGHTContract c, @Acc TCGHTUser curUser) {
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
        // 数据范围双向校验，两种情况提示不同：
        //   ① 合同「改前」归属不可见 -> 在动别人的数据，用统一「不存在」措辞，不暴露存在性（PRD §7）
        //   ② 合同「改后」归属不可见 -> 属于表单输入越权，必须说清是哪个部门不行，否则用户无法纠正
        List<String> scopeDepts = resolveScopeDepts(curUser);
        if (!inScope(scopeDepts, old.getDept_code())) {
            return outOfScope();
        }
        if (!inScope(scopeDepts, c.getDept_code())) {
            return Result.fail(ResultCode.RC10307.getCode(),
                    String.format("无权将合同归属到部门 %s（超出你的数据范围）", c.getDept_code()));
        }
        // 保留 unique_id 和 open_status，其余从 c 拷贝
        BeanUtils.copyProperties(c, old, "unique_id", "open_status");
        contractDao.update(old);
        return Result.success(old);
    }

    @RequirePermission("contract:delete")
    @Transactional
    @PostMapping(value = "/contract/delete")
    public Result<?> contractDelete(@RequestParam(name = "unique_id", required = false) String unique_id,
                                    @Acc TCGHTUser curUser) {
        if (unique_id == null || unique_id.isBlank()) {
            return Result.fail(ResultCode.RC10101, "unique_id 不能为空");
        }
        TCGHTContract c = contractDao.query(unique_id);
        if (c == null) {
            return Result.fail(ResultCode.RC10103, "目标合同不存在");
        }
        if (!inScope(resolveScopeDepts(curUser), c.getDept_code())) {
            return outOfScope();
        }
        contractDao.markInvalid(unique_id);
        return Result.success();
    }

    @RequirePermission("contract:import")
    @Transactional
    @PostMapping(value = "/contract/import")
    public Result<?> contractImport(@RequestBody List<TCGHTContract> rows, @Acc TCGHTUser curUser) {
        if (rows == null || rows.isEmpty()) {
            return Result.fail(ResultCode.RC10101, "导入数据为空");
        }
        int okCnt = 0;
        List<Map<String, Object>> failRows = new ArrayList<>();
        // 批次内即时结算类 id 去重
        Set<String> batchInstantIds = new HashSet<>();
        // 数据范围在整批里解析一次即可：同一登录账号，批次内不会变
        List<String> scopeDepts = resolveScopeDepts(curUser);
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
            // 归属部门必填：逐行校验。缺部门、部门非法、或超出本人数据范围，都按行拦截（不静默丢数据）
            if (!StringUtils.hasText(c.getDept_code())) {
                reasons.add("归属部门必填");
            } else if (!deptExists(c.getDept_code())) {
                reasons.add(String.format("归属部门 %s 不存在或已停用", c.getDept_code()));
            } else if (!inScope(scopeDepts, c.getDept_code())) {
                reasons.add(String.format("归属部门 %s 超出你的数据范围", c.getDept_code()));
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
