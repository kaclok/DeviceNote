package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TOrgDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TOrgUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TOrg;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "COrgUser", description = "组织架构相关操作")
public class COrgUser {
    private final TOrgDao orgDao;
    private final TOrgUserDao orgUserDao;

    // 将t_org表的ids_direct_sub_depts和ids_recursive_sub_depts进行计算填充
    @Transactional
    @PostMapping("/warpOrgs")
    public Result<?> warpOrgs() {
        var allDepts = orgDao.queryAll();
        for (TOrg dept : allDepts) {
            _warpOrgs(dept.getDept_code());
        }
        return Result.success();
    }

    private void _warpOrgs(String deptCode) {
        var directSubDeptCodes = orgDao.querySubDepts(deptCode);
        orgDao.updateDirect(deptCode, directSubDeptCodes);

        var recursiveSubDeptCodes = _queryOrgs(deptCode, new HashSet<>());
        orgDao.updateRecursive(deptCode, recursiveSubDeptCodes);
    }

    private String[] _queryOrgs(String deptCode, HashSet<String> set) {
        if (set.contains(deptCode)) {
            return null;
        }
        set.add(deptCode);

        List<String> r = new ArrayList<>();
        var subs = orgDao.querySubDepts(deptCode);
        for (var subDeptCode : subs) {
            r.add(subDeptCode);

            var children = _queryOrgs(subDeptCode, set);
            if (children != null && children.length > 0) {
                Collections.addAll(r, children);
            }
        }

        return r.toArray(new String[0]);
    }

    // 获取某个部门的子部门信息
    @Transactional
    @PostMapping("/getSubDepts")
    public Result<?> getSubDepts(
            @RequestParam(name = "deptCode", required = false, defaultValue = "1030") String deptCode,
            @RequestParam(name = "recursiveOrDirect", required = false, defaultValue = "0") boolean recursiveOrDirect) {
        ArrayList<TOrg> subs = _getSubDepts(deptCode, recursiveOrDirect);
        return Result.success(subs);
    }

    private ArrayList<TOrg> _getSubDepts(String deptCode, boolean recursiveOrDirect) {
        var org = orgDao.queryById(deptCode);
        ArrayList<TOrg> subs = new ArrayList<>();
        if (org != null) {
            String[] subDeptCodes = null;
            if (!recursiveOrDirect) {
                subDeptCodes = org.getIds_direct_sub_depts();
            } else {
                subDeptCodes = org.getIds_recursive_sub_depts();
            }

            if (subDeptCodes != null) {
                for (var subDeptCode : subDeptCodes) {
                    var subOrg = orgDao.queryById(subDeptCode);
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
    public Result<?> getUsers(String deptCode,
                              @RequestParam(name = "deptCode", required = false) int includeSubType,
                              @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                              @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        return null;
    }
}
