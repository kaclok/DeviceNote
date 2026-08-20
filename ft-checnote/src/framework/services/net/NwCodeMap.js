// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
import {useRouter} from 'vue-router';
import {axiosInst} from "@/framework/services/net/AxiosInst.js";
import {TokenService} from "@/framework/services/TokenService.js";
import {ApiLogin} from "@/cms/smlj/cghtz/api/ApiLogin.js";
import {ECacheType, useCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

// 获取路由实例
const router = useRouter();

const NwCodeMap = {
    [__RT_EXPIRE_CODE__]: async (resp) => {
        // rt过期，登出 并且 跳转到登录页面
        const currentAccount = wsCache.get(ECacheType.ACCOUNT).account
        ApiLogin.logout(currentAccount);
        await router.push({name: 'login'})
    },
    [__AT_EMPTY__]: async (resp) => {
        // 未登录，登出 并且 跳转到登录页面
        await router.push({name: 'login'})
    },
    [__AT_EXPIRE_INVALID__]: async (resp) => {
        // at被篡改，登出 并且 跳转到登录页面
        const currentAccount = wsCache.get(ECacheType.ACCOUNT).account
        ApiLogin.logout(currentAccount);
        await router.push({name: 'login'})
    },
    [__AT_EXPIRE_CODE__]: async (resp) => {
        // at过期,无感刷新
        // 上次失败的请求
        let originalRequest = resp.config
        TokenService.getRemoteAT().then(res => {
            // 防止重复请求，设置originalRequest
            originalRequest.headers.at = TokenService.getAT()
            axiosInst.request(originalRequest)
        })
    },
    [__HEART_BEAT_CODE__]: (resp) => {

    },
}

export {
    NwCodeMap,
}
