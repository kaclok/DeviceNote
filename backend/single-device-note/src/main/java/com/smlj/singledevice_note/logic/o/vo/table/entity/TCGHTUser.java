package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TCGHTUser implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String account;
    /** 中文名/姓名；合同的 sign_person 直接存该姓名（自由文本，模糊匹配） */
    private String username;
    /** 登录密码（明文存储，仅限内网 demo） */
    @JsonIgnore
    private String pwd;
    private String role_code;
    /**
     * 归属部门 Code，关联 train.t_org.dept_code（必填）。
     * 既是人事归属（合同的「归属部门」列、导入默认值），也是数据范围的展开起点；
     * 前端据此把编码回显成「公司/部门」名称。
     */
    private String dept_code;
    /**
     * 启用状态：true 启用、false 停用
     * 对应 hd.json.status(1/0)，后端/前端一致用布尔，序列化时为 true/false
     */
    private boolean open_status;
    /**
     * 数据范围（data_scope）—— 范围的唯一来源，库中 NOT NULL（新建账号必填）：
     * 1 本人 / 2 本部门（含下级） / 3 本公司 / 4 全集团。
     * <p>
     * 编号即**严格**包含序（部门子树 ⊂ 公司子树 ⊂ 全集），所以没有单独的「本部门及下级」档。
     * <p>
     * 为什么放在账号上而不是只放角色上：范围是「这个人在组织里的位置」的函数，
     * 挂在角色上时「全集团 VIEWER」「分公司管理员」这类组合必须为每档范围复制一份角色行
     * （连带复制整份 perms，改一次权限要改 N 行）。放账号上后，同一份角色模板可以任意配范围。
     * <p>
     * 保留包装类型 Integer 是为了让"取不到值"可识别：取不到时 dataScopeOf 返回 0，
     * 不在 SCOPE_VALID 内 → fail-closed。写入侧已设 NOT NULL，正常路径不会有 null。
     */
    private Integer data_scope;
    /** 角色对象：账号列表/登录响应都会把 role_code 关联查询出来并填充 */
    private TCGHTRole role;
}

