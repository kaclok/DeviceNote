package com.smlj.singledevice_note.logic.o.vo.logic_entity.nfcpatrol;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
public class VNFCPatrolLineRecord implements Serializable {
    private int lineid;
    private String linename;
    private String deptname;
    private String cycle_start_time;
    private String patrol_person;
    private int checked_cnt;
    private int total_cnt;
    private int error_count;
    private int status;
}
