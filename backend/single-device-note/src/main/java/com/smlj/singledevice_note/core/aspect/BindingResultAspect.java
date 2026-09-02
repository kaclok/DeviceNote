package com.smlj.singledevice_note.core.aspect;

import com.smlj.singledevice_note.core.o.to.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.stream.Collectors;

// 使用的时候，用ValidBinding标记的接口需要存在BindingResult result这个参数
//@RestController
//@RequestMapping("/api/orders")
//public class OrderController {
//
//    @ValidBinding  // 加上注解，自动处理 BindingResult
//    @PostMapping("/create")
//    public Result create(@Valid @RequestBody OrderRequest request, BindingResult result) {
//        // 不需要再判断 result.hasErrors()
//        orderService.create(request);
//        return Result.success("创建成功");
//    }
//}

@Slf4j
@Component
public class BindingResultAspect {
    @Around("@annotation(ValidBinding)")
    public Object handleBindingResult(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        // 查找 BindingResult 参数
        for (Object arg : args) {
            if (arg instanceof BindingResult result) {
                if (result.hasErrors()) {
                    List<String> errors = result.getFieldErrors()
                            .stream()
                            .map(error -> error.getField() + ": " + error.getDefaultMessage())
                            .collect(Collectors.toList());
                    return Result.fail(400, "参数校验失败", errors);
                }
            }
        }

        // 校验通过，执行原方法
        return joinPoint.proceed();
    }
}
