-- =====================================================================
-- 过磅日志监控器（gb-watcher）落库脚本
-- 位置：device-note 库 · gb_record 模式（不是独立的 gb_record 数据库！）
-- 执行方式：psql -h 10.8.54.24 -U postgres -d device-note -f gb_record-ddl.sql
-- 可重复执行（全部 if not exists）
-- =====================================================================

create schema if not exists gb_record;
set search_path to gb_record;

-- ---------------------------------------------------------------------
-- 1. gb_log：监控器上报的原始日志行（一行一条）
--    幂等键 (source, fp)：fp = MD5(日志文件完整路径|行号|行内容) 前 16 位，
--    监控器重启重报/崩溃窗口重复上报都靠它去重。
-- ---------------------------------------------------------------------
create table if not exists gb_log (
    id           bigserial primary key,
    source       varchar(32)  not null,              -- 磅号（监控器「磅号」配置，如 1 / 2 / GB01）
    fp           varchar(32)  not null,              -- 指纹，幂等键
    type         varchar(2),                         -- 日志行原始标记：[M] 手动 / [A] 自动
    log_time     timestamp,                          -- 日志行时间（解析自 yyyy/M/d H:mm:ss）
    content      text,                               -- -> 之后的行内容
    create_time  timestamp(6) not null default now(),-- 入库时间
    constraint uq_gb_log unique (source, fp)
);

create index if not exists idx_gb_log_time        on gb_log (log_time);
create index if not exists idx_gb_log_source_time on gb_log (source, log_time desc);

-- ---------------------------------------------------------------------
-- 2. gb_weigh_event：从原始行解析出的过磅事件（单号 + 车牌）
--    解析规则：content 匹配「自动数据保存成功！单号[x],车牌号[y]」
--    同一单号一进一出为两行 event_time；前端按单号聚合 in/out。
--    幂等键 (source, order_no, event_time)。
-- ---------------------------------------------------------------------
create table if not exists gb_weigh_event (
    id           bigserial primary key,
    source       varchar(32)  not null,              -- 磅号
    fp           varchar(32),                        -- 来源原始行指纹（溯源用）
    order_no     varchar(64)  not null,              -- 单号，如 WH2026102500199
    vehicle_no   varchar(32),                        -- 车牌号，如 陕KM9368
    event_time   timestamp    not null,              -- 过磅时间
    create_time  timestamp(6) not null default now(),
    constraint uq_gb_event unique (source, order_no, event_time)
);

create index if not exists idx_gb_event_order  on gb_weigh_event (order_no);
create index if not exists idx_gb_event_time   on gb_weigh_event (event_time);
create index if not exists idx_gb_event_source on gb_weigh_event (source, event_time desc);

-- ---------------------------------------------------------------------
-- 3. gb_combine：关键异常组合记录（车牌/单号 + 异常分类 + 原文）
--    上报链路同步生成：一行原始日志命中 7 类关键异常之一即写一条，
--    幂等键 (source, fp) 与 gb_log 一一对应，查询时按 (source, fp) 联回。
--    车牌识别规则：车牌号[陕KM8567] / 1#[宁AD3107]无效；
--    无车牌的异常（超出载重范围/主键重复/不可重复称/重量小于0）只带单号。
-- ---------------------------------------------------------------------
create table if not exists gb_combine (
    id            bigserial primary key,
    source        varchar(32)  not null,              -- 磅号
    fp            varchar(32)  not null,              -- 来源原始行指纹（溯源 + 联表键）
    vehicle_no    varchar(32),                        -- 识别到的车牌号（可能为空）
    order_no      varchar(64),                        -- 单号（可能为空）
    abnormal_type varchar(32)  not null,              -- 异常分类（7 类之一）
    content       varchar(1000) not null,             -- 异常原文（组合信息的正文）
    log_time      timestamp,                          -- 日志行时间
    create_time   timestamp(6) not null default now(),
    constraint uq_gb_combine unique (source, fp)
);

create index if not exists idx_gb_combine_source_time on gb_combine (source, log_time desc);
create index if not exists idx_gb_combine_vehicle     on gb_combine (vehicle_no);

-- 查询示例（其他客户端/程序里注意带模式前缀）：
--   select * from gb_record.gb_log order by id desc limit 20;
--   select * from gb_record.gb_weigh_event order by id desc limit 20;
--   select order_no, min(event_time) 进磅, max(event_time) 出磅, count(*) 次数
--     from gb_record.gb_weigh_event where order_no = 'WH2026102700030' group by order_no;
