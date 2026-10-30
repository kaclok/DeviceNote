package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsCert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsCertDao {

    TCbsCert query(@Param("id") String id);

    /** kw 模糊 cert_name/cert_no；按到期日升序（最近到期在前） */
    /** 分页查询（kw 模糊匹配，见 XML cond 片段） */
    List<TCbsCert> queryPage(@Param("kw") String kw,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw);

    int insert(@Param("c") TCbsCert c);

    int update(@Param("c") TCbsCert c);

    int remove(@Param("id") String id);
}
