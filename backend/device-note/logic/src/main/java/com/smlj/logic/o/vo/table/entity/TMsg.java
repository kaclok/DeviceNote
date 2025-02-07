package com.smlj.logic.o.vo.table.entity;

import java.util.Date;
import java.io.Serializable;

import lombok.Data;

/**
 * (TMsg)表实体类
 *
 * @author Cui
 * @since 2025-01-17 16:31:26
 */
@Data
public class TMsg implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private Integer id;
    private String userId;
    private Integer msgKind;
    private Integer msgType;
    private String msgTitle;
    private String msg;
    private Date createTime;
    private String sender;
    private Boolean isReaded;
}

