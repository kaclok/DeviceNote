package com.smlj.singledevice_note.logic.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * cbs 列表分页口径 —— 全项目唯一实现（CCbsBiz / CCbsUser 共用）。
 * <p>
 * 入参：pageNo 从 1 起；pageSize 由前端逐页配置下发，缺省 14、上限 200。
 * 出参：{rows, total, pageNo, pageSize}，前端 el-pagination 直接消费。
 * <p>
 * ⚠️ jackson NON_EMPTY：rows 为空集合时字段会整个消失，前端必须按「缺失即空」处理。
 */
public final class CbsPage {
    public static final int DEFAULT_PAGE_SIZE = 14;
    public static final int MAX_PAGE_SIZE = 200;

    private CbsPage() {
    }

    public static int pageNo(Map<String, Object> q) {
        return pageNo(intVal(q, "pageNo"));
    }

    public static int pageNo(Integer v) {
        return v == null || v < 1 ? 1 : v;
    }

    public static int pageSize(Map<String, Object> q) {
        return pageSize(intVal(q, "pageSize"));
    }

    public static int pageSize(Integer v) {
        if (v == null || v < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(v, MAX_PAGE_SIZE);
    }

    public static int offset(int pageNo, int pageSize) {
        return (pageNo - 1) * pageSize;
    }

    public static <T> Map<String, Object> pageOf(List<T> rows, long total, int pageNo, int pageSize) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", rows);
        m.put("total", total);
        m.put("pageNo", pageNo);
        m.put("pageSize", pageSize);
        return m;
    }

    /** Map 取 Integer：兼容前端传数字或数字字符串 */
    private static Integer intVal(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }
}
