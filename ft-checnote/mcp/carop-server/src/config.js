// MCP Server 配置：全部可用环境变量覆盖
//
// ⚠️ stdio 传输下 stdout 专供 MCP 协议，本进程内所有日志必须走 console.error(stderr)，
//    一旦用 console.log 写入 stdout，客户端会解析协议失败。
import {fileURLToPath} from 'node:url'

function env(name, fallback) {
    const v = process.env[name]
    return (v === undefined || v === '') ? fallback : v
}

export const config = {
    /**
     * 后端地址。carop 接口不在 AccessInterceptor 的 /cghtz/** 拦截范围内，无需登录态，
     * 所以 MCP 侧可以直接调绝对地址，不需要走前端的 vite proxy / nginx 前缀。
     */
    backendUrl: env('CAROP_BASE_URL', 'http://10.8.13.66:7090'),

    /** 单次后端调用超时(ms) */
    requestTimeoutMs: Number(env('CAROP_TIMEOUT_MS', '15000')),

    /**
     * 车道/操作字典的唯一真相源 = 前端 carop 的 gd.json。
     * 直接读它，避免"车道列表在 MCP 里再抄一份"这类两处维护。
     */
    gdJsonPath: env('CAROP_GD_JSON',
        fileURLToPath(new URL('../../../src/cms/smlj/carop/data/gd.json', import.meta.url))),

    /** 审计日志：道闸控制的每次调用(含被拒绝的)都追加一行 JSONL */
    auditLogPath: env('CAROP_AUDIT_LOG',
        fileURLToPath(new URL('../logs/carop-audit.log', import.meta.url))),
}
