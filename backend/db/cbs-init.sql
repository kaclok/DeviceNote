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
    total_sets          int2         NOT NULL DEFAULT 0,       -- 生产装置套数
    prod_sets           int2         NOT NULL DEFAULT 0,       -- 运行套数
    stop_sets           int2         NOT NULL DEFAULT 0,       -- 停产套数
    hazard_lv1          int2         NOT NULL DEFAULT 0,       -- 一级重大危险源（处）
    hazard_lv2          int2         NOT NULL DEFAULT 0,       -- 二级重大危险源（处）
    hazard_lv3          int2         NOT NULL DEFAULT 0,       -- 三级重大危险源（处）
    hazard_lv4          int2         NOT NULL DEFAULT 0,       -- 四级重大危险源（处）
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

-- =====================================================================
-- 二期补齐（2026-10-29）：岗位管理 / 在线抽查 / 项目入场·开工·验收 / 类型字典
-- 枚举列一律数值类型 + 列注释全枚举（docs/数据库设计规范.md 铁律）
-- =====================================================================

-- 岗位管理（挂 cbs:org 权限；t_user 一期未关联岗位，仅台账）
CREATE TABLE IF NOT EXISTS cbs.t_post (
    post_id             varchar      NOT NULL,
    post_code           varchar      NOT NULL,                 -- 岗位编码（启用行内唯一）
    post_name           varchar      NOT NULL,
    post_status         int2         NOT NULL DEFAULT 1,       -- 1=启用 0=停用
    remark              varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_post_pkey PRIMARY KEY (post_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS t_post_code ON cbs.t_post (post_code) WHERE open_status;

-- 特殊作业报备（原「在线抽查」，挂 cbs:spotcheck 权限；字段口径见目标站 OnlineCpotCheck）
CREATE TABLE IF NOT EXISTS cbs.t_spotcheck (
    check_id                varchar      NOT NULL,
    work_type               int2         NOT NULL,             -- 作业票类型 1=特级动火 2=一级动火 3=二级动火 4=受限空间 5=盲板抽堵 6~9=一~四级高处 10~12=一~三级起重吊装 13=临时用电 14=动土 15=断路
    place                   varchar(200),                      -- 作业地点
    begin_time              timestamp,                         -- 作业开始时间
    end_time                timestamp,                         -- 作业结束时间
    is_contractor_operate   bool         NOT NULL DEFAULT false, -- 是否承包商作业
    unit_name               varchar(200),                      -- 施工单位/人员
    operation_person        varchar(50),                       -- 项目负责人
    operation_phone         varchar(50),                       -- 联系方式
    content                 varchar(1000),                     -- 作业内容
    status                  int2         NOT NULL DEFAULT 1,   -- 报备状态 1=进行中 2=已完成 3=作废 4=已归档
    report_date             date,                              -- 报备日期
    reporter                varchar(50),                       -- 报备人员（姓名）
    creator                 varchar,
    create_time             timestamp    NOT NULL DEFAULT now(),
    open_status             bool         NOT NULL DEFAULT true,
    CONSTRAINT t_spotcheck_pkey PRIMARY KEY (check_id)
);
CREATE INDEX IF NOT EXISTS t_spotcheck_report_date ON cbs.t_spotcheck (report_date);
CREATE INDEX IF NOT EXISTS t_spotcheck_begin_time  ON cbs.t_spotcheck (begin_time);

-- 入场管理（清单第18项：不做关联培训/关联考试/进出场管理/设备管理，按钮不展示）
CREATE TABLE IF NOT EXISTS cbs.t_project_enter (
    enter_id            varchar      NOT NULL,
    project_id          varchar      NOT NULL,
    enter_date          date,
    person_count        int4,
    leader              varchar,                               -- 入场负责人
    leader_phone        varchar,
    remark              varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_project_enter_pkey PRIMARY KEY (enter_id)
);
CREATE INDEX IF NOT EXISTS t_project_enter_proj ON cbs.t_project_enter (project_id);

-- 开工准备
CREATE TABLE IF NOT EXISTS cbs.t_project_prep (
    prep_id             varchar      NOT NULL,
    project_id          varchar      NOT NULL,
    prep_date           date,
    content             varchar,                               -- 准备事项
    status              int2         NOT NULL DEFAULT 0,       -- 0=未完成 1=已完成
    checker             varchar,                               -- 确认人
    remark              varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_project_prep_pkey PRIMARY KEY (prep_id)
);
CREATE INDEX IF NOT EXISTS t_project_prep_proj ON cbs.t_project_prep (project_id);

-- 项目验收
CREATE TABLE IF NOT EXISTS cbs.t_project_accept (
    accept_id           varchar      NOT NULL,
    project_id          varchar      NOT NULL,
    accept_date         date,
    acceptor            varchar,                               -- 验收人
    result              int2         NOT NULL DEFAULT 1,       -- 1=通过 0=未通过
    opinion             varchar,                               -- 验收意见
    remark              varchar,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_project_accept_pkey PRIMARY KEY (accept_id)
);
CREATE INDEX IF NOT EXISTS t_project_accept_proj ON cbs.t_project_accept (project_id);

-- 类型字典（通用：承包商类型/承包商处罚类型/项目类型，清单第21~23项）
-- dict_type 枚举：contractor_type=承包商类型 penalty_type=承包商处罚类型 project_type=项目类型
CREATE TABLE IF NOT EXISTS cbs.t_dict (
    item_id             varchar      NOT NULL,
    dict_type           varchar      NOT NULL,
    item_name           varchar      NOT NULL,
    sort_no             int2         NOT NULL DEFAULT 0,
    is_using            bool         NOT NULL DEFAULT true,
    creator             varchar,
    create_time         timestamp    NOT NULL DEFAULT now(),
    open_status         bool         NOT NULL DEFAULT true,
    CONSTRAINT t_dict_pkey PRIMARY KEY (item_id)
);
CREATE INDEX IF NOT EXISTS t_dict_type ON cbs.t_dict (dict_type, sort_no);

-- =====================================================================
-- 三期补齐（2026-10-30）：证照分「企业证书 / 企业人员证书」/ 许可危化品补列 / 重点监管工艺补列
-- 枚举类列一律数值类型 + 列注释全枚举（docs/数据库设计规范.md 铁律）
-- 说明：全部 ADD COLUMN IF NOT EXISTS，可在已有库上重复执行
-- =====================================================================

-- 证照预警：cert_type 区分两类，两类各自一页、各自分页；人员证书另存持证人三列
ALTER TABLE cbs.t_cert ADD COLUMN IF NOT EXISTS cert_type smallint NOT NULL DEFAULT 1;
COMMENT ON COLUMN cbs.t_cert.cert_type IS '证照类别：1=企业证书 2=企业人员证书';
ALTER TABLE cbs.t_cert ADD COLUMN IF NOT EXISTS holder_name varchar;
COMMENT ON COLUMN cbs.t_cert.holder_name IS '持证人姓名（仅人员证书）';
ALTER TABLE cbs.t_cert ADD COLUMN IF NOT EXISTS holder_idcard varchar;
COMMENT ON COLUMN cbs.t_cert.holder_idcard IS '持证人身份证号（仅人员证书）';
ALTER TABLE cbs.t_cert ADD COLUMN IF NOT EXISTS cert_scope varchar;
COMMENT ON COLUMN cbs.t_cert.cert_scope IS '准操项目/作业类别（仅人员证书）';
CREATE INDEX IF NOT EXISTS t_cert_type ON cbs.t_cert (cert_type, expire_date);

-- 许可的危险化学品：补危险性类别 / 剧毒 / 重大危险源 / 储存场所 / 量口径 / 备注
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS hazard_class varchar;
COMMENT ON COLUMN cbs.t_license_chem.hazard_class IS '危险性类别（按危化品目录口径，如「易燃液体 类别2」）';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS is_poison bool NOT NULL DEFAULT false;
COMMENT ON COLUMN cbs.t_license_chem.is_poison IS '是否剧毒化学品：true=是 false=否';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS is_major_hazard bool NOT NULL DEFAULT false;
COMMENT ON COLUMN cbs.t_license_chem.is_major_hazard IS '是否构成危险化学品重大危险源：true=是 false=否';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS storage_place varchar;
COMMENT ON COLUMN cbs.t_license_chem.storage_place IS '储存场所（罐区/仓库名称）';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS critical_qty varchar;
COMMENT ON COLUMN cbs.t_license_chem.critical_qty IS '重大危险源临界量（吨，文本口径）';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS actual_qty varchar;
COMMENT ON COLUMN cbs.t_license_chem.actual_qty IS '实际存在量（吨，文本口径）';
ALTER TABLE cbs.t_license_chem ADD COLUMN IF NOT EXISTS remark varchar;
COMMENT ON COLUMN cbs.t_license_chem.remark IS '备注';

-- 重点监管的危险化工工艺：补反应类型 / 涉及危化品 / 装置 / 三套系统投用 / HAZOP / 危险度
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS proc_type varchar;
COMMENT ON COLUMN cbs.t_supervise_process.proc_type IS '反应类型（光气化/氯化/硝化/磺化…，见 gd.json#procTypes）';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS chem_involved varchar;
COMMENT ON COLUMN cbs.t_supervise_process.chem_involved IS '涉及危险化学品（多个以、分隔）';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS device_name varchar;
COMMENT ON COLUMN cbs.t_supervise_process.device_name IS '工艺装置/所在车间';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS esd_in_use bool NOT NULL DEFAULT false;
COMMENT ON COLUMN cbs.t_supervise_process.esd_in_use IS '紧急停车系统是否投用：true=是 false=否';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS sis_in_use bool NOT NULL DEFAULT false;
COMMENT ON COLUMN cbs.t_supervise_process.sis_in_use IS '安全仪表系统是否投用：true=是 false=否';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS hazop_done bool NOT NULL DEFAULT false;
COMMENT ON COLUMN cbs.t_supervise_process.hazop_done IS '是否已开展 HAZOP 分析：true=是 false=否';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS risk_level varchar;
COMMENT ON COLUMN cbs.t_supervise_process.risk_level IS '反应工艺危险度等级（1级~5级，见 gd.json#riskLevels）';
ALTER TABLE cbs.t_supervise_process ADD COLUMN IF NOT EXISTS remark varchar;
COMMENT ON COLUMN cbs.t_supervise_process.remark IS '备注';

-- =====================================================================
-- 四期补齐（2026-10-30）：资质管理 / 开工准备条件 / 资料清单
-- 说明：全部 CREATE TABLE IF NOT EXISTS，可在已有库上重复执行
-- =====================================================================

-- 承包商类型所需资质（资质管理，挂在 contractor_type 字典项 item_id 下）
CREATE TABLE IF NOT EXISTS cbs.t_contractor_qual (
    qual_id     varchar      NOT NULL,
    type_id     varchar      NOT NULL,                 -- -> t_dict.item_id (dict_type=contractor_type)
    qual_name   varchar      NOT NULL,
    qual_level  varchar,                               -- 等级/类别（如 一级 / 甲级），可空
    creator     varchar,
    create_time timestamp    NOT NULL DEFAULT now(),
    open_status bool         NOT NULL DEFAULT true,
    CONSTRAINT t_contractor_qual_pkey PRIMARY KEY (qual_id)
);
CREATE INDEX IF NOT EXISTS t_contractor_qual_type ON cbs.t_contractor_qual (type_id);

-- 开工准备条件（按项目类型维护：名称 + 说明；type_id 关联 t_dict.item_id）
CREATE TABLE IF NOT EXISTS cbs.t_prep_condition (
    id          varchar      NOT NULL,
    type_id     varchar,                               -- -> t_dict.item_id (dict_type=project_type)
    name        varchar      NOT NULL,
    remark      varchar,
    creator     varchar,
    create_time timestamp    NOT NULL DEFAULT now(),
    open_status bool         NOT NULL DEFAULT true,
    CONSTRAINT t_prep_condition_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS t_prep_condition_type ON cbs.t_prep_condition (type_id);

-- 资料清单（按项目类型维护：名称 + 说明；type_id 关联 t_dict.item_id）
CREATE TABLE IF NOT EXISTS cbs.t_doc_list (
    id          varchar      NOT NULL,
    type_id     varchar,                               -- -> t_dict.item_id (dict_type=project_type)
    name        varchar      NOT NULL,
    remark      varchar,
    creator     varchar,
    create_time timestamp    NOT NULL DEFAULT now(),
    open_status bool         NOT NULL DEFAULT true,
    CONSTRAINT t_doc_list_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS t_doc_list_type ON cbs.t_doc_list (type_id);
