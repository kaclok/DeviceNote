-- ============================================================
-- nav 数值化改造：字符串枚举列 → smallint（可重复执行）
--   nav.node.type        1=大门 2=路口 3=磅房 4=停车场 5=拉货点 6=送货点
--   nav.road.level       1=主干道 2=次干道 3=支路
--   nav.gate.gate_mode   1=双向 2=仅进厂(禁止驶出) 3=仅出厂(禁止驶入) 4=禁行(不让进不让出)
--   nav.material.direction 1=送货(进厂) 2=拉货(出厂)
--   nav.scale.gb_no      smallint 1~99，对应 gb-watcher 上报 source；NULL=未绑定
-- ============================================================

-- node.type
do $$
begin
    if exists (select 1 from information_schema.columns
               where table_schema = 'nav' and table_name = 'node'
                 and column_name = 'type' and data_type = 'character varying') then
        alter table nav.node
            alter column type type smallint
            using case type when 'gate' then 1 when 'junction' then 2 when 'scale' then 3
                            when 'parking' then 4 when 'pickup' then 5 when 'delivery' then 6 end;
    end if;
end $$;

-- road.level（原 default 'secondary'）
do $$
begin
    if exists (select 1 from information_schema.columns
               where table_schema = 'nav' and table_name = 'road'
                 and column_name = 'level' and data_type = 'character varying') then
        alter table nav.road alter column level drop default;
        alter table nav.road
            alter column level type smallint
            using case level when 'main' then 1 when 'secondary' then 2 when 'branch' then 3 end;
        alter table nav.road alter column level set default 2;
    end if;
end $$;

-- gate.gate_mode（原 default 'both'）
do $$
begin
    if exists (select 1 from information_schema.columns
               where table_schema = 'nav' and table_name = 'gate'
                 and column_name = 'gate_mode' and data_type = 'character varying') then
        alter table nav.gate alter column gate_mode drop default;
        alter table nav.gate
            alter column gate_mode type smallint
            using case gate_mode when 'both' then 1 when 'in' then 2 when 'out' then 3 when 'none' then 4 end;
        alter table nav.gate alter column gate_mode set default 1;
    end if;
end $$;

-- material.direction
do $$
begin
    if exists (select 1 from information_schema.columns
               where table_schema = 'nav' and table_name = 'material'
                 and column_name = 'direction' and data_type = 'character varying') then
        alter table nav.material
            alter column direction type smallint
            using case direction when 'in' then 1 when 'out' then 2 end;
    end if;
end $$;

-- scale.gb_no（varchar → smallint，空串转 NULL）
do $$
begin
    if exists (select 1 from information_schema.columns
               where table_schema = 'nav' and table_name = 'scale'
                 and column_name = 'gb_no' and data_type = 'character varying') then
        alter table nav.scale
            alter column gb_no type smallint
            using nullif(gb_no, '')::smallint;
    end if;
end $$;

-- ----------------------------
-- 列注释（数值编码唯一口径）
-- ----------------------------
COMMENT ON COLUMN "nav"."node"."type" IS '点位类型：1=大门(gate) 2=路口(junction) 3=磅房(scale) 4=停车场(parking) 5=拉货点(pickup) 6=送货点(delivery)';
COMMENT ON COLUMN "nav"."road"."level" IS '道路等级：1=主干道(main) 2=次干道(secondary) 3=支路(branch)';
COMMENT ON COLUMN "nav"."gate"."gate_mode" IS '通行方向：1=双向(both) 2=仅进厂(in，禁止驶出) 3=仅出厂(out，禁止驶入) 4=禁行(none，不让进不让出)';
COMMENT ON COLUMN "nav"."material"."direction" IS '物流方向：1=送货(进厂) 2=拉货(出厂)';
COMMENT ON COLUMN "nav"."scale"."gb_no" IS '磅号(1~99)，对应 gb-watcher 上报的 source；NULL=未绑定';
COMMENT ON TABLE "nav"."gate" IS '大门通行规则，与 nav.node 中 type=1(大门) 的点位一一对应';
