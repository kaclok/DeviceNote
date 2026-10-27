-- nav.scale 加「关联磅号」：对应过磅日志监控器（gb-watcher）绑定的磅号 source，
-- 地磅查询页按此字段把磅房与 gb_record.gb_log.source 关联起来。可重复执行。
ALTER TABLE "nav"."scale" ADD COLUMN IF NOT EXISTS "gb_no" varchar(32);
COMMENT ON COLUMN "nav"."scale"."gb_no" IS '关联磅号（gb-watcher 上报 source），空=未绑定';
