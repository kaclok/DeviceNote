package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TCbsContractorDao {

    TCbsContractor query(@Param("id") String id);

    /**
     * kw 模糊 name/uscc；status=审批状态(0待审/1通过/2驳回)，coop=合作状态(1/2/3)，
     * recycled=true 只看回收站（软删行）。
     */
    List<TCbsContractor> queryPage(@Param("kw") String kw, @Param("status") Integer status,
                                   @Param("coop") Integer coop, @Param("recycled") boolean recycled,
                                   @Param("ctype") String ctype,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw, @Param("status") Integer status,
                  @Param("coop") Integer coop, @Param("recycled") boolean recycled,
                  @Param("ctype") String ctype);

    /** 下拉专用：只出启用行 */
    List<TCbsContractor> queryOptions();
    /** 启用行内 uscc 查重（excludeId 编辑态排除自身） */
    int countUsccDup(@Param("uscc") String uscc, @Param("excludeId") String excludeId);

    int insert(@Param("c") TCbsContractor c);

    int update(@Param("c") TCbsContractor c);

    /** 审核流转：status 1 通过 / 2 驳回 */
    int audit(@Param("id") String id, @Param("status") int status, @Param("reason") String reason);

    /** 软删（进回收站） */
    int remove(@Param("id") String id);

    /** 回收站恢复 */
    int restore(@Param("id") String id);

    /** 彻底删除（物理删） */
    int purge(@Param("id") String id);

    /** 保证金余额增减（delta 正=增加 负=扣款），与变更记录同事务 */
    int changeDeposit(@Param("id") String id, @Param("delta") BigDecimal delta);
}
