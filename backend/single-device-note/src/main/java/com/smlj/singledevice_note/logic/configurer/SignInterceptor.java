package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.SignIgnore;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求签名拦截器：
 * 1. 从 query params 读取 __sign__ / __timestamp__ / __nonce__
 * 2. 校验 timestamp 是否在 5 分钟允许偏差内（防重放）
 * 3. 校验 nonce 是否已被使用（防重放）
 * 4. 用与前端相同的算法（HMAC-SHA256 + hex）重算 serverSign，与客户端 __sign__ 比对
 */
@Slf4j
@Component
public class SignInterceptor implements HandlerInterceptor {

    private static final long ALLOWED_TIME_DRIFT = 5 * 60 * 1000; // 5 分钟

    // 参数名（与前端 SignParamUtil.js 保持一致）
    private static final String PARAM_SIGN = "__sign__";
    private static final String PARAM_TIMESTAMP = "__timestamp__";
    private static final String PARAM_NONCE = "__nonce__";

    // nonce 缓存：防重放。value = 存入时间戳，用于过期清理
    private static final ConcurrentHashMap<String, Long> NONCE_CACHE = new ConcurrentHashMap<>();
    private static final long NONCE_EXPIRE = ALLOWED_TIME_DRIFT; // nonce 过期时间 = 时间偏差窗口

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) throws Exception {
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        try {
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }

            Method method = handlerMethod.getMethod();
            if (method.isAnnotationPresent(SignIgnore.class)) {
                return true;
            }

            // ---- 0. 无业务参数时直接放行（前端未附加签名） ----
            Map<String, String[]> rawParamMap = request.getParameterMap();
            boolean hasBusinessParam = false;
            for (Map.Entry<String, String[]> entry : rawParamMap.entrySet()) {
                if (PARAM_SIGN.equals(entry.getKey()) || PARAM_TIMESTAMP.equals(entry.getKey()) || PARAM_NONCE.equals(entry.getKey()))
                    continue;
                hasBusinessParam = true;
                break;
            }
            if (!hasBusinessParam) {
                return true; // 无业务参数，前端未签名，直接放行
            }

            // ---- 1. 读取签名参数 ----
            String sign = request.getParameter(PARAM_SIGN);
            String timestampStr = request.getParameter(PARAM_TIMESTAMP);
            String nonce = request.getParameter(PARAM_NONCE);

            if (sign == null || sign.isBlank() || timestampStr == null || timestampStr.isBlank() || nonce == null || nonce.isBlank()) {
                return reject(response, ResultCode.RC10401);
            }

            // ---- 2. timestamp 偏差校验 ----
            long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                return reject(response, ResultCode.RC10401);
            }
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > ALLOWED_TIME_DRIFT) {
                return reject(response, ResultCode.RC10403);
            }

            // ---- 3. nonce 防重放 ----
            cleanExpiredNonces(now);
            if (NONCE_CACHE.containsKey(nonce)) {
                return reject(response, ResultCode.RC10404);
            }
            NONCE_CACHE.put(nonce, now);

            // ---- 4. 重算签名 ----
            // 收集所有 query 参数（与前端 addSign 一致：过滤 __sign__，过滤 null/undefined）
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<String, String[]> entry : rawParamMap.entrySet()) {
                String key = entry.getKey();
                if (PARAM_SIGN.equals(key)) continue; // 签名本身不参与计算
                String[] vals = entry.getValue();
                if (vals == null || vals.length == 0) continue;
                String val = vals[0];
                if (val == null || val.isEmpty()) continue; // 前端过滤 null/undefined
                sorted.put(key, val);
            }

            // 拼接：key1=value1&key2=value2（按 key 字典序）
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                if (!sb.isEmpty()) sb.append('&');
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
            String queryString = sb.toString();

            // ---- 5. 比对签名 ----
            if (!SignUtil.verify(queryString, sign)) {
                log.warn("签名校验失败 | url={} | expect_query={} | sign={}", request.getRequestURI(), queryString, sign);
                return reject(response, ResultCode.RC10402);
            }

            return true;
        } catch (Exception e) {
            log.error("SignInterceptor preHandle error", e);
            try {
                response.getWriter().write(Result.fail(ResultCode.RC_1).toJson());
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    /**
     * 统一拒绝出口：写 JSON 响应并返回 false
     */
    private boolean reject(HttpServletResponse response, ResultCode rc) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(Result.fail(rc).toJson());
        return false;
    }

    /**
     * 清理过期的 nonce，防止内存无限增长
     */
    private void cleanExpiredNonces(long now) {
        NONCE_CACHE.entrySet().removeIf(e -> (now - e.getValue()) > NONCE_EXPIRE);
    }
}
