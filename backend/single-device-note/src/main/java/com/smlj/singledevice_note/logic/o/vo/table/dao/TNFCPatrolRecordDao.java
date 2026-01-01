package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolPoint;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

@Mapper
@Repository
public interface TNFCPatrolRecordDao {
    int insert(@Param("item") TNFCPatrolRecord item);

    ArrayList<TNFCPatrolRecord> queryAll(@Param("queryByDept") String queryByDept,
                                         @Param("queryByPerson") String queryByPerson,
                                         @Param("queryBegin") Date queryBegin,
                                         @Param("queryEnd") Date queryEnd);
}
