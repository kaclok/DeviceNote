package com.smlj.singledevice_note.logic.o.vo.logic_entity.nfcpatrol;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
public class VNFCPatrolLineRecord implements Serializable {
    private int lineid;
    private int cycle;
    private String linename;
    private String zzid;
    private String deptname;
    private String cycle_start_time;
    private String cycle_end_time;
    private String patrol_person;
    private int checked_cnt;
    private int total_cnt;
    private int status;
    private String deptid;
}
