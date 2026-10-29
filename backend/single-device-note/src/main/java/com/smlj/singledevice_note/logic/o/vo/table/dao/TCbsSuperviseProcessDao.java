package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSuperviseProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsSuperviseProcessDao {

    TCbsSuperviseProcess query(@Param("id") String id);

    List<TCbsSuperviseProcess> queryAll(@Param("kw") String kw);

    int insert(@Param("p") TCbsSuperviseProcess p);

    int update(@Param("p") TCbsSuperviseProcess p);

    int remove(@Param("id") String id);
}
