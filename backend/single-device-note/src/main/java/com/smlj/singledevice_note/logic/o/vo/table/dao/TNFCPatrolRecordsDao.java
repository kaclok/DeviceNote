package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.logic_entity.nfcpatrol.VNFCPatrolLineRecord;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolRecord;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolRecords;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Mapper
@Repository
public interface TNFCPatrolRecordsDao {
    int insert(@Param("item") TNFCPatrolRecords item);

    ArrayList<TNFCPatrolRecords> queryAll(@Param("queryByPerson") String queryByPerson,
                                         @Param("rfid") String rfid,
                                         @Param("queryBegin") String queryBegin,
                                         @Param("queryEnd") String queryEnd);

    ArrayList<VNFCPatrolLineRecord> querySeries(@Param("zzIds") List<String> zzIds,
                                                @Param("queryByDeptIdArray") List<String> queryByDeptIdArray,
                                                @Param("queryByStatus") Integer queryByStatus,
                                                @Param("queryBegin") String queryBegin,
                                                @Param("queryEnd") String queryEnd);

    int querySeriesCount(@Param("zzIds") List<String> zzIds,
                         @Param("queryByDeptIdArray") List<String> queryByDeptIdArray,
                         @Param("queryByStatus") Integer queryByStatus,
                         @Param("queryBegin") String queryBegin,
                         @Param("queryEnd") String queryEnd);

    int queryPointRecordCntOfLine(@Param("lineid") Integer lineid,
                                  @Param("queryBegin") String queryBegin,
                                  @Param("queryEnd") String queryEnd);
}
