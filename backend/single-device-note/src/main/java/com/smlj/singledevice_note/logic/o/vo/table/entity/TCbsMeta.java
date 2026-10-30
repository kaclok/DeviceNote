package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 通用基础数据行（cbs.t_prep_condition 开工准备条件 / cbs.t_doc_list 资料清单）。
 * 仅 name + remark 两类字段，table 由控制器白名单限定，fail-closed。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsMeta implements Serializable {

    private String id;
    private String name;
    private String remark;
    private String creator;
    private Boolean open_status;
}
