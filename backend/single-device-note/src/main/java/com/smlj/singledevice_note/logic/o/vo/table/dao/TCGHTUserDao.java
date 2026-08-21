package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCGHTUserDao {
    /**
     * 账号列表：
     * @param includeAdmin false=排除超级管理员，供签订人下拉使用；true=包含全部，账号管理/登录使用
     */
    ArrayList<TCGHTUser> queryAll(boolean includeAdmin, boolean filterOpenStatus);

    TCGHTUser query(@Param("account") String account);

    int exist(@Param("account") String account);

    /** 新增账号：account/username/pwd/role_code/open_status 必填；lastLogin 可选 */
    int insert(@Param("u") TCGHTUser u);

    /** 更新账号：username/role_code 等业务字段（密码另走 resetPwd，状态另走 toggleStatus） */
    int update(@Param("u") TCGHTUser u);

    /** 重置密码：将指定账号的 pwd 重置为新值 */
    int resetPwd(@Param("account") String account, @Param("pwd") String pwd);

    /** 启停状态切换：将 open_status 置为目标值 */
    int toggleStatus(@Param("account") String account, @Param("open_status") boolean open_status);
}
