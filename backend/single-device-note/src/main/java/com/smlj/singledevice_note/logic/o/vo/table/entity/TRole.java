package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * dept_user.t_role —— dept_user 模块的角色表（与 cght 库的 t_role 相互独立，勿混）。
 * <p>
 * 与 cght.t_role 的差异：主键是自由 id（varchar，不再是自增/固定码），
 * data_scope 直接挂在角色上（varchar，'1'~'4'，由 {@link com.smlj.singledevice_note.core.o.to.DataScope#of(String)}
 * 解析，空串 / 非数字 / 未知编号一律 NONE → fail-closed）。
 * 账号通过 t_user.roles[]（varchar 数组）挂多个角色，有效档位取全部启用角色的最大档。
 */
@Component
@Data
@NoArgsConstructor
public class TRole implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    private String name;
    /**
     * 数据范围档位：1 本人 / 2 本部门 / 3 本公司
     */
    private Integer data_scope;
    /** 启用状态：true 启用、false 停用（停用角色不参与档位与权限计算） */
    private Boolean is_using;
    private String[] perms;
}
