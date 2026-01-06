WITH time_range AS (
    -- 定义查询的时间范围（作为参数传入）
    SELECT 
        '2026-01-06 00:00:00'::timestamp AS begin_time,
        '2026-01-06 23:59:59'::timestamp AS end_time
),
patrol_cycles AS (
    -- 为每条巡检路线生成所有应该巡检的周期
    SELECT 
        l.id AS line_id,
        l.linename,
        l.linenum,
        l.deptid,
        l.pointids,
        l.begintime,
        l.cycle,
        ARRAY_LENGTH(l.pointids, 1) AS total_points,
        gs.cycle_index AS cycle_index,  -- 引用子查询的结果
        -- 计算每个周期的开始时间
        GREATEST(l.begintime, tr.begin_time) + (gs.cycle_index * (l.cycle || ' hours')::interval) AS cycle_start_time,
        -- 计算每个周期的结束时间
        GREATEST(l.begintime, tr.begin_time) + ((gs.cycle_index + 1) * (l.cycle || ' hours')::interval) - interval '1 second' AS cycle_end_time
    FROM nfcpatrol.t_nfcpatrol_line l
    CROSS JOIN time_range tr
    CROSS JOIN LATERAL (
        SELECT generate_series(
            0,
            FLOOR(EXTRACT(EPOCH FROM (tr.end_time - GREATEST(l.begintime, tr.begin_time))) / (l.cycle * 3600))::int
        ) AS cycle_index
    ) gs
    WHERE l.deleted = false AND ARRAY_LENGTH(l.pointids, 1) != 0 AND l.begintime <= tr.end_time
)

-- 获取所有路线在查询区域内的计划
select * from patrol_cycles;