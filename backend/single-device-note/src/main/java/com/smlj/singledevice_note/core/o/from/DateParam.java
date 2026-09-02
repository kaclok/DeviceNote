package com.smlj.singledevice_note.core.o.from;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// 用@ModelAttribute可以一次性接收多个参数
// 并且@ModelAttribute和@RequestParam可以一起混合使用
@Data
@NoArgsConstructor
public class DateParam {
    private Date beginTime;     // 开始时间
    private Date endTime;       // 结束时间
}
