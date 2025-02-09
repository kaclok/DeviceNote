package com.smlj.singledevice_note.core.o.to;

import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "协议交互,后端给前端返回的业务结构")
public class Msg<T> {
    @Schema(description = "协议码")
    protected int code = 0;

    @Schema(description = "业务逻辑返回数据")
    protected T data = null;

    public String toJson() {
        return JSONUtil.toJsonStr(this);
    }

    public Msg(int code, T data) {
        this.code = code;
        this.data = data;
    }
}
