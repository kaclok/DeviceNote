package com.smlj.singledevice_note.logic.o.vo.table;

import java.util.Map;

/**
 * 合同物理表的「画像」—— 一张表支持哪些**业务口径**，由列族判据推导，规则集中在这一处。
 * <p>
 * 为什么按"列族"而不是按表名注册：t_contract_template 由运维手工登记，新增一张合同物理表
 * 是**零代码**动作（建表 + 登记一行 + gd.json 配置）。画像若按表名写死，每加一张表都要改这里
 * 并发版；按列族推导则新表天然继承同款能力 —— 判据本身就是登记项。
 * <p>
 * 消费方（CCGHT）把散在各处的"这张表支不支持 X"的 if 全部收拢到这里：
 * <ul>
 *   <li>编号唯一性：{@link #idMustBeUnique}（新增 / 导入两条写链路共用）；</li>
 *   <li>「本人」档过滤：{@link #selfFilterSupported()}（列表按签订人姓名过滤的列是否存在）；</li>
 *   <li>预警天数口径：{@link #warnSupported()}（挂账日期 + 两个付款周期 + 完成环节）。</li>
 * </ul>
 * 列清单来自 colsOf(tb)（PG information_schema），因此画像永远反映**库内真实结构**，
 * 不存在"配置说有、库里没有"的漂移；缺能力的口径一律 fail-closed（可读报错），绝不静默降级
 * —— 静默降级在权限语境下等于越权（比如"本人档"不过滤 = 看到所有人的合同）。
 * <p>
 * 无状态纯判据：运行时可被探针直接反射调用，不必先造一个 Spring bean。
 */
public final class ContractTableProfile {

    /** 编号唯一性的「类别判据」列：表里有此列 = 唯一性按取值分档（见 {@link #UNIQUE_KIND_INSTANT}） */
    public static final String UNIQUE_KIND_COL = "payment_type";

    /** {@link #UNIQUE_KIND_COL} 里表示「即时结算类」的取值 —— 只有这一档要求编号唯一 */
    public static final int UNIQUE_KIND_INSTANT = 1;

    /** 「本人」档（data_scope=1）按姓名过滤的列（签订人姓名）；表没有该列则本人档不可用 */
    public static final String SELF_FILTER_COL = "sign_person";

    /** 「预警天数」口径需要的列族：挂账日期 + 两个付款周期 + 完成环节 */
    private static final String[] WARN_COLS = {"date_rk", "paycycle_dh", "paycycle_zb", "finish_step"};

    private final Map<String, String> cols;

    private ContractTableProfile(Map<String, String> cols) {
        this.cols = cols == null ? Map.of() : cols;
    }

    /** 由某表的列清单（列名 → 库内类型）推导画像 */
    public static ContractTableProfile of(Map<String, String> cols) {
        return new ContractTableProfile(cols);
    }

    /**
     * 合同编号(id)是否必须唯一 —— 「新增」与「导入」共用这**一处**判据，两条写链路不会各拦一半。
     * 口径按**表**分档，不按调用方分：
     * <ul>
     *   <li>带 {@link #UNIQUE_KIND_COL} 列（标准采购合同表）：只有「即时结算类(1)」要求唯一，
     *       周期结算类(2) 允许同号多次；</li>
     *   <li>不带该列（如 smds_sc）：没有"周期结算"这个维度，编号一律唯一。</li>
     * </ul>
     * ⚠️ 唯一性是**表内**口径（contractDao.existId 的 where 只查本表），不是跨表全局唯一。
     */
    public boolean idMustBeUnique(Map<String, Object> data) {
        if (!cols.containsKey(UNIQUE_KIND_COL)) {
            return true;
        }
        return Integer.valueOf(UNIQUE_KIND_INSTANT).equals(asInt(data == null ? null : data.get(UNIQUE_KIND_COL)));
    }

    /** 该表能否支持「本人」档按签订人过滤 */
    public boolean selfFilterSupported() {
        return cols.containsKey(SELF_FILTER_COL);
    }

    /** 该表支不支持「预警天数」口径：WARN_COLS 列族齐全才支持（XML 里的预警 SQL 要引用它们） */
    public boolean warnSupported() {
        for (String c : WARN_COLS) {
            if (!cols.containsKey(c)) {
                return false;
            }
        }
        return true;
    }

    private static Integer asInt(Object v) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
