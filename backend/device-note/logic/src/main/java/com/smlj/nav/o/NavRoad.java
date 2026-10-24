package com.smlj.nav.o;

import lombok.Data;

@Data
public class NavRoad {
    private String id;
    private String plantId;
    private String code;
    private String name;
    private String groupName;
    /** main / secondary / branch */
    private String level;
    private String fromNodeId;
    private String toNodeId;
    /** JSON 折点串，如 [[75,300],[300,300]] */
    private String points;
    private Integer oneWay;
    /** 1 通行 / 0 封路 */
    private Integer enabled;
    private Integer sortNo;
}
