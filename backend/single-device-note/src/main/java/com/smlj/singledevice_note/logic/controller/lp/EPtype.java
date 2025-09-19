package com.smlj.singledevice_note.logic.controller.lp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EPtype {
    ALL(0, "all"),
    CZP(1, "czp"),
    GZP(2, "gzp");

    private final int type;
    private final String id;

    public static EPtype fromValue(int value) {
        for (var flag : EPtype.values()) {
            if ((flag.type ^ value) == 0) {
                return flag;
            }
        }
        return ALL;
    }
}
