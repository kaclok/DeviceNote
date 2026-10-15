package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.DataScope;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.PageUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptUser;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CDeptUser", description = "组织架构相关操作")
public class CDeptUser {
    private final TDeptDao deptDao;
    private final TDeptUserDao deptUserDao;
    private final TRoleDao roleDao;

    // 将t_dept表的 direct_children / recursive_children 辅助列计算回写
    @Transactional
    @PostMapping("/warpOrgs")
    public Result<?> warpOrgs() {
        var allDepts = deptDao.queryAll();
        for (TDept dept : allDepts) {
            _warpOrgs(dept.getDept_code());
        }
        return Result.success();
    }

    private void _warpOrgs(String deptCode) {
        var directSubDeptCodes = deptDao.querySubDepts(deptCode);
        deptDao.updateDirect(deptCode, directSubDeptCodes);

        var recursiveSubDeptCodes = _queryOrgs(deptCode, new HashSet<>());
        deptDao.updateRecursive(deptCode, recursiveSubDeptCodes);
    }

    private String[] _queryOrgs(String deptCode, HashSet<String> set) {
        if (set.contains(deptCode)) {
            return null;
        }
        set.add(deptCode);

        List<String> r = new ArrayList<>();
        var subs = deptDao.querySubDepts(deptCode);
        for (var subDeptCode : subs) {
            r.add(subDeptCode);

            var children = _queryOrgs(subDeptCode, set);
            if (children != null && children.length > 0) {
                Collections.addAll(r, children);
            }
        }

        return r.toArray(new String[0]);
    }

    // 将用户填充到t_org的
    @Transactional
    @PostMapping("/warpUsersToOrgs")
    public Result<?> warpUsersToOrgs() {
        return null;
    }

    // 获取某个部门的子部门信息
    @Transactional
    @PostMapping("/getSubDepts")
    public Result<?> getSubDepts(
            @RequestParam(name = "deptCode", required = false, defaultValue = "1030") String deptCode,
            @RequestParam(name = "recursiveOrDirect", required = false, defaultValue = "0") boolean recursiveOrDirect) {
        ArrayList<TDept> subs = _getSubDepts(deptCode, recursiveOrDirect);
        return Result.success(subs);
    }

    private ArrayList<TDept> _getSubDepts(String deptCode, boolean recursiveOrDirect) {
        var org = deptDao.queryById(deptCode);
        ArrayList<TDept> subs = new ArrayList<>();
        if (org != null) {
            String[] subDeptCodes = null;
            if (!recursiveOrDirect) {
                subDeptCodes = org.getDirect_children();
            } else {
                subDeptCodes = org.getRecursive_children();
            }

            if (subDeptCodes != null) {
                for (var subDeptCode : subDeptCodes) {
                    var subOrg = deptDao.queryById(subDeptCode);
                    subs.add(subOrg);
                }
            }
        }
        return subs;
    }

    // includeSubType == 1表示只查找用户仅为deptCode部门的人员
    // includeSubType == 2表示查找用户为deptCode部门的人员，会在直接子部门查询
    // includeSubType == 3表示查找用户为deptCode部门的人员，会在递归子部门查询
    @Transactional
    @PostMapping("/getUsers")
    public Result<?> getUsers(@RequestParam(name = "deptCode", required = false, defaultValue = "1030") String deptCode,
                              @RequestParam(name = "includeSubType", required = false, defaultValue = "2") int includeSubType,
                              @RequestParam(name = "pageNum", required = false, defaultValue = "0") int pageNum,
                              @RequestParam(name = "pageSize", required = false, defaultValue = "0") int pageSize) {
        PageSerializable<TDeptUser> r = null;
        if (includeSubType == 1) {
            r = PageUtil.doPage(pageNum, pageSize, v -> {
                var depts = new ArrayList<String>(Arrays.asList(deptCode));
                var us = deptUserDao.queryByDepts(depts);
                return us;
            }, v -> {
                return v;
            });

        } else if (includeSubType == 2) {
            r = PageUtil.doPage(pageNum, pageSize, v -> {
                var depts = new ArrayList<String>(Arrays.asList(deptCode));
                var dept = deptDao.queryById(deptCode);
                if (dept != null) {
                    var ls = dept.getDirect_children();
                    if (ls != null && ls.length > 0) {
                        Collections.addAll(depts, ls);
                    }
                }

                var us = deptUserDao.queryByDepts(depts);
                return us;
            }, v -> {
                return v;
            });
        } else if (includeSubType == 3) {
            r = PageUtil.doPage(pageNum, pageSize, v -> {
                var depts = new ArrayList<String>(Arrays.asList(deptCode));
                var dept = deptDao.queryById(deptCode);
                if (dept != null) {
                    var ls = dept.getRecursive_children();
                    if (ls != null && ls.length > 0) {
                        Collections.addAll(depts, ls);
                    }
                }

                var us = deptUserDao.queryByDepts(depts);
                return us;
            }, v -> {
                return v;
            });
        }
        return Result.success(r);
    }

    // ================================================================
    // 数据范围（data_scope）
    // ================================================================
    //   范围不再挂在账号行上，而是 t_user.roles[] -> t_role.data_scope（多角色）。
    //   有效档位 = 全部启用角色档位的最大值（编号即包含序，取最大 = 取最宽，与「权限并集」惯例一致）；
    //   无角色 / 角色全停用 / 档位非法 => NONE => fail-closed，一个部门都不给。
    //   「1 本人」档的语义：可见部门就是账号自己所在部门 —— 按 account 反查 t_user.dept_code 得到，
    //   不对任何部门列表做筛选（这正是「按用户账户查找，而不是按部门列表查找」）。
    //
    // 三态返回约定（调用方必须区分）：
    //   空列表 -> 无任何可见部门（拿不到账号 / 已停用 / 无角色 / 无归属部门）—— fail-closed
    //   非空   -> 仅这些 dept_code 可见

    /**
     * 账号 + 生效档位的组合：resolveScopeDepts（拿部门）与 getScopeDepts（拿档位）共用一次反查。
     */
    private record AccountScope(TDeptUser user, DataScope scope) {
    }

    /**
     * 按 account 反查实时账号并解析生效档位。查询走 deptUserDao（dept_user.t_user，main 库），
     * 与 cghtz 的 CurUserService 不同：这里不做 TTL 缓存 —— t_user 是主键单行查询，成本可忽略，
     * 先把口径做对，真有热点再照 CurUserService 抄缓存。
     */
    private AccountScope userScopeOf(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        // queryByAccount 返回集合：t_user 的 PK 是 'account|dept_code'，同一 account 正常恰好一行；
        // 万一出现多行（脏数据）取首行，只影响展示口径，不影响 fail-closed 语义。
        var ls = deptUserDao.queryByAccount(account);
        TDeptUser user = (ls == null || ls.isEmpty()) ? null : ls.get(0);
        if (user == null) {
            // 账号不存在 / 已停用：fail-closed
            return null;
        }
        return new AccountScope(user, dataScopeOf(user));
    }

    /**
     * 账号的有效范围档位 = 全部启用角色 data_scope 的最大档。
     */
    private DataScope dataScopeOf(TDeptUser user) {
        if (user == null || user.getRoles() == null || user.getRoles().length == 0) {
            return DataScope.NONE;
        }
        DataScope max = DataScope.NONE;
        var roles = roleDao.queryByIds(new ArrayList<>(Arrays.asList(user.getRoles())));
        for (TRole r : roles) {
            if (r == null || !r.getIs_using()) {
                continue;
            }
            DataScope s = DataScope.of(r.getData_scope());
            if (s.atLeast(max)) {
                max = s;
            }
        }
        return max;
    }

    /**
     * 按账号解析可见部门（三态，口径见本段头注释）。
     * 给后续 cghtz 逻辑迁移 / 其他模块复用的统一入口：输入是 account，不是部门列表。
     */
    private List<String> resolveScopeDepts(String account) {
        var s = userScopeOf(account);
        if (s == null) {
            return List.of();
        }
        return expandScope(s.scope(), s.user().getDept_code(), s.user().getAccount());
    }

    /**
     * 把「档位」展开成可见部门集合 —— 全模块唯一的范围展开实现。
     */
    private List<String> expandScope(DataScope scope, String deptCode, String account) {
        return switch (scope == null ? DataScope.NONE : scope) {
            // 4 全集团：返回 null（"不限制"），调用方必须与"空集合"区别对待 —— 见本段头注释三态说明。
            case ALL -> null;
            // 无角色 / 角色全停用 / 档位非法：fail-closed，一个部门都不给。
            case NONE -> {
                log.warn("账号 {} 没有可用的角色档位，按无可见数据兜底", account);
                yield List.of();
            }
            // 1 本人：可见部门 = 账号自己所在部门（按 account 反查 t_user.dept_code，不做列表筛选）。
            //        部门缺失（脏数据）时不猜、不放行，按无可见数据兜底。
            case SELF -> {
                if (!StringUtils.hasText(deptCode)) {
                    yield List.of();
                }
                yield List.of(deptCode);
            }
            // 2 本部门：起点自身 + 全部递归下级（BFS，不写死层数）。
            //   部门天然包含下级，所以不存在单独的「本部门及下级」档。
            case DEPT -> {
                if (!StringUtils.hasText(deptCode)) {
                    log.warn("账号 {} 档位为本部门但没有归属部门，按无可见数据兜底", account);
                    yield List.of();
                }
                Set<String> deptTree = subtreeOf(deptCode);
                yield deptTree.isEmpty() ? List.<String>of() : List.copyOf(deptTree);
            }
            // 3 本公司：以「起点向上定位到的公司节点」为根，展开整棵子树。
            //   公司节点 = 集团根（没有父、或父不在启用集合里的那个节点）的直接子。
            case COMPANY -> {
                if (!StringUtils.hasText(deptCode)) {
                    log.warn("账号 {} 档位为本公司但没有归属部门，按无可见数据兜底", account);
                    yield List.of();
                }
                Set<String> sub = subtreeOf(companyOf(deptCode));
                if (sub.isEmpty()) {
                    log.warn("账号 {} 本公司范围展开为空(dept_code={})，按归属部门收敛", account, deptCode);
                    yield List.of(deptCode);
                }
                yield List.copyOf(sub);
            }
        };
    }

    // ---- 数据范围的组织树读取：无缓存，直读 dept_user.t_dept 辅助列 ----
    // 递归子树取 recursive_children、公司定位读 is_company（数组列由 /x/warpOrgs 回写，is_company 人工维护）。
    // 辅助列未回写的节点，subtreeOf 退化为「仅自身」—— 只收窄不放大。

    /**
     * 获取账号可见部门列表（按 account 反查，不走登录态 —— /x/** 不在 TokenInterceptor 范围内，
     * 与本 controller 现有接口同一信任模型；后续若收紧认证，把 RegistryOf 的 addPathPatterns 扩进来即可）。
     * <p>
     * 返回 {account, dept_code, scope, unlimited, depts}。
     */
    @PostMapping("/getScopeDepts")
    public Result<?> getScopeDepts(@RequestParam(name = "account") String account) {
        var s = userScopeOf(account);
        if (s.user() == null) {
            // 与 cghtz 口径一致：账号不存在/停用按不存在处理，不暴露存在性
            return Result.fail(ResultCode.RC10301);
        }
        List<String> depts = expandScope(s.scope(), s.user().getDept_code(), s.user().getAccount());
        Map<String, Object> view = new HashMap<>();
        view.put("account", s.user().getAccount());
        view.put("dept_code", s.user().getDept_code());
        view.put("scope", s.scope().getCode());
        view.put("unlimited", s.scope() == DataScope.ALL);
        view.put("depts", depts == null ? List.of() : depts);
        return Result.success(view);
    }

    /**
     * 某部门的整棵子树（含自身）：直读 recursive_children 辅助列。
     * 查无此部门 / 辅助列为空时返回仅自身 —— 只收窄不放大。null/空白入参返回空集。
     */
    private Set<String> subtreeOf(String deptCode) {
        Set<String> out = new HashSet<>();
        if (!StringUtils.hasText(deptCode)) {
            return out;
        }
        // 加上自身
        out.add(deptCode);
        TDept org = deptDao.queryById(deptCode);
        if (org != null && org.getRecursive_children() != null) {
            for (String c : org.getRecursive_children()) {
                if (StringUtils.hasText(c)) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /**
     * 账号所属部门「向上定位到的公司节点」：沿 parent_dept_code 向上，
     * 返回第一个 is_company=true 的节点（集团根 1030 也标了 true ⇒ COMPANY == 全集团）。
     * 定位不到（链断/没有公司标记）时返回入参自身 —— 语义退化为「本部门」，只会收窄范围，不会放大。
     */
    private String companyOf(String deptCode) {
        if (!StringUtils.hasText(deptCode)) {
            return null;
        }
        String cur = deptCode;
        for (int i = 0; i < 16; i++) {          // 防环上限
            TDept dept = deptDao.queryById(cur);
            if (dept == null) {
                break;                           // 表里查不到：无法继续向上
            }
            if (dept.getIs_company()) {
                return cur;                      // 公司节点（含 root：根的公司=自己 ⇒ COMPANY == 全集团）
            }
            String p = dept.getParent_dept_code();
            if (!StringUtils.hasText(p)) {
                break;                           // 无父可走
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
     * 防止越权（不信任前端传递的dept参数），求交集：合并「部门筛选」与「数据范围白名单」-> 最终可见部门集，供 SQL 单一 in 下推
     */
    private List<String> deptFilterOf(String deptCode, List<String> scopeDepts) {
        if (!StringUtils.hasText(deptCode)) {
            return scopeDepts;
        }
        Set<String> sub = subtreeOf(deptCode);
        if (scopeDepts == null) {
            return new ArrayList<>(sub);
        }
        // retainAll 前先转 Set：HashSet.retainAll(list) 逐个 list.contains 会退化 O(n*m)
        sub.retainAll(new HashSet<>(scopeDepts));
        return new ArrayList<>(sub);
    }
}
