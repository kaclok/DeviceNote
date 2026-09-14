package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TDeptDao {
    /**
     * 组织架构全量字典（只读）。
     * <p>
     * 数据源 = train.t_org（MySQL，集团组织树权威主数据，116 个启用部门）。
     * main 库的 dept_user.t_dept 只是本地副本（仅 8 行，组织树严重不全），不能作为展示来源。
     * <p>
     * 为什么 @DS 只能打在方法上、不能提到接口上：
     * 同接口的 queryAll / querySubDepts / updateDirect / updateRecursive 走默认库 main，
     * 它们要回写 PostgreSQL 数组列 ids_direct_sub_depts / ids_recursive_sub_depts（MySQL 侧无这些列），
     * 由 CDeptUser.warpOrgs / CLogin 链路使用。两条链路无法合并，因此按方法切数据源。
     */
    @DS("train")
    ArrayList<TDept> queryOptions();

    String[] querySubDepts(@Param("deptCode") String deptCode);

    ArrayList<TDept> queryAll();

    TDept queryById(@Param("deptCode") String deptCode);

    Integer updateDirect(@Param("deptCode") String deptCode, @Param("subs") String[] subs);

    Integer updateRecursive(@Param("deptCode") String deptCode, @Param("subs") String[] subs);
}
