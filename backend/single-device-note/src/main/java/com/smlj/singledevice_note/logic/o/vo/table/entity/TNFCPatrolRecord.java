package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
public class TNFCPatrolRecord implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String rfid;
    private Date dotime; // 打卡时间
    private String person; // 巡检人员
    private long id;
    private String content; // 该点位设备巡检情况
    private int errornum; // 该点位异常设备数量
}

