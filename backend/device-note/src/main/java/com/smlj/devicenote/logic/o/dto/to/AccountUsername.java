package com.smlj.devicenote.logic.o.dto.to;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUsername {
    public static final String TOKEN = "token";

    // 个人信息
    public static final String UID = "uid";
    public static final String ACCOUNT = "account";
    public static final String NAME = "name";

    public static final String DEPT_CODE = "deptCode";
    public static final String DEPT_NAME = "deptName";

    public static final String ORG_CODE = "orgCode";
    public static final String ORG_NAME = "orgName";
    //public static final String ORG_FULL_NAME = "orgFullName";

    public static final String MOBILE = "mobile";
    public static final String JOB = "job";

    @Parameter(hidden = true)
    private String uid;
    @Parameter(hidden = true)
    private String account;
    @Parameter(hidden = true)
    private String name;

    @Parameter(hidden = true)
    private String deptCode;
    @Parameter(hidden = true)
    private String deptName;

    @Parameter(hidden = true)
    private String orgCode;
    @Parameter(hidden = true)
    private String orgName;

    //private String orgFullName;
    //private String mobile;

    public AccountUsername(Map<String, Object> map) {
        this((String) map.get(AccountUsername.UID),
                (String) map.get(AccountUsername.ACCOUNT)
                , (String) map.get(AccountUsername.NAME)
                , (String) map.get(AccountUsername.DEPT_CODE)
                , (String) map.get(AccountUsername.DEPT_NAME)
                , (String) map.get(AccountUsername.ORG_CODE)
                , (String) map.get(AccountUsername.ORG_NAME)
                //, (String) map.get(AccountUsername.ORG_FULL_NAME)
                //, (String) map.get(AccountUsername.MOBILE)
        );
    }
}
