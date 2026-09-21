package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

/**
 * 合同模版表（cght.t_contract_template）—— 只读。
 * <p>
 * 模版是系统配置（由建表脚本 / 运维登记），页面只选不造，所以这里没有 insert / update。
 */
@Mapper
@Repository
public interface TCGHTContractTemplateDao {
    /**
     * 模版全量列表（按 id 升序，保证下拉顺序稳定）
     */
    ArrayList<TCGHTContractTemplate> queryAll();

    TCGHTContractTemplate query(@Param("id") Integer id);

    /**
     * 模版是否存在（写入部门绑定时校验 tpl_id，避免 FK 报错变成 500）
     */
    Integer exist(@Param("id") Integer id);
}
