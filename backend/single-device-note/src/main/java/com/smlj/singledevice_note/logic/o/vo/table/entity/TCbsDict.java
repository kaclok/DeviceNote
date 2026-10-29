package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 类型字典（cbs.t_dict，PK=item_id，通用键值表）。
 * dict_type 枚举：contractor_type=承包商类型 / penalty_type=承包商处罚类型 / project_type=项目类型。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsDict implements Serializable {

    private String item_id;
    private String dict_type;
    private String item_name;
    private Integer sort_no;
    private Boolean is_using;
    private String creator;
    private Boolean open_status;
}
