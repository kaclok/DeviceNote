package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsThreeSame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsThreeSameDao {

    TCbsThreeSame query(@Param("id") String id);

    List<TCbsThreeSame> queryAll(@Param("kw") String kw);

    int insert(@Param("t") TCbsThreeSame t);

    int update(@Param("t") TCbsThreeSame t);

    int remove(@Param("id") String id);
}
