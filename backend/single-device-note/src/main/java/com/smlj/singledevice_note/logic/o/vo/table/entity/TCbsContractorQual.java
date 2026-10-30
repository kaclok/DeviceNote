package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 承包商类型所需资质（cbs.t_contractor_qual，PK=qual_id）。
 * 每个承包商类型（t_dict.dict_type=contractor_type 的 item_id）可关联多条资质。
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
    /** 等级/类别（如 一级 / 甲级），可为空 */
    private String qual_level;
    private String creator;
    private Boolean open_status;
}
