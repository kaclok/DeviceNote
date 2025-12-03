package com.smlj.lpjtlj.logic.o.vo.table.entity.lp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TPCfg {
    private String _id;
    private List<String> selectShowName = new ArrayList<>();
    private List<String> browserShowName = new ArrayList<>();
    private List<String> fieldValue = new ArrayList<>();
    private Integer dgCount;
}
