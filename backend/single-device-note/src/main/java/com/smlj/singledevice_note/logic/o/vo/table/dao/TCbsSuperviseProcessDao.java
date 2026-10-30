package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSuperviseProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsSuperviseProcessDao {

    TCbsSuperviseProcess query(@Param("id") String id);

    /** 分页查询（kw 模糊匹配，见 XML cond 片段） */
    List<TCbsSuperviseProcess> queryPage(@Param("kw") String kw,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw);

    int insert(@Param("p") TCbsSuperviseProcess p);

    int update(@Param("p") TCbsSuperviseProcess p);

    int remove(@Param("id") String id);

    /** 批量软删（ids 已在控制层去空去重） */
    int removeBatch(@Param("ids") List<String> ids);
}
