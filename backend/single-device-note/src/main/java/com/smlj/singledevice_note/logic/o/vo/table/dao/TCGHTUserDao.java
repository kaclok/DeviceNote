package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Mapper
@Repository
public interface TCGHTUserDao {
    /**
     * 账号列表（服务端搜索 + 分页，由调用方 PageHelper.startPage 控制分页）：
     *
     * @param kw               关键字：同时模糊匹配 account / username；为空表示不过滤
     * @param dept_code        归属部门精确匹配；为空表示不过滤
     * @param includeAdmin     false=排除超级管理员，供签订人下拉使用；true=包含全部，账号管理/登录使用
     * @param filterOpenStatus true=只返回启用账号
     * @param scopeDepts       数据范围白名单（由 CCGHT.resolveContractScope 解析后下推），三态与合同一致：
     *                         null = 不限制（全集团）；空列表 = 什么都不返回（fail-closed）；非空 = 仅这些部门
     * @param scopeOwner       「本人」档（data_scope=1）的归属账号，对应合同侧的 creator 维度：
     *                         非空 = 只返回这个账号自己（1 本人）；null / 空 = 不叠加该条件（2/3/4 档）
     */
    ArrayList<TCGHTUser> queryAll(@Param("kw") String kw,
                                 @Param("dept_code") String dept_code,
                                 @Param("includeAdmin") boolean includeAdmin,
                                 @Param("filterOpenStatus") boolean filterOpenStatus,
                                 @Param("scopeDepts") List<String> scopeDepts,
                                 @Param("scopeOwner") String scopeOwner);

    TCGHTUser query(@Param("account") String account);

    int exist(@Param("account") String account);

    /** 新增账号：account/username/pwd/role_code/dept_code/open_status 必填；data_scope 由 updateScope 紧接着写入 */
    int insert(@Param("u") TCGHTUser u);

    /** 更新账号：username/role_code/dept_code 等业务字段（密码另走 resetPwd，状态另走 toggleStatus） */
    int update(@Param("u") TCGHTUser u);

    /**
     * 设置账号的数据范围（整体赋值）：data_scope 必传（列 NOT NULL、新建必填）。
     * 刻意与 update 分开：update 只改业务字段，范围有独立的写入路径。
     * 调用方必须先校验目标范围 ⊆ 操作者自己的可见范围（CCGHT.accountSave 第 (5) 道闸）。
     */
    int updateScope(@Param("account") String account,
                    @Param("data_scope") Integer data_scope);

    /** 重置密码：将指定账号的 pwd 重置为新值 */
    int resetPwd(@Param("account") String account, @Param("pwd") String pwd);

    /** 启停状态切换：将 open_status 置为目标值 */
    int toggleStatus(@Param("account") String account, @Param("open_status") boolean open_status);
}
