package com.smlj.singledevice_note.gb.o;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 从原始日志行解析出的过磅事件。
 * 解析规则：content 匹配「自动数据保存成功！单号[x],车牌号[y]」；
 * 同一单号一进一出各产生一行（event_time 区分），前端按单号聚合 in/out 时间。
 */
@Data
public class GbWeighEvent {
    private Long id;
    /** 磅号 */
    private String source;
    /** 来源原始行指纹（溯源用） */
    private String fp;
    /** 单号，如 WH2026102500199 */
    private String orderNo;
    /** 车牌号，如 陕KM9368 */
    private String vehicleNo;
    /** 过磅时间 */
    private LocalDateTime eventTime;
    /** 入库时间 */
    private LocalDateTime createTime;
}
