package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 承包商项目（cbs.t_project，PK=project_id）。
 * proj_status：0 未开始 / 1 进行中 / 2 已结束。dept_code 为数据范围起点（一期暂不启用范围过滤）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsProject implements Serializable {

    private String project_id;
    private String contractor_id;
    private String proj_name;
    private String proj_content;
    private LocalDate begin_date;
    private LocalDate end_date;
    private Integer proj_status;
    private String dept_code;
    private String creator;
    private Boolean open_status;
    /** 列表展示用：关联承包商名称（查询时填充） */
    private String contractor_name;
}
