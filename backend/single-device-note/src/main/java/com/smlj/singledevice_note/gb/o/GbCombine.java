package com.smlj.singledevice_note.gb.o;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关键异常组合记录：上报链路里从原始日志行识别出的 7 类关键异常，
 * 把「车牌/单号 + 异常分类 + 原文」组合成一行落库（gb_record.gb_combine）。
 * 幂等键 (source, fp) 与来源原始行 gb_log 一一对应，查询时按此联表带回。
 */
@Data
public class GbCombine {
    private Long id;
    /** 磅号 */
    private String source;
    /** 来源原始行指纹（溯源 + 联表键） */
    private String fp;
    /** 识别到的车牌号（可能为空） */
    private String vehicleNo;
    /** 单号（可能为空） */
    private String orderNo;
    /** 异常分类（超出载重范围/违反PRIMARY KEY约束/有其它未完成记录/未维护身份证号/不可重复称[皮重]/分钟内不可重复过磅/重量不可小于0） */
    private String abnormalType;
    /** 异常原文（组合信息的正文） */
    private String content;
    /** 日志行时间 */
    private LocalDateTime logTime;
    /** 入库时间 */
    private LocalDateTime createTime;
}
