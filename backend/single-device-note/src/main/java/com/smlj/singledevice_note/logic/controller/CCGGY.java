package com.smlj.singledevice_note.logic.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.controller.reader._500000004_library_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_wlcc_reader;
import com.smlj.singledevice_note.logic.o.vo.table.dao.Tcggy_js_500000004_Dao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_js_500000004;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    private final Tcggy_js_500000004_Dao js_500000004_dao;

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

        List<?> fr = null;

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
            fr = combine_500000004(wlcc, library, localDateTime);
        } else if (goods_id == 500000005) {

        } else if (goods_id == 200000775) {

        }

        return Result.success(fr);
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

    private List<Tcggy_js_500000004> combine_500000004(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library, LocalDateTime localDateTime) {
        List<Tcggy_js_500000004> ls = new ArrayList<>();
        for (var w : wlcc) {
            var js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
            ls.add(js);

            if (!w.is_filtered()) {
                js.setDate(w.getTare_time().toLocalDate());
                var ll = search(w.getCar_no(), w.getGross_time(), library);
                if (ll != null) {
                    js.setLibrary_id(ll.getId());
                    js.setFq(ll.getFq());
                    js.setXy_dt(ll.getXy_dt());
                    js.setC_comment(ll.getC_commecnt());
                    js.setGoods_level(ll.getGoods_level());
                    js.setModify_time(localDateTime);
                }
            } else {
                // 被过滤的item也需要设置为：结算状态
                js.set_js(true);
            }
        }

        js_500000004_dao.InsertBatch("t_500000004_js", ls);
        return ls;
    }

    private Tcggy_library_500000004 search(String carNo, LocalDateTime grossTime, List<Tcggy_library_500000004> library) {
        for (var one : library) {
            if (!one.is_handled() && one.getC_commecnt().equals(carNo) && grossTime.isBefore(one.getXy_dt())) {
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
        wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered) // 首先按照is_filtered升序排序
                .thenComparing( // 然后在is_filtered为false的情况下按照gross_time升序排序
                        obj -> obj.is_filtered() ? null : obj.getGross_time(),
                        Comparator.nullsLast(Comparator.naturalOrder()))); // 升序排序
    }
}
