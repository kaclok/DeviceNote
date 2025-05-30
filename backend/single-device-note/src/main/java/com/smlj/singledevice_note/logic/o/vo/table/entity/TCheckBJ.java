package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * (TMsg)表实体类
 *
 * @author Cui
 * @since 2025-01-17 16:31:26
 */
@Component
@Data
public class TCheckBJ implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    private String name;
    private String tableName;
}

