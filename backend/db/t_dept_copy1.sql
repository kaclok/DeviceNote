/*
 Navicat Premium Dump SQL

 Source Server         : 线上-检修记录
 Source Server Type    : PostgreSQL
 Source Server Version : 170002 (170002)
 Source Host           : 10.8.54.24:5432
 Source Catalog        : device-note
 Source Schema         : dept_user

 Target Server Type    : PostgreSQL
 Target Server Version : 170002 (170002)
 File Encoding         : 65001

 Date: 15/10/2026 09:25:10
*/


-- ----------------------------
-- Table structure for t_dept_copy1
-- ----------------------------
DROP TABLE IF EXISTS "dept_user"."t_dept_copy1";
CREATE TABLE "dept_user"."t_dept_copy1" (
  "dept_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_all_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_dept_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_leader" varchar(30) COLLATE "pg_catalog"."default",
  "master_leader" varchar(30) COLLATE "pg_catalog"."default",
  "is_using" bool NOT NULL DEFAULT true,
  "level" int2,
  "parent_type" int2,
  "direct_children" varchar[] COLLATE "pg_catalog"."default",
  "recursive_children" varchar[] COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."dept_leader" IS '部长，关联t_user的id';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."master_leader" IS '分管领导，关联t_user的id';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."is_using" IS '是否启用';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."level" IS '在树形结构中的层级';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."parent_type" IS '1：root 2：普通节点 3：叶子结点';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."direct_children" IS '直系子节点';
COMMENT ON COLUMN "dept_user"."t_dept_copy1"."recursive_children" IS '子系孙系...';

-- ----------------------------
-- Records of t_dept_copy1
-- ----------------------------
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030015002', '企业管理部', '金泰化学本部/企业管理部', '1030015', NULL, NULL, 't', 3, 3, NULL, NULL);
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030', '陕西金泰化学科技集团有限公司', '陕西投资集团有限公司/陕西金泰化学科技集团有限公司', '1', NULL, NULL, 't', 1, 1, '{1030015,1030016}', '{1030015,1030015001,1030015002,1030015003,1030016,1030016016,1030016016001}');
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030016', '陕西金泰化学神木氯碱有限公司', '陕西金泰化学科技集团有限公司/陕西金泰化学神木氯碱有限公司', '1030', NULL, NULL, 't', 2, 2, '{1030016016}', '{1030016016,1030016016001}');
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030016016', '烧碱装置', '陕西金泰化学神木氯碱有限公司/烧碱装置', '1030016', NULL, NULL, 't', 3, 2, '{1030016016001}', '{1030016016001}');
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030016016001', '烧碱装置保障组', '烧碱装置/烧碱装置保障组', '1030016016', NULL, NULL, 't', 4, 3, NULL, NULL);
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030015003', '纪律检查部', '金泰化学本部/纪律检查部', '1030015', NULL, NULL, 't', 3, 3, NULL, NULL);
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030015001', '工会工作部', '金泰化学本部/工会工作部', '1030015', NULL, NULL, 't', 3, 3, NULL, NULL);
INSERT INTO "dept_user"."t_dept_copy1" VALUES ('1030015', '金泰化学本部', '陕西金泰化学科技集团有限公司/金泰化学本部', '1030', NULL, NULL, 't', 2, 2, '{1030015001,1030015002,1030015003}', '{1030015001,1030015002,1030015003}');

-- ----------------------------
-- Primary Key structure for table t_dept_copy1
-- ----------------------------
ALTER TABLE "dept_user"."t_dept_copy1" ADD CONSTRAINT "t_dept_copy1_pkey" PRIMARY KEY ("dept_code");
