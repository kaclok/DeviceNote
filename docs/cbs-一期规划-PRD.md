# cbs（承包商安全管理）一期规划 PRD

> 依据：对「智慧安全生产管控平台」（113.135.193.137:8081，Vue + Ant Design）的实机调研。
> 复刻其 基础设置 / 承包商 / 安全承诺 三模块，采用 cghtz 同款前后端架构（Vue3 + Vite MPA + Element Plus / SysX-ApiX / JWT / 权限点 + 数据范围）。
> 2026-10-15 定稿。已确认决策：**cbs 独立账号表；企业信息全量 63 项；资质附件上传后置；二维码申请、进出场管理、岗位管理、设备类型、设备检查表、3D 模型配置、区域管理、地图配置一期不做。**

## 一、模块与页面（前端 `ft-checnote/src/cms/smlj/cbs/`，Element Plus）

```
cbs/
  main.js / gd.json（承诺字段定义、承包商类型、审批状态等字典）/ hd.json
  api/   ApiX.js  ApiLogin.js
  router/ Index.js  Router.js
  views/ login.vue      登录（cbs 独立账号）
         home.vue       模块门户（对应原系统 middleLayout2 的入口卡片）
         contractor.vue 承包商管理（单位管理 + 承包商审核 两 tab）
         project.vue    项目管理
         promise.vue    安全承诺（按日列表 + 承诺填表 + 详情）
         baseinfo.vue   基础设置（企业信息 / 证照预警 / 企业事件事故 三 tab）
         org.vue        组织管理（部门 / 人员 / 角色 三 tab）
```

- `backends.mjs` 注册 `cbs: { api: "/api-cbs", url: 同 cghtz 后端 }`
- 复用 cghtz 沉淀：DeptPicker、服务端分页、SysX/ApiX 三层、请求取消判据、状态色（超期红/临期橙/未到期蓝/正常绿）

## 二、数据表（device-note 库，schema `cbs`，与 cght 同 PG 实例免 @DS）

### 账号与组织（cbs 独立，结构对齐 dept_user 模型）

| 表 | 字段 |
|---|---|
| `cbs.t_user` | account **PK** / username / pwd（PwdUtil BCrypt）/ **role_code**（→cbs.t_role，生效档位=角色 data_scope）/ dept_code / **user_type int2（1 我司内部员工 / 2 承包商人员）** / **contractor_id（user_type=2 必填，关联 t_contractor）** / is_using / create_time |
| `cbs.t_role` | role_code **PK** / role_name / data_scope varchar '1'~'4' / perms varchar[] / is_using |
| `cbs.t_dept` | dept_code **PK** / dept_name / parent_dept_code / is_company bool / level int2 / node_type int2 / direct_children / recursive_children（类型同 dept_code）/ is_using |

树辅助列由 `/x/warpOrgs` 同款回写逻辑维护（cbs 版实现），level/node_type 用 `backend/db/tree-tag.postgres.sql` 口径。

### 业务表

| 表 | 字段 |
|---|---|
| `cbs.t_contractor` 承包商单位 | contractor_id varchar **PK** / name / uscc 统一社会信用代码 / ctype 承包商类型 / legal_person / legal_phone / safety_person / safety_phone / register_address / register_capital / deposit 保证金 / login_account（预留，一期不开登录）/ login_pwd（预留，密文）/ approve_status int2（0待审 1通过 2驳回）/ reject_reason / creator / create_time / open_status |
| `cbs.t_project` 承包商项目 | project_id varchar **PK** / contractor_id FK / proj_name / proj_content / begin_date / end_date / proj_status int2（0未开始 1进行中 2已结束）/ dept_code 归属部门 / creator / create_time / open_status |
| `cbs.t_promise` 安全承诺日报 | promise_date date **PK**（一天一行）/ prod_sets 运行 / stop_sets 停产 / hazard_lv1~lv4 int2 ×4 / start_sets 开车 / stop_dev_sets 停车 / trial_sets 试生产 / supervision_process 重点监管工艺数 / repair_sets 检维修 / fire_special 特级动火 / fire_lv1 / fire_lv2 / confined 受限空间 / blind 盲板抽堵 / work_height 高处 / lifting 吊装 / temp_power 临时用电 / digging 动土 / road_close 断路 / repair_work 检维修作业（以上均 int2）/ has_contractor_work bool / in_trial bool / in_startstop bool / in_pilot bool（中扩试）/ has_major_hazard bool / company_promise text / creator / create_time |
| `cbs.t_enterprise` 企业信息 | id **PK** / 按附录 A 全量 63 项映射列 / update_time / update_by（单行表） |
| `cbs.t_cert` 证照 | cert_id **PK** / cert_name / cert_no / issue_org / issue_date / expire_date / warn_days int2 默认 30 / dept_code / creator / create_time / open_status |
| `cbs.t_accident` 事件事故 | accident_id **PK** / title / occur_date / level int2 / accident_type / brief / loss / dept_code / creator / create_time / open_status |

## 三、后端（single-device-note 工程，仿 CCGHT 组织）

| 新增 | 职责 |
|---|---|
| `CCbsLogin` | cbs 独立登录：`/cbs/login`，PwdUtil 校验 cbs.t_user，签发 JWT（与 cghtz 同一套 at/rt 机制，token 内带 realm=cbs 区分） |
| `CCbsUser` | 组织管理：dept/role/user CRUD + `warpOrgs` 回写 + `getScopeDepts`（范围解析照 CDeptUser 口径搬 cbs 表）；`/cbs/**` 由 TokenInterceptor 拦截后按 account 查 cbs.t_user 填 `@Acc`（CurUserService 加 cbs 分支或新建） |
| `CContractor` | `/cbs/contractor/*`（list/save/delete/audit）+ `/cbs/project/*`；audit 走 `@RequirePermission("cbs:audit")`，list 走 deptFilterOf 口径 |
| `CBaseInfo` | `/cbs/enterprise/*`（get/save）+ `/cbs/cert/*`（list/save/delete，到期预警字段计算）+ `/cbs/accident/*` |
| `CSafePromise` | `/cbs/promise/list`（按月查）/ `fill`（当日承诺，PK 冲突即改）/ `detail`；字段定义放 gd.json |
| `RegistryOf` | TokenInterceptor 拦截路径扩为 `/cghtz/**, /cbs/**` |

- 权限点（追加到 cbs.t_role.perms）：`cbs:contractor` `cbs:audit` `cbs:project` `cbs:promise` `cbs:baseinfo` `cbs:org`
- 数据范围：`cbs.t_role.data_scope` 四档，口径与 cghtz 严格一致（SELF=本部门、DEPT=子树、COMPANY=is_company 定位、ALL 不限；三态 null/[]/白名单）

## 四、实施顺序

| 里程碑 | 内容 |
|---|---|
| M1 | ✅ 已完成（2026-10-15）：cbs schema DDL（9 表）已真库执行；PwdUtil（hutool BCrypt 10 轮）；CCbsLogin（真校验口令、AT/RT 带 realm=cbs）；TokenInterceptor 域隔离 + RegistryOf 放行 /cbs/**；CurCbsUserService；CCbsUser（部门/角色/人员 CRUD + warpOrgs 回写）。种子：ADMIN 角色（4 档 + 6 权限点）/ ROOT 部门 / admin 账号（初始口令 123456） |
| M2 | CContractor（单位+项目）+ 前端骨架（main/router/api/gd/hd/login/home）+ contractor.vue / project.vue 联调 |
| M3 | CSafePromise + promise.vue |
| M4 | CBaseInfo + baseinfo.vue + org.vue |
| M5 | 校验套件收口（对齐 cghtz `_runall.py` 模式） |

## 附录 A：企业信息字段（原系统实测 63 项，含单选项）

企业名称 / 统一社会信用代码 / 法定代表人 / 法定代表人联系电话 / 法定代表人身份证号 / 危险化学品登记信息管理系统企业编码 / 成立时间 / 行业分类 / 主管部门 / 职工总数 / 行政区划 / 单位地址 / 固定资产（万元）/ 总建筑面积(㎡) / 企业所属行业及类型 / 下级单位 / 经济类型 / 企业经营状态 / 监管层级 / 企业主要负责人 / 主要负责人身份证号 / 安全管理机构 / 安全管理机构电话 / 安全管理机构负责人 / 安全管理机构负责人手机 / 值班电话 / 专职安全管理人员数 / 兼职安全管理人员数 / 是否有危化品重大危险源 / 是否进行风险辨识 / 危化品重大危险源是否备案 / 涉及危险工艺 / 企业生产能力 / 企业规模 / 企业定位 / 是否涉及两重点一重大作业 / 企业端服务器地址 / mqtt_topic / 附件 / 厂区四色图 / 许可证编号 / 许可证有效开始时间 / 许可证有效截至时间 / 化工园区名称 / 投产时间 / 所属县（市、区）/ 注册地址 / 生产地址 / 特种作业人员数 / 安全标准级别 / 安全生产标准化证书有效期截止时间 / 是否接入危险化学品监测预警系统 / 是否有重点监管的危险化工工艺

（「是/否/不涉及」为单选项非字段；附件/厂区四色图一期存 file_id 引用，不上传交互。）

## 附录 B：承包商新增表单（原系统实测 16 项）

承包商类型 / 承包商名称 / 统一社会信用代码 / 法定代表人 / 安全负责人 / 安全负责人电话 / 状态 / 注册地址 / 注册资本 / 登录账号 / 登录密码 / 手机号码 / 身份证号 / 岗位 / 角色 / 保证金(元)

> 原系统承包商自带登录账号——一期 t_contractor 预留 login_account/login_pwd 列但**不开登录**；岗位/角色列一期不建（用户已裁剪岗位管理）。

## 附录 C：安全承诺填报表单（原系统实测 29 项）

生产装置套数（运行套数/停产套数）/ 一~四级重大危险源 / 开车装置数 / 停车装置数 / 试生产装置数 / 重点监管危险工艺 / 检维修套数 / 特级动火作业 / 一级动火作业 / 二级动火作业 / 进入受限空间作业 / 盲板抽堵作业 / 高处作业 / 吊装作业 / 临时用电作业 / 动土作业 / 断路作业 / 检维修作业 / 是否有承包商作业 / 是否处于试生产期 / 是否处于开停车状态 / 是否开展中(扩)试 / 有无重大隐患 / 公司承诺

（与 `t_promise` 列一一对应，字段定义放前端 gd.json 驱动表单渲染。）
