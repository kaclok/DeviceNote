package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * cbs 独立账号（cbs.t_user，PK=account）。与 cght.t_user 相互独立勿混。
 * 生效数据范围不在账号行上：账号 role_code -> cbs.t_role.data_scope（与 dept_user 模型同款）。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsUser implements Serializable {

    private String account;
    private String username;
    @JsonIgnore
    private String pwd;
    /** -> cbs.t_role.role_code；生效数据范围 = 角色 data_scope */
    private String role_code;
    private String dept_code;
    /** 1 我司内部员工 / 2 承包商人员（=2 时 contractor_id 必填） */
    private Integer user_type;
    /** 承包商人员关联单位（cbs.t_contractor.contractor_id），内部员工为 null */
    private String contractor_id;
    private Boolean is_using;
    /** 角色对象：列表/登录响应会关联查询填充 */
    private TCbsRole role;
}
