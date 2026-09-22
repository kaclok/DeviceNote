package com.smlj.singledevice_note.logic.o.vo.table.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同表 DAO。
 * <p>
 * 台账（list/get/create/update/delete）与**批量导入**都已**按物理表路由**：合同落在哪张表
 * 由部门绑定的合同模版决定（t_contract_template.tb_name），不同物理表的业务列并不相同
 * （t_contract 25 列 / t_contract_smds_sc 17 列），所以路由后的结果一律用 Map 承载 ——
 * 一张 SQL 覆盖所有已登记的模版，换表的差异全部落在配置里。
 * <p>
 * 本接口不再有写死 cght.t_contract 的语句：导入的写入同样走 insertRow(tb, pairs)。
 */
@Mapper
@Repository
public interface TCGHTContractDao {

    /* ================= 按物理表路由的台账读写 =================
     *
     * ⚠️ 表名 ${tb} 与列名 ${p.col} / ${c.col} 是**拼接**进 SQL 的标识符，绝不能直接来自请求：
     *    · tb   —— CCGHT 用 t_contract_template 已登记的 tb_name 做白名单核对后回传规范表名；
     *    · col  —— CCGHT 用 ^[a-z_][a-z0-9_]{0,62}$ 核对，并确认它真实存在于该表、且不在只读黑名单
     *              （unique_id 主键 / open_status 逻辑删除标记 / creator 录入人审计字段）。
     * 值一律 #{...} 预编译绑定，从不参与拼接。
     */

    /**
     * 物理表的列清单（name / type，来自 PG information_schema）。
     * 用途：① 核对请求下推的列名确实属于这张表（否则拼出的 SQL 必然报错）；
     *      ② 判断比较符与列类型是否搭配（like 只能用于字符列）；
     *      ③ 推导该表支不支持「预警天数」口径（需要 date_rk + paycycle_dh/paycycle_zb）。
     * 表名在这里是 #{tb} 绑定的**值**，不参与拼接。
     */
    List<LinkedHashMap<String, Object>> columnMeta(@Param("tb") String tb);

    /**
     * 列表查询：通用筛选 + 数据范围下推 + 预警天数。
     *
     * @param conds     已核对的筛选条件 [{col, op, v}]；op ∈ like|eq|gte|lte|ne。空 = 不筛
     * @param deptCodes 最终可见部门集三态（null 不限 / 空 = 查不到 / 非空 = in 这些部门），由
     *                  CCGHT.deptFilterOf 合并「选中部门的整棵子树」与「数据范围白名单」后传入
     * @param user_name 「本人」档：sign_person = 我（空 = 该档不叠加此条件）
     * @param warn_day  预警天数阈值（只有含 date_rk + paycycle_dh/zb 的表才允许传非空）
     */
    List<LinkedHashMap<String, Object>> queryRows(
            @Param("tb") String tb
            , @Param("conds") List<Map<String, Object>> conds
            , @Param("deptCodes") List<String> deptCodes
            , @Param("user_name") String user_name
            , @Param("warn_day") Integer warn_day);

    /** 按 unique_id 取单行（列随表变，同样用 Map 承载） */
    LinkedHashMap<String, Object> queryRow(@Param("tb") String tb, @Param("unique_id") String unique_id);

    /** 编号是否已存在（未作废）。即时结算类合同的唯一性校验用（手录与导入同用这一个） */
    int existId(@Param("tb") String tb, @Param("id") String id);

    /** 新增：pairs = [{col, val}]，列名已核对；未提供的列走库默认值 */
    int insertRow(@Param("tb") String tb, @Param("pairs") List<Map<String, Object>> pairs);

    /**
     * 按 unique_id 局部更新：只写 pairs 里出现的列，未出现的列保持原值。
     * 编辑页提交的是该模板的全部列，所以等价于整行覆盖；但这里刻意不做"未提交即清空"。
     */
    int updateRow(@Param("tb") String tb, @Param("pairs") List<Map<String, Object>> pairs
            , @Param("unique_id") String unique_id);

    /** 逻辑作废（open_status = false），不物理删除 */
    int markInvalidRow(@Param("tb") String tb, @Param("unique_id") String unique_id);
}
