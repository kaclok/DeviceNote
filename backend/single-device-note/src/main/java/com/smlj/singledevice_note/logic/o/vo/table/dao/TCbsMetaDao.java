package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsMeta;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsMetaDao {

    List<TCbsMeta> queryPage(@Param("table") String table, @Param("typeId") String typeId,
                             @Param("fileType") Integer fileType,
                             @Param("kw") String kw,
                             @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("table") String table, @Param("typeId") String typeId,
                  @Param("fileType") Integer fileType,
                  @Param("kw") String kw);

    TCbsMeta query(@Param("table") String table, @Param("id") String id);

    int insert(@Param("table") String table, @Param("p") TCbsMeta p);

    int update(@Param("table") String table, @Param("p") TCbsMeta p);

    /** 软删 */
    int remove(@Param("table") String table, @Param("id") String id);

    /** 物理删 */
    int purge(@Param("table") String table, @Param("id") String id);
}
