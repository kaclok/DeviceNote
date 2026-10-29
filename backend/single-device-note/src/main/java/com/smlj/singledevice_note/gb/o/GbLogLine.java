package com.smlj.singledevice_note.gb.o;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 过磅日志监控器（LogUploader）上报的一条原始日志行。
 * POST /api/gb/log 请求体是本对象的数组：
 * [{"no":1,"type":"A","time":"2026/10/26 0:10:05","content":"...","fp":"66139e904360430b"},...]
 * 其中 no 为监控器配置的磅号（JSON 里是数字，按 String 接收以兼容 "GB01" 这类配置）；
 * fp 为监控器算的幂等指纹 = MD5(日志文件完整路径|行号|行内容) 前 16 位。
 */
@Data
public class GbLogLine {
    /** 磅号（= 库表 gb_log.source） */
    private String no;
    /** 日志行原始标记：M / A */
    private String type;
    /** 日志行时间原文（yyyy/M/d H:mm:ss，小时/月不补零） */
    private String time;
    /** -> 之后的行内容 */
    private String content;
    /** 幂等指纹，(source, fp) 唯一 */
    private String fp;

    /** ---- 以下为服务端解析/回查用字段，不参与请求反序列化 ---- */
    /** 库表自增主键 */
    private Long id;
    /** 解析后的日志时间 */
    private LocalDateTime logTime;
    /** 入库时间 */
    private LocalDateTime createTime;
    /** 命中的关键异常组合记录（查询时按 source+fp 联表带出，上报链路不写） */
    private GbCombine combine;
}
