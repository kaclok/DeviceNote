package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTPerm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCGHTPermDao {
    ArrayList<TCGHTPerm> queryAll();

    TCGHTPerm query(@Param("perm_code") String perm_code);

    int exist(@Param("perm_code") String perm_code);
}
