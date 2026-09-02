package com.smlj.singledevice_note.core.o.from.param;

import jakarta.validation.constraints.AssertTrue;
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

    @AssertTrue(message = "开始时间不能晚于结束时间")
    public boolean isValid() {
        if (beginTime == null || endTime == null) {
            return true;
        }
        return beginTime.before(endTime);
    }
}
