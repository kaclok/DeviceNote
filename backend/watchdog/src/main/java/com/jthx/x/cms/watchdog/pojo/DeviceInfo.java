package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class DeviceInfo {

    // 设备id
    private int deviceId;
    // 设备名称
    private String deviceName;
    // 所属分厂id
    private int branchId;
}
