package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPerson;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsPersonDao {

    /** 某承包商已加入（审核通过）的人员，join 关联表带出 rel_id / audit_* */
    List<TCbsPerson> queryByContractor(@Param("contractorId") String contractorId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    long countByContractor(@Param("contractorId") String contractorId);

    /** 人员库：排除已加入指定承包商的人（excludeContractorId 为空则不排除） */
    List<TCbsPerson> queryLibrary(@Param("kw") String kw,
                                  @Param("excludeContractorId") String excludeContractorId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    long countLibrary(@Param("kw") String kw,
                      @Param("excludeContractorId") String excludeContractorId);

    TCbsPerson query(@Param("id") String id);

    /** 身份证查重（启用行）；编辑态排除自身 */
    int countIdCard(@Param("idCard") String idCard, @Param("excludeId") String excludeId);

    int insert(@Param("p") TCbsPerson p);

    int update(@Param("p") TCbsPerson p);

    int remove(@Param("id") String id);
}
