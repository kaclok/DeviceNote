package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProjectAccept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsProjectAcceptDao {

    TCbsProjectAccept query(@Param("id") String id);

    /** projectId 空=全部；联 t_project 带出 proj_name */
    List<TCbsProjectAccept> queryPage(@Param("projectId") String projectId,
                                      @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("projectId") String projectId);
    int insert(@Param("p") TCbsProjectAccept p);

    int update(@Param("p") TCbsProjectAccept p);

    int remove(@Param("id") String id);
}
