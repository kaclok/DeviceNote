package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsProjectDao {

    TCbsProject query(@Param("id") String id);

    /** kw 模糊 proj_name；contractorId 空=全部；联 t_contractor 带出 contractor_name */
    List<TCbsProject> queryAll(@Param("kw") String kw, @Param("contractorId") String contractorId);

    /** 承包商删除前的守卫：在挂项目数 */
    int countByContractor(@Param("contractorId") String contractorId);

    int insert(@Param("p") TCbsProject p);

    int update(@Param("p") TCbsProject p);

    int remove(@Param("id") String id);
}
