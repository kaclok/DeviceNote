package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TDeptUser implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    private String account;
    private String name;
    private String dept_code;
    /** 启用状态：true 启用、false 停用（停用账号在范围解析里一律 fail-closed） */
    private Boolean is_using;
    /**
     * 挂载的角色 id 列表（PG varchar[]），角色行在 dept_user.t_role（data_scope 挂在角色上）。
     * 映射走 DeptUserResultMap 的 ArrayTypeHandler，不能用 resultType 自动映射。
     */
    private String[] roles;
}

