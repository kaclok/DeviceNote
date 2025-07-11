package com.smlj.singledevice_note.logic.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.alibaba.excel.read.listener.ReadListener;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.utils.DateTimeUtil;
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

    private final Tcggy_js_500000004_Dao js_500000004_dao;

    @Transactional
    @PostMapping(value = "/submitExcel")
    public Result<?> submitExcel(@RequestParam(name = "goods_id") int goods_id,
                                 @RequestParam(name = "timestamp") long timestamp,
                                 MultipartFile file_wlcc,
                                 MultipartFile file_library) {

        // 将js表的modify时间修改为localDateTime
        Calendar calendar = Calendar.getInstance();
        // calendar.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 北京时区
        calendar.setTimeInMillis(timestamp); // 设置时间戳

        List<?> finalList = null;

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

            // 是否过滤、是否电石、下样时间
            library.sort(Comparator.comparing(Tcggy_library_500000004::is_filtered)
                    .thenComparing(Tcggy_library_500000004::from_ds)
                    .thenComparing(Tcggy_library_500000004::getXy_dt));

            // 是否过滤、是否电石、毛重时间
            wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered)
                    .thenComparing(Tcggy_wlcc_500000004::from_ds)
                    .thenComparing(obj -> obj.is_filtered() ? null : obj.getGross_time(),
                            Comparator.nullsLast(Comparator.naturalOrder())));

            List<Tcggy_js_500000004> fr = combine_500000004_un_ds(wlcc, library, calendar);

            // 是否过滤、是否电石(降序)、皮重时间
            wlcc.sort(Comparator.comparing(Tcggy_wlcc_500000004::is_filtered)
                    .thenComparing(Tcggy_wlcc_500000004::from_ds, Comparator.reverseOrder())
                    .thenComparing(obj -> obj.is_filtered() ? null : obj.getTare_time(),
                            Comparator.nullsLast(Comparator.naturalOrder())));

            fr.addAll(combine_500000004_ds(wlcc, library, calendar));

            finalList = fr;
        } else if (goods_id == 500000005) {

        } else if (goods_id == 200000775) {

        }

        return Result.success(finalList);
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

    private List<Tcggy_js_500000004> combine_500000004_un_ds(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library, Calendar calendar) {
        List<Tcggy_js_500000004> ls = new ArrayList<>();
        for (var w : wlcc) {
            if (!w.is_filtered()) {
                if (w.from_ds()) {
                    continue;
                }

                // 只add非电石公司且没有被过滤的
                var js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
                js.setModify_dt(calendar.getTime());
                js.setDate(w.getTare_time());
                js.setGross_dt(w.getGross_time());

                ls.add(js);

                var ll = search(w, library, false);
                if (ll != null) {
                    js.setLibrary_id(ll.getId());
                    js.setFq(ll.getFq());
                    js.setXy_dt(ll.getXy_dt());
                    js.setC_comment(ll.getC_commecnt());
                    js.setGoods_level(ll.getGoods_level());

                    js.set_matched(true);
                    js.set_js(true);
                } else {
                    js.set_matched(false);
                    js.set_js(false);
                }
            }
        }

        // js_500000004_dao.InsertBatch("t_500000004_js", ls);
        return ls;
    }

    private List<Tcggy_js_500000004> combine_500000004_ds(List<Tcggy_wlcc_500000004> wlcc, List<Tcggy_library_500000004> library, Calendar calendar) {
        List<Tcggy_js_500000004> ls = new ArrayList<>();
        for (var w : wlcc) {
            Tcggy_js_500000004 js = null;

            if (!w.is_filtered()) {
                if (!w.from_ds()) {
                    continue;
                }

                // 只add电石公司且没有被过滤的
                js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
                js.setModify_dt(calendar.getTime());
                js.setDate(w.getTare_time());
                js.setGross_dt(w.getGross_time());

                ls.add(js);

                var ll = search(w, library, true);
                if (ll != null) {
                    js.setLibrary_id(ll.getId());
                    js.setFq(ll.getFq());
                    js.setXy_dt(ll.getXy_dt());
                    js.setC_comment(ll.getC_commecnt());
                    js.setGoods_level(ll.getGoods_level());

                    js.set_matched(true);
                    js.set_js(true);
                } else {
                    js.set_matched(false);
                    js.set_js(false);
                }
            } else {
                js = new Tcggy_js_500000004(w.getWlcc_id(), w.getGoods_supplier(), w.getCar_no(), w.getDiff_weight(), w.is_filtered());
                ls.add(js);
            }
        }
        return ls;
    }

    private Tcggy_library_500000004 search(Tcggy_wlcc_500000004 w, List<Tcggy_library_500000004> library, boolean from_ds) {
        for (var ll : library) {
            if (ll.is_filtered()) {
                continue;
            }

            if (!from_ds) {
                if (!ll.from_ds() && !ll.is_matched() && ll.getC_commecnt().equals(w.getCar_no()) && w.getGross_time().before(ll.getXy_dt())) {
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

    private Date getDate(Date dt) {
        var dt12 = DateTimeUtil.convertTo(dt, 12, 0, 0, 0);
        var dt24 = DateTimeUtil.convertTo(dt, 23, 59, 59, 0);

        if (dt.before(dt12)) {
            return dt12;
        } else {
            return dt24;
        }
    }
}
