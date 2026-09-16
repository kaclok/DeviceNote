package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
@NoArgsConstructor
public class TCGHTContract implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    /** 主键（自动生成），即时结算类唯一校验按 id，周期结算类允许 id 重复但 unique_id 不同 */
    private String unique_id;
    /**
     * 归属部门 Code，关联 train.t_org.dept_code（必填）。
     * 新增/编辑合同时由部门选择器选定，列表展示与部门筛选都基于该字段。
     */
    private String dept_code;

    /**
     * 录入人：创建该合同时的登录账号（account）。由后端按登录态写入，客户端传值一律被忽略
     * （新增时覆盖、导入时逐行写入、更新时整列不动），因此不可伪造。
     * 用途是「本人」档数据范围（data_scope=1）的过滤依据 —— 见 CCGHT.expandScope。
     * 系统字段：刻意不进 Excel 导入/导出（见前端 ExcelX.FIELD_DEFS），也不在任何页面展示。
     */
    private String creator;
    private String title;
    private float amount;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_sign;
    private String sign_person;
    /**
     * 签订方式：自由文本（DB 已由 int 编码改为 varchar(255)），如「定向商定 / 续签 / 竞价」。
     * 列表筛选走 like 模糊匹配，不再有 int↔文本的映射。
     */
    private String sign_type;
    private String supplier;
    private float paycycle_dh;
    private float paycycle_zb;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_yfk;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_dhk;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_zbj;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_rk;
    private String bz;

    private String pay_type;
    /** 付款类型：1-即时结算类 2-周期结算类 */
    private Integer payment_type;
    private float settle_amount;
    /** 已付款金额（元） */
    private float has_amount;
    private int hq;
    private String date_htyj;
    private String date_fpyj;
    private String date_actual_dh;
    private String date_ruzlyj;

    /**
     * 财务环节进度：0-未完结 1-预付款 2-到货款 3-质保款(完结)
     */
    private Integer finish_step;
    private boolean open_status;
}

