package com.smlj.singledevice_note.logic.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.utils.DateTimeUtil;
import com.smlj.singledevice_note.logic.controller.reader._500000004_js_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_khl_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_library_reader;
import com.smlj.singledevice_note.logic.controller.reader._500000004_wlcc_reader;
import com.smlj.singledevice_note.logic.o.vo.table.dao.Tcggy_js_500000004_Dao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_js_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_khl_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_library_500000004;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_wlcc_500000004;
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
    public static Map<Integer, String> GOODS = Map.of(500000004, "电石", 500000005, "煤", 200000775, "焦沫（精品）");

    private final Tcggy_js_500000004_Dao js_500000004_dao;

    @Transactional
    @PostMapping(value = "/submitExcel")
    public Result<?> submitExcel(@RequestParam(name = "goods_id") int goods_id, @RequestParam(name = "timestamp") long timestamp, MultipartFile file_wlcc, MultipartFile file_library, MultipartFile file_khl) {

        // 将js表的modify时间修改为localDateTime
        Calendar calendar = Calendar.getInstance();
        // calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 北京时区
        calendar.setTimeInMillis(timestamp); // 设置时间戳

        List<?> finalList = null;

        if (goods_id == 500000004) {
            List<Tcggy_wlcc_500000004> wlcc = new ArrayList<Tcggy_wlcc_500000004>();
            List<Tcggy_library_500000004> library = new ArrayList<Tcggy_library_500000004>();
            List<Tcggy_khl_500000004> khl = new ArrayList<Tcggy_khl_500000004>();

            var r = parse(file_wlcc, Tcggy_wlcc_500000004.class, new _500000004_wlcc_reader(wlcc), 1);
            if (r != null) {
                return Result.fail(goods_id + "的物流仓储表格解析失败：" + r);
            }

            r = parse(file_khl, Tcggy_khl_500000004.class, new _500000004_khl_reader(khl), 2);
            if (r != null) {
                return Result.fail(goods_id + "的扣灰量表格解析失败：" + r);
            }

            log.info("khl length:{}", khl.size());

            // 是否过滤、皮重时间
            wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered).thenComparing(obj -> obj.is_filtered() ? null : obj.getTare_time(), Comparator.nullsLast(Comparator.naturalOrder())));
            khl.sort(Comparator.comparing(Tcggy_khl_500000004::getEnter_d).thenComparing(Tcggy_khl_500000004::getEnter_t));
            _500000004_match_wlcc_khl(wlcc, khl);

            r = parse(file_library, Tcggy_library_500000004.class, new _500000004_library_reader(library), 2);
            if (r != null) {
                return Result.fail(goods_id + "的实验室表格解析失败：" + r);
            }

            // 是否过滤、是否电石、下样时间
            library.sort(Comparator.comparing(Tcggy_library_500000004::is_filtered).thenComparing(Tcggy_library_500000004::from_ds).thenComparing(Tcggy_library_500000004::getXy_dt));

            List<Tcggy_js_500000004> js_list = new ArrayList<>();

            // 是否过滤、是否电石(升序)、毛重时间
            wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered).thenComparing(Tcggy_wlcc_500000004::from_ds).thenComparing(obj -> obj.is_filtered() ? null : obj.getGross_time(), Comparator.nullsLast(Comparator.naturalOrder())));
            _500000004_match_wlcc_lib(wlcc, library, js_list, calendar, true);

            // 是否过滤、是否电石(降序)、皮重时间
            wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered).thenComparing(Tcggy_wlcc_500000004::from_ds, Comparator.reverseOrder()).thenComparing(obj -> obj.is_filtered() ? null : obj.getTare_time(), Comparator.nullsLast(Comparator.naturalOrder())));
            _500000004_match_wlcc_lib(wlcc, library, js_list, calendar, false);

            for (int i = 0; i < wlcc.size(); i++) {
                var w = wlcc.get(i);
                if (w.is_filtered()) {
                    Tcggy_js_500000004 js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
                    js.setAsh_weight(w.getAsh_weight());
                    js.setHas_matched_khl(w.has_matched_khl());

                    js.setDt(w.getTare_time());
                    js.setGross_dt(w.getGross_time());
                    js.setModify_dt(calendar.getTime());

                    js_list.add(js);
                }
            }

            finalList = js_list;
        } else if (goods_id == 500000005) {

        } else if (goods_id == 200000775) {

        }

        return Result.success(finalList);
    }

    private String parse(MultipartFile file, Class cls, ReadListener readListener, int headRowNumber) {
        try {
            ExcelReaderBuilder readerBuilder = EasyExcel.read(file.getInputStream(), cls, readListener).excelType(ExcelTypeEnum.XLSX).autoCloseStream(true).ignoreEmptyRow(true).autoTrim(true).headRowNumber(headRowNumber);
            readerBuilder.sheet().doRead();

        } catch (ExcelDataConvertException edce) {
            return String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
        } catch (IOException e) {
            return e.getMessage();
        }

        return null;
    }

    private void _500000004_match_wlcc_khl(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_khl_500000004> khl) {
        for (var w : wlcc) {
            if (!w.is_filtered()) {
                var kk = search(w, khl);
                if (kk != null) {
                    w.setAsh_weight(kk.getKhl());
                }
            }
        }
    }

    private void _500000004_match_wlcc_lib(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library, List<Tcggy_js_500000004> js_list, Calendar calendar, boolean un_ds) {
        for (int i = 0; i < wlcc.size(); i++) {
            var w = wlcc.get(i);
            if (!w.is_filtered()/* && w.has_matched_khl()*/) {
                if (un_ds) {
                    if (w.from_ds()) {
                        continue;
                    }
                } else {
                    if (!w.from_ds()) {
                        continue;
                    }
                }

                Tcggy_js_500000004 js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
                js.setHas_matched_khl(w.has_matched_khl());
                js.setAsh_weight(w.getAsh_weight());

                js.setModify_dt(calendar.getTime());
                js.setDt(w.getTare_time());
                js.setGross_dt(w.getGross_time());

                js_list.add(js);

                var ll = search(w, library, !un_ds);
                if (ll != null) {
                    js.setHas_matched_lib(true);

                    js.setLibrary_id(ll.getId());
                    js.setFq(ll.getFq());
                    js.setXy_dt(ll.getXy_dt());
                    js.setC_comment(ll.getC_commecnt());
                    js.setGoods_level(ll.getGoods_level());
                    js.setHas_js(js.isHas_matched_khl());
                }
            }
        }
    }

    private Tcggy_library_500000004 search(Tcggy_wlcc_500000004 w, List<Tcggy_library_500000004> library, boolean from_ds) {
        for (var ll : library) {
            if (ll.is_filtered()) {
                continue;
            }

            if (!from_ds) {
                if (!ll.from_ds() && !ll.is_matched() && ll.getC_commecnt().equalsIgnoreCase(w.getCar_no()) && w.getGross_time().before(ll.getXy_dt())) {
                    ll.set_matched(true);
                    return ll;
                }
            } else {
                if (ll.from_ds() && !ll.is_matched()) {
                    var dt = getDate(ll.getXy_dt());
                    boolean beforeEqual = !(w.getTare_time().after(dt));
                    if (beforeEqual) {
                        return ll;
                    } else {
                        ll.set_matched(true);
                    }
                }
            }
        }
        return null;
    }

    private Tcggy_khl_500000004 search(Tcggy_wlcc_500000004 w, List<Tcggy_khl_500000004> khl) {
        for (var kk : khl) {
            var hms = kk.getEnter_t();
            var mills = DateTimeUtil.convertTo(kk.getEnter_d()) + (hms.getHours() * 60 * 60 + hms.getMinutes() * 60 + hms.getSeconds()) * 1000;
            if (!kk.is_matched() && kk.getCar_no().equalsIgnoreCase(w.getCar_no())
                    /*&& w.getDiff_weight() == kk.getDiff_weight()*/ && mills == w.getTare_time().getTime()) {
                kk.set_matched(true);
                return kk;
            }
        }
        return null;
    }

    private Date getDate(Date dt) {
        var dt12 = DateTimeUtil.convertTo(dt, 12, 0, 0, 0);
        var dt24 = DateTimeUtil.convertTo(dt, 23, 59, 59, 0);

        if (dt.before(dt12)) {
            return dt12;
        } else {
            return dt24;
        }
    }

    @Transactional
    @PostMapping(value = "/submitJsExcel")
    public Result<?> submitJsExcel(@RequestParam(name = "goods_id") int goods_id, @RequestParam(name = "upload_ts") long upload_ts, MultipartFile file_js) {
        if (goods_id == 500000004) {
            List<Tcggy_js_500000004> js = new ArrayList<Tcggy_js_500000004>();

            var r = parse(file_js, Tcggy_js_500000004.class, new _500000004_js_reader(js), 1);
            if (r != null) {
                return Result.fail(goods_id + "的结算表格解析失败：" + r);
            }

            for (var j : js) {
                var dt = DateTimeUtil.convertTo(upload_ts);
                j.setModify_dt(dt);
            }

            js_500000004_dao.Clear("t_500000004_js");
            js_500000004_dao.InsertBatch("t_500000004_js", js);
        }

        return Result.success();
    }

    @Transactional
    @GetMapping(value = "/search")
    public Result<?> search(@RequestParam(name = "goods_id") int goods_id, @RequestParam(name = "upload_ts") long upload_ts) {
        List<?> fr = null;
        if (goods_id == 500000004) {
            fr = js_500000004_dao.doSelectSimple("t_500000004_js", "*", "(floor(extract(epoch from timezone('Asia/Shanghai', modify_dt)))::bigint) = " + (upload_ts / 1000), "dt asc");
        }

        return Result.success(fr);
    }
}
