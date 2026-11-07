package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 承包商处罚记录（cbs.t_contractor_punish_log）。
 * type_id -> t_dict.item_id (dict_type=penalty_type)；project_id -> t_project（限该承包商名下）。
 * 附件/签字照片涉及文件服务，一期不做。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractorPunish implements Serializable {

    private String id;
    private String contractor_id;
    private String project_id;
    /** 处罚类型（t_dict.item_id, dict_type=penalty_type） */
    private String type_id;
    /** 违规人（文本口径，暂无承包商人员表） */
    private String punish_user;
    private BigDecimal money;
    private String describe;
    private LocalDate punish_time;
    private String creator;

    /** 列表联查出的显示字段（非落库列） */
    private String type_name;
    private String proj_name;
}
