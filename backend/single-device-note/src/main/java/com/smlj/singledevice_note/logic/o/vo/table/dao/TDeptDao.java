package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TDeptDao {
    String[] querySubDepts(@Param("deptCode") String deptCode);

    ArrayList<TDept> queryAll();

    TDept queryById(@Param("deptCode") String deptCode);

    Integer updateDirect(@Param("deptCode") String deptCode, @Param("subs") String[] subs);

    Integer updateRecursive(@Param("deptCode") String deptCode, @Param("subs") String[] subs);
}
