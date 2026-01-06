WITH time_range AS (
    -- 定义查询的时间范围（作为参数传入）
    SELECT 
        '2026-01-06 00:00:00'::timestamp AS begin_time,
        '2026-01-07 00:00:00'::timestamp AS end_time
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
        GREATEST(l.begintime, tr.begin_time) + ((gs.cycle_index + 1) * (l.cycle || ' hours')::interval) - interval '1 second'AS cycle_end_time
    FROM nfcpatrol.t_nfcpatrol_line l
    CROSS JOIN time_range tr
    CROSS JOIN LATERAL (
        SELECT generate_series(
            0,
            FLOOR(EXTRACT(EPOCH FROM (tr.end_time - GREATEST(l.begintime, tr.begin_time))) / (l.cycle * 3600))::int
        ) AS cycle_index
    ) gs
    WHERE l.deleted = false AND ARRAY_LENGTH(l.pointids, 1) != 0 AND l.begintime <= tr.end_time
),

-- 已解决问题：一个巡检周期内某个点可能被巡检多次
cycle_patrol_stats AS (
    SELECT 
        pc.line_id,
        pc.linename,
        pc.linenum,
        pc.deptid,
        pc.cycle_index,
        pc.cycle_start_time,
        pc.cycle_end_time,
        pc.total_points,
        -- 统计该周期内已巡检的点位数
        COUNT(DISTINCT last_records.rfid) AS checked_points,
        -- 统计该周期内的异常点位数量（每个点位只取最后一次的errornum）
        COALESCE(SUM(last_records.errornum), 0) AS total_errors,
        -- 巡检人员（多人用逗号分隔）
        STRING_AGG(DISTINCT last_records.person, ', ') AS patrol_persons
    FROM patrol_cycles pc
    LEFT JOIN LATERAL (
        -- 对每个点位，只取该周期内最后一次的打卡记录
        SELECT DISTINCT ON (r.rfid)
            r.rfid,
            r.person,
            r.errornum,
            r.dotime
        FROM nfcpatrol.t_nfcpatrol_record r
        WHERE r.rfid = ANY(pc.pointids)
            AND r.dotime BETWEEN pc.cycle_start_time AND pc.cycle_end_time
        ORDER BY r.rfid, r.dotime DESC  -- 每个点位按时间倒序，取最新的
    ) last_records ON true
    GROUP BY 
        pc.line_id, 
        pc.linename, 
        pc.linenum, 
        pc.deptid, 
        pc.cycle_index, 
        pc.cycle_start_time, 
        pc.cycle_end_time,
        pc.total_points
),

final_result AS (
    -- 最终结果，关联部门信息
    SELECT 
        cps.line_id,
        cps.linename,
        COALESCE(d.name, '/') AS deptname,
        cps.cycle_start_time,
        COALESCE(cps.patrol_persons, '/') AS patrol_person,
        cps.checked_points AS checked_cnt,
        cps.total_points AS total_cnt,
        cps.total_errors AS error_count,
        CASE
        /* A 前提：当前时间在周期内 */
        WHEN CURRENT_TIMESTAMP BETWEEN cps.cycle_start_time AND cps.cycle_end_time THEN
            CASE
                WHEN cps.checked_points = 0 THEN 1
                WHEN cps.checked_points <> 0 AND cps.checked_points < cps.total_points THEN 2
                WHEN cps.checked_points >= cps.total_points THEN 3
            END

        /* B 前提：周期已结束 */
        WHEN cps.cycle_end_time < CURRENT_TIMESTAMP THEN
            CASE
                WHEN cps.checked_points = 0 THEN 4
                WHEN cps.checked_points <> 0
                     AND cps.checked_points < cps.total_points THEN 5
                WHEN cps.checked_points >= cps.total_points THEN 3
            END

        ELSE 6
    END AS status
    FROM cycle_patrol_stats cps
    LEFT JOIN nfcpatrol.t_nfcpatrol_dept d 
        ON d.id = cps.deptid
)

-- 获取所有路线在查询区域内的计划
select * from final_result;