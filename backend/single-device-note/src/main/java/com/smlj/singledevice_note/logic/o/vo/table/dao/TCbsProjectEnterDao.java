package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProjectEnter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsProjectEnterDao {

    TCbsProjectEnter query(@Param("id") String id);

    /** projectId 空=全部；联 t_project 带出 proj_name */
    List<TCbsProjectEnter> queryAll(@Param("projectId") String projectId);

    int insert(@Param("p") TCbsProjectEnter p);

    int update(@Param("p") TCbsProjectEnter p);

    int remove(@Param("id") String id);
}
