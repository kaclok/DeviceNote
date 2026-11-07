package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorPerson;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TCbsContractorPersonDao {

    /** 关联行（含已软删），批量审核靠它做幂等：已存在的恢复、没建过的插入 */
    TCbsContractorPerson queryAny(@Param("contractorId") String contractorId,
                                  @Param("personId") String personId);

    int insert(@Param("p") TCbsContractorPerson p);

    /** 恢复软删行并刷新审核痕迹 */
    int restore(@Param("id") String id, @Param("auditor") String auditor);

    /** 移出该承包商人员库（软删） */
    int remove(@Param("id") String id);
}
