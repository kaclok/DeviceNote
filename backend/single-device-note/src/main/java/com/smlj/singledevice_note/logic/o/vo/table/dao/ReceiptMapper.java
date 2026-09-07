package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.logic.o.vo.table.entity.ReceiptInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DS("wlcc")
public interface ReceiptMapper {
    /**
     * 查询状态为130或144且remark为0的记录
     */
    public List<ReceiptInfo> selectStatus130ByStatusAndRemark();

    public List<ReceiptInfo> selectStatus144ByStatusAndRemark();

    public ReceiptInfo selectOneByStatusAndRemark(@Param("id") Long id);

    public int updateRemark(@Param("id") Long id, @Param("remark") Integer remark);

    public int updateDoorCarId(@Param("id") Long id, @Param("doorCarId") String doorCarId);
}
