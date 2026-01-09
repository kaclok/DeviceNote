package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import androidx.annotation.NonNull;

import java.io.Serializable;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TNFCPatrolDept implements Serializable {
    private String id;
    private String name;

    public TNFCPatrolDept(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Spinner 默认显示 toString()
    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
