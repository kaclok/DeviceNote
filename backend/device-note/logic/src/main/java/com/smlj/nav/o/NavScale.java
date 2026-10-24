package com.smlj.nav.o;

import lombok.Data;

@Data
public class NavScale {
    private String id;
    private String plantId;
    private String code;
    private String name;
    private String nodeId;
    private String area;
    private String capacity;
    /** 磅房状态：1 正常 / 2 检修 / 3 停用 */
    private Integer status;
    private Integer sortNo;
}
