package com.smlj.devicenote.logic.o.vo.table.dao.common;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class TableValuePasser<T> extends ValuePasser<T> {
    @NotNull
    private String tableName;

    public TableValuePasser setTable(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
