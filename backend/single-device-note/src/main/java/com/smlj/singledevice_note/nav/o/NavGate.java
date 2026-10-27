package com.smlj.singledevice_note.nav.o;

import lombok.Data;

@Data
public class NavGate {
    private String id;
    private String plantId;
    /** 对应 nav.node 中 type=1(大门) 的点位 */
    private String nodeId;
    /** 通行方向：1 双向 / 2 仅进厂(禁止驶出) / 3 仅出厂(禁止驶入) / 4 禁行（见列注释） */
    private Integer gateMode;
    /** 允许通行的物资 id（JSON 数组），为空表示不限物资 */
    private String allowedMaterialIds;
    private Integer sortNo;
}
