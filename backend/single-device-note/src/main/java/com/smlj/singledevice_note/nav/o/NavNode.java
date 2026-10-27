package com.smlj.singledevice_note.nav.o;

import lombok.Data;

@Data
public class NavNode {
    private String id;
    private String plantId;
    private String code;
    private String name;
    /** 点位类型：1 大门 / 2 路口 / 3 磅房 / 4 停车场 / 5 拉货点 / 6 送货点（见列注释） */
    private Integer type;
    private Double x;
    private Double y;
    /** 点位状态：1 正常 / 2 检修 / 3 停用，仅磅房节点有意义 */
    private Integer status;
    private String description;
    private Integer sortNo;
    /** 是否在地图和站点列表展示；false 时仅参与寻路计算 */
    private Boolean visible;
}
