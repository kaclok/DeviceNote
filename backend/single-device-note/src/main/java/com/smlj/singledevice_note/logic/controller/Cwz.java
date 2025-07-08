package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.controller.reader.wzReader;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TwzDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Twz;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/wz")
@Tag(name = "Cwz", description = "物资库存记录")
public class Cwz {
    private final TwzDao twzDao;

    @Transactional
    @GetMapping(value = "/getList")
    public Result<?> getList(@RequestParam(name = "group", required = false, defaultValue = "1") int group,
                             @RequestParam(name = "filterByName", required = false) String filterByName,
                             @RequestParam(name = "filterByModel", required = false) String filterByModel,
                             @RequestParam(name = "filterByDeclarer", required = false) String filterByDeclarer,
                             @RequestParam(name = "filterByHt", required = false) String filterByHt,
                             @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                             @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {

        String conds = null;
        if (!StrUtil.isEmpty(filterByName)) {
            conds = "c_name like '%" + filterByName + "%' ";
        }
        if (!StrUtil.isEmpty(filterByModel)) {
            if (conds == null) {
                conds = "c_model like '%" + filterByModel + "%' ";
            } else {
                conds += "and c_model like '%" + filterByModel + "%' ";
            }
        }
        if (!StrUtil.isEmpty(filterByDeclarer)) {
            if (conds == null) {
                conds = "c_declarer like '%" + filterByDeclarer + "%' ";
            } else {
                conds += "and c_declarer like '%" + filterByDeclarer + "%' ";
            }
        }
        if (!StrUtil.isEmpty(filterByHt)) {
            if (conds == null) {
                conds = "c_ht like '%" + filterByHt + "%' ";
            } else {
                conds += "and c_ht like '%" + filterByHt + "%' ";
            }
        }

        String tbName = _getTbName(group);
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = twzDao.doSelectSimple(tbName, "*", conds, null);
        return Result.success(new PageSerializable<>(ls));
    }

    private String parse(MultipartFile file, List<Twz> cs) {
        try {
            EasyExcel.read(file.getInputStream(), Twz.class, new wzReader(cs)).ignoreEmptyRow(true).autoTrim(true).headRowNumber(1).doReadAll();
        } catch (ExcelDataConvertException edce) {
            return String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
        } catch (IOException e) {
            return e.getMessage();
        }

        return null;
    }

    @Transactional
    @PostMapping(value = "/cover")
    public Result<?> cover(
            @RequestParam(name = "group", required = false, defaultValue = "1") int group,
            MultipartFile[] files) {
        String tbName = _getTbName(group);
        twzDao.Clear(tbName);
        int batchSize = 500; // 每批次插入量

        for (MultipartFile file : files) {
            List<Twz> cs = new ArrayList<>();
            var r = parse(file, cs);
            if (r != null) {
                return Result.fail(r);
            }

            // 分批次插入db
            for (int start = 0; start < cs.size(); start += batchSize) {
                int end = start + batchSize;
                end = Math.min(end, cs.size());
                var subList = cs.subList(start, end);

                twzDao.InsertBatch(tbName, subList);
            }
        }

        return Result.success();
    }

    private String _getTbName(int group) {
        String tbName = "t_wz_smlj";
        if (group == 2) {
            tbName = "t_wz_smds";
        } else if (group == 3) {
            tbName = "t_wz_mzlj";
        } else if (group == 4) {
            tbName = "t_wz_smjn";
        }
        return tbName;
    }
}
