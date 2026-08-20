import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

export class ApiLogin {
    /* ---------------- 认证 ---------------- */
    static login(paras, signal) {
        return axiosR.post("cghtz/account/login", null, {
            params: paras, signal: signal,
        })
    }

    static logout(paras, signal) {
        return axiosR.post("cghtz/account/logout", null, {
            params: paras, signal: signal,
        })
    }
}
