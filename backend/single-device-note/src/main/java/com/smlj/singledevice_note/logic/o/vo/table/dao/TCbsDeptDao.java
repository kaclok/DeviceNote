package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsDeptDao {

    TCbsDept queryById(@Param("deptCode") String deptCode);

    List<TCbsDept> queryAll();

    /** 直系子部门 code 列表（按 parent_dept_code 现算，供 warpOrgs 回写辅助列） */
    List<String> querySubDepts(@Param("deptCode") String deptCode);

    int insert(@Param("d") TCbsDept d);

    int update(@Param("d") TCbsDept d);

    int remove(@Param("deptCode") String deptCode);

    /** 辅助列回写：空数组落 NULL（口径与树表脚本一致，叶子不得留 '{}'） */
    int updateDirect(@Param("deptCode") String deptCode, @Param("subs") String[] subs);

    int updateRecursive(@Param("deptCode") String deptCode, @Param("subs") String[] subs);
}
