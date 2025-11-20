package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TCZPCfg {
    private String _id;
    private List<String> selectShowName = new ArrayList<>();
    private List<String> browserShowName = new ArrayList<>();
    private List<String> fieldValue = new ArrayList<>();
    private int dgCount;
}
