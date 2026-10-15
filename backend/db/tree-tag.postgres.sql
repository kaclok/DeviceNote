-- ============================================================================
--  树表标注 —— 为自引用父子表补 level / node_type / direct_children / recursive_children
--
--  方言：PostgreSQL（已在 PostgreSQL 17.2 · device-note 上实跑验证）
--
--  口径
--    level               int2          节点在树中的层号，**根 = 1**，逐层 +1
--    node_type         int2          1 = 根节点   父值为空 或 父值在本表中不存在
--                                     2 = 中间节点 本行有子节点
--                                     3 = 叶子节点 本行无子节点
--    direct_children     与 x 同类型[]  **直系**子节点（只一层，孙节点不含）
--                                     · 元素 = 子节点自身的 x 值，按 x 升序
--                                     · **没有直系子节点时留 NULL，不是空数组 {}**
--    recursive_children  与 x 同类型[]  **直系 + 所有递归下去的孙系节点**（在 direct 基础上继续往下）
--                                     · 元素 = 所有后代自身的 x 值，**不含本行自己**，按 x 升序
--                                     · **没有任何后代时留 NULL，不是空数组 {}**
--                                     · 恒有 direct_children ⊂ recursive_children（direct 非空时）
--
--  权限：需要表 owner 或超级用户（要 ALTER TABLE）
--  幂等：可重复执行，加了列就跳过，值每次整表重算
-- ============================================================================


-- ============================================================================
--  方式一（推荐）：DO 块 —— 只改下面 4 行参数，整段执行
-- ============================================================================
DO $tag$
DECLARE
    -- ====================== 改这里 ======================
    v_schema text := 'dept_user';            -- schema
    v_table  text := 't_dept';               -- 表名
    v_x      text := 'dept_code';            -- 节点自身列（x）
    v_px     text := 'parent_dept_code';     -- 父节点列（parent_x）
    -- ====================================================
    v_tbl   text := format('%I.%I', v_schema, v_table);
    v_col1  text := 'level';
    v_col2  text := 'node_type';
    v_col3  text := 'direct_children';
    v_col4  text := 'recursive_children';
    v_xtype text;
    v_ndims int;
    v_root int; v_mid int; v_leaf int; v_orphan int; v_total int;
    v_kids int; v_kidtotal int;
    v_rkids int; v_rkidtotal int;
BEGIN
    -- ---- 0. 取 x 列的类型：两列数组都用同类型，便于和 x 直接比较 ----------
    SELECT format_type(a.atttypid, NULL), a.attndims
      INTO v_xtype, v_ndims
      FROM pg_attribute a
     WHERE a.attrelid = v_tbl::regclass
       AND a.attname  = v_x
       AND a.attnum   > 0
       AND NOT a.attisdropped;

    IF v_xtype IS NULL THEN
        RAISE EXCEPTION '表 % 上找不到列 %', v_tbl, v_x;
    END IF;
    IF v_ndims > 0 THEN
        RAISE EXCEPTION '列 %.% 本身已是数组类型(%)，不能作为数组列的元素类型',
                        v_schema, v_x, v_xtype;
    END IF;

    -- ---- 1. 加列（幂等）------------------------------------------------
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS %I int2', v_tbl, v_col1);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS %I int2', v_tbl, v_col2);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS %I %s[]', v_tbl, v_col3, v_xtype);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS %I %s[]', v_tbl, v_col4, v_xtype);

    -- ---- 2. level：根 = 1，递归下推 ------------------------------------
    --   注意两处都要 cast 成 int2 —— smallint + int 会被提升成 int，
    --   递归 CTE 两个分支类型不一致会直接报 DatatypeMismatch。
    --   tree.lvl < 64 是防环保险；真成环的节点不在任何根的子树上，
    --   结果会留 NULL，正好当「游离/成环」的信号。
    EXECUTE format($q$
        WITH RECURSIVE tree(node, lvl) AS (
            SELECT t.%1$I, 1::int2
            FROM %2$s t
            WHERE t.%3$I IS NULL
               OR t.%3$I::text = ''
               OR NOT EXISTS (SELECT 1 FROM %2$s p WHERE p.%1$I = t.%3$I)
          UNION ALL
            SELECT c.%1$I, (tree.lvl + 1)::int2
            FROM %2$s c
            JOIN tree ON c.%3$I = tree.node
            WHERE tree.lvl < 64
        ),
        d AS (SELECT node, MIN(lvl) AS lvl FROM tree GROUP BY node)
        UPDATE %2$s u SET %4$I = d.lvl
        FROM d WHERE u.%1$I = d.node
    $q$, v_x, v_tbl, v_px, v_col1);

    -- ---- 3. node_type：1 根 / 2 中间 / 3 叶 --------------------------
    EXECUTE format($q$
        UPDATE %2$s u SET %4$I = CASE
            WHEN u.%3$I IS NULL OR u.%3$I::text = ''                     THEN 1
            WHEN NOT EXISTS (SELECT 1 FROM %2$s p WHERE p.%1$I = u.%3$I) THEN 1
            WHEN     EXISTS (SELECT 1 FROM %2$s c WHERE c.%3$I = u.%1$I) THEN 2
            ELSE 3
        END
    $q$, v_x, v_tbl, v_px, v_col2);

    -- ---- 4. direct_children：只装直系子节点，没有就留 NULL -------------
    --   取「父值 = 本行 x」的那些行的 x，按 x 升序聚成数组。
    --   array_agg 在「一行都没选中」时返回 NULL —— 正好满足
    --   「不存在直系子节点 = NULL，而不是空数组 {}」，所以这里**不要** coalesce。
    --   父值为 NULL / 空串的行一律不算别人的子节点（防有人把 x 也存成空串时互相挂上）。
    EXECUTE format($q$
        UPDATE %2$s u SET %4$I = (
            SELECT array_agg(c.%1$I ORDER BY c.%1$I)
            FROM %2$s c
            WHERE c.%3$I IS NOT NULL
              AND c.%3$I::text <> ''
              AND c.%3$I = u.%1$I
        )
    $q$, v_x, v_tbl, v_px, v_col3);

    -- ---- 5. recursive_children：直系 + 所有递归子孙，没有就留 NULL -----
    --   在 direct 的基础上继续往下递归：(祖先, 后代) 全对儿，按祖先分组聚成数组。
    --   同样靠 array_agg 零行返回 NULL 来满足「没有后代 = NULL」。
    --   ⚠️ 先整列置 NULL 再填：UPDATE ... FROM agg 只会覆盖「有后代的行」，
    --      不先清空的话，删过子节点的行会残留上一次算出来的旧值，破坏幂等。
    --   lvl < 64 同 level，防数据成环时递归打不住；DISTINCT 顺带去重。
    EXECUTE format('UPDATE %s SET %I = NULL', v_tbl, v_col4);

    EXECUTE format($q$
        WITH RECURSIVE down(root, node, lvl) AS (
            SELECT p.%1$I, c.%1$I, 1
            FROM %2$s p
            JOIN %2$s c ON c.%3$I = p.%1$I
            WHERE c.%3$I IS NOT NULL AND c.%3$I::text <> ''
          UNION ALL
            SELECT d.root, c.%1$I, d.lvl + 1
            FROM down d
            JOIN %2$s c ON c.%3$I = d.node
            WHERE c.%3$I IS NOT NULL AND c.%3$I::text <> ''
              AND d.lvl < 64
        ),
        agg AS (
            SELECT root, array_agg(DISTINCT node ORDER BY node) AS kids
            FROM down GROUP BY root
        )
        UPDATE %2$s u SET %4$I = agg.kids
        FROM agg WHERE u.%1$I = agg.root
    $q$, v_x, v_tbl, v_px, v_col4);

    -- ---- 6. 结果统计 ---------------------------------------------------
    EXECUTE format('SELECT count(*), count(*) FILTER (WHERE %I IS NULL) FROM %s',
                   v_col1, v_tbl) INTO v_total, v_orphan;
    EXECUTE format('SELECT count(*) FILTER (WHERE %I = 1), count(*) FILTER (WHERE %I = 2),'
                || ' count(*) FILTER (WHERE %I = 3) FROM %s',
                   v_col2, v_col2, v_col2, v_tbl) INTO v_root, v_mid, v_leaf;
    EXECUTE format('SELECT count(*) FILTER (WHERE %I IS NOT NULL),'
                || ' coalesce(sum(cardinality(%I)), 0) FROM %s',
                   v_col3, v_col3, v_tbl) INTO v_kids, v_kidtotal;
    EXECUTE format('SELECT count(*) FILTER (WHERE %I IS NOT NULL),'
                || ' coalesce(sum(cardinality(%I)), 0) FROM %s',
                   v_col4, v_col4, v_tbl) INTO v_rkids, v_rkidtotal;

    RAISE NOTICE '表 %  共 % 行', v_tbl, v_total;
    RAISE NOTICE '  node_type        : 根=%  中间=%  叶子=%', v_root, v_mid, v_leaf;
    RAISE NOTICE '  direct_children    : 有直系子节点的行=%  直系子节点总数=%', v_kids, v_kidtotal;
    RAISE NOTICE '  recursive_children : 有后代的行=%  后代总数=%', v_rkids, v_rkidtotal;
    RAISE NOTICE '  未被标上层号(父值成环/游离)=%', v_orphan;
END
$tag$;


-- ---- 查看结果 --------------------------------------------------------------
--   ⚠️ 这段 SQL 的表名/列名和上面 DO 块里的 4 个参数**不会联动**，必须自己一起改。
--      DO 块改的是 v_table，这里却仍写死 dept_user.t_dept，
--      就会报：错误: 字段 "level" 不存在 LINE 2: SELECT ... level ...
--      （因为加列加在 v_table 上，而这段查的是另一张表）
SELECT dept_code, dept_name, parent_dept_code, level, node_type,
       direct_children, recursive_children
FROM dept_user.t_dept          -- ← 改成你的 v_table
ORDER BY level, dept_code;


-- ============================================================================
--  方式二：等价的纯 SQL 模板（不用 DO 块，把 3 个名字直接替换掉即可）
--  下面以 dept_user.t_dept 为例，换成你自己的表/列名就行。
-- ============================================================================
/*
ALTER TABLE dept_user.t_dept ADD COLUMN IF NOT EXISTS level              int2;
ALTER TABLE dept_user.t_dept ADD COLUMN IF NOT EXISTS node_type        int2;
ALTER TABLE dept_user.t_dept ADD COLUMN IF NOT EXISTS direct_children    varchar[];
ALTER TABLE dept_user.t_dept ADD COLUMN IF NOT EXISTS recursive_children varchar[];

WITH RECURSIVE tree(node, lvl) AS (
    SELECT t.dept_code, 1::int2
    FROM dept_user.t_dept t
    WHERE t.parent_dept_code IS NULL
       OR t.parent_dept_code = ''
       OR NOT EXISTS (SELECT 1 FROM dept_user.t_dept p
                      WHERE p.dept_code = t.parent_dept_code)
  UNION ALL
    SELECT c.dept_code, (tree.lvl + 1)::int2
    FROM dept_user.t_dept c
    JOIN tree ON c.parent_dept_code = tree.node
    WHERE tree.lvl < 64
),
d AS (SELECT node, MIN(lvl) AS lvl FROM tree GROUP BY node)
UPDATE dept_user.t_dept u SET level = d.lvl
FROM d WHERE u.dept_code = d.node;

UPDATE dept_user.t_dept u SET node_type = CASE
    WHEN u.parent_dept_code IS NULL OR u.parent_dept_code = ''                        THEN 1
    WHEN NOT EXISTS (SELECT 1 FROM dept_user.t_dept p WHERE p.dept_code = u.parent_dept_code) THEN 1
    WHEN     EXISTS (SELECT 1 FROM dept_user.t_dept c WHERE c.parent_dept_code = u.dept_code) THEN 2
    ELSE 3
END;

-- 没有直系子节点 => 子查询零行 => array_agg 返回 NULL（不是 {}）
UPDATE dept_user.t_dept u SET direct_children = (
    SELECT array_agg(c.dept_code ORDER BY c.dept_code)
    FROM dept_user.t_dept c
    WHERE c.parent_dept_code IS NOT NULL
      AND c.parent_dept_code <> ''
      AND c.parent_dept_code = u.dept_code
);

-- 直系 + 所有递归子孙（不含自身）；没有后代 => NULL
-- 先整列置 NULL，避免删过子节点的行残留旧值
UPDATE dept_user.t_dept SET recursive_children = NULL;

WITH RECURSIVE down(root, node, lvl) AS (
    SELECT p.dept_code, c.dept_code, 1
    FROM dept_user.t_dept p
    JOIN dept_user.t_dept c ON c.parent_dept_code = p.dept_code
    WHERE c.parent_dept_code IS NOT NULL AND c.parent_dept_code <> ''
  UNION ALL
    SELECT d.root, c.dept_code, d.lvl + 1
    FROM down d
    JOIN dept_user.t_dept c ON c.parent_dept_code = d.node
    WHERE c.parent_dept_code IS NOT NULL AND c.parent_dept_code <> ''
      AND d.lvl < 64
),
agg AS (
    SELECT root, array_agg(DISTINCT node ORDER BY node) AS kids
    FROM down GROUP BY root
)
UPDATE dept_user.t_dept u SET recursive_children = agg.kids
FROM agg WHERE u.dept_code = agg.root;
*/


-- ============================================================================
--  若要做成可反复调用的函数（对齐 MySQL 侧的存储过程）
-- ============================================================================
/*
CREATE OR REPLACE FUNCTION tree_tag(p_schema text, p_table text, p_x text, p_px text)
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    v_tbl   text := format('%I.%I', p_schema, p_table);
    v_xtype text;
    v_ndims int;
    v_root int; v_mid int; v_leaf int; v_orphan int; v_total int;
    v_kids int; v_kidtotal int;
    v_rkids int; v_rkidtotal int;
BEGIN
    SELECT format_type(a.atttypid, NULL), a.attndims
      INTO v_xtype, v_ndims
      FROM pg_attribute a
     WHERE a.attrelid = v_tbl::regclass AND a.attname = p_x
       AND a.attnum > 0 AND NOT a.attisdropped;
    IF v_xtype IS NULL THEN
        RAISE EXCEPTION '表 % 上找不到列 %', v_tbl, p_x;
    END IF;
    IF v_ndims > 0 THEN
        RAISE EXCEPTION '列 %.% 本身已是数组类型(%)', p_schema, p_x, v_xtype;
    END IF;

    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS level int2', v_tbl);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS node_type int2', v_tbl);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS direct_children %s[]', v_tbl, v_xtype);
    EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS recursive_children %s[]', v_tbl, v_xtype);

    EXECUTE format($q$
        WITH RECURSIVE tree(node, lvl) AS (
            SELECT t.%1$I, 1::int2 FROM %2$s t
            WHERE t.%3$I IS NULL OR t.%3$I::text = ''
               OR NOT EXISTS (SELECT 1 FROM %2$s p WHERE p.%1$I = t.%3$I)
          UNION ALL
            SELECT c.%1$I, (tree.lvl + 1)::int2 FROM %2$s c
            JOIN tree ON c.%3$I = tree.node WHERE tree.lvl < 64
        ), d AS (SELECT node, MIN(lvl) AS lvl FROM tree GROUP BY node)
        UPDATE %2$s u SET level = d.lvl FROM d WHERE u.%1$I = d.node
    $q$, p_x, v_tbl, p_px);

    EXECUTE format($q$
        UPDATE %2$s u SET node_type = CASE
            WHEN u.%3$I IS NULL OR u.%3$I::text = ''                     THEN 1
            WHEN NOT EXISTS (SELECT 1 FROM %2$s p WHERE p.%1$I = u.%3$I) THEN 1
            WHEN     EXISTS (SELECT 1 FROM %2$s c WHERE c.%3$I = u.%1$I) THEN 2
            ELSE 3 END
    $q$, p_x, v_tbl, p_px);

    EXECUTE format($q$
        UPDATE %2$s u SET direct_children = (
            SELECT array_agg(c.%1$I ORDER BY c.%1$I)
            FROM %2$s c
            WHERE c.%3$I IS NOT NULL AND c.%3$I::text <> ''
              AND c.%3$I = u.%1$I
        )
    $q$, p_x, v_tbl, p_px);

    EXECUTE format('UPDATE %s SET recursive_children = NULL', v_tbl);

    EXECUTE format($q$
        WITH RECURSIVE down(root, node, lvl) AS (
            SELECT p.%1$I, c.%1$I, 1
            FROM %2$s p JOIN %2$s c ON c.%3$I = p.%1$I
            WHERE c.%3$I IS NOT NULL AND c.%3$I::text <> ''
          UNION ALL
            SELECT d.root, c.%1$I, d.lvl + 1
            FROM down d JOIN %2$s c ON c.%3$I = d.node
            WHERE c.%3$I IS NOT NULL AND c.%3$I::text <> ''
              AND d.lvl < 64
        ), agg AS (
            SELECT root, array_agg(DISTINCT node ORDER BY node) AS kids
            FROM down GROUP BY root
        )
        UPDATE %2$s u SET recursive_children = agg.kids
        FROM agg WHERE u.%1$I = agg.root
    $q$, p_x, v_tbl, p_px);

    EXECUTE format('SELECT count(*), count(*) FILTER (WHERE level IS NULL) FROM %s', v_tbl)
       INTO v_total, v_orphan;
    EXECUTE format('SELECT count(*) FILTER (WHERE node_type=1), count(*) FILTER (WHERE node_type=2),'
                || ' count(*) FILTER (WHERE node_type=3) FROM %s', v_tbl)
       INTO v_root, v_mid, v_leaf;
    EXECUTE format('SELECT count(*) FILTER (WHERE direct_children IS NOT NULL),'
                || ' coalesce(sum(cardinality(direct_children)), 0) FROM %s', v_tbl)
       INTO v_kids, v_kidtotal;
    EXECUTE format('SELECT count(*) FILTER (WHERE recursive_children IS NOT NULL),'
                || ' coalesce(sum(cardinality(recursive_children)), 0) FROM %s', v_tbl)
       INTO v_rkids, v_rkidtotal;
    RETURN format('总行数=%s  根=%s 中间=%s 叶=%s  直系行=%s 直系总=%s  后代行=%s 后代总=%s  未标层号=%s',
                  v_total, v_root, v_mid, v_leaf, v_kids, v_kidtotal, v_rkids, v_rkidtotal, v_orphan);
END $$;

-- 调用：
SELECT tree_tag('dept_user', 't_dept', 'dept_code', 'parent_dept_code');
*/
