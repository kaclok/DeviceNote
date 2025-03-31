import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"
import {ApiX} from "@/cms/daily_paper/api/ApiX.js";

async function innerDoGet(url, paras, signal) {
    return axiosR.get(url, {
        params: paras,
        signal: signal,
    })
}

async function innerDoPost(url, paras, signal) {
    return axiosR.post(url, null, {
        params: paras,
        signal: signal,
    })
}

async function doGet(paras, signal, onBefore, onAfter) {
    onBefore?.();
    innerDoGet(paras, signal).then(succ => {
        onAfter?.(true, succ.data);
    }).catch(fail => {
        onAfter?.(false, fail);
    });
}

async function doPost(paras, signal, onBefore, onAfter) {
    onBefore?.();
    innerDoPost(paras, signal).then(succ => {
        onAfter?.(true, succ.data);
    }).catch(fail => {
        onAfter?.(false, fail);
    });
}

export {
    innerDoGet,
    innerDoPost,

    doGet,
    doPost,
}