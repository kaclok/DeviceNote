import axios from "axios"
import {config} from './Config.js'

import {useCache, ECacheType} from '@/framework/utils/use/useCache.ts'
import {TokenService} from "@/framework/services/TokenService.js";

const {wsCache} = useCache()

// https://www.axios-http.cn/docs/urlencoded 默认情况下，axios将 JavaScript 对象序列化为 JSON 。 要以application/x-www-form-urlencoded格式发送数据，您可以使用以下选项之一。
// 当请求头中的 content-type 是 application/x-www-form-urlencoded 时，Axios 将自动地将普通对象序列化成 urlencoded 的格式。

// https://www.axios-http.cn/docs/multipart 从 v0.27.0 版本开始，当请求头中的 Content-Type 是 multipart/form-data 时，Axios 支持自动地将普通对象序列化成一个 FormData 对象

// https://www.axios-http.cn/docs/instance
// https://www.axios-http.cn/docs/config_defaults
const springBootURL = import.meta.env.VITE_BASE_API;
let baseURL = config.base_url;
const url = wsCache.get(ECacheType.RES_URL);
if (url) {
    baseURL = url;
}

const axiosInst = axios.create({baseURL: baseURL, timeout: 60000});

function changeResBaseURL(targetBaseURL) {
    baseURL = targetBaseURL;
    wsCache.set(ECacheType.RES_URL, targetBaseURL);
    axiosInst.defaults.baseURL = targetBaseURL;
}

let nwCodeMap = null;

function changeNwCodeMap(targetNwCodeMap) {
    nwCodeMap = targetNwCodeMap;
}

let httpCodeMap = null;

function changeHttpCodeMap(targetHttpCodeMap) {
    httpCodeMap = targetHttpCodeMap;
}

function _setToken(resp) {
    const at = resp.headers.at
    if (at) { // 尝试保存at
        TokenService.setAT(at)
        TokenService.setATIssueAt(resp.headers.atIssueAt)
        TokenService.setATExpireAt(resp.headers.atExpireAt)

        // 给axios设置默认的at,
        // ? 能否不设置，因为在request的拦截器中，有对于at的赋值
        axiosInst.defaults.headers.at = at
    }

    const rt = resp.headers.rt
    if (rt) {  // 尝试保存rt
        TokenService.setLRT(rt)
        TokenService.setRTIssueAt(resp.headers.rtIssueAt)
        TokenService.setRTExpireAt(resp.headers.rtExpireAt)

        // 给axios设置默认的rt
        // 因为访问资源服务仅仅使用at, 所有一般不设置rt, 因为会导致网络协议传输过大
        // axiosInstance.defaults.headers.rt = rt
    }
}

function _getToken(config) {
    // https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    const isRT = TokenService.isRT(config)
    if (!isRT) {
        config.headers.at = TokenService.getAT();
    } else {
        config.headers.rt = TokenService.getRT();
    }
    console.log("__isRT: ", isRT);
    return config
}

// https://mp.weixin.qq.com/s/sWDnhq6MCUusQ0-aUpfNPw
// https://v.douyin.com/iyUS3KtS/ https://v.douyin.com/iyUSqx35/
// 参考：实现token无感刷新 https://github.com/yudaocode/yudao-ui-admin-vue3/blob/master/src/config/axios/service.ts#L117
// https://www.axios-http.cn/docs/interceptors
// 添加响应拦截器，其实是把异步成功回调、失败回调给统一封装
axiosInst.interceptors.response.use(success => {
    _setToken(success);
    // 如果是文件下载等情况，直接返回
    if ((success.data instanceof Blob) || (success.data instanceof ArrayBuffer)) {
        return success.data;
    }

    const {code} = success.data;
    if (code === __OK__) {
        // 成功处理，走then分支
        return success;
    }

    // https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    // 处理错误码情况
    nwCodeMap?.[code]?.(success);
    // 也当做失败处理，让走catch分支
    return Promise.reject(success);
}, fail => {
    console.error(fail);

    const {status} = fail;
    // https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    httpCodeMap?.[status]?.(fail);
    // 异步状态转换为失败状态，走到catch分支
    return Promise.reject(fail);
})

// request拦截器, 让每个请求添加token
// https://www.axios-http.cn/docs/interceptors
// 添加响应拦截器，其实是把异步成功回调、失败回调给统一封装
axiosInst.interceptors.request.use(config => {
    return _getToken(config);
}, fail => {
    console.error(fail);
    // 异步状态转换为失败状态，走到catch分支
    return Promise.reject(fail);
})

export {
    axiosInst, changeResBaseURL, changeNwCodeMap, changeHttpCodeMap,
}