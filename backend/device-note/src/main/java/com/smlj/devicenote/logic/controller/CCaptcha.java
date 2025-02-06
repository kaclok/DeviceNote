package com.smlj.devicenote.logic.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.smlj.devicenote.core.annotation.JwtIgnore;
import com.smlj.devicenote.core.o.to.Result;
import com.smlj.devicenote.core.properties.CaptchaProperty;
import com.smlj.train.logic.o.dto.to.common.ReturnCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Date;

// https://mp.weixin.qq.com/s/xYY7gcIywAImwSsgkRs5kQ
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/train/captcha")
@Tag(name = "CCaptcha", description = "验证码 相关操作")
public class CCaptcha {
    private final CaptchaProperty captchaProp;

    @JwtIgnore
    @Operation(summary = "获取验证码图片")
    @Parameters()
    @GetMapping("/getCaptcha")
    public void getCaptcha(HttpServletResponse response, HttpSession session) {
        // 定义图形验证码的长和宽(配置默认值)
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(captchaProp.getWidth(), captchaProp.getHeight(), captchaProp.getCodeCount(), captchaProp.getLineCount());
        // 细节问题，不影响程序
        // 设置返回类型
        response.setContentType("image/jpeg");
        // 静止缓存
        response.setHeader("Progma", "No-cache");
        try {
            var stream = response.getOutputStream();
            // 图形验证码写出，可以写出到文件，也可以写出到流
            lineCaptcha.write(stream);
            // 同时将验证码内容和当前时间戳存储到Session中
            // 此处Session的键可以配置成常量
            session.setAttribute(captchaProp.getSession().getKey(), lineCaptcha.getCode());
            session.setAttribute(captchaProp.getSession().getDate(), new Date());
            // 关流
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 验证码生效时间限制
    private static final long VALID_MILLIS_TIME = 60 * 1000;

    @JwtIgnore
    @Operation(summary = "校验验证码（使用验证码中的内容信息 和 验证码图片 进行比对）")
    @Parameters({@Parameter(name = "captcha", description = "验证码中的内容信息")})
    @GetMapping("/checkCaptcha")
    // 后端的Session可以存储30分钟，如果30分钟无任何请求，就自动删除。期间有请求，就刷新过期时间,所以登录的时候验证码失败出现错误其实就是session过期导致的。
    //https://www.cnblogs.com/cocovip/p/8878524.html
    /*4.如何判断session过没过期
　　  1) 以前是根据 if(session.getAttribute('user')==null)判断是否为空
     2) 如下为看到的一个帖子，判断session不为空的好方法：request.getSeesion(boolean）方法，一下子让我恍然大悟。这个方法里面传了一个boolean值，这个值如果是true，那么如
        果当前的request的session不可用，那么就创建新的会话，如果存在就返回当前的会话。如果参数是false，那么在request的当前会话不存在的时候就返回null。
        这样我们就可以很容易的联想到这个所谓的request的当前会话是否存在和session过期的联系，所以我们就可以“近似地”认为session不存在就是session过期了，
        那么我们就可以很容易地判断session是否过期了。
    */ public Result<Boolean> checkCaptcha(String captcha, HttpSession session) {
        // 保证传过来的参数是合法的
        if (!StringUtils.hasLength(captcha)) {
            return Result.fail(ReturnCode.RC10206);
        }

        //根据配置的默认session信息获取key和date
        var t = session.getAttribute(captchaProp.getSession().getKey());
        if (t == null) {
            return Result.fail(ReturnCode.RC407);
        }

        String key = (String) t;
        t = session.getAttribute(captchaProp.getSession().getDate());
        Date date = (Date) t;
        // 1.验证码正确(不区分大小写)
        // 2.验证码还未失效
        boolean valid = key.equalsIgnoreCase(captcha)/* && System.currentTimeMillis() - date.getTime() < VALID_MILLIS_TIME*/;
        if (!valid) {
            return Result.fail(ReturnCode.RC10206);
        }
        return Result.success();
    }
}
