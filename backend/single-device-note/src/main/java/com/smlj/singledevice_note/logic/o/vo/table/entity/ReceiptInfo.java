package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)  // 开启链式编程
@NoArgsConstructor
public class ReceiptInfo {
    private Long id;                     // 表主键
    private String receipt_code;          // 主表biz_receipt_send_car_head的ID
    private Integer receipt_status;       // 单据状态
    private String door_car_id;              // 标记字段
    private String car_number;           // 司机姓名
    private String name;         // 车牌号
    private String phone;          // 手机号
}
