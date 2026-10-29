package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 项目入场记录（cbs.t_project_enter，PK=enter_id）。
 * 清单第18项范围：不做关联培训/关联考试/进出场管理/设备管理。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsProjectEnter implements Serializable {

    private String enter_id;
    private String project_id;
    private LocalDate enter_date;
    private Integer person_count;
    /** 入场负责人 */
    private String leader;
    private String leader_phone;
    private String remark;
    private String creator;
    private Boolean open_status;
    /** 列表展示用：关联项目名称 */
    private String proj_name;
}
