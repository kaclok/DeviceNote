package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * cbs 角色（cbs.t_role）。perms 为权限点数组（PG varchar[]，ArrayTypeHandler 映射）；
 * data_scope 即该角色的数据范围档：1本人/2本部门(含下级)/3本公司/4全集团。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsRole implements Serializable {

    private String role_code;
    private String role_name;
    private String data_scope;
    private String[] perms;
    private Boolean is_using;
}
