package com.smlj.singledevice_note.nav.o;

import lombok.Data;

@Data
public class NavNode {
    private String id;
    private String plantId;
    private String code;
    private String name;
    /** gate / junction / scale / pickup / delivery */
    private String type;
    private Double x;
    private Double y;
    /** 点位状态：1 正常 / 2 检修 / 3 停用，仅磅房节点有意义 */
    private Integer status;
    private String description;
    private Integer sortNo;
    /** 是否在地图和站点列表展示；false 时仅参与寻路计算 */
    private Boolean visible;
}
