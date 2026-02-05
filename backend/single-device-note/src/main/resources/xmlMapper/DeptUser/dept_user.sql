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

 Date: 05/02/2026 10:27:32
*/


-- ----------------------------
-- Table structure for t_dept
-- ----------------------------
DROP TABLE IF EXISTS "dept_user"."t_dept";
CREATE TABLE "dept_user"."t_dept" (
  "dept_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_all_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_dept_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_leader" varchar(30) COLLATE "pg_catalog"."default",
  "master_leader" varchar(30) COLLATE "pg_catalog"."default",
  "is_using" bool NOT NULL DEFAULT true,
  "ids_direct_sub_depts" varchar[] COLLATE "pg_catalog"."default",
  "ids_recursive_sub_depts" varchar[] COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "dept_user"."t_dept"."dept_leader" IS '部长，关联t_user的id';
COMMENT ON COLUMN "dept_user"."t_dept"."master_leader" IS '分管领导，关联t_user的id';
COMMENT ON COLUMN "dept_user"."t_dept"."ids_direct_sub_depts" IS '直接子部门id组成的数组';
COMMENT ON COLUMN "dept_user"."t_dept"."ids_recursive_sub_depts" IS '递归所有子部门id组成的数组';

-- ----------------------------
-- Records of t_dept
-- ----------------------------
INSERT INTO "dept_user"."t_dept" VALUES ('1030016', '陕西金泰化学神木氯碱有限公司', '陕西金泰化学科技集团有限公司/陕西金泰化学神木氯碱有限公司', '1030', NULL, NULL, 't', '{1030016016}', '{1030016016,1030016016001}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030016016', '烧碱装置', '陕西金泰化学神木氯碱有限公司/烧碱装置', '1030016', NULL, NULL, 't', '{1030016016001}', '{1030016016001}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030016016001', '烧碱装置保障组', '烧碱装置/烧碱装置保障组', '1030016016', NULL, NULL, 't', '{}', '{}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030', '陕西金泰化学科技集团有限公司', '陕西投资集团有限公司/陕西金泰化学科技集团有限公司', '1', NULL, NULL, 't', '{1030015,1030016}', '{1030015,1030015001,1030015002,1030015003,1030016,1030016016,1030016016001}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030015', '金泰化学本部', '陕西金泰化学科技集团有限公司/金泰化学本部', '1030', NULL, NULL, 't', '{1030015001,1030015002,1030015003}', '{1030015001,1030015002,1030015003}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030015001', '工会工作部', '金泰化学本部/工会工作部', '1030015', NULL, NULL, 't', '{}', '{}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030015002', '企业管理部', '金泰化学本部/企业管理部', '1030015', NULL, NULL, 't', '{}', '{}');
INSERT INTO "dept_user"."t_dept" VALUES ('1030015003', '纪律检查部', '金泰化学本部/纪律检查部', '1030015', NULL, NULL, 't', '{}', '{}');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS "dept_user"."t_user";
CREATE TABLE "dept_user"."t_user" (
  "id" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "account" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(15) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "is_using" bool NOT NULL DEFAULT true
)
;
COMMENT ON COLUMN "dept_user"."t_user"."dept_code" IS '关联t_dept的dept_code';

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO "dept_user"."t_user" VALUES ('024680|1030015001', '024680', '贺承承', '1030015001', 't');
INSERT INTO "dept_user"."t_user" VALUES ('019114|1030015003', '019114', '时光西', '1030015003', 't');
INSERT INTO "dept_user"."t_user" VALUES ('019596|1030016016001', '019596', '贺贝贝', '1030016016001', 't');
INSERT INTO "dept_user"."t_user" VALUES ('019547|1030016016', '019547', '童毅飞', '1030016016', 't');
INSERT INTO "dept_user"."t_user" VALUES ('026504|1030015002', '026504', '段媛媛', '1030015002', 't');

-- ----------------------------
-- Primary Key structure for table t_dept
-- ----------------------------
ALTER TABLE "dept_user"."t_dept" ADD CONSTRAINT "t_org_pkey" PRIMARY KEY ("dept_code");
