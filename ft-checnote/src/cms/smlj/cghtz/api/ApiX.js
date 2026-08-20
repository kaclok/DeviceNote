import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

/**
 * 合同台账 - 后端接口层
 *
 * 说明：后端接口尚未开发时，SysX 会走 MockX 本地兜底；
 * 后端就绪后，只需保持下面 URL 与后端路由一致即可无缝切换。
 *
 * 注意：axios.post(url, data, config)。
 * - 需要 query 参数(@RequestParam)的接口：第二参传 null，第三参用 {params, signal}，
 *   这样 pageNum/pageSize/筛选条件才会作为查询串发到后端，被 @RequestParam 读到。
 *   之前写成 post(url, {params, signal}) 会把 params 当成 JSON body 发出，后端 @RequestParam 读不到，分页参数永远丢失。
 * - 需要 body(@RequestBody)的接口：第二参传 paras，第三参用 {signal}。
 */
export class ApiX {
    /* ---------------- 合同台账 CRUD ---------------- */
    static getContractList(paras, signal) {
        return axiosR.post("cghtz/contract/list", null, {
            params: paras, signal: signal,
        })
    }

    static getContract(paras, signal) {
        return axiosR.post("cghtz/contract/get", null, {
            params: paras, signal: signal,
        })
    }

    static createContract(paras, signal) {
        return axiosR.post("cghtz/contract/create", null, {
            params: paras, signal: signal,
        })
    }

    static updateContract(paras, signal) {
        return axiosR.post("cghtz/contract/update", null, {
            params: paras, signal: signal,
        })
    }

    static deleteContract(paras, signal) {
        return axiosR.post("cghtz/contract/delete", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- Excel 导入 / 导出 ---------------- */
    static importContractExcel(paras, signal) {
        return axiosR.post("cghtz/contract/import", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 账号与权限 ---------------- */
    static getAccountList(paras, signal) {
        return axiosR.post("cghtz/account/list", null, {
            params: paras, signal: signal,
        })
    }

    static saveAccount(paras, signal) {
        return axiosR.post("cghtz/account/save", null, {
            params: paras, signal: signal,
        })
    }

    static resetPassword(paras, signal) {
        return axiosR.post("cghtz/account/resetPwd", null, {
            params: paras, signal: signal,
        })
    }

    static toggleAccountStatus(paras, signal) {
        return axiosR.post("cghtz/account/toggle", null, {
            params: paras, signal: signal,
        })
    }

    /* ---------------- 角色与权限字典 ---------------- */
    static getRoleList(paras, signal) {
        return axiosR.post("cghtz/role/list", null, {
            params: paras, signal: signal,
        })
    }

    static getPermDefs(paras, signal) {
        return axiosR.post("cghtz/perm/list", null, {
            params: paras, signal: signal,
        })
    }

    static getSignerList(paras, signal) {
        return axiosR.post("cghtz/signer/list", null, {
            params: paras, signal: signal,
        })
    }
}
