package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsLicenseChem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsLicenseChemDao {

    TCbsLicenseChem query(@Param("id") String id);

    List<TCbsLicenseChem> queryAll(@Param("kw") String kw);

    int insert(@Param("c") TCbsLicenseChem c);

    int update(@Param("c") TCbsLicenseChem c);

    int remove(@Param("id") String id);
}
