package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsUserDao {

    TCbsUser query(@Param("account") String account);

    List<TCbsUser> queryPage(@Param("kw") String kw,
                             @Param("userType") Integer userType,
                             @Param("filterUsing") boolean filterUsing,
                             @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw,
                  @Param("userType") Integer userType,
                  @Param("filterUsing") boolean filterUsing);

    Integer exist(@Param("account") String account);

    int insert(@Param("u") TCbsUser u);

    int update(@Param("u") TCbsUser u);

    int resetPwd(@Param("account") String account, @Param("pwd") String pwd);

    int toggleStatus(@Param("account") String account, @Param("is_using") boolean is_using);

    /** 某部门在编账号数（部门删除/停用前的守卫） */
    int countByDept(@Param("deptCode") String deptCode);
}
