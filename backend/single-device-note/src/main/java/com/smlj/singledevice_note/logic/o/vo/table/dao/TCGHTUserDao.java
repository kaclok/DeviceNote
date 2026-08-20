package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCGHTUserDao {
    ArrayList<TCGHTUser> queryAll(boolean includeAdmin);

    TCGHTUser query(@Param("account") String account);

    int exist(@Param("account") String account);
}
