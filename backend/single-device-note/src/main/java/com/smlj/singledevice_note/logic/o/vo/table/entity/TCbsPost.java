package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 岗位（cbs.t_post，PK=post_id）。
 * post_status：1 启用 / 0 停用。t_user 一期未关联岗位，仅作台账。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsPost implements Serializable {

    private String post_id;
    /** 岗位编码（启用行内唯一） */
    private String post_code;
    private String post_name;
    private Integer post_status;
    private String remark;
    private String creator;
    private Boolean open_status;
}
