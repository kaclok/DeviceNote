# 过磅日志监控 · 对接设计（LogUploader → plantmap）

> 监控端：`tool/log-uploader/`（WinForms，常驻过磅电脑）
> 本文定义监控端 → 服务端的上报契约、服务端建表与解析规则、plantmap 前端展示方案。

## 1. 数据流

```
过磅软件 ──写──▶ 日志根目录\yyyyMM\yyyyMMdd.txt
                    │ FileSystemWatcher + Tail(断点续读)
                    ▼
        LogUploader.exe（磅点编号随数据上报）
                    │ POST /api/gb/log（批量，失败重试）
                    ▼
        plantmap 后端 ── 原始行表 gb_log_line（全量留痕）
                       ├ 过磅事件表 gb_weigh_event（解析「自动数据保存成功」）
                       └ 车辆预约表 gb_vehicle_receipt（解析「过磅车辆判断信息」JSON，可选）
                    ▼
        plantmap 前端「查看磅」：按单号聚合展示出入场时间线
```

## 2. 上报契约（监控端 → 服务端）

`POST {server}/api/gb/log`，Content-Type: application/json：

```json
{
  "source": "GB01",
  "lines": [
    { "fp": "66139e904360430b", "no": 12, "type": "A",
      "time": "2026/10/26 0:10:05",
      "content": "自动数据保存成功！单号[WH2026102500199],车牌号[陕KM9368]",
      "file": "202610\\20261026.txt" }
  ]
}
```

- `fp`：MD5(完整路径|行号|内容) 前 16 位，**幂等键**。服务端按 `(source, fp)` 唯一约束，重复插入直接忽略。
- 批量大小不定（监控端攒批 ~秒级 flush），服务端需支持空 lines 直接返回 ok。
- 响应：HTTP 200 即认为成功（监控端只看状态码）；非 2xx 监控端 10s 后重试整批。

## 3. 服务端建表（以 PostgreSQL 为例，其他库同构）

```sql
-- 原始行（全量留痕，审计与追溯）
CREATE TABLE gb_log_line (
    id         bigserial PRIMARY KEY,
    source     varchar(32)  NOT NULL,             -- 磅点编号
    log_file   varchar(64)  NOT NULL,             -- 202610\20261026.txt
    line_no    int          NOT NULL,             -- 文件内行号
    fp         varchar(32)  NOT NULL,             -- 幂等指纹
    line_type  varchar(2)   NOT NULL DEFAULT '',  -- M=消息 A=动作
    line_time  timestamp    NULL,                 -- 行内时间（以日志为准）
    content    text         NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_gb_line UNIQUE (source, fp)
);
CREATE INDEX idx_gb_line_time ON gb_log_line (source, line_time);
CREATE INDEX idx_gb_line_content ON gb_log_line (source, content_pattern_ops); -- 车牌/单号 like 查询

-- 过磅事件（一行 = 一次「自动数据保存成功」，同单号出入场各一条）
CREATE TABLE gb_weigh_event (
    id         bigserial PRIMARY KEY,
    source     varchar(32)  NOT NULL,
    order_no   varchar(32)  NOT NULL,             -- WH2026102500199
    plate      varchar(16)  NOT NULL,             -- 陕KM9368
    event_time timestamp    NOT NULL,             -- 该行日志时间
    direction  varchar(8)   NOT NULL DEFAULT '',  -- 入口 / 出口（见解析规则）
    raw_line_id bigint     NULL,                 -- 关联原始行
    CONSTRAINT uq_gb_event UNIQUE (source, order_no, event_time)
);
CREATE INDEX idx_gb_event_order ON gb_weigh_event (order_no);
CREATE INDEX idx_gb_event_plate ON gb_weigh_event (plate, event_time);

-- 车辆预约信息（解析 M 行 JSON，可选但推荐：前端展示货物/收发货单位/最大载重）
CREATE TABLE gb_vehicle_receipt (
    id          bigserial PRIMARY KEY,
    source      varchar(32) NOT NULL,
    plate       varchar(16) NOT NULL,
    receipt_id  varchar(32) NOT NULL,             -- JSON 里的 receiptId
    car_number  varchar(16),
    goods_name  varchar(64),                      -- 烧碱（50）/PVC/电石渣...
    goods_spec  varchar(32),
    consignee   varchar(128),                     -- 收货单位
    forwarding  varchar(128),                     -- 发货单位
    transport   varchar(128),                     -- 承运单位
    max_load    numeric(10,2),
    is_gb       int,
    line_time   timestamp    NOT NULL,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_gb_receipt UNIQUE (source, receipt_id, line_time)
);
```

## 4. 后端解析规则（按行处理，全部幂等）

| 行内容 | 动作 |
|---|---|
| 全部行 | 插入 `gb_log_line`，fp 冲突则跳过 |
| `自动数据保存成功！单号[X],车牌号[Y]`（正则 `单号\[(.+?)\],车牌号\[(.+?)\]`） | 插入 `gb_weigh_event`；direction 取同一秒相邻行的「等待入口/出口车辆上磅」，取不到留空 |
| `过磅车辆判断信息[{json}]...`（截取首个 `{` 到 `}]` 之间的 JSON） | 解析 `data.result` 插入 `gb_vehicle_receipt` |
| 其余行（等待确认车牌号/车辆已下磅/无效等） | 只进原始行表 |

注意：同一单号会出现两次（入口磅 + 出口磅，如 WH2026102600003 在 1:46 和 2:21），事件表按 `(source, order_no, event_time)` 唯一即可，**前端按单号聚合**得到完整出入场对。

## 5. plantmap 前端「查看磅」展示方案

**查询接口**（服务端提供）：
- `GET /api/gb/records?date=2026-10-26&plate=&page=` → 当日过磅记录列表（按 order_no 聚合：单号、车牌、入口时间、出口时间、货物、收发货单位）
- `GET /api/gb/timeline?orderNo=WH2026102600003&source=GB01` → 该单号时间线：取该单号两次事件时间之间的所有原始行（或按车牌 ±30 分钟窗），按时间排序返回

**聚合 SQL 思路**（记录列表）：

```sql
SELECT order_no, plate,
       MIN(event_time) FILTER (WHERE direction = '入口') AS in_time,
       MAX(event_time) FILTER (WHERE direction = '出口') AS out_time
FROM gb_weigh_event
WHERE event_time::date = :date
GROUP BY order_no, plate
ORDER BY MIN(event_time) DESC;
```

**前端页面结构**（司机/门卫点「查看磅」）：
1. **过磅记录表**：单号 | 车牌 | 货物 | 入场时间 | 出场时间 | 收货单位 | 承运单位（来自 gb_weigh_event + gb_vehicle_receipt 按 plate+时间邻接关联）
2. **点击单号 → 时间线弹层**：vertical timeline 渲染该单号相关日志行（`[M]`灰色消息 / `[A]`青色动作，「自动数据保存成功」高亮），数据来自 timeline 接口
3. 支持按车牌搜索该车牌的全部历史过磅

## 6. 监控端部署与运维

1. 拷贝 `LogUploader.exe` 到过磅电脑任意目录，双击运行；
2. 配置日志根目录（过磅软件日志目录）、plantmap 后端地址、磅点编号 → 点「开始监控」，配置自动持久化（%APPDATA%\DevLauncher\log-uploader.ini）；
3. 断点状态存 `%APPDATA%\DevLauncher\log-uploader-state.ini`，重启续读不重不漏；
4. 首次部署当日已存在的日志会被跳过（只上报部署之后新增的行）；如需补历史，删掉 state.ini 重启并把 SkipHistory 逻辑改为从 0 读（目前默认跳过，避免灌入整日历史）；
5. 建议加开机自启（快捷方式放 shell:startup）。

## 7. 已知边界

- 监控端只认 `*.txt`、格式 `[X]yyyy/MM/dd H:mm:ss->内容`；不匹配的行原样上报（type/time 为空），服务端只入原始行表——**格式变化不会丢数据**；
- 日志编码按 GBK 读取（中文 Windows 过磅软件惯例）；若某天换成 UTF-8，中文会乱码但指纹仍唯一，不会重复上报，改监控端一行常量即可；
- 上报失败时数据保存在内存缓冲（进程不重启就不丢）；进程级灾难（断电）依赖 Offset 持久化（5s 落盘一次），最多丢最后 5s 未消费的行——但这些行仍躺在日志文件里，可通过「删 state.ini + 补历史模式」重读。
