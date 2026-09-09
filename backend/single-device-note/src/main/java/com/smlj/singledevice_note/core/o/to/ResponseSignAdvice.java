package com.smlj.singledevice_note.core.o.to;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlj.singledevice_note.core.annotation.SignIgnore;
import com.smlj.singledevice_note.core.utils.SignUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 响应签名切面(全局开关 sign.response-enabled 控制)：
 * 对每个成功响应(Result.code=200 且 data 非空、方法/类未标 @SignIgnore)，
 * 将 data 序列化后做「规范 JSON 文本」(key 字典序排序、数值整数化/去尾零、字符串与结构原样)，
 * 取 HMAC-SHA256(hex) 写入 Result 顶层 sign 字段。
 * <p>
 * 前端(AxiosInst 响应拦截器, Config.js verify_response_sign=true)会用同一算法对
 * JSON.parse 后的 data 重算 sign 并比对, 防响应数据被中间人篡改。
 * 开关关闭时不进入本切面, sign 保持 null, 因全局 jackson NON_EMPTY 配置不会输出该字段。
 * <p>
 * 与请求验签的差异: 请求签名基于「key=value 扁平拼接」(query 天然扁平);
 * 响应签名基于「任意嵌套 JSON」, 因此定义统一规范文本, 两端实现需保持一致:
 * 1. 对象: 键按 UTF-16 字典序升序, 输出 {k:v,k2:v2}(键原样、不加引号)
 * 2. 数组: 按序输出 [v1,v2]
 * 3. 字符串: 原样输出(不转义); 布尔: true/false; null: null
 * 4. 数字: 整数值输出十进制整数; 浮点先做整值归一, 再 stripTrailingZeros 后以十进制输出,
 *    保证与 JS Number 序列化一致(超 2^53 或极小/极大指数值不在约定范围内, 业务应避免)
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ResponseSignAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    /** 全局开关: application.yml 的 sign.response-enabled, 与前端 Config.js verify_response_sign 同步 */
    @Value("${sign.response-enabled:false}")
    private boolean responseSignEnabled;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!responseSignEnabled) {
            return false;
        }
        // 只在 JSON 消息转换器且返回 Result 类型时处理
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)
                && Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof Result<?> result)) {
            return body;
        }
        // 只对成功且带 data 的响应签名; 失败响应/无 data 不签
        if (!Result.isSuccess(result.getCode()) || result.getData() == null) {
            return body;
        }
        // 尊重 @SignIgnore(类级/方法级): 与请求验签相同的豁免语义
        if (isSignIgnored(returnType)) {
            return body;
        }

        try {
            // 先序列化再反解析: 复用同一 ObjectMapper, 确保数值/日期/空值过滤与最终 HTTP body 输出完全一致,
            // 树节点形态 == 前端 JSON.parse 得到的对象形态
            byte[] jsonBytes = objectMapper.writeValueAsBytes(result.getData());
            String content = canonical(objectMapper.readTree(jsonBytes));
            result.setSign(SignUtil.sign(content));
        } catch (Exception e) {
            // 签名失败只记日志, 不阻断正常业务返回(签名是附加防护)
            log.warn("响应签名失败 | url={} | msg={}", request.getURI().getPath(), e.getMessage());
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
