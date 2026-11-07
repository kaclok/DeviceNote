package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 承包商 × 人员 关联（cbs.t_contractor_person）。
 * 一个人可服务多个承包商，故不在 t_person 上挂 contractor_id，而用本表表达多对多；
 * audit_status / auditor / audit_time 留审核痕迹；是否在场是本表属性（同一人可在一家在场、另一家离场）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractorPerson implements Serializable {

    private String id;
    private String contractor_id;
    private String person_id;
    /** 审核状态：0=未审核 1=已审核（加入即 0，批量审核置 1） */
    private Integer audit_status;
    /** 是否在场：1=在场 2=已离场 */
    private Integer on_site;
    private String auditor;
    private String remark;
    private String creator;
    private Boolean open_status;
}
