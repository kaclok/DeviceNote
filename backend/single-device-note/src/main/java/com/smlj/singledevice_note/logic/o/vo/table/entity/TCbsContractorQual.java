package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 承包商类型所需的资质 / 审核所需文件（cbs.t_contractor_qual，PK=qual_id）。
 * 同一张表用 qual_type 区分：1=资质管理 2=审核所需文件；
 * 每个承包商类型（t_dict.dict_type=contractor_type 的 item_id）下各维护一份。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractorQual implements Serializable {

    private String qual_id;
    /** 所属承包商类型 item_id */
    private String type_id;
    /** 资质名称 */
    private String qual_name;
    /** 等级/类别（如 一级 / 甲级），可为空；仅 qual_type=1 使用 */
    private String qual_level;
    /** 记录分类：1=资质管理 2=审核所需文件 */
    private Integer qual_type;
    /** 有效期(月)，0=长期有效；qual_type=2 使用 */
    private Integer validity;
    /** 临期提醒(天)；qual_type=2 使用 */
    private Integer remind;
    /** 启用状态：true=启用 false=停用 */
    private Boolean is_using;
    private String creator;
    private Boolean open_status;
    /** 更新时间（查询时由 SQL 格式化为 yyyy-MM-dd HH:mm） */
    private String update_time;
}
