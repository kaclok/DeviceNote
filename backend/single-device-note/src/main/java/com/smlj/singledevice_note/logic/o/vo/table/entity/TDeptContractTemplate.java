package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门 -> 合同模版 映射：每个部门使用的是哪套合同模版。
 * <p>
 * 数据源 cght.t_dept_contract_template，dept_code 是主键 ——
 * 「一个部门只能有一套合同模版」由数据库唯一约束硬保证，不靠前端拦。
 * 一个部门是否出现在这张表里，是「已绑定 / 未绑定」的唯一判据。
 */
@Component
@Data
@NoArgsConstructor
public class TDeptContractTemplate implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    /** 部门编码（主键），关联 train.t_org.dept_code */
    private String dept_code;

    /** 使用的合同模版ID，关联 cght.t_contract_template.id */
    private Integer tpl_id;

    /** 绑定操作人账号（最后一次设置的人） */
    private String bound_by;

    /** 绑定时间（最后一次设置的时间） */
    private Date bound_at;
}
