package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
@Accessors(chain = true)
public class TlpBase implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    private int request_id; // 这个字段名不一定非要是id

    private int workflow_id;

    private Date submit_time; // 提交时间
    private Date archive_time; // 归档时间

    private String apply_user_name; // 申请人
    private String apply_dept_name; // 申请部门
}

