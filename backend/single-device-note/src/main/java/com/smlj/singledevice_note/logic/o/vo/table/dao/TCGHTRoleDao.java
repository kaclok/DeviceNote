package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCGHTRoleDao {
    ArrayList<TCGHTRole> queryAll();

    TCGHTRole query(@Param("role_code") String role_code);

    int exist(@Param("role_code") String role_code);
}
