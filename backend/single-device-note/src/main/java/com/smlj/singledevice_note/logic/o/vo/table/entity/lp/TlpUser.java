package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class TlpUser implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    private String id;

    private String name;
}
