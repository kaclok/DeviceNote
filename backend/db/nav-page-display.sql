/*
 切页显示方向配置：每个「切页」一行，orientation 用数值记录三种状态。

 0 = 跟随手机横竖屏（默认，页面按当前视口方向排版，不做旋转）
 1 = 始终横屏（手机上竖着拿时整页旋转 90° 按横屏排版，厂区寻路即此档）
 2 = 始终竖屏（始终用竖屏排版，不做旋转）

 说明：Web 端无法真正锁定系统方向，1 档的做法是把整层壳旋转 90°，
 使用者把手机横过来即可正向阅读；0/2 档都不旋转，区别在排版用哪一套。
*/
CREATE TABLE IF NOT EXISTS "nav"."page_display"
(
    "plant_id"    varchar(32) NOT NULL,
    "page_code"   varchar(32) NOT NULL,
    "orientation" int2        NOT NULL DEFAULT 0,
    "sort_no"     int4        NOT NULL DEFAULT 0,
    CONSTRAINT "page_display_pkey" PRIMARY KEY ("plant_id", "page_code")
);
COMMENT ON COLUMN "nav"."page_display"."page_code" IS '切页标识，与前端路由 name 一致：home_map=厂区寻路 home_gb=地磅查询 home_setting=配置';
COMMENT ON COLUMN "nav"."page_display"."orientation" IS '显示方向：0=跟随手机横竖屏 1=始终横屏 2=始终竖屏';
COMMENT ON COLUMN "nav"."page_display"."sort_no" IS '配置页展示顺序';
COMMENT ON TABLE "nav"."page_display" IS '各切页的屏幕显示方向配置';

INSERT INTO "nav"."page_display" ("plant_id", "page_code", "orientation", "sort_no")
VALUES ('default', 'home_map', 1, 1),
       ('default', 'home_gb', 0, 2),
       ('default', 'home_setting', 0, 3)
ON CONFLICT ("plant_id", "page_code") DO NOTHING;
