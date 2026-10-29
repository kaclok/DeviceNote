package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目开工准备（cbs.t_project_prep，PK=prep_id）。
 * status：0 未完成 / 1 已完成。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsProjectPrep implements Serializable {

    private String prep_id;
    private String project_id;
    private LocalDate prep_date;
    /** 准备事项 */
    private String content;
    private Integer status;
    /** 确认人 */
    private String checker;
    private String remark;
    private String creator;
    private Boolean open_status;
    /** 列表展示用：关联项目名称 */
    private String proj_name;
}
