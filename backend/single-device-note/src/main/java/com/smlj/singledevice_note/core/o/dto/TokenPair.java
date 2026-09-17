package com.smlj.singledevice_note.core.o.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Date;

@Data
@NoArgsConstructor
public class TokenPair {
    private Triple<Date, Date, String> at;
    private Triple<Date, Date, String> rt;
}
