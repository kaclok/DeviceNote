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
    /** 反应类型（光气化/氯化/硝化…） */
    private String proc_type;
    /** 涉及危险化学品（多个以、分隔） */
    private String chem_involved;
    /** 工艺装置/所在车间 */
    private String device_name;
    /** 自动化控制系统是否投入使用 */
    private Boolean auto_in_use;
    /** 紧急停车系统是否投入使用 */
    private Boolean esd_in_use;
    /** 安全仪表系统是否投入使用 */
    private Boolean sis_in_use;
    /** 是否已开展 HAZOP 分析 */
    private Boolean hazop_done;
    /** 反应工艺危险度等级（1级~5级） */
    private String risk_level;
    private String remark;
    private String creator;
    private Boolean open_status;
}
