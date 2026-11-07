package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 承包商 × 资质项 的逐项审核结果（cbs.t_contractor_qual_check）。
 * 清单本身不落在本表：仍以 t_contractor_qual（按承包商 ctype 对应的字典项取）为准，
 * 本表只记录"这家承包商在这一项上审得怎么样"，故查询时以清单 left join 本表。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsQualCheck implements Serializable {

    /** 本表主键；未审核过的项为空 */
    private String id;
    private String contractor_id;
    private String qual_id;
    /** 分类：1=资质证件 2=审核所需文件 */
    private Integer qual_type;
    /** 审核结果：0=未审核 1=符合 2=不符合 */
    private Integer check_status;
    /** 有效期至（空=长期有效） */
    private LocalDate valid_date;
    private String remark;
    private String checker;
    private String creator;

    /* ---- 以下为清单字段（来自 t_contractor_qual，仅查询用，不落本表） ---- */
    private String qual_name;
    private String qual_level;
    /** 有效期(月)，0=长期有效 */
    private Integer validity;
    /** 临期提醒(天) */
    private Integer remind;
}
