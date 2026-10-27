-- nav.plant 增加地磅查询条数配置（2026-10-27）
-- vehicle_weigh_count: 车牌过磅记录查询条数（默认 6）
-- scale_log_count:     磅房过磅日志查询条数（默认 50）
ALTER TABLE nav.plant ADD COLUMN IF NOT EXISTS vehicle_weigh_count int NOT NULL DEFAULT 6;
ALTER TABLE nav.plant ADD COLUMN IF NOT EXISTS scale_log_count int NOT NULL DEFAULT 50;
