package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsContractorDao {

    TCbsContractor query(@Param("id") String id);

    /** kw 模糊 name/uscc；status 空=全部（0待审/1通过/2驳回） */
    List<TCbsContractor> queryAll(@Param("kw") String kw, @Param("status") Integer status);

    /** 启用行内 uscc 查重（excludeId 编辑态排除自身） */
    int countUsccDup(@Param("uscc") String uscc, @Param("excludeId") String excludeId);

    int insert(@Param("c") TCbsContractor c);

    int update(@Param("c") TCbsContractor c);

    /** 审核流转：status 1 通过 / 2 驳回 */
    int audit(@Param("id") String id, @Param("status") int status, @Param("reason") String reason);

    /** 软删 */
    int remove(@Param("id") String id);
}
