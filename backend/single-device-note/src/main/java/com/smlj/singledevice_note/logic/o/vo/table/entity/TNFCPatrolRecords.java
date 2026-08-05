package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Component
@Data
public class TNFCPatrolRecords implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private long id;
    private String rfid;
    private Date dotime; // 打卡时间
    private String person; // 巡检人员
    private String deptid;
    private String deptname;
    private boolean zdnormal;
    private boolean wdnormal;
    private boolean ywnormal;
    private boolean qtnormal;
    private Map<String, String> zddetail;
    private Map<String, String> wddetail;
    private Map<String, String> ywdetail;
    private Map<String, String> qtdetial;
}

