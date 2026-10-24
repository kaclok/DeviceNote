package com.smlj.nav.o;

import lombok.Data;

@Data
public class NavMaterial {
    private String id;
    private String plantId;
    private String code;
    private String name;
    /** in 送货（入厂）/ out 拉货（出厂） */
    private String direction;
    private String category;
    private String scaleId;
    /** JSON 数组，备用磅顺序，如 ["s2","s5"] */
    private String backupScaleIds;
    private String pointNodeId;
    /** 物资语音介绍文案，司机页用手机朗读 */
    private String voiceText;
    private Integer enabled;
    private Integer sortNo;
}
