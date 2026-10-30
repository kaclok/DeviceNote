package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsLicenseChem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsLicenseChemDao {

    TCbsLicenseChem query(@Param("id") String id);

    /** 分页查询（kw 模糊匹配，见 XML cond 片段） */
    List<TCbsLicenseChem> queryPage(@Param("kw") String kw,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw);

    int insert(@Param("c") TCbsLicenseChem c);

    int update(@Param("c") TCbsLicenseChem c);

    int remove(@Param("id") String id);
}
