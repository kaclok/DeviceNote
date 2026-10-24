package com.smlj.nav.o;

import lombok.Data;

@Data
public class NavGate {
    private String id;
    private String plantId;
    /** 对应 nav.node 中 type=gate 的点位 */
    private String nodeId;
    /** 通行方向：both 双向 / in 仅进厂 / out 仅出厂 */
    private String gateMode;
    /** 允许通行的物资 id（JSON 数组），为空表示不限物资 */
    private String allowedMaterialIds;
    private Integer sortNo;
}
