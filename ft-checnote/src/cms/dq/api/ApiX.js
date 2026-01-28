import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

export class ApiX {
    static getList(paras, signal) {
        return axiosR.get("x/getList", {
            params: paras,
            signal: signal,
        })
    }

    static add(paras, signal) {
        return axiosR.get("x/add", {
            params: paras,
            signal: signal,
        })
    }

    static export(paras, signal) {
        return axiosR.get("x/export", {
            params: paras,
            signal: signal,
        })
    }

    static query(paras, signal) {
        return axiosR.get("x/find", {
            params: paras,
            signal: signal,
        })
    }
}
