package com.smlj.nfcpatrol.logic.activity;

import java.util.Map;

import lombok.Data;

@Data
public class CardTp {
    final boolean checked;
    final Map<String, String> positions;

    public CardTp(boolean checked, Map<String, String> positions) {
        this.checked = checked;
        this.positions = positions;
    }
}