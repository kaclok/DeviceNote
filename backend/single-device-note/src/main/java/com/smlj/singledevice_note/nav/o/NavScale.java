package com.smlj.singledevice_note.nav.o;

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
    /** 磅号(1~99)：过磅日志监控器（gb-watcher）上报的 source，NULL=未绑定 */
    private Integer gbNo;
}
