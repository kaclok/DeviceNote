package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TPackageDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CPackage", description = "package版本相关操作")
public class CPackage {
    private final TPackageDao packageDao;

    @PostMapping("/remotePackageVersion")
    public Result<String> remotePackageVersion(String packageName) {
        var app = packageDao.queryById(packageName);
        if (app != null) {
            return Result.success(app.getVersion_name());
        }
        return Result.fail(packageName + "未配置版本信息");
    }
}
