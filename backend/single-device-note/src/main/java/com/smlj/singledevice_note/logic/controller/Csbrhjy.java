package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TsbrhjyDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/sbrhjy")
@Tag(name = "Csbrhjy", description = "设备润滑加油")
public class Csbrhjy {
    private final TsbrhjyDao dao;

    @Transactional
    @GetMapping(value = "/getList")
    public Result<?> getList(
                             @RequestParam(name = "zzId", required = false) String zzId,
                             @RequestParam(name = "levelId", required = false) String levelId,
                             @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                             @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {

        String conds = "zz_id = '" + zzId + "' and level_id = '" + levelId + "'";
        String tbName = "t_device";
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = dao.doSelectSimple(tbName, "*", conds, null);
        return Result.success(new PageSerializable<>(ls));
    }
}
