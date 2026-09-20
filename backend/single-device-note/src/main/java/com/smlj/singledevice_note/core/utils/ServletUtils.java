package com.smlj.singledevice_note.core.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

// 用于请求参数处理，用于诊断每个请求的参数
// ruoyi-vue-pro\yudao-framework\yudao-common\src\main\java\cn\iocoder\yudao\framework\common\\util\servlet\ServletUtils.java
// public class ServletUtils {
//    /**
//     * 返回 JSON 字符串
//     *
//     * @param response 响应
//     * @param object   对象，会序列化成 JSON 字符串
//     */
//    @SuppressWarnings("deprecation") // 必须使用 APPLICATION_JSON_UTF8_VALUE，否则会乱码
//    public static void writeJSON(HttpServletResponse response, Object object) {
//        String content = JSONUtil.toJsonStr(object);
//        ServletUtil.write(response, content, MediaType.APPLICATION_JSON_UTF8_VALUE);
//    }
//
//    public static boolean isJsonRequest(ServletRequest request) {
//        return StrUtil.startWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE);
//    }
//
//    /**
//     * @param request 请求
//     * @return ua
//     */
//    public static String getUserAgent(HttpServletRequest request) {
//        String ua = request.getHeader("User-Agent");
//        return ua != null ? ua : "";
//    }
//
//    /**
//     * 获得请求
//     *
//     * @return HttpServletRequest
//     */
//    public static HttpServletRequest getRequest() {
//        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//        if (!(requestAttributes instanceof ServletRequestAttributes)) {
//            return null;
//        }
//        return ((ServletRequestAttributes) requestAttributes).getRequest();
//    }
//
//    // 获取请求body参数列表
//    public static String getBody(HttpServletRequest request) {
//        // 只有在 json 请求在读取，因为只有 CacheRequestBodyFilter 才会进行缓存，支持重复读取
//        if (isJsonRequest(request)) {
//            return ServletUtil.getBody(request);
//        }
//        return null;
//    }
//
//    public static String getClientIP(HttpServletRequest request) {
//        return ServletUtil.getClientIP(request);
//    }
//
//    // 获取请求query参数列表
//    public static Map<String, String> getParamMap(HttpServletRequest request) {
//        return ServletUtil.getParamMap(request);
//    }
//
//    // 获取请求headers列表
//    public static Map<String, String> getHeaderMap(HttpServletRequest request) {
//        return cn.hutool.extra.servlet.ServletUtil.getHeaderMap(request);
//    }
// }
