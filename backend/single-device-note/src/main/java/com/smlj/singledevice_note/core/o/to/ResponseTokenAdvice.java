package com.smlj.singledevice_note.core.o.to;

import com.fasterxml.jackson.databind.JsonNode;
import com.smlj.singledevice_note.core.TokenRefresher;
import com.smlj.singledevice_note.core.annotation.SignIgnore;
import com.smlj.singledevice_note.core.o.dto.TokenPair;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.core.utils.SignUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ResponseTokenAdvice implements ResponseBodyAdvice<Object> {
    @Value("${token.need-refresh:false}")
    private boolean needRefresh;

    private final TokenRefresher tokenRefresher; // 外部实现该抽象接口即可

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return needRefresh;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body instanceof Result<?> result && result.isNeedRefreshAT()) {
            TokenPair pair = tokenRefresher.refresh();
            if (pair != null) {
                JwtUtil.setAccessTokenHeader(response, pair.getAt());
                JwtUtil.setRefreshTokenHeader(response, pair.getRt());
            }
        }
        return body;
    }

    private boolean isSignIgnored(MethodParameter returnType) {
        var method = returnType.getMethod();
        if (method == null) {
            return false;
        }
        return method.isAnnotationPresent(SignIgnore.class)
                || method.getDeclaringClass().isAnnotationPresent(SignIgnore.class);
    }

    // ---------------- 规范文本 ----------------

    private String canonical(JsonNode node) {
        if (node == null || node.isNull()) {
            return "null";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "true" : "false";
        }
        if (node.isNumber()) {
            return numberText(node);
        }
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder("[");
            Iterator<JsonNode> it = node.elements();
            boolean first = true;
            while (it.hasNext()) {
                if (!first) sb.append(',');
                sb.append(canonical(it.next()));
                first = false;
            }
            return sb.append(']').toString();
        }
        // 对象: 键按字典序升序
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String k : keys) {
            if (!first) sb.append(',');
            sb.append(k).append(':').append(canonical(node.get(k)));
            first = false;
        }
        return sb.append('}').toString();
    }

    private String numberText(JsonNode node) {
        if (node.isIntegralNumber()) {
            // 整数直接用十进制(业务侧应保证在 JS Number 安全整数内)
            return node.bigIntegerValue().toString();
        }
        double d = node.doubleValue();
        // 浮点整值归一为整数文本, 与 JS Number.isInteger 语义对齐
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        // 浮点: 去掉尾零的十进制(避免 1.0/0.10 这类与 JS 不一致的表示)
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }
}
