package com.smlj.singledevice_note.core.o.to;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUser {
    @Parameter(hidden = true)
    private String uid;
    @Parameter(hidden = true)
    private String account;
    @Parameter(hidden = true)
    private String name;
}
