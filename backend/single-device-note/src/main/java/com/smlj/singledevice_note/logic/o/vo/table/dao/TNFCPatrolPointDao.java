package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TNFCPatrolPointDao {
    int insert(@Param("item") TNFCPatrolPoint item);

    int delete(@Param("rfid") String rfid);

    int deleteFromLine(@Param("rfid") String rfid);

    int exist(@Param("id") String id);

    int updateBase(@Param("item") TNFCPatrolPoint item);

    int updateLineId(@Param("rfid") String rfid, @Param("lineid") Integer lineid);

    ArrayList<TNFCPatrolPoint> queryAll(@Param("queryByNum") String queryByNum,
                                        @Param("queryByName") String queryByName,
                                        @Param("queryByRfId") String queryByRfId);

    ArrayList<TNFCPatrolPoint> queryUnused(@Param("queryByNum") String queryByNum,
                                        @Param("queryByName") String queryByName,
                                        @Param("queryByRfId") String queryByRfId);

    TNFCPatrolPoint queryById(@Param("id") String id);
}
