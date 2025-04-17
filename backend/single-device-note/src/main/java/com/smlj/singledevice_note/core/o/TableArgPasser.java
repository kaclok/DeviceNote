package com.smlj.common.o;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class TableArgPasser extends ArgPasser {
    @NotNull
    private String tableName;

    public TableArgPasser setTable(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
