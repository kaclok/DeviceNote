package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorPunish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsContractorPunishDao {

    /** 按承包商分页，联查处罚类型名 / 项目名 */
    List<TCbsContractorPunish> queryPage(@Param("contractorId") String contractorId,
                                         @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("contractorId") String contractorId);

    int insert(@Param("p") TCbsContractorPunish p);
}
