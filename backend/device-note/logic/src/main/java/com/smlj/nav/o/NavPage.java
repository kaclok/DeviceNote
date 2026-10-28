package com.smlj.nav.o;

import lombok.Data;

/**
 * 切页显示方向：orientation 0=跟随手机横竖屏 1=始终横屏 2=始终竖屏
 */
@Data
public class NavPage {
    private String plantId;
    private String pageCode;
    private Integer orientation;
    private Integer sortNo;
}
