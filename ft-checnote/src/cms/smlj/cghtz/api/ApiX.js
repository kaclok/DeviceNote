import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

/**
 * 合同台账 - 后端接口层
 *
 * 说明：后端接口尚未开发时，SysX 会走 MockX 本地兜底；
 * 后端就绪后，只需保持下面 URL 与后端路由一致即可无缝切换。
 */
export class ApiX {

    /* ---------------- 认证 ---------------- */
    static login(paras, signal) {
        return axiosR.post("cghtz/login", null, {
            params: paras,
            signal: signal,
        })
    }

    /* ---------------- 合同台账 CRUD ---------------- */
    static getContractList(paras, signal) {
        return axiosR.get("cghtz/contract/list", {
            params: paras,
            signal: signal,
        })
    }

    static getContract(paras, signal) {
        return axiosR.get("cghtz/contract/get", {
            params: paras,
            signal: signal,
        })
    }

    static createContract(paras, signal) {
        return axiosR.post("cghtz/contract/create", paras, {
            signal: signal,
        })
    }

    static updateContract(paras, signal) {
        return axiosR.post("cghtz/contract/update", paras, {
            signal: signal,
        })
    }

    static deleteContract(paras, signal) {
        return axiosR.post("cghtz/contract/delete", null, {
            params: paras,
            signal: signal,
        })
    }

    /* ---------------- Excel 导入 / 导出 ---------------- */
    static importContractExcel(paras, signal) {
        return axiosR.post("cghtz/contract/import", paras, {
            signal: signal,
        })
    }

    /* ---------------- 账号与权限 ---------------- */
    static getAccountList(paras, signal) {
        return axiosR.get("cghtz/account/list", {
            params: paras,
            signal: signal,
        })
    }

    static saveAccount(paras, signal) {
        return axiosR.post("cghtz/account/save", paras, {
            signal: signal,
        })
    }

    static resetPassword(paras, signal) {
        return axiosR.post("cghtz/account/resetPwd", null, {
            params: paras,
            signal: signal,
        })
    }

    static toggleAccountStatus(paras, signal) {
        return axiosR.post("cghtz/account/toggle", null, {
            params: paras,
            signal: signal,
        })
    }
}
