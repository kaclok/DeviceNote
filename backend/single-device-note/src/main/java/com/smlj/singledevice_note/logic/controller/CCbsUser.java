package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.PwdUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import com.smlj.singledevice_note.logic.service.CurCbsUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.smlj.singledevice_note.logic.util.CbsPage.offset;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageNo;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageOf;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * cbs 组织管理（部门 / 角色 / 人员），独立账号体系的维护入口。
 * <p>
 * 人员 = 我司内部员工(user_type=1) + 承包商人员(user_type=2，必挂 contractor_id)，
 * 同一张 cbs.t_user，按类型筛选展示 —— 原系统"人员管理含承包商人员"的口径。
 * <p>
 * 范围解析（expandScope 等）在业务控制器（M2）落地，本类只管组织数据的维护与回写。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cbs")
@Tag(name = "CCbsUser", description = "cbs组织管理")
public class CCbsUser {
    private final TCbsDeptDao deptDao;
    private final TCbsRoleDao roleDao;
    private final TCbsUserDao userDao;
    private final CurCbsUserService curCbsUserService;

    // ---------------- 部门 ----------------

    @RequirePermission("cbs:org")
    @PostMapping("/dept/list")
    public Result<?> deptList() {
        return Result.success(deptDao.queryAll());
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/dept/save")
    public Result<?> deptSave(@RequestBody TCbsDept d) {
        if (!StringUtils.hasText(d.getDept_code()) || !StringUtils.hasText(d.getDept_name())) {
            return Result.fail(ResultCode.RC10101, "dept_code/dept_name 不能为空");
        }
        // 父部门必须真实存在（根节点 parent 留空）
        if (StringUtils.hasText(d.getParent_dept_code()) && deptDao.queryById(d.getParent_dept_code()) == null) {
            return Result.fail(ResultCode.RC10103, "父部门不存在");
        }
        if (d.getDept_code().equals(d.getParent_dept_code())) {
            return Result.fail(ResultCode.RC10101, "父部门不能是自身");
        }
        if (d.getIs_company() == null) {
            d.setIs_company(false);
        }
        if (deptDao.queryById(d.getDept_code()) == null) {
            d.setIs_using(true);
            deptDao.insert(d);
        } else {
            deptDao.update(d);
        }
        return Result.success();
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/dept/delete")
    public Result<?> deptDelete(@RequestParam(name = "deptCode") String deptCode) {
        if (!StringUtils.hasText(deptCode)) {
            return Result.fail(ResultCode.RC10101, "deptCode 不能为空");
        }
        // 守卫：有直系子部门 / 有在编人员 的一律拒删（软删后读取链路自动屏蔽，但语义上不允许断链）
        if (!deptDao.querySubDepts(deptCode).isEmpty()) {
            return Result.fail(ResultCode.RC10101, "存在子部门，不能删除");
        }
        if (userDao.countByDept(deptCode) > 0) {
            return Result.fail(ResultCode.RC10101, "部门下存在在编人员，不能删除");
        }
        deptDao.remove(deptCode);
        return Result.success();
    }

    // ---------------- 角色 ----------------

    @RequirePermission("cbs:org")
    @PostMapping("/role/list")
    public Result<?> roleList() {
        return Result.success(roleDao.queryAll());
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/role/save")
    public Result<?> roleSave(@RequestBody TCbsRole r) {
        if (!StringUtils.hasText(r.getRole_code()) || !StringUtils.hasText(r.getRole_name())) {
            return Result.fail(ResultCode.RC10101, "role_code/role_name 不能为空");
        }
        if (!com.smlj.singledevice_note.core.o.to.DataScope.of(r.getData_scope()).isValid()) {
            return Result.fail(ResultCode.RC10101, "data_scope 必须是 1~4");
        }
        boolean exists = roleDao.query(r.getRole_code()) != null;
        if (exists) {
            roleDao.update(r);
        } else {
            r.setIs_using(true);
            roleDao.insert(r);
        }
        // 角色的 perms/data_scope 变了，所有持该角色账号的缓存必须全失效
        curCbsUserService.evictAll();
        return Result.success();
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/role/delete")
    public Result<?> roleDelete(@RequestParam(name = "roleCode") String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Result.fail(ResultCode.RC10101, "roleCode 不能为空");
        }
        if (roleDao.countByRole(roleCode) > 0) {
            return Result.fail(ResultCode.RC10101, "存在使用该角色的账号，不能停用");
        }
        roleDao.toggleStatus(roleCode, false);
        curCbsUserService.evictAll();
        return Result.success();
    }

    // ---------------- 人员 ----------------

    /**
     * 人员列表：kw 模糊 account/username；userType 1=内部 2=承包商 空=全部；服务端分页。
     */
    @RequirePermission("cbs:org")
    @PostMapping("/user/list")
    public Result<?> userList(@RequestParam(name = "kw", required = false) String kw,
                              @RequestParam(name = "userType", required = false) Integer userType,
                              @RequestParam(name = "pageNo", required = false) Integer no,
                              @RequestParam(name = "pageSize", required = false) Integer size) {
        int pn = pageNo(no), ps = pageSize(size);
        var ls = userDao.queryPage(kw, userType, false, offset(pn, ps), ps);
        for (var u : ls) {
            u.setRole(roleDao.query(u.getRole_code()));
        }
        return Result.success(pageOf(ls, userDao.countAll(kw, userType, false), pn, ps));
    }

    @RequirePermission(value = "cbs:org", minScope = com.smlj.singledevice_note.core.o.to.DataScope.DEPT)
    @Transactional
    @PostMapping("/user/save")
    public Result<?> userSave(@RequestBody TCbsUser u) {
        if (!StringUtils.hasText(u.getAccount()) || !StringUtils.hasText(u.getUsername())) {
            return Result.fail(ResultCode.RC10101, "account/username 不能为空");
        }
        if (!StringUtils.hasText(u.getRole_code()) || roleDao.query(u.getRole_code()) == null) {
            return Result.fail(ResultCode.RC10101, "角色不存在");
        }
        if (!StringUtils.hasText(u.getDept_code()) || deptDao.queryById(u.getDept_code()) == null) {
            return Result.fail(ResultCode.RC10101, "归属部门不存在");
        }
        if (u.getUser_type() == null) {
            u.setUser_type(1);
        }
        if (u.getUser_type() == 2 && !StringUtils.hasText(u.getContractor_id())) {
            return Result.fail(ResultCode.RC10101, "承包商人员必须关联承包商单位");
        }
        boolean exists = userDao.exist(u.getAccount()) > 0;
        if (exists) {
            userDao.update(u);
        } else {
            // 新增：pwd 必填（前端提交或用默认初始口令），只在落库前一刻加密
            String raw = StringUtils.hasText(u.getPwd()) ? u.getPwd() : "123456";
            u.setPwd(PwdUtil.encrypt(raw));
            u.setIs_using(true);
            userDao.insert(u);
        }
        curCbsUserService.evict(u.getAccount());
        return Result.success();
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/user/resetPwd")
    public Result<?> userResetPwd(@RequestParam(name = "account") String account) {
        if (!StringUtils.hasText(account) || userDao.exist(account) == 0) {
            return Result.fail(ResultCode.RC10101, "账号不存在");
        }
        userDao.resetPwd(account, PwdUtil.encrypt("123456"));
        curCbsUserService.evict(account);
        return Result.success();
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/user/toggle")
    public Result<?> userToggle(@RequestParam(name = "account") String account,
                                @RequestParam(name = "isUsing") boolean isUsing) {
        if (!StringUtils.hasText(account) || userDao.exist(account) == 0) {
            return Result.fail(ResultCode.RC10101, "账号不存在");
        }
        userDao.toggleStatus(account, isUsing);
        curCbsUserService.evict(account);
        return Result.success();
    }

    /**
     * 当前登录用户的实时快照（cghtz account/me 同角色）：路由守卫 ensureMe 的数据源。
     * 不加权限点 —— 任何已登录账号都需要它来渲染自己的菜单/按钮，零权限账号也要能拿到自身信息。
     */
    @PostMapping("/me")
    public Result<?> me(@Acc TCbsUser curUser) {
        if (curUser == null) {
            return Result.fail(ResultCode.RC10005);
        }
        TCbsDept org = deptDao.queryById(curUser.getDept_code());
        HashMap<String, Object> r = new HashMap<>();
        r.put("account", curUser.getAccount());
        r.put("username", curUser.getUsername());
        r.put("dept_code", curUser.getDept_code());
        r.put("user_type", curUser.getUser_type());
        r.put("role", curUser.getRole());
        if (org != null) {
            r.put("dept_name", org.getDept_name());
        }
        return Result.success(r);
    }

    // ---------------- 组织树辅助列回写 ----------------

    /**
     * 部门增删改后重算 direct_children / recursive_children（口径同 dept_user 的 warpOrgs）。
     * is_company / level / node_type 不在这里回写：前者人工维护，后两者用树表脚本口径批量算。
     */
    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/warpOrgs")
    public Result<?> warpOrgs(@Acc TCbsUser curUser) {
        for (TCbsDept dept : deptDao.queryAll()) {
            String code = dept.getDept_code();
            List<String> direct = new ArrayList<>(deptDao.querySubDepts(code));
            Collections.sort(direct);
            deptDao.updateDirect(code, direct.toArray(new String[0]));

            HashSet<String> seen = new HashSet<>();
            List<String> recursive = new ArrayList<>();
            collectRecursive(code, seen, recursive);
            Collections.sort(recursive);
            deptDao.updateRecursive(code, recursive.toArray(new String[0]));
        }
        return Result.success();
    }

    /** 递归收集全部后代（含孙系）；seen 防环。 */
    private void collectRecursive(String deptCode, HashSet<String> seen, List<String> out) {
        if (!seen.add(deptCode)) {
            return;
        }
        for (String sub : deptDao.querySubDepts(deptCode)) {
            out.add(sub);
            collectRecursive(sub, seen, out);
        }
    }
}
