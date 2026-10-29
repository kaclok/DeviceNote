-- =====================================================================
-- cbs（承包商安全管理）一期建表脚本 —— device-note 库，schema cbs
-- 依据：docs/cbs-一期规划-PRD.md
-- 约定：口令 BCrypt(10轮, $2a$)，唯一口径 core/utils/PwdUtil；
--       树辅助列口径同 backend/db/tree-tag.postgres.sql（level 根=1、node_type 1根/2中/3叶、空子集=NULL）
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS cbs;

-- ---------------------------------------------------------------------
-- 账号与组织（cbs 独立账号体系，结构对齐 dept_user 模型）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cbs.t_dept (
    dept_code           varchar      NOT NULL,
    dept_name           varchar      NOT NULL,
    parent_dept_code    varchar,
    is_company          bool         NOT NULL DEFAULT false,   -- 公司节点标记（人工维护）
    level               int2,                                  -- 层号，根=1（tree-tag 口径）
    node_type           int2,                                  -- 1根/2中间/3叶（tree-tag 口径）
    direct_children     varchar[],                             -- 直系子部门，无=NULL
    recursive_children  varchar[],                             -- 递归子部门，无=NULL
    is_using            bool         NOT NULL DEFAULT true,
    create_time         timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT t_dept_pkey PRIMARY KEY (dept_code)
);

CREATE TABLE IF NOT EXISTS cbs.t_role (
    role_code           varchar      NOT NULL,
    role_name           varchar      NOT NULL,
    data_scope          varchar(1)   NOT NULL,                 -- 1本人/2本部门(含下级)/3本公司/4全集团
    perms               varchar[]    NOT NULL DEFAULT '{}',    -- 权限点数组
    is_using            bool         NOT NULL DEFAULT true,
    create_time         timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT t_role_pkey PRIMARY KEY (role_code)
);

-- user_type：1 我司内部员工 / 2 承包商人员（org.vue 人员管理按类型筛选；
-- user_type=2 时 contractor_id 必填，关联 cbs.t_contractor）
CREATE TABLE IF NOT EXISTS cbs.t_user (
    account             varchar      NOT NULL,
    username            varchar      NOT NULL,
    pwd                 varchar      NOT NULL,                 -- BCrypt 密文
    role_code           varchar      NOT NULL,                 -- -> cbs.t_role（生效档位 = 角色 data_scope）
    dept_code           varchar      NOT NULL,
    user_type           int2         NOT NULL DEFAULT 1,
    contractor_id       varchar,
    is_using            bool         NOT NULL DEFAULT true,
    create_time         timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT t_user_pkey PRIMARY KEY (account)
);
CREATE INDEX IF NOT EXISTS t_user_dept_idx   ON cbs.t_user (dept_code);
CREATE INDEX IF NOT EXISTS t_user_contractor ON cbs.t_user (contractor_id);

-- ---------------------------------------------------------------------
-- 业务表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cbs.t_contractor (
    contractor_id       varchar      NOT NULL,                 -- 业务主键（cghtz unique_id 同款生成）
    name                varchar      NOT NULL,
    uscc                varchar,                               -- 统一社会信用代码
    ctype               varchar,                               -- 承包商类型（gd.json 字典）
    legal_person        varchar,
    legal_phone         varchar,
    safety_person       varchar,
    safety_phone        varchar,
    register_address    varchar,
    register_capital    varchar,                               -- 注册资本（原系统文本口径）
    deposit             numeric,                               -- 保证金(元)
    login_account       varchar,                               -- 预留：一期不开承包商登录
    login_pwd           varchar,                               -- 预留：密文
    approve_status      int2         NOT NULL DEFAULT 0,       -- 0待审 1通过 2驳回
    reject_reason       varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_contractor_pkey PRIMARY KEY (contractor_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS t_contractor_uscc ON cbs.t_contractor (uscc) WHERE uscc IS NOT NULL AND open_status;

CREATE TABLE IF NOT EXISTS cbs.t_project (
    project_id          varchar      NOT NULL,
    contractor_id       varchar      NOT NULL,
    proj_name           varchar      NOT NULL,
    proj_content        varchar,
    begin_date          date,
    end_date            date,
    proj_status         int2         NOT NULL DEFAULT 0,       -- 0未开始 1进行中 2已结束
    dept_code           varchar,                               -- 归属部门（数据范围起点）
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_project_pkey PRIMARY KEY (project_id)
);
CREATE INDEX IF NOT EXISTS t_project_contractor ON cbs.t_project (contractor_id);
CREATE INDEX IF NOT EXISTS t_project_dept       ON cbs.t_project (dept_code);

-- 安全承诺日报：一天一行，promise_date 即业务主键；字段口径见 PRD 附录 C
CREATE TABLE IF NOT EXISTS cbs.t_promise (
    promise_date        date         NOT NULL,
    prod_sets           int2         NOT NULL DEFAULT 0,       -- 运行套数
    stop_sets           int2         NOT NULL DEFAULT 0,       -- 停产套数
    hazard_lv1          int2         NOT NULL DEFAULT 0,
    hazard_lv2          int2         NOT NULL DEFAULT 0,
    hazard_lv3          int2         NOT NULL DEFAULT 0,
    hazard_lv4          int2         NOT NULL DEFAULT 0,
    start_sets          int2         NOT NULL DEFAULT 0,       -- 开车装置数
    stop_dev_sets       int2         NOT NULL DEFAULT 0,       -- 停车装置数
    trial_sets          int2         NOT NULL DEFAULT 0,       -- 试生产装置数
    supervision_process int2         NOT NULL DEFAULT 0,       -- 重点监管危险工艺
    repair_sets         int2         NOT NULL DEFAULT 0,       -- 检维修套数
    fire_special        int2         NOT NULL DEFAULT 0,       -- 特级动火
    fire_lv1            int2         NOT NULL DEFAULT 0,
    fire_lv2            int2         NOT NULL DEFAULT 0,
    confined            int2         NOT NULL DEFAULT 0,       -- 受限空间
    blind               int2         NOT NULL DEFAULT 0,       -- 盲板抽堵
    work_height         int2         NOT NULL DEFAULT 0,       -- 高处
    lifting             int2         NOT NULL DEFAULT 0,       -- 吊装
    temp_power          int2         NOT NULL DEFAULT 0,       -- 临时用电
    digging             int2         NOT NULL DEFAULT 0,       -- 动土
    road_close          int2         NOT NULL DEFAULT 0,       -- 断路
    repair_work         int2         NOT NULL DEFAULT 0,       -- 检维修作业
    has_contractor_work bool         NOT NULL DEFAULT false,
    in_trial            bool         NOT NULL DEFAULT false,   -- 试生产期
    in_startstop        bool         NOT NULL DEFAULT false,   -- 开停车状态
    in_pilot            bool         NOT NULL DEFAULT false,   -- 中(扩)试
    has_major_hazard    bool         NOT NULL DEFAULT false,   -- 重大隐患
    company_promise     text,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT t_promise_pkey PRIMARY KEY (promise_date)
);

-- 企业信息：单行表（PRD 附录 A 全量 63 项映射；附件/四色图一期只存 file 引用）
CREATE TABLE IF NOT EXISTS cbs.t_enterprise (
    id                       int2         NOT NULL DEFAULT 1, -- 恒为 1 的单行
    ent_name                 varchar,
    uscc                     varchar,
    legal_person             varchar,
    legal_phone              varchar,
    legal_idcard             varchar,
    chem_reg_code            varchar,                        -- 危化品登记系统企业编码
    found_date               date,
    industry_type            varchar,                        -- 行业分类
    supervisor               varchar,                        -- 主管部门
    staff_count              int4,
    region_code              varchar,                        -- 行政区划
    address                  varchar,                        -- 单位地址
    fixed_assets             numeric,                        -- 固定资产(万元)
    total_area               numeric,                        -- 总建筑面积(㎡)
    industry_detail          varchar,                        -- 企业所属行业及类型
    sub_units                varchar,                        -- 下级单位
    economy_type             varchar,                        -- 经济类型
    run_status               varchar,                        -- 企业经营状态
    supervise_level          varchar,                        -- 监管层级
    main_person              varchar,                        -- 企业主要负责人
    main_person_idcard       varchar,
    safety_org               varchar,                        -- 安全管理机构
    safety_org_phone         varchar,
    safety_org_leader        varchar,
    safety_org_leader_phone  varchar,
    duty_phone               varchar,                        -- 值班电话
    full_safety_count        int4,                           -- 专职安全管理人员数
    part_safety_count        int4,                           -- 兼职安全管理人员数
    has_major_hazard         bool,                           -- 是否有危化品重大危险源
    risk_identified          bool,                           -- 是否进行风险辨识
    hazard_filed             varchar,                        -- 重大危险源是否备案（是/否/不涉及）
    has_danger_process       varchar,                        -- 涉及危险工艺（是/否/不涉及）
    capacity                 varchar,                        -- 企业生产能力
    ent_scale                varchar,                        -- 企业规模
    ent_position             varchar,                        -- 企业定位
    two_key_one_major        varchar,                        -- 是否涉及两重点一重大作业（是/否/不涉及）
    server_addr              varchar,                        -- 企业端服务器地址
    mqtt_topic               varchar,
    attach_ids               varchar[],                      -- 附件 file 引用
    plant_map_id             varchar,                        -- 厂区四色图 file 引用
    license_no               varchar,                        -- 许可证编号
    license_begin            date,
    license_end              date,
    park_name                varchar,                        -- 化工园区名称
    prod_start_date          date,                           -- 投产时间
    county                   varchar,                        -- 所属县(市、区)
    register_address         varchar,
    product_address          varchar,
    special_work_count       int4,                           -- 特种作业人员数
    safety_std_level         varchar,                        -- 安全标准级别
    std_cert_expire          date,                           -- 安全标准化证书有效期截止
    monitor_system           bool,                           -- 是否接入危化品监测预警系统
    has_supervision_process  bool,                           -- 是否有重点监管的危险化工工艺
    ent_intro                text,                           -- 企业简介（富文本，原系统 CaompanyOverview）
    update_time              timestamp,
    update_by                varchar,
    CONSTRAINT t_enterprise_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cbs.t_cert (
    cert_id             varchar      NOT NULL,
    cert_name           varchar      NOT NULL,
    cert_no             varchar,
    issue_org           varchar,
    issue_date          date,
    expire_date         date,                                  -- 到期预警判据
    warn_days           int2         NOT NULL DEFAULT 30,
    dept_code           varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_cert_pkey PRIMARY KEY (cert_id)
);
CREATE INDEX IF NOT EXISTS t_cert_expire ON cbs.t_cert (expire_date);

CREATE TABLE IF NOT EXISTS cbs.t_accident (
    accident_id         varchar      NOT NULL,
    title               varchar      NOT NULL,
    occur_date          date,
    level               int2,                                  -- 事故等级（gd.json 字典）
    accident_type       varchar,
    brief               varchar,
    loss                varchar,
    dept_code           varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_accident_pkey PRIMARY KEY (accident_id)
);

-- 安全"三同时"项目台账（原系统 /threeSimultaneity/project）
CREATE TABLE IF NOT EXISTS cbs.t_three_same (
    ts_id               varchar      NOT NULL,
    project_name        varchar      NOT NULL,
    project_type        varchar,
    approval_date       date,                                  -- 项目立项时间
    reply_date          date,                                  -- 项目批复时间
    progress            varchar,                               -- 项目进展
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_three_same_pkey PRIMARY KEY (ts_id)
);

-- 许可的危险化学品（原系统 /system/sysCompanyInvolveHazardChemicals）
CREATE TABLE IF NOT EXISTS cbs.t_license_chem (
    chem_id             varchar      NOT NULL,
    chem_name           varchar      NOT NULL,
    is_key              bool         NOT NULL DEFAULT false,   -- 是否属于重点监管的危险化学品
    storage             varchar,                               -- 储存量（原系统文本口径）
    yearly              varchar,                               -- 年使用/产量（原系统文本口径）
    state               varchar,                               -- 物料状态
    fire_class          varchar,                               -- 火灾危险类别
    catalog_no          varchar,                               -- 危险化学品目录序号
    cas_no              varchar,
    longitude           varchar,                               -- 经纬度
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_license_chem_pkey PRIMARY KEY (chem_id)
);

-- 涉及的重点监管的危险化工工艺（原系统 /system/sysCompanyInvolveHazardProcess）
CREATE TABLE IF NOT EXISTS cbs.t_supervise_process (
    proc_id             varchar      NOT NULL,
    proc_name           varchar      NOT NULL,
    auto_in_use         bool         NOT NULL DEFAULT false,   -- 自动化控制系统、紧急停车系统是否投入使用
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_supervise_process_pkey PRIMARY KEY (proc_id)
);

-- ---------------------------------------------------------------------
-- 种子数据（角色 + 根部门 + 初始管理员；口令见文件头注释，由 PwdUtil 口径生成）
-- ⚠️ <PWD_HASH> 占位替换为 BCrypt($2a$10$...) 密文后再执行本段
-- ---------------------------------------------------------------------
-- INSERT INTO cbs.t_role (role_code, role_name, data_scope, perms)
-- VALUES ('ADMIN', '系统管理员', '4',
--         ARRAY['cbs:org','cbs:contractor','cbs:audit','cbs:project','cbs:promise','cbs:baseinfo']);
-- INSERT INTO cbs.t_dept (dept_code, dept_name, parent_dept_code, is_company, level, node_type)
-- VALUES ('ROOT', '集团公司', NULL, true, 1, 1);
-- INSERT INTO cbs.t_user (account, username, pwd, role_code, dept_code, user_type)
-- VALUES ('admin', '系统管理员', '<PWD_HASH>', 'ADMIN', 'ROOT', 1);
