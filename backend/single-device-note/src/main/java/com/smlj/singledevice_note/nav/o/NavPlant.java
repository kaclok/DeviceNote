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
}
