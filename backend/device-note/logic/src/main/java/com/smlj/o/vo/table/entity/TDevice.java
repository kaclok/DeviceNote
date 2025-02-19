package com.smlj.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * (TMsg)表实体类
 *
 * @author Cui
 * @since 2025-01-17 16:31:26
 */
@Data
public class TDevice implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    private String gy_id;
    private String pos_idx;
    private String name;
    private String install_location;
}

