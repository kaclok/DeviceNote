package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsQualCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsQualCheckDao {

    /**
     * 某承包商的资质审核清单：以 t_contractor_qual 为准 left join 审核结果，
     * 故未审核过的项也会出现（check_status=0、id 为空）。
     *
     * @param typeId 承包商 ctype 对应的 t_dict.item_id
     */
    List<TCbsQualCheck> listByContractor(@Param("contractorId") String contractorId,
                                         @Param("typeId") String typeId,
                                         @Param("qualType") Integer qualType);

    /** 单条审核结果（未软删） */
    TCbsQualCheck query(@Param("contractorId") String contractorId,
                        @Param("qualId") String qualId);

    int insert(@Param("p") TCbsQualCheck p);

    int update(@Param("p") TCbsQualCheck p);
}
