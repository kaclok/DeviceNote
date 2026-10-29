package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsEnterprise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TCbsEnterpriseDao {

    /** 单行表：id 恒为 1 */
    TCbsEnterprise get();

    int insert(@Param("e") TCbsEnterprise e);

    /** update_time/update_by 在 SQL 内写死 now() / #{me} */
    int update(@Param("e") TCbsEnterprise e, @Param("me") String me);
}
