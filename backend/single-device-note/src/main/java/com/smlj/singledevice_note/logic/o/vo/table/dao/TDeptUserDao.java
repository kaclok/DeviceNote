package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TDeptUserDao {
    Integer isIdExist(@Param("id") String id);

    TDeptUser queryById(@Param("id") String id);

    ArrayList<TDeptUser> queryByIds(@Param("ids") ArrayList<String> ids);

    ArrayList<TDeptUser> queryByDept(@Param("dept") String dept);

    ArrayList<TDeptUser> queryByDepts(@Param("deptCodes") ArrayList<String> deptCodes);

    ArrayList<TDeptUser> queryByAccount(@Param("account") String account);

    ArrayList<TDeptUser> queryByAccounts(@Param("accounts") ArrayList<String> accounts);
}
