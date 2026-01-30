package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TOrg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TOrgDao {
    public String[] querySubDepts(@Param("deptCode") String deptCode);

    public ArrayList<TOrg> queryAll();

    TOrg queryById(@Param("deptCode") String deptCode);

    int updateDirect(@Param("deptCode") String deptCode, @Param("subs") String[] subs);

    int updateRecursive(@Param("deptCode") String deptCode, @Param("subs") String[] subs);
}
