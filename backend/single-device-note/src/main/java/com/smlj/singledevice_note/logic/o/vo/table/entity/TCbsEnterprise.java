package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 企业信息（cbs.t_enterprise，id 恒为 1 的单行表，PRD 附录 A 全量映射；附件/四色图一期只存 file 引用）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsEnterprise implements Serializable {

    private Integer id;
    // ---- 基本信息 ----
    private String ent_name;
    /** 企业简介（富文本，单行表 text 列） */
    private String ent_intro;
    private String uscc;
    private String legal_person;
    private String legal_phone;
    private String legal_idcard;
    /** 危化品登记系统企业编码 */
    private String chem_reg_code;
    private LocalDate found_date;
    private String industry_type;
    private String supervisor;
    private Integer staff_count;
    private String region_code;
    private String address;
    /** 固定资产(万元) */
    private BigDecimal fixed_assets;
    /** 总建筑面积(㎡) */
    private BigDecimal total_area;
    private String register_address;
    private String product_address;
    private String county;
    private String park_name;
    private LocalDate prod_start_date;
    // ---- 经营与规模 ----
    private String industry_detail;
    private String sub_units;
    private String economy_type;
    private String run_status;
    private String supervise_level;
    private String capacity;
    private String ent_scale;
    private String ent_position;
    /** 是否涉及两重点一重大作业（是/否/不涉及） */
    private String two_key_one_major;
    private String license_no;
    private LocalDate license_begin;
    private LocalDate license_end;
    // ---- 安全管理 ----
    private String main_person;
    private String main_person_idcard;
    private String safety_org;
    private String safety_org_phone;
    private String safety_org_leader;
    private String safety_org_leader_phone;
    private String duty_phone;
    private Integer full_safety_count;
    private Integer part_safety_count;
    private Integer special_work_count;
    private String safety_std_level;
    private LocalDate std_cert_expire;
    // ---- 危化与风险 ----
    private Boolean has_major_hazard;
    private Boolean risk_identified;
    /** 重大危险源是否备案（是/否/不涉及） */
    private String hazard_filed;
    /** 涉及危险工艺（是/否/不涉及） */
    private String has_danger_process;
    private Boolean monitor_system;
    private Boolean has_supervision_process;
    // ---- 系统 ----
    private String server_addr;
    private String mqtt_topic;
    /** 厂区四色图 file 引用 */
    private String plant_map_id;
}
