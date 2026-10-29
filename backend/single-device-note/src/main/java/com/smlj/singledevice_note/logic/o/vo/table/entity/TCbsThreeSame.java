package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 安全"三同时"项目台账（cbs.t_three_same，PK=ts_id，原系统 /threeSimultaneity/project）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsThreeSame implements Serializable {

    private String ts_id;
    private String project_name;
    private String project_type;
    /** 项目立项时间 */
    private LocalDate approval_date;
    /** 项目批复时间 */
    private LocalDate reply_date;
    /** 项目进展 */
    private String progress;
    private String creator;
    private Boolean open_status;
}
