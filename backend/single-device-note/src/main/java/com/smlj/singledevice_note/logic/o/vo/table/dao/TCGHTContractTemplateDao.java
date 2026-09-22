package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContractTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

/**
 * 合同模版表（cght.t_contract_template）—— 只读 + 一处写入。
 * <p>
 * 模版本体（id / name / tb_name）是系统配置（由建表脚本 / 运维登记），页面只选不造，
 * 所以这里没有 insert，也没有对那三列的 update；唯一的写入口是 updateColOrder ——
 * 它改的是"Excel 列顺序"这一份展示口径，不动模版本身。
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

    /**
     * 保存模版下的 Excel 列顺序（逗号分隔字段名）。
     * <p>
     * 传 null = 清除覆盖、回到 gd.json 的登记顺序。字段名的合法性由调用方（CCGHT）先对着
     * 该模版指向的物理表核对过，本方法只负责落库。
     */
    Integer updateColOrder(@Param("id") Integer id, @Param("col_order") String colOrder);
}
