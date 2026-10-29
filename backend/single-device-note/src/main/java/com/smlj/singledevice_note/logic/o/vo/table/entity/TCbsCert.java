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
    private String cert_name;
    private String cert_no;
    private String issue_org;
    private LocalDate issue_date;
    private LocalDate expire_date;
    private Integer warn_days;
    private String dept_code;
    private String creator;
    private Boolean open_status;
}
