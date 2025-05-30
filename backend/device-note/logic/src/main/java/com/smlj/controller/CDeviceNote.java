package com.smlj.controller;

import com.smlj.o.to.Result;
import com.smlj.o.vo.table.dao.TGYDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

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
    @Autowired
    private TGYDao tgyDao;

    @GetMapping(value = "/getGYListByZZ")
    public Result<?> getGYListByZZ(@RequestParam(value = "zzId") String zzId) {
        ArrayList<String> conds = new ArrayList<>();
        conds.add("zz = \'" + zzId + "\'");

        var ls = tgyDao.doSelect("t_gy", "*", conds, null);
        return Result.success(ls);
    }
}
