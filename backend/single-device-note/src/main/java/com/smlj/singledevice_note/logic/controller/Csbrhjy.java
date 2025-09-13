package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TsbrhjyDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/sbrhjy")
@Tag(name = "Csbrhjy", description = "设备润滑加油")
public class Csbrhjy {
    private final TsbrhjyDao dao;

    @Transactional
    @GetMapping(value = "/getList")
    public Result<?> getList(@RequestParam(name = "zzId", required = false) String zzId,
                             @RequestParam(name = "levelId", required = false) String levelId,
                             @RequestParam(name = "filterByName", required = false) String filterByName,
                             @RequestParam(name = "filterByWeihao", required = false) String filterByWeihao,
                             @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                             @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {

        String conds = "zz_id = '" + zzId + "' and level_id = '" + levelId + "'";
        if (!StrUtil.isEmpty(filterByName)) {
            conds += "and name like '%" + filterByName + "%' ";
        }
        if (!StrUtil.isEmpty(filterByWeihao)) {
            conds += "and weihao like '%" + filterByWeihao + "%' ";
        }
        String tbName = "t_device";
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = dao.doSelectSimple(tbName, "*", conds, "id");
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @GetMapping(value = "/save")
    public Result<?> save(@RequestParam(name = "device_id") int device_id, @RequestParam(name = "c_a_person", required = false) String c_a_person, @RequestParam(name = "c_b_person", required = false) String c_b_person, @RequestParam(name = "type") int type) {
        String tbName = "t_device";
        LocalDateTime now = LocalDateTime.now();
        var result = 0; // 更新行数
        if (type == 1) {
            result = dao.updateTime(tbName, device_id, now, null);
        } else if (type == 2) {
            result = dao.updateTime(tbName, device_id, null, now);
        }
        return Result.success(result);
    }
}
