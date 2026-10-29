package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 在线抽查（cbs.t_spotcheck，PK=check_id）。
 * work_type：1 动火 / 2 受限空间 / 3 盲板抽堵 / 4 高处作业 / 5 吊装 / 6 临时用电 / 7 动土 / 8 断路。
 * result：1 合格 / 0 不合格。rectify_status：0 无需整改 / 1 待整改 / 2 已整改。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsSpotcheck implements Serializable {

    private String check_id;
    private Integer work_type;
    private LocalDate check_date;
    /** 抽查人 */
    private String checker;
    /** 被抽查承包商（可空=厂内自查）；列表展示用 contractor_name 查询时填充 */
    private String contractor_id;
    /** 抽查内容 */
    private String spot_content;
    private Integer result;
    /** 问题描述（不合格时） */
    private String problem_desc;
    private Integer rectify_status;
    private String creator;
    private Boolean open_status;
    /** 列表展示用：关联承包商名称 */
    private String contractor_name;
}
