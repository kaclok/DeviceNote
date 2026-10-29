package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSpotcheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsSpotcheckDao {

    TCbsSpotcheck query(@Param("id") String id);

    /** kw 模糊 spot_content/checker；contractorId 空=全部；workType 空=全部 */
    List<TCbsSpotcheck> queryAll(@Param("kw") String kw, @Param("contractorId") String contractorId,
                                 @Param("workType") Integer workType);

    int insert(@Param("p") TCbsSpotcheck p);

    int update(@Param("p") TCbsSpotcheck p);

    int remove(@Param("id") String id);
}
