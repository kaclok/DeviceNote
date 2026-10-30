package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSpotcheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsSpotcheckDao {

    TCbsSpotcheck query(@Param("id") String id);

    /**
     * 列表分页。筛选口径与目标站在线抽查一致：
     * 报备人员 / 作业票类型 / 报备状态 / 报备日期区间 / 作业时间区间。
     */
    List<TCbsSpotcheck> queryPage(@Param("reporter") String reporter, @Param("workType") Integer workType,
                                  @Param("status") Integer status,
                                  @Param("reportStart") String reportStart, @Param("reportEnd") String reportEnd,
                                  @Param("beginStart") String beginStart, @Param("beginEnd") String beginEnd,
                                  @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("reporter") String reporter, @Param("workType") Integer workType,
                  @Param("status") Integer status,
                  @Param("reportStart") String reportStart, @Param("reportEnd") String reportEnd,
                  @Param("beginStart") String beginStart, @Param("beginEnd") String beginEnd);

    /** 导出：与列表同条件、不分页（行数上限由控制器控制） */
    List<TCbsSpotcheck> queryAll(@Param("reporter") String reporter, @Param("workType") Integer workType,
                                 @Param("status") Integer status,
                                 @Param("reportStart") String reportStart, @Param("reportEnd") String reportEnd,
                                 @Param("beginStart") String beginStart, @Param("beginEnd") String beginEnd);

    int insert(@Param("p") TCbsSpotcheck p);

    int update(@Param("p") TCbsSpotcheck p);

    /** 作废：status = 3 */
    int invalidate(@Param("id") String id);

    /** 物理删除（列表「删除」按钮，每行都有） */
    int delete(@Param("id") String id);
}
