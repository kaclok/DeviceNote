package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorChange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsContractorChangeDao {

    /** 按承包商分页，变更时间倒序 */
    List<TCbsContractorChange> queryPage(@Param("contractorId") String contractorId,
                                         @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("contractorId") String contractorId);

    int insert(@Param("p") TCbsContractorChange p);
}
