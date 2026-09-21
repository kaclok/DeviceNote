package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 合同模版：一条记录 = 一套合同模版，它的字段结构由 tb_name 指向的物理表承载。
 * <p>
 * 数据源 cght.t_contract_template。新增一套模版 = 建一张 t_contract_N 物理表 + 在这里登记一行，
 * 台账页与批量导入页的「合同模版」下拉读的就是这张表。
 */
@Component
@Data
@NoArgsConstructor
public class TCGHTContractTemplate implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    /** 模版ID（主键），与 tb_name 后缀一致，如 id=1 -> t_contract_1 */
    private Integer id;

    /** 模版名称（展示用文字描述） */
    private String name;

    /** 该模版引用的合同物理表名（cght schema 下），如 t_contract / t_contract_1 */
    private String tb_name;
}
