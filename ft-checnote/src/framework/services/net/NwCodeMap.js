// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
import {axiosInst} from "@/framework/services/net/AxiosInst.js";
import {TokenService} from "@/framework/services/TokenService.js";

const NwCodeMap = {
    [__RT_EXPIRE_CODE__]: async (resp) => {
        // rt超时，跳转到登录页面
    },
    [__AT_EXPIRE_CODE__]: async (resp) => {
        if (!TokenService.isRT(resp.config)) {
            // 上次失败的请求
            let originalRequest = resp.config
            TokenService.getRemoteAT().then(res => {
                originalRequest.headers.at = TokenService.getAT()
                axiosInst.request(originalRequest)
            }).catch(err => {
                // 获取at失败
                // 跳转到登录页
            })
        }
    },
    [__HEART_BEAT_CODE__]: (resp) => {

    },
}

export {
    NwCodeMap,
}