import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

export class ApiX {
    static getGYListByZZ(paras, signal) {
        return axiosR.get("x/getGYListByZZ", {
            params: paras,
            signal: signal,
        })
    }

    static getDeviceList(paras, signal) {
        return axiosR.get("x/getDeviceList", {
            params: paras,
            signal: signal,
        })
    }

    static getRecordList(paras, signal) {
        return axiosR.get("x/getRecordList", {
            params: paras,
            signal: signal,
        })
    }

    static getRecord(paras, signal) {
        return axiosR.get("x/getRecord", {
            params: paras,
            signal: signal,
        })
    }

    static report(paras, signal) {
        return axiosR.get("x/report", {
            params: paras,
            signal: signal,
        })
    }

    static login(paras, signal) {
        return axiosR.post("x/login", null, {
            params: paras,
            signal: signal,
        })
    }
}
