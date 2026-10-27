/*
 Navicat Premium Dump SQL

 Source Server         : 线上-检修记录
 Source Server Type    : PostgreSQL
 Source Server Version : 170002 (170002)
 Source Host           : 10.8.54.24:5432
 Source Catalog        : device-note
 Source Schema         : gb_record

 Target Server Type    : PostgreSQL
 Target Server Version : 170002 (170002)
 File Encoding         : 65001

 Date: 27/10/2026 11:35:21
*/


-- ----------------------------
-- Sequence structure for gb_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "gb_record"."gb_log_id_seq";
CREATE SEQUENCE "gb_record"."gb_log_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for gb_weigh_event_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "gb_record"."gb_weigh_event_id_seq";
CREATE SEQUENCE "gb_record"."gb_weigh_event_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for gb_log
-- ----------------------------
DROP TABLE IF EXISTS "gb_record"."gb_log";
CREATE TABLE "gb_record"."gb_log" (
  "id" int8 NOT NULL DEFAULT nextval('"gb_record".gb_log_id_seq'::regclass),
  "source" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "fp" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(2) COLLATE "pg_catalog"."default",
  "log_time" timestamp(6),
  "content" text COLLATE "pg_catalog"."default",
  "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Records of gb_log
-- ----------------------------
INSERT INTO "gb_record"."gb_log" VALUES (13, '4', '33bffd1f20023d73', 'M', '2026-10-26 18:03:28', '1#识别车牌：陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (14, '4', '961d04a5ca12d25e', 'A', '2026-10-26 18:03:28', '等待入口车辆上磅:陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (15, '4', 'caa7f2f7943ba914', 'A', '2026-10-26 18:03:48', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (16, '4', '9f0f295121ea0893', 'A', '2026-10-26 18:04:19', '确认完成:陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (17, '4', '505115de5ab8f6cb', 'A', '2026-10-26 18:04:45', '自动数据保存成功！单号[WH2026102600161],车牌号[陕KR9795]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (18, '4', 'e924c0f7a5da8461', 'A', '2026-10-26 18:05:03', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (19, '4', '92e6dfaf33acf098', 'M', '2026-10-26 18:05:25', '2#识别车牌：晋JV8549', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (20, '4', '9ea0661e30e6db8b', 'A', '2026-10-26 18:05:25', '等待出口车辆上磅:晋JV8549', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (21, '4', '537ec1df32ea9f5c', 'A', '2026-10-26 18:05:48', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (22, '4', '43516e143015b332', 'A', '2026-10-26 18:06:19', '确认完成:晋JV8549', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (23, '4', '6d027aaf0d549265', 'A', '2026-10-26 18:06:23', '自动数据保存成功！单号[WH2026102600085],车牌号[晋JV8549]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (24, '4', '2c5de9a05db93456', 'A', '2026-10-26 18:07:06', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (25, '4', '67ce7350d704ba8b', 'M', '2026-10-26 18:31:30', '1#识别车牌：晋JJ4186', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (26, '4', '1a32a5710b74e5ff', 'M', '2026-10-26 18:31:32', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"晋JJ4186","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517313887993859,"receiptType":1,"transportCorpName":"方山县宸鹏运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (27, '4', 'bfbf80704ac39069', 'M', '2026-10-26 18:31:32', '正在下载车牌数据[晋JJ4186]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (28, '4', '81d3068dd26a9ee9', 'A', '2026-10-26 18:31:32', '等待入口车辆上磅:晋JJ4186', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (29, '4', '97e2755478a6fdf4', 'A', '2026-10-26 18:31:52', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (30, '4', '2929afdf4197bdaf', 'A', '2026-10-26 18:32:22', '确认完成:晋JJ4186', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (31, '4', '99752b3e0496c302', 'A', '2026-10-26 18:32:26', '自动数据保存成功！单号[WH2026102600165],车牌号[晋JJ4186]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (32, '4', '6be83c24cdcb97f5', 'A', '2026-10-26 18:32:47', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (33, '4', '75c3ecdedffd05d9', 'M', '2026-10-26 18:33:13', '1#识别车牌：甘D80315', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (34, '4', '41a2dc07c5e782e4', 'M', '2026-10-26 18:33:15', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"甘D80315","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517314198372355,"receiptType":1,"transportCorpName":"白银圣业商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (35, '4', '69bf71806ce91bdf', 'M', '2026-10-26 18:33:15', '正在下载车牌数据[甘D80315]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (36, '4', '156ed474a5f5f978', 'A', '2026-10-26 18:33:15', '等待入口车辆上磅:甘D80315', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (37, '4', '1bc1dd100300f496', 'A', '2026-10-26 18:33:42', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (38, '4', '27f9739ee2d0197f', 'A', '2026-10-26 18:34:12', '确认完成:甘D80315', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (39, '4', '9efd3b760f458d1a', 'A', '2026-10-26 18:34:16', '自动数据保存成功！单号[WH2026102600166],车牌号[甘D80315]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (40, '4', '3b82c23ca72c2139', 'A', '2026-10-26 18:34:49', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (41, '4', '3602d5a3674d36fd', 'M', '2026-10-26 18:34:56', '1#识别车牌：甘D85015', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (42, '4', 'a65b096669c9e61c', 'M', '2026-10-26 18:34:59', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"甘D85015","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517314198372357,"receiptType":1,"transportCorpName":"白银圣业商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (43, '4', '0e17917cf15c9586', 'M', '2026-10-26 18:34:59', '正在下载车牌数据[甘D85015]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (44, '4', 'f713a19222f78f6d', 'A', '2026-10-26 18:35:00', '等待入口车辆上磅:甘D85015', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (45, '4', '9aa25f0b6967c20d', 'A', '2026-10-26 18:35:25', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (46, '4', '40abe54d60324ce9', 'A', '2026-10-26 18:35:56', '确认完成:甘D85015', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (47, '4', '49081c9267adb33c', 'A', '2026-10-26 18:36:00', '自动数据保存成功！单号[WH2026102600168],车牌号[甘D85015]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (48, '4', 'b3a4023bcaa6d336', 'A', '2026-10-26 18:36:29', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (49, '4', 'edec70e374f38b36', 'M', '2026-10-26 18:39:03', '1#识别车牌：陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (50, '4', 'baab2acd11911528', 'M', '2026-10-26 18:39:04', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KM8567","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830242799623,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (51, '4', '37425be57304dfc3', 'M', '2026-10-26 18:39:04', '正在下载车牌数据[陕KM8567]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (52, '4', '007a2c50220b66de', 'A', '2026-10-26 18:39:04', '等待入口车辆上磅:陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (53, '4', '1577f231ef7bbaa8', 'A', '2026-10-26 18:39:23', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (54, '4', 'cc411491ee96fafc', 'A', '2026-10-26 18:39:54', '确认完成:陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (55, '4', 'c81a8c2fbd863e1e', 'A', '2026-10-26 18:39:58', '自动数据保存成功！单号[WH2026102600169],车牌号[陕KM8567]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (56, '4', '2959599446e72663', 'A', '2026-10-26 18:40:23', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (57, '4', '7d01359d9d4413fd', 'M', '2026-10-26 18:44:41', '1#识别车牌：陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (58, '4', '4c4220f3d0f6626a', 'M', '2026-10-26 18:44:43', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KJ1738","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517317792890881,"receiptType":1,"transportCorpName":"子洲县鑫旺运销有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (59, '4', 'ce6f8ddbdb8469b7', 'M', '2026-10-26 18:44:43', '正在下载车牌数据[陕KJ1738]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (60, '4', '04c83e80d4565007', 'A', '2026-10-26 18:44:43', '等待入口车辆上磅:陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (61, '4', '2b60843eda530f57', 'A', '2026-10-26 18:45:02', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (62, '4', 'ed992b0ba7974711', 'A', '2026-10-26 18:45:33', '确认完成:陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (63, '4', 'f5b9191f0b990925', 'A', '2026-10-26 18:45:37', '自动数据保存成功！单号[WH2026102600171],车牌号[陕KJ1738]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (64, '4', '8b6a341fde67b284', 'A', '2026-10-26 18:45:57', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (65, '4', '19543e83d92d7cb8', 'M', '2026-10-26 18:51:22', '1#识别车牌：冀A43288', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (66, '4', '591671ca2077a3fb', 'M', '2026-10-26 18:51:24', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"冀A43288","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517362856493057,"receiptType":1,"transportCorpName":"晋州市明宝化工商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (67, '4', '4fc1e5088279014f', 'M', '2026-10-26 18:51:24', '正在下载车牌数据[冀A43288]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (68, '4', '06be7edc1baf582e', 'A', '2026-10-26 18:51:24', '等待入口车辆上磅:冀A43288', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (69, '4', '5b8b077be830c3b3', 'A', '2026-10-26 18:51:44', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (70, '4', 'd4178fad8d86e1f8', 'A', '2026-10-26 18:52:14', '确认完成:冀A43288', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (71, '4', 'c4e665facedb3b28', 'A', '2026-10-26 18:52:18', '自动数据保存成功！单号[WH2026102600173],车牌号[冀A43288]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (72, '4', '6114948392b5b1aa', 'A', '2026-10-26 18:52:43', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (73, '4', 'c91600867fd07530', 'M', '2026-10-26 18:55:21', '1#识别车牌：晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (74, '4', 'ad9e6b6c67d9322a', 'M', '2026-10-26 18:55:22', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"晋JN5755","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517313887993857,"receiptType":1,"transportCorpName":"方山县宸鹏运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (75, '4', '28e5e9051a8038c9', 'M', '2026-10-26 18:55:22', '正在下载车牌数据[晋JN5755]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (76, '4', '6ff0cf1b7aad5f13', 'A', '2026-10-26 18:55:23', '等待入口车辆上磅:晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (77, '4', '96e4952bc9a755b0', 'A', '2026-10-26 18:55:43', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (78, '4', '18440c7e82225000', 'A', '2026-10-26 18:56:23', '确认完成:晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (79, '4', '3526dca11e784732', 'A', '2026-10-26 18:56:28', '自动数据保存成功！单号[WH2026102600174],车牌号[晋JN5755]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (80, '4', '6fea7769b988909d', 'A', '2026-10-26 18:56:52', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (81, '4', 'b28928e99086f7f4', 'M', '2026-10-26 18:57:08', '1#识别车牌：陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (82, '4', 'fb0886a3cae38866', 'M', '2026-10-26 18:57:09', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KB5935","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517343801769987,"receiptType":1,"transportCorpName":"榆林市聚润危险品运输服务有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (83, '4', 'ec1f6bd42f159274', 'M', '2026-10-26 18:57:10', '正在下载车牌数据[陕KB5935]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (84, '4', 'f94c05c81aae2e2e', 'A', '2026-10-26 18:57:10', '等待入口车辆上磅:陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (85, '4', '4f5c88465f2bea52', 'A', '2026-10-26 18:57:30', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (86, '4', '463c00ae80201fe5', 'A', '2026-10-26 18:58:01', '确认完成:陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (87, '4', '2f312b0cef68dbfc', 'A', '2026-10-26 18:58:05', '自动数据保存成功！单号[WH2026102600175],车牌号[陕KB5935]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (88, '4', '2655f29c856513e7', 'A', '2026-10-26 18:58:25', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (89, '4', 'd056930d1c536f92', 'M', '2026-10-26 18:59:29', '1#识别车牌：陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (90, '4', '4373662210794417', 'A', '2026-10-26 18:59:30', '等待入口车辆上磅:陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (91, '4', '8546027043e9962d', 'A', '2026-10-26 18:59:54', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (92, '4', 'c2ca728d4885e19b', 'A', '2026-10-26 19:00:24', '确认完成:陕KM8567', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (93, '4', 'b94d42f80f2884d0', 'A', '2026-10-26 19:00:29', '自动数据保存成功！单号[WH2026102600169],车牌号[陕KM8567]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (94, '4', '99fc265d82664446', 'A', '2026-10-26 19:00:59', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (95, '4', 'a31428cd1266417c', 'M', '2026-10-26 19:06:00', '1#识别车牌：陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (96, '4', '7b3a57b18c818b46', 'M', '2026-10-26 19:06:01', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KD8395","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830240702471,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (97, '4', '45ea5a4bb1c50b37', 'M', '2026-10-26 19:06:01', '正在下载车牌数据[陕KD8395]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (98, '4', '3ff0240f8925e9c0', 'A', '2026-10-26 19:06:01', '等待入口车辆上磅:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (99, '4', '5aae2bbb35166ce7', 'A', '2026-10-26 19:06:19', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (100, '4', '258a8df2e3341125', 'A', '2026-10-26 19:07:03', '确认完成:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (101, '4', '2910e0567e186b65', 'A', '2026-10-26 19:07:07', '自动数据保存成功！单号[WH2026102600178],车牌号[陕KD8395]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (102, '4', 'ca81e9e275895414', 'A', '2026-10-26 19:07:26', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (103, '4', 'c81f653b0053b85f', 'M', '2026-10-26 19:07:51', '1#识别车牌：蒙C27130', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (104, '4', '7b01a18a71b9719b', 'M', '2026-10-26 19:07:53', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙C27130","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517326615609347,"receiptType":1,"transportCorpName":"乌海市昌益物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (105, '4', '64f0e5474b559a99', 'M', '2026-10-26 19:07:53', '正在下载车牌数据[蒙C27130]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (106, '4', '9850daf8c8d83f8b', 'A', '2026-10-26 19:07:53', '等待入口车辆上磅:蒙C27130', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (107, '4', '9399e5ad3368b095', 'A', '2026-10-26 19:08:20', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (108, '4', '848d53b6f179729f', 'A', '2026-10-26 19:08:50', '确认完成:蒙C27130', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (109, '4', 'b1414af53f43d9a7', 'A', '2026-10-26 19:08:54', '自动数据保存成功！单号[WH2026102600179],车牌号[蒙C27130]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (110, '4', 'e605f32c2ca64a11', 'A', '2026-10-26 19:09:35', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (111, '4', '1711e9d455f3a96a', 'M', '2026-10-26 19:09:46', '1#识别车牌：蒙B86541', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (112, '4', '96b4de5dbe6d2430', 'M', '2026-10-26 19:09:47', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B86541","consigneeCorpName":"包头市昱和通贸易有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517372794896387,"receiptType":1,"transportCorpName":"包头市昱和通贸易有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (113, '4', 'c14cf6e6dd13f499', 'M', '2026-10-26 19:09:47', '正在下载车牌数据[蒙B86541]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (114, '4', '6656bcff772a8188', 'A', '2026-10-26 19:09:47', '等待入口车辆上磅:蒙B86541', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (115, '4', '582fd3baa0e27e4c', 'A', '2026-10-26 19:10:09', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (116, '4', '4f19ef86c4eefb0f', 'A', '2026-10-26 19:10:40', '确认完成:蒙B86541', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (117, '4', '0207d07ff867e35c', 'A', '2026-10-26 19:10:44', '自动数据保存成功！单号[WH2026102600180],车牌号[蒙B86541]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (118, '4', '9aeefb8a749ca08e', 'A', '2026-10-26 19:11:06', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (119, '4', 'f8d12010216c4630', 'M', '2026-10-26 19:11:26', '1#识别车牌：蒙BA3948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (120, '4', '7b05b6ea31e7c033', 'M', '2026-10-26 19:11:28', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA3948","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327070691333,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (121, '4', '2acc227cddcb8185', 'M', '2026-10-26 19:11:28', '正在下载车牌数据[蒙BA3948]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (122, '4', 'ed91eef03ece8d52', 'A', '2026-10-26 19:11:28', '等待入口车辆上磅:蒙BA3948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (123, '4', '0ab552635e34595b', 'A', '2026-10-26 19:11:49', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (124, '4', 'a7b6edf6381c2ea1', 'A', '2026-10-26 19:12:22', '确认完成:蒙BA3948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (125, '4', '403b22ac31b0b08f', 'A', '2026-10-26 19:12:26', '自动数据保存成功！单号[WH2026102600181],车牌号[蒙BA3948]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (126, '4', 'f036e5ede7f79551', 'A', '2026-10-26 19:12:59', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (127, '4', '01e8730470ef8abb', 'M', '2026-10-26 19:21:25', '1#识别车牌：陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (128, '4', '8a21705fa767b287', 'A', '2026-10-26 19:21:25', '等待入口车辆上磅:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (129, '4', 'a36684f57a9b54c5', 'A', '2026-10-26 19:21:52', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (130, '4', 'f8f4a8d490a74ad8', 'A', '2026-10-26 19:22:23', '确认完成:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (131, '4', 'ddb0880413c09e9d', 'A', '2026-10-26 19:22:40', '自动数据保存成功！单号[WH2026102600178],车牌号[陕KD8395]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (132, '4', '0d9edad46f845e58', 'A', '2026-10-26 19:22:59', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (133, '4', '9ae0c313fa152121', 'M', '2026-10-26 19:25:06', '1#识别车牌：陕KS9319', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (134, '4', '83fa0370f3e7bf0b', 'M', '2026-10-26 19:25:08', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KS9319","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517326397505539,"receiptType":1,"transportCorpName":"绥德县德顺昌能源有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (135, '4', '3437a14510e78748', 'M', '2026-10-26 19:25:08', '正在下载车牌数据[陕KS9319]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (136, '4', 'e7a668ddc4306bf0', 'A', '2026-10-26 19:25:08', '等待入口车辆上磅:陕KS9319', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (137, '4', '0bcca063436acf28', 'A', '2026-10-26 19:25:28', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (138, '4', '6c90f58512c31cc5', 'A', '2026-10-26 19:25:59', '确认完成:陕KS9319', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (139, '4', 'a76ff86f72fc9fa8', 'A', '2026-10-26 19:26:03', '自动数据保存成功！单号[WH2026102600183],车牌号[陕KS9319]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (140, '4', 'bb8196f32366f781', 'A', '2026-10-26 19:26:46', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (141, '4', '07444c66625a8579', 'M', '2026-10-26 19:34:32', '1#识别车牌：陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (142, '4', 'df788a0a9bea4133', 'M', '2026-10-26 19:34:34', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KG0391","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517326196178945,"receiptType":1,"transportCorpName":"榆林市胜润物流有限责任公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (143, '4', '4b515e895bb4e4b5', 'M', '2026-10-26 19:34:34', '正在下载车牌数据[陕KG0391]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (144, '4', '17d8dbb35acd7df9', 'A', '2026-10-26 19:34:34', '等待入口车辆上磅:陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (145, '4', 'e58101c540f7fdae', 'A', '2026-10-26 19:34:54', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (146, '4', '593bf49a01c1d961', 'A', '2026-10-26 19:35:25', '确认完成:陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (147, '4', 'b61a5b9a7810f1ff', 'A', '2026-10-26 19:35:29', '自动数据保存成功！单号[WH2026102600184],车牌号[陕KG0391]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (148, '4', '124e4552594441ef', 'A', '2026-10-26 19:35:53', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (149, '4', '2ede64923a679484', 'M', '2026-10-26 19:36:16', '1#识别车牌：冀AS3762', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (150, '4', '073d03f8d28a6ae2', 'M', '2026-10-26 19:36:18', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"冀AS3762","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517362854395918,"receiptType":1,"transportCorpName":"晋州市明宝化工商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (151, '4', '3af00b0e13d3823e', 'M', '2026-10-26 19:36:18', '正在下载车牌数据[冀AS3762]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (152, '4', 'd2397b365583fa3e', 'A', '2026-10-26 19:36:18', '等待入口车辆上磅:冀AS3762', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (153, '4', 'd29f4a837b707bae', 'A', '2026-10-26 19:36:37', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (154, '4', '9931ecc2d035f4cb', 'A', '2026-10-26 19:37:07', '确认完成:冀AS3762', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (155, '4', '143bcb8b30a8cda1', 'A', '2026-10-26 19:37:12', '自动数据保存成功！单号[WH2026102600185],车牌号[冀AS3762]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (156, '4', '5bd7872faf63d52d', 'A', '2026-10-26 19:37:39', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (157, '4', '26a5eb5422b67c74', 'M', '2026-10-26 19:41:25', '2#识别车牌：陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (158, '4', '814b59169c220a09', 'A', '2026-10-26 19:41:25', '等待出口车辆上磅:陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (159, '4', '00a10b7ca897ddd8', 'A', '2026-10-26 19:41:47', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (160, '4', 'ce6b203151472391', 'A', '2026-10-26 19:42:17', '确认完成:陕KB5935', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (161, '4', '7c79c4c5947d1995', 'A', '2026-10-26 19:42:26', '自动数据保存成功！单号[WH2026102600175],车牌号[陕KB5935]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (162, '4', '00783e9975a3eb3b', 'A', '2026-10-26 19:42:53', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (163, '4', '1717fdf1b088145c', 'M', '2026-10-26 19:44:01', '2#识别车牌：陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (164, '4', '464c1ad26bde5897', 'A', '2026-10-26 19:44:02', '等待出口车辆上磅:陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (165, '4', 'a41d3cc5c232662f', 'A', '2026-10-26 19:44:28', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (166, '4', '2f9b7cebbd53501b', 'A', '2026-10-26 19:44:58', '确认完成:陕KJ1738', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (167, '4', '99f45ef678c3e3c8', 'A', '2026-10-26 19:45:03', '自动数据保存成功！单号[WH2026102600171],车牌号[陕KJ1738]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (168, '4', 'b92e24215754bfe9', 'A', '2026-10-26 19:45:33', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (169, '4', '7f0faf2911cbe2b3', 'M', '2026-10-26 19:45:49', '2#识别车牌：晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (170, '4', '07618d5848fcec84', 'A', '2026-10-26 19:45:49', '等待出口车辆上磅:晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (171, '4', '45cd058fe54bf038', 'A', '2026-10-26 19:46:16', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (172, '4', '72259aa05b01edc2', 'A', '2026-10-26 19:46:47', '确认完成:晋JN5755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (173, '4', '107e9658a3df0898', 'A', '2026-10-26 19:46:51', '自动数据保存成功！单号[WH2026102600174],车牌号[晋JN5755]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (174, '4', '1a1dfa5e567462eb', 'A', '2026-10-26 19:47:16', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (175, '4', 'cd669f5d20cbec55', 'M', '2026-10-26 19:56:16', '1#识别车牌：陕KL6759', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (176, '4', '1d76b42933c749ca', 'M', '2026-10-26 19:56:18', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KL6759","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517318067617795,"receiptType":1,"transportCorpName":"盐城市中捷物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (177, '4', '1c80c280368e7843', 'M', '2026-10-26 19:56:18', '正在下载车牌数据[陕KL6759]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (178, '4', '45922c7e88cb4e47', 'A', '2026-10-26 19:56:18', '等待入口车辆上磅:陕KL6759', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (179, '4', '5dd405361458d38f', 'A', '2026-10-26 19:56:44', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (180, '4', '27147f73d9b6a1cd', 'A', '2026-10-26 19:57:15', '确认完成:陕KL6759', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (181, '4', '13cbf3743711f198', 'A', '2026-10-26 19:57:19', '自动数据保存成功！单号[WH2026102600190],车牌号[陕KL6759]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (182, '4', '2f7d4552be2e85c7', 'A', '2026-10-26 19:57:42', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (183, '4', 'bc61828a8371fdf7', 'M', '2026-10-26 20:15:39', '1#识别车牌：蒙BA6409', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (184, '4', 'afe6f76f73f87a95', 'M', '2026-10-26 20:15:40', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA6409","consigneeCorpName":"包头市昱和通贸易有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517372796993539,"receiptType":1,"transportCorpName":"包头市昱和通贸易有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (185, '4', 'd791afbd27ac560c', 'M', '2026-10-26 20:15:40', '正在下载车牌数据[蒙BA6409]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (186, '4', 'd676d3e890038ee1', 'A', '2026-10-26 20:15:40', '等待入口车辆上磅:蒙BA6409', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (187, '4', 'c4fbd64f0093a3c3', 'A', '2026-10-26 20:15:59', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (188, '4', 'e36582b598ffb686', 'A', '2026-10-26 20:16:29', '确认完成:蒙BA6409', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (189, '4', 'fdc5c848252e32bb', 'A', '2026-10-26 20:16:33', '自动数据保存成功！单号[WH2026102600192],车牌号[蒙BA6409]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (190, '4', 'f2827f052c3a966e', 'A', '2026-10-26 20:16:49', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (191, '4', '921fb2111c9fbf7d', 'M', '2026-10-26 20:17:06', '1#识别车牌：陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (192, '4', '0bd4bf7901d25f3a', 'M', '2026-10-26 20:17:08', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KJ9774","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517314129166343,"receiptType":1,"transportCorpName":"靖边县宝宏运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (193, '4', '9defbed8b7085ed8', 'M', '2026-10-26 20:17:08', '正在下载车牌数据[陕KJ9774]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (194, '4', '42260d44e23cb878', 'A', '2026-10-26 20:17:08', '等待入口车辆上磅:陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (195, '4', '2f5fdc8beaafc5f3', 'A', '2026-10-26 20:17:29', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (196, '4', 'd7674a9c9a132b81', 'A', '2026-10-26 20:18:31', '确认完成:陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (197, '4', '51d66809f8dfe375', 'A', '2026-10-26 20:18:35', '自动数据保存成功！单号[WH2026102600193],车牌号[陕KJ9774]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (198, '4', '25590db0b46ddfc0', 'A', '2026-10-26 20:18:50', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (199, '4', '4369726e11af4192', 'M', '2026-10-26 20:23:11', '2#识别车牌：陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (200, '4', '87cc502630048adb', 'A', '2026-10-26 20:23:12', '等待出口车辆上磅:陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (201, '4', '312264cdc3614ad0', 'A', '2026-10-26 20:23:37', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (202, '4', '6bd3870c51a6ee80', 'A', '2026-10-26 20:24:08', '确认完成:陕KG0391', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (203, '4', '2779fb675affbecb', 'A', '2026-10-26 20:24:12', '自动数据保存成功！单号[WH2026102600184],车牌号[陕KG0391]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (204, '4', 'c2c07488f4f7c82b', 'A', '2026-10-26 20:24:35', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (205, '4', 'c0c6010671271fea', 'M', '2026-10-26 20:47:17', '2#识别车牌：陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (206, '4', '2a988cd709a4d52e', 'A', '2026-10-26 20:47:17', '等待出口车辆上磅:陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (207, '4', 'c256d35c29bfdb63', 'A', '2026-10-26 20:47:39', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (208, '4', '7a6be47d08ee935b', 'A', '2026-10-26 20:48:10', '确认完成:陕KJ9774', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (209, '4', 'e842f2823ede9960', 'A', '2026-10-26 20:48:14', '自动数据保存成功！单号[WH2026102600193],车牌号[陕KJ9774]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (210, '4', 'd91e3d355c50a73f', 'A', '2026-10-26 20:48:37', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (211, '4', '7ee9dec793cf5c04', 'M', '2026-10-26 20:50:01', '1#识别车牌：陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (212, '4', '5261bc2b7ad1f470', 'M', '2026-10-26 20:50:03', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KK7974","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517330090590209,"receiptType":1,"transportCorpName":"榆林市欧胜达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (213, '4', 'c19a6317fe37afc1', 'M', '2026-10-26 20:50:03', '正在下载车牌数据[陕KK7974]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (214, '4', '7eac503b93d8e8b8', 'A', '2026-10-26 20:50:03', '等待入口车辆上磅:陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (215, '4', 'b8de91d938b0b1b6', 'A', '2026-10-26 20:50:23', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (216, '4', '17d1caf20831096b', 'A', '2026-10-26 20:50:54', '确认完成:陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (217, '4', '11b23216a28ed729', 'A', '2026-10-26 20:50:58', '自动数据保存成功！单号[WH2026102600195],车牌号[陕KK7974]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (218, '4', '17d0bf49981caa6e', 'A', '2026-10-26 20:51:18', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (219, '4', 'd2534281c91bb2d4', 'M', '2026-10-26 20:51:44', '1#识别车牌：陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (220, '4', '09a19553aa33afd6', 'M', '2026-10-26 20:51:46', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KK7948","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517314129166341,"receiptType":1,"transportCorpName":"靖边县宝宏运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (221, '4', 'd0ead6dbb2ba7260', 'M', '2026-10-26 20:51:46', '正在下载车牌数据[陕KK7948]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (222, '4', 'ad0715f2783d5caa', 'A', '2026-10-26 20:51:46', '等待入口车辆上磅:陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (223, '4', 'e75138d5a38103ff', 'A', '2026-10-26 20:52:12', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (224, '4', 'd762df68d5778263', 'A', '2026-10-26 20:52:42', '确认完成:陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (274, '4', 'c55993fd2704d5fd', 'A', '2026-10-26 21:09:49', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (225, '4', '89c74b73a144968d', 'A', '2026-10-26 20:52:47', '自动数据保存成功！单号[WH2026102600196],车牌号[陕KK7948]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (226, '4', 'b020b3af8c5f9d62', 'A', '2026-10-26 20:53:13', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (227, '4', 'a1d477dd0d2d66e2', 'M', '2026-10-26 20:53:16', '1#识别车牌：蒙BB5512', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (228, '4', '2eaa20871feda891', 'M', '2026-10-26 20:53:23', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BB5512","consigneeCorpName":"中国北方稀土(集团)高科技股份有限公司冶炼分公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517377496711172,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (229, '4', '08334f1ea8110517', 'M', '2026-10-26 20:53:23', '正在下载车牌数据[蒙BB5512]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (230, '4', 'cfe087fda3797f5b', 'A', '2026-10-26 20:53:23', '等待入口车辆上磅:蒙BB5512', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (231, '4', '9913bb606148b5e0', 'A', '2026-10-26 20:53:44', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (232, '4', 'eda3b8c98a51ea24', 'A', '2026-10-26 20:54:13', '确认完成:蒙BB5512', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (233, '4', 'e5644e17edd5e9bd', 'A', '2026-10-26 20:54:18', '自动数据保存成功！单号[WH2026102600197],车牌号[蒙BB5512]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (234, '4', '1a7ba509194fc58b', 'A', '2026-10-26 20:54:38', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (235, '4', 'd6ca2324ee5633a9', 'M', '2026-10-26 20:58:06', '1#识别车牌：蒙B86011', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (236, '4', '7c301e6193edf902', 'M', '2026-10-26 20:58:08', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B86011","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327070691331,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (237, '4', '61d5a618f2e1510d', 'M', '2026-10-26 20:58:08', '正在下载车牌数据[蒙B86011]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (238, '4', '9b0ccdc6237225ac', 'A', '2026-10-26 20:58:08', '等待入口车辆上磅:蒙B86011', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (239, '4', '21ac76b535ee37ed', 'A', '2026-10-26 20:58:33', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (240, '4', 'aa07f13a8137186d', 'A', '2026-10-26 20:59:02', '确认完成:蒙B86011', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (241, '4', '5ff552dce30ec8aa', 'A', '2026-10-26 20:59:07', '自动数据保存成功！单号[WH2026102600200],车牌号[蒙B86011]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (242, '4', '5c6f986f3c7b7a1c', 'A', '2026-10-26 21:00:09', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (243, '4', '266ee1efe4b5eebf', 'M', '2026-10-26 21:00:28', '1#识别车牌：蒙BB2640', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (244, '4', '8c4de32953efd248', 'M', '2026-10-26 21:00:29', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BB2640","consigneeCorpName":"包头市昱和通贸易有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517372796993537,"receiptType":1,"transportCorpName":"包头市昱和通贸易有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (245, '4', 'da1e1a065d617002', 'M', '2026-10-26 21:00:29', '正在下载车牌数据[蒙BB2640]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (246, '4', '671c7a1d285a0d17', 'A', '2026-10-26 21:00:29', '等待入口车辆上磅:蒙BB2640', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (247, '4', 'f13ef515677cc81d', 'A', '2026-10-26 21:00:54', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (248, '4', 'c68399b54f815c38', 'A', '2026-10-26 21:01:25', '确认完成:蒙BB2640', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (249, '4', 'c82ea32578d9642c', 'A', '2026-10-26 21:01:29', '自动数据保存成功！单号[WH2026102600201],车牌号[蒙BB2640]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (250, '4', '49ed8b0e32a67e7a', 'A', '2026-10-26 21:01:59', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (251, '4', '4c88f238c4e47767', 'M', '2026-10-26 21:02:09', '1#识别车牌：陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (252, '4', 'f057675f13a11204', 'M', '2026-10-26 21:02:11', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KG0159","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517342757388291,"receiptType":1,"transportCorpName":"榆林中顺通运输有限责任公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (253, '4', '50a33b45196e2df8', 'M', '2026-10-26 21:02:11', '正在下载车牌数据[陕KG0159]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (254, '4', '6ae1e255c720e567', 'A', '2026-10-26 21:02:11', '等待入口车辆上磅:陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (255, '4', 'e8ea283c759d93f8', 'A', '2026-10-26 21:02:42', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (256, '4', '5889f1521bd7dba4', 'A', '2026-10-26 21:03:12', '确认完成:陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (257, '4', 'fa410e3b8bd763d7', 'A', '2026-10-26 21:03:16', '自动数据保存成功！单号[WH2026102600203],车牌号[陕KG0159]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (258, '4', 'edb795f874979cfb', 'A', '2026-10-26 21:03:55', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (259, '4', '97bd6579d7e94f3d', 'M', '2026-10-26 21:04:24', '1#识别车牌：甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (260, '4', '2dd509b383abda8c', 'M', '2026-10-26 21:04:26', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"甘D86105","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517377398145027,"receiptType":1,"transportCorpName":"白银圣业商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (261, '4', 'd147013eb216e5ed', 'M', '2026-10-26 21:04:26', '正在下载车牌数据[甘D86105]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (262, '4', '5f4a4a15201c150b', 'A', '2026-10-26 21:04:26', '等待入口车辆上磅:甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (263, '4', 'c52d30c1e2cabda9', 'A', '2026-10-26 21:04:52', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (264, '4', 'd8ee163dd500f06e', 'A', '2026-10-26 21:05:23', '确认完成:甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (265, '4', 'fa88b0c4f4d25857', 'A', '2026-10-26 21:05:29', '自动数据保存成功！单号[WH2026102600204],车牌号[甘D86105]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (266, '4', 'c24b68f942c85680', 'A', '2026-10-26 21:05:54', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (267, '4', 'cd04f630c7d112d8', 'M', '2026-10-26 21:08:17', '1#识别车牌：蒙C29023', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (268, '4', 'f39aa455ec47b93e', 'M', '2026-10-26 21:08:20', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙C29023","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327074885639,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (269, '4', 'c38876fcd18ec2da', 'M', '2026-10-26 21:08:20', '正在下载车牌数据[蒙C29023]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (270, '4', '5dbb0307ecb2b00d', 'A', '2026-10-26 21:08:20', '等待入口车辆上磅:蒙C29023', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (271, '4', '405e7b2997caf837', 'A', '2026-10-26 21:08:44', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (272, '4', 'e4c748ee47c4cd35', 'A', '2026-10-26 21:09:14', '确认完成:蒙C29023', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (273, '4', 'f07f5b160afa586f', 'A', '2026-10-26 21:09:19', '自动数据保存成功！单号[WH2026102600205],车牌号[蒙C29023]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (275, '4', '57add710f598f95d', 'M', '2026-10-26 21:10:06', '1#识别车牌：蒙B74723', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (276, '4', 'b876738329108af5', 'M', '2026-10-26 21:10:08', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B74723","consigneeCorpName":"包头市昱和通贸易有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517372794896385,"receiptType":1,"transportCorpName":"包头市昱和通贸易有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (277, '4', '5c5f39ff438d6599', 'M', '2026-10-26 21:10:08', '正在下载车牌数据[蒙B74723]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (278, '4', '13b497293022db06', 'A', '2026-10-26 21:10:08', '等待入口车辆上磅:蒙B74723', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (279, '4', '55a144a0bd21622b', 'A', '2026-10-26 21:10:31', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (280, '4', '6881d3bd81855252', 'A', '2026-10-26 21:11:02', '确认完成:蒙B74723', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (281, '4', '986e3142ddc005d8', 'A', '2026-10-26 21:11:06', '自动数据保存成功！单号[WH2026102600206],车牌号[蒙B74723]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (282, '4', '666a92b763f875c3', 'A', '2026-10-26 21:11:36', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (283, '4', '2b2ddb4b8192c912', 'M', '2026-10-26 21:11:41', '1#识别车牌：蒙BA2574', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (284, '4', 'b579b6785fc7e35e', 'M', '2026-10-26 21:11:46', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA2574","consigneeCorpName":"中国北方稀土(集团)高科技股份有限公司冶炼分公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517326735147011,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (285, '4', '9bb31c1403cba4c1', 'M', '2026-10-26 21:11:46', '正在下载车牌数据[蒙BA2574]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (286, '4', 'ace05d6d70ca534a', 'A', '2026-10-26 21:11:46', '等待入口车辆上磅:蒙BA2574', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (287, '4', 'f2566c98e33e99e4', 'A', '2026-10-26 21:12:09', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (288, '4', '376e647fa45242aa', 'A', '2026-10-26 21:12:40', '确认完成:蒙BA2574', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (289, '4', 'cfff158c56d0ce2f', 'A', '2026-10-26 21:12:44', '自动数据保存成功！单号[WH2026102600207],车牌号[蒙BA2574]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (290, '4', '41383ce59cc886d9', 'A', '2026-10-26 21:13:26', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (291, '4', 'ef5752f067c29f57', 'M', '2026-10-26 21:18:07', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA1405","consigneeCorpName":"中国北方稀土(集团)高科技股份有限公司冶炼分公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517326735147009,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (292, '4', 'a628eaef058efd66', 'M', '2026-10-26 21:18:07', '正在下载车牌数据[蒙BA1405]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (293, '4', 'eb8d8110842831a1', 'A', '2026-10-26 21:18:07', '等待入口车辆上磅:蒙BA1405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (294, '4', 'aab4e02423642427', 'M', '2026-10-26 21:18:07', '1#识别车牌：蒙BA1405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (295, '4', '70da2ee3c0fb491e', 'A', '2026-10-26 21:18:27', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (296, '4', '271b137dd68d3013', 'A', '2026-10-26 21:18:56', '确认完成:蒙BA1405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (297, '4', '759e77146269314f', 'A', '2026-10-26 21:19:01', '自动数据保存成功！单号[WH2026102600208],车牌号[蒙BA1405]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (298, '4', '4ea0c59fac7a6c13', 'A', '2026-10-26 21:19:41', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (299, '4', '5ed962ef704f36ff', 'M', '2026-10-26 21:20:38', '1#识别车牌：冀A74659', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (300, '4', '9d54aa2c8eba6723', 'M', '2026-10-26 21:20:39', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"冀A74659","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517362856493059,"receiptType":1,"transportCorpName":"晋州市明宝化工商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (301, '4', '2e57d2d88bcf9393', 'M', '2026-10-26 21:20:39', '正在下载车牌数据[冀A74659]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (302, '4', 'ad736b6e0b09c722', 'A', '2026-10-26 21:20:39', '等待入口车辆上磅:冀A74659', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (303, '4', '910e51f91495412a', 'A', '2026-10-26 21:21:04', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (304, '4', 'e85e035306214691', 'A', '2026-10-26 21:21:33', '确认完成:冀A74659', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (305, '4', 'e3b71922bd7971be', 'A', '2026-10-26 21:21:37', '自动数据保存成功！单号[WH2026102600209],车牌号[冀A74659]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (306, '4', 'fb722d9303870d22', 'A', '2026-10-26 21:22:01', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (307, '4', '0b67b043c5833ebd', 'M', '2026-10-26 21:24:34', '2#识别车牌：陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (308, '4', '9eaae2d361720d0b', 'A', '2026-10-26 21:24:35', '等待出口车辆上磅:陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (309, '4', '96da37430321c1ce', 'A', '2026-10-26 21:25:12', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (310, '4', '4d82b37a174617f1', 'A', '2026-10-26 21:25:42', '确认完成:陕KK7948', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (311, '4', 'e706f9eb32a8b2ac', 'A', '2026-10-26 21:25:46', '自动数据保存成功！单号[WH2026102600196],车牌号[陕KK7948]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (312, '4', '01aa7e2da9259193', 'A', '2026-10-26 21:26:16', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (313, '4', 'f52b7dec29a7bd44', 'M', '2026-10-26 21:26:44', '1#识别车牌：蒙B97852', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (314, '4', '5e7c7e1c793123f1', 'M', '2026-10-26 21:26:46', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B97852","consigneeCorpName":"中国北方稀土(集团)高科技股份有限公司冶炼分公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517377496711169,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (315, '4', '7a7b9272686064e0', 'M', '2026-10-26 21:26:46', '正在下载车牌数据[蒙B97852]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (316, '4', '3334958f5a4c5f16', 'A', '2026-10-26 21:26:46', '等待入口车辆上磅:蒙B97852', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (317, '4', 'd3b4f608d0f3e03f', 'A', '2026-10-26 21:27:08', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (318, '4', '81ed558db7282e33', 'A', '2026-10-26 21:27:39', '确认完成:蒙B97852', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (319, '4', '31a591edaa4ace7d', 'A', '2026-10-26 21:27:50', '自动数据保存成功！单号[WH2026102600211],车牌号[蒙B97852]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (320, '4', 'cbe79145be63be68', 'A', '2026-10-26 21:28:15', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (321, '4', '474b98ba8ec793e0', 'M', '2026-10-26 21:28:29', '2#识别车牌：陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (322, '4', 'b4cffe192209dea5', 'A', '2026-10-26 21:28:30', '等待出口车辆上磅:陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (323, '4', '7c1d17c2783daae0', 'A', '2026-10-26 21:28:56', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (324, '4', '19099b766675dab5', 'A', '2026-10-26 21:29:27', '确认完成:陕KK7974', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (325, '4', '122d33141fc19645', 'A', '2026-10-26 21:29:31', '自动数据保存成功！单号[WH2026102600195],车牌号[陕KK7974]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (326, '4', '6ead43fbb4f36b7c', 'A', '2026-10-26 21:29:50', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (327, '4', '62f1ab8a78280555', 'M', '2026-10-26 21:48:58', '2#识别车牌：冀KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (328, '4', '927aa0eb51d03640', 'M', '2026-10-26 21:48:59', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"冀KG0159","consigneeCorpName":"","forwardingCorpName":"","goodsName":"","goodsSpec":"","isGb":0,"maxLoad":0,"minLoad":0,"receiptId":0,"receiptType":0,"transportCorpName":""}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (329, '4', 'e105aa1bdcdd3006', 'M', '2026-10-26 21:48:59', '正在下载车牌数据[冀KG0159]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (330, '4', '32d227f278261acd', 'A', '2026-10-26 21:48:59', '2#[冀KG0159]无效： 不允许过磅<21:48>', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (331, '4', '7882f330ebffe6dc', 'M', '2026-10-26 21:49:23', '2#识别车牌：陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (332, '4', 'a7cf146cbb0136b3', 'A', '2026-10-26 21:49:23', '等待出口车辆上磅:陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (333, '4', '2c67789fd83112ca', 'A', '2026-10-26 21:50:00', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (334, '4', 'c062ef11d32600ad', 'A', '2026-10-26 21:50:30', '确认完成:陕KG0159', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (335, '4', 'fb48f10bdfc35771', 'A', '2026-10-26 21:50:34', '自动数据保存成功！单号[WH2026102600203],车牌号[陕KG0159]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (336, '4', '4abb4af1c525a6a9', 'A', '2026-10-26 21:51:04', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (337, '4', '849b3b03bfafcbb6', 'M', '2026-10-26 21:57:17', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KG4742","consigneeCorpName":"中铝物资有限公司（兴县）","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517314456322049,"receiptType":1,"transportCorpName":"榆林恒瑞达运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (338, '4', '5dae8ced9b70e7e3', 'M', '2026-10-26 21:57:17', '正在下载车牌数据[陕KG4742]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (339, '4', '639f830dc41869f0', 'A', '2026-10-26 21:57:17', '等待入口车辆上磅:陕KG4742', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (340, '4', 'e1308701080b6b75', 'M', '2026-10-26 21:57:17', '1#识别车牌：陕KG4742', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (341, '4', '95fbfd9e89fd6930', 'A', '2026-10-26 21:57:42', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (342, '4', 'f7e52a7db9c99706', 'A', '2026-10-26 21:58:12', '确认完成:陕KG4742', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (343, '4', 'b4e324b1ed93610f', 'A', '2026-10-26 21:58:17', '自动数据保存成功！单号[WH2026102600217],车牌号[陕KG4742]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (344, '4', 'd68a644532e685f8', 'A', '2026-10-26 21:58:38', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (345, '4', '43faf7f8b9cc4d52', 'M', '2026-10-26 22:12:40', '1#识别车牌：蒙B98146', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (346, '4', '07dc59dffed574ad', 'M', '2026-10-26 22:12:41', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B98146","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327072788484,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (347, '4', '40d1a451917388e7', 'M', '2026-10-26 22:12:41', '正在下载车牌数据[蒙B98146]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (348, '4', '9c0364ac3aa000ca', 'A', '2026-10-26 22:12:42', '等待入口车辆上磅:蒙B98146', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (349, '4', '7ed98c49f0333820', 'A', '2026-10-26 22:13:03', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (350, '4', 'c8894e6b99052776', 'A', '2026-10-26 22:13:33', '确认完成:蒙B98146', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (351, '4', 'fd00739adcb496f8', 'A', '2026-10-26 22:13:37', '自动数据保存成功！单号[WH2026102600220],车牌号[蒙B98146]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (352, '4', '2f7addc4667d473c', 'A', '2026-10-26 22:13:58', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (353, '4', '07a09214ac8a4a5d', 'M', '2026-10-26 22:14:12', '1#识别车牌：蒙BA7473', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (354, '4', 'd6f540409b6d076c', 'M', '2026-10-26 22:14:13', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA7473","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327076982786,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (355, '4', '032edae86ecc829c', 'M', '2026-10-26 22:14:13', '正在下载车牌数据[蒙BA7473]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (356, '4', 'cf93f0560a1675f6', 'A', '2026-10-26 22:14:13', '等待入口车辆上磅:蒙BA7473', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (357, '4', '42761ad9f37631ea', 'A', '2026-10-26 22:14:34', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (358, '4', '6f8ec3a39bfef056', 'A', '2026-10-26 22:15:06', '确认完成:蒙BA7473', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (359, '4', '7a2a06db2b433a6b', 'A', '2026-10-26 22:15:10', '自动数据保存成功！单号[WH2026102600221],车牌号[蒙BA7473]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (360, '4', '0ca2ff0cd44ccfb2', 'A', '2026-10-26 22:15:38', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (361, '4', '32ccaaac42671748', 'M', '2026-10-26 22:15:55', '1#识别车牌：蒙BA1244', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (362, '4', '49eede0fc96746f9', 'M', '2026-10-26 22:15:57', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA1244","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327074885635,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (363, '4', '0a616cb3cb1f6c9e', 'M', '2026-10-26 22:15:57', '正在下载车牌数据[蒙BA1244]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (364, '4', '0e87c011e49bbc94', 'A', '2026-10-26 22:15:57', '等待入口车辆上磅:蒙BA1244', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (365, '4', 'f4c00ba84c59676a', 'A', '2026-10-26 22:16:24', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (366, '4', '13b9c32794ec97b3', 'A', '2026-10-26 22:16:54', '确认完成:蒙BA1244', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (367, '4', 'e5fb19dda88bfa89', 'A', '2026-10-26 22:16:58', '自动数据保存成功！单号[WH2026102600222],车牌号[蒙BA1244]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (368, '4', '0f6e8e0da06afd18', 'A', '2026-10-26 22:17:34', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (369, '4', '1b6019aa5f496635', 'M', '2026-10-26 22:22:41', '1#识别车牌：陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (370, '4', '18b55512cbb6c2fd', 'M', '2026-10-26 22:22:42', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕J73277","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517377286995969,"receiptType":1,"transportCorpName":"绥德县德顺昌能源有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (371, '4', '3daeb448a755e4cc', 'M', '2026-10-26 22:22:42', '正在下载车牌数据[陕J73277]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (372, '4', 'edca7281fd1324b7', 'A', '2026-10-26 22:22:42', '等待入口车辆上磅:陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (373, '4', 'db577217a88d653a', 'A', '2026-10-26 22:23:08', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (374, '4', 'b9f5f480eed40599', 'A', '2026-10-26 22:23:39', '确认完成:陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (375, '4', '724e9a562a5fc552', 'A', '2026-10-26 22:23:43', '自动数据保存成功！单号[WH2026102600223],车牌号[陕J73277]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (376, '4', '1cbd244b75455418', 'A', '2026-10-26 22:24:20', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (377, '4', '337e19e33d4d3d5c', 'M', '2026-10-26 22:29:57', '1#识别车牌：蒙C12192', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (378, '4', 'a9e663a578f912fc', 'M', '2026-10-26 22:29:58', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙C12192","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327074885637,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (379, '4', '3a6b9de2feddfb6d', 'M', '2026-10-26 22:29:58', '正在下载车牌数据[蒙C12192]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (380, '4', '3714ff7293c47536', 'A', '2026-10-26 22:29:58', '等待入口车辆上磅:蒙C12192', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (381, '4', '21c8b85d23eaebb8', 'A', '2026-10-26 22:30:21', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (382, '4', 'ebba1dd95f47b864', 'A', '2026-10-26 22:31:20', '确认完成:蒙C12192', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (383, '4', 'd47429adc725e13d', 'A', '2026-10-26 22:31:24', '自动数据保存成功！单号[WH2026102600225],车牌号[蒙C12192]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (384, '4', '0c9b5c44702021c8', 'A', '2026-10-26 22:31:54', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (385, '4', 'ad5860a61cbf7264', 'M', '2026-10-26 22:39:26', '1#识别车牌：蒙BA2183', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (386, '4', '4d131f96ad5a680a', 'M', '2026-10-26 22:39:28', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BA2183","consigneeCorpName":"中国北方稀土(集团)高科技股份有限公司冶炼分公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517377494614017,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (387, '4', '74d069f1c08f507f', 'M', '2026-10-26 22:39:28', '正在下载车牌数据[蒙BA2183]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (388, '4', 'a4175313efc4583c', 'A', '2026-10-26 22:39:28', '等待入口车辆上磅:蒙BA2183', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (389, '4', '5b318a18d80b0c83', 'A', '2026-10-26 22:39:49', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (390, '4', '7d278a3a0e4b0629', 'A', '2026-10-26 22:40:19', '确认完成:蒙BA2183', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (391, '4', 'b61314058f92734b', 'A', '2026-10-26 22:40:24', '自动数据保存成功！单号[WH2026102600228],车牌号[蒙BA2183]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (392, '4', '5b0ebc78e3f12f7a', 'A', '2026-10-26 22:40:53', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (393, '4', 'ac455629ad22e560', 'M', '2026-10-26 22:44:53', '1#识别车牌：蒙BB2533', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (394, '4', 'cf75d5e5e3d0e8f5', 'M', '2026-10-26 22:44:55', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BB2533","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327072788488,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (395, '4', 'e170380c3e394e76', 'M', '2026-10-26 22:44:55', '正在下载车牌数据[蒙BB2533]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (396, '4', '2188081d4e968118', 'A', '2026-10-26 22:44:55', '等待入口车辆上磅:蒙BB2533', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (397, '4', '110a1111275d57fd', 'A', '2026-10-26 22:45:15', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (398, '4', 'dc0a461c3f36bdf7', 'A', '2026-10-26 22:45:45', '确认完成:蒙BB2533', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (399, '4', '81aee9fc92053d7a', 'A', '2026-10-26 22:45:49', '自动数据保存成功！单号[WH2026102600229],车牌号[蒙BB2533]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (400, '4', '96059c82baf81789', 'A', '2026-10-26 22:46:33', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (401, '4', '5d15cb3cafe82557', 'M', '2026-10-26 22:59:18', '2#识别车牌：冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (402, '4', '7f57bf3047bfec84', 'A', '2026-10-26 22:59:18', '等待出口车辆上磅:冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (403, '4', '4c53ae89150f00b6', 'A', '2026-10-26 22:59:45', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (404, '4', '30bcf63513cdcb6b', 'A', '2026-10-26 23:00:16', '确认完成:冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (405, '4', '1939f9ba7a485764', 'A', '2026-10-26 23:00:25', '自动数据保存失败！单号[WH2026102600067]:毛重(49.04)超出载重范围内:(0 ~ 49)', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (406, '4', '85d6eda33862d95d', '', NULL, '[AUTO_PROCESSED] 2026-10-26 23:01:29 超重已处理', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (407, '4', '46fb9aea034213a5', 'A', '2026-10-26 23:01:30', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (408, '4', 'd6f091d1f80bb1be', 'M', '2026-10-26 23:02:22', '2#识别车牌：冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (409, '4', 'd86de45afb0a26cb', 'A', '2026-10-26 23:02:22', '等待出口车辆上磅:冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (410, '4', '8f9d9c16200279c4', 'A', '2026-10-26 23:02:59', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (411, '4', '670cc290780d0fe0', 'A', '2026-10-26 23:03:29', '确认完成:冀A650BP', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (412, '4', 'a4b6810e7efeebfa', 'A', '2026-10-26 23:03:41', '自动数据保存成功！单号[WH2026102600067],车牌号[冀A650BP]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (413, '4', 'b0c3a38bc18faf00', 'A', '2026-10-26 23:04:14', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (414, '4', 'dc423eda1608207e', 'M', '2026-10-26 23:07:05', '1#识别车牌：陕J92171', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (415, '4', '160e9ca1d3788f2e', 'M', '2026-10-26 23:07:06', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕J92171","consigneeCorpName":"内蒙古灵圣作物科技有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":516952966037509,"receiptType":1,"transportCorpName":"盐城市中捷物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (416, '4', '6ec70c0c6397e7b2', 'M', '2026-10-26 23:07:06', '正在下载车牌数据[陕J92171]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (417, '4', '2a701f366d58bce1', 'A', '2026-10-26 23:07:06', '等待入口车辆上磅:陕J92171', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (418, '4', '8b76aab6d6bb6d5d', 'A', '2026-10-26 23:07:31', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (419, '4', '6f6886f123d90301', 'A', '2026-10-26 23:08:02', '确认完成:陕J92171', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (420, '4', '26a37f5395b90006', 'A', '2026-10-26 23:08:06', '自动数据保存成功！单号[WH2026102600231],车牌号[陕J92171]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (421, '4', '7a09262cc8871173', 'A', '2026-10-26 23:08:42', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (422, '4', 'c51a8fd999b9c2da', 'M', '2026-10-26 23:16:50', '1#识别车牌：陕KH9405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (423, '4', '39a996033c6d9523', 'M', '2026-10-26 23:16:52', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KH9405","consigneeCorpName":"孝义市兴安化工有限公司","forwardingCorpName":"神木化工","goodsName":"烧碱（50）","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517343801769989,"receiptType":1,"transportCorpName":"榆林市聚润危险品运输服务有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (424, '4', '49eac2a49406fb3b', 'M', '2026-10-26 23:16:52', '正在下载车牌数据[陕KH9405]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (425, '4', '40186e229d75d6aa', 'A', '2026-10-26 23:16:52', '等待入口车辆上磅:陕KH9405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (426, '4', '280d46bc1522b8fb', 'A', '2026-10-26 23:17:20', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (427, '4', '435174e86a7e1cd7', 'A', '2026-10-26 23:17:50', '确认完成:陕KH9405', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (428, '4', '4bd5d271ce8e975a', 'A', '2026-10-26 23:17:54', '自动数据保存成功！单号[WH2026102600232],车牌号[陕KH9405]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (429, '4', '74af5cb25b4bebd1', 'A', '2026-10-26 23:18:21', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (430, '4', '08261ce2eda5edd7', 'M', '2026-10-26 23:28:17', '2#识别车牌：陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (431, '4', '31746330dc7383b1', 'A', '2026-10-26 23:28:17', '等待出口车辆上磅:陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (432, '4', '7204dfd5ec201375', 'A', '2026-10-26 23:28:42', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (433, '4', 'e3799b50b68c5909', 'A', '2026-10-26 23:29:13', '确认完成:陕J73277', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (434, '4', 'd4d7d5cc6ab3d268', 'A', '2026-10-26 23:29:17', '自动数据保存成功！单号[WH2026102600223],车牌号[陕J73277]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (435, '4', '64704889019b9f69', 'A', '2026-10-26 23:29:43', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (436, '4', 'ea8ab4b780eb0c7d', 'M', '2026-10-26 23:30:10', '1#识别车牌：蒙B98781', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (437, '4', '03aaad50536aca29', 'M', '2026-10-26 23:30:11', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙B98781","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327074885633,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (438, '4', '67cc4c293e8f1c57', 'M', '2026-10-26 23:30:11', '正在下载车牌数据[蒙B98781]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (439, '4', 'b5e3ea81561db1f6', 'A', '2026-10-26 23:30:11', '等待入口车辆上磅:蒙B98781', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (440, '4', 'fc912ac3cde2d2e4', 'A', '2026-10-26 23:30:32', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (441, '4', '7fa5a363b875b2f6', 'A', '2026-10-26 23:31:03', '确认完成:蒙B98781', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (442, '4', 'f40fddc0ade741cc', 'A', '2026-10-26 23:31:07', '自动数据保存成功！单号[WH2026102600235],车牌号[蒙B98781]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (443, '4', 'b9a98cf05e9ed48f', 'A', '2026-10-26 23:31:36', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (444, '4', 'e589253034d30f08', 'M', '2026-10-26 23:48:36', '1#识别车牌：蒙BB1973', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (445, '4', 'cc3732a1724e1ed8', 'M', '2026-10-26 23:48:38', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙BB1973","consigneeCorpName":"包头华美稀土高科有限公司","forwardingCorpName":"神木化工","goodsName":"高纯盐酸","goodsSpec":"产成品","isGb":1,"maxLoad":49.00,"minLoad":0.00,"receiptId":517327072788486,"receiptType":1,"transportCorpName":"包头市锦联运输有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (446, '4', 'bfca4ae3ce806d0e', 'M', '2026-10-26 23:48:38', '正在下载车牌数据[蒙BB1973]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (447, '4', 'b0932b7d3553b804', 'A', '2026-10-26 23:48:38', '等待入口车辆上磅:蒙BB1973', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (448, '4', '75af13887324c2cd', 'A', '2026-10-26 23:49:02', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (449, '4', 'a3be210f5996f6a0', 'A', '2026-10-26 23:49:31', '确认完成:蒙BB1973', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (450, '4', '56a97212205dac98', 'A', '2026-10-26 23:49:36', '自动数据保存成功！单号[WH2026102600237],车牌号[蒙BB1973]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (451, '4', 'fbd07b2662c3e8ba', 'A', '2026-10-26 23:50:07', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (452, '4', 'c8627e59fc688bcf', 'A', '2026-10-27 08:31:15', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (453, '4', '0c5f233a0a1c61f5', 'A', '2026-10-27 08:31:45', '确认完成:陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (454, '4', 'be8459dda9ff7a61', 'A', '2026-10-27 08:31:48', '自动数据保存失败！单号[WH2026102700016]:毛重(49.02)超出载重范围内:(0 ~ 49)', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (455, '4', '4514475b86bf09d0', '', NULL, '[AUTO_PROCESSED] 2026-10-27 08:32:49 超重已处理', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (456, '4', '12a50018fa6987b7', 'A', '2026-10-27 08:33:29', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (457, '4', '11a93b075aca39cb', 'M', '2026-10-27 08:33:37', '2#识别车牌：陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (458, '4', 'ae32bfb06dab512b', 'A', '2026-10-27 08:33:37', '车辆下磅后清空出场车牌:陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (459, '4', '20744d89e05de795', 'M', '2026-10-27 08:33:45', '1#识别车牌：蒙J92102', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (460, '4', 'ff2ae7b0c0eb4e08', 'M', '2026-10-27 08:33:47', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"蒙J92102","consigneeCorpName":"内蒙古盛汇化学有限公司","forwardingCorpName":"神木化工","goodsName":"液氯","goodsSpec":"产成品","isGb":1,"maxLoad":48.00,"minLoad":0.00,"receiptId":517391405023233,"receiptType":1,"transportCorpName":"内蒙古盛汇化学有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (461, '4', '5665f03d0cc615e0', 'M', '2026-10-27 08:33:47', '正在下载车牌数据[蒙J92102]...', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (462, '4', '9c04d3ce15251b17', 'A', '2026-10-27 08:33:47', '等待入口车辆上磅:蒙J92102', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (463, '4', '693c47891e3b06f3', 'A', '2026-10-27 08:34:11', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (464, '4', 'fa07c8b26f7c3d51', 'A', '2026-10-27 08:34:41', '确认完成:蒙J92102', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (465, '4', '18b68a0b46613c50', 'A', '2026-10-27 08:34:46', '自动数据保存成功！单号[WH2026102700042],车牌号[蒙J92102]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (466, '4', '8b163aafe77c14af', 'A', '2026-10-27 08:35:13', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (467, '4', '2f68ca4871abd1fb', 'M', '2026-10-27 08:35:57', '2#识别车牌：陕KD2691', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (468, '4', '7650fa3f88194b07', 'A', '2026-10-27 08:35:58', '等待出口车辆上磅:陕KD2691', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (469, '4', 'a84990ec36af8d6e', 'A', '2026-10-27 08:36:22', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (470, '4', 'f881ba338c3bd167', 'A', '2026-10-27 08:36:52', '确认完成:陕KD2691', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (471, '4', '40c5bd303c935066', 'A', '2026-10-27 08:37:14', '自动数据保存成功！单号[WH2026102600070],车牌号[陕KD2691]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (472, '4', '2165772dca94e783', 'A', '2026-10-27 08:37:36', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (473, '4', 'c92c32dc926d790d', 'M', '2026-10-27 08:37:55', '2#识别车牌：陕KK4084', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (474, '4', '1a65dfc8b19dd415', 'A', '2026-10-27 08:37:56', '等待出口车辆上磅:陕KK4084', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (475, '4', '19cbaba00774839e', 'A', '2026-10-27 08:38:18', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (476, '4', 'f0c23855c183da25', 'A', '2026-10-27 08:38:48', '确认完成:陕KK4084', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (477, '4', '3339c4ff3f186992', 'A', '2026-10-27 08:38:52', '自动数据保存成功！单号[WH2026102700017],车牌号[陕KK4084]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (478, '4', '0b4ad2646e263b1d', 'A', '2026-10-27 08:39:14', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (479, '4', 'b75ec6af58375f4e', 'M', '2026-10-27 08:39:35', '2#识别车牌：陕KJ7161', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (480, '4', '2f0c05e9a8b6c2c3', 'A', '2026-10-27 08:39:36', '等待出口车辆上磅:陕KJ7161', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (481, '4', '8d3593e4261cabf9', 'A', '2026-10-27 08:40:00', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (482, '4', 'e636888602885a10', 'A', '2026-10-27 08:40:29', '确认完成:陕KJ7161', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (483, '4', '7b6f3c3e89b47341', 'A', '2026-10-27 08:40:33', '自动数据保存成功！单号[WH2026102700021],车牌号[陕KJ7161]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (484, '4', 'a48ecabfa28e8bcf', 'A', '2026-10-27 08:41:03', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (485, '4', 'c280d7370cf8935a', 'M', '2026-10-27 08:41:09', '2#识别车牌：陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (486, '4', '561b9b6ad8e511af', 'A', '2026-10-27 08:41:12', '等待出口车辆上磅:陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (487, '4', '120f4adc3a591078', 'A', '2026-10-27 08:41:43', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (488, '4', '6239ce1178609dd8', 'M', '2026-10-27 08:42:10', '1#摄像头视频重连:', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (489, '4', '17ea94b4744452f1', 'A', '2026-10-27 08:42:27', '确认完成:陕KR3801', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (490, '4', 'f4989ced4ce489e9', 'A', '2026-10-27 08:42:36', '自动数据保存成功！单号[WH2026102700016],车牌号[陕KR3801]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (491, '4', 'c0e32c5cbb1ede99', 'A', '2026-10-27 08:43:35', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (492, '4', '2fb25153c1336c3f', 'M', '2026-10-27 08:43:45', '2#识别车牌：晋JL7085', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (493, '4', '1bbd994c27f6119b', 'A', '2026-10-27 08:43:46', '等待出口车辆上磅:晋JL7085', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (494, '4', '70c6c13ee62e01c5', 'A', '2026-10-27 08:44:12', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (495, '4', '18161181b6b2dfcc', 'A', '2026-10-27 08:44:43', '确认完成:晋JL7085', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (496, '4', '88bbffac5e01b8aa', 'A', '2026-10-27 08:44:47', '自动数据保存成功！单号[WH2026102700019],车牌号[晋JL7085]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (497, '4', '45a8b671b5640e25', 'A', '2026-10-27 08:45:11', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (498, '4', 'bcb3eebb9d610d05', 'M', '2026-10-27 08:45:26', '2#识别车牌：陕KG2755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (499, '4', '1bbb79d8eeeae3a1', 'A', '2026-10-27 08:45:27', '等待出口车辆上磅:陕KG2755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (500, '4', 'f9e8c4f06382edfa', 'A', '2026-10-27 08:45:47', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (501, '4', '5a8d852b9a2b6a1d', 'A', '2026-10-27 08:46:17', '确认完成:陕KG2755', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (502, '4', '911186991d8bd7e7', 'A', '2026-10-27 08:46:21', '自动数据保存成功！单号[WH2026102600177],车牌号[陕KG2755]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (503, '4', '5f04e19885d9acd4', 'A', '2026-10-27 08:46:53', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (504, '4', '07f36a583935bc59', 'M', '2026-10-27 08:47:50', '2#识别车牌：陕KK0486', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (505, '4', '3c2a6b0a22e8d5bd', 'A', '2026-10-27 08:47:51', '等待出口车辆上磅:陕KK0486', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (506, '4', '3eaa8d35aaf06635', 'A', '2026-10-27 08:48:20', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (507, '4', 'db262efac6d38f06', 'A', '2026-10-27 08:48:50', '确认完成:陕KK0486', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (508, '4', 'da3f65e7edf6897e', 'A', '2026-10-27 08:48:54', '自动数据保存成功！单号[WH2026102700018],车牌号[陕KK0486]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (509, '4', 'b3d9a1e9791e138c', 'A', '2026-10-27 08:49:17', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (510, '4', 'fa077b4a579b3080', 'M', '2026-10-27 08:49:40', '2#识别车牌：陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (511, '4', 'db11a0b140d43a72', 'A', '2026-10-27 08:49:40', '等待出口车辆上磅:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (512, '4', '77cd593247d66713', 'A', '2026-10-27 08:50:03', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (513, '4', '788f8361c2076d2a', 'A', '2026-10-27 08:50:34', '确认完成:陕KD8395', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (514, '4', '1cf6cd2680cca66d', 'A', '2026-10-27 08:50:38', '自动数据保存成功！单号[WH2026102700022],车牌号[陕KD8395]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (515, '4', '31e3316bf0504cb9', 'A', '2026-10-27 08:51:01', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (516, '4', 'afead4bd58d5f349', 'M', '2026-10-27 08:51:16', '2#识别车牌：陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (517, '4', '0ec2ced0ac431378', 'A', '2026-10-27 08:51:17', '等待出口车辆上磅:陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (518, '4', '3869f78c6b92fbed', 'A', '2026-10-27 08:51:39', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (519, '4', '7f9ca265daf872b0', 'A', '2026-10-27 08:52:09', '确认完成:陕KR9795', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (520, '4', '1aeb699ac023c231', 'A', '2026-10-27 08:52:12', '自动数据保存成功！单号[WH2026102700027],车牌号[陕KR9795]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (521, '4', '78ca384ce3276d9e', 'A', '2026-10-27 08:52:43', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (522, '4', '44266b010d2dec78', 'M', '2026-10-27 08:54:34', '2#识别车牌：甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (523, '4', '3228958c1069d13b', 'A', '2026-10-27 08:54:35', '等待出口车辆上磅:甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (524, '4', '1e800b32cb236661', 'A', '2026-10-27 08:54:54', '等待确认车牌号', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (525, '4', 'ad64c3bad881ef94', 'A', '2026-10-27 08:55:24', '确认完成:甘D86105', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (526, '4', 'aa9607eb24ca0442', 'A', '2026-10-27 08:55:28', '自动数据保存成功！单号[WH2026102600204],车牌号[甘D86105]', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (527, '4', '25e60566591b26be', 'A', '2026-10-27 08:55:57', '车辆已下磅', '2026-10-27 08:54:47.495443');
INSERT INTO "gb_record"."gb_log" VALUES (528, '4', '80f2e42bb8496783', 'A', '2026-10-27 08:56:34', '等待出口车辆上磅:陕KM8567', '2026-10-27 08:54:52.602275');
INSERT INTO "gb_record"."gb_log" VALUES (529, '4', '0fa196d6861ecb84', 'M', '2026-10-27 08:56:34', '2#识别车牌：陕KM8567', '2026-10-27 08:54:52.602275');
INSERT INTO "gb_record"."gb_log" VALUES (530, '4', 'e63aa9ae016c7c1b', 'A', '2026-10-27 08:56:58', '等待确认车牌号', '2026-10-27 08:55:16.706547');
INSERT INTO "gb_record"."gb_log" VALUES (531, '4', '6e7cfa3153f99fda', 'A', '2026-10-27 08:57:28', '确认完成:陕KM8567', '2026-10-27 08:55:46.742865');
INSERT INTO "gb_record"."gb_log" VALUES (532, '4', 'f9fe4dc4c2305b18', 'A', '2026-10-27 08:57:33', '自动数据保存成功！单号[WH2026102700030],车牌号[陕KM8567]', '2026-10-27 08:55:51.774026');
INSERT INTO "gb_record"."gb_log" VALUES (533, '4', 'f61fe3d68b7d3caf', 'A', '2026-10-27 08:57:59', '车辆已下磅', '2026-10-27 08:56:17.814476');
INSERT INTO "gb_record"."gb_log" VALUES (534, '4', 'e9d30551fd2d0959', 'M', '2026-10-27 08:58:36', '2#识别车牌：陕KN6270', '2026-10-27 08:56:54.851049');
INSERT INTO "gb_record"."gb_log" VALUES (535, '4', '33daf42bd45c487d', 'A', '2026-10-27 08:58:37', '等待出口车辆上磅:陕KN6270', '2026-10-27 08:56:55.899294');
INSERT INTO "gb_record"."gb_log" VALUES (536, '4', 'd8d65805d92aedbc', 'A', '2026-10-27 08:58:59', '等待确认车牌号', '2026-10-27 08:57:17.932204');
INSERT INTO "gb_record"."gb_log" VALUES (537, '4', '0df45bc5fff5bdc6', 'A', '2026-10-27 08:59:29', '确认完成:陕KN6270', '2026-10-27 08:57:47.966736');
INSERT INTO "gb_record"."gb_log" VALUES (538, '4', '9234e93c67ba5470', 'A', '2026-10-27 08:59:33', '自动数据保存成功！单号[WH2026102600076],车牌号[陕KN6270]', '2026-10-27 08:57:51.995899');
INSERT INTO "gb_record"."gb_log" VALUES (539, '4', 'c6c8a8c2db098b53', 'A', '2026-10-27 09:00:00', '车辆已下磅', '2026-10-27 08:58:19.035681');
INSERT INTO "gb_record"."gb_log" VALUES (540, '4', '2fde5663930bb544', 'M', '2026-10-27 09:04:51', '2#识别车牌：陕J78491', '2026-10-27 09:03:10.108671');
INSERT INTO "gb_record"."gb_log" VALUES (541, '4', '1f24d7e4bc92493c', 'A', '2026-10-27 09:04:52', '等待出口车辆上磅:陕J78491', '2026-10-27 09:03:11.14734');
INSERT INTO "gb_record"."gb_log" VALUES (542, '4', '69ee19e6410991e9', 'A', '2026-10-27 09:05:23', '等待确认车牌号', '2026-10-27 09:03:42.186196');
INSERT INTO "gb_record"."gb_log" VALUES (543, '4', '632df2f347fcbbd4', 'A', '2026-10-27 09:05:53', '确认完成:陕J78491', '2026-10-27 09:04:11.227817');
INSERT INTO "gb_record"."gb_log" VALUES (544, '4', 'b0c3fa299f346887', 'A', '2026-10-27 09:05:57', '自动数据保存成功！单号[WH2026102700036],车牌号[陕J78491]', '2026-10-27 09:04:15.259916');
INSERT INTO "gb_record"."gb_log" VALUES (545, '4', 'fb9eef5038ca4915', 'A', '2026-10-27 09:06:28', '车辆已下磅', '2026-10-27 09:04:47.308379');
INSERT INTO "gb_record"."gb_log" VALUES (547, '4', 'd7e8c8d34192167f', 'M', '2026-10-27 09:19:50', '1#识别车牌：陕KJ7161', '2026-10-27 09:18:09.012087');
INSERT INTO "gb_record"."gb_log" VALUES (548, '4', '27ffcc890f987715', 'M', '2026-10-27 09:19:51', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KJ7161","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830240702473,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:18:10.202404');
INSERT INTO "gb_record"."gb_log" VALUES (549, '4', 'a4b1c2800707aa7f', 'M', '2026-10-27 09:19:51', '正在下载车牌数据[陕KJ7161]...', '2026-10-27 09:18:10.202404');
INSERT INTO "gb_record"."gb_log" VALUES (550, '4', '56b90ceb56179ad7', 'A', '2026-10-27 09:19:52', '等待入口车辆上磅:陕KJ7161', '2026-10-27 09:18:10.202404');
INSERT INTO "gb_record"."gb_log" VALUES (551, '4', '9cbf588dd9269965', 'A', '2026-10-27 09:20:10', '等待确认车牌号', '2026-10-27 09:18:28.354409');
INSERT INTO "gb_record"."gb_log" VALUES (552, '4', 'f5c0811d38d02c84', 'A', '2026-10-27 09:20:40', '确认完成:陕KJ7161', '2026-10-27 09:18:59.400504');
INSERT INTO "gb_record"."gb_log" VALUES (553, '4', 'e739167856f07f1a', 'A', '2026-10-27 09:20:44', '自动数据保存成功！单号[WH2026102700052],车牌号[陕KJ7161]', '2026-10-27 09:19:03.434099');
INSERT INTO "gb_record"."gb_log" VALUES (554, '4', '538d443675c815b8', 'A', '2026-10-27 09:21:00', '车辆已下磅', '2026-10-27 09:19:18.601345');
INSERT INTO "gb_record"."gb_log" VALUES (555, '4', 'a8e97e24fa3991b8', 'M', '2026-10-27 09:26:44', '1#识别车牌：陕KJ7161', '2026-10-27 09:25:02.688702');
INSERT INTO "gb_record"."gb_log" VALUES (556, '4', 'ede7bf01beb9efb3', 'A', '2026-10-27 09:26:44', '等待入口车辆上磅:陕KJ7161', '2026-10-27 09:25:03.765644');
INSERT INTO "gb_record"."gb_log" VALUES (557, '4', '487b6e26a3a00e1d', 'A', '2026-10-27 09:27:06', '等待确认车牌号', '2026-10-27 09:25:24.835066');
INSERT INTO "gb_record"."gb_log" VALUES (558, '4', 'e0795d702f4c3d8d', 'A', '2026-10-27 09:27:36', '确认完成:陕KJ7161', '2026-10-27 09:25:54.878154');
INSERT INTO "gb_record"."gb_log" VALUES (559, '4', '6ec7e703b23dad2c', 'A', '2026-10-27 09:27:40', '自动数据保存成功！单号[WH2026102700052],车牌号[陕KJ7161]', '2026-10-27 09:25:58.958764');
INSERT INTO "gb_record"."gb_log" VALUES (560, '4', 'c184ae0a4671aea6', 'A', '2026-10-27 09:27:57', '车辆已下磅', '2026-10-27 09:26:16.076919');
INSERT INTO "gb_record"."gb_log" VALUES (561, '4', 'f4fa00ce782c04c3', 'M', '2026-10-27 09:36:21', '1#识别车牌：陕KD8395', '2026-10-27 09:34:40.251702');
INSERT INTO "gb_record"."gb_log" VALUES (562, '4', 'a82304a10a717a83', 'M', '2026-10-27 09:36:22', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KD8395","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830238605317,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:34:41.311246');
INSERT INTO "gb_record"."gb_log" VALUES (563, '4', 'a53859ccb892b246', 'M', '2026-10-27 09:36:22', '正在下载车牌数据[陕KD8395]...', '2026-10-27 09:34:41.311246');
INSERT INTO "gb_record"."gb_log" VALUES (564, '4', 'b1703a0af674d07f', 'A', '2026-10-27 09:36:22', '等待入口车辆上磅:陕KD8395', '2026-10-27 09:34:41.311246');
INSERT INTO "gb_record"."gb_log" VALUES (565, '4', '5148562dc3c575c5', 'A', '2026-10-27 09:36:40', '等待确认车牌号', '2026-10-27 09:34:58.390295');
INSERT INTO "gb_record"."gb_log" VALUES (566, '4', '12b36f0e14edc7b7', 'A', '2026-10-27 09:37:10', '确认完成:陕KD8395', '2026-10-27 09:35:28.430721');
INSERT INTO "gb_record"."gb_log" VALUES (567, '4', '7c21db1a28857a9e', 'A', '2026-10-27 09:37:14', '自动数据保存成功！单号[WH2026102700055],车牌号[陕KD8395]', '2026-10-27 09:35:33.469358');
INSERT INTO "gb_record"."gb_log" VALUES (568, '4', 'b31222c85421fcdb', 'A', '2026-10-27 09:37:35', '车辆已下磅', '2026-10-27 09:35:53.537201');
INSERT INTO "gb_record"."gb_log" VALUES (569, '4', 'c6404f0d441dffbd', 'M', '2026-10-27 09:45:02', '1#识别车牌：陕KR9795', '2026-10-27 09:43:20.639027');
INSERT INTO "gb_record"."gb_log" VALUES (570, '4', 'c2e8bbe72a8c659a', 'M', '2026-10-27 09:45:03', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KR9795","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516476195307529,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:43:22.677655');
INSERT INTO "gb_record"."gb_log" VALUES (571, '4', 'e6903d19a8e70e37', 'M', '2026-10-27 09:45:03', '正在下载车牌数据[陕KR9795]...', '2026-10-27 09:43:22.677655');
INSERT INTO "gb_record"."gb_log" VALUES (572, '4', '1392a52bf5722ee1', 'A', '2026-10-27 09:45:03', '等待入口车辆上磅:陕KR9795', '2026-10-27 09:43:22.677655');
INSERT INTO "gb_record"."gb_log" VALUES (573, '4', 'd13a8feba5112ba5', 'A', '2026-10-27 09:45:22', '等待确认车牌号', '2026-10-27 09:43:40.718673');
INSERT INTO "gb_record"."gb_log" VALUES (574, '4', 'bebc026e1951f7fb', 'A', '2026-10-27 09:45:52', '确认完成:陕KR9795', '2026-10-27 09:44:10.768679');
INSERT INTO "gb_record"."gb_log" VALUES (575, '4', 'faeb0fbccb11491e', 'A', '2026-10-27 09:45:56', '自动数据保存成功！单号[WH2026102700057],车牌号[陕KR9795]', '2026-10-27 09:44:14.802762');
INSERT INTO "gb_record"."gb_log" VALUES (576, '4', '023b00b3110942d6', 'A', '2026-10-27 09:46:13', '车辆已下磅', '2026-10-27 09:44:31.845184');
INSERT INTO "gb_record"."gb_log" VALUES (577, '4', '9e44652822d1d903', 'M', '2026-10-27 09:46:24', '1#识别车牌：陕KM8567', '2026-10-27 09:44:42.883458');
INSERT INTO "gb_record"."gb_log" VALUES (578, '4', 'df623cac61bb00a5', 'M', '2026-10-27 09:46:25', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KM8567","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830240702469,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:44:43.921648');
INSERT INTO "gb_record"."gb_log" VALUES (579, '4', '0310db9fec9801e9', 'M', '2026-10-27 09:46:25', '正在下载车牌数据[陕KM8567]...', '2026-10-27 09:44:43.921648');
INSERT INTO "gb_record"."gb_log" VALUES (580, '4', '82add64b702e1d19', 'A', '2026-10-27 09:46:25', '等待入口车辆上磅:陕KM8567', '2026-10-27 09:44:43.921648');
INSERT INTO "gb_record"."gb_log" VALUES (581, '4', '5dcd185701a58b4e', 'A', '2026-10-27 09:46:49', '等待确认车牌号', '2026-10-27 09:45:07.968392');
INSERT INTO "gb_record"."gb_log" VALUES (582, '4', 'd5fa464f7739f73b', 'A', '2026-10-27 09:47:19', '确认完成:陕KM8567', '2026-10-27 09:45:38.007964');
INSERT INTO "gb_record"."gb_log" VALUES (583, '4', '7a3f7f3b3f8977d5', 'A', '2026-10-27 09:47:23', '自动数据保存成功！单号[WH2026102700058],车牌号[陕KM8567]', '2026-10-27 09:45:42.04285');
INSERT INTO "gb_record"."gb_log" VALUES (584, '4', '489b2ece7fbf38e2', 'A', '2026-10-27 09:47:48', '车辆已下磅', '2026-10-27 09:46:07.08909');
INSERT INTO "gb_record"."gb_log" VALUES (585, '4', 'd68ecac098e34cd5', 'M', '2026-10-27 09:48:42', '1#识别车牌：冀BE55M7', '2026-10-27 09:47:01.133903');
INSERT INTO "gb_record"."gb_log" VALUES (586, '4', 'b119bd0556c13116', 'A', '2026-10-27 09:48:42', '等待入口车辆上磅:冀BE55M7', '2026-10-27 09:47:01.133903');
INSERT INTO "gb_record"."gb_log" VALUES (587, '4', 'a671d76e0580d351', 'A', '2026-10-27 09:49:01', '等待确认车牌号', '2026-10-27 09:47:19.509431');
INSERT INTO "gb_record"."gb_log" VALUES (588, '4', 'c54f2392bdc18216', 'A', '2026-10-27 09:49:31', '确认完成:冀BE55M7', '2026-10-27 09:47:49.588558');
INSERT INTO "gb_record"."gb_log" VALUES (589, '4', '13ab34979aa6c148', 'A', '2026-10-27 09:49:35', '自动数据保存成功！单号[WH2026102700023],车牌号[冀BE55M7]', '2026-10-27 09:47:53.652467');
INSERT INTO "gb_record"."gb_log" VALUES (590, '4', '48c5e988e0b22736', 'M', '2026-10-27 09:50:04', '2#识别车牌：冀BE55M7', '2026-10-27 09:48:22.696209');
INSERT INTO "gb_record"."gb_log" VALUES (591, '4', '2d7d495e5cae938a', 'A', '2026-10-27 09:50:06', '车辆已下磅', '2026-10-27 09:48:24.725357');
INSERT INTO "gb_record"."gb_log" VALUES (592, '4', 'cabc029e1a5dba1f', 'A', '2026-10-27 09:50:14', '车辆下磅后清空出场车牌:冀BE55M7', '2026-10-27 09:48:32.76268');
INSERT INTO "gb_record"."gb_log" VALUES (593, '4', 'e016c714b8c042f5', 'M', '2026-10-27 09:50:35', '1#识别车牌：陕KQ9665', '2026-10-27 09:48:54.802752');
INSERT INTO "gb_record"."gb_log" VALUES (594, '4', '995571460d799756', 'M', '2026-10-27 09:50:36', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KQ9665","consigneeCorpName":"","forwardingCorpName":"","goodsName":"","goodsSpec":"","isGb":0,"maxLoad":51.40,"minLoad":0.00,"receiptId":0,"receiptType":0,"transportCorpName":""}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:48:54.802752');
INSERT INTO "gb_record"."gb_log" VALUES (595, '4', '961984e445a8d847', 'M', '2026-10-27 09:50:36', '正在下载车牌数据[陕KQ9665]...', '2026-10-27 09:48:54.802752');
INSERT INTO "gb_record"."gb_log" VALUES (596, '4', '144063aac6add3f0', 'A', '2026-10-27 09:50:36', '1#[陕KQ9665]无效： 不允许过磅<9:50>', '2026-10-27 09:48:55.871421');
INSERT INTO "gb_record"."gb_log" VALUES (597, '4', '2fc93d57cb12eb62', 'M', '2026-10-27 09:50:56', '1#识别车牌：陕KQ9665', '2026-10-27 09:49:14.91431');
INSERT INTO "gb_record"."gb_log" VALUES (598, '4', '0957dd600ded8a9f', 'M', '2026-10-27 09:50:57', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KQ9665","consigneeCorpName":"","forwardingCorpName":"","goodsName":"","goodsSpec":"","isGb":0,"maxLoad":51.40,"minLoad":0.00,"receiptId":0,"receiptType":0,"transportCorpName":""}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:49:15.946129');
INSERT INTO "gb_record"."gb_log" VALUES (599, '4', '77cd3da01ae0d1ab', 'M', '2026-10-27 09:50:57', '正在下载车牌数据[陕KQ9665]...', '2026-10-27 09:49:15.946129');
INSERT INTO "gb_record"."gb_log" VALUES (600, '4', '14606a88988c12e3', 'M', '2026-10-27 09:51:16', '1#识别车牌：陕KQ9665', '2026-10-27 09:49:34.982691');
INSERT INTO "gb_record"."gb_log" VALUES (601, '4', '9213bde7068b341f', 'M', '2026-10-27 09:51:18', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KQ9665","consigneeCorpName":"广东联塑科技实业有限公司","forwardingCorpName":"神木化工","goodsName":"PVC","goodsSpec":"产成品","isGb":1,"maxLoad":51.40,"minLoad":0.00,"receiptId":503425303838729,"receiptType":1,"transportCorpName":"陕西外运国铁物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 09:49:37.015349');
INSERT INTO "gb_record"."gb_log" VALUES (602, '4', 'd419e028893fd82a', 'M', '2026-10-27 09:51:18', '正在下载车牌数据[陕KQ9665]...', '2026-10-27 09:49:37.015349');
INSERT INTO "gb_record"."gb_log" VALUES (603, '4', '58f9cee63ce472e7', 'A', '2026-10-27 09:51:18', '等待入口车辆上磅:陕KQ9665', '2026-10-27 09:49:37.015349');
INSERT INTO "gb_record"."gb_log" VALUES (604, '4', '7e9d059da98edb5f', 'A', '2026-10-27 09:51:47', '等待确认车牌号', '2026-10-27 09:50:05.05692');
INSERT INTO "gb_record"."gb_log" VALUES (605, '4', 'cef137c25f369cd2', 'A', '2026-10-27 09:52:17', '确认完成:陕KQ9665', '2026-10-27 09:50:36.187973');
INSERT INTO "gb_record"."gb_log" VALUES (606, '4', '396260d04811f11f', 'A', '2026-10-27 09:52:21', '自动数据保存成功！单号[WH2026102700059],车牌号[陕KQ9665]', '2026-10-27 09:50:40.218687');
INSERT INTO "gb_record"."gb_log" VALUES (607, '4', 'cceed46ee7e97572', 'A', '2026-10-27 09:52:52', '车辆已下磅', '2026-10-27 09:51:10.26096');
INSERT INTO "gb_record"."gb_log" VALUES (608, '4', 'ede5fcd2f3a5653b', 'M', '2026-10-27 09:57:26', '2#识别车牌：陕KD8395', '2026-10-27 09:55:45.338001');
INSERT INTO "gb_record"."gb_log" VALUES (609, '4', '5bb356f7f3b42a3d', 'A', '2026-10-27 09:57:26', '等待出口车辆上磅:陕KD8395', '2026-10-27 09:55:45.338001');
INSERT INTO "gb_record"."gb_log" VALUES (610, '4', 'be777cf6eedf7010', 'A', '2026-10-27 09:57:50', '等待确认车牌号', '2026-10-27 09:56:08.380401');
INSERT INTO "gb_record"."gb_log" VALUES (611, '4', 'ba866a3229809e0d', 'A', '2026-10-27 09:58:20', '确认完成:陕KD8395', '2026-10-27 09:56:39.422909');
INSERT INTO "gb_record"."gb_log" VALUES (612, '4', '381b6205fd4e4f79', 'A', '2026-10-27 09:58:24', '自动数据保存成功！单号[WH2026102700055],车牌号[陕KD8395]', '2026-10-27 09:56:43.454875');
INSERT INTO "gb_record"."gb_log" VALUES (613, '4', 'dd9d950666714d09', 'A', '2026-10-27 09:58:47', '车辆已下磅', '2026-10-27 09:57:05.505017');
INSERT INTO "gb_record"."gb_log" VALUES (614, '4', 'a86bcb5a6060c431', 'M', '2026-10-27 09:58:51', '2#识别车牌：陕KR9795', '2026-10-27 09:57:09.534402');
INSERT INTO "gb_record"."gb_log" VALUES (615, '4', '888b42f2d32e71dd', 'A', '2026-10-27 09:58:56', '等待出口车辆上磅:陕KR9795', '2026-10-27 09:57:14.565992');
INSERT INTO "gb_record"."gb_log" VALUES (616, '4', '5dd86c995db129a2', 'A', '2026-10-27 09:59:18', '等待确认车牌号', '2026-10-27 09:57:37.604551');
INSERT INTO "gb_record"."gb_log" VALUES (617, '4', 'c848cc899d891cbe', 'A', '2026-10-27 09:59:49', '确认完成:陕KR9795', '2026-10-27 09:58:07.643122');
INSERT INTO "gb_record"."gb_log" VALUES (618, '4', '20e75c436ace9613', 'A', '2026-10-27 09:59:54', '自动数据保存成功！单号[WH2026102700057],车牌号[陕KR9795]', '2026-10-27 09:58:12.681832');
INSERT INTO "gb_record"."gb_log" VALUES (619, '4', 'e4be7f8215945aad', 'A', '2026-10-27 10:00:21', '车辆已下磅', '2026-10-27 09:58:39.736566');
INSERT INTO "gb_record"."gb_log" VALUES (620, '4', 'b6cf3e795d7c50b4', 'M', '2026-10-27 10:01:22', '2#识别车牌：陕KM8567', '2026-10-27 09:59:40.783341');
INSERT INTO "gb_record"."gb_log" VALUES (621, '4', 'c6b9121717558b4c', 'A', '2026-10-27 10:01:23', '等待出口车辆上磅:陕KM8567', '2026-10-27 09:59:41.821962');
INSERT INTO "gb_record"."gb_log" VALUES (622, '4', 'ea23030ced6c1da1', 'A', '2026-10-27 10:01:49', '等待确认车牌号', '2026-10-27 10:00:07.865488');
INSERT INTO "gb_record"."gb_log" VALUES (623, '4', '1e1aa3a0af398dd3', 'A', '2026-10-27 10:02:20', '确认完成:陕KM8567', '2026-10-27 10:00:38.907474');
INSERT INTO "gb_record"."gb_log" VALUES (624, '4', '942066fe21945470', 'A', '2026-10-27 10:02:24', '自动数据保存成功！单号[WH2026102700058],车牌号[陕KM8567]', '2026-10-27 10:00:42.946004');
INSERT INTO "gb_record"."gb_log" VALUES (625, '4', '46e5209bb7498077', 'A', '2026-10-27 10:02:48', '车辆已下磅', '2026-10-27 10:01:06.994503');
INSERT INTO "gb_record"."gb_log" VALUES (626, '4', '6965935ed9213c57', 'M', '2026-10-27 10:11:04', '1#识别车牌：陕KJ7161', '2026-10-27 10:09:22.094692');
INSERT INTO "gb_record"."gb_log" VALUES (627, '4', '93e42360aa161f91', 'M', '2026-10-27 10:11:04', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KJ7161","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830240702465,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 10:09:23.128029');
INSERT INTO "gb_record"."gb_log" VALUES (628, '4', 'c63c5f4d479e2b71', 'M', '2026-10-27 10:11:04', '正在下载车牌数据[陕KJ7161]...', '2026-10-27 10:09:23.128029');
INSERT INTO "gb_record"."gb_log" VALUES (629, '4', 'e6a3fbf6c111d35c', 'A', '2026-10-27 10:11:04', '等待入口车辆上磅:陕KJ7161', '2026-10-27 10:09:23.128029');
INSERT INTO "gb_record"."gb_log" VALUES (637, '4', '55676283f092ad04', 'A', '2026-10-27 10:23:47', '确认完成:陕KJ7161', '2026-10-27 10:22:05.593597');
INSERT INTO "gb_record"."gb_log" VALUES (638, '4', '0fb665e5888854d7', 'A', '2026-10-27 10:23:51', '自动数据保存成功！单号[WH2026102700065],车牌号[陕KJ7161]', '2026-10-27 10:22:09.628199');
INSERT INTO "gb_record"."gb_log" VALUES (639, '4', 'd2c1f54761e4875e', 'A', '2026-10-27 10:24:16', '车辆已下磅', '2026-10-27 10:22:35.675008');
INSERT INTO "gb_record"."gb_log" VALUES (630, '4', '8d9a33f012cd9ffd', 'A', '2026-10-27 10:11:23', '等待确认车牌号', '2026-10-27 10:09:41.1639');
INSERT INTO "gb_record"."gb_log" VALUES (631, '4', '453304b037695bb3', 'A', '2026-10-27 10:11:53', '确认完成:陕KJ7161', '2026-10-27 10:10:12.211283');
INSERT INTO "gb_record"."gb_log" VALUES (632, '4', '5f43675949245b63', 'A', '2026-10-27 10:11:57', '自动数据保存成功！单号[WH2026102700065],车牌号[陕KJ7161]', '2026-10-27 10:10:16.253866');
INSERT INTO "gb_record"."gb_log" VALUES (633, '4', '61fc4333bd923412', 'A', '2026-10-27 10:12:13', '车辆已下磅', '2026-10-27 10:10:31.396539');
INSERT INTO "gb_record"."gb_log" VALUES (634, '4', 'a12e9497f66dfa5f', 'M', '2026-10-27 10:22:54', '2#识别车牌：陕KJ7161', '2026-10-27 10:21:13.517822');
INSERT INTO "gb_record"."gb_log" VALUES (635, '4', '906ad4b346ac481b', 'A', '2026-10-27 10:22:55', '等待出口车辆上磅:陕KJ7161', '2026-10-27 10:21:13.517822');
INSERT INTO "gb_record"."gb_log" VALUES (636, '4', '32ed61694dc32a18', 'A', '2026-10-27 10:23:16', '等待确认车牌号', '2026-10-27 10:21:35.55456');
INSERT INTO "gb_record"."gb_log" VALUES (640, '4', 'c87baeb8558cb7b1', 'M', '2026-10-27 10:38:45', '2#识别车牌：陕KQ9665', '2026-10-27 10:37:03.846691');
INSERT INTO "gb_record"."gb_log" VALUES (641, '4', '3599cdae070f3583', 'A', '2026-10-27 10:38:46', '等待出口车辆上磅:陕KQ9665', '2026-10-27 10:37:04.880353');
INSERT INTO "gb_record"."gb_log" VALUES (642, '4', '0a059f35c41585a2', 'A', '2026-10-27 10:39:07', '等待确认车牌号', '2026-10-27 10:37:25.922172');
INSERT INTO "gb_record"."gb_log" VALUES (643, '4', '28ef8c632c235cc8', 'A', '2026-10-27 10:39:38', '确认完成:陕KQ9665', '2026-10-27 10:37:55.963801');
INSERT INTO "gb_record"."gb_log" VALUES (644, '4', '59caef60144280b4', 'A', '2026-10-27 10:39:42', '自动数据保存成功！单号[WH2026102700059],车牌号[陕KQ9665]', '2026-10-27 10:38:00.992873');
INSERT INTO "gb_record"."gb_log" VALUES (645, '4', '0d4bbc320d5c38a7', 'A', '2026-10-27 10:40:00', '车辆已下磅', '2026-10-27 10:38:19.053577');
INSERT INTO "gb_record"."gb_log" VALUES (646, '4', '48078be0af63a218', 'M', '2026-10-27 10:40:24', '1#识别车牌：陕KR9795', '2026-10-27 10:38:43.092694');
INSERT INTO "gb_record"."gb_log" VALUES (647, '4', 'e4e7eee6cdf10168', 'M', '2026-10-27 10:40:25', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KR9795","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516476197404673,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 10:38:44.12266');
INSERT INTO "gb_record"."gb_log" VALUES (648, '4', '453f955e1e75fd0c', 'M', '2026-10-27 10:40:25', '正在下载车牌数据[陕KR9795]...', '2026-10-27 10:38:44.12266');
INSERT INTO "gb_record"."gb_log" VALUES (649, '4', 'afd577e61ce4ebd4', 'A', '2026-10-27 10:40:25', '等待入口车辆上磅:陕KR9795', '2026-10-27 10:38:44.12266');
INSERT INTO "gb_record"."gb_log" VALUES (650, '4', '657d617cb94db255', 'A', '2026-10-27 10:40:43', '等待确认车牌号', '2026-10-27 10:39:02.346743');
INSERT INTO "gb_record"."gb_log" VALUES (651, '4', '8a37457880d7d114', 'A', '2026-10-27 10:41:14', '确认完成:陕KR9795', '2026-10-27 10:39:32.387266');
INSERT INTO "gb_record"."gb_log" VALUES (652, '4', '83ad6fe784fb5adb', 'A', '2026-10-27 10:41:18', '自动数据保存成功！单号[WH2026102700069],车牌号[陕KR9795]', '2026-10-27 10:39:36.418327');
INSERT INTO "gb_record"."gb_log" VALUES (653, '4', '0f74549cb110bb5e', 'A', '2026-10-27 10:41:32', '车辆已下磅', '2026-10-27 10:39:51.461021');
INSERT INTO "gb_record"."gb_log" VALUES (654, '4', '3697897aca08f54a', 'M', '2026-10-27 10:41:37', '1#识别车牌：陕KD8395', '2026-10-27 10:39:56.495477');
INSERT INTO "gb_record"."gb_log" VALUES (655, '4', '6e1c5c6d30349c69', 'M', '2026-10-27 10:41:42', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KD8395","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516476180627471,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 10:40:00.529136');
INSERT INTO "gb_record"."gb_log" VALUES (656, '4', 'c52560076a9fb1bc', 'M', '2026-10-27 10:41:42', '正在下载车牌数据[陕KD8395]...', '2026-10-27 10:40:00.529136');
INSERT INTO "gb_record"."gb_log" VALUES (657, '4', 'd32495e852972b40', 'A', '2026-10-27 10:41:42', '等待入口车辆上磅:陕KD8395', '2026-10-27 10:40:00.529136');
INSERT INTO "gb_record"."gb_log" VALUES (658, '4', '304c760d865d22b0', 'A', '2026-10-27 10:42:03', '等待确认车牌号', '2026-10-27 10:40:21.568476');
INSERT INTO "gb_record"."gb_log" VALUES (659, '4', '9b589268894aa700', 'A', '2026-10-27 10:42:33', '确认完成:陕KD8395', '2026-10-27 10:40:52.613695');
INSERT INTO "gb_record"."gb_log" VALUES (660, '4', 'cda9472f1da3071b', 'A', '2026-10-27 10:42:38', '自动数据保存成功！单号[WH2026102700070],车牌号[陕KD8395]', '2026-10-27 10:40:56.651307');
INSERT INTO "gb_record"."gb_log" VALUES (661, '4', 'ff61feae56d4c5d9', 'A', '2026-10-27 10:42:55', '车辆已下磅', '2026-10-27 10:41:13.709589');
INSERT INTO "gb_record"."gb_log" VALUES (662, '4', '8f58573658477f18', 'M', '2026-10-27 10:44:49', '1#识别车牌：陕KM8567', '2026-10-27 10:43:07.759617');
INSERT INTO "gb_record"."gb_log" VALUES (663, '4', '740e374124b89f00', 'M', '2026-10-27 10:44:50', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KM8567","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830238605315,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 10:43:08.789625');
INSERT INTO "gb_record"."gb_log" VALUES (664, '4', '5e7b9297cc582423', 'M', '2026-10-27 10:44:50', '正在下载车牌数据[陕KM8567]...', '2026-10-27 10:43:08.789625');
INSERT INTO "gb_record"."gb_log" VALUES (665, '4', '0fe47d9da2cdafaa', 'A', '2026-10-27 10:44:50', '等待入口车辆上磅:陕KM8567', '2026-10-27 10:43:08.789625');
INSERT INTO "gb_record"."gb_log" VALUES (666, '4', '3b2a4ff137b873ec', 'A', '2026-10-27 10:45:07', '等待确认车牌号', '2026-10-27 10:43:25.848468');
INSERT INTO "gb_record"."gb_log" VALUES (667, '4', '87f49d96abd3c422', 'A', '2026-10-27 10:45:39', '确认完成:陕KM8567', '2026-10-27 10:43:57.885655');
INSERT INTO "gb_record"."gb_log" VALUES (668, '4', '06e2604d746e1f91', 'A', '2026-10-27 10:45:43', '自动数据保存成功！单号[WH2026102700072],车牌号[陕KM8567]', '2026-10-27 10:44:02.168292');
INSERT INTO "gb_record"."gb_log" VALUES (669, '4', '1ff78433e27646e9', 'A', '2026-10-27 10:46:10', '车辆已下磅', '2026-10-27 10:44:28.207284');
INSERT INTO "gb_record"."gb_log" VALUES (670, '4', 'fa7811f4ce6b4d28', 'M', '2026-10-27 10:53:49', '2#识别车牌：陕KR9795', '2026-10-27 10:52:07.305874');
INSERT INTO "gb_record"."gb_log" VALUES (672, '4', '3684e28a3b53cc3f', 'A', '2026-10-27 10:53:49', '等待出口车辆上磅:陕KR9795', '2026-10-27 10:52:27.518835');
INSERT INTO "gb_record"."gb_log" VALUES (673, '4', 'f7ac4b0c26adb5c4', 'A', '2026-10-27 10:54:15', '等待确认车牌号', '2026-10-27 10:52:33.968309');
INSERT INTO "gb_record"."gb_log" VALUES (674, '4', '8b5783564c2485db', 'A', '2026-10-27 10:54:46', '确认完成:陕KR9795', '2026-10-27 10:53:05.012582');
INSERT INTO "gb_record"."gb_log" VALUES (675, '4', '68a9cdb40fe9ecc6', 'A', '2026-10-27 10:54:50', '自动数据保存成功！单号[WH2026102700069],车牌号[陕KR9795]', '2026-10-27 10:53:09.046009');
INSERT INTO "gb_record"."gb_log" VALUES (676, '4', '0d024501847016f4', 'A', '2026-10-27 10:55:12', '车辆已下磅', '2026-10-27 10:53:31.095885');
INSERT INTO "gb_record"."gb_log" VALUES (677, '4', '0222053cff816971', 'M', '2026-10-27 11:00:17', '1#识别车牌：陕KD8395', '2026-10-27 10:58:36.173339');
INSERT INTO "gb_record"."gb_log" VALUES (678, '4', '3031654a5dadda0e', 'A', '2026-10-27 11:00:18', '等待入口车辆上磅:陕KD8395', '2026-10-27 10:58:36.173339');
INSERT INTO "gb_record"."gb_log" VALUES (679, '4', '492e08e084d98d42', 'A', '2026-10-27 11:00:38', '等待确认车牌号', '2026-10-27 10:58:57.211343');
INSERT INTO "gb_record"."gb_log" VALUES (680, '4', '8c2ab1f2727bb095', 'A', '2026-10-27 11:01:08', '确认完成:陕KD8395', '2026-10-27 10:59:27.250953');
INSERT INTO "gb_record"."gb_log" VALUES (681, '4', '1c91d31bc88a0e7e', 'A', '2026-10-27 11:01:12', '自动数据保存成功！单号[WH2026102700070],车牌号[陕KD8395]', '2026-10-27 10:59:31.286334');
INSERT INTO "gb_record"."gb_log" VALUES (682, '4', '95b29a1fa59598e6', 'A', '2026-10-27 11:01:29', '车辆已下磅', '2026-10-27 10:59:47.328822');
INSERT INTO "gb_record"."gb_log" VALUES (683, '4', 'd74888a2f290b902', 'M', '2026-10-27 11:02:34', '1#识别车牌：陕KJ7161', '2026-10-27 11:00:52.376733');
INSERT INTO "gb_record"."gb_log" VALUES (684, '4', '9546bb22909ea90c', 'M', '2026-10-27 11:02:35', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KJ7161","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516830236508163,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 11:00:54.418768');
INSERT INTO "gb_record"."gb_log" VALUES (685, '4', '729dc2dcf31a968f', 'M', '2026-10-27 11:02:35', '正在下载车牌数据[陕KJ7161]...', '2026-10-27 11:00:54.418768');
INSERT INTO "gb_record"."gb_log" VALUES (686, '4', '5a8c9d557d1dc815', 'A', '2026-10-27 11:02:35', '等待入口车辆上磅:陕KJ7161', '2026-10-27 11:00:54.418768');
INSERT INTO "gb_record"."gb_log" VALUES (687, '4', 'e9392678d59b4527', 'A', '2026-10-27 11:02:54', '等待确认车牌号', '2026-10-27 11:01:13.460771');
INSERT INTO "gb_record"."gb_log" VALUES (688, '4', '87546f7d0ed578b7', 'A', '2026-10-27 11:03:25', '确认完成:陕KJ7161', '2026-10-27 11:01:43.501735');
INSERT INTO "gb_record"."gb_log" VALUES (689, '4', 'f752dd51e56ea4b9', 'A', '2026-10-27 11:03:29', '自动数据保存成功！单号[WH2026102700077],车牌号[陕KJ7161]', '2026-10-27 11:01:47.534092');
INSERT INTO "gb_record"."gb_log" VALUES (690, '4', '30abe0be2ee459fe', 'A', '2026-10-27 11:03:46', '车辆已下磅', '2026-10-27 11:02:04.572713');
INSERT INTO "gb_record"."gb_log" VALUES (691, '4', 'c51eb66710fc4911', 'M', '2026-10-27 11:06:28', '1#识别车牌：陕KM8567', '2026-10-27 11:04:46.628724');
INSERT INTO "gb_record"."gb_log" VALUES (692, '4', 'c83f8061f712dc6e', 'A', '2026-10-27 11:06:29', '等待入口车辆上磅:陕KM8567', '2026-10-27 11:04:47.658752');
INSERT INTO "gb_record"."gb_log" VALUES (694, '4', '7e4ede1ad6295ea4', 'A', '2026-10-27 11:07:20', '确认完成:陕KM8567', '2026-10-27 11:05:38.735553');
INSERT INTO "gb_record"."gb_log" VALUES (695, '4', 'e51df7c110d3e0e6', 'A', '2026-10-27 11:07:24', '自动数据保存成功！单号[WH2026102700072],车牌号[陕KM8567]', '2026-10-27 11:05:42.765239');
INSERT INTO "gb_record"."gb_log" VALUES (696, '4', '70e3a80e3b295391', 'A', '2026-10-27 11:07:47', '车辆已下磅', '2026-10-27 11:06:05.803239');
INSERT INTO "gb_record"."gb_log" VALUES (697, '4', 'f15a2c2507ed5161', 'M', '2026-10-27 11:09:49', '1#识别车牌：陕KR3179', '2026-10-27 11:08:07.85536');
INSERT INTO "gb_record"."gb_log" VALUES (698, '4', 'ed60cf96559cd15d', 'M', '2026-10-27 11:09:50', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KR3179","consigneeCorpName":"广东联塑科技实业有限公司","forwardingCorpName":"神木化工","goodsName":"PVC","goodsSpec":"产成品","isGb":1,"maxLoad":51.40,"minLoad":0.00,"receiptId":512659196215297,"receiptType":1,"transportCorpName":"陕西外运国铁物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 11:08:08.883401');
INSERT INTO "gb_record"."gb_log" VALUES (699, '4', 'de829f7d8342cce1', 'M', '2026-10-27 11:09:50', '正在下载车牌数据[陕KR3179]...', '2026-10-27 11:08:08.883401');
INSERT INTO "gb_record"."gb_log" VALUES (700, '4', '9427298df35c2f6d', 'A', '2026-10-27 11:09:50', '等待入口车辆上磅:陕KR3179', '2026-10-27 11:08:08.883401');
INSERT INTO "gb_record"."gb_log" VALUES (714, '4', '7c8b98e41cf17b0e', 'M', '2026-10-27 11:15:15', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KR0357","consigneeCorpName":"广东联塑科技实业有限公司","forwardingCorpName":"神木化工","goodsName":"PVC","goodsSpec":"产成品","isGb":1,"maxLoad":51.40,"minLoad":0.00,"receiptId":511595036278787,"receiptType":1,"transportCorpName":"陕西外运国铁物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 11:13:34.511403');
INSERT INTO "gb_record"."gb_log" VALUES (715, '4', 'a4f1e16a190ff5af', 'M', '2026-10-27 11:15:15', '正在下载车牌数据[陕KR0357]...', '2026-10-27 11:13:34.511403');
INSERT INTO "gb_record"."gb_log" VALUES (716, '4', '8838765423a6d343', 'A', '2026-10-27 11:15:15', '等待入口车辆上磅:陕KR0357', '2026-10-27 11:13:34.511403');
INSERT INTO "gb_record"."gb_log" VALUES (693, '4', '9095def8b63daa01', 'A', '2026-10-27 11:06:49', '等待确认车牌号', '2026-10-27 11:05:07.695099');
INSERT INTO "gb_record"."gb_log" VALUES (701, '4', '7ffd886a684e91af', 'A', '2026-10-27 11:10:13', '等待确认车牌号', '2026-10-27 11:08:31.922998');
INSERT INTO "gb_record"."gb_log" VALUES (702, '4', 'e7329d93b62e1514', 'A', '2026-10-27 11:10:43', '确认完成:陕KR3179', '2026-10-27 11:09:01.977901');
INSERT INTO "gb_record"."gb_log" VALUES (703, '4', '4b69fcad2746d3ae', 'A', '2026-10-27 11:10:48', '自动数据保存成功！单号[WH2026102700080],车牌号[陕KR3179]', '2026-10-27 11:09:06.035641');
INSERT INTO "gb_record"."gb_log" VALUES (704, '4', 'a999499546f82ce4', 'A', '2026-10-27 11:11:29', '车辆已下磅', '2026-10-27 11:09:48.080675');
INSERT INTO "gb_record"."gb_log" VALUES (705, '4', '4bf4b8d8320504ff', 'M', '2026-10-27 11:13:46', '1#识别车牌：陕KS3118', '2026-10-27 11:12:05.135833');
INSERT INTO "gb_record"."gb_log" VALUES (706, '4', '5519f311ee3b65e2', 'M', '2026-10-27 11:13:47', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KS3118","consigneeCorpName":"广东联塑科技实业有限公司","forwardingCorpName":"神木化工","goodsName":"PVC","goodsSpec":"产成品","isGb":1,"maxLoad":51.40,"minLoad":0.00,"receiptId":504900102914049,"receiptType":1,"transportCorpName":"厦门国贸泰达物流有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 11:12:05.135833');
INSERT INTO "gb_record"."gb_log" VALUES (707, '4', '89f2eb5843ed905a', 'M', '2026-10-27 11:13:47', '正在下载车牌数据[陕KS3118]...', '2026-10-27 11:12:05.135833');
INSERT INTO "gb_record"."gb_log" VALUES (708, '4', 'a8410dd1b03461ad', 'A', '2026-10-27 11:13:47', '等待入口车辆上磅:陕KS3118', '2026-10-27 11:12:05.135833');
INSERT INTO "gb_record"."gb_log" VALUES (709, '4', '17e6cf9b8d3dff2a', 'A', '2026-10-27 11:14:11', '等待确认车牌号', '2026-10-27 11:12:30.178959');
INSERT INTO "gb_record"."gb_log" VALUES (710, '4', 'c4291bf4de37ea40', 'A', '2026-10-27 11:14:41', '确认完成:陕KS3118', '2026-10-27 11:13:00.217852');
INSERT INTO "gb_record"."gb_log" VALUES (711, '4', 'bde8af75db0f8221', 'A', '2026-10-27 11:14:45', '自动数据保存成功！单号[WH2026102700082],车牌号[陕KS3118]', '2026-10-27 11:13:04.256791');
INSERT INTO "gb_record"."gb_log" VALUES (712, '4', 'c79a29fac9126b31', 'A', '2026-10-27 11:15:01', '车辆已下磅', '2026-10-27 11:13:19.423903');
INSERT INTO "gb_record"."gb_log" VALUES (713, '4', '7ff227b79673c66d', 'M', '2026-10-27 11:15:15', '1#识别车牌：陕KR0357', '2026-10-27 11:13:33.47319');
INSERT INTO "gb_record"."gb_log" VALUES (717, '4', '2dea8891cb017ec3', 'A', '2026-10-27 11:15:36', '等待确认车牌号', '2026-10-27 11:13:54.549185');
INSERT INTO "gb_record"."gb_log" VALUES (718, '4', 'fbccecf638a6e7a6', 'A', '2026-10-27 11:16:05', '确认完成:陕KR0357', '2026-10-27 11:14:23.589638');
INSERT INTO "gb_record"."gb_log" VALUES (719, '4', '154faa06458eb902', 'A', '2026-10-27 11:16:09', '自动数据保存成功！单号[WH2026102700083],车牌号[陕KR0357]', '2026-10-27 11:14:28.617987');
INSERT INTO "gb_record"."gb_log" VALUES (720, '4', '95e7513aa87f11b2', 'A', '2026-10-27 11:16:49', '车辆已下磅', '2026-10-27 11:15:07.663388');
INSERT INTO "gb_record"."gb_log" VALUES (721, '4', '6445b92287c41587', 'M', '2026-10-27 11:17:02', '2#识别车牌：陕KJ7161', '2026-10-27 11:15:20.703786');
INSERT INTO "gb_record"."gb_log" VALUES (722, '4', 'a63c65bc5bc17896', 'A', '2026-10-27 11:17:02', '等待出口车辆上磅:陕KJ7161', '2026-10-27 11:15:21.739147');
INSERT INTO "gb_record"."gb_log" VALUES (723, '4', '0247fdb3b34764f0', 'A', '2026-10-27 11:17:29', '等待确认车牌号', '2026-10-27 11:15:47.775949');
INSERT INTO "gb_record"."gb_log" VALUES (724, '4', '41ec476e2e006bf1', 'A', '2026-10-27 11:17:58', '确认完成:陕KJ7161', '2026-10-27 11:16:16.858916');
INSERT INTO "gb_record"."gb_log" VALUES (725, '4', '011fca814b99bc73', 'A', '2026-10-27 11:18:02', '自动数据保存成功！单号[WH2026102700077],车牌号[陕KJ7161]', '2026-10-27 11:16:20.895127');
INSERT INTO "gb_record"."gb_log" VALUES (726, '4', '348a01abc4f6e371', 'A', '2026-10-27 11:18:35', '车辆已下磅', '2026-10-27 11:16:54.070716');
INSERT INTO "gb_record"."gb_log" VALUES (727, '4', 'c7ff52b64809b726', 'M', '2026-10-27 11:28:29', '1#识别车牌：陕KR9795', '2026-10-27 11:26:48.286014');
INSERT INTO "gb_record"."gb_log" VALUES (728, '4', '8381cc33e9a5e16a', 'M', '2026-10-27 11:28:30', '过磅车辆判断信息[{"args":[],"body":[],"code":"","data":{"result":{"carNumber":"陕KR9795","consigneeCorpName":"神木市电石集团能源发展有限责任公司","forwardingCorpName":"神木化工","goodsName":"电石渣","goodsSpec":"产成品","isGb":1,"maxLoad":100.00,"minLoad":0.00,"receiptId":516476197404681,"receiptType":1,"transportCorpName":"榆林和顺达商贸有限公司"}},"msg":"","msgLevel":0,"returnCode":1,"returnMsg":"成功","status":true}]...', '2026-10-27 11:26:49.323905');
INSERT INTO "gb_record"."gb_log" VALUES (729, '4', 'fb0b40d4ae73f775', 'M', '2026-10-27 11:28:30', '正在下载车牌数据[陕KR9795]...', '2026-10-27 11:26:49.323905');
INSERT INTO "gb_record"."gb_log" VALUES (730, '4', 'ac55a92d53759ea6', 'A', '2026-10-27 11:28:31', '等待入口车辆上磅:陕KR9795', '2026-10-27 11:26:49.323905');
INSERT INTO "gb_record"."gb_log" VALUES (731, '4', '696fdf23a8ec8eb9', 'A', '2026-10-27 11:28:49', '等待确认车牌号', '2026-10-27 11:27:07.361553');
INSERT INTO "gb_record"."gb_log" VALUES (732, '4', 'd3b417fa9aca1f12', 'A', '2026-10-27 11:29:19', '确认完成:陕KR9795', '2026-10-27 11:27:38.401572');
INSERT INTO "gb_record"."gb_log" VALUES (733, '4', 'a74294a55485e45c', 'A', '2026-10-27 11:29:23', '自动数据保存成功！单号[WH2026102700088],车牌号[陕KR9795]', '2026-10-27 11:27:42.434474');
INSERT INTO "gb_record"."gb_log" VALUES (734, '4', '5b83e289e5b1d334', 'A', '2026-10-27 11:29:39', '车辆已下磅', '2026-10-27 11:27:57.473167');

-- ----------------------------
-- Table structure for gb_weigh_event
-- ----------------------------
DROP TABLE IF EXISTS "gb_record"."gb_weigh_event";
CREATE TABLE "gb_record"."gb_weigh_event" (
  "id" int8 NOT NULL DEFAULT nextval('"gb_record".gb_weigh_event_id_seq'::regclass),
  "source" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "fp" varchar(32) COLLATE "pg_catalog"."default",
  "order_no" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "vehicle_no" varchar(32) COLLATE "pg_catalog"."default",
  "event_time" timestamp(6) NOT NULL,
  "create_time" timestamp(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Records of gb_weigh_event
-- ----------------------------
INSERT INTO "gb_record"."gb_weigh_event" VALUES (7, '4', '505115de5ab8f6cb', 'WH2026102600161', '陕KR9795', '2026-10-26 18:04:45', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (8, '4', '6d027aaf0d549265', 'WH2026102600085', '晋JV8549', '2026-10-26 18:06:23', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (9, '4', '99752b3e0496c302', 'WH2026102600165', '晋JJ4186', '2026-10-26 18:32:26', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (10, '4', '9efd3b760f458d1a', 'WH2026102600166', '甘D80315', '2026-10-26 18:34:16', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (11, '4', '49081c9267adb33c', 'WH2026102600168', '甘D85015', '2026-10-26 18:36:00', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (12, '4', 'c81a8c2fbd863e1e', 'WH2026102600169', '陕KM8567', '2026-10-26 18:39:58', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (13, '4', 'f5b9191f0b990925', 'WH2026102600171', '陕KJ1738', '2026-10-26 18:45:37', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (14, '4', 'c4e665facedb3b28', 'WH2026102600173', '冀A43288', '2026-10-26 18:52:18', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (15, '4', '3526dca11e784732', 'WH2026102600174', '晋JN5755', '2026-10-26 18:56:28', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (16, '4', '2f312b0cef68dbfc', 'WH2026102600175', '陕KB5935', '2026-10-26 18:58:05', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (17, '4', 'b94d42f80f2884d0', 'WH2026102600169', '陕KM8567', '2026-10-26 19:00:29', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (18, '4', '2910e0567e186b65', 'WH2026102600178', '陕KD8395', '2026-10-26 19:07:07', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (19, '4', 'b1414af53f43d9a7', 'WH2026102600179', '蒙C27130', '2026-10-26 19:08:54', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (20, '4', '0207d07ff867e35c', 'WH2026102600180', '蒙B86541', '2026-10-26 19:10:44', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (21, '4', '403b22ac31b0b08f', 'WH2026102600181', '蒙BA3948', '2026-10-26 19:12:26', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (22, '4', 'ddb0880413c09e9d', 'WH2026102600178', '陕KD8395', '2026-10-26 19:22:40', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (23, '4', 'a76ff86f72fc9fa8', 'WH2026102600183', '陕KS9319', '2026-10-26 19:26:03', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (24, '4', 'b61a5b9a7810f1ff', 'WH2026102600184', '陕KG0391', '2026-10-26 19:35:29', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (25, '4', '143bcb8b30a8cda1', 'WH2026102600185', '冀AS3762', '2026-10-26 19:37:12', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (26, '4', '7c79c4c5947d1995', 'WH2026102600175', '陕KB5935', '2026-10-26 19:42:26', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (27, '4', '99f45ef678c3e3c8', 'WH2026102600171', '陕KJ1738', '2026-10-26 19:45:03', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (28, '4', '107e9658a3df0898', 'WH2026102600174', '晋JN5755', '2026-10-26 19:46:51', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (29, '4', '13cbf3743711f198', 'WH2026102600190', '陕KL6759', '2026-10-26 19:57:19', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (30, '4', 'fdc5c848252e32bb', 'WH2026102600192', '蒙BA6409', '2026-10-26 20:16:33', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (31, '4', '51d66809f8dfe375', 'WH2026102600193', '陕KJ9774', '2026-10-26 20:18:35', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (32, '4', '2779fb675affbecb', 'WH2026102600184', '陕KG0391', '2026-10-26 20:24:12', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (33, '4', 'e842f2823ede9960', 'WH2026102600193', '陕KJ9774', '2026-10-26 20:48:14', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (34, '4', '11b23216a28ed729', 'WH2026102600195', '陕KK7974', '2026-10-26 20:50:58', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (35, '4', '89c74b73a144968d', 'WH2026102600196', '陕KK7948', '2026-10-26 20:52:47', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (36, '4', 'e5644e17edd5e9bd', 'WH2026102600197', '蒙BB5512', '2026-10-26 20:54:18', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (37, '4', '5ff552dce30ec8aa', 'WH2026102600200', '蒙B86011', '2026-10-26 20:59:07', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (38, '4', 'c82ea32578d9642c', 'WH2026102600201', '蒙BB2640', '2026-10-26 21:01:29', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (39, '4', 'fa410e3b8bd763d7', 'WH2026102600203', '陕KG0159', '2026-10-26 21:03:16', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (40, '4', 'fa88b0c4f4d25857', 'WH2026102600204', '甘D86105', '2026-10-26 21:05:29', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (41, '4', 'f07f5b160afa586f', 'WH2026102600205', '蒙C29023', '2026-10-26 21:09:19', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (42, '4', '986e3142ddc005d8', 'WH2026102600206', '蒙B74723', '2026-10-26 21:11:06', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (43, '4', 'cfff158c56d0ce2f', 'WH2026102600207', '蒙BA2574', '2026-10-26 21:12:44', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (44, '4', '759e77146269314f', 'WH2026102600208', '蒙BA1405', '2026-10-26 21:19:01', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (45, '4', 'e3b71922bd7971be', 'WH2026102600209', '冀A74659', '2026-10-26 21:21:37', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (46, '4', 'e706f9eb32a8b2ac', 'WH2026102600196', '陕KK7948', '2026-10-26 21:25:46', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (47, '4', '31a591edaa4ace7d', 'WH2026102600211', '蒙B97852', '2026-10-26 21:27:50', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (48, '4', '122d33141fc19645', 'WH2026102600195', '陕KK7974', '2026-10-26 21:29:31', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (49, '4', 'fb48f10bdfc35771', 'WH2026102600203', '陕KG0159', '2026-10-26 21:50:34', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (50, '4', 'b4e324b1ed93610f', 'WH2026102600217', '陕KG4742', '2026-10-26 21:58:17', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (51, '4', 'fd00739adcb496f8', 'WH2026102600220', '蒙B98146', '2026-10-26 22:13:37', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (52, '4', '7a2a06db2b433a6b', 'WH2026102600221', '蒙BA7473', '2026-10-26 22:15:10', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (53, '4', 'e5fb19dda88bfa89', 'WH2026102600222', '蒙BA1244', '2026-10-26 22:16:58', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (54, '4', '724e9a562a5fc552', 'WH2026102600223', '陕J73277', '2026-10-26 22:23:43', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (55, '4', 'd47429adc725e13d', 'WH2026102600225', '蒙C12192', '2026-10-26 22:31:24', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (56, '4', 'b61314058f92734b', 'WH2026102600228', '蒙BA2183', '2026-10-26 22:40:24', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (57, '4', '81aee9fc92053d7a', 'WH2026102600229', '蒙BB2533', '2026-10-26 22:45:49', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (58, '4', 'a4b6810e7efeebfa', 'WH2026102600067', '冀A650BP', '2026-10-26 23:03:41', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (59, '4', '26a37f5395b90006', 'WH2026102600231', '陕J92171', '2026-10-26 23:08:06', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (60, '4', '4bd5d271ce8e975a', 'WH2026102600232', '陕KH9405', '2026-10-26 23:17:54', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (61, '4', 'd4d7d5cc6ab3d268', 'WH2026102600223', '陕J73277', '2026-10-26 23:29:17', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (62, '4', 'f40fddc0ade741cc', 'WH2026102600235', '蒙B98781', '2026-10-26 23:31:07', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (63, '4', '56a97212205dac98', 'WH2026102600237', '蒙BB1973', '2026-10-26 23:49:36', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (64, '4', '18b68a0b46613c50', 'WH2026102700042', '蒙J92102', '2026-10-27 08:34:46', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (65, '4', '40c5bd303c935066', 'WH2026102600070', '陕KD2691', '2026-10-27 08:37:14', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (66, '4', '3339c4ff3f186992', 'WH2026102700017', '陕KK4084', '2026-10-27 08:38:52', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (67, '4', '7b6f3c3e89b47341', 'WH2026102700021', '陕KJ7161', '2026-10-27 08:40:33', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (68, '4', 'f4989ced4ce489e9', 'WH2026102700016', '陕KR3801', '2026-10-27 08:42:36', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (69, '4', '88bbffac5e01b8aa', 'WH2026102700019', '晋JL7085', '2026-10-27 08:44:47', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (70, '4', '911186991d8bd7e7', 'WH2026102600177', '陕KG2755', '2026-10-27 08:46:21', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (71, '4', 'da3f65e7edf6897e', 'WH2026102700018', '陕KK0486', '2026-10-27 08:48:54', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (72, '4', '1cf6cd2680cca66d', 'WH2026102700022', '陕KD8395', '2026-10-27 08:50:38', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (73, '4', '1aeb699ac023c231', 'WH2026102700027', '陕KR9795', '2026-10-27 08:52:12', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (74, '4', 'aa9607eb24ca0442', 'WH2026102600204', '甘D86105', '2026-10-27 08:55:28', '2026-10-27 08:54:47.546517');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (75, '4', 'f9fe4dc4c2305b18', 'WH2026102700030', '陕KM8567', '2026-10-27 08:57:33', '2026-10-27 08:55:51.778778');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (76, '4', '9234e93c67ba5470', 'WH2026102600076', '陕KN6270', '2026-10-27 08:59:33', '2026-10-27 08:57:51.99956');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (77, '4', 'b0c3fa299f346887', 'WH2026102700036', '陕J78491', '2026-10-27 09:05:57', '2026-10-27 09:04:15.264156');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (78, '4', 'e739167856f07f1a', 'WH2026102700052', '陕KJ7161', '2026-10-27 09:20:44', '2026-10-27 09:19:03.514356');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (79, '4', '6ec7e703b23dad2c', 'WH2026102700052', '陕KJ7161', '2026-10-27 09:27:40', '2026-10-27 09:25:59.021939');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (80, '4', '7c21db1a28857a9e', 'WH2026102700055', '陕KD8395', '2026-10-27 09:37:14', '2026-10-27 09:35:33.494877');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (81, '4', 'faeb0fbccb11491e', 'WH2026102700057', '陕KR9795', '2026-10-27 09:45:56', '2026-10-27 09:44:14.807029');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (82, '4', '7a3f7f3b3f8977d5', 'WH2026102700058', '陕KM8567', '2026-10-27 09:47:23', '2026-10-27 09:45:42.04856');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (83, '4', '13ab34979aa6c148', 'WH2026102700023', '冀BE55M7', '2026-10-27 09:49:35', '2026-10-27 09:47:53.656955');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (84, '4', '396260d04811f11f', 'WH2026102700059', '陕KQ9665', '2026-10-27 09:52:21', '2026-10-27 09:50:40.22253');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (85, '4', '381b6205fd4e4f79', 'WH2026102700055', '陕KD8395', '2026-10-27 09:58:24', '2026-10-27 09:56:43.461465');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (86, '4', '20e75c436ace9613', 'WH2026102700057', '陕KR9795', '2026-10-27 09:59:54', '2026-10-27 09:58:12.690484');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (87, '4', '942066fe21945470', 'WH2026102700058', '陕KM8567', '2026-10-27 10:02:24', '2026-10-27 10:00:42.950318');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (88, '4', '5f43675949245b63', 'WH2026102700065', '陕KJ7161', '2026-10-27 10:11:57', '2026-10-27 10:10:16.294687');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (90, '4', '59caef60144280b4', 'WH2026102700059', '陕KQ9665', '2026-10-27 10:39:42', '2026-10-27 10:38:01.003957');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (91, '4', '83ad6fe784fb5adb', 'WH2026102700069', '陕KR9795', '2026-10-27 10:41:18', '2026-10-27 10:39:36.422938');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (92, '4', 'cda9472f1da3071b', 'WH2026102700070', '陕KD8395', '2026-10-27 10:42:38', '2026-10-27 10:40:56.654851');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (93, '4', '06e2604d746e1f91', 'WH2026102700072', '陕KM8567', '2026-10-27 10:45:43', '2026-10-27 10:44:02.171825');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (97, '4', 'e51df7c110d3e0e6', 'WH2026102700072', '陕KM8567', '2026-10-27 11:07:24', '2026-10-27 11:05:42.768836');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (89, '4', '0fb665e5888854d7', 'WH2026102700065', '陕KJ7161', '2026-10-27 10:23:51', '2026-10-27 10:22:09.632173');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (94, '4', '68a9cdb40fe9ecc6', 'WH2026102700069', '陕KR9795', '2026-10-27 10:54:50', '2026-10-27 10:53:09.050005');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (95, '4', '1c91d31bc88a0e7e', 'WH2026102700070', '陕KD8395', '2026-10-27 11:01:12', '2026-10-27 10:59:31.292478');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (96, '4', 'f752dd51e56ea4b9', 'WH2026102700077', '陕KJ7161', '2026-10-27 11:03:29', '2026-10-27 11:01:47.537911');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (98, '4', '4b69fcad2746d3ae', 'WH2026102700080', '陕KR3179', '2026-10-27 11:10:48', '2026-10-27 11:09:06.043993');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (99, '4', 'bde8af75db0f8221', 'WH2026102700082', '陕KS3118', '2026-10-27 11:14:45', '2026-10-27 11:13:04.309322');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (100, '4', '154faa06458eb902', 'WH2026102700083', '陕KR0357', '2026-10-27 11:16:09', '2026-10-27 11:14:28.621763');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (101, '4', '011fca814b99bc73', 'WH2026102700077', '陕KJ7161', '2026-10-27 11:18:02', '2026-10-27 11:16:20.960594');
INSERT INTO "gb_record"."gb_weigh_event" VALUES (102, '4', 'a74294a55485e45c', 'WH2026102700088', '陕KR9795', '2026-10-27 11:29:23', '2026-10-27 11:27:42.437485');

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "gb_record"."gb_log_id_seq"
OWNED BY "gb_record"."gb_log"."id";
SELECT setval('"gb_record"."gb_log_id_seq"', 734, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "gb_record"."gb_weigh_event_id_seq"
OWNED BY "gb_record"."gb_weigh_event"."id";
SELECT setval('"gb_record"."gb_weigh_event_id_seq"', 102, true);

-- ----------------------------
-- Indexes structure for table gb_log
-- ----------------------------
CREATE INDEX "idx_gb_log_source_time" ON "gb_record"."gb_log" USING btree (
  "source" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "log_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_gb_log_time" ON "gb_record"."gb_log" USING btree (
  "log_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table gb_log
-- ----------------------------
ALTER TABLE "gb_record"."gb_log" ADD CONSTRAINT "uq_gb_log" UNIQUE ("source", "fp");

-- ----------------------------
-- Primary Key structure for table gb_log
-- ----------------------------
ALTER TABLE "gb_record"."gb_log" ADD CONSTRAINT "gb_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table gb_weigh_event
-- ----------------------------
CREATE INDEX "idx_gb_event_order" ON "gb_record"."gb_weigh_event" USING btree (
  "order_no" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_gb_event_source" ON "gb_record"."gb_weigh_event" USING btree (
  "source" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "event_time" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_gb_event_time" ON "gb_record"."gb_weigh_event" USING btree (
  "event_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table gb_weigh_event
-- ----------------------------
ALTER TABLE "gb_record"."gb_weigh_event" ADD CONSTRAINT "uq_gb_event" UNIQUE ("source", "order_no", "event_time");

-- ----------------------------
-- Primary Key structure for table gb_weigh_event
-- ----------------------------
ALTER TABLE "gb_record"."gb_weigh_event" ADD CONSTRAINT "gb_weigh_event_pkey" PRIMARY KEY ("id");
