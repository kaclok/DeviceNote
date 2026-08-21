import axios from "axios"
import {config} from './Config.js'

import {ECacheType, useCache} from '@/framework/composable/use/useCache.ts'
import {TokenService} from "@/framework/services/TokenService.js";

const {wsCache} = useCache()

// https://www.axios-http.cn/docs/urlencoded 默认情况下，axios将 JavaScript 对象序列化为 JSON 。 要以application/x-www-form-urlencoded格式发送数据，您可以使用以下选项之一。
// 当请求头中的 content-type 是 application/x-www-form-urlencoded 时，Axios 将自动地将普通对象序列化成 urlencoded 的格式。

// https://www.axios-http.cn/docs/multipart 从 v0.27.0 版本开始，当请求头中的 Content-Type 是 multipart/form-data 时，Axios 支持自动地将普通对象序列化成一个 FormData 对象

// https://www.axios-http.cn/docs/instance
// https://www.axios-http.cn/docs/config_defaults
const springBootURL = import.meta.env.VITE_BASE_API;
let baseURL = config.base_url;
// const url = wsCache.get(ECacheType.RES_URL);
// if (url) {
//     baseURL = url;
// }

const axiosInst = axios.create({
    baseURL: baseURL, timeout: 60000
});

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

// 鉴权失败(RT过期/未登录/AT被篡改)时的处理函数，由应用层(main.js)注册：
// 一般是清登录态并跳转登录页。框架层不直接耦合路由与具体登出接口。
let authFailureHandler = null;

function changeAuthFailureHandler(fn) {
    authFailureHandler = fn;
}

function triggerAuthFailure(needLogout = false) {
    authFailureHandler?.(needLogout);
}

// ============ access token 无感刷新 ============
// 解决并发刷新风暴：页面同时发出多个请求，AT一起过期，若各自刷新会触发N次refresh。
// 方案：首个失败请求发起刷新，其余请求复用同一个refreshPromise，刷新成功后再各自重试。
// (防死循环逻辑暂未加入，待理解刷新风暴后再补)
let isRefreshing = false;
let refreshPromise = null;

// 处理AT过期(业务码__AT_EXPIRE_CODE__)：刷新AT后重试原请求
async function handleATExpired(failedResponse) {
    const originalConfig = failedResponse.config;

    // 并发刷新风暴：已有刷新在进行，当前请求排队等待同一份刷新结果
    if (isRefreshing && refreshPromise) {
        try {
            await refreshPromise;
        } catch {
            // 刷新失败，原请求也无法恢复(登出由发起刷新的那个请求负责)
            throw failedResponse;
        }
        // 刷新成功，用新AT重试原请求
        originalConfig.headers.at = TokenService.getAT();
        return axiosInst.request(originalConfig);
    }

    // 首个触发刷新的请求：发起refresh，期间其它并发请求会复用此promise
    isRefreshing = true;
    refreshPromise = TokenService.getRemoteAT();
    try {
        await refreshPromise;
    } catch {
        // 仅当refresh本身失败(RT过期/无效)才触发登出
        triggerAuthFailure();
        throw failedResponse;
    } finally {
        isRefreshing = false;
        refreshPromise = null;
    }
    // 刷新成功(新AT已由响应拦截器_setToken写入存储)，用新AT重试原请求
    // 注意：重试失败不在此处catch，错误原样向上抛，避免误触发登出
    originalConfig.headers.at = TokenService.getAT();
    return axiosInst.request(originalConfig);
}

function _setToken(resp) {
    const at = resp.headers.at
    if (at) { // 尝试保存at
        TokenService.setAT(at)
        TokenService.setATIssueAt(resp.headers.at_issue_at)
        TokenService.setATExpireAt(resp.headers.at_expire_at)

        // 给axios设置默认的at,
        // ? 能否不设置，因为在request的拦截器中，有对于at的赋值
        // 之后所有通过 axiosInst 发送的请求，都会自动在请求头中携带 rt: <token值>
        // axiosInst.defaults.headers.at = at
    }

    const rt = resp.headers.rt
    if (rt) {  // 尝试保存rt
        TokenService.setRT(rt)
        TokenService.setRTIssueAt(resp.headers.rt_issue_at)
        TokenService.setRTExpireAt(resp.headers.rt_expire_at)

        // 给axios设置默认的rt
        // 因为访问资源服务仅仅使用at, 所有一般不设置rt, 因为会导致网络协议传输过大
        // axiosInst.defaults.headers.rt = rt
    }
}

// https://mp.weixin.qq.com/s/sWDnhq6MCUusQ0-aUpfNPw
// https://v.douyin.com/iyUS3KtS/ https://v.douyin.com/iyUSqx35/
// 参考：实现token无感刷新 https://github.com/yudaocode/yudao-ui-admin-vue3/blob/master/src/config/axios/service.ts#L117
// https://www.axios-http.cn/docs/interceptors
// 添加响应拦截器，其实是把异步成功回调、失败回调给统一封装
// axiosInst.interceptors.response.use(onFulfilled, onRejected);通过它可以在请求得到响应后，对响应数据进行统一的处理
// onFulfilled：一个可选的回调函数，当响应成功（状态码在 200 - 299 之间）时会被调用。它接收一个 response 对象作为参数，你可以在这个函数里对响应数据进行处理，最后返回处理后的 response 对象。
// onRejected：一个可选的回调函数，当响应失败（状态码不在 200 - 299 之间或者发生网络错误等）时会被调用。它接收一个 error 对象作为参数，你可以在这个函数里对错误进行处理，最后可以选择返回一个被拒绝的 Promise 或者一个新的 response 对象

// let r = await syncFunction(x, y).then((a) => {}).catch((b) => {})
// syncFunction(x, y) 会返回一个 Promise 对象。
// 接着调用 then 方法，它接收一个回调函数作为参数，该回调函数会在 Promise 状态变为 fulfilled（已成功）时执行。
// 再调用 catch 方法，其回调函数会在 Promise 状态变为 rejected（已失败）时执行。
// await 会等待 syncFunction(x, y).then(...).catch(...) 这个链式调用最终返回的 Promise 对象解决（fulfilled 或 rejected），然后将结果赋值给 r。
// 此时await其实等待的是then或者catch内部包装之后新的Promise对象，而不是原来的Promise对象。
// 建议要么用await配合try catch，要么就then catch不用await, 不要混用
axiosInst.interceptors.response.use(async (success) => {
    _setToken(success);
    // 如果是文件下载等情况，直接返回
    if ((success.data instanceof Blob) || (success.data instanceof ArrayBuffer)) {
        return success.data;
    }

    const code = success.data?.code;
    if (code === __OK__) {
        // 成功处理，走then分支
        return success;
    }

    // AT过期：刷新AT后重试原请求(含并发刷新风暴防护与防死循环)
    if (code === __AT_EXPIRE_CODE__) {
        return handleATExpired(success);
    }

    // RT过期 / 未登录 / AT被篡改：触发登出(清登录态+跳登录页)
    const needLogout = (code === __RT_EXPIRE_CODE__ || code === __AT_EXPIRE_INVALID__)
    if (needLogout || code === __AT_EMPTY__) {
        triggerAuthFailure(needLogout);
        return Promise.reject(success);
    }

    // https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    // 处理其它错误码情况
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
    // https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    const isRT = TokenService.isRT(config)
    config.headers.at = TokenService.getAT();
    if (isRT) {
        config.headers.rt = TokenService.getRT();
    }

    // console.error("-----------------request url: %s, isRT: %s", config.url, isRT);
    return config;
}, fail => {
    console.error(fail);
    // 异步状态转换为失败状态，走到catch分支
    return Promise.reject(fail);
})

export {
    axiosInst, changeResBaseURL, changeNwCodeMap, changeHttpCodeMap, changeAuthFailureHandler, triggerAuthFailure
}
