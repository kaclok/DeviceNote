package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class ExceptionInfo {
    private int id;
    private double value;
    private int indicatorId;
    private String indicatorName;
    private int deviceId;
    private int branchId;
    private LocalDateTime date;
}
