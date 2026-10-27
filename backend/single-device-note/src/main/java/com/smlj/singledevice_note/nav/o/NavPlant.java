package com.smlj.singledevice_note.nav.o;

import lombok.Data;

@Data
public class NavPlant {
    private String id;
    private String name;
    private String imageUrl;
    private Integer imageWidth;
    private Integer imageHeight;
    private Double metersPerPixel;
    private String remark;
    private String dutyUrl;
    /** 地磅查询：车牌过磅记录条数（默认 6） */
    private Integer vehicleWeighCount;
    /** 地磅查询：磅房过磅日志条数（默认 50） */
    private Integer scaleLogCount;
}
