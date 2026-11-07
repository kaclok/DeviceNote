package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 承包商 × 人员 关联（cbs.t_contractor_person）。
 * 一个人可服务多个承包商，故不在 t_person 上挂 contractor_id，而用本表表达多对多；
 * audit_status / auditor / audit_time 留审核痕迹，"批量审核"即批量写入或恢复本表。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractorPerson implements Serializable {

    private String id;
    private String contractor_id;
    private String person_id;
    /** 审核状态：1=已通过 2=已驳回 */
    private Integer audit_status;
    private String auditor;
    private String remark;
    private String creator;
    private Boolean open_status;
}
