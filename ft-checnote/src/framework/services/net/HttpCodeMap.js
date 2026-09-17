// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// ============ HTTP 层错误处理表 ============
// 只登记"需要特殊处理(含显式静默)"的 HTTP 状态码，例如某个轮询接口想自定义文案或想静默。
//
// 【表怎么用】AxiosInst 的失败分支只做一件事 —— 查表取 handler：
//      (httpCodeMap?.[status] || httpCodeMap?.fallback)?.(fail)
// 未登记的码走表内 fallback 统一提示（文案见 HTTP_TEXT）。
// 所以不要用空函数"占位" —— 那会把兜底提示吃掉（原先 400/403/404 就是空函数，导致 HTTP 层错误全部静默）。
//
// 注意：后端约定"鉴权失败"走 HTTP 200 + 业务码 10001/10002/10005，不在本表处理。
// 通用提示机制 notifyError 定义在 NwCodeMap.js，本表复用它。
import {notifyError} from "./NwCodeMap.js";

// HTTP 状态码兜底文案（401/403 多来自网关/代理/容器，因为后端鉴权失败走"HTTP 200 + 业务码"）
const HTTP_TEXT = {
    400: "请求参数有误",
    401: "登录状态已失效，请重新登录",
    403: "无访问权限",
    404: "请求的接口不存在",
    405: "请求方式不被允许",
    408: "请求超时，请重试",
    413: "上传内容过大",
    429: "操作过于频繁，请稍后再试",
    500: "服务器异常，请稍后重试",
    502: "网关异常，请稍后重试",
    503: "服务暂不可用，请稍后重试",
    504: "网关超时，请稍后重试",
};

const httpCodeMap = {
    // 未登记状态码的兜底 handler：统一弹提示。删掉它 = 未登记的码彻底静默。
    fallback: (fail) => notifyError(fail, HTTP_TEXT[fail?.status]),
}

export {
    httpCodeMap, HTTP_TEXT,
}
