package com.smlj.singledevice_note.gb.o;

import lombok.Data;

import java.time.LocalDateTime;

/** 单号聚合视图：一次完整的进出磅（首行=进场，末行=出场） */
@Data
public class GbTimeline {
    private String orderNo;
    /** 首次过磅时间（进场） */
    private LocalDateTime inTime;
    /** 最后一次过磅时间（出场） */
    private LocalDateTime outTime;
    /** 过磅次数 */
    private Integer cnt;
}
