package com.smlj.singledevice_note.logic.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.controller.reader._500000004_library_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_wlcc_reader;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_library_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_wlcc_500000004;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/cggy")
@Tag(name = "Cggy", description = "采购供应系统")
public class CCGGY {
    public static Map<Integer, String> GOODS = Map.of(
            500000004, "电石",
            500000005, "煤",
            200000775, "焦沫（精品）"
    );

    @Transactional
    @PostMapping(value = "/submitExcel")
    public Result<?> submitExcel(@RequestParam(name = "goods_id") int goods_id,
                                 @RequestParam(name = "timestamp") long timestamp,
                                 MultipartFile file_wlcc,
                                 MultipartFile file_library) {

        // 将js表的modify时间修改为localDateTime
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                // ZoneId.systemDefault() // 使用系统默认时区
                ZoneId.of("Asia/Shanghai") // 指定时区
        );

        if (goods_id == 500000004) {
            List<Tcggy_wlcc_500000004> wlcc = new ArrayList<Tcggy_wlcc_500000004>();
            List<Tcggy_library_500000004> library = new ArrayList<Tcggy_library_500000004>();

            var r = parse(file_wlcc, Tcggy_wlcc_500000004.class, new _500000004_wlcc_reader(wlcc), 1);
            if (r != null) {
                return Result.fail(goods_id + "的物流仓储表格解析失败：" + r);
            }

            r = parse(file_library, Tcggy_library_500000004.class, new _500000004_library_reader(library), 2);
            if (r != null) {
                return Result.fail(goods_id + "的实验室表格解析失败：" + r);
            }

            sort_500000004(wlcc, library);
            combine_500000004(wlcc, library);
        } else if (goods_id == 500000005) {

        } else if (goods_id == 200000775) {

        }

        return Result.success();
    }

    private String parse(MultipartFile file, Class cls, ReadListener readListener, int headRowNumber) {
        try {
            EasyExcel.read(file.getInputStream(), cls, readListener).ignoreEmptyRow(true).autoTrim(true).headRowNumber(headRowNumber).doReadAll();
        } catch (ExcelDataConvertException edce) {
            return String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
        } catch (IOException e) {
            return e.getMessage();
        }

        return null;
    }

    private void combine_500000004(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library) {
        for (var w : wlcc) {
            if (w.is_handled()) {
                var ll = search(w.getCar_no(), w.getGross_time(), library);
            }
        }
    }

    private Tcggy_library_500000004 search(String carNo, Date grossTime, List<Tcggy_library_500000004> library) {
        for (var one : library) {
            if (!one.is_handled() && one.getC_commecnt().equals(carNo) && grossTime.before(one.getXy_dt())) {
                one.set_handled(true);
                return one;
            }
        }
        return null;
    }

    private void sort_500000004(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library) {
        library.sort(Comparator.comparing(Tcggy_library_500000004::is_ds)  // 首先按照is_ds升序排序, 神木电石的排序在最后
                .thenComparing( // 然后按照下样时间升序排序
                        obj -> !obj.is_ds() ? obj.getXy_dt() : null,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_handled, Comparator.reverseOrder()) // 首先按照is_handled降序排序
                .thenComparing( // 然后在is_handled为true的情况下按照gross_time升序排序
                        obj -> obj.is_handled() ? obj.getGross_time() : null,
                        Comparator.nullsLast(Comparator.naturalOrder()))); // 升序排序
    }
}
