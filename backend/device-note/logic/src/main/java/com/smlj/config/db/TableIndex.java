package com.smlj.config.db;

import lombok.Data;

@Data
public class TableIndex {
    private String Table;
    private String Non_unique;
    private String Key_name;
    private String seq_in_index;
    private String Collation;
    private String Cardinality;
    private String Sub_part;
    private String Packed;
    private String Null;
    private String Index_type;
    private String Comment;
    private String Index_comment;
}
