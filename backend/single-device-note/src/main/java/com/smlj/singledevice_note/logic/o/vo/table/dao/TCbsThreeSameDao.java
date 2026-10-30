package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsThreeSame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsThreeSameDao {

    TCbsThreeSame query(@Param("id") String id);

    /** 分页查询（kw 模糊匹配，见 XML cond 片段） */
    List<TCbsThreeSame> queryPage(@Param("kw") String kw,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw);

    int insert(@Param("t") TCbsThreeSame t);

    int update(@Param("t") TCbsThreeSame t);

    int remove(@Param("id") String id);
}
