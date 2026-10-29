package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 涉及的重点监管的危险化工工艺（cbs.t_supervise_process，PK=proc_id，原系统 /system/sysCompanyInvolveHazardProcess）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsSuperviseProcess implements Serializable {

    private String proc_id;
    private String proc_name;
    /** 自动化控制系统、紧急停车系统是否投入使用 */
    private Boolean auto_in_use;
    private String creator;
    private Boolean open_status;
}
