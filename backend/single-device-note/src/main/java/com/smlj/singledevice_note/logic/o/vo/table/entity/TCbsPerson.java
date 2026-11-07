package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 人员库（cbs.t_person）：全局人员台账，与登录账号 cbs.t_user 解耦
 * （一人一账号，人员库里的人未必有账号）。与承包商的归属走 t_contractor_person 关联表，
 * 因为一个人可同时服务多个承包商。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsPerson implements Serializable {

    private String person_id;
    private String name;
    /** 身份证号（启用行唯一，防重复建档） */
    private String id_card;
    private String phone;
    /** 性别：1=男 2=女 */
    private Integer gender;
    /** 岗位 / 工种 */
    private String post_name;
    /** 证书名称 */
    private String cert_name;
    /** 证书编号 */
    private String cert_no;
    /** 进场日期 */
    private LocalDate entry_date;
    private String remark;
    private String creator;

    /** 入参专用：新增人员时一并加入该承包商（不落 t_person，关联写 t_contractor_person） */
    private String contractor_id;

    /* ---- 以下为关联字段（t_contractor_person，仅"某承包商人员"查询用） ---- */
    /** 关联行主键，移除时用 */
    private String rel_id;
    /** 审核状态：0=未审核 1=已审核 */
    private Integer audit_status;
    /** 是否在场：1=在场 2=已离场（关联表字段） */
    private Integer on_site;
    /** 加入（审核）时间，SQL 格式化为 yyyy-MM-dd HH:mm */
    private String audit_time;
    private String auditor;
}
