package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeviceDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TDeviceRecordDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TGYDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeviceRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

// https://blog.csdn.net/miles067/article/details/132567377
// @RestController 是一个组合注解，它结合了 @Controller 和 @ResponseBody 注解的功能（就相当于把两个注解组合在一起）。在使用 @RestController 注解标记的类中，每个方法的返回值都会以 JSON 或 XML 的形式直接
// https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao

// @RequestMapping是Spring MVC中用于映射web请求（如URL路径）到具体的方法上的注解。它既可以标注在类上，也可以标注在方法上。标注在类上时，表示类中的所有响应请求的方法都是以该类路径为父路径
// @GetMapping用于将HTTP get请求映射到特定处理程序的方法注解,具体来说，@GetMapping是一个组合注解，是@RequestMapping(method = RequestMethod.GET)的缩写
// @PostMapping用于将HTTP post请求映射到特定处理程序的方法注解,具体来说，@PostMapping是一个组合注解，是@RequestMapping(method = RequestMethod.POST)的缩写

// http://localhost:8090/swagger-ui/index.html https://blog.csdn.net/weixin_37833693/article/details/137050893 https://www.iocoder.cn/Spring-Boot/Swagger/?yudao https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CDeviceNote", description = "检修记录")
public class CDeviceNote {
    private final TGYDao tgyDao;
    private final TDeviceDao tDeviceDao;
    private final TDeviceRecordDao tDeviceRecordDao;

    @GetMapping(value = "/getGYListByZZ") // https://mp.weixin.qq.com/s/xCFx5b1fqODDUey6bWGX_A
    public Result<?> getGYListByZZ(String zzId) {
        String conds = "zz = \'" + zzId + "\'";
        String orderBy = "id asc";
        var ls = tgyDao.doSelectSimple("t_gy", "*", conds, orderBy);
        return Result.success(ls);
    }

    @GetMapping(value = "/getDeviceList")
    public Result<?> getDeviceList(String gyId, @RequestParam(name = "posIdx", required = false) String posIdx, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        String conds = "gy_id = \'" + gyId + "\'";
        if (!StrUtil.isEmpty(posIdx)) {
            conds += " and pos_Idx like \'" + posIdx + "\'";
        }

        String orderBy = "id asc";
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = tDeviceDao.doSelectSimple("t_device", "*", conds, orderBy);
        return Result.success(new PageInfo<>(ls));
    }

    @GetMapping(value = "/getRecordList")
    public Result<?> getRecordList(String deviceId, @RequestParam(name = "person", required = false) String person, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        String conds = "device_id = \'" + deviceId + "\'";
        if (!StrUtil.isEmpty(person)) {
            conds += " and c_person like \'" + person + "\'";
        }

        String orderBy = "record_time desc";
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = tDeviceRecordDao.doSelectSimple("t_device_record", "*", conds, orderBy);
        return Result.success(new PageInfo<>(ls));
    }

    @GetMapping(value = "/getRecord")
    public Result<?> getRecord(Integer recordId) {
        String conds = "id = \'" + recordId + "\'";
        var ls = tDeviceRecordDao.doSelectSimple("t_device_record", "*", conds, null);
        return Result.success(ls);
    }

    @GetMapping(value = "/report")
    public Result<?> report(String gyId, String deviceId, String info) {
        var t = new TDeviceRecord(info);
        t.setGy_id(gyId);
        t.setDevice_id(deviceId);
        t.setRecord_time(new Date());
        tDeviceRecordDao.insert(t);
        return Result.success();
    }
}
