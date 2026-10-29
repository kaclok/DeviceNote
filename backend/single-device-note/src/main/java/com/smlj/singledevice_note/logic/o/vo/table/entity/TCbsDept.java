package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * cbs 部门（cbs.t_dept）。树辅助列口径同 backend/db/tree-tag.postgres.sql：
 * level 根=1；node_type 1根/2中间/3叶；数组列空=NULL（不落 '{}'）。
 * is_company 为人工维护的公司节点标记（COMPANY 档定位用），warpOrgs 不回写它。
 */
@Component
@Data
@NoArgsConstructor
public class TCbsDept implements Serializable {

    private String dept_code;
    private String dept_name;
    private String parent_dept_code;
    private Boolean is_company;
    private Integer level;
    private Integer node_type;
    private String[] direct_children;
    private String[] recursive_children;
    private Boolean is_using;
}
