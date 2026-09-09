package com.smlj.singledevice_note.core.o.to;

import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true) // 链式
@Schema(description = "协议交互,后端给前端返回的业务结构")
public class Msg<T> {
    @Schema(description = "协议码")
    protected int code = 0;

    @Schema(description = "业务逻辑返回数据")
    protected T data = null;

    /**
     * 响应签名: data 规范 JSON 文本的 HMAC-SHA256(hex)。
     * 仅在全局开关 sign.response-enabled=true 且成功返回(data 非空、无 @SignIgnore)时由 ResponseSignAdvice 填充。
     * 为 null 时因全局 jackson NON_EMPTY 配置不会序列化输出，不影响旧接口。
     */
    @Schema(description = "响应签名(data 规范 JSON 的 HMAC-SHA256), 未开启响应签名时为 null")
    protected String sign = null;

    public String toJson() {
        return JSONUtil.toJsonStr(this);
    }

    public Msg(int code, T data) {
        this.code = code;
        this.data = data;
    }
}
