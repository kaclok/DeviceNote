package com.smlj.singledevice_note.gb.o;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * jt_dev 车辆过磅记录（dic_weigh_weight）+ 关联单据头（送货通知 / 派车管理）。
 * docType：delivery=送货（biz_receipt_delivery_notice_head，receipt_type=220）
 *          sendcar=拉货（biz_receipt_send_car_head，receipt_type=811）；无关联单据时为空。
 */
@Data
public class GbVehicleWeigh {
    private Long id;
    /** 称重编号，如 WH2026102700058 */
    private String weighingNo;
    /** 车牌号 */
    private String plateNo;
    /** 过磅类型 */
    private String weighingType;
    /** 货物名称 */
    private String goodsName;
    /** 货物规格 */
    private String goodsSpec;
    /** 过磅重量 */
    private java.math.BigDecimal weighingWeight;
    /** 重量单位 */
    private String weightUnit;
    /** 过磅磅号 */
    private String weighingStation;
    /** 过磅时间 */
    private LocalDateTime weighingTime;
    /** 原始单据类型：220 送货通知 / 811 派车管理 */
    private Integer receiptType;
    /** 关联单据号（送货 SH / 拉货 PC） */
    private String docCode;
    /** 关联单据状态（见各类型状态字典） */
    private Integer docStatus;
    /** delivery=送货 / sendcar=拉货 / null=未关联单据 */
    private String docType;
}
