// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// ============ 业务码处理表 + 通用错误提示出口（net 层唯一"失败一定要让人看见"的地方） ============
//
// 【这些码已被 AxiosInst 拦截器统一处理，本表不再重复登记】
// AT过期(10000)、RT过期(10001)、未登录(10002)、AT被篡改(10005)
// 已在 AxiosInst.js 响应拦截器中统一处理(含并发刷新风暴防护、防死循环、登出跳转)：
//  - 10000: handleATExpired 刷新AT后重试原请求(共享refreshPromise防风暴、__retried防死循环)
//  - 10001/10002/10005: triggerAuthFailure 清登录态+跳登录页(由main.js注册)
// 此处不再重复，避免一处逻辑两处维护导致刷新/登出被触发两次。
// 原先这里用 useRouter() 在模块顶层调用会拿到 undefined(不在setup上下文)，router.push 会崩，已移除。
//
// 【表怎么用】AxiosInst 的业务码分支只做一件事 —— 查表取 handler：
//      (nwCodeMap?.[code] || nwCodeMap?.fallback)?.(success)
// 命中的码走专用处理；未命中的码走表内 fallback 统一提示（文案见 CODE_TEXT）。
// 所以不要用空函数"占位"（那会把兜底提示吃掉）；想静默就在自己的 handler 里什么都不做。
import {ElMessage} from "element-plus";
// 样式按需注入：经过 unplugin 自动导入的 API 由 resolver 补样式，
// 而这里是显式 import，resolver 不会介入，所以自己引一次（重复引入会被打包器去重）。
import "element-plus/es/components/message/style/css";
import {isCanceled} from "./NetCancel.js";

// 业务码兜底文案：与后端 ResultCode 枚举保持一致。
// 后端用 Result.fail(ResultCode.RCxxxxx.getCode(), "更具体的文案") 时会带自己的 message，
// 那种情况优先用后端的，这里只兜"后端没给文案"的码。
const CODE_TEXT = {
    199: "失败",
    400: "请求参数不正确",
    401: "请求参数缺失",
    403: "无访问权限",
    404: "请求未找到",
    500: "系统异常，请稍后重试",
    501: "功能未实现/未开启",
    502: "错误的配置项",
    // 公共业务（参数校验/重复/为空）
    10101: "请求参数为空或不合法",
    10102: "数据已存在，禁止重复录入",
    10103: "目标数据不存在",
    10104: "文件超过限制大小(单个300MB,总共1000MB)",
    // 验证码
    10201: "验证码不正确",
    // 用户
    10301: "用户不存在",
    10302: "用户所在的部门不存在",
    10303: "密码错误",
    10304: "账号已存在，禁止重复创建",
    10305: "分配的角色不存在或不合法",
    10306: "不能停用当前登录账号自身",
    10307: "无操作权限",
    // 请求签名校验
    10401: "请求签名参数缺失",
    10402: "请求签名校验失败",
    10403: "请求已过期",
    10404: "重复请求(请勿重复提交)",
    // 文件服务（MinIO）
    10501: "文件记录不存在",
    10502: "文件未上传完成，无法下载",
    10503: "文件对象不存在",
    10504: "文件已删除",
    10505: "生成下载地址失败",
    10506: "文件上传失败",
    10507: "文件下载失败",
    10508: "文件删除失败",
    "-1": "未知错误",
};

const NwCodeMap = {
    // 心跳：正常业务动作，不该打扰用户
    [__HEART_BEAT_CODE__]: (resp) => {

    },
    // 未登记业务码的兜底 handler：统一弹提示。删掉它 = 未登记的业务码彻底静默。
    fallback: (resp) => notifyError(resp, CODE_TEXT[resp?.data?.code]),
}

// ============ 错误提示出口（码表兜底与视图层共用）============
// 同一维度（业务码 / HTTP码 / 网络层）的重复提示抑制窗口：
// 吃掉"码表兜底 + 视图兜底"在一次请求内的重复，以及并发请求的提示风暴。
const DEDUP_MS = 1200;
let _lastKey = "";
let _lastAt = 0;

/** 只认普通对象响应体：文件下载场景 data 是 Blob/ArrayBuffer，不能当业务体解析 */
function _body(v) {
    return (v && typeof v === "object" && !(v instanceof Blob) && !(v instanceof ArrayBuffer)) ? v : null;
}

/** 网络层文案：请求根本没到服务端（断网 / 超时 / CORS） */
function _netText(fail) {
    const c = fail?.code;
    if (c === "ECONNABORTED" || c === "ETIMEDOUT") return "请求超时，请重试";
    if (c === "ERR_NETWORK") return "网络连接失败，请检查网络";
    return null;
}

/**
 * 弹一条错误提示（带去重）。码表兜底与视图层都用它，是全站唯一的提示出口。
 *
 * @param fail      axios 的 reject 值。业务失败传 response 对象（fail.data 即 Result），
 *                  HTTP/网络失败传 AxiosError。两者都吃。
 * @param fallbacks 兜底文案，按顺序取第一个非空：码表传"码表文案"，视图传"场景文案"。
 *
 * 文案优先级：后端 message > fallbacks > 网络层文案 > "操作失败"。
 * 说明：同一次请求里"码表兜底"先执行、"视图兜底"后执行，落在去重窗口内的第二次会被吃掉，
 *       所以实际显示的多半是后端 message（后端几乎总会给 message）或码表文案。
 */
function notifyError(fail, ...fallbacks) {
    // 主动取消（AbortController：翻页、离开页面、超时重发）必然发生，不该打扰用户。
    // 判据与 AxiosInst 共用 isCanceled（NetCancel.js），别在这里另写一份：两处漂移就会重新刷出提示。
    if (isCanceled(fail)) return;
    // 显式静默：发起请求时传 option {__silentError__: true}
    if (fail?.config?.__silentError__ === true) return;

    const body = _body(fail?.data) || _body(fail?.response?.data);
    const code = (typeof body?.code === "number") ? body.code : null;
    const httpStatus = (typeof fail?.response?.status === "number")
        ? fail.response.status
        : ((typeof fail?.status === "number") ? fail.status : null);

    let msg = body?.message || body?.msg || fallbacks.find(t => t) || "";
    if (!msg && code != null) msg = "操作失败（错误码 " + code + "）";
    // HTTP 200 是正常响应，落到这里只是因为业务码不认识，别拿它当错误文案
    if (!msg && httpStatus != null && httpStatus !== 200) msg = "请求失败(" + httpStatus + ")";
    if (!msg) msg = _netText(fail) || "操作失败";

    // 去重维度：业务码 / HTTP 状态码 / 网络层
    const key = (code != null) ? ("C" + code)
        : ((httpStatus != null && httpStatus !== 200) ? ("H" + httpStatus) : "NET");
    const now = Date.now();
    if (key === _lastKey && now - _lastAt < DEDUP_MS) return;
    _lastKey = key;
    _lastAt = now;

    ElMessage.error(msg);
}

export {
    NwCodeMap, CODE_TEXT, notifyError,
}
