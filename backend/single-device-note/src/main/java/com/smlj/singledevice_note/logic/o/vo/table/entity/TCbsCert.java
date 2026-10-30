package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 证照（cbs.t_cert，PK=cert_id）。expire_date 为到期预警判据，warn_days 提前预警天数。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsCert implements Serializable {

    private String cert_id;
    /** 证照类别：1=企业证书 2=企业人员证书（两类各自一页） */
    private Integer cert_type;
    private String cert_name;
    private String cert_no;
    private String issue_org;
    private LocalDate issue_date;
    private LocalDate expire_date;
    private Integer warn_days;
    /** 以下三列仅人员证书使用 */
    private String holder_name;
    private String holder_idcard;
    private String cert_scope;
    private String dept_code;
    private String creator;
    private Boolean open_status;
}
