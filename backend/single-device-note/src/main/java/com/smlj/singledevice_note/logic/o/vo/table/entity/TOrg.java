package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;

@Component
@Data
@NoArgsConstructor
public class TOrg implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String dept_code;
    private String dept_name;
    private String dept_all_name;
    private String parent_dept_code;
    private String dept_leader;
    private String master_leader;

    private String[] ids_direct_sub_depts;
    private String[] ids_recursive_sub_depts;
}

