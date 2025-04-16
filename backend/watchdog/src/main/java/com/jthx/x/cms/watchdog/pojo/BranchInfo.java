package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class BranchInfo {

    // 分厂名称
    private String branchName;
    // 分厂id
    private int branchId;
}
