package com.smlj.singledevice_note.logic.util;

/**
 * 承包商审批状态（cbs.t_contractor.approve_status）。
 * 与前端 gd.json#approveStatus、承包商单位管理页「审批通过 / 待审批 / 未通过」三个切页一一对应。
 * 新增状态时先改本枚举，再同步 gd.json 与切页配置，不要在各处硬编码数字。
 */
public enum CbsApproveStatus {

    /** 待审批（新建承包商的默认状态） */
    PENDING(0, "待审批"),
    /** 审批通过（可进入承包商审核、可被项目选用） */
    PASSED(1, "审批通过"),
    /** 未通过（必须记录原因） */
    REJECTED(2, "未通过");

    private final int code;
    private final String label;

    CbsApproveStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /** 仅未通过需要原因；流转到其他状态时原因一律清空 */
    public boolean needReason() {
        return this == REJECTED;
    }

    /** 按库内编码解析，非法编码返回 null（调用方 fail-closed 拒绝） */
    public static CbsApproveStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CbsApproveStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
