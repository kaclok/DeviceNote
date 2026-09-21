package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.DataScope;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTContractDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTContractTemplateDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTPermDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptContractTemplateDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContract;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContractTemplate;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptContractTemplate;
import com.smlj.singledevice_note.logic.service.CurUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
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

    /**
     * 导入链路当前真正写入的物理表。
     * <p>
     * 模版的 tb_name 已经可以指向别的表（例如 t_contract_smds_sc），但台账的读路径
     * （contract/list|get|update|delete）还没有按模版路由。此时若照着 tb_name 落表，
     * 数据会写进一张台账页面永远读不到的表里，而且部门绑定还会被记成"已用该模版"——
     * 事后想对齐只能手工搬数据。所以插入前先 fail-closed：表没接入就明确拒绝。
     * 读路径接入后，把这个常量换成"按模版解析 tb_name"即可（另需定跨表编号唯一口径）。
     */
    private static final String IMPORT_TARGET_TABLE = "t_contract";

    /*
     * data_scope 档位：定义已收敛到 DataScope 枚举 —— 编号即包含序的说明、库值/入参解析、
     * 「不小于」比较都在那里。本类不再保留 int 常量：档位一旦散成字面量，新增一档时编译器不会
     * 提醒你漏了哪个分支；换成枚举后 expandScope 的 switch 是穷尽的，漏掉档位直接编译不过。
     * 前端「只能分配不高于自身档位」的收窄下拉按编号判断（只是体验层，真正的拦截在后端的集合包含校验）。
     */
    /**
     * 当前登录用户解析（account -> 实时用户 + 角色，带 TTL 缓存）。
     * token 里只剩 account，凡是要判 data_scope / username / 停用状态的地方都走它，
     * 保证「分档判定」与「权限执法」同源，也让 admin 对账号的改动在一个 TTL 内全站生效。
     */
    private final CurUserService curUserService;
    private final TCGHTUserDao userDao;
    private final TCGHTRoleDao roleDao;
    private final TCGHTPermDao permDao;
    private final TCGHTContractDao contractDao;
    /**
     * 组织架构只读 DAO：queryOptions 走 train 库（@DS 打在方法上），其余方法走 main 库
     */
    private final TDeptDao deptDao;
    /**
     * 合同模版登记表（cght.t_contract_template）—— 只读，模版由建表脚本/运维登记
     */
    private final TCGHTContractTemplateDao tplDao;
    /**
     * 部门-合同模版映射（cght.t_dept_contract_template）—— dept_code 主键，
     * 一个部门只能有一套模版，由数据库唯一约束硬保证
     */
    private final TDeptContractTemplateDao deptTplDao;

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
        // 只告诉前端「是不是初始密码」，不下发明文密码（pwd 上有 @JsonIgnore，本来就到不了前端）
        user.setInitPwd(DEFAULT_INIT_PWD.equals(user.getPwd()));

        // 登录成功，创建JWT令牌。
        // token 里**只放 account**：username / role / perms / data_scope 全部由 TokenInterceptor
        // 按 account 现查 t_user（CurUserService，带 TTL 缓存）。这是「admin 改了我的信息、
        // 本地 token 不及时」的根治办法：
        //   - 改姓名/角色/权限/数据范围 -> 最长 TTL 内全站生效，不需要用户重新登录；
        //   - 停用/删除账号 -> 下一个请求即被拒（原来能靠旧 token 撑到 RT 过期，最长 4h）；
        //   - 顺带消掉了明文密码进 payload 的问题：hutool 的 JWT.addPayloads 不认 @JsonIgnore
        //     （实测 5.8.16），原来 claims.put("user", user) 会把 pwd 原样写进 JWT。
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtUtil.ACCOUNT_CLAIM, account);

        JwtUtil.setResponseHeader(response, claims);

        // 响应体照旧返回完整 user：前端登录后要立刻渲染菜单/头像/数据范围，
        // 没必要为这几个字段再多等一次 /account/me 往返。它只是展示数据，不参与鉴权。
        HashMap<String, Object> body = new HashMap<>();
        body.put("user", user);
        return Result.success(body);
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
     * 当前登录用户的实时快照 —— 前端唯一的数据来源，用来回填本地 ACCOUNT 缓存。
     * <p>
     * 为什么需要它：token 里只有 account，而前端要渲染菜单、头像、数据范围下拉。
     * 这里返回的结构**刻意与登录响应里的 user 完全一致**（含嵌套 role.perms），
     * 于是前端那几处读 ECacheType.ACCOUNT 的代码一行都不用改。
     * <p>
     * 刻意不加 @Transactional：纯读接口。
     * 刻意只读 @Acc：它已经是 TokenInterceptor 按 account 现查出来的实时账号行，再查一次库没有意义。
     */
    @PostMapping(value = "/account/me")
    public Result<?> accountMe(@Acc TCGHTUser curUser) {
        if (curUser == null || !StringUtils.hasText(curUser.getAccount())) {
            return Result.fail(ResultCode.RC10301);
        }
        // 只补一个派生字段（pwd 本身有 @JsonIgnore，不会下发）。
        // 注意 curUser 是 CurUserService 缓存里的实例，这里给它写一个由 pwd 派生的布尔值是幂等的，
        // 不改变任何鉴权判据；其它响应里的账号行都是各自 query 出来的独立对象。
        curUser.setInitPwd(DEFAULT_INIT_PWD.equals(curUser.getPwd()));
        return Result.success(curUser);
    }

    /**
     * 账号列表：服务端分页 + 关键字/部门筛选。
     * <p>
     * 关键字 kw 同时匹配 account / username（模糊）；dept_code 按**组织树节点**语义筛选 ——
     * 选中某部门即含其整棵子树（点「金泰化学本部」看得到其下各部门的账号），见 deptFilterOf。
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
        // curUser 是 TokenInterceptor 按 account 现查出来的实时账号行（不是 JWT 快照），
        // 所以这里读档位等于读库：admin 改了数据范围，不需要用户重登就按新档位收窄。
        var account = dataScopeOf(curUser) == DataScope.SELF ? curUser.getAccount() : null;
        var ls = userDao.queryAll(kw, deptFilterOf(dept_code, resolveScopeDepts(curUser)), true, false, account);
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
     * <p>
     * ⚠️ 本接口额外要求操作者自己至少具备「本部门」档（minScope = DataScope.DEPT）：
     * 账号写入是**管理他人**的操作，而「本人」档的账号可见面只有自己 ——
     * 即使角色里带着 perm:assign 也不该能建号/改号，否则「数据范围」这一层等于没有。
     * 范围不足由 TokenInterceptor 直接拒（RC10307），下面各道闸都来不及看。
     */
    @RequirePermission(value = "perm:assign", minScope = DataScope.DEPT)
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
        // 合法档位只有 1/2/3/4。DataScope.of 把「空串 / 不是数字 / 是数字但不在合法集合内」统一归成
        // NONE，所以这里一次判定就够，不必分三种情况报不同文案 —— 对调用方它们是同一件事。
        DataScope scopeValue = null;
        if (scopeProvided) {
            scopeValue = DataScope.of(data_scope);
            if (!scopeValue.isValid()) {
                return Result.fail(ResultCode.RC10101.getCode(), "数据范围取值必须为 1/2/3/4");
            }
        }
        // (5) 不能给出「超出自己可管理范围」的数据范围。
        //     判据是精确集合包含（新范围的展开 ⊆ 我的可见部门），比数值比大小严谨。
        //     只在新账号、或范围真的发生变化时校验：编辑既有账号而范围原样不动时不该被拦，
        //     否则低权限操作者连"改个姓名"都做不了（那一行的范围值本来就超出他）。
        final boolean scopeChanged = old == null
                || scopeValue != DataScope.of(old.getData_scope());
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
            userDao.updateScope(account, scopeValue.getCode());
            saved.setData_scope(scopeValue.getCode());
        }
        saved.setRole(roleDao.query(role_code));
        // 姓名 / 角色 / 归属部门 / 数据范围 / 密码都可能变了，缓存必须失效
        evictUserAfterCommit(account);
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
        // 重置成初始密码后 initPwd 会变 true，缓存不失效前端就不弹「请修改初始密码」
        evictUserAfterCommit(account);
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
        // 改密后 initPwd 变 false，缓存不失效前端会一直挂着「初始密码」提醒
        evictUserAfterCommit(account);
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
        // 业务约束：不允许停用当前登录账号自身，仍由前端 v-notSelf 先行拦截（后端未做二次兜底，见 REF 遗留项）
        userDao.toggleStatus(account, next);
        // 停用要立刻生效：清掉缓存后，下一个请求 byAccount 直接返回 null，前端被收回登录页
        evictUserAfterCommit(account);
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
    // 合同模版（t_contract_template / t_dept_contract_template）
    // 一套模版 = 一套合同字段结构，由 t_contract_template.tb_name 指向一张真实物理表；
    // 部门与模版是一对一（t_dept_contract_template.dept_code 主键），绑定关系在本页调整。
    // 模版本体由建表脚本/运维登记，接口层只读不写 —— 页面能做的只有「给部门指定哪一套」。
    // ================================================================

    /**
     * 模版全量列表（id / name / tb_name）。
     * <p>
     * 刻意不加 @RequirePermission：台账页与批量导入页都要靠它渲染「合同模版」下拉，
     * 而这是系统配置（模版名 + 物理表名），不含任何合同数据 —— 凡是能进那两个页面的人都该看得到。
     * 真正的写入口（deptTpl/save|delete）另有 perm:assign + 范围闸兜底。
     */
    @Transactional
    @PostMapping(value = "/template/list")
    public Result<?> templateList() {
        var ls = tplDao.queryAll();
        var out = new ArrayList<Map<String, Object>>();
        for (TCGHTContractTemplate t : ls) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("tb_name", t.getTb_name());
            // 把"这模版现在能不能导"提前交给前端：否则用户选完模版、挑完部门、传完文件，
            // 才在最后一步被拒 —— 那一趟解析白跑，提示也来得太晚。
            m.put("importable", IMPORT_TARGET_TABLE.equalsIgnoreCase(t.getTb_name()));
            out.add(m);
        }
        return Result.success(out);
    }

    /**
     * 部门 -> 模版 绑定列表（只读），供「部门合同模板」页渲染组织树徽标与右侧表单。
     * <p>
     * 只回传**当前操作者可见范围内**的部门绑定：范围受限的操作者连这些部门都看不到，
     * 回传全量等于把别的公司的模版配置暴露给他 —— 读同样不能越权。
     * 三态口径与合同/账号列表一致：scopeDepts == null（全集团档）才返回全量。
     */
    @RequirePermission("perm:assign")
    @Transactional
    @PostMapping(value = "/deptTpl/list")
    public Result<?> deptTplList(@Acc TCGHTUser curUser) {
        var ls = deptTplDao.queryAll();
        var scopeDepts = resolveScopeDepts(curUser);
        if (scopeDepts == null) {
            return Result.success(ls);
        }
        Set<String> visible = new HashSet<>(scopeDepts);
        var out = new ArrayList<TDeptContractTemplate>();
        for (TDeptContractTemplate b : ls) {
            if (b != null && visible.contains(b.getDept_code())) {
                out.add(b);
            }
        }
        return Result.success(out);
    }

    /**
     * 设置某部门的合同模版（一个部门一条记录，重复设置即覆盖）。
     * <p>
     * 三道闸缺一不可：
     * (1) 权限点 perm:assign —— 「能不能做这件事」；
     * (2) minScope = DEPT —— 「本人」档的可见面只有自己，任何一个部门都比它大，不配调整部门配置；
     * (3) inScope 范围校验 —— 「能不能管这个部门」，只判部门存在是不够的
     *     （deptExists 只回答"存在吗"，不回答"归你管吗"，两者必须分开）。
     */
    @RequirePermission(value = "perm:assign", minScope = DataScope.DEPT)
    @Transactional
    @PostMapping(value = "/deptTpl/save")
    public Result<?> deptTplSave(@RequestParam(name = "dept_code", required = false) String dept_code,
                                 @RequestParam(name = "tpl_id", required = false) Integer tpl_id,
                                 @Acc TCGHTUser curUser) {
        if (!StringUtils.hasText(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), "归属部门(dept_code)不能为空");
        }
        if (tpl_id == null) {
            return Result.fail(ResultCode.RC10101.getCode(), "合同模版(tpl_id)不能为空");
        }
        if (!deptExists(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("部门 %s 不存在或已停用，请重新选择", dept_code));
        }
        // 先判模版存在：否则 FK 违例会以 500 抛出去，用户看到的是一句无法理解的系统异常
        if (tplDao.exist(tpl_id) <= 0) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("合同模版 %s 不存在", tpl_id));
        }
        if (!inScope(resolveScopeDepts(curUser), dept_code)) {
            return Result.fail(ResultCode.RC10307.getCode(),
                    String.format("无权调整部门 %s 的合同模版（超出你的数据范围）", dept_code));
        }
        deptTplDao.save(dept_code, tpl_id, accountOf(curUser));
        return Result.success(deptTplDao.query(dept_code));
    }

    /**
     * 解除某部门的合同模版绑定（回到「未绑定」，而不是改成"空模版"）。
     * <p>
     * 闸门与 save 完全一致 —— 能设置就要能解除，否则管理员遇到配错的绑定只能找运维删库。
     * 未绑定时报"未绑定"而不是静默成功：这是页面上点出来的操作，给一句可读的话比幂等更好用。
     */
    @RequirePermission(value = "perm:assign", minScope = DataScope.DEPT)
    @Transactional
    @PostMapping(value = "/deptTpl/delete")
    public Result<?> deptTplDelete(@RequestParam(name = "dept_code", required = false) String dept_code,
                                   @Acc TCGHTUser curUser) {
        if (!StringUtils.hasText(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), "归属部门(dept_code)不能为空");
        }
        if (!deptExists(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), String.format("部门 %s 不存在或已停用，请重新选择", dept_code));
        }
        if (!inScope(resolveScopeDepts(curUser), dept_code)) {
            return Result.fail(ResultCode.RC10307.getCode(),
                    String.format("无权调整部门 %s 的合同模版（超出你的数据范围）", dept_code));
        }
        if (deptTplDao.query(dept_code) == null) {
            return Result.fail(ResultCode.RC10103.getCode(), String.format("部门 %s 未绑定合同模版", dept_code));
        }
        deptTplDao.delete(dept_code);
        return Result.success();
    }

    /**
     * 某个部门「实际生效」的合同模版（只读），供批量导入页在上传前核对绑定关系。
     * <p>
     * 为什么不复用 deptTpl/list：那个接口要 perm:assign（它是配置管理页的入口），
     * 而导入页的用户持有的是 contract:import —— 两者的人不一定重合。
     * 让导入的人因为"看不到模版配置"而被迫无脑上传，正是要避免的情形。
     * 这里只回答"这一个部门用哪套模版"，且必须落在操作者自己的数据范围内（读同样不越权）。
     * <p>
     * data 为 null = 该部门及其所有上级都没有绑定，导入页据此提示"本次上传将建立绑定"。
     */
    @Transactional
    @PostMapping(value = "/deptTpl/effective")
    public Result<?> deptTplEffective(@RequestParam(name = "dept_code", required = false) String dept_code,
                                      @Acc TCGHTUser curUser) {
        if (!StringUtils.hasText(dept_code)) {
            return Result.fail(ResultCode.RC10101.getCode(), "归属部门(dept_code)不能为空");
        }
        if (!inScope(resolveScopeDepts(curUser), dept_code)) {
            return Result.fail(ResultCode.RC10307.getCode(),
                    String.format("部门 %s 超出你的数据范围", dept_code));
        }
        TDeptContractTemplate bind = effectiveDeptTpl(dept_code);
        if (bind == null) {
            return Result.success();
        }
        TCGHTContractTemplate tpl = tplDao.query(bind.getTpl_id());
        Map<String, Object> data = new HashMap<>();
        data.put("dept_code", dept_code);
        data.put("tpl_id", bind.getTpl_id());
        data.put("tpl_name", tpl == null ? tplNameOf(bind.getTpl_id()) : tpl.getName());
        // 物理表名与"能不能导"一并下发：导入页据此就能说清"这批数据写进哪张表"，
        // 不必再拉一次模版列表 —— 少一次请求，也少一份可能过期的副本。
        data.put("tb_name", tpl == null ? null : tpl.getTb_name());
        data.put("importable", tpl != null && IMPORT_TARGET_TABLE.equalsIgnoreCase(tpl.getTb_name()));
        // owner：真正挂着这条绑定的部门。「部门合同模板」页也是这个口径（自身设置 / 继承自 xx），
        // 两个页面必须给出一致的答案，否则用户会以为导入页认错了部门。
        data.put("owner_dept_code", bind.getDept_code());
        data.put("inherited", !dept_code.equals(bind.getDept_code()));
        return Result.success(data);
    }

    /**
     * 部门「实际生效」的合同模版绑定 = 自身绑定，否则沿组织树向上取最近的已绑定祖先。
     * <p>
     * 为什么是"继承"而不是"只看自己"：绑定通常只打在几个公司节点上，
     * 末端部门靠继承生效 —— 逐个绑定既不现实，组织一调整就全失效。
     * 返回 null 表示整条链上都没有绑定。
     */
    private TDeptContractTemplate effectiveDeptTpl(String deptCode) {
        if (!StringUtils.hasText(deptCode)) {
            return null;
        }
        if (this.deptParent.isEmpty()) {
            refreshDeptCache();
        }
        Set<String> seen = new HashSet<>();          // 防脏数据成环
        String cur = deptCode;
        while (StringUtils.hasText(cur) && seen.add(cur)) {
            TDeptContractTemplate bind = deptTplDao.query(cur);
            if (bind != null) {
                return bind;
            }
            String parent = this.deptParent.get(cur);
            // 与 companyOf 同一判据：父为空、或父不在启用集合里，就是已经到顶
            cur = (StringUtils.hasText(parent) && this.deptCodes.contains(parent)) ? parent : null;
        }
        return null;
    }

    /** 模版名展示口径：模版被删/被改时退化成 #id，不把 null 抛给页面 */
    private String tplNameOf(Integer tplId) {
        if (tplId == null) {
            return "-";
        }
        TCGHTContractTemplate t = tplDao.query(tplId);
        return t == null ? ("#" + tplId) : t.getName();
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
    // 档位 = 怎么展开（枚举见 DataScope），展开起点固定为账号自己的 dept_code（组织归属，与人事一致）：
    //   DataScope.SELF    -> 按 t_contract.creator 过滤（列已建，过滤尚未接入，暂降级为「本部门」）
    //   DataScope.DEPT    -> 起点整棵子树（含下级部门 / 分厂 / 中心，BFS 不写死层数）
    //   DataScope.COMPANY -> 起点向上定位到的公司节点，取其整棵子树
    //   DataScope.ALL     -> 不限制
    //
    // 返回约定（三态，调用方必须区分）：
    //   null   → 不做范围限制（data_scope=4 全集团）
    //   空列表 → 无任何可见部门（拿不到账号、账号已停用、无归属部门、未知档位）—— fail-closed
    //   非空   → 仅这些 dept_code 可见
    private List<String> resolveScopeDepts(TCGHTUser curUser) {
        if (curUser == null || !StringUtils.hasText(curUser.getAccount())) {
            return List.of();
        }
        // 走 CurUserService，与 TokenInterceptor 填充 @Acc 用的是同一份缓存、同一份实现。
        // 传进来的 curUser 本来就是它查出来的，这里再取一次是为了保证「哪怕调用方拿的是
        // 别处拼出来的对象，范围解析也仍然以库为准」，代价只是一次 map 命中。
        TCGHTUser db = curUserService.byAccount(curUser.getAccount());
        if (db == null) {
            // 账号不存在 / 已停用都被 byAccount 归成 null —— fail-closed，返回空集合
            return List.of();
        }
        return expandScope(dataScopeOf(db), db.getDept_code(), db.getAccount());
    }

    /**
     * 让用户缓存失效，但推迟到**事务提交之后**。
     * <p>
     * 为什么不能在方法体里直接 evict：这些都是 @Transactional 写接口。提交前清缓存会留一个窗口 ——
     * 另一个线程紧接着回源（byAccount -> reload），读到的还是本事务未提交的旧值，然后把它缓存一个 TTL，
     * 等于把「改完立即生效」又打回去了。注册 afterCommit 才能保证：缓存失效发生在新数据可见之后。
     */
    private void evictUserAfterCommit(String account) {
        if (!StringUtils.hasText(account)) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    curUserService.evict(account);
                }
            });
            return;
        }
        curUserService.evict(account);
    }

    /**
     * 当前登录账号 —— 只取身份标识 account，专供写 creator（录入人）用。
     * 刻意不读 @Acc 里的 data_scope：那个字段反序列化失败会静默变 0（原因见 resolveScopeDepts 上方说明）。
     */
    private String accountOf(TCGHTUser curUser) {
        return curUser == null ? null : curUser.getAccount();
    }

    /**
     * 把「档位」展开成可见部门集合 —— 全模块唯一的范围展开实现。
     * resolveScopeDepts（执法）与 accountSave 的「不许分配高于自身的范围」（判定）都走它，
     * 保证"判定范围"与"执法范围"永远同源。
     * <p>
     * ⚠️ 用 switch 表达式穷尽全部档位、<strong>不写 default</strong>：新增一档时这里编译不过，
     * 逼着人把"这一档展开成什么"想清楚。档位是范围的唯一口径，静默落进兜底分支就是静默越权。
     *
     * @param scope      档位；null 或 {@link DataScope#NONE} 一律按无可见数据兜底
     * @param deptCode   展开起点部门（账号归属部门）
     * @param logAccount 仅用于日志
     * @return 三态：null = 不限制 / 空列表 = 无可见部门(fail-closed) / 非空 = 白名单
     */
    private List<String> expandScope(DataScope scope, String deptCode, String logAccount) {
        return switch (scope == null ? DataScope.NONE : scope) {
            // 4 全集团：返回 null（"不限制"），调用方必须与"空集合"区别对待 —— 见方法上方三态说明。
            case ALL -> null;
            // 未知档位 / 字段缺失：fail-closed，一个部门都不给。
            case NONE -> {
                log.warn("账号 {} 的 data_scope={} 不是已知档位，按无可见数据兜底", logAccount,
                        scope == null ? null : scope.getCode());
                yield List.of();
            }
            // 3 本公司：以「起点向上定位到的公司节点」为根，展开整棵子树。
            //   公司节点 = 集团根（没有父、或父不在启用集合里的那个节点）的直接子。
            //   例：1030015006（本部/采购供应部）-> 公司 1030015（金泰化学本部）-> 其下 14 个部门。
            case COMPANY -> {
                if (!StringUtils.hasText(deptCode)) {
                    log.warn("账号 {} 的 data_scope=3(本公司) 但没有归属部门，按无可见数据兜底", logAccount);
                    yield List.of();
                }
                Set<String> sub = subtreeOf(companyOf(deptCode));
                if (sub.isEmpty()) {
                    log.warn("账号 {} 本公司范围展开为空(dept_code={})，按归属部门收敛", logAccount, deptCode);
                    yield List.of(deptCode);
                }
                yield List.copyOf(sub);
            }
            // 2 本部门：起点自身 + 全部递归下级（BFS，不写死层数）。
            //   部门天然包含下级 —— 这正是不再单设「本部门及下级」一档的原因：
            //   同一个 dept_code 下两者都展开成 subtreeOf(dept_code)，完全重合，留两档只会让人以为有得选。
            //   注意它不是「本公司」的别名 —— 起点=生产技术部时本档 12 个，本公司档是整公司 24 个。
            // 1 本人：依据是 t_contract.creator（录入人）。该列已于 2026-09-16 建立，但接入过滤要换一个
            //        维度下推（creator = ? 而不是 dept_code in (...)），且历史数据 creator 为空 ——
            //        接入前先降级为「本部门」而不是放行，比原档位更严格，不会造成越权。见 PRD §4.5.7。
            case SELF, DEPT -> {
                if (scope == DataScope.SELF) {
                    log.warn("账号 {} 的 data_scope=1(本人) 尚未接入 creator 过滤，暂按本部门收敛", logAccount);
                }
                if (!StringUtils.hasText(deptCode)) {
                    log.warn("账号 {} 的 data_scope={} 但没有归属部门，按无可见数据兜底", logAccount, scope.getCode());
                    yield List.of();
                }
                Set<String> deptTree = subtreeOf(deptCode);
                yield deptTree.isEmpty() ? List.<String>of() : List.copyOf(deptTree);
            }
        };
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
     * 取不到（账号不存在 / 该列为空 / 未知编号）统一得到 {@link DataScope#NONE}：
     * NONE 不是合法档位 → expandScope 返回空集合 → fail-closed。
     * 写入侧已把该列设为 NOT NULL 且新建必填，所以 NONE 只可能出现在脏数据上，按最严处理。
     */
    private DataScope dataScopeOf(TCGHTUser db) {
        return db == null ? DataScope.NONE : DataScope.of(db.getData_scope());
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
     * 合并「部门筛选」与「数据范围白名单」→ 最终可见部门集，供 SQL 单一 in 下推。
     * <p>
     * 为什么筛选部门必须先展开子树：筛选栏/组织树给出的 dept_code 是**组织树节点**
     * （如「金泰化学本部」1030015），语义天然含下级；而账号行与合同行的 dept_code 存的是
     * **最小归属单元**（叶子部门，如「采供部」1030015006）。直接用 dept_code = ? 精确匹配，
     * 点父节点必然 0 行 —— 用户看到的是"这个部门没有数据"，而不是"筛选条件太严"。
     * <p>
     * 为什么与可见范围求交、并成一条 in 而不是两条 in AND：
     * 求交后语义唯一（最终可见集）—— 报表与注释都不必再解释"两个 in 是什么关系"；
     * 且手输超出可见范围的 dept_code 时交集为空，与 resolveScopeDepts 的 fail-closed 一致，
     * 不会因为"筛选"反而放大可见范围。
     *
     * @param deptCode   筛选部门（组织树节点）；空白 = 不筛部门，原样透传 scopeDepts
     * @param scopeDepts 可见部门白名单三态：null 不限 / 空 = 无可见 / 非空 = 仅这些
     * @return 三态同 scopeDepts：null = 不限制 / 空 = 查不到（XML 走 1=0）/ 非空 = in 这些部门
     */
    private List<String> deptFilterOf(String deptCode, List<String> scopeDepts) {
        if (!StringUtils.hasText(deptCode)) {
            return scopeDepts;
        }
        Set<String> sub = subtreeOf(deptCode);
        if (scopeDepts == null) {
            return new ArrayList<>(sub);
        }
        Set<String> visible = new HashSet<>(scopeDepts);
        List<String> out = new ArrayList<>();
        for (String d : sub) {
            if (visible.contains(d)) {
                out.add(d);
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
        // 筛选栏的 dept_code 先按组织树展开成子树，再与可见范围求交（deptFilterOf，与账号列表同一口径）：
        // 受限用户筛了范围外的部门，交集为空 → 结果为空，而不是越权。
        var scopeDepts = resolveScopeDepts(curUser);
        // 1 本人档：只放行 sign_person 等于我姓名的合同。
        // curUser 是现查的实时账号行，改了姓名这里立刻跟上，不会拿登录时的旧姓名去比对。
        var username = dataScopeOf(curUser) == DataScope.SELF ? curUser.getUsername() : null;
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = contractDao.queryAll(id, title, sign_person, sign_type, payment_type, supplier, queryBegin, queryEnd, finish_step, rkBegin, rkEnd, warn_day, deptFilterOf(dept_code, scopeDepts), username);
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
        // 录入人按登录态写入（覆盖请求体里的任何值）：客户端不可伪造
        c.setCreator(accountOf(curUser));
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
        // creator（录入人）不在拷贝范围：审计字段不随表单改，否则可伪造归属
        BeanUtils.copyProperties(c, old, "unique_id", "open_status", "creator");
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
    public Result<?> contractImport(@RequestParam(name = "tpl_id", required = false) Integer tpl_id,
                                    @RequestBody List<TCGHTContract> rows, @Acc TCGHTUser curUser) {
        if (rows == null || rows.isEmpty()) {
            return Result.fail(ResultCode.RC10101.getCode(), "导入数据为空");
        }
        // 模版必须在处理任何一行之前就确定：它既是"这批数据属于哪套模版"的声明，
        // 也是下面比对部门绑定关系的基准。没有它，导入就是无据可依地往一张表里灌数据 ——
        // 这正是"无脑导入"的根子，所以在入口一次拦掉，不留到逐行去猜。
        TCGHTContractTemplate tpl = tpl_id == null ? null : tplDao.query(tpl_id);
        if (tpl == null) {
            return Result.fail(ResultCode.RC10101.getCode(),
                    "合同模版未确定或已不存在，请确认所选部门已在「部门合同模板」页配置了有效的模板");
        }
        // 模版存在还不够：它的物理表必须是导入链路真正写的那张。
        // 宁可在这里整批拒绝，也不能"选 A 表却把数据写进 B 表"——那是最难发现的一类错。
        if (!IMPORT_TARGET_TABLE.equalsIgnoreCase(tpl.getTb_name())) {
            return Result.fail(ResultCode.RC10101.getCode(),
                    String.format("模版「%s」对应的物理表 %s 尚未接入导入链路（当前仅支持 %s），请联系管理员",
                            tpl.getName(), tpl.getTb_name(), IMPORT_TARGET_TABLE));
        }
        String tplName = tpl.getName();
        int okCnt = 0;
        List<Map<String, Object>> failRows = new ArrayList<>();
        // 批次内即时结算类 id 去重
        Set<String> batchInstantIds = new HashSet<>();
        // 数据范围与录入人在整批里解析一次即可：同一登录账号，批次内不会变
        List<String> scopeDepts = resolveScopeDepts(curUser);
        String creator = accountOf(curUser);
        // 部门 -> 生效绑定：整批里每个部门只解析一次（同一批通常只有一个部门，但不做这个假设）。
        // 值为 null 是合法结果（该部门整条链都未绑定），所以判"是否查过"要用 containsKey。
        Map<String, TDeptContractTemplate> bindOfDept = new HashMap<>();
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
            String deptCode = c.getDept_code();
            if (!StringUtils.hasText(deptCode)) {
                reasons.add("归属部门必填");
            } else if (!deptExists(deptCode)) {
                reasons.add(String.format("归属部门 %s 不存在或已停用", deptCode));
            } else if (!inScope(scopeDepts, deptCode)) {
                reasons.add(String.format("归属部门 %s 超出你的数据范围", deptCode));
            } else {
                // 部门确定了，才能核对它与合同模版的绑定关系。
                // 一个部门只能有一套模版：已绑定别的模版就必须拦下，
                // 否则等于借一次导入悄悄把这个部门的模版换掉（历史合同可就不认了）。
                TDeptContractTemplate bind = bindOfDept.get(deptCode);
                if (!bindOfDept.containsKey(deptCode)) {
                    bind = effectiveDeptTpl(deptCode);
                    bindOfDept.put(deptCode, bind);
                }
                if (bind == null) {
                    // 部门没配模板 = 这批数据没有合法的落脚点。以前这里会"顺手"把本次模版绑上去，
                    // 但那等于借一次导入改掉部门的配置口径；现在统一要求先去配置页设定，
                    // 导入链路上不再有"隐式建绑定"这回事。
                    reasons.add(String.format("归属部门 %s 未配置合同模板，请先到「部门合同模板」页为该部门指定模板",
                            deptCode));
                } else if (!tpl_id.equals(bind.getTpl_id())) {
                    reasons.add(String.format("归属部门 %s 已绑定合同模版「%s」，与本次选择的模版「%s」不一致",
                            deptCode, tplNameOf(bind.getTpl_id()), tplName));
                }
            }
            if (c.getDate_sign() == null) {
                reasons.add("签订时间必填");
            }
            if (reasons.isEmpty()) {
                c.setUnique_id(UUID.randomUUID().toString().replace("-", ""));
                c.setCreator(creator);   // 导入的合同同样记「谁导的」
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
        // 部门与模版的关系只在「部门合同模板」页建立：导入链路不再写绑定，
        // 未配置就是整行拦截（见上方 bind == null 分支）。
        Map<String, Object> data = new HashMap<>();
        data.put("success", okCnt);
        data.put("fail", failRows.size());
        data.put("failRows", failRows);
        data.put("tpl_id", tpl_id);
        data.put("tpl_name", tplName);
        return Result.success(data);
    }
}
