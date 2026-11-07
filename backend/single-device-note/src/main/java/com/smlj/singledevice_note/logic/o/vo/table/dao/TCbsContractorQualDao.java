package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorQual;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsContractorQualDao {

    /** 某承包商类型下、某一分类（1=资质 2=审核所需文件）的全部记录（未软删） */
    List<TCbsContractorQual> listByType(@Param("typeId") String typeId,
                                        @Param("qualType") Integer qualType);

    int insert(@Param("p") TCbsContractorQual p);

    int update(@Param("p") TCbsContractorQual p);

    /** 启用/停用切换 */
    int toggleUsing(@Param("id") String id, @Param("using") Boolean using);

    int delete(@Param("id") String id);
}
