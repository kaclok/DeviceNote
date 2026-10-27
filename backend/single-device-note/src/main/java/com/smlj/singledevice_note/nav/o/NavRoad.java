package com.smlj.singledevice_note.nav.o;

import lombok.Data;

@Data
public class NavRoad {
    private String id;
    private String plantId;
    private String code;
    private String name;
    private String groupName;
    /** 道路等级：1 主干道 / 2 次干道 / 3 支路（见列注释） */
    private Integer level;
    private String fromNodeId;
    private String toNodeId;
    /** JSON 折点串，如 [[75,300],[300,300]] */
    private String points;
    private Integer oneWay;
    /** 1 通行 / 0 封路 */
    private Integer enabled;
    private Integer sortNo;
}
