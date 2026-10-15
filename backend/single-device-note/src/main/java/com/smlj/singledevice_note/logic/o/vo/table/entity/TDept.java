package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TDept implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String dept_code;
    private String dept_name;
    private String dept_all_name;
    private String parent_dept_code;
    private String dept_leader;
    private String master_leader;

    private String[] direct_children;
    private String[] recursive_children;

    private Boolean is_company;
}

