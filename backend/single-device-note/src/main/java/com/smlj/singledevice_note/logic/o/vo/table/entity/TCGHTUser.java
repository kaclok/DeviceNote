package com.smlj.singledevice_note.logic.o.vo.table.entity;

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
    /** 中文名/姓名：供签订人列表展示(username)；保持与前端 signerOptions({account,username}) 一致 */
    private String username;
    /** 登录密码（明文存储，仅限内网 demo） */
    private String pwd;
    private String role_code;
    /**
     * 启用状态：true 启用、false 停用
     * 对应 hd.json.status(1/0)，后端/前端一致用布尔，序列化时为 true/false
     */
    private boolean open_status;
    /** 角色对象：账号列表/登录响应都会把 role_code 关联查询出来并填充 */
    private TCGHTRole role;
}

