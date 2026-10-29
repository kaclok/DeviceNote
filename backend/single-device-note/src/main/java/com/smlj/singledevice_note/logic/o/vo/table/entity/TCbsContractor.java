package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 承包商单位（cbs.t_contractor，PK=contractor_id）。
 * approve_status：0 待审 / 1 通过 / 2 驳回（审核流转由 /cbs/contractor/audit 单独控制）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsContractor implements Serializable {

    private String contractor_id;
    private String name;
    /** 统一社会信用代码（启用行内唯一） */
    private String uscc;
    /** 承包商类型（gd.json 字典） */
    private String ctype;
    private String legal_person;
    private String legal_phone;
    private String safety_person;
    private String safety_phone;
    private String register_address;
    /** 注册资本（原系统文本口径） */
    private String register_capital;
    /** 保证金(元) */
    private BigDecimal deposit;
    /** 预留：一期不开承包商登录 */
    private String login_account;
    private Integer approve_status;
    private String reject_reason;
    private String creator;
    private Boolean open_status;
}
