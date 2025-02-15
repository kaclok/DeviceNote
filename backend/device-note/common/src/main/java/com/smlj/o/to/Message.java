package com.smlj.o.to;

import com.smlj.utils.DateTimeUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Schema(description = "协议交互,后端给前端返回的业务结构")
public class Message<T> extends Msg<T> {
    @Schema(description = "业务发生的时间戳")
    protected long timestamp = 0;

    @Schema(description = "业务发生的时区")
    protected int zoneOffset = 0;

    public Message(int code, T data) {
        super(code, data);
        this.setTime();
    }

    public void setTime() {
        this.timestamp = DateTimeUtil.nowTimestamp(true);
        this.zoneOffset = DateTimeUtil.getZoneOffset();
    }
}
