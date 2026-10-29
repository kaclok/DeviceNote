package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsRoleDao {

    TCbsRole query(@Param("roleCode") String roleCode);

    List<TCbsRole> queryAll();

    int insert(@Param("r") TCbsRole r);

    int update(@Param("r") TCbsRole r);

    int toggleStatus(@Param("roleCode") String roleCode, @Param("is_using") boolean is_using);

    /** 引用该角色的账号数（角色删除/停用前的守卫） */
    int countByRole(@Param("roleCode") String roleCode);
}
