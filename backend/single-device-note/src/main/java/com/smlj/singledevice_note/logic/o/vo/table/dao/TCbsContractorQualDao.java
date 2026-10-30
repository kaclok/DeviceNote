package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorQual;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsContractorQualDao {

    /** 某承包商类型下的全部资质（启用档） */
    List<TCbsContractorQual> listByType(@Param("typeId") String typeId);

    int insert(@Param("p") TCbsContractorQual p);

    int delete(@Param("id") String id);
}
