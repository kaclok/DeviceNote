import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

/**
 * cbs 组织管理 - 后端接口层（对接 CCbsUser）
 *
 * 注意：axios.post(url, data, config)。
 * - 需要 query 参数(@RequestParam)的接口：第二参传 null，第三参用 {params, signal}
 * - 需要 body(@RequestBody)的接口：第二参传 paras，第三参用 {signal}
 */
export class ApiX {
    /* ---------------- 部门 ---------------- */
    static getDeptList(signal) {
        return axiosR.post("cbs/dept/list", null, {signal})
    }

    static saveDept(paras, signal) {
        return axiosR.post("cbs/dept/save", paras, {signal})
    }

    static deleteDept(paras, signal) {
        return axiosR.post("cbs/dept/delete", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 角色 ---------------- */
    static getRoleList(signal) {
        return axiosR.post("cbs/role/list", null, {signal})
    }

    static saveRole(paras, signal) {
        return axiosR.post("cbs/role/save", paras, {signal})
    }

    static deleteRole(paras, signal) {
        return axiosR.post("cbs/role/delete", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 人员 ---------------- */
    static getUserList(paras, signal) {
        return axiosR.post("cbs/user/list", null, {
            params: paras, signal: signal,
        })
    }

    static saveUser(paras, signal) {
        return axiosR.post("cbs/user/save", paras, {signal})
    }

    static resetUserPwd(paras, signal) {
        return axiosR.post("cbs/user/resetPwd", null, {
            params: paras, signal: signal,
        })
    }

    static toggleUser(paras, signal) {
        return axiosR.post("cbs/user/toggle", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 组织树辅助列回写 ---------------- */
    static warpOrgs(signal) {
        return axiosR.post("cbs/warpOrgs", null, {signal})
    }

    /* ---------------- 业务模块（承包商/项目/安全承诺/基础信息） ----------------
     * 后端 CCbsBiz 约定：列表筛选与删除参数走 JSON body，与组织管理走 query 的旧风格不同。
     */
    static biz(url, paras, signal) {
        return axiosR.post(url, paras || {}, {signal})
    }
}
