package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 特殊作业报备（cbs.t_spotcheck，PK=check_id）。
 * <p>
 * work_type：作业票类型 1~15，与前端 gd.json#jobTicketTypes 同序
 * （1 特级动火 / 2 一级动火 / 3 二级动火 / 4 受限空间 / 5 盲板抽堵 / 6~9 一~四级高处 /
 * 10~12 一~三级起重吊装 / 13 临时用电 / 14 动土 / 15 断路）。
 * status：1 进行中 / 2 已完成 / 3 作废 / 4 已归档（对应目标站 ticket_report_status）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsSpotcheck implements Serializable {

    private String check_id;
    /** 作业票类型 1~15 */
    private Integer work_type;
    /** 作业地点 */
    private String place;
    /** 作业开始时间 */
    private LocalDateTime begin_time;
    /** 作业结束时间 */
    private LocalDateTime end_time;
    /** 是否承包商作业 */
    private Boolean is_contractor_operate;
    /** 施工单位/人员 */
    private String unit_name;
    /** 项目负责人 */
    private String operation_person;
    /** 联系方式 */
    private String operation_phone;
    /** 作业内容 */
    private String content;
    /** 报备状态 1=进行中 2=已完成 3=作废 4=已归档 */
    private Integer status;
    /** 报备日期 */
    private LocalDate report_date;
    /** 报备人员（姓名） */
    private String reporter;
    private String creator;
    private Boolean open_status;
}
