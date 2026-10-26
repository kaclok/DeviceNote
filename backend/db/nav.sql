/*
 Navicat Premium Dump SQL

 Source Server         : Postgres
 Source Server Type    : PostgreSQL
 Source Server Version : 170002 (170002)
 Source Host           : localhost:5432
 Source Catalog        : device-note
 Source Schema         : nav

 Target Server Type    : PostgreSQL
 Target Server Version : 170002 (170002)
 File Encoding         : 65001

 Date: 26/10/2026 08:51:11
*/


-- ----------------------------
-- Table structure for gate
-- ----------------------------
DROP TABLE IF EXISTS "nav"."gate";
CREATE TABLE "nav"."gate" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plant_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "node_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "gate_mode" varchar(24) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'both'::character varying,
  "sort_no" int4 NOT NULL DEFAULT 0,
  "allowed_material_ids" varchar(512) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "nav"."gate"."gate_mode" IS '通行方向：both=双向通行 in=仅进厂（禁止驶出） out=仅出厂（禁止驶入）';
COMMENT ON TABLE "nav"."gate" IS '大门通行规则，与 nav.node 中 type=gate 的点位一一对应';

-- ----------------------------
-- Records of gate
-- ----------------------------
INSERT INTO "nav"."gate" VALUES ('gate_gate_w', 'default', 'gate_w', 'both', 1, '["m_lye","m_hcl","m_pvc","m_slag"]');
INSERT INTO "nav"."gate" VALUES ('gate_gate_n', 'default', 'gate_n', 'both', 2, '["m_carbide","m_coal"]');

-- ----------------------------
-- Table structure for material
-- ----------------------------
DROP TABLE IF EXISTS "nav"."material";
CREATE TABLE "nav"."material" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plant_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "direction" varchar(8) COLLATE "pg_catalog"."default" NOT NULL,
  "category" varchar(50) COLLATE "pg_catalog"."default",
  "scale_id" varchar(64) COLLATE "pg_catalog"."default",
  "backup_scale_ids" text COLLATE "pg_catalog"."default",
  "point_node_id" varchar(64) COLLATE "pg_catalog"."default",
  "enabled" int2 NOT NULL DEFAULT 1,
  "sort_no" int4 NOT NULL DEFAULT 0,
  "voice_text" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of material
-- ----------------------------
INSERT INTO "nav"."material" VALUES ('m_lye', 'default', 'WZ01', '烧碱', 'out', '液碱类', 's1', '[]', 'm_lye', 1, 1, '烧碱，学名氢氧化钠，也叫火碱、苛性钠，液碱。');
INSERT INTO "nav"."material" VALUES ('m_hcl', 'default', 'WZ02', '盐酸', 'out', '酸类', 's1', '[]', 'm_hcl', 1, 2, '盐酸，学名氯化氢水溶液，也叫氢氯酸。');
INSERT INTO "nav"."material" VALUES ('m_pvc', 'default', 'WZ03', 'PVC', 'out', '成品树脂', 's4', '["s1"]', 'm_pvc', 1, 3, 'PVC，学名聚氯乙烯，吨包，也常被叫作树脂粉。白色粉末或颗粒.');
INSERT INTO "nav"."material" VALUES ('m_slag', 'default', 'WZ04', '电石渣', 'out', '固废', 's4', '[]', 'm_slag', 1, 4, '电石渣，也叫渣浆、干渣。装车时请听从现场人员指挥。');
INSERT INTO "nav"."material" VALUES ('m_carbide', 'default', 'WZ05', '电石', 'in', '原料', 's8', '[]', 'm_carbide', 1, 5, '电石，学名碳化钙，遇水会产生乙炔气体，属于危险品。');
INSERT INTO "nav"."material" VALUES ('m_coal', 'default', 'WZ06', '煤', 'in', '原料', 's8', '[]', 'm_coal', 1, 6, '煤');

-- ----------------------------
-- Table structure for node
-- ----------------------------
DROP TABLE IF EXISTS "nav"."node";
CREATE TABLE "nav"."node" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plant_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "x" numeric(10,2) NOT NULL,
  "y" numeric(10,2) NOT NULL,
  "status" int2 NOT NULL DEFAULT 1,
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "sort_no" int4 NOT NULL DEFAULT 0,
  "visible" bool NOT NULL DEFAULT true
)
;
COMMENT ON COLUMN "nav"."node"."status" IS '点位状态：1=正常 2=检修 3=停用；仅磅房节点有意义，其它类型固定为1';

-- ----------------------------
-- Records of node
-- ----------------------------
INSERT INTO "nav"."node" VALUES ('gate_w', 'default', 'G1', '4#门', 'gate', 47.00, 207.60, 1, '', 10, 't');
INSERT INTO "nav"."node" VALUES ('gate_n', 'default', 'G2', '5#门', 'gate', 256.50, 42.30, 1, '', 20, 't');
INSERT INTO "nav"."node" VALUES ('n_mvknd5up', 'default', 'c8', 'C8', 'junction', 254.50, 100.20, 1, '', 26, 't');
INSERT INTO "nav"."node" VALUES ('n_mvknkw9u', 'default', 'c12', 'C12', 'junction', 517.70, 433.70, 1, '', 27, 't');
INSERT INTO "nav"."node" VALUES ('n_mvko2xng', 'default', 'c9', 'C9', 'junction', 67.10, 209.30, 1, '', 28, 't');
INSERT INTO "nav"."node" VALUES ('n_mvko5jb1', 'default', 'c10', 'C10', 'junction', 1055.10, 433.50, 1, '', 29, 't');
INSERT INTO "nav"."node" VALUES ('n_mvkoc3j6', 'default', 'c11', 'C11', 'junction', 118.10, 216.50, 1, '', 30, 't');
INSERT INTO "nav"."node" VALUES ('n_mvoi1tno', 'default', 'c13', 'C13', 'junction', 255.90, 73.20, 1, '', 31, 't');
INSERT INTO "nav"."node" VALUES ('j1', 'default', 'J1', 'C1', 'junction', 70.10, 60.80, 1, '', 101, 't');
INSERT INTO "nav"."node" VALUES ('j2', 'default', 'J2', 'C4', 'junction', 249.30, 296.90, 1, '', 102, 't');
INSERT INTO "nav"."node" VALUES ('j4', 'default', 'J4', 'C3', 'junction', 61.70, 431.00, 1, '', 104, 't');
INSERT INTO "nav"."node" VALUES ('j5', 'default', 'J5', 'C5', 'junction', 241.50, 436.00, 1, '', 105, 't');
INSERT INTO "nav"."node" VALUES ('j7', 'default', 'J7', 'C6', 'junction', 769.60, 294.50, 1, '', 107, 't');
INSERT INTO "nav"."node" VALUES ('j8', 'default', 'J8', 'C2', 'junction', 65.90, 293.90, 1, '', 108, 't');
INSERT INTO "nav"."node" VALUES ('j9', 'default', 'J9', 'C7', 'junction', 771.40, 434.90, 1, '', 109, 't');
INSERT INTO "nav"."node" VALUES ('s1', 'default', 'B1', '1#磅', 'scale', 1106.20, 435.80, 1, '', 201, 't');
INSERT INTO "nav"."node" VALUES ('s2', 'default', 'B2', '2#磅', 'scale', 108.80, 428.60, 2, '东西主干道东段', 202, 't');
INSERT INTO "nav"."node" VALUES ('s3', 'default', 'B3', '3#磅', 'scale', 72.70, 236.30, 3, '', 203, 't');
INSERT INTO "nav"."node" VALUES ('s4', 'default', 'B4', '4#磅', 'scale', 91.00, 217.90, 1, '', 204, 't');
INSERT INTO "nav"."node" VALUES ('s5', 'default', 'B5', '5#磅', 'scale', 89.90, 164.10, 3, '', 205, 't');
INSERT INTO "nav"."node" VALUES ('s6', 'default', 'B6', '6#磅', 'scale', 115.60, 68.50, 3, '', 206, 't');
INSERT INTO "nav"."node" VALUES ('s7', 'default', 'B7', '7#磅', 'scale', 231.90, 91.00, 2, '', 207, 't');
INSERT INTO "nav"."node" VALUES ('s8', 'default', 'B8', '8#磅', 'scale', 309.80, 55.60, 1, '', 208, 't');
INSERT INTO "nav"."node" VALUES ('m_lye', 'default', 'M1', '烧碱装车点', 'pickup', 1042.90, 421.60, 1, '烧碱（液碱）拉货装车', 301, 't');
INSERT INTO "nav"."node" VALUES ('m_hcl', 'default', 'M2', '盐酸装车点', 'pickup', 1042.20, 397.20, 1, '盐酸拉货装车', 302, 't');
INSERT INTO "nav"."node" VALUES ('m_pvc', 'default', 'M3', 'PVC装车点', 'pickup', 529.80, 467.00, 1, '', 303, 't');
INSERT INTO "nav"."node" VALUES ('m_slag', 'default', 'M4', '电石渣装车点', 'pickup', 149.60, 244.50, 1, '电石渣拉货装车', 304, 't');
INSERT INTO "nav"."node" VALUES ('m_carbide', 'default', 'M5', '电石卸车点', 'delivery', 277.50, 117.80, 1, '电石送货卸车', 305, 't');
INSERT INTO "nav"."node" VALUES ('m_coal', 'default', 'M6', '卸煤点', 'delivery', 90.70, 110.30, 1, '', 306, 't');
INSERT INTO "nav"."node" VALUES ('p1', 'default', 'P1', '1#停车场', 'parking', 172.90, 73.40, 1, '煤、电石停车场', 401, 't');
INSERT INTO "nav"."node" VALUES ('p2', 'default', 'P2', '2#停车场', 'parking', 1171.40, 444.80, 1, '南门内侧待卸车辆停车场', 402, 't');

-- ----------------------------
-- Table structure for plant
-- ----------------------------
DROP TABLE IF EXISTS "nav"."plant";
CREATE TABLE "nav"."plant" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "image_url" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "image_width" int4 NOT NULL,
  "image_height" int4 NOT NULL,
  "meters_per_pixel" numeric(10,4) NOT NULL DEFAULT 0.9,
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "update_time" timestamp(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Records of plant
-- ----------------------------
INSERT INTO "nav"."plant" VALUES ('default', '陕西金泰化学神木氯碱有限公司厂区图', '/plantmap/plant.png', 1280, 558, 0.9000, '坐标基于卫星平面图估算，可在「地图配置」中拖拽修正', '2026-10-26 08:34:22.150038');

-- ----------------------------
-- Table structure for road
-- ----------------------------
DROP TABLE IF EXISTS "nav"."road";
CREATE TABLE "nav"."road" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plant_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "group_name" varchar(100) COLLATE "pg_catalog"."default",
  "level" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'secondary'::character varying,
  "from_node_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "to_node_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "points" text COLLATE "pg_catalog"."default" NOT NULL,
  "one_way" int2 NOT NULL DEFAULT 0,
  "enabled" int2 NOT NULL DEFAULT 1,
  "sort_no" int4 NOT NULL DEFAULT 0
)
;

-- ----------------------------
-- Records of road
-- ----------------------------
INSERT INTO "nav"."road" VALUES ('r_jing_er_1', 'default', '', '经二路·北段', '新增道路', 'main', 'gate_n', 'n_mvoi1tno', '[[256.5,42.3],[255.9,73.2]]', 0, 1, 1);
INSERT INTO "nav"."road" VALUES ('r_mvkegqqu', 'default', '', '纬一路·西段', '新增道路', 'secondary', 'j4', 's2', '[[61.7,431],[108.8,428.6]]', 0, 1, 1);
INSERT INTO "nav"."road" VALUES ('r_jing_er_2', 'default', '', '经二路·中段', '新增道路', 'main', 'n_mvknd5up', 'j2', '[[254.5,100.2],[249.3,296.9]]', 0, 1, 2);
INSERT INTO "nav"."road" VALUES ('r_mvkehy4j', 'default', '', '纬二路·西段', '新增道路', 'secondary', 'j8', 'j2', '[[65.9,293.9],[249.3,296.9]]', 0, 1, 2);
INSERT INTO "nav"."road" VALUES ('r_jing_er_3', 'default', '', '经二路·南段', '新增道路', 'main', 'j2', 'j5', '[[249.3,296.9],[241.5,436]]', 0, 1, 3);
INSERT INTO "nav"."road" VALUES ('r_c11_s4', 'default', '', '电石渣通道·西段', '新增道路', 'secondary', 's4', 'n_mvkoc3j6', '[[91,217.9],[118.1,216.5]]', 0, 1, 4);
INSERT INTO "nav"."road" VALUES ('r_mvkek55q', 'default', '', '经一路·北段', '新增道路', 'secondary', 'j1', 'n_mvko2xng', '[[70.1,60.8],[67.1,209.3]]', 0, 1, 4);
INSERT INTO "nav"."road" VALUES ('r_c11_slag', 'default', '', '电石渣装车点支路', '新增道路', 'secondary', 'n_mvkoc3j6', 'm_slag', '[[118.1,216.5],[149.6,244.5]]', 0, 1, 5);
INSERT INTO "nav"."road" VALUES ('r_mvkemro2', 'default', '', '经三路', '新增道路', 'secondary', 'j7', 'j9', '[[769.6,294.5],[771.4,434.9]]', 0, 1, 5);
INSERT INTO "nav"."road" VALUES ('r_sup_01', 'default', '', '纬二路·东段', '补充路网', 'secondary', 'j2', 'j7', '[[249.3,296.9],[769.6,294.5]]', 0, 1, 99);
INSERT INTO "nav"."road" VALUES ('r_sup_100', 'default', '', '北侧支路·西北段', '补充路网', 'branch', 'j1', 's6', '[[70.1,60.8],[115.6,68.5]]', 0, 1, 100);
INSERT INTO "nav"."road" VALUES ('r_sup_101', 'default', '', '卸煤点支路', '补充路网', 'branch', 's6', 'm_coal', '[[115.6,68.5],[90.7,110.3]]', 0, 1, 101);
INSERT INTO "nav"."road" VALUES ('r_sup_102', 'default', '', '西北纵向支路·北段', '补充路网', 'branch', 'm_coal', 's5', '[[90.7,110.3],[89.9,164.1]]', 0, 1, 102);
INSERT INTO "nav"."road" VALUES ('r_sup_103', 'default', '', '西北纵向支路·南段', '补充路网', 'branch', 's5', 's4', '[[89.9,164.1],[91,217.9]]', 0, 1, 103);
INSERT INTO "nav"."road" VALUES ('r_sup_104', 'default', '', '4#门连接线', '补充路网', 'secondary', 'gate_w', 'n_mvko2xng', '[[47,207.6],[67.1,209.3]]', 0, 1, 104);
INSERT INTO "nav"."road" VALUES ('r_sup_105', 'default', '', '4#门内侧支路', '补充路网', 'branch', 'n_mvko2xng', 's3', '[[67.1,209.3],[72.7,236.3]]', 0, 1, 105);
INSERT INTO "nav"."road" VALUES ('r_p1_c13', 'default', '', '1#停车场支路', '补充路网', 'branch', 'p1', 'n_mvoi1tno', '[[172.9,73.4],[255.9,73.2]]', 0, 1, 111);
INSERT INTO "nav"."road" VALUES ('r_sup_112', 'default', '', '北侧支路·西段', '补充路网', 'branch', 'p1', 's6', '[[172.9,73.4],[115.6,68.5]]', 0, 1, 112);
INSERT INTO "nav"."road" VALUES ('r_sup_113', 'default', '', '2#停车场支路', '补充路网', 'branch', 's1', 'p2', '[[1106.2,435.8],[1171.4,444.8]]', 0, 1, 113);
INSERT INTO "nav"."road" VALUES ('r_sup_114', 'default', '', '烧碱装车点支路', '补充路网', 'branch', 'n_mvko5jb1', 'm_lye', '[[1055.1,433.5],[1042.9,421.6]]', 0, 1, 114);
INSERT INTO "nav"."road" VALUES ('r_sup_115', 'default', '', '盐酸装车点支路', '补充路网', 'branch', 'm_lye', 'm_hcl', '[[1042.9,421.6],[1042.2,397.2]]', 0, 1, 115);
INSERT INTO "nav"."road" VALUES ('r_sup_116', 'default', '', 'PVC装车点支路', '补充路网', 'branch', 'n_mvknkw9u', 'm_pvc', '[[517.7,433.7],[529.8,467]]', 0, 1, 116);
INSERT INTO "nav"."road" VALUES ('r_nav_ww1', 'default', '', '纬一路·中段', '补充路网', 'secondary', 'j5', 'n_mvknkw9u', '[[241.5,436],[517.7,433.7]]', 0, 1, 118);
INSERT INTO "nav"."road" VALUES ('r_nav_ww2', 'default', '', '纬一路·东段', '补充路网', 'secondary', 'n_mvknkw9u', 'j9', '[[517.7,433.7],[771.4,434.9]]', 0, 1, 119);
INSERT INTO "nav"."road" VALUES ('r_nav_ww3', 'default', '', '纬一路·东延', '补充路网', 'secondary', 'j9', 'n_mvko5jb1', '[[771.4,434.9],[1055.1,433.5]]', 0, 1, 120);
INSERT INTO "nav"."road" VALUES ('r_nav_jy1', 'default', '', '经一路·南段', '补充路网', 'secondary', 'j8', 'j4', '[[65.9,293.9],[61.7,431]]', 0, 1, 121);
INSERT INTO "nav"."road" VALUES ('r_nav_c8m', 'default', '', '电石卸车点支路', '补充路网', 'secondary', 'n_mvknd5up', 'm_carbide', '[[254.5,100.2],[277.5,117.8]]', 0, 1, 123);
INSERT INTO "nav"."road" VALUES ('r_nav_ww0', 'default', '', '纬一路·西二段', '补充路网', 'secondary', 's2', 'j5', '[[108.8,428.6],[241.5,436]]', 0, 1, 124);
INSERT INTO "nav"."road" VALUES ('r_nav_jy2', 'default', '', '经一路·中段', '补充路网', 'secondary', 'n_mvko2xng', 'j8', '[[67.1,209.3],[65.9,293.9]]', 0, 1, 126);
INSERT INTO "nav"."road" VALUES ('r_nav_s4c9', 'default', '', '4#磅连接线', '补充路网', 'secondary', 'n_mvko2xng', 's4', '[[67.1,209.3],[91,217.9]]', 0, 1, 127);
INSERT INTO "nav"."road" VALUES ('r_nav_ww4', 'default', '', '纬一路·东端', '补充路网', 'secondary', 'n_mvko5jb1', 's1', '[[1055.1,433.5],[1106.2,435.8]]', 0, 1, 128);
INSERT INTO "nav"."road" VALUES ('r_c13_c8', 'default', '', '经二路·北段下', '新增道路', 'main', 'n_mvoi1tno', 'n_mvknd5up', '[[255.9,73.2],[254.5,100.2]]', 0, 1, 131);
INSERT INTO "nav"."road" VALUES ('r_c13_s8', 'default', '', '8#磅连接线', '补充路网', 'secondary', 'n_mvoi1tno', 's8', '[[255.9,73.2],[309.8,55.6]]', 0, 1, 132);
INSERT INTO "nav"."road" VALUES ('r_c13_s7', 'default', '', '7#磅连接线', '补充路网', 'secondary', 'n_mvoi1tno', 's7', '[[255.9,73.2],[231.9,91]]', 0, 1, 133);

-- ----------------------------
-- Table structure for scale
-- ----------------------------
DROP TABLE IF EXISTS "nav"."scale";
CREATE TABLE "nav"."scale" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "plant_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default",
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "node_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "area" varchar(100) COLLATE "pg_catalog"."default",
  "capacity" varchar(50) COLLATE "pg_catalog"."default",
  "status" int2 NOT NULL DEFAULT 1,
  "sort_no" int4 NOT NULL DEFAULT 0
)
;
COMMENT ON COLUMN "nav"."scale"."status" IS '磅房状态：1=正常 2=检修 3=停用；非正常状态的磅房不会出现在可称重列表中';

-- ----------------------------
-- Records of scale
-- ----------------------------
INSERT INTO "nav"."scale" VALUES ('s1', 'default', 'B1', '1#磅房', 's1', '', '51t', 1, 1);
INSERT INTO "nav"."scale" VALUES ('s2', 'default', 'B2', '2#磅房', 's2', '', '51t', 2, 2);
INSERT INTO "nav"."scale" VALUES ('s3', 'default', 'B3', '3#磅房', 's3', '', '51t', 3, 3);
INSERT INTO "nav"."scale" VALUES ('s4', 'default', 'B4', '4#磅房', 's4', '', '51t', 1, 4);
INSERT INTO "nav"."scale" VALUES ('s5', 'default', 'B5', '5#磅房', 's5', '', '51t', 3, 5);
INSERT INTO "nav"."scale" VALUES ('s6', 'default', 'B6', '6#磅房', 's6', '', '51t', 3, 6);
INSERT INTO "nav"."scale" VALUES ('s7', 'default', 'B7', '7#磅房', 's7', '', '51t', 2, 7);
INSERT INTO "nav"."scale" VALUES ('s8', 'default', 'B8', '8#磅房', 's8', '', '51t', 1, 8);

-- ----------------------------
-- Indexes structure for table gate
-- ----------------------------
CREATE INDEX "idx_nav_gate_plant" ON "nav"."gate" USING btree (
  "plant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table gate
-- ----------------------------
ALTER TABLE "nav"."gate" ADD CONSTRAINT "gate_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table material
-- ----------------------------
CREATE INDEX "idx_nav_material_plant" ON "nav"."material" USING btree (
  "plant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table material
-- ----------------------------
ALTER TABLE "nav"."material" ADD CONSTRAINT "material_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table node
-- ----------------------------
CREATE INDEX "idx_nav_node_plant" ON "nav"."node" USING btree (
  "plant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table node
-- ----------------------------
ALTER TABLE "nav"."node" ADD CONSTRAINT "node_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table plant
-- ----------------------------
ALTER TABLE "nav"."plant" ADD CONSTRAINT "plant_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table road
-- ----------------------------
CREATE INDEX "idx_nav_road_plant" ON "nav"."road" USING btree (
  "plant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table road
-- ----------------------------
ALTER TABLE "nav"."road" ADD CONSTRAINT "road_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table scale
-- ----------------------------
CREATE INDEX "idx_nav_scale_plant" ON "nav"."scale" USING btree (
  "plant_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table scale
-- ----------------------------
ALTER TABLE "nav"."scale" ADD CONSTRAINT "scale_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table gate
-- ----------------------------
ALTER TABLE "nav"."gate" ADD CONSTRAINT "gate_node_id_fkey" FOREIGN KEY ("node_id") REFERENCES "nav"."node" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "nav"."gate" ADD CONSTRAINT "gate_plant_id_fkey" FOREIGN KEY ("plant_id") REFERENCES "nav"."plant" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table material
-- ----------------------------
ALTER TABLE "nav"."material" ADD CONSTRAINT "material_plant_id_fkey" FOREIGN KEY ("plant_id") REFERENCES "nav"."plant" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "nav"."material" ADD CONSTRAINT "material_point_node_id_fkey" FOREIGN KEY ("point_node_id") REFERENCES "nav"."node" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "nav"."material" ADD CONSTRAINT "material_scale_id_fkey" FOREIGN KEY ("scale_id") REFERENCES "nav"."scale" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table node
-- ----------------------------
ALTER TABLE "nav"."node" ADD CONSTRAINT "node_plant_id_fkey" FOREIGN KEY ("plant_id") REFERENCES "nav"."plant" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table road
-- ----------------------------
ALTER TABLE "nav"."road" ADD CONSTRAINT "road_from_node_id_fkey" FOREIGN KEY ("from_node_id") REFERENCES "nav"."node" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "nav"."road" ADD CONSTRAINT "road_plant_id_fkey" FOREIGN KEY ("plant_id") REFERENCES "nav"."plant" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "nav"."road" ADD CONSTRAINT "road_to_node_id_fkey" FOREIGN KEY ("to_node_id") REFERENCES "nav"."node" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table scale
-- ----------------------------
ALTER TABLE "nav"."scale" ADD CONSTRAINT "scale_node_id_fkey" FOREIGN KEY ("node_id") REFERENCES "nav"."node" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "nav"."scale" ADD CONSTRAINT "scale_plant_id_fkey" FOREIGN KEY ("plant_id") REFERENCES "nav"."plant" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
