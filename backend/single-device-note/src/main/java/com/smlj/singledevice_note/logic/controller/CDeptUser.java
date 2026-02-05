package com.smlj.singledevice_note.logic.controller;

import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.utils.PageUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeptUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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

    // 将t_org表的ids_direct_sub_depts和ids_recursive_sub_depts进行计算填充
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
                subDeptCodes = org.getIds_direct_sub_depts();
            } else {
                subDeptCodes = org.getIds_recursive_sub_depts();
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
                    var ls = dept.getIds_direct_sub_depts();
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
                    var ls = dept.getIds_recursive_sub_depts();
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
}
