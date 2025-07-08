package com.smlj.singledevice_note.logic.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.controller.reader._500000004_library_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_wlcc_reader;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_library_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_wlcc_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Twz;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                                 MultipartFile file_wlcc,
                                 MultipartFile file_library) {

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

            combine_500000004(wlcc, library);
        }
        else if (goods_id == 500000005) {

        }
        else if (goods_id == 200000775) {

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
        wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::getGross_time)); // 升序排序
        library.sort(Comparator.comparing(Tcggy_library_500000004::getXy_dt)); // 升序排序
    }
}
