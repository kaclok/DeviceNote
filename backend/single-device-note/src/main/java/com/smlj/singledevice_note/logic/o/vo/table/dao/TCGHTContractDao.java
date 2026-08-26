package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;

@Mapper
@Repository
public interface TCGHTContractDao {
    /**
     * 列表查询（带筛选 + date_sign 日期区间 + date_rk 挂账日期区间）。
     * 筛选参数空值视为不筛选。queryBegin/queryEnd 对应 date_sign 范围，rkBegin/rkEnd 对应 date_rk 范围。
     * date_rk 可能为 null：填了挂账区间时，null 的记录自然不命中范围条件，符合"按挂账日期筛选"语义。
     */
    ArrayList<TCGHTContract> queryAll(
            @Param("id") String id
            , @Param("title") String title
            , @Param("sign_person") String sign_person
            , @Param("sign_type") Integer sign_type
            , @Param("supplier") String supplier
            , @Param("queryBegin") Date queryBegin
            , @Param("queryEnd") Date queryEnd
            , @Param("finish_step") Integer finish_step
            , @Param("has_rk") Boolean has_rk
            , @Param("rkBegin") Date rkBegin
            , @Param("rkEnd") Date rkEnd);

    TCGHTContract query(@Param("id") String id);

    int exist(@Param("id") String id);

    int insert(@Param("c") TCGHTContract c);

    int update(@Param("c") TCGHTContract c);

    /**
     * 逻辑作废：将 open_status 置 false。不物理删除。
     */
    int markInvalid(@Param("id") String id);

    /** 重新启用：将 open_status 置 true（复用作废合同时使用） */
    int reactivate(@Param("id") String id);
}
