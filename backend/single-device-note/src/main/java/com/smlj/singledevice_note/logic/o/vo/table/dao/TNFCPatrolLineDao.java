package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolLine;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TNFCPatrolLineDao {
    int insert(@Param("item") TNFCPatrolLine item);

    int delete(@Param("id") int id);

    int updateBase(@Param("item") TNFCPatrolLine item);

    int updatePoints(@Param("id") int id, @Param("pointIds") ArrayList<String> pointIds);

    ArrayList<TNFCPatrolLine> queryAll(@Param("queryByNum") String queryByNum,
                                       @Param("queryByName") String queryByName,
                                       @Param("queryByDeptId") String queryByDeptId);

    ArrayList<TNFCPatrolPoint> queryPointsByLine(@Param("id") int id);

    ArrayList<TNFCPatrolPoint> queryRelativedPoints(@Param("id") int id);

    TNFCPatrolLine queryById(@Param("id") int id);
}
