package com.smlj.logic.config.db;

import lombok.Data;

@Data
public class TableColumn {
    private String COLUMN_NAME;
    private String COLUMN_TYPE;
    private String IS_NULLABLE;
    private String COLUMN_COMMENT;
}
