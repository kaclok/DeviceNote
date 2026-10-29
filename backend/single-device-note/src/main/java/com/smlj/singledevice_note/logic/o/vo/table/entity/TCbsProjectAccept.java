package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目验收（cbs.t_project_accept，PK=accept_id）。
 * result：1 通过 / 0 未通过。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsProjectAccept implements Serializable {

    private String accept_id;
    private String project_id;
    private LocalDate accept_date;
    /** 验收人 */
    private String acceptor;
    private Integer result;
    /** 验收意见 */
    private String opinion;
    private String remark;
    private String creator;
    private Boolean open_status;
    /** 列表展示用：关联项目名称 */
    private String proj_name;
}
