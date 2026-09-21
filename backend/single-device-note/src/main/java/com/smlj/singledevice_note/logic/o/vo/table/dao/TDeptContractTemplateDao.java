package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeptContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

/**
 * 部门-合同模版映射表（cght.t_dept_contract_template）。
 * <p>
 * dept_code 是主键，所以「设置」用 upsert 一条语句完成 —— 不需要先查后写，
 * 也就不存在并发下两个请求同时插入同一部门的风险（唯一约束会直接挡掉第二个）。
 */
@Mapper
@Repository
public interface TDeptContractTemplateDao {
    ArrayList<TDeptContractTemplate> queryAll();

    TDeptContractTemplate query(@Param("dept_code") String dept_code);

    /**
     * 设置部门模版：已存在则覆盖（一个部门一条记录，唯一约束由主键保证）
     */
    Integer save(@Param("dept_code") String dept_code,
                 @Param("tpl_id") Integer tpl_id,
                 @Param("bound_by") String bound_by);

    /**
     * 解除绑定：删除该部门的记录，该部门回到「未绑定」状态
     */
    Integer delete(@Param("dept_code") String dept_code);
}
