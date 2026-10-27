package com.smlj.singledevice_note.gb.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.gb.o.GbVehicleWeigh;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * jt_dev 过磅系统查询（10.8.54.84:3306/jt_dev，wlcc 数据源）。
 * ⚠️ 类级 @DS 切库：调用链上不可包 @Transactional（事务内切库失效，同 CCGHT 坑）。
 */
@DS("wlcc")
@Mapper
@Repository
public interface JtWeighDao {
    /**
     * 按车牌查最近过磅记录（含关联的送货/拉货单据号与状态），最多 size 条。
     * exact=true 走 plate_no 精确匹配（idx_plate_time 索引，毫秒级）；
     * exact=false 走 like '%x%' 模糊兜底（无索引可走，全表扫 ~0.3s）。
     */
    List<GbVehicleWeigh> selectVehicleWeighs(@Param("vehicleNo") String vehicleNo,
                                             @Param("size") int size,
                                             @Param("exact") boolean exact);
}
