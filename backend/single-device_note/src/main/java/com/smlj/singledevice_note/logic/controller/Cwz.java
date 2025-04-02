package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
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
    public Result<?> getList(@RequestParam(name = "filterByName", required = false) String filterByName,
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

        log.error("conds: {}", conds);
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = twzDao.doSelectSimple("t_wz", "*", conds, null);
        return Result.success(new PageSerializable<>(ls));
    }

    private String parse(MultipartFile file, List<Twz> cs) {
        try {
            EasyExcel.read(file.getInputStream(), Twz.class, new wzReader(cs) {
                @Override
                public void invoke(Twz cr, AnalysisContext analysisContext) {
                    cs.add(cr);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    super.doAfterAllAnalysed(context);
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) throws Exception {
                    log.error("解析失败:{}", exception.getMessage());
                    // https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
                    if (exception instanceof ExcelDataConvertException edce) {
                        throw edce;
                    }

                    throw new Exception(exception.getMessage());
                } // 读取所有sheet： https://easyexcel.opensource.alibaba.com/qa/read
            }).ignoreEmptyRow(true).autoTrim(true).headRowNumber(1).doReadAll();
        } catch (ExcelDataConvertException edce) {
            return String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
        } catch (IOException e) {
            return e.getMessage();
        }

        return null;
    }

    @Transactional
    @PostMapping(value = "/cover")
    public Result<?> cover(MultipartFile[] files) {
        twzDao.Clear("t_wz");

        for (MultipartFile file : files) {
            List<Twz> cs = new ArrayList<>();
            var r = parse(file, cs);
            if (r != null) {
                return Result.fail(r);
            }

            twzDao.InsertBatch("t_wz", cs);
        }

        return Result.success();
    }
}
