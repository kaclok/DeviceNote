package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 通用基础数据行（cbs.t_prep_condition 开工准备文件 / cbs.t_doc_list 资料清单）。
 * 主字段 name + remark；t_doc_list 额外按 file_type 区分「项目条件 / 开工前安全条件」两个分页签。
 * table 由控制器白名单限定，fail-closed。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsMeta implements Serializable {

    private String id;
    /** 所属项目类型（t_dict.item_id, dict_type=project_type） */
    private String type_id;
    /** 仅 t_doc_list 有意义：1=项目条件 2=开工前安全条件；t_prep_condition 恒为 null（该表无此列） */
    private Integer file_type;
    private String name;
    private String remark;
    private String creator;
    private Boolean open_status;
}
