package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 承包商保证金变更记录（cbs.t_contractor_change_log）。
 * type：1=扣款 2=增加；保存时事务内同步增减 t_contractor.deposit。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractorChange implements Serializable {

    private String id;
    private String contractor_id;
    /** 1=扣款 2=增加 */
    private Integer type;
    /** 变更金额(元)，恒为正数，方向由 type 决定 */
    private BigDecimal money;
    private String describe;
    /** 变更时间（页面可指定，默认当天） */
    private LocalDate change_time;
    /** 操作人 */
    private String creator;
}
