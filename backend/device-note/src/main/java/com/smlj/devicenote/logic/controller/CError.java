package com.smlj.devicenote.logic.controller;

import com.smlj.devicenote.core.o.to.Result;

import com.smlj.train.logic.o.dto.to.common.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// 全局异常处理器
// 处理404、错误等情况， 可以利用Thymeleaf返回404.html或者error.html
@Slf4j
@Controller
// @ControllerAdvice
@Tag(name = "CError", description = "全局错误 相关操作（比如404...）")
public class CError implements ErrorController {
    // 测试服务器是否启动成功
    @Operation(summary = "默认主页展示")
    @Parameters({@Parameter(name = "model", description = "Thymeleaf的模版操作对象")})
    /*@ApiResponses({
            @ApiResponse(responseCode = "200", description = "请求成功"),
            @ApiResponse(responseCode = "400", description = "请求参数没填好"),
            @ApiResponse(responseCode = "401", description = "没有权限"),
            @ApiResponse(responseCode = "403", description = "禁止访问"),
            @ApiResponse(responseCode = "404", description = "请求路径没有或页面跳转路径不对")
    })*/
    @GetMapping("/")
    private String defaultIndex(Model model) {
        /*  // 测试异常被捕获
            Integer i = 10;
            i = i / 0;
        */
        // 模版引擎Thymeleaf
        // https://blog.csdn.net/ymaini/article/details/85269200
        // https://blog.csdn.net/Lzy410992/article/details/115371017
        model.addAttribute("msg", "Hello, Thymeleaf!");
        // 默认html文件存储在templates下
        return "views/index"; // 返回的是html的路径/文件名
    }

    /*
        当 Spring Boot 接收到不存在的请求连接时，默认情况下会按照以下步骤执行：
        首先，Spring Boot 的 DispatcherServlet 会接收到这个请求。
        然后，它会遍历已配置的所有控制器（Controller）及其方法上的 @RequestMapping 注解，尝试匹配当前请求的 URL 和 HTTP 方法。
        如果没有找到匹配的处理方法，Spring Boot 会检查是否配置了自定义的错误处理机制。
        如果没有自定义的错误处理，默认情况下会返回 404（Not Found）状态码。
        如果您自定义了错误处理，例如通过实现 ErrorController 或使用 @ControllerAdvice 注解来处理全局异常，那么在没有匹配到有效请求处理方法时，将执行您自定义的错误处理逻辑。

        @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)全局异常捕获会优先捕获异常，如果没有全局异常捕获，那么controller内部的异常也会被/error
        捕获到，从而通过html信息输出
    */

    // 处理404 或者 代码异常 的展示
    @RequestMapping("/error")
    public String error(HttpServletRequest request, HttpServletResponse response, Model model) {
        log.info("statusCode:{}", response.getStatus());
        /*if (response.getStatus() == 404) {
            return "views/404";
        } else*/ {
            Exception exception = (Exception) request.getAttribute("jakarta.servlet.error.exception");
            model.addAttribute("exceptionMessage", exception == null ? "" : exception.toString());
            return "views/error";
        }
    }

    // SpringMVC参数不正确
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public Result<Boolean> missingServletRequestParameterExceptionHandler(HttpServletRequest req, MissingServletRequestParameterException e) {
        var err = e.getMessage();
        log.error("exception:" + err);
        return Result.fail(ReturnCode.RC402.getCode(), err);
    }

    // https://mp.weixin.qq.com/s/95ziMFD4Lz3F7mjZ4TvclA
    // 全局异常捕获 https://mp.weixin.qq.com/s?chksm=fa4b37c8cd3cbede86b965ffecafa7ee87374e7c2d8adb9f2da82a877340ee3e4949b5c48d53&exptype=subscribed_raw_exper_tlfeeds&ranksessionid=1725511862_7&mid=2247600249&sn=c47d04c39364d08695ed358dbd5cfe37&idx=1&__biz=MzUzMTA2NTU2Ng==&sessionid=1725517581&subscene=200&clicktime=1725517586&enterid=1725517586&flutter_pos=70&biz_enter_id=5&ascene=0&devicetype=iOS17.6.1&version=1800323b&nettype=WIFI&abtest_cookie=AAACAA==&lang=zh_CN&countrycode=NZ&fontScale=100&wx_header=&start=6525&end=7620&underline_after_modify=0&item_show_type=0&share_id=1725517732584&contentMd5=aa2daf1a9adc05a73631be2bf8920438&scene=260&forbidShowSource=0#wechat_redirect
    @ExceptionHandler(Exception.class)
    public Result<Boolean> h_exception(Exception e, HttpServletRequest req, HttpServletResponse response) {
        var err = e.getMessage();
        log.error("exception:" + err);
        // 包装 Result结果
        return Result.fail(ReturnCode.RC500.getCode(), err);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Boolean> h_MaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        var err = e.getMessage();
        log.error("exception:" + err);
        // 包装 Result结果
        return Result.fail(ReturnCode.RC500.getCode(), err);
    }
}
