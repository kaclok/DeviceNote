package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 安全承诺日报（cbs.t_promise，PK=promise_date，一天一行）。
 * 字段口径见原系统安全承诺页：生产装置 / 隐患分级 / 装置动态 / 特殊作业 / 其他状态 / 企业承诺。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsPromise implements Serializable {

    private LocalDate promise_date;
    // ---- 生产装置 ----
    /** 生产装置套数 */
    private Integer total_sets;
    /** 运行套数 */
    private Integer prod_sets;
    private Integer stop_sets;
    // ---- 隐患分级 ----
    private Integer hazard_lv1;
    private Integer hazard_lv2;
    private Integer hazard_lv3;
    private Integer hazard_lv4;
    // ---- 装置动态 ----
    private Integer start_sets;
    private Integer stop_dev_sets;
    private Integer trial_sets;
    private Integer supervision_process;
    private Integer repair_sets;
    // ---- 特殊作业 ----
    private Integer fire_special;
    private Integer fire_lv1;
    private Integer fire_lv2;
    private Integer confined;
    private Integer blind;
    private Integer work_height;
    private Integer lifting;
    private Integer temp_power;
    private Integer digging;
    private Integer road_close;
    private Integer repair_work;
    // ---- 其他状态 ----
    private Boolean has_contractor_work;
    private Boolean in_trial;
    private Boolean in_startstop;
    private Boolean in_pilot;
    private Boolean has_major_hazard;
    // ---- 企业安全承诺 ----
    private String company_promise;

    private String creator;
}
