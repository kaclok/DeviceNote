package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

/**
 * dept_user.t_role 的只读访问（角色是低频变更的字典表，当前没有维护接口）。
 */
@Mapper
@Repository
public interface TRoleDao {
    TRole queryById(@Param("id") String id);

    /**
     * 按 id 批量查启用角色（t_user.roles[] 的反查入口）。
     * 停用角色被 where 过滤 —— 调用方（CDeptUser.dataScopeOf）按「查不到 = 不参与档位计算」处理。
     */
    ArrayList<TRole> queryByIds(@Param("ids") ArrayList<String> ids);
}
