package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TNFCPatrolZzDao {
    ArrayList<TNFCPatrolDept> queryAll();

    TNFCPatrolDept query(@Param("id") String id);
}
